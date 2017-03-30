package br.com.homembala.dedos;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import org.osmdroid.util.TileSystem;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;

/**
 * Created by tiago on 29/03/17.
 */
class LabelsOverlay extends org.osmdroid.views.overlay.Overlay
{

    public LabelsOverlay(Context ctx)
    {
        super(ctx);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void draw(Canvas pC, MapView pOsmv, boolean shadow)
    {
        if (shadow)
            return;

        Paint lp3;
        lp3 = new Paint();
        lp3.setColor(Color.RED);
        lp3.setAntiAlias(true);
        lp3.setStyle(Paint.Style.STROKE);
        lp3.setStrokeWidth(1);
        lp3.setTextAlign(Paint.Align.LEFT);
        lp3.setTextSize(12);
        // Calculate the half-world size
        final Rect viewportRect = new Rect();
        final Projection projection = pOsmv.getProjection();
        final int zoomLevel = projection.getZoomLevel();
        int mWorldSize_2 = TileSystem.MapSize(zoomLevel) / 2;

        // Save the Mercator coordinates of what is on the screen
        viewportRect.set(projection.getScreenRect());
        // DON'T set offset with either of below
        // viewportRect.offset(-mWorldSize_2, -mWorldSize_2);
        // viewportRect.offset(mWorldSize_2, mWorldSize_2);

        // Draw a line from one corner to the other
        pC.drawLine(viewportRect.left, viewportRect.top,
                viewportRect.right, viewportRect.bottom, lp3);
    }

    public void onProviderDisabled(String arg0)
    {
    }

    public void onProviderEnabled(String provider)
    {
    }
}