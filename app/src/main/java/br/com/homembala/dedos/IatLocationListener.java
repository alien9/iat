package br.com.homembala.dedos;

import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tiago on 27/03/17.
 */
public class IatLocationListener implements android.location.LocationListener {
        private static final long MIN_TIME_ALLOWED = 5000;
    public static final int POSITION_UPDATE = 1;
    private final Handler handler;
        private long last;

        public IatLocationListener(Handler h){
            handler=h;
            last=0;
        }
        final String LOG_LABEL = "Location Listener>>";

        @Override
        public void onLocationChanged(Location location) {
            Log.d("iat", LOG_LABEL + "Location Changed");
            if (location != null) {
                long past = location.getTime() - last;
                if(past>MIN_TIME_ALLOWED) {
                    Log.d("iat", "entrou");
                    Message mess = new Message();
                    mess.what = POSITION_UPDATE;
                    JSONObject h = new JSONObject();
                    try {
                        h.put("accuracy", location.getAccuracy());
                        h.put("altitude", location.getAltitude());
                        h.put("latitude", location.getLatitude());
                        h.put("longitude", location.getLongitude());
                        h.put("speed", location.getSpeed());
                        h.put("bearing", location.getBearing());
                        h.put("time", location.getTime());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    last = location.getTime();
                    mess.getData().putString("location", h.toString());
                    handler.sendMessage(mess);
                    double longitude = location.getLongitude();
                    Log.d("iat", LOG_LABEL + "Longitude:" + longitude);
                    //Toast.makeText(getApplicationContext(),"Long::"+longitude,Toast.LENGTH_SHORT).show();
                    double latitude = location.getLatitude();
                    ///Toast.makeText(getApplicationContext(),"Lat::"+latitude,Toast.LENGTH_SHORT).show();
                    Log.d("iat", "Latitude:" + latitude);
                }
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }

}
