package org.bigrs.croqui;

import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase;

import java.net.URLEncoder;

/**
 * Created by tiago on 29/03/17.
 */
public abstract class WMSTileProvider extends OnlineTileSourceBase {

    // cql filters
    private String cqlString = "";

    // Construct with tile size in pixels, normally 256, see parent class.
    public WMSTileProvider(String[] baseurl, int tileSizeInPixels) {
        super("WMS tile source", 0 ,20,tileSizeInPixels,"png",baseurl);

    }

    protected String getCql() {
        return URLEncoder.encode(cqlString);
    }

    public void setCql(String c) {
        cqlString = c;
    }

}