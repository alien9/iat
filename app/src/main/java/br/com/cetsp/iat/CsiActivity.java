package br.com.cetsp.iat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Base64;
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
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import org.apache.commons.lang3.ArrayUtils;

import br.com.cetsp.iat.util.VehicleFix;

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
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.TilesOverlay;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.com.cetsp.iat.util.Pega;

import jsqlite.Constants;
import jsqlite.Database;
import jsqlite.Exception;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static br.com.cetsp.iat.util.VehicleFix.AUTO;
import static br.com.cetsp.iat.util.VehicleFix.BICI;
import static br.com.cetsp.iat.util.VehicleFix.CAMINHAO;
import static br.com.cetsp.iat.util.VehicleFix.CAMINHONETE;
import static br.com.cetsp.iat.util.VehicleFix.CAMIONETA;
import static br.com.cetsp.iat.util.VehicleFix.CARROCA;
import static br.com.cetsp.iat.util.VehicleFix.MICROONIBUS;
import static br.com.cetsp.iat.util.VehicleFix.ONIBUS;
import static br.com.cetsp.iat.util.VehicleFix.TAXI;
import static br.com.cetsp.iat.util.VehicleFix.VIATURA;

/**
 * Created by tiago on 27/03/17.
 */
public class CsiActivity extends AppCompatActivity {
    private static final double MAP_SIZE = 20037508.34789244 * 2;
    private static final double[] TILE_ORIGIN = {-20037508.34789244,20037508.34789244};
    private static final int LABEL_SIZE = 20;
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
    private View selectedVehicle;
    private String[] mess;
    private JSONArray paths=new JSONArray();
    private int croqui_size;
    private boolean gps;
    private Hashtable<String,ArrayList> modelos;
    private ArrayList<String> todos;
    private int[] labelOffset;
    private ViewTreeObserver.OnGlobalLayoutListener vehicleLoader;
    private CharSequence[] placas;
    private AlertDialog datetime_picker;
    private AlertDialog alert;
    private CsiGroundOverlay local_map_overlay;
    private int localmap_updates;
    private boolean isMoving;
    private SpatialTileSource local_source;
    //private List<String> placas;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Iat iat = (Iat) getApplicationContext();
        /*
        if(!iat.isAuthenticated()){
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
            return;
        }
        */
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)||(ActivityCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)) {
            Intent intent=new Intent(this,PermissionRequest.class);
            Bundle b=getIntent().getExtras();
            if(b!=null)
                intent.putExtras(b);
            startActivity(intent);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            iat.startGPS(this);
        }
        setContentView(R.layout.csi);

        findViewById(R.id.info_box).setVisibility(View.GONE);
        float ls = convertDpToPixel(LABEL_SIZE);
        labelOffset=new int[]{
                //(int) (-1*ls/2), (int) ls
                (int) (-1*ls/2), (int) convertDpToPixel(50)
        };
        //findViewById(R.id.messageria).setVisibility(View.GONE);
        mess=new String[]{"","","","","","","","","","","","","","",""};
        context=this;
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        Bundle bundle = getIntent().getExtras();

        if(bundle!=null){
            if(bundle.containsKey("starter")){
                iat.setStarter(bundle.getString("starter"));
            }
        }
        //((Panel) findViewById(R.id.drawing_panel)).setVisibility(View.GONE);
        findViewById(R.id.vehicles_canvas).setDrawingCacheEnabled(true);
        final MapView map = (MapView) findViewById(R.id.map);
        map.setTileSource(TileSourceFactory.USGS_SAT);
        map.setTilesScaledToDpi(false);
        String u="http://bigrs.alien9.net:8080/geoserver/gwc/service/tms/1.0.0/";
        //clear_source = new GeoServerTileSource("quadras_e_logradouros", 17, 21, 512, ".png", new String[]{u});
        great_source = new GeoServerTileSource("cidade_com_semaforos_e_lotes", 17, 21, 512, ".png", new String[]{u});
        map.setTileSource(great_source);
/* local mapa - bugfix
        local_source = new SpatialTileSource("cidade_local", 17, 21, 512, ".png");
        MapTileProviderBasic tileProvider=null;
        try {
            tileProvider = new MapTileProviderSpatial(getApplicationContext(),map);
            tileProvider.setTileSource(local_source);

        } catch (Exception e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        final TilesOverlay tilesOverlay = new TilesOverlay(tileProvider, this.getBaseContext());
        tilesOverlay.setLoadingBackgroundColor(Color.TRANSPARENT);
        map.getOverlays().add(tilesOverlay);
*/

        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);
        map.setClickable(true);
        map.setUseDataConnection(true);
        //(new KmlLoader(map)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        isMoving=false;
        ScaleBarOverlay sbo = new ScaleBarOverlay(map);
        sbo.setCentred(false);
        sbo.setScaleBarOffset(10, 10);
        map.getController().setZoom(20);
        map.setMaxZoomLevel((double) 21);
        map.setTilesScaledToDpi(true);
        map.getOverlays().add(sbo);
        overlays=new Hashtable<>();
        vehicles=new JSONArray();
        current_mode=999;
        Intent intent=getIntent();
        croqui_size=400;
        if(intent.hasExtra("size")){
            croqui_size=intent.getIntExtra("size",500);
        }
        JSONObject point=new JSONObject();
        if(intent.hasExtra("latitude") && intent.hasExtra("longitude")){
            Log.d("IAT", "Application with a parameter");
            try {
                Log.d("IAT", "latitude");
                String l=intent.getStringExtra("latitude");
                Log.d("IAT", "latitude é "+l);
                point.put("latitude",Double.parseDouble(intent.getStringExtra("latitude")));
                point.put("longitude",Double.parseDouble(intent.getStringExtra("longitude")));
            }catch (JSONException | NumberFormatException ignore) {
            }
        }
        gps=false;
        if(!point.has("latitude") || !point.has("longitude") ){
            gps=true;
            ((Iat) getApplicationContext()).startGPS(this);
            point = ((Iat) getApplicationContext()).getLastKnownPosition();
        }
        if(!point.has("latitude")){
            try {
                point.put("longitude",-46.625290);
                point.put("latitude",-23.533773);
            } catch (JSONException ignore) {}
        }
        placas=new String[]{};
        if(intent.hasExtra("placas")){
            placas= intent.getStringExtra("placas").split("[,\\s]+");
        }
        JSONArray existent_vehicles=new JSONArray();
        JSONArray existent_paths=new JSONArray();

        if(intent.hasExtra("info")){
            try{
                Log.d("IAT recebe parâmetros",intent.getStringExtra("info"));
                JSONObject j = new JSONObject(intent.getStringExtra("info"));
                existent_paths=j.optJSONArray("paths");
                if(j.has("latitude")){
                    point.put("latitude",j.optDouble("latitude"));
                }
                if(j.has("longitude")){
                    point.put("longitude",j.optDouble("longitude"));
                }
                if(j.has("longitude")&&j.has("latitude"))
                    map.getController().setCenter(new GeoPoint(point.optDouble("latitude"), point.optDouble("longitude")));
                if(j.has("vehicles"))
                    existent_vehicles = j.optJSONArray("vehicles");
            } catch (JSONException e) {}
        }
        map.getController().setCenter(new GeoPoint(point.optDouble("latitude"), point.optDouble("longitude")));
        map.addMapListener(new DelayedMapListener(new MapListener() {
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
                refresh();
                isMoving=false;
                return true;
            }
            public boolean onScroll(final ScrollEvent e) {
                if(show_labels){
                    updateLabels();
                }
                isMoving=false;
                Log.d("IAT DRAG", e.toString());
                return true;
            }
        }, 1000 ));
        map.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                isMoving=true;
                return false;
            }
        });
        is_updating_labels =false;
        update_labels_after =false;
        is_updating_closeup=false;
        localmap_updates=0;
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
        findViewById(R.id.cancel_review_butt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exitReview();
            }
        });
        findViewById(R.id.ok_review_butt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                exitReview();
                Intent data=new Intent();
                data.putExtra("data",collectData().toString());
                Log.d("IAT send result", "enviando croqui para o eGO");
                setResult(RESULT_OK, data);
                finish();
            }
        });
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
        findViewById(R.id.cancel_eraser).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopEraser();
            }
        });
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

        //if(vehicles.length()==0){
        //    ((RadioButton)findViewById(R.id.radio_mapa)).setChecked(true);
        //}
        findViewById(R.id.pegador).setVisibility(View.VISIBLE);
        ((ImageButton)findViewById(R.id.edit_vehicle_butt)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    int vid=Integer.parseInt(String.valueOf(((TextView)findViewById(R.id.vehicle_id_text)).getText()));
                    detailPagerSetup(vid);
                }catch(NumberFormatException exx){

                }

            }
        });
        ((ImageButton)findViewById(R.id.edit_vehicle_rotate)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((VehicleFix)getSelectedVehicle()).vira();
            }
        });
        ((ImageButton)findViewById(R.id.bt_delete)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ViewGroup c = (ViewGroup) findViewById(R.id.vehicles_canvas);
                View vu = getSelectedVehicle();
                int position=-1;
                for(int i=0;i<vehicles.length();i++){
                    JSONObject vehicle = vehicles.optJSONObject(i);
                    if(((VehicleFix)vu).getVehicleId()==vehicle.optInt("view_id")){
                        vu=c.getChildAt(i);
                        position=i;
                    }
                }
                if(position>=0) {
                    if (vu != null)
                        c.removeView(vu);
                    vehicles.remove(position);
                    c.invalidate();
                }
                setSelectedVehicle(null);
            }
        });

        /*
        try {
            paths=new JSONArray("[{\"style\":2,\"points\":[[298356683,-348306911],[298356681,-348306915],[298356672,-348306927],[298356667,-348306935],[298356669,-348306925],[298356675,-348306906],[298356695,-348306851],[298356715,-348306794],[298356755,-348306678],[298356771,-348306630],[298356784,-348306591],[298356784,-348306593],[298356784,-348306595],[298356782,-348306601],[298356780,-348306605],[298356779,-348306609],[298356779,-348306611],[298356771,-348306619]],\"geom\":[{\"latitude\":-23.597773513443997,\"longitude\":-46.62416920065879},{\"latitude\":-23.59777187483192,\"longitude\":-46.62417009472847},{\"latitude\":-23.597766958995564,\"longitude\":-46.62417411804199},{\"latitude\":-23.597763681771227,\"longitude\":-46.62417635321618},{\"latitude\":-23.597767778301616,\"longitude\":-46.6241754591465},{\"latitude\":-23.597775561709057,\"longitude\":-46.624172776937485},{\"latitude\":-23.597798092622696,\"longitude\":-46.62416383624076},{\"latitude\":-23.597821442838196,\"longitude\":-46.62415489554406},{\"latitude\":-23.597868962562174,\"longitude\":-46.62413701415061},{\"latitude\":-23.597888625891187,\"longitude\":-46.62412986159325},{\"latitude\":-23.597904602343846,\"longitude\":-46.62412405014039},{\"latitude\":-23.597903783038618,\"longitude\":-46.62412405014039},{\"latitude\":-23.59790296373339,\"longitude\":-46.62412405014039},{\"latitude\":-23.597900505817677,\"longitude\":-46.624124944210045},{\"latitude\":-23.59789886720722,\"longitude\":-46.624125838279724},{\"latitude\":-23.597897228596693,\"longitude\":-46.62412628531455},{\"latitude\":-23.59789640929145,\"longitude\":-46.62412628531455},{\"latitude\":-23.59789313207031,\"longitude\":-46.62412986159325}]},{\"style\":2,\"points\":[[298357025,-348307065],[298357026,-348307051],[298357031,-348307034],[298357051,-348306984],[298357063,-348306953],[298357088,-348306889],[298357099,-348306861],[298357117,-348306820],[298357126,-348306800],[298357129,-348306792],[298357133,-348306779],[298357134,-348306773],[298357139,-348306765],[298357144,-348306772]],\"geom\":[{\"latitude\":-23.59771042686424,\"longitude\":-46.62401631474494},{\"latitude\":-23.597716162009107,\"longitude\":-46.62401586771011},{\"latitude\":-23.59772312611328,\"longitude\":-46.62401363253593},{\"latitude\":-23.597743608770358,\"longitude\":-46.624004691839225},{\"latitude\":-23.597756308016173,\"longitude\":-46.623999327421195},{\"latitude\":-23.597782525810047,\"longitude\":-46.623988151550286},{\"latitude\":-23.597793996093216,\"longitude\":-46.623983234167106},{\"latitude\":-23.59781079186321,\"longitude\":-46.62397518754006},{\"latitude\":-23.597818984920977,\"longitude\":-46.62397116422654},{\"latitude\":-23.59782226214392,\"longitude\":-46.62396982312203},{\"latitude\":-23.597827587631073,\"longitude\":-46.623968034982674},{\"latitude\":-23.597830045548122,\"longitude\":-46.623967587947845},{\"latitude\":-23.597833322770796,\"longitude\":-46.62396535277366},{\"latitude\":-23.59783045520095,\"longitude\":-46.623963117599494}]},{\"style\":3,\"points\":[[298356243,-348306545],[298356237,-348306549],[298356219,-348306566],[298356210,-348306596],[298356208,-348306624],[298356210,-348306636],[298356217,-348306653],[298356232,-348306674],[298356244,-348306684],[298356277,-348306699],[298356316,-348306708],[298356349,-348306713],[298356366,-348306715],[298356396,-348306717],[298356420,-348306716],[298356429,-348306715],[298356443,-348306710],[298356447,-348306705],[298356442,-348306692],[298356434,-348306683],[298356412,-348306669],[298356385,-348306664],[298356354,-348306666],[298356326,-348306675],[298356308,-348306686],[298356302,-348306690],[298356297,-348306698],[298356298,-348306708],[298356312,-348306732],[298356340,-348306756],[298356361,-348306772],[298356376,-348306779],[298356412,-348306796],[298356456,-348306807],[298356495,-348306817],[298356541,-348306824],[298356583,-348306826],[298356616,-348306823],[298356638,-348306817],[298356656,-348306812],[298356664,-348306810],[298356677,-348306808],[298356686,-348306807],[298356693,-348306808],[298356699,-348306808],[298356702,-348306809],[298356707,-348306811],[298356715,-348306813]],\"geom\":[{\"latitude\":-23.59792344636243,\"longitude\":-46.62436589598656},{\"latitude\":-23.597921807752215,\"longitude\":-46.62436857819557},{\"latitude\":-23.59791484365863,\"longitude\":-46.62437662482262},{\"latitude\":-23.597902554080804,\"longitude\":-46.62438064813614},{\"latitude\":-23.5978910838071,\"longitude\":-46.62438154220582},{\"latitude\":-23.597886167975204,\"longitude\":-46.62438064813614},{\"latitude\":-23.597879203879742,\"longitude\":-46.624377518892295},{\"latitude\":-23.597870601173028,\"longitude\":-46.62437081336976},{\"latitude\":-23.597866504645836,\"longitude\":-46.62436544895173},{\"latitude\":-23.597860359854792,\"longitude\":-46.624350696802146},{\"latitude\":-23.59785667298003,\"longitude\":-46.62433326244355},{\"latitude\":-23.597854624716234,\"longitude\":-46.62431851029397},{\"latitude\":-23.597853805410693,\"longitude\":-46.62431091070175},{\"latitude\":-23.597852986105167,\"longitude\":-46.62429749965668},{\"latitude\":-23.59785339575795,\"longitude\":-46.62428677082062},{\"latitude\":-23.597853805410693,\"longitude\":-46.624282747507095},{\"latitude\":-23.59785585367453,\"longitude\":-46.62427648901939},{\"latitude\":-23.597857901938283,\"longitude\":-46.62427470088005},{\"latitude\":-23.597863227423986,\"longitude\":-46.62427693605424},{\"latitude\":-23.597866914298578,\"longitude\":-46.62428051233291},{\"latitude\":-23.59787264943658,\"longitude\":-46.62429034709931},{\"latitude\":-23.597874697700078,\"longitude\":-46.62430241703988},{\"latitude\":-23.597873878394694,\"longitude\":-46.62431627511978},{\"latitude\":-23.597870191520315,\"longitude\":-46.62432879209518},{\"latitude\":-23.597865685340395,\"longitude\":-46.62433683872222},{\"latitude\":-23.59786404672944,\"longitude\":-46.62433952093124},{\"latitude\":-23.597860769507534,\"longitude\":-46.62434175610542},{\"latitude\":-23.59785667298003,\"longitude\":-46.624341309070594},{\"latitude\":-23.597846841313483,\"longitude\":-46.624335050582886},{\"latitude\":-23.597837009646213,\"longitude\":-46.62432253360749},{\"latitude\":-23.59783045520095,\"longitude\":-46.62431314587594},{\"latitude\":-23.597827587631073,\"longitude\":-46.6243064403534},{\"latitude\":-23.597820623532456,\"longitude\":-46.62429034709931},{\"latitude\":-23.597816117350803,\"longitude\":-46.62427067756653},{\"latitude\":-23.597812020821905,\"longitude\":-46.62425324320793},{\"latitude\":-23.59780915325163,\"longitude\":-46.62423267960549},{\"latitude\":-23.597808333945764,\"longitude\":-46.62421390414239},{\"latitude\":-23.5978095629045,\"longitude\":-46.624199151992805},{\"latitude\":-23.597812020821905,\"longitude\":-46.6241893172264},{\"latitude\":-23.597814069086397,\"longitude\":-46.62418127059936},{\"latitude\":-23.59781488839215,\"longitude\":-46.624177694320686},{\"latitude\":-23.59781570769792,\"longitude\":-46.624171882867806},{\"latitude\":-23.597816117350803,\"longitude\":-46.624167859554284},{\"latitude\":-23.59781570769792,\"longitude\":-46.62416473031044},{\"latitude\":-23.59781570769792,\"longitude\":-46.624162048101425},{\"latitude\":-23.597815298045063,\"longitude\":-46.62416070699692},{\"latitude\":-23.597814478739252,\"longitude\":-46.62415847182273},{\"latitude\":-23.597813659433484,\"longitude\":-46.62415489554406}]}]");
            existent_vehicles=new JSONArray("[{\"view_id\":1,\"model\":0,\"width\":1.9,\"length\":3.8,\"latitude\":-23.597813659433484,\"longitude\":-46.62419155240059,\"position\":{\"heading\":70.61231994628906,\"x\":428,\"y\":407},\"roll\":0,\"rotation\":70.61231994628906,\"label\":\"1\",\"placa\":\"\",\"marca\":\"\",\"modelo\":\"\",\"municipio\":\"\",\"uf\":\"\",\"tipo_veiculo_id\":0,\"tipo_veiculo\":\"Auto\"},{\"view_id\":2,\"model\":13,\"width\":1.7,\"length\":2.5,\"latitude\":-23.597801779499108,\"longitude\":-46.62408247590066,\"position\":{\"heading\":0,\"x\":431,\"y\":422},\"roll\":0,\"rotation\":0,\"label\":\"1\"},{\"view_id\":3,\"model\":15,\"width\":4,\"length\":4,\"latitude\":-23.597812430474818,\"longitude\":-46.6241580247879,\"position\":{\"heading\":0,\"x\":405,\"y\":405},\"roll\":0,\"rotation\":0,\"label\":\"\",\"tipo_impacto_id\":0,\"tipo_impacto\":\"Colisão Frontal\"}]");
        } catch (JSONException e) {
            }
*/
        //if((existent_vehicles.length()>0)||(paths.length()>0)) {
        //((RadioButton)findViewById(R.id.radio_desenho)).setChecked(true);
        //
        ((RadioButton)findViewById(R.id.radio_mapa)).setChecked(true);
        final JSONArray finalExistent_vehicles = existent_vehicles;
        paths=existent_paths;
        vehicleLoader=new ViewTreeObserver.OnGlobalLayoutListener() { // mapa pronto
            @Override
            public void onGlobalLayout() {
                ((RadioButton)findViewById(R.id.radio_desenho)).setChecked(true);
                //loadPaths((Panel)findViewById(R.id.drawing_panel));
                setVehicles(finalExistent_vehicles);
                reloadVehiclesAndPaths();
                findViewById(R.id.map).getViewTreeObserver().removeOnGlobalLayoutListener(vehicleLoader);
            }
        };
        if((existent_vehicles.length()>0)||(existent_paths.length()>0)){
            map.getViewTreeObserver().addOnGlobalLayoutListener(vehicleLoader);
        }
        ((WebView)findViewById(R.id.digest_webview)).getSettings().setJavaScriptEnabled(true);
        ((WebView)findViewById(R.id.digest_webview)).setWebViewClient(new WebViewClient(){
            public void onPageFinished(WebView view, String url) {
                if(findViewById(R.id.digest_view).getVisibility()!=View.VISIBLE) return;
                view.evaluateJavascript(readFromAsset("jquery"),null);
                view.evaluateJavascript(readFromAsset("functions"),null);
                JSONObject d = collectData();
                JSONArray jv= d.optJSONObject("info").optJSONArray("vehicles");
                ValueCallback<String> callback = new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        Log.d("IAT JS ", ":: " + value);
                    }
                };
                for(int i=0;i<jv.length();i++){
                    JSONObject v=jv.optJSONObject(i);
                    switch(v.optInt("model")){
                        case(VehicleFix.COLISAO):
                            view.evaluateJavascript(String.format("document.getElementById('incidente').innerHTML+='<div>%s</div>'",new String[]{v.optString("tipo_impacto")}),null);
                            break;
                        case AUTO:
                        case BICI:
                        case CAMINHAO:
                        case CAMINHONETE:
                        case CAMIONETA:
                        case CARROCA:
                        case MICROONIBUS:
                        case VehicleFix.MOTO:
                        case VehicleFix.REBOQUE:
                        case VehicleFix.SEMI:
                        case TAXI:
                        case VehicleFix.TRAILER:
                        case VIATURA:
                            String script=String.format("veiculo(%s);",v.toString());
                            view.evaluateJavascript(script,null);
                            break;
                        case VehicleFix.PEDESTRE:
                            script=String.format("pedestre(%s);", v.toString());
                            view.evaluateJavascript(script,null);
                            break;
                        case VehicleFix.OBSTACULO:
                            break;
                    }
                }
                view.evaluateJavascript(String.format("document.getElementById('croqui').setAttribute('src','data:image/png;base64,%s');",new String[]{d.optString("thumbnail")}),null);
            }
        });
    }

    private String readFromAsset(String filename) {
        StringBuilder buf=new StringBuilder();
        InputStream json= null;
        try {
            json = getAssets().open(filename);
        } catch (IOException e) {
            Log.e("IAT", String.format("Arquivo [[[%s]]] NÃO EXISTE",new String[]{filename}));
            return null;
        }
        BufferedReader in=null;
        try {
            in = new BufferedReader(new InputStreamReader(json, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Log.e("IAT", String.format("Encoding problemático em Arquivo [[[%s]]]",new String[]{filename}));
            return null;
        }
        String str;
        try {
            while ((str=in.readLine()) != null) {
                buf.append(str);
            }
            in.close();
        } catch (IOException e) {
            Log.e("IAT", String.format("I/O error em Arquivo [[[%s]]]",new String[]{filename}));
            return null;
        }
        return buf.toString();
    }


    private void refresh() {
        setCurrentMode(VEHICLES);
        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        ((RadioButton)findViewById(R.id.radio_desenho)).setChecked(true);
                        ((RadioButton)findViewById(R.id.radio_mapa)).setChecked(true);
                    }
                },
                1000);
    }

    private JSONObject getPicture() throws JSONException {
        JSONObject res=new JSONObject();
        View map = findViewById(R.id.map);
        map.setWillNotCacheDrawing(false);
        map.destroyDrawingCache();
        map.buildDrawingCache();
        Bitmap bi = Bitmap.createBitmap(map.getDrawingCache());
        View draw = findViewById(R.id.drawing_panel);
        draw.setWillNotCacheDrawing(false);
        draw.destroyDrawingCache();
        draw.buildDrawingCache();
        Bitmap bi_d = Bitmap.createBitmap(draw.getDrawingCache());
        Bitmap bo = Bitmap.createBitmap(bi.getWidth(), bi.getHeight(), bi.getConfig());
        Canvas c=new Canvas(bo);
        c.drawColor(ContextCompat.getColor(context, R.color.white));
        c.drawBitmap(bi,0,0,null);
        c.drawBitmap(bi_d,0,0,null);
        View cars = findViewById(R.id.vehicles_canvas);
        cars.setWillNotCacheDrawing(false);
        cars.destroyDrawingCache();
        cars.buildDrawingCache();
        Bitmap ca = cars.getDrawingCache();
        if(ca!=null){
            Bitmap bi_c = Bitmap.createBitmap(cars.getDrawingCache());
            c.drawBitmap(bi_c,0,0,null);
        }
        if(Debug.isDebuggerConnected()) {
            try {
                bo.compress(Bitmap.CompressFormat.PNG, 95, new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/map_view.png"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        bo = Bitmap.createBitmap(
                bo,
                0,
                bo.getHeight()/2 - bo.getWidth()/2,
                bo.getWidth(),
                bo.getWidth()
        );
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bo.compress(Bitmap.CompressFormat.PNG, 90, baos);
        byte[] b = baos.toByteArray();
        if(Debug.isDebuggerConnected()) {
            try {
                bo.compress(Bitmap.CompressFormat.PNG, 95, new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/map_view_sq.png"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        res.put("image",Base64.encodeToString(b, Base64.DEFAULT).replaceAll("\\n",""));
        bo = Bitmap.createScaledBitmap(bo, croqui_size, croqui_size, true);
        if(Debug.isDebuggerConnected()){
            try {
                bo.compress(Bitmap.CompressFormat.PNG, 95, new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/map_view_thu.png"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        baos = new ByteArrayOutputStream();
        bo.compress(Bitmap.CompressFormat.PNG, 90, baos);
        b = baos.toByteArray();

        res.put("thumbnail",Base64.encodeToString(b,Base64.DEFAULT).replaceAll("\\n",""));
        return res;
    }

    public void detailPagerSetup(int vehicle_id) {
        Toolbar toolbar=(Toolbar)findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setVisibility(View.GONE);
        findViewById(R.id.vehicle_details).setVisibility(View.VISIBLE);
        ((ViewPager)findViewById(R.id.vehicle_details)).setAdapter(new VehicleDetailsAdapter(context));
        ((ViewPager)findViewById(R.id.vehicle_details)).setCurrentItem(getVehicleIndexById(vehicle_id));
        findViewById(R.id.vehicle_details).invalidate();
    }
    public void detailPagerSetup() {
        detailPagerSetup(((VehicleFix)getSelectedVehicle()).getVehicleId());
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

    private View addPessoaResumo(JSONObject p, final ViewGroup g) {
        final LayoutInflater inflater = this.getLayoutInflater();
        View pd = inflater.inflate(R.layout.pessoa_resumo, null);
        g.addView(pd);
        pd.findViewById(R.id.edit_pessoa_butt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPessoa((View)view.getParent().getParent(),null);
            }
        });
        pd.findViewById(R.id.remove_pessoa_butt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                g.removeView((View)view.getParent().getParent());
            }
        });
        ((TextView)pd.findViewById(R.id.nome_text)).setText(p.optString("nome"));
        ((TextView)pd.findViewById(R.id.pessoa_data)).setText(p.toString());
        return pd;
    }
    private void showPessoa(final View pessoa_detalhe, final ViewGroup parent){
        final LayoutInflater inflater = this.getLayoutInflater();
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final View v=inflater.inflate(R.layout.pessoa, null);
        builder.setView(v);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                JSONObject p=new JSONObject();
                View pd;
                try {
                    p.put("nome",((EditText)v.findViewById(R.id.nome_text)).getText());
                    int f=((RadioGroup)v.findViewById(R.id.tipo_user_r)).getCheckedRadioButtonId();
                    if(f>=0)
                        p.put("tipo_usuario", ((RadioButton)((RadioGroup)v.findViewById(R.id.tipo_user_r)).findViewById(f)).getText());
                    f=((RadioGroup)v.findViewById(R.id.ferimento_r)).getCheckedRadioButtonId();
                    if(f>=0)
                        p.put("ferimento", ((RadioButton)((RadioGroup)v.findViewById(R.id.ferimento_r)).findViewById(f)).getText());
                    f=((RadioGroup)v.findViewById(R.id.sexo_r)).getCheckedRadioButtonId();
                    if(f>=0)
                        p.put("sexo", ((RadioButton)((RadioGroup)v.findViewById(R.id.sexo_r)).findViewById(f)).getText());
                    p.put("idade",((EditText)v.findViewById(R.id.idade_text)).getText());
                    f=((RadioGroup)v.findViewById(R.id.pos_r)).getCheckedRadioButtonId();
                    if(f>=0)
                        p.put("posicao_no_veiculo", ((RadioButton)((RadioGroup)v.findViewById(R.id.pos_r)).findViewById(f)).getText());
                    f=((RadioGroup)v.findViewById(R.id.cinto_r)).getCheckedRadioButtonId();
                    if(f>=0)
                        p.put("cinto_ou_capacete", ((RadioButton)((RadioGroup)v.findViewById(R.id.cinto_r)).findViewById(f)).getText());

                } catch (JSONException ignore) {}
                if(pessoa_detalhe==null) {
                    pd = addPessoaResumo(p,parent);
                }else{
                    pd=pessoa_detalhe;
                }
                ((TextView)pd.findViewById(R.id.nome_text)).setText(p.optString("nome"));
                ((TextView)pd.findViewById(R.id.pessoa_data)).setText(p.toString());
            }
        });

        builder.setNegativeButton(getText(R.string.cancelar), (dialog, which) -> dialog.dismiss());
        builder.create().show();
        if(pessoa_detalhe!=null){
            try {
                JSONObject pessoa=new JSONObject((String) ((TextView)pessoa_detalhe.findViewById(R.id.pessoa_data)).getText());
                ((EditText)v.findViewById(R.id.nome_text)).setText(pessoa.optString("nome"));
                ((EditText)v.findViewById(R.id.idade_text)).setText(pessoa.optString("idade"));
                RadioGroup r;
                if(pessoa.has("tipo_usuario")){
                    r = (RadioGroup) v.findViewById(R.id.tipo_user_r);
                    for(int i=0;i<r.getChildCount();i++){
                        if(((RadioButton)r.getChildAt(i)).getText().toString().equals(pessoa.optString("tipo_usuario","")))
                            r.check(r.getChildAt(i).getId());
                    }
                }
                if(pessoa.has("ferimento")){
                    r = (RadioGroup) v.findViewById(R.id.ferimento_r);
                    for(int i=0;i<r.getChildCount();i++){
                        if(((RadioButton)r.getChildAt(i)).getText().toString().equals(pessoa.optString("ferimento","")))
                            r.check(r.getChildAt(i).getId());
                    }
                }
                if(pessoa.has("sexo")){
                    r = (RadioGroup) v.findViewById(R.id.sexo_r);
                    for(int i=0;i<r.getChildCount();i++){
                        if(((RadioButton)r.getChildAt(i)).getText().toString().equals(pessoa.optString("sexo","")))
                            r.check(r.getChildAt(i).getId());
                    }
                }
                if(pessoa.has("posicao_no_veiculo")){
                    r = (RadioGroup) v.findViewById(R.id.pos_r);
                    for(int i=0;i<r.getChildCount();i++){
                        if(((RadioButton)r.getChildAt(i)).getText().toString().equals(pessoa.optString("posicao_no_veiculo","")))
                            r.check(r.getChildAt(i).getId());
                    }
                }
                if(pessoa.has("cinto_ou_capacete")){
                    r = (RadioGroup) v.findViewById(R.id.cinto_r);
                    for(int i=0;i<r.getChildCount();i++){
                        if(((RadioButton)r.getChildAt(i)).getText().toString().equals(pessoa.optString("cinto_ou_capacete","")))
                            r.check(r.getChildAt(i).getId());
                    }
                }
            } catch (JSONException e) {}
        }

    }
    private void plot(int t){
        plot(t, null);
    }

    private void plot(int t, final String dt) {
        final int tipo=t;
        LayoutInflater inflater = this.getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch(tipo){
            case R.layout.fields_obstaculo:
                builder.setTitle("Detalhes");
                break;
            case R.layout.fields_colisao:
                builder.setTitle("Impacto");
                break;
            default:
                builder.setTitle("Veículo");
        }
        final View v=inflater.inflate(tipo, null);
        builder.setView(v);
        builder.setPositiveButton("OK", null);
        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which){
                dialog.dismiss();
            }
        });
        switch(tipo){
            case R.layout.fields_obstaculo:
                final EditText xu = (EditText) v.findViewById(R.id.largura_text);
                xu.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        String vu = charSequence.toString();
                        double valor;
                        try {
                            valor = Double.parseDouble(vu);
                        }catch(NumberFormatException exx){
                            return;
                        }
                        if(valor<=0)
                            xu.setText("");
                        if(valor>10)
                            xu.setText("10");
                    }
                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });
                break;
            case R.layout.fields_vehicle:
                placaTrick(v);
                marcaTrick(v);
                break;
        }
        final AlertDialog di = builder.create();
        di.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button button = ((AlertDialog) di).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // TODO Do something
                        switch(tipo){
                            case R.layout.fields_obstaculo:
                                try {
                                    double largura = Double.parseDouble(((TextView) v.findViewById(R.id.largura_text)).getText().toString());
                                    double comprimento = Double.parseDouble(((TextView) v.findViewById(R.id.comprimento_text)).getText().toString());
                                    JSONObject d=new JSONObject();
                                    try {
                                        d.put("comprimento",comprimento);
                                        d.put("largura",largura);
                                        d.put("nome",((EditText) v.findViewById(R.id.tipo_obstaculo_text)).getText().toString());
                                    } catch (JSONException ignored) {
                                    }

                                    if (largura > 0 && comprimento > 0) {
                                        createVehicle(VehicleFix.OBSTACULO, largura, comprimento,d);
                                    }
                                }catch(NumberFormatException xxx){
                                    return;
                                }
                                break;
                            case R.layout.fields_vehicle:
                                try {
                                    String placa= String.valueOf(((EditText)v.findViewById(R.id.placa_text)).getText());
                                    if(((CheckedTextView)v.findViewById(R.id.is_placa_padrao)).isChecked()){
                                        placa=String.format("%s%s",((EditText)v.findViewById(R.id.placa_letras)).getText().toString(),((EditText)v.findViewById(R.id.placa_numeros)).getText().toString());
                                    }
                                    int tipo_veiculo= ((Spinner)v.findViewById(R.id.tipo_veiculo_spinner)).getSelectedItemPosition();
                                    String marca= String.valueOf(((EditText)v.findViewById(R.id.marca_text_auto)).getText());
                                    String modelo= String.valueOf(((EditText)v.findViewById(R.id.modelo_text_auto)).getText());
                                    JSONObject d=new JSONObject();
                                    try {
                                        d.put("placa",placa);
                                        d.put("marca",marca);
                                        d.put("modelo",modelo);
                                        d.put("municipio", ((EditText) v.findViewById(R.id.municipio_text)).getText());
                                        d.put("uf", ((Spinner) v.findViewById(R.id.uf_spinner)).getSelectedItem().toString());

                                        d.put("tipo_veiculo_id",tipo_veiculo);
                                        d.put("tipo_veiculo",String.valueOf(((Spinner)v.findViewById(R.id.tipo_veiculo_spinner)).getSelectedItem()));
                                    } catch (JSONException ignored) {}


                            /*
<item>Auto</item>
<item>Caminhão</item>
<item>Caminhonete</item>
<item>Camioneta</item>
<item>Carroça</item>
<item>Micro Ônibus</item>
<item>Moto</item>
<item>Ônibus</item>
<item>Reboque</item>
<item>Semi Reboque</item>
<item>Taxi</item>
<item>Trailer</item>
<item>Viatura</item>*/

                                    switch(tipo_veiculo){
                                        case VehicleFix.AUTO: //carro
                                            createVehicle(VehicleFix.AUTO,1.9,3.8,d);
                                            break;
                                        case VehicleFix.CAMINHAO: //caminhao
                                            createVehicle(VehicleFix.CAMINHAO,3.9,11.4,d);
                                            break;
                                        case VehicleFix.CAMINHONETE:
                                            createVehicle(VehicleFix.CAMINHONETE,2.9,6.0,d);
                                            break;
                                        case VehicleFix.CAMIONETA:
                                            createVehicle(VehicleFix.CAMIONETA,3.9,7.6,d);
                                            break;
                                        case VehicleFix.CARROCA:
                                            createVehicle(VehicleFix.CARROCA,1.7,2.4,d);
                                            break;
                                        case VehicleFix.MICROONIBUS:
                                            createVehicle(VehicleFix.MICROONIBUS,3.6,8.4,d);
                                            break;
                                        case VehicleFix.MOTO:
                                            createVehicle(VehicleFix.MOTO,2.8,4.4,d);
                                            break;
                                        case VehicleFix.ONIBUS:
                                            createVehicle(VehicleFix.ONIBUS,3.8,10.4,d);
                                            break;
                                        case VehicleFix.REBOQUE:
                                            createVehicle(VehicleFix.REBOQUE,2.3,5.8,d);
                                            break;
                                        case VehicleFix.SEMI:
                                            createVehicle(VehicleFix.SEMI,5.1,22.4,d);
                                            break;
                                        case VehicleFix.TAXI:
                                            createVehicle(VehicleFix.TAXI,1.9,3.8,d);
                                            break;
                                        case VehicleFix.TRAILER:
                                            createVehicle(VehicleFix.TRAILER,2.3,5.8,d);
                                            break;
                                        case VehicleFix.VIATURA:
                                            createVehicle(VehicleFix.VIATURA,1.9,3.8,d);
                                            break;

                                    }
                                }catch(NumberFormatException xxx){
                                    return;
                                }
                                break;
                            case R.layout.fields_colisao:
                                JSONObject d=new JSONObject();
                                try {
                                    d.put("data_e_hora", dt);
                                    d.put("tipo_impacto_id",((Spinner)v.findViewById(R.id.impacto_spinner)).getSelectedItemPosition());
                                    d.put("tipo_impacto",String.valueOf(((Spinner)v.findViewById(R.id.impacto_spinner)).getSelectedItem()));
                                } catch (JSONException ignored) {}
                                createVehicle(VehicleFix.COLISAO,4.0,4.0,d);
                        }
                        //Dismiss once everything is OK.
                        di.dismiss();
                    }
                });
            }
        });
        di.show();
    }



    private void marcaTrick(final View v) {
        v.findViewById(R.id.marca_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((EditText)v.findViewById(R.id.marca_text_auto)).setText("");
            }
        });
        v.findViewById(R.id.modelo_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((EditText)v.findViewById(R.id.modelo_text_auto)).setText("");
            }
        });
        ((AutoCompleteTextView)v.findViewById(R.id.marca_text_auto)).setAdapter(getMarcas());
        ((EditText)v.findViewById(R.id.marca_text_auto)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                setModelos(v);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void setModelos(View v) {
        ((AutoCompleteTextView)v.findViewById(R.id.modelo_text_auto)).setAdapter(getModelos(((TextView)v.findViewById(R.id.marca_text_auto)).getText().toString()));
    }

    private void placaTrick(final View v) {
        final View numbers = v.findViewById(R.id.placa_numeros);
        v.findViewById(R.id.is_placa_padrao).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean s = !((CheckedTextView) view).isChecked();
                ((CheckedTextView)view).setChecked(s);
                v.findViewById(R.id.placa_padrao_layout).setVisibility((s)?View.VISIBLE:View.GONE);
                v.findViewById(R.id.placa_text).setVisibility((s)?View.GONE:View.VISIBLE);
                if(s){
                    Pattern p = Pattern.compile("(\\w{3})(\\d{4})");
                    Matcher m = p.matcher(((TextView)v.findViewById(R.id.placa_text)).getText());
                    if(m.matches()) {
                        ((TextView) v.findViewById(R.id.placa_letras)).setText(m.group(1));
                        ((TextView) v.findViewById(R.id.placa_numeros)).setText(m.group(2));
                    }else{

                    }
                }else{
                    ((TextView)v.findViewById(R.id.placa_text)).setText(String.format("%s%s",new String[]{
                            ((TextView) v.findViewById(R.id.placa_letras)).getText().toString(),
                            ((TextView) v.findViewById(R.id.placa_numeros)).getText().toString()
                    }));
                }
            }
        });

        InputFilter filter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend)
            {
                if((dest.length()+source.length()>2)&&(source.length()>0)){
                    numbers.requestFocus();
                }
                for (int i = start; i < end; i++) {
                    if (!Character.isLetter(source.charAt(i))) {
                        return "";
                    }
                }
                return null;
            }
        };
        ((EditText)v.findViewById(R.id.placa_letras)).setFilters(new InputFilter[]{filter});
        final CsiActivity a = this;
        final View formic=v;
        ((Button)v.findViewById(R.id.listaplaca_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(a);
                builder.setTitle(R.string.choose_license)
                        .setItems(placas, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String chosen= (String) placas[i];
                                if(chosen.matches("\\w{3}\\d{4}")){
                                    ((CheckedTextView)formic.findViewById(R.id.is_placa_padrao)).setChecked(true);
                                    formic.findViewById(R.id.placa_padrao_layout).setVisibility(View.VISIBLE);
                                    formic.findViewById(R.id.placa_text).setVisibility(View.GONE);
                                    ((EditText)formic.findViewById(R.id.placa_letras)).setText(chosen.substring(0,3));
                                    ((EditText)formic.findViewById(R.id.placa_numeros)).setText(chosen.substring(3));
                                }else{
                                    ((CheckedTextView)formic.findViewById(R.id.is_placa_padrao)).setChecked(false);
                                    formic.findViewById(R.id.placa_padrao_layout).setVisibility(View.GONE);
                                    formic.findViewById(R.id.placa_text).setVisibility(View.VISIBLE);
                                    ((EditText)formic.findViewById(R.id.placa_text)).setText(chosen);

                                }

                            }
                        });

                AlertDialog ad = builder.create();
                ad.show();
            }
        });
    }

    private String getLabel(JSONObject v) {
        switch(v.optInt("model")){
            case VehicleFix.PEDESTRE:
                return getNextPedestrianLabel();
            case VehicleFix.OBSTACULO:
                return getNextObstacleLabel();
            case VehicleFix.SENTIDO:
            case VehicleFix.COLISAO:
                return "";
            default:
                return getNextVehicleLabel();
        }
    }
    private String getNextObstacleLabel() {
        ArrayList<String> labels=new ArrayList<>();
        for(int i=0;i<vehicles.length();i++){
            JSONObject vu = vehicles.optJSONObject(i);
            if(vu.has("label")&&(vu.optInt("model")==VehicleFix.OBSTACULO)) {
                labels.add(vu.optString("label"));
            }
        }
        Collections.sort(labels,new Comparator<String>() {
            @Override
            public int compare(String s, String t1) {
                return t1.compareTo(s);
            }
            @Override
            public boolean equals(Object o) {
                return false;
            }
        });
        if(labels.size()==0)return "A";
        int lastlabel = (int)labels.get(0).charAt(0);
        lastlabel++;
        return String.valueOf((char)lastlabel);
    }
    private String getNextVehicleLabel() {
        int label=1;
        int[] kinds=new int[]{
                BICI,
                VehicleFix.SEMI,
                VehicleFix.REBOQUE,
                VehicleFix.MICROONIBUS,
                VehicleFix.ONIBUS,
                VehicleFix.TRAILER,
                VehicleFix.VIATURA,
                VehicleFix.TAXI,
                VehicleFix.CAMIONETA,
                VehicleFix.CAMINHONETE,
                VehicleFix.AUTO,
                VehicleFix.CAMINHAO,
                VehicleFix.ONIBUS,
                VehicleFix.MOTO
        };
        for(int i=0;i<vehicles.length();i++){
            JSONObject vu = vehicles.optJSONObject(i);
            if(ArrayUtils.contains(kinds,vu.optInt("model"))) {
                label++;
            }
        }
        return ""+label;
    }
    private String getNextPedestrianLabel() {
        int label=1;
        for(int i=0;i<vehicles.length();i++){
            JSONObject vu = vehicles.optJSONObject(i);
            if(vu.optInt("model")==VehicleFix.PEDESTRE) {
                label++;
            }
        }
        return ""+label;
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
        if(findViewById(R.id.palette_layout)==null)return;
        if(findViewById(R.id.palette_layout).getVisibility()==View.VISIBLE){
            findViewById(R.id.palette_layout).setVisibility(View.GONE);
            findViewById(R.id.show_pallette).setVisibility(View.VISIBLE);
            return;
        }
        if(findViewById(R.id.vehicle_details).getVisibility()==View.VISIBLE) {
            findViewById(R.id.vehicle_details).setVisibility(View.GONE);
            Toolbar toolbar=(Toolbar)findViewById(R.id.my_toolbar);
            setSupportActionBar(toolbar);
            toolbar.setVisibility(View.VISIBLE);
            return;
        }
        if(findViewById(R.id.digest_view).getVisibility()==View.VISIBLE){
            exitReview();
            return;
        }
        ((Panel) findViewById(R.id.drawing_panel)).back();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.controls, menu);
        MenuItem sub = menu.findItem(R.id.desenhar_sub);
        inflater.inflate(R.menu.freehand_tools, sub.getSubMenu());
        sub = menu.findItem(R.id.objects_sub);
        inflater.inflate(R.menu.item_tools, sub.getSubMenu());
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.center_here).setVisible(gps);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.new_vehicle:
                plot(R.layout.fields_vehicle);
                break;
            case R.id.new_walker:
                JSONObject d=new JSONObject();
                createVehicle(VehicleFix.PEDESTRE,1.7,2.5,d);
                break;
            case R.id.new_bicycle:
                d=new JSONObject();
                try {
                    d.put("tipo_veiculo","Bicicleta");
                } catch (JSONException ignore) {}
                createVehicle(BICI,1.8,2.2,d);
                break;
            case R.id.new_direction:
                createVehicle(VehicleFix.SENTIDO,3.0,3.8,new JSONObject());
                break;
            case R.id.new_post:
                createVehicle(VehicleFix.SPU,1.0,1.0,new JSONObject());
                break;
            case R.id.new_tree:
                createVehicle(VehicleFix.ARVORE,5.0,5.0,new JSONObject());
                break;
            case R.id.new_obstacle:
                plot(R.layout.fields_obstaculo);
                break;
            case R.id.new_mark:
                startDraw(Panel.SKID);
                break;
            case R.id.new_zebra:
                startDraw(Panel.ZEBRA);
                break;
            case R.id.new_centerline:
                startDraw(Panel.CENTERLINE);
                break;
            case R.id.new_path:
                startDraw(Panel.TRACK);
                break;
            case R.id.remove_line:
                startEraser();
                break;
            case R.id.new_impact:
                prepareImpact();
                //plot(R.layout.fields_colisao);
                break;
            case R.id.move_map:
                ((RadioButton)findViewById(R.id.radio_mapa)).setChecked(true);
                break;
            case R.id.move_car:
                ((RadioButton)findViewById(R.id.radio_desenho)).setChecked(true);
                break;
            case R.id.preview:
                review();
                break;
            case R.id.end:
                Intent data=new Intent();
                data.putExtra("data",collectData().toString());
                Log.d("IAT send result", "enviando croqui para o eGO");
                setResult(RESULT_OK, data);
                finish();
                break;

            case R.id.center_here:
                ((RadioButton)findViewById(R.id.radio_mapa)).setChecked(true);
                JSONObject point = ((Iat) getApplicationContext()).getLastKnownPosition();
                if (point.has("latitude")) {
                    ((MapView) findViewById(R.id.map)).getController().setCenter(new GeoPoint(point.optDouble("latitude"), point.optDouble("longitude")));
                }
                break;
/*
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
                */
        }
        return super.onOptionsItemSelected(item);
    }

    private void prepareImpact() {
        // coletar a data e hora do evento
        final Calendar cal = Calendar.getInstance();
        final DateFormat dateFormat = new SimpleDateFormat("dd/M/yyyy HH:mm");
        final Date date=new Date();
        cal.setTime(date);

        AlertDialog.Builder bu = new AlertDialog.Builder(context);
        LayoutInflater inflater = getLayoutInflater();
        final View l = inflater.inflate(R.layout.dialog_datetimepicker, null);
        ((DatePicker)l.findViewById(R.id.datepicker)).init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker datePicker, int i, int i1, int i2) {
                l.findViewById(R.id.datepicker).setVisibility(View.GONE);
                l.findViewById(R.id.timepicker).setVisibility(View.VISIBLE);
                l.findViewById(R.id.datetime_confirm).setVisibility(View.VISIBLE);
            }
        });
        ((TimePicker)l.findViewById(R.id.timepicker)).setCurrentHour(cal.get(Calendar.HOUR));
        ((TimePicker)l.findViewById(R.id.timepicker)).setCurrentMinute(cal.get(Calendar.MINUTE));

        bu.setView(l);
        final AlertDialog al = bu.create();
        l.findViewById(R.id.datetime_confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatePicker d=(DatePicker)l.findViewById(R.id.datepicker);
                TimePicker t=(TimePicker) l.findViewById(R.id.timepicker);
                cal.set(d.getYear(),d.getMonth(),d.getDayOfMonth(),t.getCurrentHour(),t.getCurrentMinute());
                //dh.setText(dateFormat.format(cal.getTime()));
                plot(R.layout.fields_colisao, dateFormat.format(cal.getTime()));
                al.dismiss();
            }
        });
        al.show();
    }

    private JSONObject collectData() {
        //((RadioButton)findViewById(R.id.radio_desenho)).setChecked(true);
        MapView map=((MapView)findViewById(R.id.map));
        //savePaths();
        saveVehicles();
        JSONObject o=new JSONObject();
        try {
            JSONObject dj=new JSONObject();
            o = getPicture();
            dj.put("vehicles",vehicles);
            dj.put("paths",paths);
            dj.put("zoom",map.getZoomLevel());
            dj.put("latitude",map.getMapCenter().getLatitude());
            dj.put("longitude",map.getMapCenter().getLongitude());
            JSONArray placas=new JSONArray();
            JSONObject q=new JSONObject();
            for(int i=0;i<vehicles.length();i++){
                JSONObject v = vehicles.optJSONObject(i);
                if(v.has("placa")){
                    placas.put(v.optString("placa"));
                }
                switch(v.optInt("model")){
                    case BICI:
                        q.put("QBIKE",q.optInt("QBIKE",0)+1);
                        break;
                    case AUTO:
                        q.put("QAUTO",q.optInt("QAUTO",0)+1);
                        break;
                    case TAXI:
                        q.put("QTAXI",q.optInt("QTAXI",0)+1);
                        break;
                    case CAMINHAO:
                        q.put("QCAM",q.optInt("QCAM",0)+1);
                        break;
                    case CARROCA:
                        q.put("QCARR",q.optInt("QCARR",0)+1);
                        break;
                    case CAMINHONETE:
                    case CAMIONETA:
                        q.put("QCNTE",q.optInt("QCNTE",0)+1);
                        break;
                    case MICROONIBUS:
                        q.put("QMBUS",q.optInt("QMBUS",0)+1);
                        break;
                    case ONIBUS:
                        q.put("QBUS",q.optInt("QBUS",0)+1);
                        break;
                    case VIATURA:
                        q.put("QVTR",q.optInt("QVTR",0)+1);
                        break;
                }
            }
            o.put("quantidades",q);
            o.put("placas",placas);
            o.put("info",dj);
        } catch (JSONException ignore) {}
        return o;
    }

    public static String readRawTextFile(Context ctx, int resId)
    {
        InputStream inputStream = ctx.getResources().openRawResource(resId);

        InputStreamReader inputreader = new InputStreamReader(inputStream);
        BufferedReader buffreader = new BufferedReader(inputreader);
        String line;
        StringBuilder text = new StringBuilder();

        try {
            while (( line = buffreader.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
        } catch (IOException e) {
            return null;
        }
        return text.toString();
    }
    private void review() {
        ((RadioButton)findViewById(R.id.radio_mapa)).setChecked(true);
        findViewById(R.id.digest_view).setVisibility(View.VISIBLE);
        findViewById(R.id.my_toolbar).setVisibility(View.GONE);
        String h   = readRawTextFile(this, R.raw.digest);
        WebView w = (WebView) findViewById(R.id.digest_webview);
        w.loadData(h.toString(),"text/html; charset=utf-8", "utf-8");
    }



    private void exitReview() {
        findViewById(R.id.digest_view).setVisibility(View.GONE);
        findViewById(R.id.my_toolbar).setVisibility(View.VISIBLE);
        ((WebView)findViewById(R.id.digest_webview)).loadUrl("about:blank");
        //((RadioButton)findViewById(R.id.radio_desenho)).setChecked(true);
    }


    private void startEraser() {
        ((RadioButton)findViewById(R.id.radio_desenho)).setChecked(true);
        setCurrentMode(FREEHAND);
        findViewById(R.id.vehicle_details).setVisibility(View.GONE);
        findViewById(R.id.vehicle_details_inicial).setVisibility(View.GONE);
        findViewById(R.id.info_box).setVisibility(View.VISIBLE);
        findViewById(R.id.tool_instructions).setVisibility(View.VISIBLE);
        ((TextView)findViewById(R.id.tool_tip_text)).setText(getString(R.string.touch_to_erase));
        ((Panel)findViewById(R.id.drawing_panel)).setLigado(true);
        ((Panel)findViewById(R.id.drawing_panel)).setStyle(Panel.ERASER,getResolution());
        ((Panel)findViewById(R.id.drawing_panel)).setVisibility(View.VISIBLE);

    }
    private void stopEraser() {
        ((Panel)findViewById(R.id.drawing_panel)).setLigado(false);
        findViewById(R.id.vehicle_details_inicial).setVisibility(View.VISIBLE);
        findViewById(R.id.info_box).setVisibility(View.GONE);
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
        Log.d("IAT", "terminou de configurar carros");
    }
    private void showLabels(boolean b) {
        ViewGroup cv = (ViewGroup) findViewById(R.id.vehicles_canvas);
        for(int i=0;i<cv.getChildCount();i++){
            View label = cv.getChildAt(i).findViewById(R.id.vehicle_label_text);
            label.setVisibility((b)?View.VISIBLE:View.GONE);
        }
        ((MapView)findViewById(R.id.map)).invalidate();
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
            new LayerLoader(url, bb, "labels").execute();

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
            updateLabelPosition(fu);
            pegador.invalidate();
        }
    }
    public void updateLabelPosition(VehicleFix fu) {
        View bode = fu.findViewById(R.id.vehicle_chassi);
        TextView label= (TextView) fu.findViewById(R.id.vehicle_label_text);
        label.setX(bode.getX()+bode.getWidth()/2+labelOffset[0]);
        label.setY(bode.getY()+bode.getHeight()/2+labelOffset[1]);
    }


    public void updateVehiclePosition(Pega l, float[] ponta) {
        View r = getSelectedVehicle();
        if(r==null) return;
        LinearLayout body = (LinearLayout) r.findViewById(R.id.vehicle_body);
        LinearLayout chassi = (LinearLayout) r.findViewById(R.id.vehicle_chassi);
        TextView label = (TextView) r.findViewById(R.id.vehicle_label_text);
        if(body==null)return;
        //float[] ce = {l.getX(),l.getY()};
        //Log.d("IAT","POSICAO: "+body.getX()+", "+body.getY()+" - ponta pegada "+ponta[0]+" "+ponta[1]+" centro "+ce[0]+" "+ce[1]);
        body.setRotation(l.getRodRotation());
        chassi.setX(ponta[0] - convertDpToPixel(150));
        chassi.setY(ponta[1] - convertDpToPixel(150));
        updateLabelPosition((VehicleFix) r);
        body.invalidate();
    }
    public void setSelectedVehicle(View v) {
        setSelectedVehicle(v,false);
    }

    protected String getVehicleDescription(JSONObject ve){
        String l="";
        if(ve.has("label")){
            l=ve.optString("label")+" - ";
        }
        switch(ve.optInt("model")){
            case VehicleFix.ARVORE:
                l=getString(R.string.arvore);
                findViewById(R.id.bt_delete).setVisibility(View.VISIBLE);
                findViewById(R.id.edit_vehicle_rotate).setVisibility(View.GONE);
                findViewById(R.id.edit_vehicle_butt).setVisibility(View.GONE);
                break;
            case VehicleFix.SPU:
                findViewById(R.id.bt_delete).setVisibility(View.VISIBLE);
                l=getString(R.string.poste);
                findViewById(R.id.edit_vehicle_rotate).setVisibility(View.GONE);
                findViewById(R.id.edit_vehicle_butt).setVisibility(View.GONE);
                break;
            case VehicleFix.SENTIDO:
                findViewById(R.id.bt_delete).setVisibility(View.VISIBLE);
                l=getString(R.string.sentido_da_via);
                findViewById(R.id.edit_vehicle_rotate).setVisibility(View.VISIBLE);
                findViewById(R.id.edit_vehicle_butt).setVisibility(View.GONE);
                break;
            case VehicleFix.OBSTACULO:
                l=l+ve.optString("nome");
                findViewById(R.id.bt_delete).setVisibility(View.GONE);
                findViewById(R.id.edit_vehicle_rotate).setVisibility(View.GONE);
                findViewById(R.id.edit_vehicle_butt).setVisibility(View.VISIBLE);
                break;
            default:
                findViewById(R.id.bt_delete).setVisibility(View.GONE);
                findViewById(R.id.edit_vehicle_butt).setVisibility(View.VISIBLE);
                findViewById(R.id.edit_vehicle_rotate).setVisibility(View.VISIBLE);
                if(ve.has("tipo_veiculo")) {
                    l=l+ve.optString("tipo_veiculo");
                    findViewById(R.id.edit_vehicle_rotate).setVisibility(View.VISIBLE);
                }else if(ve.has("tipo_impacto")){
                    l=ve.optString("tipo_impacto");
                }else if(ve.optInt("model")==VehicleFix.PEDESTRE){
                    l=l+getResources().getString(R.string.pessoa);
                }
                break;
        }
        return l;
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
            int xid = ((VehicleFix) sv).getVehicleId();
            JSONObject veiculo = getVehicleById(xid);
            //findViewById(R.id.edit_vehicle_rotate).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.vehicle_description_text)).setText(getVehicleDescription(veiculo));
            //findViewById(R.id.edit_vehicle_rotate).setVisibility(veiculo.has("tipo_veiculo")?View.VISIBLE:View.GONE);
            ((TextView)findViewById(R.id.vehicle_id_text)).setText(""+xid);
            findViewById(R.id.info_box).setVisibility(View.VISIBLE);
            findViewById(R.id.vehicle_details_inicial).setVisibility(View.VISIBLE);
            findViewById(R.id.tool_instructions).setVisibility(View.GONE);
        }else{
            Log.d("IAT", "nada a fazer aqui");
            pegador.setVisibility(View.GONE);
            findViewById(R.id.info_box).setVisibility(View.GONE);
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
        stopEraser();
        if(mode==current_mode)return;
        MapView map = (MapView) findViewById(R.id.map);
        map.setEnabled(false);
        Panel panel = (Panel) findViewById(R.id.drawing_panel);
        switch (mode){
            case VEHICLES:
                findViewById(R.id.map_block).setVisibility(View.VISIBLE);
                if(current_mode==FREEHAND)savePaths();
                panel.setLigado(false);
                findViewById(R.id.show_pallette).setVisibility(View.VISIBLE);
                findViewById(R.id.vehicles_canvas).setVisibility(View.VISIBLE);
                Log.d("IAT","will reload");
                reloadVehiclesAndPaths();
                ligaCarros(true);
                //showLabels(true);
                break;
            case FREEHAND:
                findViewById(R.id.map_block).setVisibility(View.VISIBLE);
                saveVehicles();
                findViewById(R.id.show_pallette).setVisibility(View.GONE);
                findViewById(R.id.vehicles_canvas).setVisibility(View.VISIBLE);
                panel.setLigado(true);
                reloadVehiclesAndPaths();
                ligaCarros(true);
                setSelectedVehicle(null);
                //showLabels(true);
                break;
            case MAP:
                findViewById(R.id.map_block).setVisibility(View.GONE);
                //showLabels(false);
                savePaths();
                saveVehicles();
                ((Panel) findViewById(R.id.drawing_panel)).setLigado(false);
                map.setBuiltInZoomControls(true);
                map.setEnabled(true);
                map.invalidate();
                setSelectedVehicle(null);
                break;
        }
        current_mode=mode;
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
            drawing_zooming_over = null;
            map.invalidate();
        }
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

    public void pickdate(View view) {

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
        public String getTileURLString(long tileIndex) {
            String u = "http://bigrs.alien9.net:8080/geoserver/gwc/service/tms/1.0.0/BIGRS%3A"+layer+"@3857@png" + "/" + MapTileIndex.getZoom(tileIndex) + "/" + MapTileIndex.getX(tileIndex) + "/" + (int)(Math.pow(2.0,MapTileIndex.getZoom(tileIndex))-MapTileIndex.getY(tileIndex)-1) + ".png";
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
        finish();
    }
    protected void savePaths() {
        MapView map = (MapView) findViewById(R.id.map);
        if (drawing_zooming_over!= null) {
            map.getOverlays().remove(drawing_zooming_over);
            drawing_zooming_over = null;
            map.invalidate();
        }
        View p = findViewById(R.id.drawing_panel);
        savePaths((Panel) p);
        p.setDrawingCacheEnabled(true);
        Bitmap pc = p.getDrawingCache();
        if(pc==null) return;
        Bitmap bii = p.getDrawingCache(true).copy(p.getDrawingCache().getConfig(), false);
        p.destroyDrawingCache();
        if (bii == null) return;
        try {
            bii.compress(Bitmap.CompressFormat.PNG, 95, new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/drawing_screen.png"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BoundingBox b = map.getBoundingBox();
        IGeoPoint position = map.getMapCenter();
        drawing_zooming_over = new CsiGroundOverlay().setBounds(b);
        drawing_zooming_over.setImage(new BitmapDrawable(getResources(), bii));
        drawing_zooming_over.setPosition((GeoPoint) position);
        map.getOverlays().add(drawing_zooming_over);
        ((Panel) p).reset();
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
            if(o.getDrawingCache()==null){
                o.setVisibility(View.GONE);
                return;
            }
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
            vehicles_zooming_over = new CsiGroundOverlay().setBounds(b);
            IGeoPoint position = map.getMapCenter();
            vehicles_zooming_over.setImage(new BitmapDrawable(getResources(), bi));
            vehicles_zooming_over.setPosition((GeoPoint) position);
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
        createVehicle(model, width, length, new JSONObject());
    }

    protected void createVehicle(int model, double width, double length, JSONObject data){
        if(current_mode!=VEHICLES){
            ((RadioButton)findViewById(R.id.radio_desenho)).setChecked(true);
            //setCurrentMode(VEHICLES);
        }
        //((RadioButton)findViewById(R.id.radio_desenho)).setChecked(true);
        MapView map = (MapView) findViewById(R.id.map);
        Point size = getDisplaySize();
        double pixels_per_m = getResolution();
        int w = (int) (width * pixels_per_m);
        int l = (int) (length * pixels_per_m);
        final View v=new VehicleFix(context);
        ((ViewGroup) findViewById(R.id.vehicles_canvas)).addView(v);
        ((VehicleFix)v).zinit(model,0);
        RelativeLayout.LayoutParams rparams = new RelativeLayout.LayoutParams(10*Math.round(w),10*Math.round(l));
        Log.d("IAT QUEBRADO ","mais");
        View chassis = v.findViewById(R.id.vehicle_chassi);
        TextView label = (TextView) v.findViewById(R.id.vehicle_label_text);
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
            veiculo.put("label", getLabel(veiculo));
            if(data!=null){
                Iterator<?> keys = data.keys();
                while( keys.hasNext() ) {
                    String key = (String)keys.next();
                    veiculo.put(key,data.get(key));
                }
                if(data.has("view_id")){
                    ((VehicleFix)v).setVehicleId(data.getInt("view_id"));
                }
            }
        }catch (JSONException e) {}
        label.setText(veiculo.optString("label"));
        //label.setY(labelOffset[1]);
        switch(model){
            case VehicleFix.COLISAO:
                label.setVisibility(View.GONE);
                break;
            default:
                label.setVisibility(View.VISIBLE);
        }
        vehicles.put(veiculo);
        setSelectedVehicle(v, true);
        v.invalidate();
        //new android.os.Handler().postDelayed(
        //        new Runnable() {
        //public void run() {
        //                updateLabelPosition((VehicleFix) getSelectedVehicle());
        //           }
        //},
        //       400);
        v.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                updateLabelPosition((VehicleFix) v);
                updatePegadorForSelectedVehicle();
            }
        });
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
            Log.d("IAT", "removendo layer de veiculos");
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
        Log.d("IAT", "PIXELS POR METRO: "+pixels_per_m);
        //for (int i = 0; i < vehicles.length(); i++) {
        for (int i = 0; i < ((ViewGroup) findViewById(R.id.vehicles_canvas)).getChildCount(); i++) {
            View v=((ViewGroup) findViewById(R.id.vehicles_canvas)).getChildAt(i);
            int vehicle_id=((VehicleFix) v).getVehicleId();
            JSONObject vehicle = getVehicleById(vehicle_id);
            if(vehicle==null){
                Log.e("IAT","veículo não existe com id "+vehicle_id);
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
            TextView label = (TextView) v.findViewById(R.id.vehicle_label_text);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) Math.round(w), (int) Math.round(l));
            body.setLayoutParams(params);
            float pix = convertDpToPixel(150);
            if(position!=null) {
                chassi.setX(position.x-pix);
                chassi.setY(position.y-pix);
            }
            label.setText(vehicle.optString("label"));
            updateLabelPosition((VehicleFix) v);
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
    protected void setVehicles(JSONArray v){
        ((CsiActivity)context).setSelectedVehicle(null);
        findViewById(R.id.vehicles_canvas).setVisibility(View.VISIBLE);
        findViewById(R.id.vehicles_canvas).invalidate();
        findViewById(R.id.vehicles_canvas).setDrawingCacheEnabled(true);
        for (int i = 0; i < v.length(); i++) {
            JSONObject veiculo = v.optJSONObject(i);
            createVehicle(veiculo.optInt("model"),veiculo.optDouble("width"),veiculo.optDouble("length"),veiculo);
        }
        refresh();
        findViewById(R.id.vehicles_canvas).invalidate();
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
                case VehicleFix.AUTO:
                case VehicleFix.CAMINHAO:
                case VehicleFix.CAMINHONETE:
                case VehicleFix.CAMIONETA:
                case VehicleFix.TRAILER:
                case VehicleFix.SEMI:
                case VehicleFix.VIATURA:
                case VehicleFix.TAXI:
                case VehicleFix.REBOQUE:
                case VehicleFix.MICROONIBUS:
                case VehicleFix.CARROCA:
                case VehicleFix.ONIBUS:
                case VehicleFix.MOTO:
                    layout = (ViewGroup) inflater.inflate(R.layout.form_vehicle_data, collection, false);
                    if(vehicle.optString("placa").length()>0){
                        Pattern p = Pattern.compile("(\\w{3})(\\d{4})");
                        Matcher m = p.matcher(vehicle.optString("placa"));
                        if(m.matches()) {
                            ((TextView) layout.findViewById(R.id.placa_letras)).setText(m.group(1));
                            ((TextView) layout.findViewById(R.id.placa_numeros)).setText(m.group(2));
                        }else{
                            ((CheckedTextView)layout.findViewById(R.id.is_placa_padrao)).setChecked(false);
                            layout.findViewById(R.id.placa_padrao_layout).setVisibility(View.GONE);
                            layout.findViewById(R.id.placa_text).setVisibility(View.VISIBLE);
                            ((TextView) layout.findViewById(R.id.placa_text)).setText(vehicle.optString("placa"));
                        }
                    }
                    placaTrick(layout);
                    marcaTrick(layout);
                    ((EditText)layout.findViewById(R.id.placa_text)).setText(vehicle.optString("placa"));
                    ((EditText)layout.findViewById(R.id.marca_text_auto)).setText(vehicle.optString("marca"));
                    ((EditText)layout.findViewById(R.id.modelo_text_auto)).setText(vehicle.optString("modelo"));
                    ((EditText)layout.findViewById(R.id.municipio_text)).setText(vehicle.optString("municipio"));
                    ((TextView)layout.findViewById(R.id.tipo_veiculo_text)).setText(vehicle.optString("tipo_veiculo"));
                    layout.findViewById(R.id.veiculo_tipo_read).setVisibility(View.VISIBLE);
                    layout.findViewById(R.id.veiculo_tipo_edit).setVisibility(View.GONE);
                    String[] a = getResources().getStringArray(R.array.ufs);
                    ((Spinner)layout.findViewById(R.id.uf_spinner)).setSelection(Arrays.asList(a).indexOf(vehicle.optString("uf")));
                    final ViewGroup finalLayout = layout;
                    ((ImageButton)layout.findViewById(R.id.add_pessoa_butt)).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showPessoa(null, (ViewGroup)finalLayout.findViewById(R.id.pessoas_layout));
                        }
                    });
                    if(vehicle.has("pessoas")){
                        JSONArray pes=vehicle.optJSONArray("pessoas");
                        for(int i=0;i<pes.length();i++){
                            addPessoaResumo(pes.optJSONObject(i),(ViewGroup)finalLayout.findViewById(R.id.pessoas_layout));
                        }
                    }
                    if(vehicle.has("damage")){
                        byte[] decoded=Base64.decode(vehicle.optString("damage"),Base64.DEFAULT);
                        Bitmap db= BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                        ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageBitmap(db);
                    }else{

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            switch(vehicle.optInt("model")){
                                case VehicleFix.CAMINHAO:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageDrawable(getResources().getDrawable(R.drawable.truck_000,null));
                                    break;
                                case VehicleFix.ONIBUS:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageDrawable(getResources().getDrawable(R.drawable.bus_000,null));
                                    break;
                                case VehicleFix.MICROONIBUS:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageDrawable(getResources().getDrawable(R.drawable.microbus_000,null));
                                    break;
                                case VehicleFix.CAMINHONETE:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageDrawable(getResources().getDrawable(R.drawable.suv_000,null));
                                    break;
                                case VehicleFix.CAMIONETA:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageDrawable(getResources().getDrawable(R.drawable.camioneta_000,null));
                                    break;
                                case VehicleFix.TAXI:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageDrawable(getResources().getDrawable(R.drawable.taxi_000,null));
                                    break;
                                case VehicleFix.VIATURA:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageDrawable(getResources().getDrawable(R.drawable.viatura_000,null));
                                    break;
                                case VehicleFix.MOTO:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageDrawable(getResources().getDrawable(R.drawable.motorcycle_000,null));
                                    break;
                                case VehicleFix.REBOQUE:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageDrawable(getResources().getDrawable(R.drawable.reboque_000,null));
                                    break;
                                case VehicleFix.SEMI:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageDrawable(getResources().getDrawable(R.drawable.semi_000,null));
                                    break;
                                case VehicleFix.TRAILER:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageDrawable(getResources().getDrawable(R.drawable.trailer_000,null));
                                    break;
                                case VehicleFix.CARROCA:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageDrawable(getResources().getDrawable(R.drawable.carroca_000,null));
                                    break;
                            }
                        }else{
                            switch(vehicle.optInt("model")){
                                case VehicleFix.CAMINHAO:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageResource(R.drawable.truck_000);
                                    break;
                                case VehicleFix.ONIBUS:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageResource(R.drawable.bus_000);
                                    break;
                                case VehicleFix.MICROONIBUS:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageResource(R.drawable.microbus_000);
                                    break;
                                case VehicleFix.CAMINHONETE:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageResource(R.drawable.suv_000);
                                    break;
                                case VehicleFix.CAMIONETA:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageResource(R.drawable.camioneta_000);
                                    break;
                                case VehicleFix.TAXI:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageResource(R.drawable.taxi_000);
                                    break;
                                case VehicleFix.VIATURA:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageResource(R.drawable.viatura_000);
                                    break;
                                case VehicleFix.MOTO:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageResource(R.drawable.motorcycle_000);
                                    break;
                                case VehicleFix.REBOQUE:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageResource(R.drawable.reboque_000);
                                    break;
                                case VehicleFix.SEMI:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageResource(R.drawable.semi_000);
                                    break;
                                case VehicleFix.TRAILER:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageResource(R.drawable.trailer_000);
                                    break;
                                case VehicleFix.CARROCA:
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageResource(R.drawable.carroca_000);
                                    break;
                            }
                        }
                    }
                    if(vehicle.has("dano")){
                        RadioGroup r = (RadioGroup) finalLayout.findViewById(R.id.dano_r);
                        for(int i=0;i<r.getChildCount();i++){
                            if(((RadioButton)r.getChildAt(i)).getText().toString().equals(vehicle.optString("dano"))){
                                ((RadioButton)r.getChildAt(i)).setChecked(true);
                            }
                        }
                    }
                    final int model = vehicle.optInt("model");
                    finalLayout.findViewById(R.id.damage_image).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            final LayoutInflater inflater = getLayoutInflater();
                            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                            final View v=inflater.inflate(R.layout.damage, null);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                switch (model) {
                                    case VehicleFix.CAMINHAO:
                                        ((ImageView) v.findViewById(R.id.damage_bg)).setImageDrawable(getResources().getDrawable(R.drawable.truck_000, null));
                                        break;
                                    case VehicleFix.ONIBUS:
                                        ((ImageView) v.findViewById(R.id.damage_bg)).setImageDrawable(getResources().getDrawable(R.drawable.bus_000, null));
                                        break;
                                    case VehicleFix.MOTO:
                                        ((ImageView) v.findViewById(R.id.damage_bg)).setImageDrawable(getResources().getDrawable(R.drawable.motorcycle_000, null));
                                        break;
                                    case VehicleFix.CAMIONETA:
                                        ((ImageView) v.findViewById(R.id.damage_bg)).setImageDrawable(getResources().getDrawable(R.drawable.camioneta_000, null));
                                        break;
                                    case VehicleFix.CAMINHONETE:
                                        ((ImageView) v.findViewById(R.id.damage_bg)).setImageDrawable(getResources().getDrawable(R.drawable.suv_000, null));
                                        break;
                                    case VehicleFix.MICROONIBUS:
                                        ((ImageView) v.findViewById(R.id.damage_bg)).setImageDrawable(getResources().getDrawable(R.drawable.microbus_000, null));
                                        break;
                                    case VehicleFix.VIATURA:
                                        ((ImageView) v.findViewById(R.id.damage_bg)).setImageDrawable(getResources().getDrawable(R.drawable.viatura_000, null));
                                        break;
                                    case VehicleFix.TAXI:
                                        ((ImageView) v.findViewById(R.id.damage_bg)).setImageDrawable(getResources().getDrawable(R.drawable.taxi_000, null));
                                        break;
                                    case VehicleFix.TRAILER:
                                        ((ImageView) v.findViewById(R.id.damage_bg)).setImageDrawable(getResources().getDrawable(R.drawable.trailer_000, null));
                                        break;
                                    case VehicleFix.SEMI:
                                        ((ImageView) v.findViewById(R.id.damage_bg)).setImageDrawable(getResources().getDrawable(R.drawable.semi_000, null));
                                        break;
                                    case VehicleFix.REBOQUE:
                                        ((ImageView) v.findViewById(R.id.damage_bg)).setImageDrawable(getResources().getDrawable(R.drawable.reboque_000, null));
                                        break;
                                    case BICI:
                                        ((ImageView) v.findViewById(R.id.damage_bg)).setImageDrawable(getResources().getDrawable(R.drawable.bici000, null));
                                        break;
                                    case VehicleFix.CARROCA:
                                        ((ImageView) v.findViewById(R.id.damage_bg)).setImageDrawable(getResources().getDrawable(R.drawable.carroca_000, null));
                                        break;
                                }
                            }
                            ((Panel)v.findViewById(R.id.damage_panel)).setLigado(true);
                            ((Panel)v.findViewById(R.id.damage_panel)).setStyle(Panel.DAMAGE,40);
                            builder.setView(v).setTitle(getResources().getString(R.string.partes_amassadas));
                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    View draw = v.findViewById(R.id.damage_panel_container);
                                    draw.setWillNotCacheDrawing(false);
                                    draw.destroyDrawingCache();
                                    draw.buildDrawingCache();
                                    ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageBitmap(Bitmap.createBitmap(draw.getDrawingCache()));
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
                    });
                    break;
                case VehicleFix.PEDESTRE:
                    layout=(ViewGroup) inflater.inflate(R.layout.form_pedestre_data, collection, false);
                    ((EditText)layout.findViewById(R.id.nome_text)).setText(vehicle.optString("nome"));
                    ((EditText)layout.findViewById(R.id.idade_text)).setText(vehicle.optString("idade"));
                    RadioGroup r;
                    if(vehicle.has("ferimento")){
                        r = (RadioGroup) layout.findViewById(R.id.ferimento_r);
                        for(int i=0;i<r.getChildCount();i++){
                            if(((RadioButton)r.getChildAt(i)).getText().toString().equals(vehicle.optString("ferimento","")))
                                r.check(r.getChildAt(i).getId());
                        }
                    }
                    if(vehicle.has("sexo")){
                        r = (RadioGroup) layout.findViewById(R.id.sexo_r);
                        for(int i=0;i<r.getChildCount();i++){
                            if(((RadioButton)r.getChildAt(i)).getText().toString().equals(vehicle.optString("sexo","")))
                                r.check(r.getChildAt(i).getId());
                        }
                    }
                    break;
                case VehicleFix.COLISAO:
                    final Calendar cal = Calendar. getInstance();
                    final DateFormat dateFormat = new SimpleDateFormat("dd/M/yyyy HH:mm");
                    Date da=new Date();
                    if(vehicle.has("data_e_hora")){
                        try {
                            da=dateFormat.parse(vehicle.optString("data_e_hora"));
                        } catch (ParseException ignore) {
                        }
                    }
                    final Date date=da;
                    cal.setTime(date);
                    layout=(ViewGroup) inflater.inflate(R.layout.form_colisao_data, collection, false);
                    int c= Arrays.asList(getResources().getStringArray(R.array.impact_type)).indexOf(vehicle.optString("tipo_impacto"));
                    if(c>=0)
                        ((Spinner)layout.findViewById(R.id.impacto_spinner)).setSelection(c);
                    ((EditText)layout.findViewById(R.id.description)).setText(vehicle.optString("descricao"));
                    String inv="";
                    if(vehicle.has("envolvidos"))
                        inv=vehicle.optJSONArray("envolvidos").toString();
                    final Button dh=((Button)layout.findViewById(R.id.data_e_hora));
                    dh.setText(vehicle.optString("data_e_hora",dateFormat.format(date)));
                    for(int i=0;i<vehicles.length();i++){
                        JSONObject vc = vehicles.optJSONObject(i);
                        if(vc.optInt("model")!=VehicleFix.COLISAO) {
                            CheckBox cc = new CheckBox(context);
                            String s=getVehicleDescription(vc);
                            if(inv.contains(s))
                                cc.setChecked(true);
                            cc.setText(s);
                            ((ViewGroup)layout.findViewById(R.id.itens_envolvidos)).addView(cc);
                        }
                    }
                    dh.setText(dateFormat.format(date));
                    dh.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            AlertDialog.Builder bu = new AlertDialog.Builder(context);
                            LayoutInflater inflater = getLayoutInflater();
                            final View l = inflater.inflate(R.layout.dialog_datetimepicker, null);
                            ((DatePicker)l.findViewById(R.id.datepicker)).init(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), new DatePicker.OnDateChangedListener() {
                                @Override
                                public void onDateChanged(DatePicker datePicker, int i, int i1, int i2) {
                                    l.findViewById(R.id.datepicker).setVisibility(View.GONE);
                                    l.findViewById(R.id.timepicker).setVisibility(View.VISIBLE);
                                    l.findViewById(R.id.datetime_confirm).setVisibility(View.VISIBLE);
                                }
                            });
                            ((TimePicker)l.findViewById(R.id.timepicker)).setCurrentHour(cal.get(Calendar.HOUR));
                            ((TimePicker)l.findViewById(R.id.timepicker)).setCurrentMinute(cal.get(Calendar.MINUTE));

                            bu.setView(l);
                            final AlertDialog al = bu.create();
                            l.findViewById(R.id.datetime_confirm).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    DatePicker d=(DatePicker)l.findViewById(R.id.datepicker);
                                    TimePicker t=(TimePicker) l.findViewById(R.id.timepicker);
                                    cal.set(d.getYear(),d.getMonth(),d.getDayOfMonth(),t.getCurrentHour(),t.getCurrentMinute());
                                    dh.setText(dateFormat.format(cal.getTime()));
                                    al.dismiss();
                                }
                            });
                            al.show();
                        }
                    });
                    break;
                case VehicleFix.OBSTACULO:
                    layout=(ViewGroup) inflater.inflate(R.layout.form_obstaculo_data, collection, false);
                    ((EditText)layout.findViewById(R.id.tipo_obstaculo_text)).setText(vehicle.optString("nome"));
                    ((EditText)layout.findViewById(R.id.largura_text)).setText(vehicle.optString("largura"));
                    ((EditText)layout.findViewById(R.id.comprimento_text)).setText(vehicle.optString("comprimento"));
                    break;
                case BICI:
                    layout=(ViewGroup) inflater.inflate(R.layout.form_bici_data, collection, false);
                    ((EditText)layout.findViewById(R.id.marca_text)).setText(vehicle.optString("marca"));
                    final ViewGroup finalLayoutbi = layout;
                    ((ImageButton)layout.findViewById(R.id.add_pessoa_butt)).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showPessoa(null, (ViewGroup)finalLayoutbi.findViewById(R.id.pessoas_layout));
                        }
                    });
                    if(vehicle.has("pessoas")){
                        JSONArray pes=vehicle.optJSONArray("pessoas");
                        for(int i=0;i<pes.length();i++){
                            addPessoaResumo(pes.optJSONObject(i),(ViewGroup)finalLayoutbi.findViewById(R.id.pessoas_layout));
                        }
                    }

                    break;
            }
            if(vehicle.has("fatores_contribuintes")){
                ViewGroup fg= (ViewGroup) layout.findViewById(R.id.fatores_contribuintes_layout);
                int j=0;
                JSONArray fca=vehicle.optJSONArray("fatores_contribuintes");
                for(int i=0;i<fg.getChildCount();i++){
                    View fu= fg.getChildAt(i);
                    try{
                        CheckBox ch=(CheckBox)fu;
                        ch.setChecked(fca.optBoolean(j));
                        j++;
                    }catch(ClassCastException xu){}
                }
            }
            if(vehicle.has("tipo_veiculo_id")){
                int vid=vehicle.optInt("tipo_veiculo_id");
                Spinner s= (Spinner) layout.findViewById(R.id.tipo_veiculo_spinner);
                if(s!=null) s.setSelection(vid);
            }
            if(layout!=null) {
                final ViewGroup finalLayout = layout;

                layout.findViewById(R.id.voltar_butt).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        findViewById(R.id.vehicle_details).setVisibility(View.GONE);
                        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
                        setSupportActionBar(toolbar);
                        toolbar.setVisibility(View.VISIBLE);
                    }
                });
                layout.findViewById(R.id.delete_butt).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        JSONObject vehicle = vehicles.optJSONObject(position);
                        ViewGroup c = (ViewGroup) findViewById(R.id.vehicles_canvas);
                        View vu = null;
                        for (int i = 0; i < c.getChildCount(); i++) {
                            if (((VehicleFix) c.getChildAt(i)).getVehicleId() == vehicle.optInt("view_id")) {
                                vu = c.getChildAt(i);
                            }
                        }
                        if (vu != null)
                            c.removeView(vu);
                        vehicles.remove(position);
                        c.invalidate();
                        findViewById(R.id.vehicle_details).setVisibility(View.GONE);
                        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
                        setSupportActionBar(toolbar);
                        toolbar.setVisibility(View.VISIBLE);
                        setSelectedVehicle(null);
                    }
                });
                layout.findViewById(R.id.ok_butt).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            JSONObject vehicle = vehicles.optJSONObject(position);
                            switch (vehicle.optInt("model")) {
                                case VehicleFix.AUTO:
                                case VehicleFix.CAMINHAO:
                                case VehicleFix.ONIBUS:
                                case VehicleFix.MOTO:
                                case VehicleFix.MICROONIBUS:
                                case VehicleFix.REBOQUE:
                                case VehicleFix.SEMI:
                                case VehicleFix.VIATURA:
                                case VehicleFix.TAXI:
                                case VehicleFix.CAMINHONETE:
                                case VehicleFix.CAMIONETA:
                                case VehicleFix.CARROCA:
                                case VehicleFix.TRAILER:
                                    if (((CheckedTextView) finalLayout.findViewById(R.id.is_placa_padrao)).isChecked()) {
                                        vehicle.put("placa", String.format("%s%s", new String[]{((EditText) finalLayout.findViewById(R.id.placa_letras)).getText().toString(), ((EditText) finalLayout.findViewById(R.id.placa_numeros)).getText().toString()}));
                                    } else {
                                        vehicle.put("placa", ((EditText) finalLayout.findViewById(R.id.placa_text)).getText());
                                    }
                                    vehicle.put("marca", ((EditText) finalLayout.findViewById(R.id.marca_text_auto)).getText());
                                    vehicle.put("modelo", ((EditText) finalLayout.findViewById(R.id.modelo_text_auto)).getText());
                                    vehicle.put("municipio", ((EditText) finalLayout.findViewById(R.id.municipio_text)).getText());
                                    vehicle.put("uf", ((Spinner) finalLayout.findViewById(R.id.uf_spinner)).getSelectedItem().toString());
                                    ViewGroup pes = ((ViewGroup) finalLayout.findViewById(R.id.pessoas_layout));
                                    JSONArray ja = new JSONArray();
                                    for (int i = 0; i < pes.getChildCount(); i++) {
                                        ja.put(new JSONObject(String.valueOf(((TextView) pes.getChildAt(i).findViewById(R.id.pessoa_data)).getText())));
                                    }
                                    vehicle.put("pessoas", ja);
                                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                    View damaged = finalLayout.findViewById(R.id.damage_image);
                                    damaged.setWillNotCacheDrawing(false);
                                    damaged.destroyDrawingCache();
                                    damaged.buildDrawingCache();
                                    Bitmap bo = damaged.getDrawingCache();
                                    bo.compress(Bitmap.CompressFormat.PNG, 90, baos);
                                    byte[] b = baos.toByteArray();
                                    if (Debug.isDebuggerConnected()) {
                                        try {
                                            bo.compress(Bitmap.CompressFormat.PNG, 95, new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/damage.png"));
                                        } catch (FileNotFoundException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    vehicle.put("damage", Base64.encodeToString(b, Base64.DEFAULT).replaceAll("\\n", ""));
                                    int rid = ((RadioGroup) finalLayout.findViewById(R.id.dano_r)).getCheckedRadioButtonId();
                                    vehicle.remove("dano");
                                    if (rid >= 0)
                                        vehicle.put("dano", ((RadioButton) finalLayout.findViewById(rid)).getText().toString());
                                    break;
                                case BICI:
                                    vehicle.put("marca", ((EditText) finalLayout.findViewById(R.id.marca_text)).getText());
                                    pes = ((ViewGroup) finalLayout.findViewById(R.id.pessoas_layout));
                                    ja = new JSONArray();
                                    for (int i = 0; i < pes.getChildCount(); i++) {
                                        ja.put(new JSONObject(String.valueOf(((TextView) pes.getChildAt(i).findViewById(R.id.pessoa_data)).getText())));
                                    }
                                    vehicle.put("pessoas", ja);
                                    break;
                                case VehicleFix.PEDESTRE:
                                    vehicle.put("nome", ((EditText) finalLayout.findViewById(R.id.nome_text)).getText());
                                    vehicle.put("idade", ((EditText) finalLayout.findViewById(R.id.idade_text)).getText());
                                    int f = ((RadioGroup) finalLayout.findViewById(R.id.sexo_r)).getCheckedRadioButtonId();
                                    if (f >= 0)
                                        vehicle.put("sexo", ((RadioButton) ((RadioGroup) finalLayout.findViewById(R.id.sexo_r)).findViewById(f)).getText());
                                    f = ((RadioGroup) finalLayout.findViewById(R.id.ferimento_r)).getCheckedRadioButtonId();
                                    if (f >= 0)
                                        vehicle.put("ferimento", ((RadioButton) ((RadioGroup) finalLayout.findViewById(R.id.ferimento_r)).findViewById(f)).getText());
                                    break;
                                case VehicleFix.COLISAO:
                                    vehicle.put("tipo_impacto", ((Spinner) finalLayout.findViewById(R.id.impacto_spinner)).getSelectedItem().toString());
                                    vehicle.put("descricao", ((EditText) finalLayout.findViewById(R.id.description)).getText().toString());
                                    vehicle.put("data_e_hora", ((Button)finalLayout.findViewById(R.id.data_e_hora)).getText());
                                    ViewGroup vu = (ViewGroup) finalLayout.findViewById(R.id.itens_envolvidos);
                                    JSONArray involved = new JSONArray();
                                    for (int i = 0; i < vu.getChildCount(); i++) {
                                        if (((CheckBox) vu.getChildAt(i)).isChecked()) {
                                            String nam = (String) ((CheckBox) vu.getChildAt(i)).getText();
                                            involved.put(nam);
                                        }
                                    }
                                    vehicle.put("envolvidos", involved);
                                    break;
                                case VehicleFix.OBSTACULO:
                                    vehicle.put("nome", ((EditText) finalLayout.findViewById(R.id.tipo_obstaculo_text)).getText().toString());
                                    break;
                            }
                            if (finalLayout.findViewById(R.id.fatores_contribuintes_layout) != null) {
                                JSONArray fc = new JSONArray();
                                JSONArray ft = new JSONArray();
                                ViewGroup fg = (ViewGroup) finalLayout.findViewById(R.id.fatores_contribuintes_layout);
                                for (int i = 0; i < fg.getChildCount(); i++) {
                                    View fu = fg.getChildAt(i);
                                    try {
                                        fc.put(((CheckBox) fu).isChecked());
                                        if (((CheckBox) fu).isChecked()) {
                                            ft.put(((CheckBox) fu).getText().toString());
                                        }
                                    } catch (ClassCastException xu) {
                                    }
                                }
                                vehicle.put("fatores_contribuintes", fc);
                                vehicle.put("fatores_contribuintes_text", ft);
                            }
                            vehicles.put(position, vehicle);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        findViewById(R.id.vehicle_details).setVisibility(View.GONE);
                        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
                        setSupportActionBar(toolbar);
                        toolbar.setVisibility(View.VISIBLE);
                        View v = getCurrentFocus();
                        if (v != null) {
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        }
                        setSelectedVehicle(getSelectedVehicle());
                    }
                });
                collection.addView(layout);
            }
            return layout;
        }
        @Override
        public void destroyItem(ViewGroup collection, int position, Object view) {
            collection.removeView((View) view);
        }
    }

    private JSONObject getVehicleByLabel(String label) {
        for(int i=0;i<vehicles.length();i++){
            if(label.equals(vehicles.optJSONObject(i).optString("label"))){
                return vehicles.optJSONObject(i);
            }
        }
        return null;
    }

    private ArrayAdapter<String> getMarcas(){
        modelos=new Hashtable<>();
        modelos.put(getString(R.string.nao_identificado), new ArrayList());
        todos=new ArrayList();
        todos.add(getString(R.string.nao_identificado));
        try {
            Resources res = getResources();
            InputStream in_s = res.openRawResource(R.raw.marcas);
            byte[] b = new byte[in_s.available()];
            in_s.read(b);
            JSONObject jm = new JSONObject(new String(b));
            List<String> mks=new ArrayList<>();
            mks.add(getString(R.string.nao_identificada));
            Iterator<?> keys = jm.keys();
            while( keys.hasNext() ) {
                String key = (String)keys.next();
                String nome= jm.optJSONObject(key).optString("NOME");
                mks.add(nome);
                if(!modelos.containsKey(nome)){
                    modelos.put(nome,new ArrayList());
                    modelos.get(nome).add(getString(R.string.nao_identificado));
                }
                JSONArray m=jm.optJSONObject(key).optJSONArray("MODELS");
                for(int i=0;i<m.length();i++){
                    modelos.get(nome).add(m.optString(i));
                    todos.add(m.optString(i));
                }
            }
            return new ArrayAdapter<String>
                    (this, android.R.layout.select_dialog_item, mks);
        }catch (JSONException e) {
            return null;
        } catch (IOException e1) {
            return null;
        }
    }
    private ArrayAdapter<String> getModelos(String marca){
        if(modelos.containsKey(marca)) {
            return new ArrayAdapter<String>
                    (this, android.R.layout.select_dialog_item, modelos.get(marca));
        }else{
            return new ArrayAdapter<String>
                    (this, android.R.layout.select_dialog_item, todos);
        }
    }
    private String getExternalPath() {
        String dir = Environment.getExternalStorageDirectory()
                .getAbsolutePath();
        if (android.os.Build.DEVICE.contains("samsung")
                || android.os.Build.MANUFACTURER.contains("samsung")) {
            File f = new File(Environment.getExternalStorageDirectory()
                    .getParent() + "/extSdCard" + "/myDirectory");
            if (f.exists() && f.isDirectory()) {
                dir = Environment.getExternalStorageDirectory()
                        .getParent() + "/extSdCard";
            } else {
                f = new File(Environment.getExternalStorageDirectory()
                        .getAbsolutePath() + "/external_sd" + "/myDirectory");
                if (f.exists() && f.isDirectory()) {
                    dir= Environment
                            .getExternalStorageDirectory().getAbsolutePath()
                            + "/external_sd";
                }
            }
        }
        return dir;

    }
}
