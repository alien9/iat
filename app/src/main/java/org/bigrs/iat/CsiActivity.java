package org.bigrs.iat;

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
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.preference.PreferenceManager;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.apache.commons.lang3.ArrayUtils;
import org.bigrs.iat.util.VehicleFix;
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

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bigrs.iat.util.Pega;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Iat iat = (Iat) getApplicationContext();
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)||(ActivityCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)) {
            Intent intent=new Intent(this,PermissionRequest.class);
            Bundle b=getIntent().getExtras();
            if(b!=null)
                intent.putExtras(b);
            startActivity(intent);
            return;
        }
        iat.startGPS(this);
        setContentView(R.layout.csi);
        float ls = convertDpToPixel(LABEL_SIZE);
        labelOffset=new int[]{
                (int) (-1*ls/2), (int) ls
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
        String u="http://bigrs.alien9.net:8080/geoserver/gwc/service/tms/1.0.0/";
        clear_source = new GeoServerTileSource("quadras_e_logradouros", 17, 21, 512, ".png", new String[]{u});
        great_source = new GeoServerTileSource("cidade_com_semaforos_e_lotes", 17, 21, 512, ".png", new String[]{u});
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
            }catch (JSONException ignore) {
            }catch(NumberFormatException ignore){}
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
        if(intent.hasExtra("info")){
            try{
                setCurrentMode(MAP);
                setCurrentMode(VEHICLES);
                Log.d("IAT recebe parâmetros",intent.getStringExtra("info"));
                JSONObject j = new JSONObject(intent.getStringExtra("info"));
                paths=j.optJSONArray("paths");
                if(j.has("latitude")){
                    point.put("latitude",j.optDouble("latitude"));
                }
                if(j.has("longitude")){
                    point.put("longitude",j.optDouble("longitude"));
                }
                if(j.has("longitude")&&j.has("llatitude"))
                    map.getController().setCenter(new GeoPoint(point.optDouble("latitude"), point.optDouble("longitude")));
                setVehicles(j.optJSONArray("vehicles"));
                reloadVehiclesAndPaths();
                ligaCarros(true);
            } catch (JSONException e) {}
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
                //showLabels(true);
                refresh();
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
                    case R.id.tools_veiculo:
                        plot(R.layout.fields_vehicle);
                        break;
                    case R.id.tools_carro:
                        createVehicle(VehicleFix.AUTO,1.9,3.8);
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
                        createVehicle(VehicleFix.BICI,1.8,2.2);
                        break;
                    case R.id.tools_pessoa:
                        createVehicle(VehicleFix.PEDESTRE,1.7,2.5);
                        break;
                    case R.id.tools_colisao:
                        createVehicle(VehicleFix.COLISAO,4.0,4.0);
                        break;
                    case R.id.tools_obstaculo:
                        plot(R.layout.fields_obstaculo);
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
                    case R.id.exit_command:
                        Intent data=new Intent();
                        savePaths();
                        saveVehicles();
                        JSONObject o=new JSONObject();
                        try {
                            JSONObject dj=new JSONObject();
                            o = getPicture();
                            dj.put("vehicles",vehicles);
                            dj.put("paths",paths);
                            o.put("info",dj);
                        } catch (JSONException ignore) {
                        }
                        data.putExtra("data",o.toString());
                        Log.d("IAT send result", "enviando croqui para o eGO");
                        setResult(RESULT_OK, data);
                        finish();
                        break;
                }
                findViewById(R.id.show_pallette).setVisibility(View.VISIBLE);
                findViewById(R.id.palette_layout).setVisibility(View.GONE);
            }
        };
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

        if(vehicles.length()==0){
            ((RadioButton)findViewById(R.id.radio_mapa)).setChecked(true);
        }
        findViewById(R.id.pegador).setVisibility(View.VISIBLE);
        ((Pega) findViewById(R.id.pegador)).setPontaPosition(-10000,-10000,0);
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
        if((vehicles.length()>0)||(paths.length()>0))
            refresh();//((RadioButton)findViewById(R.id.radio_desenho)).setChecked(true);
    }

    private void refresh() {
        reloadVehiclesAndPaths();
        //savePaths();
        //saveVehicles();
        setCurrentMode(VEHICLES);
        new android.os.Handler().postDelayed(
            new Runnable() {
                public void run() {
                    setCurrentMode(MAP);
                }
            },
        1000);
        //((RadioButton)findViewById(R.id.radio_desenho)).setChecked(true);
        //((RadioButton)findViewById(R.id.radio_mapa)).setChecked(true);
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
                    p.put("ano_de_nascimento",((EditText)v.findViewById(R.id.ano_nasc)).getText());
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

        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which){
                dialog.dismiss();
            }
        });
        builder.create().show();
        if(pessoa_detalhe!=null){
            try {
                JSONObject pessoa=new JSONObject((String) ((TextView)pessoa_detalhe.findViewById(R.id.pessoa_data)).getText());
                ((EditText)v.findViewById(R.id.nome_text)).setText(pessoa.optString("nome"));
                ((EditText)v.findViewById(R.id.ano_nasc)).setText(pessoa.optString("ano_de_nascimento"));
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

    private void plot(int t) {
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
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which){
                switch(tipo){
                    case R.layout.fields_obstaculo:
                        try {
                            int largura = Integer.parseInt(((TextView) v.findViewById(R.id.largura_text)).getText().toString());
                            int comprimento = Integer.parseInt(((TextView) v.findViewById(R.id.comprimento_text)).getText().toString());
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
                                placa=String.format("%s%s",new String[]{
                                        String.valueOf(((EditText)v.findViewById(R.id.placa_letras)).getText()),
                                        String.valueOf(((EditText)v.findViewById(R.id.placa_numeros)).getText())
                                });
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
                                    createVehicle(VehicleFix.CAMINHONETE,3.9,11.4,d);
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
                                    createVehicle(VehicleFix.SEMI,4.1,22.4,d);
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
                            d.put("tipo_impacto_id",((Spinner)v.findViewById(R.id.impacto_spinner)).getSelectedItemPosition());
                            d.put("tipo_impacto",String.valueOf(((Spinner)v.findViewById(R.id.impacto_spinner)).getSelectedItem()));
                        } catch (JSONException ignored) {}
                        createVehicle(VehicleFix.COLISAO,4.0,4.0,d);
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
        switch(tipo){
            case R.layout.fields_obstaculo:
                final EditText xu = (EditText) v.findViewById(R.id.largura_text);
                xu.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        double valor=Double.parseDouble(charSequence.toString());
                        if(valor<0)
                            xu.setText("1");
                        if(valor>8)
                            xu.setText("8");
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
        builder.create().show();
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
                for (int i = start; i < end; i++) {
                    if (!Character.isLetter(source.charAt(i))) {
                        return "";
                    }
                }
                return null;
            }
        };
        ((EditText)v.findViewById(R.id.placa_letras)).setFilters(new InputFilter[]{filter});
    }

    private String getLabel(JSONObject v) {
        switch(v.optInt("model")){
            case VehicleFix.PEDESTRE:
                return getNextPedestrianLabel();
            case VehicleFix.OBSTACULO:
                return getNextObstacleLabel();
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
                VehicleFix.BICI,
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
        //menu.findItem(R.id.labels).setChecked(show_labels);
        //menu.findItem(R.id.semaforos).setChecked(show_semaforos);
        //menu.findItem(R.id.mode_map).setChecked(current_mode==MAP);
        //menu.findItem(R.id.mode_freehand).setChecked(current_mode==FREEHAND);
        //menu.findItem(R.id.mode_vehicles).setChecked(current_mode==VEHICLES);
        //int z = ((MapView) findViewById(R.id.map)).getZoomLevel();
        //menu.findItem(R.id.mode_freehand).setEnabled(z>19);
        //menu.findItem(R.id.mode_vehicles).setEnabled(z>19);
        //menu.findItem(R.id.tombar_veiculo).setVisible(getSelectedVehicle()!=null);
        //menu.findItem(R.id.reset_veiculo).setVisible(current_mode!=MAP);
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
                createVehicle(VehicleFix.BICI,1.8,2.2,d);
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
            case R.id.new_path:
                startDraw(Panel.TRACK);
                break;
            case R.id.new_impact:
                plot(R.layout.fields_colisao);
                break;
            case R.id.move_map:
                ((RadioButton)findViewById(R.id.radio_mapa)).setChecked(true);
                break;
            case R.id.move_car:
                ((RadioButton)findViewById(R.id.radio_desenho)).setChecked(true);
                break;
            case R.id.end:
                MapView map=((MapView)findViewById(R.id.map));
                savePaths();
                saveVehicles();
                Intent data=new Intent();
                JSONObject o=new JSONObject();
                try {
                    JSONObject dj=new JSONObject();
                    o = getPicture();
                    dj.put("vehicles",vehicles);
                    dj.put("paths",paths);
                    dj.put("zoom",map.getZoomLevel());
                    dj.put("latitude",map.getMapCenter().getLatitude());
                    dj.put("longitude",map.getMapCenter().getLongitude());
                    o.put("info",dj);
                } catch (JSONException ignore) {}
                data.putExtra("data",o.toString());
                Log.d("IAT send result", "enviando croqui para o eGO");
                setResult(RESULT_OK, data);
                finish();
                break;

            case R.id.center_here:
                map = (MapView) findViewById(R.id.map);
                JSONObject point = ((Iat) getApplicationContext()).getLastKnownPosition();
                if (point.has("latitude")) {
                    map.getController().setCenter(new GeoPoint(point.optDouble("latitude"), point.optDouble("longitude")));
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
            TextView label= (TextView) fu.findViewById(R.id.vehicle_label_text);
            pegador.setPontaPosition(bode.getX()+bode.getWidth()/2, bode.getY()+bode.getHeight()/2, fu.findViewById(R.id.vehicle_body).getRotation());
            label.setX(bode.getX()+bode.getWidth()/2+labelOffset[0]);
            label.setY(bode.getY()+bode.getHeight()/2+labelOffset[1]);
            pegador.invalidate();
        }

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
        label.setX(ponta[0]+labelOffset[0]);
        label.setY(ponta[1]+labelOffset[1]);
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
            int xid = ((VehicleFix) sv).getVehicleId();
            JSONObject veiculo = getVehicleById(xid);
            findViewById(R.id.edit_vehicle_rotate).setVisibility(View.GONE);
            String label = "";
            if(veiculo.has("label")){
                label=veiculo.optString("label")+" - ";
            }
            switch(veiculo.optInt("model")){
                case VehicleFix.OBSTACULO:
                    ((TextView) findViewById(R.id.vehicle_type_text)).setText(label+veiculo.optString("nome"));
                    findViewById(R.id.edit_vehicle_rotate).setVisibility(View.GONE);
                default:
                    if(veiculo.has("tipo_veiculo")) {
                        ((TextView) findViewById(R.id.vehicle_type_text)).setText(label+veiculo.optString("tipo_veiculo"));
                        findViewById(R.id.edit_vehicle_rotate).setVisibility(View.VISIBLE);
                    }else if(veiculo.has("tipo_impacto")){
                        ((TextView) findViewById(R.id.vehicle_type_text)).setText(veiculo.optString("tipo_impacto"));
                    }else if(veiculo.optInt("model")==VehicleFix.PEDESTRE){
                        ((TextView) findViewById(R.id.vehicle_type_text)).setText(getResources().getString(R.string.pessoa));
                    }
                    break;
            }
            ((TextView)findViewById(R.id.vehicle_id_text)).setText(""+xid);
            findViewById(R.id.info_box).setVisibility(View.VISIBLE);
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
            Double[] p1 = degrees2meters(b.getLonEast(), b.getLatSouth());
            Double[] p2 = degrees2meters(b.getLonWest(), b.getLatNorth());
            Double[] p3 = degrees2meters(b.getLonWest(), b.getLatSouth());
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
        View v=new VehicleFix(context);
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
        } catch (JSONException e) {}
        label.setText(veiculo.optString("label"));
        label.setY((size.y-pix)/2+labelOffset[1]);
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
            TextView label = (TextView) v.findViewById(R.id.vehicle_label_text);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) Math.round(w), (int) Math.round(l));
            body.setLayoutParams(params);
            float pix = convertDpToPixel(150);
            if(position!=null) {
                chassi.setX(position.x-pix);
                chassi.setY(position.y-pix);
            }
            label.setText(vehicle.optString("label"));
            label.setX(position.x+labelOffset[0]);
            label.setY(position.y+labelOffset[1]);

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
            // cria os veiculos
            createVehicle(veiculo.optInt("model"),veiculo.optDouble("width"),veiculo.optDouble("length"),veiculo);
        }
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
                        switch(vehicle.optInt("model")){
                            case VehicleFix.CAMINHAO:
                                ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageDrawable(getResources().getDrawable(R.drawable.truck_000,null));
                                break;
                            case VehicleFix.ONIBUS:
                                ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageDrawable(getResources().getDrawable(R.drawable.bus_000,null));
                                break;
                            case VehicleFix.MOTO:
                                ((ImageView)finalLayout.findViewById(R.id.damage_image)).setImageDrawable(getResources().getDrawable(R.drawable.motorcycle_000,null));
                                break;
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
                            switch(model){
                                case VehicleFix.CAMINHAO:
                                    ((ImageView)v.findViewById(R.id.damage_bg)).setImageDrawable(getResources().getDrawable(R.drawable.truck_000,null));
                                    break;
                                case VehicleFix.ONIBUS:
                                    ((ImageView)v.findViewById(R.id.damage_bg)).setImageDrawable(getResources().getDrawable(R.drawable.bus_000,null));
                                    break;
                                case VehicleFix.MOTO:
                                    ((ImageView)v.findViewById(R.id.damage_bg)).setImageDrawable(getResources().getDrawable(R.drawable.motorcycle_000,null));
                                    break;

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
                    ((EditText)layout.findViewById(R.id.ano_nasc)).setText(vehicle.optString("ano_nascimento"));
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
                    layout=(ViewGroup) inflater.inflate(R.layout.form_colisao_data, collection, false);
                    int c= Arrays.asList(getResources().getStringArray(R.array.impact_type)).indexOf(vehicle.optString("tipo_impacto"));
                    if(c>=0)
                        ((Spinner)layout.findViewById(R.id.impacto_spinner)).setSelection(c);
                    ((EditText)layout.findViewById(R.id.description)).setText(vehicle.optString("descricao"));
                    String inv="";
                    if(vehicle.has("envolvidos"))
                        inv=vehicle.optJSONArray("envolvidos").toString();
                    //if(inv==null) inv=new JSONArray();
                    for(int i=0;i<vehicles.length();i++){
                        JSONObject vc = vehicles.optJSONObject(i);
                        if(vc.has("label")) {
                            CheckBox cc = new CheckBox(context);
                            if(inv.contains("\""+vc.optString("label")+"\""))
                                cc.setChecked(true);
                            cc.setText(vc.optString("label"));
                            ((ViewGroup)layout.findViewById(R.id.itens_envolvidos)).addView(cc);
                        }
                    }
                    break;
                case VehicleFix.OBSTACULO:
                    layout=(ViewGroup) inflater.inflate(R.layout.form_obstaculo_data, collection, false);
                    ((EditText)layout.findViewById(R.id.tipo_obstaculo_text)).setText(vehicle.optString("nome"));
                    ((EditText)layout.findViewById(R.id.largura_text)).setText(vehicle.optString("largura"));
                    ((EditText)layout.findViewById(R.id.comprimento_text)).setText(vehicle.optString("comprimento"));
                    break;
                case VehicleFix.BICI:
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

            final ViewGroup finalLayout = layout;

            layout.findViewById(R.id.voltar_butt).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    findViewById(R.id.vehicle_details).setVisibility(View.GONE);
                    Toolbar toolbar=(Toolbar)findViewById(R.id.my_toolbar);
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
                    for(int i=0;i<c.getChildCount();i++){
                        if(((VehicleFix)c.getChildAt(i)).getVehicleId()==vehicle.optInt("view_id")){
                            vu=c.getChildAt(i);
                        }
                    }
                    if(vu!=null)
                        c.removeView(vu);
                    vehicles.remove(position);
                    c.invalidate();
                    findViewById(R.id.vehicle_details).setVisibility(View.GONE);
                    Toolbar toolbar=(Toolbar)findViewById(R.id.my_toolbar);
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
                                if(((CheckedTextView)finalLayout.findViewById(R.id.is_placa_padrao)).isChecked()){
                                    vehicle.put("placa", String.format("%s%s",new String[]{((EditText) finalLayout.findViewById(R.id.placa_letras)).getText().toString(),((EditText) finalLayout.findViewById(R.id.placa_numeros)).getText().toString()}));
                                }else {
                                    vehicle.put("placa", ((EditText) finalLayout.findViewById(R.id.placa_text)).getText());
                                }
                                vehicle.put("marca", ((EditText) finalLayout.findViewById(R.id.marca_text_auto)).getText());
                                vehicle.put("modelo", ((EditText) finalLayout.findViewById(R.id.modelo_text_auto)).getText());
                                vehicle.put("municipio", ((EditText) finalLayout.findViewById(R.id.municipio_text)).getText());
                                vehicle.put("uf", ((Spinner) finalLayout.findViewById(R.id.uf_spinner)).getSelectedItem().toString());
                                ViewGroup pes = ((ViewGroup) finalLayout.findViewById(R.id.pessoas_layout));
                                JSONArray ja=new JSONArray();
                                for(int i=0;i<pes.getChildCount();i++){
                                    ja.put(new JSONObject(String.valueOf(((TextView)pes.getChildAt(i).findViewById(R.id.pessoa_data)).getText())));
                                }
                                vehicle.put("pessoas",ja);
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                View damaged=finalLayout.findViewById(R.id.damage_image);
                                damaged.setWillNotCacheDrawing(false);
                                damaged.destroyDrawingCache();
                                damaged.buildDrawingCache();
                                Bitmap bo = damaged.getDrawingCache();
                                bo.compress(Bitmap.CompressFormat.PNG, 90, baos);
                                byte[] b = baos.toByteArray();
                                if(Debug.isDebuggerConnected()) {
                                    try {
                                        bo.compress(Bitmap.CompressFormat.PNG, 95, new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/damage.png"));
                                    } catch (FileNotFoundException e) {
                                        e.printStackTrace();
                                    }
                                }
                                vehicle.put("damage",Base64.encodeToString(b, Base64.DEFAULT).replaceAll("\\n",""));
                                int rid=((RadioGroup) finalLayout.findViewById(R.id.dano_r)).getCheckedRadioButtonId();
                                vehicle.remove("dano");
                                if(rid>=0)
                                    vehicle.put("dano", ((RadioButton)finalLayout.findViewById(rid)).getText().toString());
                                break;
                            case VehicleFix.BICI:
                                vehicle.put("marca", ((EditText) finalLayout.findViewById(R.id.marca_text)).getText());
                                pes = ((ViewGroup) finalLayout.findViewById(R.id.pessoas_layout));
                                ja=new JSONArray();
                                for(int i=0;i<pes.getChildCount();i++){
                                    ja.put(new JSONObject(String.valueOf(((TextView)pes.getChildAt(i).findViewById(R.id.pessoa_data)).getText())));
                                }
                                vehicle.put("pessoas",ja);
                                break;
                            case VehicleFix.PEDESTRE:
                                vehicle.put("nome", ((EditText) finalLayout.findViewById(R.id.nome_text)).getText());
                                vehicle.put("ano_nascimento", ((EditText) finalLayout.findViewById(R.id.ano_nasc)).getText());
                                int f = ((RadioGroup) finalLayout.findViewById(R.id.sexo_r)).getCheckedRadioButtonId();
                                if(f>=0)
                                    vehicle.put("sexo", ((RadioButton)((RadioGroup)finalLayout.findViewById(R.id.sexo_r)).findViewById(f)).getText());
                                f=((RadioGroup)finalLayout.findViewById(R.id.ferimento_r)).getCheckedRadioButtonId();
                                if(f>=0)
                                    vehicle.put("ferimento", ((RadioButton)((RadioGroup)finalLayout.findViewById(R.id.ferimento_r)).findViewById(f)).getText());
                                break;
                            case VehicleFix.COLISAO:
                                vehicle.put("tipo_impacto",((Spinner)finalLayout.findViewById(R.id.impacto_spinner)).getSelectedItem().toString());
                                vehicle.put("descricao",((EditText)finalLayout.findViewById(R.id.description)).getText().toString());
                                ViewGroup vu= (ViewGroup) finalLayout.findViewById(R.id.itens_envolvidos);
                                JSONArray involved=new JSONArray();
                                Pattern p = Pattern.compile("^\\w+");
                                for(int i=0;i<vu.getChildCount();i++){
                                    if(((CheckBox)vu.getChildAt(i)).isChecked()) {
                                        String nam = (String) ((CheckBox) vu.getChildAt(i)).getText();
                                        String label = nam.substring(0);
                                        Matcher m = p.matcher(nam);
                                        if (m.matches())
                                            label = m.group();
                                        JSONObject ve = getVehicleByLabel(label);
                                        if (ve != null) {
                                            involved.put(label);
                                        }
                                    }
                                }
                                vehicle.put("envolvidos",involved);
                                break;
                            case VehicleFix.OBSTACULO:

                                break;
                        }
                        if(finalLayout.findViewById(R.id.fatores_contribuintes_layout)!=null){
                            JSONArray fc=new JSONArray();
                            JSONArray ft=new JSONArray();
                            ViewGroup fg= (ViewGroup) finalLayout.findViewById(R.id.fatores_contribuintes_layout);
                            for(int i=0;i<fg.getChildCount();i++){
                                View fu= fg.getChildAt(i);
                                try{
                                    fc.put(((CheckBox)fu).isChecked());
                                    if(((CheckBox)fu).isChecked()){
                                        ft.put(((CheckBox)fu).getText().toString());
                                    }
                                }catch(ClassCastException xu){}
                            }
                            vehicle.put("fatores_contribuintes",fc);
                            vehicle.put("fatores_contribuintes_text",ft);
                        }
                        vehicles.put(position,vehicle);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    findViewById(R.id.vehicle_details).setVisibility(View.GONE);
                    Toolbar toolbar=(Toolbar)findViewById(R.id.my_toolbar);
                    setSupportActionBar(toolbar);
                    toolbar.setVisibility(View.VISIBLE);
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
        todos=new ArrayList();
        try {
            Resources res = getResources();
            InputStream in_s = res.openRawResource(R.raw.marcas);
            byte[] b = new byte[in_s.available()];
            in_s.read(b);
            JSONObject jm = new JSONObject(new String(b));
            List<String> mks=new ArrayList<>();
            Iterator<?> keys = jm.keys();
            while( keys.hasNext() ) {
                String key = (String)keys.next();
                String nome= jm.optJSONObject(key).optString("NOME");
                mks.add(nome);
                if(!modelos.containsKey(nome)){
                    modelos.put(nome,new ArrayList());
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
}
