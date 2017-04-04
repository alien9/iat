package br.com.homembala.dedos;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by tiago on 27/03/17.
 */
public class LocationService extends Service{
    private static final long MIN_TIME_BW_UPDATES = 10;
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 10;
    private LocationManager locationManager;

    @Override
    public void onCreate() {
        //android.os.Debug.waitForDebugger();
        super.onCreate();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        final Handler locator = new Chandler();
        IatLocationListener l = new IatLocationListener(locator);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("iat SERVICE", "permission estercated");
            return START_REDELIVER_INTENT;
        }else {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, l);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, l);
            return START_NOT_STICKY;
        }
    }
    private class Chandler extends Handler {
        public void handleMessage (Message msg){
            if(msg.what==IatLocationListener.POSITION_UPDATE){
                try {
                    ((Iat)getApplicationContext()).setLastKnownPosition(new JSONObject(msg.getData().getString("location")));
                } catch (JSONException ignore) {
                    Log.d("iat SERVICE", "position error");
                }
            }
        }
    }

}
