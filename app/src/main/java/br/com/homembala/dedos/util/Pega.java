package br.com.homembala.dedos.util;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.StringDef;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import br.com.homembala.dedos.CsiActivity;
import br.com.homembala.dedos.R;

/**
 * Created by tiago on 31/05/17.
 */

public class Pega extends LinearLayout {
    public static final int MOVE = 0;
    private Context context;
    private int[] tamanho;
    private float rotation;
    private int[] center;
    private float[] ponta_atual;
    int rod_length=dpToPx(153);
    int ball_size=dpToPx(13);
    private Handler handler;

    public Pega(Context context) {
        super(context);
        init();
    }
    public Pega(Context c, AttributeSet attrs) {
        super(c, attrs);
        context=c;
        init();
    }
    public Pega(Context c, AttributeSet attrs, int defStyleAttr) {
        super(c, attrs, defStyleAttr);
        context=c;
        init();
    }
    public Pega(Context c, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(c, attrs, defStyleAttr, defStyleRes);
        context=c;
        init();
    }
    private double getAngle(float[] vetor){ //a inclinação entre dois pontos, medida em ângulo em relação ao rotation zero
        if(vetor[1]==0){
            return 270-((vetor[0]>0)?90:270);
        }
        return ((vetor[1]>0)?180:360)-180*Math.atan(vetor[0]/vetor[1])/Math.PI;
    }
    public float[] getPonta(){
        return getPonta(new float[]{getX(),getY()}, rotation);
    }
    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }
    public float[] getPonta(float[] centerposition, double rotation) {
        return new float[]{
                (float) (centerposition[0] - (Math.sin(rotation)*rod_length)),
                (float) (centerposition[1] + (Math.cos(rotation)*rod_length))
        };
    }
    public float[] getHandlerCenter(float[] pon, double rotation){
        return new float[]{
                (float) (pon[0] + (Math.sin(rotation)*rod_length)),
                (float) (pon[1] - (Math.cos(rotation)*rod_length))
        };
    }

    private void init() {
        final View v = inflate(getContext(), R.layout.vehicle_handler, this);
        rotation= 0.0f;//(float) getAngle(ponta_atual);
        //int[] tamanho = new int[]{v.findViewById(R.id.floatingActionButton).getWidth(), v.findViewById(R.id.floatingActionButton).getHeight()};
        //pezinho=new float[]{0,};
        final Pega pegador=this;
        final View rod = v.findViewById(R.id.rod);
        rod.setPivotX(0);
        rod.setPivotY(0);
        final View bolinha = v.findViewById(R.id.floatingActionButton);

        v.findViewById(R.id.floatingActionButton).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                float[] ponta_position = new float[2];
                switch(motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        //inicia arrasto
                        Log.d("IAT","Iniciando o movimento o maus");
                        tamanho = new int[]{v.findViewById(R.id.floatingActionButton).getWidth()/2
                                , v.findViewById(R.id.floatingActionButton).getHeight()/2};
                        center = new int[]{
                                bolinha.getWidth()/2,
                                bolinha.getHeight()/2
                        };
                        int[] pos = new int[2];
                        bolinha.getLocationInWindow(pos);
                        ponta_atual=getPonta(new float[]{
                                pos[0],
                                pos[1]
                        },rotation*Math.PI/180);
                        break;
                    case MotionEvent.ACTION_UP:
                        //finaliza arrasto

                        Log.d("IAT","parando o maus");
                        break;
                    case MotionEvent.ACTION_MOVE:
                        //Log.d("IAT",String.format("mexendo o maus %s %s",motionEvent.getX(),motionEvent.getY()));

                        //posicao da bolinha:
                        float[] position = new float[]{
                            bolinha.getX()+tamanho[0],bolinha.getY()+tamanho[0]
                        };
                        //bolinha.getLocationOnScreen(position);

                        float[] centerposition=new float[]{
                                position[0],
                                position[1]
                        };
                        //Log.d("IAT",String.format("centro %s %s - ponta %s %s para rotten %s",centerposition[0],centerposition[1],ponta_atual[0],ponta_atual[1],rotation));
                        Log.d("IAT",String.format("centro %s %s - ponta %s %s para rotten %s",centerposition[0],centerposition[1],ponta_atual[0],ponta_atual[1],rotation));
                        float[] move=new float[]{
                                motionEvent.getX(),motionEvent.getY()
                        };

                        float[] prox=new float[]{
                                centerposition[0]+move[0],
                                centerposition[1]+move[1]
                        };
                        bolinha.setX(prox[0]-tamanho[0]);
                        bolinha.setY(prox[1]-tamanho[0]);
                        rod.setX(prox[0]);
                        rod.setY(prox[1]);

                        float[] vetor = new float[] {
                                prox[0]-ponta_atual[0],
                                prox[1]-ponta_atual[1]
                        };

                        double angle = getAngle(vetor);
                        rotation=(float) angle;
                        ponta_atual=getPonta(new float[]{
                                prox[0],
                                prox[1]
                        },rotation*Math.PI/180);

                        rod.setRotation(rotation);
                        rod.setX(prox[0]);
                        rod.setY(prox[1]);
                        ((CsiActivity)context).updateVehiclePosition(pegador,getPonta(prox, rotation*Math.PI/180));
                        break;
                }

                return false;
            }
        });
    }

    public float getRodRotation() {
        return findViewById(R.id.rod).getRotation();
    }

    public void setPontaPosition(float x, float y, float rotation) {
        float[] po = getHandlerCenter(new float[]{x, y}, rotation);
        View bolinha = findViewById(R.id.floatingActionButton);
        bolinha.setX(po[0]);
        bolinha.setY(po[1]);
        findViewById(R.id.rod).setRotation(rotation);
    }
}
