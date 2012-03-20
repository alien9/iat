package br.com.homembala.dedos;

import java.util.ArrayList;

import android.app.Activity;
import android.graphics.Paint;
import android.os.Bundle;

public class DedosActivity extends Activity {
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Panel d = new Panel(this, null);
        
        setContentView(d);

    }

}
