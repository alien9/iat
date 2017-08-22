package org.bigrs.croqui;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import org.bigrs.croqui.util.VehicleFix;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by tiago on 27/03/17.
 */
public class Iat extends Application {
    private static final int IAT_REQUEST_GPS_PERMISSION = 0;
    private static JSONObject lastKnownPosition;
    private static LocationManager locationManager;
    private static Iat singleton;
    private VehicleFix selectedVehicle;

    public static Iat getInstance() {
        return singleton;
    }

    public JSONObject getLastKnownPosition() {
        if(lastKnownPosition==null){
            SharedPreferences sharedpreferences = getSharedPreferences("position", Context.MODE_PRIVATE);
            try {
                return new JSONObject(sharedpreferences.getString("last", "{}"));
            } catch (JSONException ignore) {}
        }
        return lastKnownPosition;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        singleton = this;
    }
    public void setLastKnownPosition(JSONObject l) {
        lastKnownPosition = l;
        SharedPreferences sharedpreferences = getSharedPreferences("position", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString("last", lastKnownPosition.toString());
        editor.commit();

    }

    public void startGPS(Activity a) {
        Log.d("IAT SERVICE", "should start");
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)||(ActivityCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)) {
            Log.d("IAT SERVICE", "permission nort granted");
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.AppTask> tasks = activityManager.getAppTasks();
            tasks.get(0);
            ActivityCompat.requestPermissions(a,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    IAT_REQUEST_GPS_PERMISSION);
            if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)&&(ActivityCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED)) {
                Intent intent = new Intent(this, CsiActivity.class);
                startActivity(intent);
            }
            return;
        }
        Log.d("IAT SERVICE", "start now");
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), LocationService.class);
        startService(intent);
    }

    public void setSelectedVehicle(VehicleFix sv) {
        selectedVehicle = sv;
    }

    public VehicleFix getSelectedVehicle() {
        return selectedVehicle;
    }

    public void updateSelectedVehicle(VehicleFix view) {

    }
}
