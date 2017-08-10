package br.com.homembala.dedos;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.bonuspack.overlays.GroundOverlay;
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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;

import br.com.homembala.dedos.util.Pega;
import br.com.homembala.dedos.util.VehicleFix;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by tiago on 27/03/17.
 */
public class CsiActivity extends AppCompatActivity {
    private static final double MAP_SIZE = 20037508.34789244 * 2;
    private static final double[] TILE_ORIGIN = {-20037508.34789244,20037508.34789244};
    private boolean show_labels=true;
    private Overlay closeup;
    private Hashtable<String,Overlay> overlays;

    private static final int FREEHAND = 1;
    private static final int MAP = 0;
    private static final int VEHICLES = 2;

    private int current_mode;
    private boolean is_updating_labels;
    private boolean update_labels_after;

    private boolean is_updating_closeup;
    private JSONArray vehicles;
    private Context context;
    private GroundOverlay vehicles_zooming_over;
    private GroundOverlay drawing_zooming_over;
    private GeoServerTileSource clear_source;
    private GeoServerTileSource great_source;
    private boolean show_semaforos=true;
    private View selectedVehicle;
    private String[] mess;
    private int mode;
    private JSONArray paths;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.csi);
        //findViewById(R.id.messageria).setVisibility(View.GONE);
        mess=new String[]{"","","","","","","","","","","","","","",""};
        context=this;
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        ((Iat) getApplicationContext()).startGPS(this);
        //((Panel) findViewById(R.id.drawing_panel)).setVisibility(View.GONE);
        findViewById(R.id.vehicles_canvas).setDrawingCacheEnabled(true);
        final MapView map = (MapView) findViewById(R.id.map);
        String u="http://bigrs.alien9.net:8080/geoserver/gwc/service/tms/1.0.0/";
        clear_source = new GeoServerTileSource("quadras_e_logradouros", 17, 21, 512, ".png", new String[]{u});
        great_source = new GeoServerTileSource("Cidade+com+Sem%C3%A1foros", 17, 21, 512, ".png", new String[]{u});
        map.setTileSource(great_source);
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
                //loadVehicles();
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
                if(motionEvent.getAction()==MotionEvent.ACTION_DOWN){
                    if(current_mode!=VEHICLES) return false;
                }
                View v = getSelectedVehicle();
                if(v==null) return false;


                return true;
            }
        });
        vehicles=new JSONArray();
        Pega pegador=(Pega)findViewById(R.id.pegador);
        pegador.setPontaPosition(10000,10000,0);
        current_mode=VEHICLES;
        ((ImageButton)findViewById(R.id.show_pallette)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setSelectedVehicle(null);
                map.setBuiltInZoomControls(false);
                map.invalidate();
                findViewById(R.id.show_pallette).setVisibility(View.GONE);
                findViewById(R.id.palette_layout).setVisibility(View.VISIBLE);
            }
        });
        findViewById(R.id.palette_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.show_pallette).setVisibility(View.VISIBLE);
                findViewById(R.id.palette_layout).setVisibility(View.GONE);
            }
        });
        findViewById(R.id.palette_layout).setVisibility(View.GONE);
        View.OnClickListener cl = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setSelectedVehicle(null);
                int id=view.getId();
                switch(id){
                    case R.id.tools_carro:
                        createVehicle(VehicleFix.CARRO,1.9,3.8);
                        Log.d("IAT","deve ter criado");
                        break;
                    case R.id.tools_moto:
                        createVehicle(VehicleFix.MOTO,2.8,4.4);
                        break;
                    case R.id.tools_onibus:
                        createVehicle(VehicleFix.ONIBUS,3.8,10.4);
                        break;
                    case R.id.tools_vuc:
                        createVehicle(VehicleFix.CAMINHAO,3.9,11.4);
                        break;
                    case R.id.tools_bici:
                        createVehicle(VehicleFix.BICI,1.8,2.0);
                        break;
                    case R.id.tools_pessoa:
                        createVehicle(VehicleFix.PEDESTRE,2.0,2.5);
                        break;
                    case R.id.tools_colisao:
                        createVehicle(VehicleFix.COLISAO,2.0,2.0);
                        break;
                    case R.id.tools_freada:
                        ((RadioButton)findViewById(R.id.radio_desenho)).setChecked(true);
                        setCurrentMode(FREEHAND);
                        //map.getController().setZoom(20);
                        findViewById(R.id.show_pallette).setVisibility(View.GONE);
                        findViewById(R.id.drawing_panel).setVisibility(View.VISIBLE);
                        ((Panel)findViewById(R.id.drawing_panel)).setStyle(Panel.SKID);
                        ((Panel)findViewById(R.id.drawing_panel)).setLigado(true);
                        ((Panel)findViewById(R.id.drawing_panel)).setVisibility(View.VISIBLE);
                        break;
                    case R.id.tools_zebra:
                        ((RadioButton)findViewById(R.id.radio_desenho)).setChecked(true);
                        setCurrentMode(FREEHAND);
                        //map.getController().setZoom(20);
                        findViewById(R.id.show_pallette).setVisibility(View.GONE);
                        findViewById(R.id.drawing_panel).setVisibility(View.VISIBLE);
                        ((Panel)findViewById(R.id.drawing_panel)).setStyle(Panel.ZEBRA);
                        ((Panel)findViewById(R.id.drawing_panel)).setLigado(true);
                        ((Panel)findViewById(R.id.drawing_panel)).setVisibility(View.VISIBLE);
                        break;
                    case R.id.move_map_command:
                        ((RadioButton)findViewById(R.id.radio_mapa)).setChecked(true);
                        break;
                    case R.id.edit_command:
                        ((RadioButton)findViewById(R.id.radio_desenho)).setChecked(true);
                        break;
                }
                findViewById(R.id.show_pallette).setVisibility(View.VISIBLE);
                findViewById(R.id.palette_layout).setVisibility(View.GONE);
            }
        };
        Log.d("IAT","veiculo criado!");
        setDescendentOnClickListener((ViewGroup) findViewById(R.id.palette_container),cl);
        findViewById(R.id.vehicles_layout).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction()==MotionEvent.ACTION_MOVE){
                    Log.d("IAT","movendo o maus");
                }
                return false;
            }
        });
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radio_set_mode);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.radio_mapa:
                        setCurrentMode(MAP);
                        break;
                    case R.id.radio_desenho:
                        setCurrentMode(VEHICLES);
                }
            }
        });
    }

    private void setDescendentOnClickListener(ViewGroup gw, View.OnClickListener cl) {
        for(int i=0;i<gw.getChildCount();i++) {
            gw.getChildAt(i).setOnClickListener(cl);
        }
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
    public void onBackPressed(){
        if(findViewById(R.id.palette_layout).getVisibility()==View.VISIBLE){
            findViewById(R.id.palette_layout).setVisibility(View.GONE);
            findViewById(R.id.show_pallette).setVisibility(View.VISIBLE);
            return;
        }
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
        menu.findItem(R.id.semaforos).setChecked(show_semaforos);
        menu.findItem(R.id.mode_map).setChecked(current_mode==MAP);
        menu.findItem(R.id.mode_freehand).setChecked(current_mode==FREEHAND);
        menu.findItem(R.id.mode_vehicles).setChecked(current_mode==VEHICLES);
        int z = ((MapView) findViewById(R.id.map)).getZoomLevel();
        menu.findItem(R.id.mode_freehand).setEnabled(z>19);
        menu.findItem(R.id.mode_vehicles).setEnabled(z>19);
        menu.findItem(R.id.tombar_veiculo).setVisible(getSelectedVehicle()!=null);
        menu.findItem(R.id.reset_veiculo).setVisible(current_mode!=MAP);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.center_here:
                MapView map = (MapView) findViewById(R.id.map);
                JSONObject point = ((Iat) getApplicationContext()).getLastKnownPosition();
                if (point.has("latitude")) {
                    map.getController().setCenter(new GeoPoint(point.optDouble("latitude"), point.optDouble("longitude")));
                }
                break;
            case R.id.labels:
                map = (MapView) findViewById(R.id.map);
                if (item.isChecked()) {
                    item.setChecked(false);
                    show_labels = false;
                } else {
                    item.setChecked(true);
                    show_labels = true;
                }
                setLabels(show_labels);
                break;
            case R.id.semaforos:
                if (item.isChecked()) {
                    item.setChecked(false);
                    show_semaforos = false;
                } else {
                    item.setChecked(true);
                    show_semaforos = true;
                }
                setTiles(show_semaforos);
                break;
            case R.id.mode_map:
                ((RadioButton)findViewById(R.id.radio_mapa)).setChecked(true);
                break;
            case R.id.mode_freehand:
                setCurrentMode(FREEHAND);
                break;
            case R.id.mode_vehicles:
                ((RadioButton)findViewById(R.id.radio_desenho)).setChecked(true);
                break;
            case R.id.tombar_veiculo:
                View v = ((CsiActivity) context).getSelectedVehicle();
                if (v != null) {
                    //v.vira();
                }
                break;
            case R.id.reset_veiculo:
                resetVehicles();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setTiles(boolean show_semaforos) {
        MapView map = (MapView) findViewById(R.id.map);
        map.setTileSource((show_semaforos)?great_source:clear_source);
    }

    private void ligaCarros(boolean b) {
        int z=((MapView)findViewById(R.id.map)).getZoomLevel();
        ViewGroup cv = (ViewGroup) findViewById(R.id.vehicles_canvas);
        for(int i=0;i<cv.getChildCount();i++){
            View car = cv.getChildAt(i);
            if(car.getClass().getCanonicalName().equals(VehicleFix.class.getCanonicalName())){
                ((VehicleFix)car).liga(b);
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

    public void updatePegadorForSelectedVehicle() {
        Pega pegador=(Pega)findViewById(R.id.pegador);

        VehicleFix fu = (VehicleFix)getSelectedVehicle();
        if(fu!=null) {
            //pegador.findViewById(R.id.rod).setRotation(view.getRotation());
            View bode = fu.findViewById(R.id.vehicle_chassi);
            pegador.setPontaPosition(bode.getX()+bode.getWidth()/2, bode.getY()+bode.getHeight()/2, fu.findViewById(R.id.vehicle_body).getRotation());
            pegador.invalidate();
        }

    }

    public void updateVehiclePosition(Pega l, float[] ponta) {
        View r = getSelectedVehicle();
        if(r==null) return;
        LinearLayout body = (LinearLayout) r.findViewById(R.id.vehicle_body);
        LinearLayout chassi = (LinearLayout) r.findViewById(R.id.vehicle_chassi);
        if(body==null)return;
        float[] ce = {l.getX(),l.getY()};
        //Log.d("IAT","POSICAO: "+body.getX()+", "+body.getY()+" - ponta pegada "+ponta[0]+" "+ponta[1]+" centro "+ce[0]+" "+ce[1]);
        body.setRotation(l.getRodRotation());
        chassi.setX(ponta[0]-convertDpToPixel(40));
        chassi.setY(ponta[1]-convertDpToPixel(40));
        body.invalidate();
    }
    public void setSelectedVehicle(View v) {
        setSelectedVehicle(v,false);
    }

    public void setSelectedVehicle(View sv, boolean reset) {
        Log.d("IAT", "tentando selecionar o veiciulo");
        ViewGroup canvas= (ViewGroup) findViewById(R.id.vehicles_canvas);
        for(int i=0;i<canvas.getChildCount();i++) ((VehicleFix) canvas.getChildAt(i)).setSelectedVehicle(false);
        selectedVehicle = sv;
        Pega pegador = (Pega) findViewById(R.id.pegador);
        if(sv!=null) {
            setCurrentMode(VEHICLES);
            ((VehicleFix)sv).setSelectedVehicle(true);
            Log.d("IAT", "tentando pegar o pegador");
            pegador.setVisibility(View.VISIBLE);
            View bod = sv.findViewById(R.id.vehicle_body);
            View chassi = sv.findViewById(R.id.vehicle_chassi);
            float pix = convertDpToPixel(40);

            if(!reset) {
                Log.d("IAT", "tentando resetar o pegador");
                pegador.setPontaPosition(chassi.getX() + pix, chassi.getY() + pix, bod.getRotation());
            }else{
                Point size = getDisplaySize();
                pegador.setPontaPosition(size.x/2,size.y/2, 0);
            }
        }else{
            Log.d("IAT", "nada a fazer aqui");
            pegador.setVisibility(View.GONE);
        }
        Log.d("IAT","selecionouy o veiculo");
        ((Panel) findViewById(R.id.drawing_panel)).setLigado(false);
    }

    public View getSelectedVehicle() {
        return selectedVehicle;
    }

    public Point getDisplaySize() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size;
    }

    public void setCurrentMode(int mode) {
        if(mode==current_mode)return;
        current_mode=mode;
        switch (mode){
            case VEHICLES:
                ((Panel) findViewById(R.id.drawing_panel)).setLigado(false);
                findViewById(R.id.show_pallette).setVisibility(View.VISIBLE);
                findViewById(R.id.vehicles_canvas).setVisibility(View.VISIBLE);
                Log.d("IAT","will reload");
                reloadVehicles();
                ligaCarros(true);
                break;
            case FREEHAND:
                saveVehicles();
                findViewById(R.id.show_pallette).setVisibility(View.GONE);
                ((Panel) findViewById(R.id.drawing_panel)).setLigado(true);
                findViewById(R.id.vehicles_canvas).setVisibility(View.VISIBLE);
                reloadVehicles();
                ligaCarros(true);
                setSelectedVehicle(null);
                break;
            case MAP:
                ((Panel) findViewById(R.id.drawing_panel)).setLigado(false);
                savePaths((Panel) findViewById(R.id.drawing_panel));
                ((MapView)findViewById(R.id.map)).setBuiltInZoomControls(true);
                saveVehicles();
                break;
        }

    }

    private void savePaths(Panel panel) {
        paths=panel.getJSONPaths();
        MapView map= (MapView) findViewById(R.id.map);
        try {
            for(int i=0;i<paths.length();i++){
                JSONArray pts = paths.getJSONObject(i).optJSONArray("points");
                for(int j=0;j<pts.length();j++){
                    IGeoPoint latlng = map.getProjection().fromPixels(
                            (int) pts.getJSONArray(j).getDouble(0),
                            (int) pts.getJSONArray(j).getDouble(1)
                    );
                    pts.put(j,latlng);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public class GeoServerTileSource extends OnlineTileSourceBase {
        private final String[] base_url;
        private final String layer;

        public GeoServerTileSource(String aName, int aZoomMinLevel, int aZoomMaxLevel, int aTileSizePixels, String aImageFilenameEnding, String[] aBaseUrl) {
            super(aName, aZoomMinLevel, aZoomMaxLevel, aTileSizePixels, aImageFilenameEnding, aBaseUrl);
            base_url = aBaseUrl;
            layer=aName;
        }
        @Override
        public String getTileURLString(MapTile aTile) {
            String u = "http://bigrs.alien9.net:8080/geoserver/gwc/service/tms/1.0.0/BIGRS%3A"+layer+"@3857@png" + "/" + aTile.getZoomLevel() + "/" + aTile.getX() + "/" + (int)(Math.pow(2.0,aTile.getZoomLevel())-aTile.getY()-1) + ".png";
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
            MapView map = (MapView) findViewById(R.id.map);
            if (vehicles_zooming_over != null) {
                map.getOverlays().remove(vehicles_zooming_over);
                vehicles_zooming_over = null;
                map.invalidate();
            }
            ViewGroup o = ((ViewGroup) findViewById(R.id.vehicles_canvas));
            Bitmap bi = o.getDrawingCache().copy(o.getDrawingCache().getConfig(),true);
            if (bi == null) return;
            bi.compress(Bitmap.CompressFormat.PNG, 95, new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/vehicle_screen.png"));
            for (int i = 0; i < o.getChildCount(); i++) {
                JSONObject veiculo = vehicles.optJSONObject(i);
                if (veiculo == null) {
                    veiculo = new JSONObject();
                }
                VehicleFix vw = (VehicleFix) o.getChildAt(i);
                JSONObject position = vw.getPosition();
                View bode = vw.findViewById(R.id.vehicle_body);
                View chassi = vw.findViewById(R.id.vehicle_chassi);
                float pix = convertDpToPixel(40);
                IGeoPoint latlng = map.getProjection().fromPixels(
                        Math.round(chassi.getX()+pix), Math.round(chassi.getY()+pix)
                );
                veiculo.put("latitude", latlng.getLatitude());
                veiculo.put("longitude", latlng.getLongitude());
                veiculo.put("position", position);
                veiculo.put("roll", vw.getRoll());
                veiculo.put("rotation",vw.getRotation());
                vehicles.put(i, veiculo);
            }
            //o.removeAllViews();
            o.setVisibility(View.GONE);
            BoundingBox b = map.getBoundingBox();
            Double[] p1 = degrees2meters(b.getLonEast(), b.getLatSouth());
            Double[] p2 = degrees2meters(b.getLonWest(), b.getLatNorth());
            Double[] p3 = degrees2meters(b.getLonWest(), b.getLatSouth());
            vehicles_zooming_over = new CsiGroundOverlay().setBounds(b);
            IGeoPoint position = map.getMapCenter();
            vehicles_zooming_over.setImage(new BitmapDrawable(getResources(), bi));
            vehicles_zooming_over.setPosition((GeoPoint) position);
            map.getOverlays().add(vehicles_zooming_over);

            View p = findViewById(R.id.drawing_panel);
            Bitmap bii = p.getDrawingCache().copy(p.getDrawingCache().getConfig(),true);
            if (bii == null) return;
            ((Panel)p).reset();
            bii.compress(Bitmap.CompressFormat.PNG, 95, new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/drawing_screen.png"));
            drawing_zooming_over = new CsiGroundOverlay().setBounds(b);
            drawing_zooming_over.setImage(new BitmapDrawable(getResources(), bii));
            drawing_zooming_over.setPosition((GeoPoint) position);
            map.getOverlays().add(drawing_zooming_over);
            map.invalidate();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ((CsiActivity) context).setSelectedVehicle(null);
    }
    protected void createVehicle(int model, double width, double length){
        //if(current_mode!=VEHICLES) setCurrentMode(VEHICLES);
        ((RadioButton)findViewById(R.id.radio_desenho)).setChecked(true);
        MapView map = (MapView) findViewById(R.id.map);
        BoundingBox b = map.getBoundingBox();
        float[] results = new float[1];
        Location.distanceBetween(b.getLatNorth(),b.getLonWest(),b.getLatSouth(),b.getLonEast(),results);
        Point size = getDisplaySize();
        double diagonal = Math.sqrt(Math.pow(size.x,2.0) + Math.pow(size.y,2.0));
        double pixels_per_m = diagonal / results[0];
        int w = (int) (width * pixels_per_m);
        int l = (int) (length * pixels_per_m);
        View v=new VehicleFix(context);
        ((ViewGroup) findViewById(R.id.vehicles_canvas)).addView(v);
        ((VehicleFix)v).zinit(model,0);
        RelativeLayout.LayoutParams rparams = new RelativeLayout.LayoutParams(10*Math.round(w),10*Math.round(l));
        Log.d("IAT QUEBRADO ","mais");
        View chassis = v.findViewById(R.id.vehicle_chassi);
        float pix = convertDpToPixel(80);
        chassis.setY((size.y-pix)/2);
        chassis.setX((size.x-pix)/2);
        LinearLayout body = (LinearLayout) v.findViewById(R.id.vehicle_body);
        Log.d("IAT QUEBRADO ","até aquii veio");
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) Math.round(w), (int) Math.round(l));
        body.setLayoutParams(params);
        Log.d("IAT QUEBRADO ","opa");
        JSONObject veiculo = new JSONObject();
        IGeoPoint center = map.getMapCenter();
        try {
            veiculo.put("model", model);
            veiculo.put("width", width);
            veiculo.put("length", length);
            veiculo.put("latitude", center.getLatitude());
            veiculo.put("longitude", center.getLongitude());
            veiculo.put("position", ((VehicleFix) v).getPosition());
            veiculo.put("roll", 0);
            veiculo.put("rotation",0.0);
        } catch (JSONException e) {}
        vehicles.put(veiculo);
        setSelectedVehicle(v, true);
        v.invalidate();
    }

    protected void reloadVehicles() {
        ((Iat)getApplicationContext()).setSelectedVehicle(null);
        ViewGroup o = ((ViewGroup) findViewById(R.id.vehicles_canvas));
        MapView map = (MapView) findViewById(R.id.map);
        if(vehicles_zooming_over!=null){
            map.getOverlays().remove(vehicles_zooming_over);
            map.invalidate();
            vehicles_zooming_over=null;
        }
        BoundingBox b = map.getBoundingBox();
        float[] results = new float[1];
        Location.distanceBetween(b.getLatNorth(),b.getLonWest(),b.getLatSouth(),b.getLonEast(),results);
        Point size = getDisplaySize();
        double diagonal = Math.sqrt(Math.pow(size.x,2.0) + Math.pow(size.y,2.0));
        double pixels_per_m = diagonal / results[0];
        for (int i = 0; i < vehicles.length(); i++) {
            int w = (int) (vehicles.optJSONObject(i).optDouble("width") * pixels_per_m);//1000 * Math.pow(2.0, map.getZoomLevel()) / (2 * 20037508.34));
            int l = (int) (vehicles.optJSONObject(i).optDouble("length") * pixels_per_m);//1000 * Math.pow(2.0, map.getZoomLevel()) / (2 * 20037508.34));
            View v=((ViewGroup) findViewById(R.id.vehicles_canvas)).getChildAt(i);
            Point position = new Point();
            map.getProjection().toPixels(new GeoPoint(vehicles.optJSONObject(i).optDouble("latitude"),vehicles.optJSONObject(i).optDouble("longitude")),position);
            View body = v.findViewById(R.id.vehicle_body);
            View chassi = v.findViewById(R.id.vehicle_chassi);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) Math.round(w), (int) Math.round(l));
            body.setLayoutParams(params);
            float pix = convertDpToPixel(40);
            if(position!=null) {
                chassi.setX(position.x-pix);
                chassi.setY(position.y-pix);
            }
            body.setRotation((float) vehicles.optJSONObject(i).optDouble("rotation"));
            setSelectedVehicle(v);
            body.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setSelectedVehicle((View) view.getParent());
                }
            });
            v.invalidate();
        }
    }
    protected void resetVehicles(){
        ((CsiActivity)context).setSelectedVehicle(null);
        for (int i = 0; i < vehicles.length(); i++) {
            JSONObject v = vehicles.optJSONObject(i);
            v.remove("latitude");
            v.remove("longitude");
            v.remove("position");
            v.remove("roll");
        }
        reloadVehicles();
    }
    public void exlog(String text, int line){
        mess[line]=text;
        String fim="";
        for(int i=0;i<mess.length;i++){
            fim+="\n"+mess[i];
        }
        ((TextView)findViewById(R.id.messageria)).setText(fim);
    }
    public float convertDpToPixel(float dp){
        Resources resources = getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float px = dp * ((float)metrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        return px;
    }

}