package br.com.cetsp.iat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.Projection;

import java.util.ArrayList;

import jsqlite.Database;
import jsqlite.Exception;

class LabelLoader  extends AsyncTask<Void, Void, Boolean> {
    private final BoundingBox bb;
    private final Projection projection;
    private final Handler handler;
    private final Bitmap bitmap;
    private final Context context;

    public LabelLoader(Context c, Bitmap b, BoundingBox boundingbox, Projection p, Handler h) {
        bitmap=b;
        bb=boundingbox;
        projection=p;
        handler=h;
        context = c;
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        SpatialTextCallback ctb=new SpatialTextCallback(bb,bitmap,handler, projection);
        String query = "select nome, distance(a,b) as l, x(centro) as x, y(centro)as y, case\n" +
                "    when X(A) == X(B) then case\n" +
                "        when Y(A) > Y(B) then -90.0\n" +
                "        else 90.0\n" +
                "        end\n" +
                "    when Y(A) == Y(B) then case \n" +
                "        when X(A) > X(B) then 180\n" +
                "        else 0.0\n" +
                "        end\n" +
                "    else 360*(atan( (Y(A) - Y(B)) / (X(A) - X(B)) ) )/(2*3.141592653589793)\n" +
                "    end from(\n" +
                "select nome, transform(startpoint(g),3857) as a,transform(endpoint(g),3857) as b, Line_Interpolate_Point(g,0.5) as centro from (\n" +
                "select nome,intersection(mbr,Geometry) as g from logradouros, (select buildmbr(\n" +
                "%q, %q, %q, %q" +
                ", 4326) as mbr) bb\n" +
                " where intersects(mbr,geometry)=1\n" +
                ") c\n" +
                ") d\n" +
                "order by l desc limit 10\n" +
                "";
        Database jdb = ((Iat) context.getApplicationContext()).getDatabase(context.getFilesDir().getPath() + "mapas");
        try {
            jdb.exec(query,ctb,new String[]{
                    Double.toString(bb.getLonWest()),
                    Double.toString(bb.getLatSouth()),
                    Double.toString(bb.getLonEast()),
                    Double.toString(bb.getLatNorth())
            });
            jdb.exec("select ''", ctb);
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("IAT", "Error execute query");
            Log.d("IAT", e.getMessage());
        }

        return null;
    }
    private class SpatialTextCallback implements jsqlite.Callback {
        private final Handler handler;
        private ArrayList<String> done;
        private Bitmap bitmap;
        private Canvas canvas;
        private Projection projection;
        private BoundingBox boundingbox;


        public SpatialTextCallback(BoundingBox bb, Bitmap bitlabel, Handler h, Projection p) {
            bitmap=bitlabel;
            boundingbox=bb;
            handler=h;
            projection=p;
            canvas=new Canvas(bitmap);
            done=new ArrayList<>();
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
                Message m = new Message();
                m.what=1;
                handler.dispatchMessage(m);
                return false;
            }
            if(done.contains(rowdata[0]))
                return false;
            done.add(rowdata[0]);
            if((rowdata[3]!=null)&&(rowdata[2]!=null)&&(rowdata[4]!=null)) {
                Paint wallpaint = new Paint();
                wallpaint.setColor(ContextCompat.getColor(context, R.color.dark_gray));
                wallpaint.setStyle(Paint.Style.FILL_AND_STROKE);
                wallpaint.setTextSize(20.0f);
                wallpaint.setAntiAlias(true);
                wallpaint.setTextAlign(Paint.Align.CENTER);
                float azimuth = Float.parseFloat(rowdata[4]);
                Point point = new Point();
                this.projection.toPixels(new GeoPoint(Float.parseFloat(rowdata[3]), Float.parseFloat(rowdata[2])), point);
                canvas.save();
                canvas.rotate(-1*azimuth, point.x, point.y);
                canvas.drawText(rowdata[0], (float) (point.x), (float) (point.y+10), wallpaint);
                canvas.restore();
            }
            return false;
        }
    }

}
