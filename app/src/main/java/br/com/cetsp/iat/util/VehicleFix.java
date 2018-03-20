package br.com.cetsp.iat.util;

import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import org.json.JSONException;
import org.json.JSONObject;

import br.com.cetsp.iat.CsiActivity;
import br.com.cetsp.iat.R;

import static android.view.MotionEvent.ACTION_HOVER_ENTER;
import static android.view.MotionEvent.ACTION_HOVER_MOVE;
import static android.view.MotionEvent.ACTION_OUTSIDE;

/**
 * Created by tiago on 01/06/17.
 */

public class VehicleFix extends RelativeLayout {

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
        <item>Viatura</item>
    * */

    public static final int AUTO = 0;
    public static final int CAMINHAO = 1;
    public static final int CAMINHONETE=2;
    public static final int CAMIONETA=3;
    public static final int CARROCA=4;
    public static final int MICROONIBUS=5;
    public static final int MOTO = 6;
    public static final int ONIBUS = 7;
    public static final int ARTICULADO = 8;
    public static final int REBOQUE=9;
    public static final int SEMI=10;
    public static final int TAXI=11;
    public static final int TRAILER=12;
    public static final int VIATURA=13;
    public static final int PEDESTRE = 14;
    public static final int BICI = 15;
    public static final int COLISAO = 16;
    public static final int OBSTACULO = 17;
    private Context context;
    private int model;
    private int roll;
    private boolean ligado;
    private Point position;
    private float[] posicao_atual;
    private double current_rotation;
    private boolean selectedVehicle=false;
    private int vehicleId;
    private OnTouchListener touchy;
    public float currentY;
    public float currentX;
    public float y;
    public float x;
    private int width;
    private int height;
    private int tail_width;
    private int tail_height;
    private int tail_center_x;
    private int tail_center_y;


    public JSONObject getPosition(){
        JSONObject p=new JSONObject();
        try {
            p.put("heading", this.getRotation());
            p.put("x", this.findViewById(R.id.vehicle_body).getX());
            p.put("y", this.findViewById(R.id.vehicle_body).getY());
        } catch (JSONException ignore) {
        }
        return p;
    }
    public VehicleFix(Context c) {
        super(c);
        context=c;
        init(context);
    }
    public VehicleFix(Context c, AttributeSet attrs) {
        super(c, attrs);
        context=c;
        init(context);
    }
    public VehicleFix(Context c, AttributeSet attrs, int defStyleAttr) {
        super(c, attrs, defStyleAttr);
        context=c;
        init(context);
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public VehicleFix(Context c, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(c, attrs, defStyleAttr, defStyleRes);
        context=c;
        init(context);
    }

    private void init(Context c) {
        final VehicleFix car=this;
        this.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                View habbo=car.findViewById(R.id.vehicle_tail);
                width=car.findViewById(R.id.vehicle_chassi).getWidth();
                height=car.findViewById(R.id.vehicle_chassi).getHeight();
                switch(car.model){
                    case ARTICULADO:
                        tail_width=habbo.getWidth();
                        tail_height=habbo.getHeight();
                        habbo.setX(car.currentX-habbo.getWidth()/2);
                        habbo.setY(car.currentY);
                        break;
                    default:
                }
            }
        });
        Log.d("IAT","inicializandoo veiculo");
        inflate(context, R.layout.vehicle, this);
        context=c;
        touchy = new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Log.d("IAT", "touching things "+motionEvent.getX()+"   "+motionEvent.getY());
                if(!selectedVehicle) {
                    Log.d("IAT", "touching deselected "+motionEvent.getActionMasked());
                    return true;
                }else{
                    switch (motionEvent.getActionMasked()) {
                        case MotionEvent.ACTION_HOVER_ENTER:
                            if (!selectedVehicle) return true;
                            x = motionEvent.getX();
                            y = motionEvent.getY();
                            break;
                        case MotionEvent.ACTION_DOWN:
                            if (!selectedVehicle) return true;
                            x = motionEvent.getX();
                            y = motionEvent.getY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            if (!selectedVehicle) return true;
                            Log.d("IAT","Movendo o veiculo sem mover o label");
                            moveVeiculo(view,motionEvent,x,y);
                            break;
                        case MotionEvent.ACTION_UP:
                            // será que clickou em algum outro?
                            ViewGroup tela = (ViewGroup) view.getParent().getParent().getParent();
                            for (int i = 0; i < tela.getChildCount(); i++) {
                                View car = tela.getChildAt(i).findViewById(R.id.vehicle_chassi);
                                int w = car.getWidth() / 2;
                                float rex = motionEvent.getX() + view.getX();
                                float rey = motionEvent.getY() + view.getY();
                                if ((Math.abs(car.getX() + w - rex) < 60) && (Math.abs(car.getY() + w - rey) < 60))
                                    ((CsiActivity) context).setSelectedVehicle((View) car.getParent().getParent());
                            }
                            break;
                        default:
                            return true;
                    }

                    return true;
                }
            }
        };

        //RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT,LayoutParams.MATCH_PARENT);
        //setLayoutParams(params);
        // setup all your Views from here with calls to getViewById(...);
    }

    private void moveVeiculo(View view,MotionEvent motionEvent,float x,float y) {
        currentX = motionEvent.getX();
        currentY = motionEvent.getY();
        view.setY(view.getY() + currentY - y);
        view.setX(view.getX() + currentX - x);
        ((CsiActivity) context).updatePegadorForSelectedVehicle();
    }
    public void updateRabo(float[] ponta, float[] ponta_anterior, double angle){
        View rabo = this.findViewById(R.id.vehicle_tail);
        if(rabo!=null) {
            double dista = Math.pow(Math.pow(ponta[0] - ponta_anterior[0], 2) + Math.pow(ponta[1] - ponta_anterior[1], 2), 0.5f);
            //posicao do centro de rotação:
            float[] center = new float[]{
                    rabo.getX() + rabo.getWidth() / 2,
                    rabo.getY() + rabo.getHeight() / 2
            };
            // lado a

            double a = Math.pow(Math.pow(ponta[0] - center[0], 2) + Math.pow(ponta[1] - center[1], 2), 0.5d);
            double b = Math.pow(Math.pow(ponta_anterior[0] - center[0], 2) + Math.pow(ponta_anterior[1] - center[1], 2), 0.5d);

            double delta_teta = angle - rabo.getRotation();//180d/Math.PI*Math.acos((Math.pow(dista, 2) - Math.pow(a, 2)-Math.pow(b, 2)) / (-2 * a*b));
            float a_v = this.findViewById(R.id.vehicle_body).getRotation() % 360;
            float r_v=rabo.getRotation() % 360;
            while(a_v<0) a_v+=360;
            while(r_v<0) r_v+=360;
            if(Math.abs(a_v-r_v+delta_teta)>Math.abs(a_v-r_v-delta_teta)){
                //delta_teta*=-1;
            }
            Log.d("IAT angulo de rabo",""+delta_teta);
            rabo.setRotation((float) (rabo.getRotation()+delta_teta));
            Log.d("IAT posicao do habbo:",rabo.getX()+" "+rabo.getY());
            Log.d("IAT posição do centro:",""+center[0]+" "+center[1]);
            rabo.setX((float) (ponta[0]));// - rabo.getWidth() / 2));
            //rabo.setX((float) (ponta[0] - rabo.getWidth() / 2 - Math.cos(Math.PI/180d*rabo.getRotation()) * rabo.getWidth()/2));//chassi.getX()+chassi.getWidth()/2);
            rabo.setY((float) (ponta[1]));//-rabo.getHeight()*Math.sin(Math.PI/180d*rabo.getRotation())));
        }
    }

    /*
    public VehicleFix(Context c, View bg,float w, float h,int m,int p) {
        super(c);
        context=c;
        background=bg;
        height = h;
        width=w;
        model=m;
        roll = p;
        init();
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams((int) Math.round(width), (int) Math.round(height));
        //params.gravity=Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL;
        this.setLayoutParams(params);
        ligado=true;
        mexido=false;
        setPivotX(width/2);
        setPivotX(height/2);
    }
*/

    public void zinit(int m,int p) {
        model=m;
        roll = p;
        final VehicleFix vu = this;
        View body = this.findViewById(R.id.vehicle_body);
        setRoll(roll);

        final View chassi = this.findViewById(R.id.vehicle_chassi);
        View habbo=this.findViewById(R.id.vehicle_tail);
        switch (this.model){
            case ARTICULADO:
                habbo.setVisibility(VISIBLE);
                break;
            default:
                habbo.setVisibility(GONE);
        }
        body.setOnTouchListener(new RelativeLayout.OnTouchListener() {
            @Override
            public boolean onTouch(View bode, MotionEvent motionEvent) {
                Log.d("IAT","tocou nodo body "+motionEvent.getAction());
                switch(motionEvent.getAction()){
                    case MotionEvent.ACTION_UP:
                        if(selectedVehicle) return true;
                        ((CsiActivity)context).setSelectedVehicle(vu);
                        break;
                    case MotionEvent.ACTION_MOVE:
                    case MotionEvent.ACTION_DOWN:
                        if (selectedVehicle) return false;
                        //bad idea
                        //moveVeiculo((View) bode.getParent(),motionEvent,0,0);
                        break;
                    case MotionEvent.ACTION_HOVER_EXIT:
                        Log.d("IAT","Saiu do body");
                        break;
                    case ACTION_HOVER_ENTER:
                        Log.d("IAT","entrou do body");
                        break;
                    case ACTION_HOVER_MOVE:
                        Log.d("IAT","mexeu do body");
                        break;
                    case ACTION_OUTSIDE:
                        Log.d("IAT","mexeu fora do body");
                        break;
                    default:
                        return false;
                }
                return false;
            }
        });
        OnLongClickListener tu = new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                ((CsiActivity) context).detailPagerSetup(getVehicleId());
                return true;
            }
        };
        body.setOnLongClickListener(tu);
        chassi.setOnTouchListener(touchy);
    }

    private void setRoll(int roll) {
        View body = this.findViewById(R.id.vehicle_body);
        //View rabo=this.findViewById(R.id.vehicle_tail);
        switch(model){
            case AUTO:
                switch(roll) {
                    case 0:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.carro_000));
                        break;
                    case 1:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.carro_090));
                        break;
                    case 2:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.carro_180));
                        break;
                    case 3:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.carro_270));
                        break;
                }
                break;
            case MOTO:
                switch(roll) {
                    case 0:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.motorcycle_090));
                        break;
                    case 1:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.motorcycle_180));
                        break;
                    case 2:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.motorcycle_270));
                        break;
                    case 3:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.motorcycle_000));
                        break;
                }
                break;
            case BICI:
                switch(roll) {
                    case 0:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.bici090));
                        break;
                    case 1:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.bici000));
                        break;
                    case 2:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.bici270));
                        break;
                    case 3:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.bici000));
                        break;
                }
                break;
            case ONIBUS:
                switch(roll){
                    case 0:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.bus_000));
                        break;
                    case 1:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.bus_090));
                        break;
                    case 2:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.bus_180));
                        break;
                    case 3:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.bus_270));
                        break;

                }
                break;
            case ARTICULADO:
                //TODO:  criar aqui uma instância do    Gonzagão
                switch(roll){
                    case 0:
                        //rabo.setBackground(ContextCompat.getDrawable(context,R.drawable.gonzagao_1_000));
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.bus_000));
                        break;
                    case 1:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.bus_090));
                        break;
                    case 2:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.bus_180));
                        break;
                    case 3:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.bus_270));
                        break;

                }
                break;
            case MICROONIBUS:
                switch(roll){
                    case 0:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.microbus_000));
                        break;
                    case 1:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.microbus_090));
                        break;
                    case 2:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.microbus_180));
                        break;
                    case 3:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.microbus_270));
                        break;

                }
                break;
            case CAMINHAO:
                switch(roll){
                    case 0:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.truck_000));
                        break;
                    case 1:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.truck_090));
                        break;
                    case 2:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.truck_180));
                        break;
                    case 3:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.truck_270));
                        break;
                }
                break;
            case CAMINHONETE:
                switch(roll){
                    case 0:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.suv_000));
                        break;
                    case 1:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.suv_090));
                        break;
                    case 2:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.suv_180));
                        break;
                    case 3:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.suv_270));
                        break;
                }
                break;
            case CAMIONETA:
                switch(roll){
                    case 0:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.camioneta_000));
                        break;
                    case 1:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.camioneta_090));
                        break;
                    case 2:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.camioneta_180));
                        break;
                    case 3:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.camioneta_270));
                        break;
                }
                break;
            case SEMI:
                switch(roll){
                    case 0:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.semi_000));
                        break;
                    case 1:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.semi_090));
                        break;
                    case 2:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.semi_180));
                        break;
                    case 3:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.semi_270));
                        break;
                }
                break;
            case CARROCA:
                switch(roll){
                    case 0:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.carroca_000));
                        break;
                    case 1:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.carroca_090));
                        break;
                    case 2:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.carroca_000));
                        break;
                    case 3:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.carroca_270));
                        break;
                }
                break;
            case TAXI:
                switch(roll) {
                    case 0:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.taxi_000));
                        break;
                    case 1:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.taxi_090));
                        break;
                    case 2:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.taxi_180));
                        break;
                    case 3:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.taxi_270));
                        break;
                }
                break;
            case TRAILER:
                switch(roll) {
                    case 0:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.trailer_000));
                        break;
                    case 1:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.trailer_090));
                        break;
                    case 2:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.trailer_180));
                        break;
                    case 3:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.trailer_270));
                        break;
                }
                break;
            case VIATURA:
                switch(roll) {
                    case 0:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.viatura_000));
                        break;
                    case 1:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.viatura_090));
                        break;
                    case 2:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.viatura_180));
                        break;
                    case 3:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.viatura_270));
                        break;
                }
                break;
            case REBOQUE:
                switch(roll) {
                    case 0:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.reboque_000));
                        break;
                    case 1:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.reboque_090));
                        break;
                    case 2:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.reboque_180));
                        break;
                    case 3:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.reboque_270));
                        break;
                }
                break;


            case PEDESTRE:
                body.setBackground(ContextCompat.getDrawable(context,R.drawable.pessoa));
                break;
            case COLISAO:
                body.setBackground(ContextCompat.getDrawable(context,R.drawable.explode));
                break;
            case OBSTACULO:
                Box c = new Box(context);
                ((ViewGroup) body).addView(c);
                break;
        }
    }

    public void liga(boolean l){
        ligado=l;
    }

    public void vira() {
        roll++;
        if(roll>3)roll=0;
        setRoll(roll);
        //init(); REDRAW
    }
    public float getRotation(){
        return this.findViewById(R.id.vehicle_body).getRotation();
    }

    public int getRoll() {
        return roll;
    }

    public void addOrRemoveProperty(int property, boolean flag){
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) getLayoutParams();
        if(flag){
            layoutParams.addRule(property);
        }else {
            layoutParams.removeRule(property);
        }
        setLayoutParams(layoutParams);
    }

    public void setSelectedVehicle(boolean s) {
        selectedVehicle = s;
        this.findViewById(R.id.vehicle_body).setClickable(!s);
        if(s) {
            this.findViewById(R.id.vehicle_chassi).setOnTouchListener(touchy);
            //this.findViewById(R.id.vehicle_chassi).setBackground(ResourcesCompat.getDrawable(getResources(), R.drawable.elipse, null));
        }else{
            this.findViewById(R.id.vehicle_chassi).setOnTouchListener(null);
            //this.findViewById(R.id.vehicle_chassi).setBackground(null);
        }
    }

    public void setVehicleId(int vid) {
        vehicleId = vid;
    }

    public int getVehicleId() {
        return vehicleId;
    }
}
