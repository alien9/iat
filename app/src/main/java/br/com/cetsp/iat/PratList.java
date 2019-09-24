package br.com.cetsp.iat;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import javax.sql.RowSet;

import br.com.cetsp.iat.ftt.FormControl;
import br.com.cetsp.iat.util.VehicleFix;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PratList extends AppCompatActivity{
    private static final int SKETCH_REQUEST = 1000;
    private int currentPosition;
    private boolean mark_to_exit=false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Iat iat = (Iat) getApplicationContext();
        /*
        if(!iat.isAuthenticated()){
            Intent intent = new Intent(this, Login.class);
            startActivity(intent);
            return;
        }
        */
        Intent intent=getIntent();
        if(intent.hasExtra("info")){
            Intent i= new Intent(this, CsiActivity.class);
            i.putExtras(intent.getExtras());
            startActivityForResult(i,SKETCH_REQUEST);
            return;
        }
        setContentView(R.layout.lista_prat);
        ((ListView) findViewById(R.id.listview)).setAdapter(new IatAdapter(this, iat.getReport()));
        setTitle(R.string.app_name);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.app_name));
        setSupportActionBar(toolbar);
        final Context context = this;
        ((ListView) findViewById(R.id.listview)).setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Iat iat = (Iat) getApplicationContext();
                try {
                    JSONObject d=new JSONObject(iat.getReport(position));
                    if(!d.has("sent")) {
                        Intent intent = new Intent(context, CsiActivity.class);
                        currentPosition = position;
                        intent.putExtra("info", d.optJSONObject("info").toString());
                        startActivityForResult(intent, 1);
                    }
                } catch (JSONException e) {
                    Toast.makeText(context, getString(R.string.not_found), Toast.LENGTH_LONG);
                }
                Toast.makeText(context, "ok, recebido", Toast.LENGTH_LONG);

            }
        });
    }

    public void createReport(View view) {
        currentPosition=-1;
        Intent intent = new Intent(this, CsiActivity.class);
        startActivityForResult(intent, 1);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==SKETCH_REQUEST){
            Log.d("Cordova IAT Plugin", "returned from");
            if(data!=null){
                Log.d("Cordova IAT Croqui", data.toString());
            }else{
                Log.d("Cordova IAT Croqui", "dados null");
            }
            String jay=data.getStringExtra("data");
            Log.d("IAT Croqui Plugin one", jay);
            setResult(RESULT_OK, data);
            finish();
            return;
        }
        Iat iat = (Iat) getApplicationContext();
        iat.append(data.getStringExtra("data"), currentPosition);
        Toast.makeText(this,"Mensagem Recebida ", Toast.LENGTH_LONG).show();
        ((ListView) findViewById(R.id.listview)).setAdapter(new IatAdapter(this, iat.getReport()));
    }

    private class IatAdapter extends ArrayAdapter<String> {
        private final ArrayList<String> items;

        IatAdapter(Context c, ArrayList<String> j) {
            super(c, 0);
            items=j;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            try {
                JSONObject item= new JSONObject(items.get(position));
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_prat, parent, false);
                    byte[] decodedString = Base64.decode(item.getString("thumbnail"), Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    decodedByte=Bitmap.createBitmap(decodedByte, 150,150,200, 200);
                    ((ImageView)convertView.findViewById(R.id.thumbnail)).setImageBitmap(decodedByte);
                    ArrayList<String> misshaps=new ArrayList<>();
                    JSONObject oa = item.optJSONObject("info");
                    if(oa!=null){
                        convertView.setBackgroundColor(getResources().getColor(R.color.white));
                        if(item.has("sent"))
                            convertView.setBackgroundColor(Color.parseColor("#cccccc"));

                        JSONArray a = oa.optJSONArray("vehicles");
                        if(a!=null){
                            final DateFormat dateFormat = new SimpleDateFormat("dd/M/yyyy HH:mm");
                            ArrayList<Date> datas=new ArrayList<>();
                            for(int i=0;i<a.length();i++){
                                if(a.optJSONObject(i).optInt("model")==VehicleFix.COLISAO){
                                    misshaps.add(a.optJSONObject(i).optString("tipo_impacto"));
                                    datas.add(dateFormat.parse(a.optJSONObject(i).optString("data_e_hora")));
                                }
                            }

                            if(datas.size()>0){
                                Collections.sort(datas);
                                ((TextView)convertView.findViewById(R.id.textView_item_data)).setText(dateFormat.format(datas.get(0)));
                            }
                        }
                    }
                    TextView t = (TextView) convertView.findViewById(R.id.textView_item);
                    t.setText(TextUtils.join(", ", misshaps));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (ParseException ignore) {

            }
            return convertView;
        }
        public int getCount() {
            return items.size();
        }
    }
    public void onBackPressed(){
        return;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.lista_prat_menu, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout_menu:
                mark_to_exit=true;
                sync();
                return true;
            case R.id.sync:
                sync();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void sync(){
        sync(0);
    }
    private void sync(int k) {
        Iat iat= (Iat) getApplicationContext();
        ArrayList<String> r = iat.getReport();
        if(k>=r.size()) {
            if(mark_to_exit) {
                iat.setSession(null);
                finish();
            }
            return;
        }
        try {
            JSONObject j = new JSONObject(r.get(k));
            if(!j.has("sent")){
                (new Uploader(this, k)).execute(r.get(k));
            }else{
                sync(k+1);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private class Uploader extends AsyncTask<String, Integer, Boolean> {
        private final Context context;
        private final int index;

        public Uploader(Context c, int i) {
            context=c;
            index=i;
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            String response="";
            try {
                MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
                OkHttpClient client = FormControl.getHttpClient(context);
                Request request = new Request.Builder()
                        .url(getString(R.string.backend_url_dev) + "webservices/FTT/api/upload")
                        .post(RequestBody.create(MEDIA_TYPE_JSON, strings[0]))
                        .build();

                Response responsec = client.newCall(request).execute();
                if (!responsec.isSuccessful()) {
                    if(responsec.code()==504){
                        response = "504: Server not responding";
                    }
                    return false;
                } else {
                    return true;
                }
            } catch (IOException e) {
                response = "Rede não disponível";
            }
            return false;
        }
        @Override
        protected void onPostExecute(Boolean returnVal) {
            if (!returnVal) {
                Toast.makeText(context, ":-(", Toast.LENGTH_LONG).show();
            } else {
                Iat iat = (Iat) getApplicationContext();
                iat.markAsSent(index);
                ((ListView) findViewById(R.id.listview)).setAdapter(new IatAdapter(context, iat.getReport()));
                sync(index + 1);
            }
        }
    }
}
