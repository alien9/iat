package br.com.homembala.dedos;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ViewAnimator;

public class Choice extends Activity implements View.OnTouchListener {
	float here = 0;
	ViewAnimator vu;
	View.OnClickListener ic;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		setContentView(R.layout.chosen);
		((LinearLayout) findViewById(R.id.linearLayout1))
				.setOnTouchListener(this);
		vu = (ViewAnimator) findViewById(R.id.ViewFlipper1);
		View.OnClickListener c = new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (v.getId() == R.id.pagedown) {
					pageUp();
				} else {
					pageDown();
				}
			}
		};
		((Button) findViewById(R.id.pagedown)).setOnClickListener(c);
		((Button) findViewById(R.id.pageup)).setOnClickListener(c);
		ic = new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!v.getClass().toString()
						.equals("class android.widget.Button"))
					return;
				Button but = (Button) v;
				Resources res = getResources();
				String name = res.getResourceName(v.getId());
				name = name.replaceAll("\\D", "");
				name = String.format("%02d", Integer.parseInt(name));
				Bundle b = new Bundle();
				int bgid = getResources().getIdentifier("cap" + name,
						"drawable", getPackageName());

				b.putInt("background", bgid);
				b.putInt("background_index", Integer.parseInt(name));

				Intent intent = new Intent(Choice.this, DedosActivity.class);
				intent.putExtras(b);
				startActivity(intent);
				System.exit(0);
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
		((Button) findViewById(R.id.Button0)).setOnClickListener(ic);

		((Button) findViewById(R.id.Button1)).setOnTouchListener(this);
		((Button) findViewById(R.id.Button2)).setOnTouchListener(this);
		((Button) findViewById(R.id.Button3)).setOnTouchListener(this);
		((Button) findViewById(R.id.Button4)).setOnTouchListener(this);
		((Button) findViewById(R.id.Button5)).setOnTouchListener(this);
		((Button) findViewById(R.id.Button6)).setOnTouchListener(this);
		((Button) findViewById(R.id.Button7)).setOnTouchListener(this);
		((Button) findViewById(R.id.Button8)).setOnTouchListener(this);
		((Button) findViewById(R.id.Button9)).setOnTouchListener(this);
		((Button) findViewById(R.id.Button10)).setOnTouchListener(this);
		((Button) findViewById(R.id.Button11)).setOnTouchListener(this);
		((Button) findViewById(R.id.Button12)).setOnTouchListener(this);
		((Button) findViewById(R.id.Button0)).setOnTouchListener(this);
	}

	private void pageUp() {
		vu.setInAnimation(inFromLeftAnimation());
		vu.setOutAnimation(outToRightAnimation());
		vu.showPrevious();
	}

	private void pageDown() {
		vu.setInAnimation(inFromRightAnimation());
		vu.setOutAnimation(outToLeftAnimation());
		vu.showNext();
	}

	private Animation inFromLeftAnimation() {
		Animation inFromLeft = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT, -1.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f);
		inFromLeft.setDuration(250);
		inFromLeft.setInterpolator(new AccelerateInterpolator());
		return inFromLeft;
	}

	private Animation outToRightAnimation() {
		Animation outtoRight = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, +1.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f);
		outtoRight.setDuration(250);
		outtoRight.setInterpolator(new AccelerateInterpolator());
		return outtoRight;
	}

	private Animation outToLeftAnimation() {
		Animation inFromLeft = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, -1.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f);
		inFromLeft.setDuration(250);
		inFromLeft.setInterpolator(new AccelerateInterpolator());
		return inFromLeft;
	}

	private Animation inFromRightAnimation() {
		Animation outtoRight = new TranslateAnimation(
				Animation.RELATIVE_TO_PARENT, +1.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f,
				Animation.RELATIVE_TO_PARENT, 0.0f);
		outtoRight.setDuration(250);
		outtoRight.setInterpolator(new AccelerateInterpolator());
		return outtoRight;
	}

	@Override
	public boolean onTouch(View arg0, MotionEvent event) {

		float x = event.getX();
		float y = event.getY();

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			here = x;
			break;
		case MotionEvent.ACTION_MOVE:
			if (here == 0)
				break;
			if (Math.abs(here - x) < 50)
				break;
			if (here < x)
				pageUp();
			else if (here > x)
				pageDown();
			here = 0;
			break;
		case MotionEvent.ACTION_UP:
			if (Math.abs(here - x) < 10)
				ic.onClick(arg0);
			here = 0;
			// break;
			return true;
		}
		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			Intent intent = new Intent(Choice.this, RadioActivity.class);
			startActivity(intent);
			System.exit(0);
			break;
		case KeyEvent.KEYCODE_HOME:
			System.exit(0);
		}

		return false;

	}
}
