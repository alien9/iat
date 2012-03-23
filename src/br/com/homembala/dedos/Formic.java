package br.com.homembala.dedos;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Formic extends Activity {
	int bgIndex,background;
	String imei;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		Bundle b = this.getIntent().getExtras();
		bgIndex = b.getInt("background_index");
		background = b.getInt("background");
		setContentView(R.layout.formic);
		((ImageView) findViewById(R.id.bgzinho)).setImageResource(background);
		Bitmap bm=BitmapFactory.decodeFile(Environment.getExternalStorageDirectory().toString()+"/vivo_samsung_note/screentest.png");
		((ImageView) findViewById(R.id.dibujo)).setImageBitmap(bm);
		TelephonyManager telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
		imei=telephonyManager.getDeviceId();
		((TextView) findViewById(R.id.textView1)).setText(imei);
		((Button) findViewById(R.id.enviado)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				((LinearLayout) findViewById(R.id.overflow)).setVisibility(View.VISIBLE);
				((LinearLayout) findViewById(R.id.results)).setVisibility(View.VISIBLE);
			}
		});
	}

}
