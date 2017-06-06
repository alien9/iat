package br.com.homembala.dedos.util;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import org.json.JSONException;
import org.json.JSONObject;

import br.com.homembala.dedos.CsiActivity;
import br.com.homembala.dedos.R;

/**
 * Created by tiago on 01/06/17.
 */

public class VehicleFix extends LinearLayout {
    public static final int CARRO = 0;
    public static final int CAMINHAO = 1;
    public static final int ONIBUS = 2;
    public static final int MOTO = 3;
    public static final int PEDESTRE = 4;
    public static final int BICI = 5;


    private final double width;
    private final double height;
    private final Context context;
    private final int model;
    private boolean mexido;
    private int roll;
    private View background;
    private float[] inicio;
    private boolean ligado;

    public JSONObject getPosition(){
        JSONObject p=new JSONObject();
        try {
            p.put("heading", this.getRotation());
            p.put("x", this.getX());
            p.put("y",this.getY());
        } catch (JSONException ignore) {
        }
        return p;
    }

    public VehicleFix(Context c, View bg,int w, int h,int m,int p) {
        super(c);
        context=c;
        background=bg;
        height = h;
        width=w;
        model=m;
        roll = p;
        setCustomBackground();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams((int) Math.round(width), (int) Math.round(height));
        params.gravity=Gravity.CENTER_VERTICAL|Gravity.CENTER_HORIZONTAL;
        this.setLayoutParams(params);
        ligado=true;
        mexido=false;
    }

    private void setCustomBackground() {
        switch(model){
            case CARRO:
                switch(roll) {
                    case 0:
                        this.setBackground(ContextCompat.getDrawable(context, R.drawable.carro_000));
                        break;
                    case 1:
                        this.setBackground(ContextCompat.getDrawable(context, R.drawable.carro_090));
                        break;
                    case 2:
                        this.setBackground(ContextCompat.getDrawable(context, R.drawable.carro_180));
                        break;
                    case 3:
                        this.setBackground(ContextCompat.getDrawable(context, R.drawable.carro_270));
                        break;
                }
                break;
            case MOTO:
                switch(roll) {
                    case 0:
                        this.setBackground(ContextCompat.getDrawable(context, R.drawable.motorcycle_090));
                        break;
                    case 1:
                        this.setBackground(ContextCompat.getDrawable(context, R.drawable.motorcycle_180));
                        break;
                    case 2:
                        this.setBackground(ContextCompat.getDrawable(context, R.drawable.motorcycle_270));
                        break;
                    case 3:
                        this.setBackground(ContextCompat.getDrawable(context, R.drawable.motorcycle_000));
                        break;
                }
                break;
            case ONIBUS:
                switch(roll){
                    case 0:
                        this.setBackground(ContextCompat.getDrawable(context,R.drawable.bus_000));
                        break;
                    case 1:
                        this.setBackground(ContextCompat.getDrawable(context,R.drawable.bus_090));
                        break;
                    case 2:
                        this.setBackground(ContextCompat.getDrawable(context,R.drawable.bus_180));
                        break;
                    case 3:
                        this.setBackground(ContextCompat.getDrawable(context,R.drawable.bus_270));
                        break;

                }
                break;
            case CAMINHAO:
                switch(roll){
                    case 0:
                        this.setBackground(ContextCompat.getDrawable(context,R.drawable.truck_000));
                        break;
                    case 1:
                        this.setBackground(ContextCompat.getDrawable(context,R.drawable.truck_090));
                        break;
                    case 2:
                        this.setBackground(ContextCompat.getDrawable(context,R.drawable.truck_000));
                        break;
                    case 3:
                        this.setBackground(ContextCompat.getDrawable(context,R.drawable.truck_270));
                        break;
                }
                break;
            case PEDESTRE:
                this.setBackground(ContextCompat.getDrawable(context,R.drawable.pessoa));
                break;
        }
        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch(motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        inicio=new float[]{(float) (motionEvent.getX()+width*0.5), (float) (motionEvent.getY()-view.getHeight())};
                        ((CsiActivity)context).setSelectedVehicle((VehicleFix) view);
                        break;

                    case MotionEvent.ACTION_MOVE:
                    /*    int[] position = new int[2];
                        view.getLocationInWindow(position);

                        if(inicio!=null) {
                            Log.d("IAT",String.format("inicidando em %s",inicio[1]));
                            view.setX(motionEvent.getX() +position[0]- inicio[0]);
                            view.setY(motionEvent.getY() +position[1]+inicio[1]-200);
                        }else{
                            view.setX(motionEvent.getX()+position[0]);
                            view.setY(motionEvent.getY()+position[1]);
                        }
                        view.invalidate();
                        break;
                    */
                    case MotionEvent.ACTION_UP:
                        ((CsiActivity)context).updatePegadorForSelectedVehicle((VehicleFix) view,motionEvent);

                        break;
                }
                return true;
            }
        });
    }

    public void liga(boolean l){
        ligado=l;
    }

    public void vira() {
        roll++;
        if(roll>3)roll=0;
        setCustomBackground();
    }

    public int getRoll() {
        return roll;
    }
}
