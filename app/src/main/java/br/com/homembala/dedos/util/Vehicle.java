package br.com.homembala.dedos.util;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.LinearLayout;

import br.com.homembala.dedos.R;


/**
 * Created by tiago on 31/03/17.
 */

public class Vehicle extends View {
    private final double width;
    private final double height;
    private View background;
    private Float[] inicio;
    private DragShadowBuilder shadowBuilder;
    private boolean ligado;

    public Vehicle(Context context) {
        super(context);
        this.setOnTouchListener(new VehicleTouchListener());
        height = 4;
        width=2.2;
    }

    public Vehicle(Context context, View bg,int w, int h) {
        super(context);
        background=bg;
        height = h;
        width=w;
        this.setBackground(ContextCompat.getDrawable(context,R.drawable.carro));
        this.setOnTouchListener(new VehicleTouchListener());
        this.setLayoutParams(new LinearLayout.LayoutParams((int)Math.round(width), (int)Math.round(height)));
        background.setOnDragListener(new VehicleDragListener());
        ligado=true;
    }

    protected boolean OnDrag(View v, DragEvent event){
        return true;
    }
    public void liga(boolean l){
        ligado=l;
    }

    private class VehicleTouchListener implements OnTouchListener {
        @Override
        public boolean onTouch(final View view, MotionEvent motionEvent) {
            if(!ligado) return true;
            if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
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
                    view.setX(event.getX()-inicio[0]);
                    view.setY(event.getY()-inicio[1]);
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
                    Log.d("IAT DRAW", ""+angulo);
                    previousPoint=new Float[]{event.getX(),event.getY()};
                    view = (View) event.getLocalState();
                    view.setRotation((float) angulo);
                    view.setX(event.getX()-inicio[0]);
                    view.setY(event.getY()-inicio[1]);
                    view.invalidate();
                    break;
                default:
                    break;
            }
            return true;
        }
    }
}
