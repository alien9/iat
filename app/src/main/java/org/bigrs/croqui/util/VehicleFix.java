package org.bigrs.croqui.util;

import android.content.Context;
import android.graphics.Point;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import org.json.JSONException;
import org.json.JSONObject;

import org.bigrs.croqui.CsiActivity;

import static android.view.MotionEvent.ACTION_HOVER_ENTER;
import static android.view.MotionEvent.ACTION_HOVER_MOVE;
import static android.view.MotionEvent.ACTION_OUTSIDE;

/**
 * Created by tiago on 01/06/17.
 */

public class VehicleFix extends RelativeLayout {
    public static final int CARRO = 0;
    public static final int CAMINHAO = 1;
    public static final int ONIBUS = 2;
    public static final int MOTO = 3;
    public static final int PEDESTRE = 4;
    public static final int BICI = 5;
    public static final int COLISAO = 6;
    public static final int OBSTACULO = 7;

    private float width;
    private float height;
    private Context context;
    private int model;
    private int roll;
    private View background;
    private float[] inicio;
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
    public VehicleFix(Context c, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(c, attrs, defStyleAttr, defStyleRes);
        context=c;
        init(context);
    }

    private void init(Context c) {
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
        switch(model){
            case CARRO:
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
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.bici));
                        break;
                    case 2:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.bici270));
                        break;
                    case 3:
                        body.setBackground(ContextCompat.getDrawable(context, R.drawable.bici));
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
            case CAMINHAO:
                switch(roll){
                    case 0:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.truck_000));
                        break;
                    case 1:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.truck_090));
                        break;
                    case 2:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.truck_000));
                        break;
                    case 3:
                        body.setBackground(ContextCompat.getDrawable(context,R.drawable.truck_270));
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
        final View chassi = this.findViewById(R.id.vehicle_chassi);

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

    public void liga(boolean l){
        ligado=l;
    }

    public void vira() {
        roll++;
        if(roll>3)roll=0;
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
