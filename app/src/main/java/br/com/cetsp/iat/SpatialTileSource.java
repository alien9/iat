package br.com.cetsp.iat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import org.osmdroid.api.IMapView;
import org.osmdroid.tileprovider.BitmapPool;
import org.osmdroid.tileprovider.ReusableBitmapDrawable;
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase;
import org.osmdroid.tileprovider.util.Counters;

import java.io.File;

public class SpatialTileSource extends BitmapTileSourceBase {
    private int mTileSizePixels;
    public SpatialTileSource(String aName, int aZoomMinLevel, int aZoomMaxLevel, int aTileSizePixels, String aImageFilenameEnding) {

        super(aName, aZoomMinLevel, aZoomMaxLevel, aTileSizePixels, aImageFilenameEnding);
        mTileSizePixels = aTileSizePixels;
    }


    @Override
    public Drawable getDrawable(final String aFilePath) throws LowMemoryException {
        Log.d(IMapView.LOGTAG, aFilePath + " attempting to load bitmap");
        try {
            // default implementation will load the file as a bitmap and create
            // a BitmapDrawable from it
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            BitmapPool.getInstance().applyReusableOptions(
                    bitmapOptions, mTileSizePixels, mTileSizePixels);
            final Bitmap bitmap;
            //fix for API 15 see https://github.com/osmdroid/osmdroid/issues/227
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
                bitmap=BitmapFactory.decodeFile(aFilePath);
            else
                bitmap = BitmapFactory.decodeFile(aFilePath, bitmapOptions);
            if (bitmap != null) {
                return new ReusableBitmapDrawable(bitmap);
            } else {
                File bmp = new File(aFilePath);
                if (bmp.exists()) {
                    // if we couldn't load it then it's invalid - delete it
                    Log.d(IMapView.LOGTAG, aFilePath + " is an invalid image file, deleting...");
                    try {
                        new File(aFilePath).delete();
                    } catch (final Throwable e) {
                        Log.e(IMapView.LOGTAG, "Error deleting invalid file: " + aFilePath, e);
                    }
                } else
                    Log.d(IMapView.LOGTAG, "Request tile: " + aFilePath + " does not exist");
            }
        } catch (final OutOfMemoryError e) {
            Log.e(IMapView.LOGTAG,"OutOfMemoryError loading bitmap: " + aFilePath);
            System.gc();
            throw new LowMemoryException(e);
        } catch (final Exception e){
            Log.e(IMapView.LOGTAG,"Unexpected error loading bitmap: " + aFilePath,e);
            Counters.tileDownloadErrors++;
            System.gc();
        }
        return null;
    }


}
