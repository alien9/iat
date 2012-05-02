package br.com.homembala.dedos;

import ogrelab.org.apache.http.HttpResponse;
import ogrelab.org.apache.http.client.ClientProtocolException;
import ogrelab.org.apache.http.client.HttpClient;
import ogrelab.org.apache.http.client.ResponseHandler;
import ogrelab.org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import ogrelab.org.apache.http.client.methods.HttpGet;
import ogrelab.org.apache.http.client.methods.HttpPost;
import ogrelab.org.apache.http.client.methods.HttpUriRequest;
import ogrelab.org.apache.http.entity.mime.HttpMultipartMode;
import ogrelab.org.apache.http.entity.mime.MultipartEntity;
import ogrelab.org.apache.http.entity.mime.content.StringBody;
import ogrelab.org.apache.commons.logging.LogFactory;
import ogrelab.org.apache.http.entity.mime.content.FileBody;
import ogrelab.org.apache.http.impl.client.BasicResponseHandler;
import ogrelab.org.apache.http.impl.client.DefaultHttpClient;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;

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
		if (b == null) {
			bgIndex = 0;
			background = 0;
		} else {
			bgIndex = (b.containsKey("background_index")) ? b
					.getInt("background_index") : 0;
			background = (b.containsKey("background")) ? b.getInt("background")
					: 0;
		}
		setContentView(R.layout.formic);
		((LinearLayout) findViewById(R.id.bgzinho))
				.setBackgroundResource(background);
		final File file = new File(Environment.getExternalStorageDirectory()
				.toString() + "/vivo_samsung_note/screentest.png");
		Bitmap bm = BitmapFactory.decodeFile(Environment
				.getExternalStorageDirectory().toString()
				+ "/vivo_samsung_note/screentest.png");
		((ImageView) findViewById(R.id.dibujo)).setImageBitmap(bm);
		final Context me = Formic.this;
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		// imm.hideSoftInputFromWindow(((Button)
		// findViewById(R.id.enviado)).getWindowToken(), 0);
		((Button) findViewById(R.id.enviado))
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						HttpClient client = new DefaultHttpClient();
						HttpUriRequest request;
						String mess = "";
						View foca=(View) findViewById(R.id.editText1);
						if (((EditText) findViewById(R.id.editText1)).getText()
								.toString().length() == 0) {
							mess = "Preencha seu nome";
							foca=(View) findViewById(R.id.editText1);
						} else if (((EditText) findViewById(R.id.editText2))
								.getText().toString().length() < 11) {
							mess = "CPF deve ter 11 dígitos.";
							foca=(View) findViewById(R.id.editText2);
						} else if (((EditText) findViewById(R.id.editText7))
								.getText().toString().length() != 15) {
							foca=(View) findViewById(R.id.editText7);
							mess = "IMEI inválido";
						} else {
							request = new HttpGet(
									"http://galaxynotevivo.com.br/imei.php?imei="
											+ ((EditText) findViewById(R.id.editText7))
													.getText().toString());
							try {
								ResponseHandler<String> rh = new BasicResponseHandler();
								String r = client.execute(request, rh);
								JSONObject j = new JSONObject(r);
								if (j.get("exists").toString().equals("true"))
									mess = "IMEI já cadastrado.";
							} catch (ClientProtocolException e) {
								mess = "Erro de rede.";
							} catch (IOException e) {
								mess = "Erro de rede.";
							} catch (JSONException e) {
								mess = "Erro do servidor.";
							}
						}
						if (!mess.equals("")) {

							builder.setMessage(mess)
									.setCancelable(false)
									.setPositiveButton(
											"OK",
											new DialogInterface.OnClickListener() {
												public void onClick(
														DialogInterface dialog,
														int id) {
													dialog.cancel();
												}
											});

							AlertDialog alert = builder.create();
							alert.show();
							foca.requestFocus();
							return;
						}
						imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
						pd = ProgressDialog.show(me,
								me.getString(R.string.send),
								me.getString(R.string.sending), true, false);

						request = new HttpPost(
								"http://galaxynotevivo.com.br/participantes_insere.php");
						//MultipartEntity form = new MultipartEntity(HttpMultipartMode.STRICT,null,Charset.forName("UTF-8"));
						MultipartEntity form = new MultipartEntity();
						// disable expect-continue handshake (lighttpd doesn't
						// support
						client.getParams().setBooleanParameter(
								"http.protocol.expect-continue", false);
						form.addPart("imagem", new FileBody(file));
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

							String t = ((RadioButton) findViewById(((RadioGroup) findViewById(R.id.radioGroup1))
									.getCheckedRadioButtonId())).getText()
									.toString();

							form.addPart(
									"capa",
									new StringBody(
											(t.equals(getString(R.string.at_home)) ? "1"
													: "")));
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
									response = ((HttpClient) arg0[0])
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
						Intent intent = new Intent(Formic.this,
								RadioActivity.class);
						startActivity(intent);
						Formic.this.finish();
					}
				});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			Bundle b = new Bundle();
			b.putInt("background", background);
			b.putInt("background_index", bgIndex);
			Intent intent = new Intent(Formic.this, DedosActivity.class);
			intent.putExtras(b);
			startActivity(intent);
			Formic.this.finish();
		}
		return super.onKeyDown(keyCode, event);
	}

}
