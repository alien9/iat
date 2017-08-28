package org.bigrs.croqui.util;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import org.bigrs.croqui.CsiActivity;

import static android.view.MotionEvent.ACTION_MOVE;

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
    int rod_width=dpToPx(1);

    int ball_radius =dpToPx(14);
    int big_ball_radius =dpToPx(42);
    private float[] posicao_atual;
    private float x=0;
    private float y=0;
    private boolean click=false;

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
        return ((vetor[1]>0)?180:0)-180*Math.atan(vetor[0]/vetor[1])/Math.PI;
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
        final View bolinha = v.findViewById(R.id.bolinha);
        v.findViewById(R.id.bolinha).setOnTouchListener(new OnTouchListener() {
            public float currentX;
            public float currentY;
            public float y;
            public float x;
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                float[] ponta_position = new float[2];
                switch(motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        //inicia arrasto
                        Log.d("IAT","Iniciando o movimento o maus");
                        center = new int[]{
                                ball_radius,
                                ball_radius
                        };
                        x = motionEvent.getX();
                        y = motionEvent.getY();
                        //int[] pos = new int[2];
                        //bolinha.getLocationInWindow(pos);
                        posicao_atual=new float[]{
                                posicao_atual[0] + motionEvent.getX(),
                                posicao_atual[1] + motionEvent.getY()
                        };
                        //ponta_atual=getPonta(posicao_atual,getRodRotation()*Math.PI/180); // quem poderia imaginar uma coisa dessas
                        ponta_atual=getPonta(new float[]{
                                posicao_atual[0]-ball_radius,posicao_atual[1]-ball_radius
                        },getRodRotation()*Math.PI/180); // quem poderia imaginar uma coisa dessas
                        break;
                    case MotionEvent.ACTION_UP:
                        //finaliza arrasto
                        Log.d("IAT","parando o maus");
                        break;
                    case ACTION_MOVE:
                        currentX=motionEvent.getX();
                        currentY=motionEvent.getY();
                        float[] prox=new float[]{
                                bolinha.getX()+currentX-x+ball_radius,
                                bolinha.getY()+currentY-y+ball_radius
                        };
                        bolinha.setX(prox[0]-ball_radius);
                        bolinha.setY(prox[1]-ball_radius);

                        float[] vetor = new float[] {
                                prox[0]-ponta_atual[0],
                                prox[1]-ponta_atual[1]
                        };

                        rotation=(float) getAngle(vetor);
                        ponta_atual=getPonta(new float[]{
                                prox[0],
                                prox[1]
                        },rotation*Math.PI/180);
                        rod.setRotation(rotation);
                        rod.setX(prox[0]);
                        rod.setY(prox[1]);
                        float[] ponta = getPonta(prox, rotation * Math.PI / 180);
                        ((CsiActivity)context).updateVehiclePosition(pegador,ponta);
                        findViewById(R.id.movedor).setX(ponta[0]-big_ball_radius);
                        findViewById(R.id.movedor).setY(ponta[1]-big_ball_radius);
                        posicao_atual=prox;
                        break;
                }

                return true;
            }
        });
        findViewById(R.id.movedor).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        click=true;
                        x = motionEvent.getX();
                        y = motionEvent.getY();
                        rotation=rod.getRotation();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        click=false;
                        ponta_atual=new float[]{
                                ponta_atual[0]+motionEvent.getX()-x,ponta_atual[1]+motionEvent.getY()-y
                        };
                        setPontaPosition(ponta_atual[0],ponta_atual[1],rotation);
                        ((CsiActivity)context).updateVehiclePosition(pegador,ponta_atual);
                        Log.d("IAT","Translação "+motionEvent.getX()+"   "+motionEvent.getY());
                        return true;
                    case MotionEvent.ACTION_UP:
                        if(click){
                            click=false;
                            ((CsiActivity)context).detailPagerSetup();
                        }
                        break;
                }
                return false;
            }
        });
        //findViewById(R.id.movedor).setOnLongClickListener(new OnLongClickListener() {
        //    @Override
        //    public boolean onLongClick(View view) {
        //         //((CsiActivity)context).detailPagerSetup();
        //return false;
        //     }
        //});

    }

    public float getRodRotation() {
        return findViewById(R.id.rod).getRotation();
    }
    public void setRodRotation(float r) {
        findViewById(R.id.rod).setRotation(r);
    }

    public void setPontaPosition(float x, float y, float rotation) {
        Log.d("IAT", "setando a posiocao da pornta");
        posicao_atual = getHandlerCenter(new float[]{x, y}, rotation*Math.PI/180);
        ponta_atual=new float[]{x,y};
        Log.d("IAT", "pegou posiocao da pornta");
        View bolinha = findViewById(R.id.bolinha);
        View rod = findViewById(R.id.rod);
        bolinha.setX(posicao_atual[0]-ball_radius);
        bolinha.setY(posicao_atual[1]-ball_radius);
        rod.setX(posicao_atual[0]);
        rod.setY(posicao_atual[1]);
        rod.setRotation(rotation);
        findViewById(R.id.movedor).setX(x-big_ball_radius);
        findViewById(R.id.movedor).setY(y-big_ball_radius);
    }
}
