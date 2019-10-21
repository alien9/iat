package br.com.cetsp.iat;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposePathEffect;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PathEffect;
import android.support.v4.content.ContextCompat;
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
    public static final int TRACK = 3;
    public static final int DAMAGE = 4;
    public static final int CENTERLINE = 5;
    public static final int ERASER = 6;
    private final Context context;
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
    private JSONArray json_paths=new JSONArray();
    private double resolution=1;

    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
    }

    public Panel(Context c, AttributeSet attrs) {
        super(c, attrs);
        setFocusable(true);
        setFocusableInTouchMode(true);
        this.setOnTouchListener(this);
        canvas = new Canvas();
        setDrawingCacheEnabled(true);
        context=c;
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
    }
    @Override
    public boolean onTouch(View arg0, MotionEvent event) {
        if(!ligado)return false;
        float x = event.getX();
        float y = event.getY();
        if(style==ERASER) {
            if(event.getAction()==MotionEvent.ACTION_UP) erase(x,y);
            return true;
        }
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

    private void erase(float x, float y) {
        int n=-1;
        double d=99999999999d;
        json_paths=getJSONPaths();
        for(int i=0;i<paths.size();i++){
            JSONArray p=json_paths.optJSONObject(i).optJSONArray("points");
            for(int j=0;j<p.length();j++){
                JSONArray pu = p.optJSONArray(j);
                double distance=Math.pow(Math.pow(pu.optDouble(0)-x,2d)+Math.pow(pu.optDouble(1)-y,2d),0.5d);
                if(distance<d){
                    d=distance;
                    n=i;
                }
            }
        }
        if(n>=0){
            paths.remove(n);
            paints.remove(n);
            serializable.remove(n);
            styles.remove(n);
            synchronized(this){
                canvas=new Canvas();
                canvas.save();
            }
            invalidate();
        }
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
            serializable.remove(s);
            styles.remove(s);
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

    public void setStyle(int s, double r) {
        style = s;
        resolution=r;
    }
    private Paint getPaint(){
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStyle(Paint.Style.STROKE);
        switch (style) {
            case DAMAGE:
                paint.setColor(Color.RED);
                paint.setStrokeJoin(Paint.Join.ROUND);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeWidth(40);
                break;
            case SKID:
                paint.setColor(Color.BLACK);
                paint.setStrokeJoin(Paint.Join.ROUND);
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setStrokeWidth((float) (0.5*resolution));
                break;
            case ZEBRA:
                paint.setColor(ContextCompat.getColor(context, R.color.medium_gray));
                paint.setStrokeWidth((float) (3*resolution));
                paint.setPathEffect(new DashPathEffect(new float[]{(float) (0.6*resolution), (float) (0.3*resolution)},(float) (0.3*resolution)));
                break;
            case TRACK:
                paint.setColor(Color.BLACK);
                paint.setStrokeWidth((float) (0.5*resolution));
                paint.setPathEffect(new DashPathEffect(new float[]{(float) (1*resolution), (float) (0.6*resolution)},(float) (0.4*resolution)));
                break;
            case CENTERLINE:
                paint.setColor(Color.argb(255,204,204,204));
                paint.setStrokeWidth((float) (4*resolution));
                PathEffect ee = new CornerPathEffect((float) (10*resolution));
                PathEffect ef = new PathDashPathEffect(makePathDash(resolution), 12, 30, PathDashPathEffect.Style.MORPH);
                paint.setPathEffect(new ComposePathEffect(ef,ee));
                break;
        }
        return paint;
    }

    private static Path makePathDash(double r) {
        Path p = new Path();
        r/=10d;
        p.moveTo(Math.round((-6)*r), Math.round(4*r));
        p.lineTo(Math.round(6*r),Math.round(4*r));
        p.lineTo(Math.round(6*r),Math.round(2*r));
        p.lineTo(Math.round((-6)*r), Math.round(2*r));
        p.close();
        p.moveTo(Math.round((-6)*r), Math.round((-4)*r));
        p.lineTo(Math.round(6*r), Math.round((-4)*r));
        p.lineTo(Math.round(6*r),Math.round((-2*r)));
        p.lineTo(Math.round((-6)*r),Math.round((-2*r)));
        return p;
    }


    public void reset(){
        paths=new ArrayList<>();
        paints=new ArrayList<>();
        styles=new ArrayList<>();
        serializable=new ArrayList<>();
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
                if(json_paths.length()>i){
                    item=json_paths.getJSONObject(i);
                }
                item.put("style",styles.get(i));
                item.put("points",serializable.get(i).getPoints());
                res.put(item);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return res;
    }

    public void setJSONPaths(JSONArray jp,double r) {
        reset();
        json_paths=jp;
        for(int i=0;i<json_paths.length();i++){
            JSONObject json_path=json_paths.optJSONObject(i);
            setStyle(json_path.optInt("style"),r);
            if(json_path.has("points")){
                JSONArray points = json_path.optJSONArray("points");
                if(points.length()>0){
                    int j=0;
                    touch_start((float)points.optJSONArray(j).optDouble(0),(float)points.optJSONArray(j).optDouble(1));
                    j++;
                    while(j<points.length()){
                        touch_move((float)points.optJSONArray(j).optDouble(0),(float)points.optJSONArray(j).optDouble(1));
                        j++;
                    }
                }
            }
        }
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
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return false;
    }
}
