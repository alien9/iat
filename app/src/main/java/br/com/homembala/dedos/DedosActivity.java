package br.com.homembala.dedos;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;

public class DedosActivity extends Activity {
	int bg;
	int bgIndex;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.frame);
		Bundle b = this.getIntent().getExtras();
		bg = b.getInt("background");
		bgIndex = b.getInt("background_index");
		((LinearLayout) findViewById(R.id.bgzinho)).setBackgroundResource(bg);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.control, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.reset_image:
			Intent intent = new Intent(DedosActivity.this, Choice.class);
			startActivity(intent);
			DedosActivity.this.finish();
			break;
		case R.id.share_image:
			// ((FrameLayout) findViewById(R.id.frameLayout1))
			// .removeView((LinearLayout) findViewById(R.id.bgzinho));
			Panel p = (Panel) findViewById(R.id.SurfaceView01);
			p.setDrawingCacheEnabled(true);
			// this is the important code :)
			// Without it the view will have a
			// dimension of 0,0 and the bitmap will
			// be null
			p.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
					MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
			p.layout(0, 0, p.getWidth(), p.getHeight());
			p.buildDrawingCache(true);
			Bitmap bm = Bitmap.createBitmap(p.getDrawingCache());
			p.setDrawingCacheEnabled(false);

			if (bm != null) {
				DownloadImage di = new DownloadImage();
				di.execute(new Object[] { bm });

			}
			p.setVisibility(View.INVISIBLE);
			//bm.recycle();
			// ((LinearLayout)
			// findViewById(R.id.bgzinho)).setBackgroundResource(bg);
		}
		return true;
	}

	private class DownloadImage extends AsyncTask<Object, Object, Object> {
		@Override
		protected void onPostExecute(Object result) {
			Bundle b = new Bundle();
			b.putInt("background_index", bgIndex);
			b.putInt("background", bg);
			Intent intent = new Intent(DedosActivity.this, Formic.class);
			intent.putExtras(b);
			startActivity(intent);
			//DedosActivity.this.finish();
		}

		@Override
		protected Object doInBackground(Object... arg0) {
			try {
				String path = Environment.getExternalStorageDirectory()
						.toString();
				OutputStream fOut = null;
				File file = new File(path, "vivo_samsung_note");
				file.mkdirs();
				file = new File(path, "vivo_samsung_note/screentest.png");
				fOut = new FileOutputStream(file);
				((Bitmap) arg0[0])
						.compress(Bitmap.CompressFormat.PNG, 85, fOut);
				fOut.flush();
				fOut.close();
				arg0 = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		final LinearLayout l = (LinearLayout) findViewById(R.id.progresswindow);
		class Forget extends AsyncTask<Object, Object, Object> {
			protected void onPostExecute(Object result) {
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
		Panel p = (Panel) findViewById(R.id.SurfaceView01);
		float sw = p.getStrokeWidth();
		boolean o = false;
		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			sw -= 5;
			o = true;
			break;
		case KeyEvent.KEYCODE_VOLUME_UP:
			sw += 5;
			o = true;
			break;
		case KeyEvent.KEYCODE_BACK:
			if (!p.back()) {
				Intent intent = new Intent(DedosActivity.this, Choice.class);
				startActivity(intent);
				DedosActivity.this.finish();
				return super.onKeyDown(keyCode, event);
			} else {
				return true;
			}
		}
		if (o) {
			l.setVisibility(View.VISIBLE);
			p.setStrokeWidth(sw);
			sw = p.getStrokeWidth();
			((SeekBar) findViewById(R.id.seekBar1)).setProgress(Math
					.round(100 * sw / 50));
			new Forget().execute();
		}

		return o;
	}

}
