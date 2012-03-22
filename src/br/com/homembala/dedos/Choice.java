package br.com.homembala.dedos;

import java.io.Serializable;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ViewFlipper;

public class Choice extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.chosen);
		final ViewFlipper vu = (ViewFlipper) findViewById(R.id.ViewFlipper1);
		View.OnClickListener c = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (v.getId() == R.id.pagedown)
					vu.showPrevious();
				else
					vu.showNext();
			}
		};
		((Button) findViewById(R.id.pagedown)).setOnClickListener(c);
		((Button) findViewById(R.id.pageup)).setOnClickListener(c);
		View.OnClickListener ic=new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Button but=(Button) v;
				Resources res=getResources();
				String name=res.getResourceName(v.getId());
				name=name.replaceAll("D", "");
				//name=String.format("%05w", name);
				Bundle b=new Bundle();
				int bgid=getResources().getIdentifier("thumb01", "drawable", getPackageName());

				b.putInt("background", bgid);
				
				Intent intent = new Intent(Choice.this, DedosActivity.class);
				intent.putExtras(b);
			    startActivity(intent);
			}
		};
		((Button) findViewById(R.id.Button1)).setOnClickListener(ic);
		((Button) findViewById(R.id.Button2)).setOnClickListener(ic);
		((Button) findViewById(R.id.Button3)).setOnClickListener(ic);
		((Button) findViewById(R.id.Button4)).setOnClickListener(ic);
		((Button) findViewById(R.id.Button5)).setOnClickListener(ic);
		((Button) findViewById(R.id.Button6)).setOnClickListener(ic);
		((Button) findViewById(R.id.Button7)).setOnClickListener(ic);
		((Button) findViewById(R.id.Button8)).setOnClickListener(ic);
		((Button) findViewById(R.id.Button9)).setOnClickListener(ic);
		((Button) findViewById(R.id.Button10)).setOnClickListener(ic);
		((Button) findViewById(R.id.Button11)).setOnClickListener(ic);
		((Button) findViewById(R.id.Button12)).setOnClickListener(ic);
	}
}
