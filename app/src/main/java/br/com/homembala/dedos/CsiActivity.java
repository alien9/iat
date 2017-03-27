package br.com.homembala.dedos;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by tiago on 27/03/17.
 */
public class CsiActivity extends AppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.csi);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        ((Iat)getApplicationContext()).startGPS(this);
    }

    @Override
    public void onBackPressed() {
        ((Panel) findViewById(R.id.drawing_panel)).back();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.drawing_controls, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.request_background:
                loadBackground();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    Double[] degrees2meters(double lon, double lat) {
        double x = lon * 20037508.34 / 180;
        double y = Math.log(Math.tan((90 + lat) * Math.PI / 360)) / (Math.PI / 180);
        y = y * 20037508.34 / 180;
        return new Double[]{x, y};
    }

    private void loadBackground() {
        JSONObject position = ((Iat) getApplicationContext()).getLastKnownPosition();
        if(position==null){
            return;
        }
        if(position.length()==0)return;
        new MapLoader(position).execute();
    }

    private class MapLoader extends AsyncTask<String,String,Boolean>{
        private final JSONObject position;

        public MapLoader(JSONObject p) {
            position=p;
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            OkHttpClient client = new OkHttpClient.Builder().build();
            Double[] center = degrees2meters(position.optDouble("latitude"), position.optDouble("longitude"));
            String url=String.format("%s?service=WMS&version=1.1.0&request=GetMap&layers=BIGRS:sirgas_shp_quadraviaria_&styles=&bbox=%s,%s,%s,%s&width=656&height=768&srs=EPSG:31983&format=application/openlayers",
                    getResources().getString(R.string.wms_url),
                    center[0]-1000,
                    center[1]-1000,
                    center[0]+1000,
                    center[1]+1000
            );
//http://bigrs.alien9.net:8080/geoserver/BIGRS/wms?service=WMS&version=1.1.0&request=GetMap&layers=BIGRS:sirgas_shp_quadraviaria_&styles=&bbox=313086.375,7360294.0,361095.90625,7416448.5&width=656&height=768&srs=EPSG:31983&format=application/openlayers
            Request request = new Request.Builder()
                    .url(url)
                    .build();
            try {
                Response response = client.newCall(request).execute();
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }
}