package br.com.cetsp.iat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.ExpirableBitmapDrawable;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.tilesource.BitmapTileSourceBase;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.util.TileSystem;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jsqlite.Constants;
import jsqlite.Database;
import jsqlite.Exception;

class MapTileProviderSpatial extends MapTileProviderBasic {
    private final MapView map;
    private final Context context;
    private final Database jdb;
    private final ArrayList<Long> making;
    private final Hashtable<Integer, ArrayList<Long>> maker;
    private final float scale;
    private final Handler mapHandler;
    private boolean externalStorage;

    public MapTileProviderSpatial(Context applicationContext, MapView p, float s, Handler h) throws Exception, IOException {
        super(applicationContext);
        mapHandler=h;
        scale=s;
        making= new ArrayList<>();
        maker=new Hashtable<>();
        for(int i=0;i<23;i++){
            maker.put(i,new ArrayList<>());
        }
        context=applicationContext;
        map = p;
        jdb = ((Iat)(context.getApplicationContext())).getDatabase(context.getFilesDir().getPath() + "mapas");
        jdb.open(context.getFilesDir().getPath() + "mapas", Constants.SQLITE_OPEN_READONLY);

    }

    @Override
    public Drawable getMapTile(long pMapTileIndex) {
        Log.d("IAT GET TILE", "" + pMapTileIndex);
        Drawable t = getTileCache().getMapTile(pMapTileIndex);
        int z = MapTileIndex.getZoom(pMapTileIndex);
        if(t!=null) return t;
        ITileSource s = getTileSource();
        try {
//            t = s.getDrawable(context.getFilesDir().getPath() + "/" + z + "/" + MapTileIndex.getX(pMapTileIndex) + "_" + MapTileIndex.getY(pMapTileIndex));

            t = s.getDrawable(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getPath() + "/" + z + "/" + MapTileIndex.getX(pMapTileIndex) + "_" + MapTileIndex.getY(pMapTileIndex));
        } catch (BitmapTileSourceBase.LowMemoryException e) {
            e.printStackTrace();
            return null;
        }
        if (t != null){
            if (z != map.getZoomLevelDouble()) {
                Log.d("IAT", "wrong zoom layer for this");
            } else {
                return t;
            }
        }
        if(maker.get(z).contains(pMapTileIndex))
            return null;
        maker.get(z).add(pMapTileIndex);
        int px = Math.round((s.getTileSizePixels() >> 1) * scale);
        //map.getProjection();
        final TileSystem tileSystem = org.osmdroid.views.MapView.getTileSystem();

        int x = MapTileIndex.getX(pMapTileIndex);
        int y = MapTileIndex.getY(pMapTileIndex);
        Projection projection = new Projection(
                (double) z,
                px, px,
                new GeoPoint(tileSystem.getLatitudeFromTileY(y, z), tileSystem.getLongitudeFromTileX(x, z)),
                (float) 0,
                true,
                true,
                0,0
        );

        BoundingBox bb = new BoundingBox(
                tileSystem.getLatitudeFromTileY(y, z),
                tileSystem.getLongitudeFromTileX(x + 1, z),
                tileSystem.getLatitudeFromTileY(y + 1, z),
                tileSystem.getLongitudeFromTileX(x, z)
        );
        Rect rect = projection.getPixelFromTile(x, y, null);
        Bitmap b=Bitmap.createBitmap(px,px, Bitmap.Config.ARGB_8888);
        b.eraseColor(ContextCompat.getColor(context,R.color.white));
        (new SpatialLoader(bb,tileSystem,rect, b, pMapTileIndex, projection)).execute();
        return null;

    }

    @Override
    public int getMinimumZoomLevel() {
        return 0;
    }

    @Override
    public int getMaximumZoomLevel() {
        return 0;
    }

    class SpatialLoader extends AsyncTask<Void, Void, Boolean> {
        private Bitmap bitmap;
        private long index;
        private Projection projection;
        private BoundingBox bb;
        private TileSystem tileSystem;
        private Rect rect;

        public SpatialLoader(BoundingBox boundingbox, TileSystem ts, Rect r, Bitmap c, long pMapTileIndex, Projection p) {
            this.rect=r;
            this.bitmap=c;
            this.index=pMapTileIndex;
            this.projection=p;
            this.bb=boundingbox;
            this.tileSystem=ts;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            double threshold=getThreshold(projection);
            String query="select asText(simplify(geometry,%q)) as quadra from quadras where intersects(buildmbr(%q, %q, %q, %q, 4326),geometry)=1";
            Log.d("IAT DATABASE QUERY", query);
            SpatialCallback cb = new SpatialCallback(rect,tileSystem,bitmap,index,projection,bb);
            try {
                jdb.exec(query,cb,new String[]{
                        Double.toString(threshold),
                        Double.toString(bb.getLonWest()),
                        Double.toString(bb.getLatSouth()),
                        Double.toString(bb.getLonEast()),
                        Double.toString(bb.getLatNorth())
                });
                jdb.exec("select ''", cb);
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("IAT", "Error execute query");
                Log.d("IAT", e.getMessage());
            }
            return true;
        }
    }

    private double getThreshold(Projection projection) {
        IGeoPoint px0 = projection.fromPixels(0, 0);
        IGeoPoint px1 = projection.fromPixels(1, 1);
        return Math.sqrt(Math.pow(px0.getLongitude()-px1.getLongitude(),2)+Math.pow(px0.getLatitude()-px1.getLatitude(),2));
    }

    private class SpatialCallback implements jsqlite.Callback {
        private Bitmap bitmap;
        private Canvas canvas;
        private long index;
        private Projection projection;
        private TileSystem tileSystem;
        private int zoom;
        private Point corner;
        //private BoundingBox boundingbox;
        private Rect rect;


        public SpatialCallback(Rect b, TileSystem ts, Bitmap c, long i, Projection p, BoundingBox bb) {
            this.rect=b;
            this.bitmap=c;
            this.canvas=new Canvas(c);
            this.index=i;
            this.projection=p;
            this.tileSystem=ts;
            //this.boundingbox=bb;
            this.zoom=MapTileIndex.getZoom(i);
            Log.d("IAT database callback ",""+projection.getZoomLevel());
            Log.d("IAT", "zoom real:"+zoom);
            corner=new Point();
            GeoPoint gcorner=new GeoPoint(bb.getLatNorth(),bb.getLonWest());
            projection.toPixels(gcorner,corner);
            //GeoPoint ponta=new GeoPoint(bb.getLatSouth(),bb.getLonEast());
            //Point pu=new Point();
            //projection.toPixels(ponta,pu);
        }

        @Override
        public void columns(String[] coldata) {
            Log.d("IAT RETURN COLUMNS", coldata[0].toString());
        }

        @Override
        public void types(String[] types) {
            Log.d("IAT RETURN TYPES", types.toString());
        }

        @Override
        public boolean newrow(String[] rowdata) {
            if(rowdata[0].length()==0){
                final BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
                if(map.getZoomLevelDouble()!=zoom){
                    Log.d("IAT", "writing cache at wrong zoom");
                }else {
                    putTileIntoCache(index, drawable, ExpirableBitmapDrawable.UP_TO_DATE);
                }
                maker.get(zoom).remove(index);
                saveBitmapToFile("/"+zoom+"/", ""+MapTileIndex.getX(index)+"_"+MapTileIndex.getY(index),bitmap);
                Message m = new Message();
                m.what=1;
                mapHandler.dispatchMessage(m);
                return false;
            }

            Pattern p= Pattern.compile("[\\d\\s\\.\\-\\,]+");
            Matcher m=p.matcher(rowdata[0]);
            Paint wallpaint = new Paint();
            wallpaint.setColor(ContextCompat.getColor(context,R.color.medium_gray));
            wallpaint.setStyle(Paint.Style.FILL);
            Point point = new Point();

            while(m.find()) {
                Path block = new Path();
                String[] pts = m.group().split(",\\s?");
                if (pts.length > 1) {
                    String[] cords = pts[0].split(" ");
                    this.projection.toPixels(new GeoPoint(Float.parseFloat(cords[1]), Float.parseFloat(cords[0])),point);
                    block.moveTo(point.x-corner.x,point.y-corner.y);
                    for (int j = 0; j < pts.length; j++) {
                        cords = pts[j].split(" ");
                        this.projection.toPixels(new GeoPoint(Float.parseFloat(cords[1]), Float.parseFloat(cords[0])),point);
                        block.lineTo(point.x-corner.x,point.y-corner.y);

                    }
                    canvas.drawPath(block, wallpaint);
                }
            }
            return false;
        }
    }

    private void saveBitmapToFile(String path, String fileName, Bitmap bitmap) {

//        File dir = new File(context.getFilesDir().getPath()+path);
        File dir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getPath()+path);
        StatFs stat = new StatFs(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getPath());
        if(stat.getAvailableBytes()>0){
            externalStorage =true;
            dir.mkdirs();
        }else{
            dir=new File(context.getFilesDir()+path);
            dir.mkdirs();
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(dir+"/"+fileName);
            bitmap.compress(Bitmap.CompressFormat.PNG,66,fos);
            fos.close();
        }
        catch (IOException e) {
            Log.e("app",e.getMessage());
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
