package br.com.cetsp.iat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.tileprovider.ExpirableBitmapDrawable;
import org.osmdroid.tileprovider.MapTileProviderBase;
import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.modules.IFilesystemCache;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.MapTileIndex;
import org.osmdroid.util.PointL;
import org.osmdroid.util.TileSystem;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
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

    public MapTileProviderSpatial(Context applicationContext, MapView p) throws Exception, IOException {
        super(applicationContext);
        making= new ArrayList<>();
        maker=new Hashtable<>();
        for(int i=0;i<23;i++){
            maker.put(i,new ArrayList<>());
        }
        context=applicationContext;
        map = p;
        jdb = ((Iat)(context.getApplicationContext())).getDatabase();

        File quadras=new File(context.getFilesDir().getPath() + "mapas");
        if(!quadras.exists()){
            InputStream in = context.getResources().openRawResource(R.raw.db);
            FileOutputStream out = new FileOutputStream(context.getFilesDir().getPath() + "mapas");
            byte[] buff = new byte[1024];
            int read = 0;
            try {
                while ((read = in.read(buff)) > 0) {
                    out.write(buff, 0, read);
                }
            } finally {
                in.close();
                out.close();
            }
        }
        jdb.open(context.getFilesDir().getPath() + "mapas", Constants.SQLITE_OPEN_READONLY);

    }

    @Override
    public Drawable getMapTile(long pMapTileIndex) {
        Log.d("IAT GET TILE", ""+ pMapTileIndex);
        Drawable t = getTileCache().getMapTile(pMapTileIndex);
        if(t!=null) return t;

        if(maker.get((int)map.getZoomLevelDouble()).contains(pMapTileIndex))
            return null;
        maker.get((int)map.getZoomLevelDouble()).add(pMapTileIndex);
        Projection projection = map.getProjection();
        final TileSystem tileSystem = org.osmdroid.views.MapView.getTileSystem();
        ITileSource s = getTileSource();
        int px = 380;//s.getTile();
        int x = MapTileIndex.getX(pMapTileIndex);
        int y = MapTileIndex.getY(pMapTileIndex);
        int z = MapTileIndex.getZoom(pMapTileIndex);
        BoundingBox bb = new BoundingBox(
            tileSystem.getLatitudeFromTileY(y, z),
            tileSystem.getLongitudeFromTileX(x + 1, z),
            tileSystem.getLatitudeFromTileY(y + 1, z),
            tileSystem.getLongitudeFromTileX(x, z)
        );
        Rect rect = projection.getPixelFromTile(x, y, null);
        IGeoPoint nw = projection.fromPixels(rect.left, rect.top);
        IGeoPoint se = projection.fromPixels(rect.right, rect.bottom);
        Bitmap b=Bitmap.createBitmap(px,px, Bitmap.Config.ARGB_8888);
        final BitmapDrawable drawable = new BitmapDrawable(context.getResources(), b);

        //b.eraseColor(context.getColor(R.color.red));
        Canvas canvas = new Canvas(b);
        (new SpatialLoader(bb,tileSystem,rect, nw, se, b, pMapTileIndex, projection)).execute();
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
        private final Bitmap bitmap;
        private final long index;
        private final Projection projection;
        private final BoundingBox bb;
        private final TileSystem tileSystem;
        private Rect rect;
        private IGeoPoint northwest;
        private IGeoPoint southeast;

        public SpatialLoader(BoundingBox boundingbox, TileSystem ts, Rect r, IGeoPoint nw, IGeoPoint se, Bitmap c, long pMapTileIndex, Projection p) {
            this.northwest=nw;
            this.southeast=se;
            this.rect=r;
            this.bitmap=c;
            this.index=pMapTileIndex;
            this.projection=p;
            this.bb=boundingbox;
            this.tileSystem=ts;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            String query="select asText(geometry) as quadra from quadras where intersects(buildmbr(%q, %q, %q, %q, 4326),geometry)=1";

            Log.d("IAT DATABASE QUERY", query);
            SpatialCallback cb = new SpatialCallback(rect,tileSystem,bitmap,index,projection,bb);
            try {
                jdb.exec(query,cb,new String[]{
                        Double.toString(bb.getLonWest()),
                        Double.toString(bb.getLatSouth()),
                        Double.toString(bb.getLonEast()),
                        Double.toString(bb.getLatNorth())
  //                      Double.toString(northwest.getLongitude()),Double.toString(southeast.getLatitude()),
//                        Double.toString(southeast.getLongitude()),Double.toString(northwest.getLatitude()),
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
    private class SpatialCallback implements jsqlite.Callback {
        private final Bitmap bitmap;
        private final Canvas canvas;
        private final long index;
        private final Projection projection;
        private final TileSystem tileSystem;
        private Point corner;
        private final BoundingBox boundingbox;
        private Rect rect;


        public SpatialCallback(Rect b, TileSystem ts, Bitmap c, long i, Projection p, BoundingBox bb) {
            this.rect=b;
            this.bitmap=c;
            this.canvas=new Canvas(c);
            this.index=i;
            this.projection=p;
            this.tileSystem=ts;
            this.boundingbox=bb;
            corner=new Point();
            GeoPoint gcorner=new GeoPoint(bb.getLatNorth(),bb.getLonWest());
            projection.toPixels(gcorner,corner);
            GeoPoint ponta=new GeoPoint(bb.getLatSouth(),bb.getLonEast());
            Point pu=new Point();
            projection.toPixels(ponta,pu);
            int x=pu.x-corner.x;
            //MapView map = (MapView) findViewById(R.id.map);
            //Projection projection = map.getProjection();
//            GeoPoint korner = new GeoPoint(bb.getLatNorth(), bb.getLonWest());
            //corner=new Point();
            //projection.toPixels(korner, corner);
            //GeoPoint sudeste = new GeoPoint(bb.getLatSouth(), bb.getLonEast());
            //Point southeast = new Point();
            //projection.toPixels(sudeste, southeast);
            //View draw = findViewById(R.id.drawing_panel);
            //bitmap = Bitmap.createBitmap(draw.getDrawingCache());
            //bitmap = Bitmap.createBitmap(southeast.x-corner.x,southeast.y-corner.y, Bitmap.Config.ARGB_8888);
            //canvas = new Canvas(bitmap);
            //bitmap.eraseColor(getResources().getColor(R.color.white));
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

//            MapView map = (MapView) findViewById(R.id.map);
            if(rowdata[0].length()==0){

                final BitmapDrawable drawable = new BitmapDrawable(context.getResources(), bitmap);
                putTileIntoCache(index,drawable, ExpirableBitmapDrawable.UP_TO_DATE);
                maker.get((int)map.getZoomLevelDouble()).remove(index);
                /* localmap_updates--;
                if((localmap_updates==0)&&(!isMoving)) { // only redraws if this is the last update call
                    local_map_overlay.setPosition((GeoPoint) map.getMapCenter());
                    local_map_overlay.adjustBounds(bb);
                    local_map_overlay.setImage(new BitmapDrawable(getResources(), bitmap));
                    map.invalidate();
                }
                return false; */
            }

            Pattern p= Pattern.compile("[\\d\\s\\.\\-\\,]+");
            Matcher m=p.matcher(rowdata[0]);
            Paint wallpaint = new Paint();
            wallpaint.setColor(ContextCompat.getColor(context,R.color.red));
            wallpaint.setStyle(Paint.Style.FILL_AND_STROKE);
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
}
