package br.com.homembala.dedos;

import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

public class DedosActivity extends Activity {
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //final Panel d = new Panel(this, null);
        //setContentView(d);
        setContentView(R.layout.main);
    }

}
