package br.com.cetsp.iat;

import android.graphics.Canvas;

import org.osmdroid.bonuspack.overlays.GroundOverlay;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;

/**
 * Created by tiago on 18/04/17.
 */

public class CsiGroundOverlay extends GroundOverlay {
    private BoundingBox bounds;
    public CsiGroundOverlay setBounds(BoundingBox b){
        bounds=b;
        return this;
    }
    public void adjustBounds(BoundingBox b){
        bounds=b;
    }
    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if(!shadow) {
            if(this.mImage != null) {
                if(this.mHeight == -1.0F) {
                    this.mHeight = this.mWidth * (float)this.mImage.getIntrinsicHeight() / (float)this.mImage.getIntrinsicWidth();
                }
                Projection pj = mapView.getProjection();
                pj.toPixels(this.mPosition, this.mPositionPixels);
                GeoPoint pSouthEast=new GeoPoint(this.bounds.getLatSouth(),this.bounds.getLonEast());
                pj.toPixels(pSouthEast, this.mSouthEastPixels);
                int hWidth = this.mSouthEastPixels.x - this.mPositionPixels.x;
                int hHeight = this.mSouthEastPixels.y - this.mPositionPixels.y;
                this.mImage.setBounds(-hWidth, -hHeight, hWidth, hHeight);
                this.mImage.setAlpha(255 - (int)(this.mTransparency * 255.0F));
                drawAt(canvas, this.mImage, this.mPositionPixels.x, this.mPositionPixels.y, false, -this.mBearing);
            }
        }
    }
}
