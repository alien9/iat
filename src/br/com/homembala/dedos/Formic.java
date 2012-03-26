package br.com.homembala.dedos;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.methods.HttpPost;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.CheckBox;

import android.widget.ImageView;
import android.widget.LinearLayout;

public class Formic extends Activity {
	int bgIndex, background;
	ProgressDialog pd;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		Bundle b = this.getIntent().getExtras();
		bgIndex = b.getInt("background_index");
		background = b.getInt("background");
		setContentView(R.layout.formic);
		((LinearLayout) findViewById(R.id.bgzinho)).setBackgroundResource(background);
		final File file = new File(Environment.getExternalStorageDirectory()
				.toString() + "/vivo_samsung_note/screentest.png");
		Bitmap bm = BitmapFactory.decodeFile(Environment
				.getExternalStorageDirectory().toString()
				+ "/vivo_samsung_note/screentest.png");
		((ImageView) findViewById(R.id.dibujo)).setImageBitmap(bm);
		final Context me = Formic.this;
		((Button) findViewById(R.id.enviado))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						pd = ProgressDialog.show(me,
								me.getString(R.string.send),
								me.getString(R.string.sending), true, false);
						HttpClient client = new DefaultHttpClient();
						HttpUriRequest request = new HttpPost(
								"http://galaxynotevivo.com.br/participantes_insere.php");
						MultipartEntity form = new MultipartEntity();
						// disable expect-continue handshake (lighttpd doesn't
						// support
						client.getParams().setBooleanParameter(
								"http.protocol.expect-continue", false);
						form.addPart(
								"imagem",
								new org.apache.http.entity.mime.content.FileBody(
										file));
						try {
							form.addPart("nome", new StringBody(
									((EditText) findViewById(R.id.editText1))
											.getText().toString()));
							form.addPart("cpf", new StringBody(
									((EditText) findViewById(R.id.editText2))
											.getText().toString()));
							form.addPart("email", new StringBody(
									((EditText) findViewById(R.id.editText3))
											.getText().toString()));
							form.addPart("telefone", new StringBody(
									((EditText) findViewById(R.id.editText4))
											.getText().toString()));
							form.addPart("endereco", new StringBody(
									((EditText) findViewById(R.id.editText5))
											.getText().toString()));
							form.addPart("cidade", new StringBody(
									((EditText) findViewById(R.id.editText6))
											.getText().toString()));
							form.addPart("imei", new StringBody(
									((EditText) findViewById(R.id.editText7))
											.getText().toString()));
							form.addPart("capa", new StringBody(
									""+((CheckBox) findViewById(R.id.checkBox1)).isChecked()));
							form.addPart("background", new StringBody(""
									+ bgIndex));

						} catch (UnsupportedEncodingException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}

						((HttpEntityEnclosingRequestBase) request)
								.setEntity(form);

						class FranticSender extends
								AsyncTask<Object, Object, Object> {

							@Override
							protected Object doInBackground(Object... arg0) {
								HttpResponse response = null;

								try {
									response=((HttpClient) arg0[0])
											.execute((HttpUriRequest) arg0[1]);
								} catch (ClientProtocolException e) {
								} catch (IOException ee) {
								}

								return response;
							}

							protected void onPostExecute(Object result) {
								((LinearLayout) findViewById(R.id.overflow))
										.setVisibility(View.VISIBLE);
								((LinearLayout) findViewById(R.id.bgzinho))
										.setVisibility(View.VISIBLE);
								((LinearLayout) findViewById(R.id.success))
										.setVisibility(View.VISIBLE);
								pd.dismiss();
							}
						}
						FranticSender fs = new FranticSender();
						fs.execute(new Object[] { client, request });

					}
				});
		((Button) findViewById(R.id.end))
		.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent intent = new Intent(Formic.this, RadioActivity.class);
				startActivity(intent);
				System.exit(0);
			}
		});
	}
	

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			Intent intent = new Intent(Formic.this, Choice.class);
			startActivity(intent);
			System.exit(0);
		}
		return false;

	}

}
