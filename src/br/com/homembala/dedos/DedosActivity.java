package br.com.homembala.dedos;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.database.CursorJoiner.Result;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;

public class DedosActivity extends Activity {
	Panel p;
	
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.frame);
        p=(Panel) findViewById(R.id.SurfaceView01);
        Bundle b=this.getIntent().getExtras();
        
        //Button bu=(Button) findViewById(b.getInt("background"));
        //Resources res=getResources();
    	//Bitmap kangoo = BitmapFactory.decodeResource(getResources(), b.getInt("background"));
       // String bg=res.getResourceName(b.getInt("background"));
    	((ImageView) findViewById(R.id.bgzinho)).setBackgroundResource(b.getInt("background"));
    	
    	//.setBackgroundResource(R.drawable.thumb09);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.control, menu);
        return true;
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
    	final LinearLayout l=(LinearLayout) findViewById(R.id.progresswindow);
        class Forget extends AsyncTask<Object, Object, Object> {   
        	protected void onPostExecute(Object result){
        		l.setVisibility(View.INVISIBLE);
        	}
    		@Override
    		protected Object doInBackground(Object... params) {
    			try {
    				Thread.sleep(3000);
    			} catch (InterruptedException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
    			return params;
    		}
        }
    	float sw=p.getStrokeWidth();
    	boolean o=false;
    	switch(keyCode){
	    	case KeyEvent.KEYCODE_VOLUME_DOWN:
				sw-=5;
				o=true;
				break;
			case KeyEvent.KEYCODE_VOLUME_UP:
				sw+=5;
				o=true;
				break;
			case KeyEvent.KEYCODE_BACK:
				if(!p.back()){
					Intent intent = new Intent(DedosActivity.this, Choice.class);
				    startActivity(intent);
				}
    	}
    	if(o){
    		l.setVisibility(View.VISIBLE);
    		p.setStrokeWidth(sw);
    		sw=p.getStrokeWidth();
    		((SeekBar) findViewById(R.id.seekBar1)).setProgress(Math.round(100*sw/50));
    		new Forget().execute();
    	}

    	return o;
    }

}
