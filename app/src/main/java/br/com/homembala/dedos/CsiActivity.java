package br.com.homembala.dedos;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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

import static br.com.homembala.dedos.R.id.map;

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
    private JSONArray paths=new JSONArray();

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
        great_source = new GeoServerTileSource("Cidade+com+Sem%C3%A1foros%20e%20Lotes", 17, 21, 512, ".png", new String[]{u});
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
                MapView map = (MapView) findViewById(R.id.map);
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
                if(current_mode==FREEHAND)
                    return false;
                if(motionEvent.getAction()==MotionEvent.ACTION_DOWN){
                    if(current_mode!=VEHICLES) {
                        return true;
                    }
                }
                View v = getSelectedVehicle();
                if(v==null){
                    return true;
                }else {
                    setSelectedVehicle(null);
                }
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
                        createVehicle(VehicleFix.PEDESTRE,1.7,2.5);
                        break;
                    case R.id.tools_colisao:
                        createVehicle(VehicleFix.COLISAO,4.0,4.0);
                        break;
                    case R.id.tools_obstaculo:
                        plot(R.layout.prompt_obstaculo);
                        break;
                    case R.id.tools_freada:
                        startDraw(Panel.SKID);
                        break;
                    case R.id.tools_zebra:
                        startDraw(Panel.ZEBRA);
                        break;
                    case R.id.tools_trajetoria:
                        startDraw(Panel.TRACK);
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
        findViewById(R.id.map_block).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });
        //detailPagerSetup();
    }

    public void detailPagerSetup(int vehicle_id) {
        findViewById(R.id.vehicle_details).setVisibility(View.VISIBLE);
        ((ViewPager)findViewById(R.id.vehicle_details)).setAdapter(new VehicleDetailsAdapter(context));
        ((ViewPager)findViewById(R.id.vehicle_details)).setCurrentItem(getVehicleIndexById(vehicle_id));
        findViewById(R.id.vehicle_details).invalidate();

    }
    public void detailPagerSetup() {
        findViewById(R.id.vehicle_details).setVisibility(View.VISIBLE);
        ((ViewPager)findViewById(R.id.vehicle_details)).setAdapter(new VehicleDetailsAdapter(context));
        ((ViewPager)findViewById(R.id.vehicle_details)).setCurrentItem(getVehicleIndexById(((VehicleFix)getSelectedVehicle()).getVehicleId()));
        findViewById(R.id.vehicle_details).invalidate();
    }

    private void startDraw(int skid) {
        ((RadioButton)findViewById(R.id.radio_desenho)).setChecked(true);
        setCurrentMode(FREEHAND);
        findViewById(R.id.show_pallette).setVisibility(View.GONE);
        findViewById(R.id.drawing_panel).setVisibility(View.VISIBLE);
        ((Panel)findViewById(R.id.drawing_panel)).setStyle(skid,getResolution());
        ((Panel)findViewById(R.id.drawing_panel)).setLigado(true);
        ((Panel)findViewById(R.id.drawing_panel)).setVisibility(View.VISIBLE);
    }

    private void plot(int t) {
        final int tipo=t;
        LayoutInflater inflater = this.getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Detalhes");
        final View v=inflater.inflate(tipo, null);
        builder.setView(v);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which){
                switch(tipo){
                    case R.layout.prompt_obstaculo:
                        try {
                            int largura = Integer.parseInt(((TextView) v.findViewById(R.id.largura_text)).getText().toString());
                            int comprimento = Integer.parseInt(((TextView) v.findViewById(R.id.comprimento_text)).getText().toString());
                            if (largura > 0 && comprimento > 0) {
                                createVehicle(VehicleFix.OBSTACULO, largura, comprimento);
                            }
                        }catch(NumberFormatException xxx){
                            return;
                        }
                        break;
                }
                // Handle click on positive button here.
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which){
                dialog.dismiss();
            }
        });
        builder.create().show();
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
        if(findViewById(R.id.vehicle_details).getVisibility()==View.VISIBLE)
            findViewById(R.id.vehicle_details).setVisibility(View.GONE);
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
        int z = ((MapView) findViewById(map)).getZoomLevel();
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
        int z=((MapView)findViewById(map)).getZoomLevel();
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
        //float[] ce = {l.getX(),l.getY()};
        //Log.d("IAT","POSICAO: "+body.getX()+", "+body.getY()+" - ponta pegada "+ponta[0]+" "+ponta[1]+" centro "+ce[0]+" "+ce[1]);
        body.setRotation(l.getRodRotation());
        chassi.setX(ponta[0]-convertDpToPixel(150));
        chassi.setY(ponta[1]-convertDpToPixel(150));
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
            float pix = convertDpToPixel(150);
            if(!reset) {
                Log.d("IAT", "tentando resetar o pegador");
                pegador.setPontaPosition(chassi.getX() + pix, chassi.getY() + pix, bod.getRotation());
            }else{
                Point size = getDisplaySize();
                pegador.setPontaPosition(size.x/2,size.y/2, 0);
            }
            sv.bringToFront();
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
        MapView map = (MapView) findViewById(R.id.map);
        map.setEnabled(false);
        Panel panel = (Panel) findViewById(R.id.drawing_panel);
        switch (mode){
            case VEHICLES:
                findViewById(R.id.map_block).setVisibility(View.VISIBLE);
                panel.setLigado(false);
                findViewById(R.id.show_pallette).setVisibility(View.VISIBLE);
                findViewById(R.id.vehicles_canvas).setVisibility(View.VISIBLE);
                Log.d("IAT","will reload");
                reloadVehiclesAndPaths();
                ligaCarros(true);
                break;
            case FREEHAND:
                findViewById(R.id.map_block).setVisibility(View.VISIBLE);
                saveVehiclesAndPaths();
                //map.getController().setZoom(20);
                findViewById(R.id.show_pallette).setVisibility(View.GONE);
                findViewById(R.id.vehicles_canvas).setVisibility(View.VISIBLE);
                panel.setLigado(true);

                reloadVehiclesAndPaths();
                ligaCarros(true);
                setSelectedVehicle(null);
                break;
            case MAP:
                findViewById(R.id.map_block).setVisibility(View.GONE);
                ((Panel) findViewById(R.id.drawing_panel)).setLigado(false);
                saveVehiclesAndPaths();
                map.setBuiltInZoomControls(true);
                map.setEnabled(true);
                setSelectedVehicle(null);
                break;
        }

    }

    private void savePaths(Panel panel) {
        paths=panel.getJSONPaths();
        MapView map= (MapView) findViewById(R.id.map);
        try {
            for(int i=0;i<paths.length();i++){
                if(!paths.getJSONObject(i).has("geom")) {
                    JSONArray pts = paths.getJSONObject(i).optJSONArray("points");
                    JSONArray points = new JSONArray();
                    for (int j = 0; j < pts.length(); j++) {
                        JSONArray pt = pts.getJSONArray(j);
                        IGeoPoint latlng = map.getProjection().fromPixels(
                                (int) pt.getDouble(0),
                                (int) pt.getDouble(1)
                        );
                        JSONObject g=new JSONObject();
                        g.put("latitude",latlng.getLatitude());
                        g.put("longitude",latlng.getLongitude());
                        points.put(j, g);
                    }
                    paths.getJSONObject(i).put("geom", points);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadPaths(Panel panel){
        MapView map= (MapView) findViewById(R.id.map);
        if(drawing_zooming_over!=null) {
            map.getOverlays().remove(drawing_zooming_over);
            map.invalidate();
            drawing_zooming_over = null;
            try {
                for (int i = 0; i < paths.length(); i++) {
                    JSONArray pontos = new JSONArray();
                    JSONArray points = paths.optJSONObject(i).optJSONArray("geom");
                    for (int j = 0; j < points.length(); j++) {
                        JSONObject pt = points.optJSONObject(j);
                        GeoPoint g = new GeoPoint(pt.optDouble("latitude"), pt.optDouble("longitude"));
                        Point px = new Point();
                        map.getProjection().toPixels(g, px);
                        JSONArray ponto = new JSONArray();
                        ponto.put(px.x);
                        ponto.put(px.y);
                        pontos.put(ponto);
                    }
                    paths.optJSONObject(i).put("points", pontos);
                }
                panel.setJSONPaths(paths,getResolution());
            } catch (JSONException e) {
                e.printStackTrace();
            }
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

    protected void saveVehiclesAndPaths() {
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
            JSONArray vs=new JSONArray();
            for (int i = 0; i < o.getChildCount(); i++) {
                VehicleFix vw = (VehicleFix) o.getChildAt(i);
                JSONObject veiculo = getVehicleById(vw.getVehicleId());
                if (veiculo == null) {
                    veiculo = new JSONObject();
                }
                JSONObject position = vw.getPosition();
                View bode = vw.findViewById(R.id.vehicle_body);
                View chassi = vw.findViewById(R.id.vehicle_chassi);
                float pix = convertDpToPixel(150);
                IGeoPoint latlng = map.getProjection().fromPixels(
                        Math.round(chassi.getX()+pix), Math.round(chassi.getY()+pix)
                );
                veiculo.put("latitude", latlng.getLatitude());
                veiculo.put("longitude", latlng.getLongitude());
                veiculo.put("position", position);
                veiculo.put("roll", vw.getRoll());
                veiculo.put("rotation",vw.getRotation());
                vs.put(veiculo);
            }
            vehicles=vs;
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
            if (drawing_zooming_over!= null) {
                map.getOverlays().remove(drawing_zooming_over);
                drawing_zooming_over = null;
                map.invalidate();
            }
            View p = findViewById(R.id.drawing_panel);
            savePaths((Panel) p);
            Bitmap bii = p.getDrawingCache().copy(p.getDrawingCache().getConfig(), true);
            if (bii == null) return;
            ((Panel) p).reset();
            bii.compress(Bitmap.CompressFormat.PNG, 95, new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/drawing_screen.png"));
            drawing_zooming_over = new CsiGroundOverlay().setBounds(b);
            drawing_zooming_over.setImage(new BitmapDrawable(getResources(), bii));
            drawing_zooming_over.setPosition((GeoPoint) position);
            map.getOverlays().add(drawing_zooming_over);
            map.getOverlays().add(vehicles_zooming_over);
            map.invalidate();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ((CsiActivity) context).setSelectedVehicle(null);
    }

    private JSONObject getVehicleById(int id) {
        JSONObject j=null;
        for(int i=0;i<vehicles.length();i++){
            JSONObject v = vehicles.optJSONObject(i);
            if(v.optInt("view_id")==id) j=v;
        }
        return j;
    }
    private int getVehicleIndexById(int id) {
        int ind = 0;
        for(int i=0;i<vehicles.length();i++){
            JSONObject v = vehicles.optJSONObject(i);
            if(v.optInt("view_id")==id) ind=i;
        }
        return ind;
    }



    protected void createVehicle(int model, double width, double length){
        if(current_mode!=VEHICLES){
            ((RadioButton)findViewById(R.id.radio_desenho)).setChecked(true);
            //setCurrentMode(VEHICLES);
        }
        ((RadioButton)findViewById(R.id.radio_desenho)).setChecked(true);
        MapView map = (MapView) findViewById(R.id.map);
        Point size = getDisplaySize();
        double pixels_per_m = getResolution();
        int w = (int) (width * pixels_per_m);
        int l = (int) (length * pixels_per_m);
        View v=new VehicleFix(context);
        ((ViewGroup) findViewById(R.id.vehicles_canvas)).addView(v);
        ((VehicleFix)v).zinit(model,0);
        RelativeLayout.LayoutParams rparams = new RelativeLayout.LayoutParams(10*Math.round(w),10*Math.round(l));
        Log.d("IAT QUEBRADO ","mais");
        View chassis = v.findViewById(R.id.vehicle_chassi);
        float pix = convertDpToPixel(300);
        chassis.setY((size.y-pix)/2);
        chassis.setX((size.x-pix)/2);
        LinearLayout body = (LinearLayout) v.findViewById(R.id.vehicle_body);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) Math.round(w), (int) Math.round(l));
        body.setLayoutParams(params);
        JSONObject veiculo = new JSONObject();
        IGeoPoint center = map.getMapCenter();
        int vid=vehicles.length()+1;
        ((VehicleFix)v).setVehicleId(vid);
        try {
            veiculo.put("view_id",vid);
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

    private double getResolution() {
        MapView map = (MapView) findViewById(R.id.map);
        BoundingBox b = map.getBoundingBox();
        float[] results = new float[1];
        Location.distanceBetween(b.getLatNorth(),b.getLonWest(),b.getLatSouth(),b.getLonEast(),results);
        Point size = getDisplaySize();
        double diagonal = Math.sqrt(Math.pow(size.x,2.0) + Math.pow(size.y,2.0));
        return diagonal / results[0];
    }

    protected void reloadVehiclesAndPaths() {
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
        //for (int i = 0; i < vehicles.length(); i++) {
        for (int i = 0; i < ((ViewGroup) findViewById(R.id.vehicles_canvas)).getChildCount(); i++) {
            View v=((ViewGroup) findViewById(R.id.vehicles_canvas)).getChildAt(i);
            int vehicle_id=((VehicleFix) v).getVehicleId();
            JSONObject vehicle = getVehicleById(vehicle_id);
            if(vehicle==null){
                Log.d("","");
            }
            int w = (int) (vehicle.optDouble("width") * pixels_per_m);//1000 * Math.pow(2.0, map.getZoomLevel()) / (2 * 20037508.34));
            int l = (int) (vehicle.optDouble("length") * pixels_per_m);//1000 * Math.pow(2.0, map.getZoomLevel()) / (2 * 20037508.34));
            //int vid=vehicle.optInt("view_id");
            //
            //View v=((ViewGroup) findViewById(R.id.vehicles_canvas)).findViewById(vid);
            Point position = new Point();
            map.getProjection().toPixels(new GeoPoint(vehicle.optDouble("latitude"),vehicle.optDouble("longitude")),position);
            View body = v.findViewById(R.id.vehicle_body);
            View chassi = v.findViewById(R.id.vehicle_chassi);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) Math.round(w), (int) Math.round(l));
            body.setLayoutParams(params);
            float pix = convertDpToPixel(150);
            if(position!=null) {
                chassi.setX(position.x-pix);
                chassi.setY(position.y-pix);
            }
            body.setRotation((float) vehicle.optDouble("rotation"));
            body.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    setSelectedVehicle((View) view.getParent().getParent().getParent());
                }
            });
            v.invalidate();
        }
        loadPaths((Panel)findViewById(R.id.drawing_panel));
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
        ((ViewGroup) findViewById(R.id.vehicles_canvas)).removeAllViews();
        reloadVehiclesAndPaths();
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

    private class VehicleDetailsAdapter extends PagerAdapter {
        private final Context mcontext;

        @Override
        public int getCount() {
            return vehicles.length();
        }
        public VehicleDetailsAdapter(Context c) {
            mcontext = c;
        }
        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view==object;
        }
        @Override
        public Object instantiateItem(ViewGroup collection, final int position) {
            //ModelObject modelObject = ModelObject.values()[position];
            LayoutInflater inflater = LayoutInflater.from(mcontext);
            ViewGroup layout = null;
            JSONObject vehicle = vehicles.optJSONObject(position);
            switch(vehicle.optInt("model")){
                case VehicleFix.CARRO:
                case VehicleFix.CAMINHAO:
                case VehicleFix.ONIBUS:
                case VehicleFix.MOTO:
                    layout = (ViewGroup) inflater.inflate(R.layout.vehicle_data, collection, false);
                    ((EditText)layout.findViewById(R.id.placa_text)).setText(vehicle.optString("placa"));
                    ((EditText)layout.findViewById(R.id.marca_text)).setText(vehicle.optString("marca"));
                    break;
                case VehicleFix.PEDESTRE:
                    layout=(ViewGroup) inflater.inflate(R.layout.pedestre_data, collection, false);
                    ((EditText)layout.findViewById(R.id.nome_text)).setText(vehicle.optString("nome"));
                    ((EditText)layout.findViewById(R.id.idade_text)).setText(vehicle.optString("idade"));
                    break;
            }

            final ViewGroup finalLayout = layout;
            layout.findViewById(R.id.voltar_butt).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        JSONObject vehicle = vehicles.optJSONObject(position);
                        switch (vehicle.optInt("model")) {
                            case VehicleFix.CARRO:
                            case VehicleFix.CAMINHAO:
                            case VehicleFix.ONIBUS:
                            case VehicleFix.MOTO:
                                vehicles.optJSONObject(position).put("placa", ((EditText) finalLayout.findViewById(R.id.placa_text)).getText());
                                vehicles.optJSONObject(position).put("marca", ((EditText) finalLayout.findViewById(R.id.marca_text)).getText());
                                break;
                            case VehicleFix.PEDESTRE:
                                vehicles.optJSONObject(position).put("nome", ((EditText) finalLayout.findViewById(R.id.nome_text)).getText());
                                vehicles.optJSONObject(position).put("idade", ((EditText) finalLayout.findViewById(R.id.idade_text)).getText());
                                break;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    findViewById(R.id.vehicle_details).setVisibility(View.GONE);
                    View v = getCurrentFocus();
                    if (v != null) {
                        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    }
                }
            });
            collection.addView(layout);
            return layout;
        }
        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
        }
    }

}