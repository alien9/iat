package org.bigrs.iat.util;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import org.bigrs.iat.R;
import org.json.JSONException;
import org.json.JSONObject;

import org.bigrs.iat.Iat;


/**
 * Created by tiago on 31/03/17.
 */

public class Vehicle extends View {
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
    private Float[] inicio;
    private DragShadowBuilder shadowBuilder;
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

    public Vehicle(Context c, View bg,int w, int h,int m,int p) {
        super(c);
        context=c;
        background=bg;
        height = h;
        width=w;
        model=m;
        roll = p;
        setCustomBackground();
        this.setOnTouchListener(new VehicleTouchListener());
        this.setLayoutParams(new LinearLayout.LayoutParams((int)Math.round(width), (int)Math.round(height)));
        background.setOnDragListener(new VehicleDragListener());
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

    public boolean getMexido() {
        return mexido;
    }

    private class VehicleTouchListener implements OnTouchListener {
        @Override
        public boolean onTouch(final View view, MotionEvent motionEvent) {
            if(!ligado) return true;
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                mexido=true;
                inicio=new Float[]{motionEvent.getX(),motionEvent.getY()};
                ClipData data = ClipData.newPlainText("", "");
                shadowBuilder = new View.DragShadowBuilder(view) {
                    @Override
                    public void onDrawShadow(Canvas canvas) {
                        //canvas.scale(view.getScaleX(), view.getScaleY(), width / 2,height / 2);
                        //canvas.rotate(view.getRotation(), width / 2, height / 2);
                        //canvas.translate((width - view.getWidth()) / 2,(height - view.getHeight()) / 2);
                        //super.onDrawShadow(canvas);
                    }

                    @Override
                    public void onProvideShadowMetrics(Point shadowSize,
                                                       Point shadowTouchPoint) {
                        shadowSize.set(1, 1);
                        shadowTouchPoint.set(shadowSize.x / 2, shadowSize.y / 2);
                    }
                };
                //DragShadowBuilder shadowBuilder = new DragShadowBuilder(view);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    view.startDragAndDrop(data, shadowBuilder, view, 0);
                }else{
                    view.startDrag(data, shadowBuilder, view, 0);
                }
                //view.setVisibility(View.INVISIBLE);
                return true;
            } else {
                return false;
            }
        }
    }

    private class VehicleDragListener implements OnDragListener {
        private Float[] previousPoint;
        @Override
        public boolean onDrag(View v, DragEvent event) {
            Log.d("IAT ESTRANHO", "drag"+event.getAction());
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    previousPoint=new Float[]{event.getX(),event.getY()};
                    break;
                case DragEvent.ACTION_DRAG_ENTERED:
                    //v.setBackgroundDrawable(enterShape);
                    break;
                case DragEvent.ACTION_DRAG_EXITED:
                    //v.setBackgroundDrawable(normalShape);
                    break;
                case DragEvent.ACTION_DROP:
                    View view = (View) event.getLocalState();
                    Log.d("IAT DRAW",""+event.getX());
                    if(inicio!=null) {
                        view.setX(event.getX()-inicio[0]);
                        view.setY(event.getY()-inicio[1]);
                    }else{
                        view.setX(event.getX());
                        view.setY(event.getY());
                    }
                    view.setVisibility(VISIBLE);
                    break;
                case DragEvent.ACTION_DRAG_ENDED:
                    //v.setBackgroundDrawable(normalShape);
                    break;
                case DragEvent.ACTION_DRAG_LOCATION:
                    Float[] delta = new Float[]{event.getX() - previousPoint[0], event.getY() - previousPoint[1]};
                    if(Math.sqrt(Math.pow(delta[0],2.0)+Math.pow(delta[1],2.0))<20)break;
                    double angulo = Math.acos(-1*delta[1]/(Math.sqrt(Math.pow(delta[0],2.0)+Math.pow(delta[1],2.0))));
                    angulo=angulo/(2.0*Math.PI)*360.0;
                    if(delta[0]<0) angulo*=-1;
                    previousPoint=new Float[]{event.getX(),event.getY()};
                    view = (View) event.getLocalState();
                    view.setRotation((float) angulo);
                    if(inicio!=null) {
                        view.setX(event.getX() - inicio[0]);
                        view.setY(event.getY() - inicio[1]);
                    }else{
                        view.setX(event.getX());
                        view.setY(event.getY());
                    }
                    view.invalidate();
                    ((Iat)context.getApplicationContext()).setSelectedVehicle((VehicleFix) view);
                    break;
                default:
                    break;
            }
            return true;
        }
    }
}
