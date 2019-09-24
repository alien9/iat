package org.bigrs.croqui;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.app.Activity;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.LOG;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionInfo;



import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class BigrsCroqui extends CordovaPlugin {
    public static final int SKETCH_REQUEST = 1000;
    private CallbackContext cc;
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        cc=callbackContext;
        Context context = cordova.getActivity().getApplicationContext();
        PluginResult.Status status = PluginResult.Status.OK;
        if(action.equals("new_activity")) {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage("br.com.cetsp.iat");
            intent.setFlags(0);
            intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, "");

            Log.d("IAT croqui plugin",args.toString());
            JSONObject params=new JSONObject();
            try{
                params=args.getJSONObject(0);
            }catch(JSONException ex){
                Log.d("IAT croqui parse error",ex.getMessage());

            }
            Log.d("IAT croqui params",params.toString());
            try{
                intent.putExtra("placas", params.optString("placas"));
                intent.putExtra("info",params.optString("info"));
                intent.putExtra("latitude",params.optString("latitude"));
                intent.putExtra("longitude",params.optString("longitude"));
                intent.putExtra("zoom",params.optString("zoom"));
                intent.putExtra("size",params.optInt("size",300));
            }catch(Exception ex){
                cc.sendPluginResult(new PluginResult(PluginResult.Status.NO_RESULT, "Programa não encontrado"));
                return false;
            }
            Log.d("IAT croqui longitude",params.optString("longitude"));
            int CAMERA=1;
            int returnType=1;
            if(intent!=null){
                int launchFlags = intent.getFlags();
                Log.d("IAT LAUNCH", "get flags");
                intent.putExtra("starter", "org.bigrs.croqui");
                Log.d("Cordova IAT Plugin", "calling croqui activity");
                this.cordova.startActivityForResult((CordovaPlugin) this, intent, SKETCH_REQUEST);
            }else{
                cc.sendPluginResult(new PluginResult(PluginResult.Status.NO_RESULT, "Programa não encontrado"));
            }
            PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
            r.setKeepCallback(true);
            cc.sendPluginResult(r);
            return true;
        }
        return false;
    }

    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        Log.d("Cordova IAT Plugin", "returned from");
        if(data!=null){
            Log.d("Cordova IAT Croqui Plugin", data.toString());
        }else{
            Log.d("Cordova IAT Croqui Plugin", "dados null");
        }
        String jay=data.getStringExtra("data");
        Log.d("IAT Croqui Plugin one", jay);
        PluginResult.Status status = PluginResult.Status.OK;
        Log.d("IAT Croqui Plugin prepare", "executando callback");
        cc.sendPluginResult(new PluginResult(status, jay));
        Log.d("IAT Croqui Plugin ret", "callback executado");

    }
}
