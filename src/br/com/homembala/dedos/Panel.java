package br.com.homembala.dedos;

import java.util.ArrayList;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.View;

class Panel extends View implements View.OnTouchListener{
	private Canvas canvas;
	private Path path;
	private Paint paint;
	private ArrayList<Path> paths = new ArrayList<Path>();
	private ArrayList<Float> widths = new ArrayList<Float>();
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
    // TODO Auto-generated method stub
   
	}
    
    public Panel(Context context, AttributeSet attrs) {
    	super(context, attrs);
    	setFocusable(true);
        setFocusableInTouchMode(true);
        this.setOnTouchListener(this);
        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(6);
        canvas = new Canvas();
        path = new Path();
        paths.add(path);
        widths.add(new Float(6));
	}       
    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        /*Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Style.FILL);
        canvas.drawPaint(paint);
        paint.setColor(Color.BLACK);
        Bitmap kangoo = BitmapFactory.decodeResource(getResources(), R.drawable.kangoo);
        //canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(kangoo, 10, 10, null);
        canvas.drawText("pichorra", 10,400, paint);
        //canvasthread.setRunning(false);*/

        int i=0;
		for (Path p : paths){
			paint.setStrokeWidth(widths.get(i));
			i++;
			canvas.drawPath(p, paint);
		}
    }
    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;

    private void touch_start(float x, float y) {
        path.reset();
        path.moveTo(x, y);
        mX = x;
        mY = y;
    }
    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            path.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            mX = x;
            mY = y;
        }
    }
    private void touch_up() {
        path.lineTo(mX, mY);
        // commit the path to our offscreen
        canvas.drawPath(path, paint);
        canvas.save();
        // kill this so we don't double draw            
        path = new Path();
        paths.add(path);
        widths.add(paint.getStrokeWidth());
    }
    @Override
    public boolean onTouch(View arg0, MotionEvent event) {
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
    	if(paths.size()>1){
			float sw=paint.getStrokeWidth();
			path.reset();
			paths.remove(paths.size()-1);
			paths.remove(paths.size()-1);
			widths.remove(widths.size()-1);
			widths.remove(widths.size()-1);
			synchronized(this){
				canvas=new Canvas();
				canvas.save();
			}
			path=new Path();
			paths.add(path);
			widths.add(sw);
			invalidate();
		    return true;
		}else{
			return false;
		}
    }
}