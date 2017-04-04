package br.com.homembala.dedos;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.MapTile;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;

import java.io.IOException;
import java.util.Hashtable;

import br.com.homembala.dedos.util.Vehicle;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by tiago on 27/03/17.
 */
public class CsiActivity extends AppCompatActivity {
    private static final double MAP_SIZE = 20037508.34789244 * 2;
    private static final double[] TILE_ORIGIN = {-20037508.34789244,20037508.34789244};
    private boolean show_labels=false;
    private Overlay olabels;
    private Overlay closeup;
    private Hashtable<String,Overlay> overlays;

    private static final int FREEHAND = 1;
    private static final int MAP = 0;
    private static final int VEHICLES = 2;

    private int current_mode;
    private boolean is_updating_labels;
    private boolean update_labels_after;
    private boolean is_drawing;

    private boolean is_updating_closeup;
    private JSONArray vehicles;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.csi);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        ((Iat) getApplicationContext()).startGPS(this);
        ((Panel) findViewById(R.id.drawing_panel)).setVisibility(View.GONE);
        findViewById(R.id.vehicles_canvas).setVisibility(View.GONE);
        final MapView map = (MapView) findViewById(R.id.map);
        String u="http://bigrs.alien9.net:8080/geoserver/gwc/service/tms/1.0.0/";
        map.setTileSource(new GeoServerTileSource("geoserver", 17, 21, 512, ".png", new String[]{u}));
        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        map.setClickable(true);
        map.setUseDataConnection(true);
        ScaleBarOverlay sbo = new ScaleBarOverlay(map);
        sbo.setCentred(false);
        sbo.setScaleBarOffset(10, 10);
        map.getController().setZoom(20);
        map.setMaxZoomLevel(21);
        map.setTilesScaledToDpi(true);
        map.getOverlays().add(sbo);
        overlays=new Hashtable<>();
        JSONObject point = ((Iat) getApplicationContext()).getLastKnownPosition();
        if(!point.has("latitude")){
            try {
                point.put("longitude",-46.625290);
                point.put("latitude",-23.533773);
            } catch (JSONException ignore) {}
        }
        map.getController().setCenter(new GeoPoint(point.optDouble("latitude"), point.optDouble("longitude")));
        map.setMapListener(new DelayedMapListener(new MapListener() {
            public boolean onZoom(final ZoomEvent e) {
                if(show_labels){
                    updateLabels();
                }
                if(map.getZoomLevel()>21){
                    updateCloseUp();
                }else{
                    if(overlays.containsKey("closeup")) {
                        map.getOverlays().remove(overlays.get("closeup"));
                        overlays.remove("closeup");
                        map.invalidate();
                    }
                }
                loadVehicles();
                return true;
            }
            public boolean onScroll(final ScrollEvent e) {
                if(show_labels){
                    updateLabels();
                }
                return true;
            }
        }, 1000 ));
        is_updating_labels =false;
        update_labels_after =false;
        is_updating_closeup=false;
        findViewById(R.id.vehicles_canvas).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(current_mode==FREEHAND) return false;
                return true;
            }
        });
        //carros vêm na intention
        vehicles=new JSONArray();

        for(int i=0;i<2;i++) {
            JSONObject v = new JSONObject();
            try {
                v.put("model", Vehicle.CARRO);
                v.put("width", 2.0);
                v.put("length", 3.9);
            } catch (JSONException e) {}
            vehicles.put(v);
        }
        for(int i=0;i<3;i++) {
            JSONObject v = new JSONObject();
            try {
                v.put("model", Vehicle.MOTO);
                v.put("width", 1.8);
                v.put("length", 2.5);
            } catch (JSONException e) {}
            vehicles.put(v);
        }

        loadVehicles();
        //Vehicle carrinho=new Vehicle(this, findViewById(R.id.vehicles_canvas),width,height);
        //((ViewGroup)findViewById(R.id.vehicles_canvas)).addView(carrinho);
        //carrinho=new Vehicle(this, findViewById(R.id.vehicles_canvas),width,height);
        //((ViewGroup)findViewById(R.id.vehicles_canvas)).addView(carrinho);
        current_mode=MAP;
    }

    private void updateCloseUp() {
        MapView map = (MapView) findViewById(R.id.map);
        if(closeup!=null){
            map.getOverlays().remove(closeup);
            closeup=null;
        }
        if(is_updating_closeup) return;
        BoundingBox bb = map.getBoundingBox();
        Double[] northwest = degrees2meters(bb.getLonWest(), bb.getLatNorth());
        Double[] southeast = degrees2meters(bb.getLonEast(), bb.getLatSouth());
        String url = String.format("%s?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&FORMAT=image/png&TRANSPARENT=true&STYLES&LAYERS=BIGRS:quadras_e_logradouros&SRS=EPSG:3857&WIDTH=%s&HEIGHT=%s&BBOX=%s,%s,%s,%s",
                getResources().getString(R.string.wms_url),
                map.getWidth(),map.getHeight(),
                northwest[0],
                southeast[1],
                southeast[0],
                northwest[1]
        );
        new LayerLoader(url, bb, "closeup").execute();//degrees2pixels(bb.getLonWest(), bb.getLatNorth(),map.getZoomLevel()),degrees2pixels(bb.getLonEast(), bb.getLatSouth(),map.getZoomLevel())).execute();
    }

    private void updateLabels() {
        if(is_updating_labels){
            update_labels_after=true;
            return;
        }
        setLabels(show_labels);
    }

    @Override
    public void onBackPressed() {
        ((Panel) findViewById(R.id.drawing_panel)).back();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.drawing_controls, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.labels).setChecked(show_labels);
        menu.findItem(R.id.mode_map).setChecked(current_mode==MAP);
        menu.findItem(R.id.mode_freehand).setChecked(current_mode==FREEHAND);
        menu.findItem(R.id.mode_vehicles).setChecked(current_mode==VEHICLES);
        int z = ((MapView) findViewById(R.id.map)).getZoomLevel();
        menu.findItem(R.id.mode_freehand).setEnabled(z>19);
        menu.findItem(R.id.mode_vehicles).setEnabled(z>19);
        menu.findItem(R.id.tombar_veiculo).setVisible(((Iat)getApplicationContext()).getSelectedVehicle()!=null);
        menu.findItem(R.id.reset_veiculo).setVisible(current_mode!=MAP);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.center_here:
                MapView map= (MapView) findViewById(R.id.map);
                JSONObject point = ((Iat) getApplicationContext()).getLastKnownPosition();
                if(point.has("latitude")) {
                    map.getController().setCenter(new GeoPoint(point.optDouble("latitude"), point.optDouble("longitude")));
                }
                break;
            case R.id.labels:
                map= (MapView) findViewById(R.id.map);
                if(item.isChecked()){
                    item.setChecked(false);
                    show_labels=false;
                }else{
                    item.setChecked(true);
                    show_labels=true;
                }
                setLabels(show_labels);
                break;
            case R.id.mode_map:
                current_mode=MAP;
                saveVehicles();
                findViewById(R.id.drawing_panel).setVisibility(View.GONE);
                findViewById(R.id.vehicles_canvas).setVisibility(View.GONE);
                break;
            case R.id.mode_freehand:
                current_mode=FREEHAND;
                saveVehicles();
                findViewById(R.id.drawing_panel).setVisibility(View.VISIBLE);
                findViewById(R.id.vehicles_canvas).setVisibility(View.VISIBLE);
                ((Panel)findViewById(R.id.drawing_panel)).setLigado(true);
                ligaCarros(false);
                break;
            case R.id.mode_vehicles:
                current_mode=VEHICLES;
                loadVehicles();
                findViewById(R.id.drawing_panel).setVisibility(View.VISIBLE);
                findViewById(R.id.vehicles_canvas).setVisibility(View.VISIBLE);
                ((Panel)findViewById(R.id.drawing_panel)).setLigado(false);
                ligaCarros(true);
                break;
            case R.id.tombar_veiculo:
                Vehicle v = ((Iat) getApplicationContext()).getSelectedVehicle();
                if(v!=null){
                    v.vira();
                }
                break;
            case R.id.reset_veiculo:
                resetVehicles();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void ligaCarros(boolean b) {
        int z=((MapView)findViewById(R.id.map)).getZoomLevel();
        ViewGroup cv = (ViewGroup) findViewById(R.id.vehicles_canvas);
        for(int i=0;i<cv.getChildCount();i++){
            View car = cv.getChildAt(i);
            if(car.getClass().getCanonicalName().equals(Vehicle.class.getCanonicalName())){
                ((Vehicle)car).liga(b);
            }
        }
    }

    private void setLabels(boolean b) {
        if(is_updating_labels) return;
        is_updating_labels=true;
        MapView map = (MapView) findViewById(R.id.map);
        if (b) {
            BoundingBox bb = map.getBoundingBox();
            Double[] northwest = degrees2meters(bb.getLonWest(), bb.getLatNorth());
            Double[] southeast = degrees2meters(bb.getLonEast(), bb.getLatSouth());
            String url = String.format("%s?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&FORMAT=image/png&TRANSPARENT=true&STYLES&LAYERS=BIGRS:logradouros_3857&SRS=EPSG:3857&WIDTH=%s&HEIGHT=%s&BBOX=%s,%s,%s,%s",
                    getResources().getString(R.string.wms_url),
                    map.getWidth(),map.getHeight(),
                    northwest[0],
                    southeast[1],
                    southeast[0],
                    northwest[1]
            );
            new LayerLoader(url, bb, "labels").execute();//degrees2pixels(bb.getLonWest(), bb.getLatNorth(),map.getZoomLevel()),degrees2pixels(bb.getLonEast(), bb.getLatSouth(),map.getZoomLevel())).execute();
        }else{
            if(overlays.containsKey("labels")) {
                if (overlays.get("labels") != null) {
                    map.getOverlays().remove(overlays.get("labels"));
                    overlays.remove("labels");
                    map.invalidate();
                }
            }
        }
    }

    Double[] degrees2meters(double lon, double lat) {
        double x = lon * 20037508.34 / 180;
        double y = Math.log(Math.tan((90 + lat) * Math.PI / 360)) / (Math.PI / 180);
        y = y * 20037508.34 / 180;
        return new Double[]{x, y};
    }

    public void onResume() {
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
    }

    public class GeoServerTileSource extends OnlineTileSourceBase {
        private final String[] base_url;
        public GeoServerTileSource(String aName, int aZoomMinLevel, int aZoomMaxLevel, int aTileSizePixels, String aImageFilenameEnding, String[] aBaseUrl) {
            super(aName, aZoomMinLevel, aZoomMaxLevel, aTileSizePixels, aImageFilenameEnding, aBaseUrl);
            base_url = aBaseUrl;
        }
        @Override
        public String getTileURLString(MapTile aTile) {
            String u = "http://bigrs.alien9.net:8080/geoserver/gwc/service/tms/1.0.0/BIGRS%3Aquadras_e_logradouros@3857@png" + "/" + aTile.getZoomLevel() + "/" + aTile.getX() + "/" + (int)(Math.pow(2.0,aTile.getZoomLevel())-aTile.getY()-1) + ".png";
            Log.d("IAT request",u);
            return u;
        }
    }

    private class LayerLoader extends AsyncTask<String,String,Boolean>{
        private final String url;
        private final BoundingBox box;
        private final String overlay;
        private Bitmap mapbit;
        public LayerLoader(String u, BoundingBox bb,String o) {
            box=bb;
            url=u;
            overlay=o;
        }
        @Override
        protected Boolean doInBackground(String... strings) {
            OkHttpClient client = new OkHttpClient.Builder().build();
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            try {
                Response response = client.newCall(request).execute();
                mapbit = BitmapFactory.decodeStream(response.body().byteStream());
                return true;
            } catch (IOException e) {
                return false;
            }
        }
        @Override
        protected void onPostExecute(final Boolean success) {
            is_updating_labels=false;
            final MapView map = (MapView) findViewById(R.id.map);
            if(overlays.containsKey(overlay)) {
                map.getOverlays().remove(overlays.get(overlay));
                overlays.remove(overlay);
            }
            overlays.put(overlay,new Overlay() {
                @Override
                public void draw(Canvas canvas, MapView mapView, boolean b) {
                    if(mapbit==null)return;
                    Projection pj = map.getProjection();
                    Point pixel_nw = null;
                    pixel_nw=pj.toPixels(new GeoPoint(box.getLatNorth(),box.getLonWest()),pixel_nw);
                    Point pixel_se = null;
                    pixel_se=pj.toPixels(new GeoPoint(box.getLatSouth(),box.getLonEast()),pixel_se);
                    Log.d("IAT DRAW", ""+pixel_nw.toString());
                    canvas.drawBitmap(mapbit,null,new Rect(pixel_nw.x,pixel_nw.y,pixel_se.x,pixel_se.y),null);
                }
            });
            map.getOverlays().add(overlays.get(overlay));
            map.invalidate();
            if(update_labels_after){
                update_labels_after=false;
                setLabels(show_labels);
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults){
        Intent intent=new Intent(this,CsiActivity.class);
        startActivity(intent);
    }
    protected void saveVehicles() {
        try {
            ViewGroup o = ((ViewGroup) findViewById(R.id.vehicles_canvas));
            MapView map = (MapView) findViewById(R.id.map);
            for (int i = 0; i < vehicles.length(); i++) {
                JSONObject v = vehicles.optJSONObject(i);
                Vehicle vw = (Vehicle) o.getChildAt(i + 1);
                if(vw.getMexido()) {
                    JSONObject position = vw.getPosition();
                    IGeoPoint latlng = map.getProjection().fromPixels(position.getInt("x"), position.getInt("y"));
                    v.put("latitude", latlng.getLatitude());
                    v.put("longitude", latlng.getLongitude());
                    v.put("position", position);
                    v.put("roll", vw.getRoll());
                    vehicles.put(i, v);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ((Iat)getApplicationContext()).setSelectedVehicle(null);
    }
    protected void loadVehicles() {
        ((Iat)getApplicationContext()).setSelectedVehicle(null);
        ViewGroup o = ((ViewGroup) findViewById(R.id.vehicles_canvas));
        MapView map = (MapView) findViewById(R.id.map);
        for (int i = o.getChildCount()-1; i >=0; i--) {
            if (o.getChildAt(i).getClass().getCanonicalName().equals(Vehicle.class.getCanonicalName())) {
                o.removeView(o.getChildAt(i));
            }
        }
        int auto_increment_position = 10;
        for (int i = 0; i < vehicles.length(); i++) {
            int width = (int) (vehicles.optJSONObject(i).optDouble("width") * 1000 * Math.pow(2.0, map.getZoomLevel()) / (2 * 20037508.34));
            int height = (int) (vehicles.optJSONObject(i).optDouble("length") * 1000 * Math.pow(2.0, map.getZoomLevel()) / (2 * 20037508.34));
            Vehicle v = new Vehicle(this, findViewById(R.id.vehicles_canvas), width, height, vehicles.optJSONObject(i).optInt("model"),vehicles.optJSONObject(i).optInt("roll"));
            ((ViewGroup) findViewById(R.id.vehicles_canvas)).addView((View) v);
            if(vehicles.optJSONObject(i).has("latitude") && vehicles.optJSONObject(i).has("longitude")){
                Point pix = map.getProjection().toPixels(new GeoPoint(vehicles.optJSONObject(i).optDouble("latitude"), vehicles.optJSONObject(i).optDouble("longitude")), null);
                v.setX(pix.x);
                v.setY(pix.y);
            }else{
                v.setX(auto_increment_position);
                auto_increment_position+=width;
            }
            if(vehicles.optJSONObject(i).has("position")){
                v.setRotation((float) vehicles.optJSONObject(i).optJSONObject("position").optDouble("heading"));
            }
        }
    }
    protected void resetVehicles(){
        ((Iat)getApplicationContext()).setSelectedVehicle(null);
        for (int i = 0; i < vehicles.length(); i++) {
            JSONObject v = vehicles.optJSONObject(i);
            v.remove("latitude");
            v.remove("longitude");
            v.remove("position");
            v.remove("roll");
        }
        loadVehicles();
    }
}