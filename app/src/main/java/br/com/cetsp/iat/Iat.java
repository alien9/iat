package br.com.cetsp.iat;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import br.com.cetsp.iat.util.VehicleFix;
import jsqlite.Database;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tiago on 27/03/17.
 */
public class Iat extends Application {
    private static final int IAT_REQUEST_GPS_PERMISSION = 0;
    public static final int FORMULARIO_SPTRANS = 99;
    private static JSONObject lastKnownPosition;
    private static LocationManager locationManager;
    private static Iat singleton;
    private VehicleFix selectedVehicle;
    private String starter;
    private JSONObject session;

    private ArrayList<String> reports;
    private Database jdb;

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
        reports= new ArrayList<>();
        SharedPreferences prefs = getSharedPreferences("PRATT", MODE_PRIVATE);
        int i=0;
        while(prefs.getString(String.format("prat_%09d", i),null)!=null){
            reports.add(prefs.getString(String.format("prat_%09d", i),null));
            i++;
        };
        session=null;
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        singleton = this;
        jdb = new Database();
    }
    public void setLastKnownPosition(JSONObject l) {
        lastKnownPosition = l;
        SharedPreferences sharedpreferences = getSharedPreferences("position", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putString("last", lastKnownPosition.toString());
        editor.commit();

    }
    public Database getDatabase(String path) {
        if(jdb==null){
            Log.d("IAT DATABASE CREATION ", "It's out of place.");
            jdb=new Database();
        }
        File quadras=new File(path);
        if(!quadras.exists()){
            InputStream in = this.getApplicationContext().getResources().openRawResource(R.raw.db);
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(path);
                byte[] buff = new byte[1024];
                int read = 0;
                while ((read = in.read(buff)) > 0) {
                    out.write(buff, 0, read);
                }
                in.close();
                out.close();
            } catch (IOException e) {
                return jdb;
            }
        }
        return jdb;

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void startGPS(Activity a) {
        Log.d("IAT SERVICE", "should start");
        if ((ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)||(ActivityCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)) {
            Log.d("IAT SERVICE", "permission nort granted");
            ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.AppTask> tasks = activityManager.getAppTasks();
            if(tasks.size()>0) {
                tasks.get(0);
            }
            ActivityCompat.requestPermissions(a,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.WRITE_EXTERNAL_STORAGE},
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

    public void setStarter(String s) {
        starter = s;
    }

    public String getStarter() {
        return starter;
    }

    public boolean isAuthenticated() {
        return session!=null;
    }

    public void setSession(JSONObject j) {
        session=j;
    }

    public void append(String data, int position) {
        SharedPreferences.Editor editor = getSharedPreferences("PRATT", MODE_PRIVATE).edit();
        if(position>=0) {
            reports.set(position, data);
        }else {
            reports.add(data);
        }
        for(int i=0;i<reports.size();i++){
            editor.putString(String.format("prat_%09d", i), reports.get(i));
        }
        editor.apply();
    }
    public ArrayList<String> getReport(){
        return reports;
    }

    public String getReport(int n){
        return reports.get(n);
    }

    public void markAsSent(int index) {
        try {
            JSONObject j=new JSONObject(reports.get(index));
            j.put("sent", true);
            append(j.toString(),index);
        } catch (JSONException ignore) {

        }
    }

    public void eraseReports() {
        reports=new ArrayList<>();
        SharedPreferences prefs = getSharedPreferences("PRATT", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        int i=0;
        while(prefs.contains(String.format("prat_%09d", i))){
            editor.remove(String.format("prat_%09d", i));
            i++;
        }
        editor.apply();
    }
}
