package br.com.cetsp.iat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import br.com.cetsp.iat.ftt.FormControl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Login  extends AppCompatActivity{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Iat iat = (Iat) getApplicationContext();
        setContentView(R.layout.login);
    }

    public void login(View view) {
        String username=((EditText) findViewById(R.id.login_text)).getText().toString();
        String password=((EditText) findViewById(R.id.password_text)).getText().toString();
        JSONObject jp = new JSONObject();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            jp.put("backend_url", getString(R.string.backend_url));
            jp.put("username", username);
            jp.put("senha", password);
            jp.put("dataHoraEquipamento", sdf.format(new Date()));
            jp.put("idFormulario",Iat.FORMULARIO_SPTRANS);
            jp.put("imei", 2);
            //try {
            //jp.put("imei", ((TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId());
            //}catch(SecurityException xu){
            //    Toast.makeText(this, getString(R.string.auth_required), Toast.LENGTH_LONG).show();
            //}
//
// }  jp.put("imei", "354991057221832"); // DEV ONLY
            jp.put("version", getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
        } catch (JSONException | PackageManager.NameNotFoundException ignored) {}
        if (username.equals("") || password.equals("")) {
            Toast.makeText(this, "Por favor preencha todos os campos", Toast.LENGTH_LONG).show();
        }
        (new LoginAgent((Context) this)).execute(jp.toString());
    }

    private class LoginAgent extends AsyncTask<String, Integer, String>{
        private final Context context;

        LoginAgent(Context context) {
            this.context = context;

        }
        @Override
        protected String doInBackground(String... strings) {
            String response="";
            try {
                MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
                OkHttpClient client = FormControl.getHttpClient(context);
                Request request = new Request.Builder()
                        .url(getString(R.string.backend_url_dev) + "webservices/FTT/api/login")
                        .post(RequestBody.create(MEDIA_TYPE_JSON, strings[0]))
                        .build();

                Response responsec = client.newCall(request).execute();
                if (!responsec.isSuccessful()) {
                    if(responsec.code()==504){
                        response = "504: Server not responding";
                    }else {
                        throw new IOException("Unexpected code " + responsec.code());
                    }
                } else {
                    Iat iat = (Iat) getApplicationContext();
                    iat.setSession(new JSONObject(responsec.body().string()));
                    Intent intent = new Intent(context, PratList.class);
                    startActivity(intent);
                }
            } catch (IOException e) {
                response = "Rede não disponível";
            } catch (JSONException e) {
                response = e.getMessage();
            }
            return response;
        }
        @Override
        protected void onPostExecute(String returnVal) {
            if(returnVal.length()>0)
                Toast.makeText(context,returnVal,Toast.LENGTH_LONG).show();
        }
    }
}
