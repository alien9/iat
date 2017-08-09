package br.com.homembala.dedos;

import java.util.ArrayList;
import java.util.Deque;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.DiscretePathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Panel extends View implements View.OnTouchListener{
    public static final int SKID = 1;
    public static final int ZEBRA = 2;
    private Canvas canvas;
    private Path path;
    private Paint paint;
    private ArrayList<Path> paths = new ArrayList<Path>();
    private ArrayList<Float> widths = new ArrayList<Float>();
    private ArrayList<Paint> paints = new ArrayList<Paint>();
    private ArrayList<Quad> serializable = new ArrayList<>();
    private boolean ligado;
    private int style;
    private ArrayList<Integer> styles=new ArrayList<>();

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
    }

    public Panel(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        setFocusableInTouchMode(true);
        this.setOnTouchListener(this);
        canvas = new Canvas();
        setDrawingCacheEnabled(true);
    }
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (int i=0;i<paths.size();i++){
            canvas.drawPath(paths.get(i), paints.get(i));
        }
    }
    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 1;

    private void touch_start(float x, float y) {
        path=new Path();
        path.reset();
        path.moveTo(x, y);
        paths.add(path);
        paints.add(getPaint());
        styles.add(style);
        serializable.add(new Quad());
        mX = x;
        mY = y;
    }
    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            paths.get(paths.size()-1).quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            mX = x;
            mY = y;
            serializable.get(serializable.size()-1).add(x,y);
        }
    }
    private void touch_up() {
        paths.get(paths.size()-1).lineTo(mX, mY);
        // commit the path to our offscreen
        //canvas.drawPath(paths.get(paths.size()-1), getPaint());
        //canvas.save();
        // kill this so we don't double draw            
        //path = new Path();

    }
    @Override
    public boolean onTouch(View arg0, MotionEvent event) {
        if(!ligado)return false;
        float x = event.getX();
        float y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touch_start(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touch_move(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                touch_up();
                invalidate();
                break;
        }
        return true;
    }
    public float getStrokeWidth(){
        return paint.getStrokeWidth();
    }

    public void setStrokeWidth(float sw){
        if(sw<0) sw=1;
        if(sw>50) sw=50;
        paint.setStrokeWidth(sw);
        widths.set(widths.size()-1,sw);
    }
    public boolean back(){
        if(paths.size()>0){
            int s=paths.size()-1;
            paths.remove(s);
            paints.remove(s);
            synchronized(this){
                canvas=new Canvas();
                canvas.save();
            }
            invalidate();
            return true;
        }else{
            return false;
        }
    }
    public void setLigado(boolean l) {
        ligado = l;
    }

    public void setStyle(int s) {
        style = s;
    }
    private Paint getPaint(){
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStyle(Paint.Style.STROKE);
        switch (style) {
            case SKID:
                paint.setColor(Color.BLACK);
                paint.setStrokeJoin(Paint.Join.ROUND);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeWidth(6);
                break;
            case ZEBRA:
                paint.setColor(Color.argb(255,204,204,204));
                paint.setStrokeWidth(52);
                paint.setPathEffect(new DashPathEffect(new float[]{10,5},5));
                break;
        }
        return paint;
    }
    public void reset(){
        paths=new ArrayList<>();
        paints=new ArrayList<>();
        styles=new ArrayList<>();
        invalidate();
    }
    public String serialize(){
        for(int i=0;i<paths.size();i++){

        }
        return "";
    }

    public ArrayList<Path> getPaths() {
        return paths;
    }

    public JSONArray getJSONPaths() {
        JSONArray res = new JSONArray();
        try {
            for(int i=0;i<paths.size();i++){
                JSONObject item = new JSONObject();
                item.put("style",styles.get(i));
                item.put("points",serializable.get(i).getPoints());
                res.put(item);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return res;
    }

    private class Quad {
        private JSONArray points;
        public Quad(){
            points=new JSONArray();
        }
        public void add(float x, float y) {
            JSONArray p = new JSONArray();
            try {
                p.put(x);
                p.put(y);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            points.put(p);
        }
        public JSONArray getPoints(){
            return points;
        }
    }
}
