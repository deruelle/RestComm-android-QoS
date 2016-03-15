package com.cortxt.app.MMC.Sampling.Transit;

import java.io.Serializable;

import com.google.android.maps.GeoPoint;

public class SerializableGeoPoint implements Serializable {
    private static final long serialVersionUID = -99127398709809L;

    private int latitudeE6;
    private int longitudeE6;

    public SerializableGeoPoint() {
        this.setLatitudeE6(0);
        this.setLongitudeE6(0);
    }

    public SerializableGeoPoint(int latitudeE6, int longitudeE6) {
        this.setLatitudeE6(latitudeE6);
        this.setLongitudeE6(longitudeE6);
    }

    public int getLatitudeE6() {
        return latitudeE6;
    }

    public void setLatitudeE6(int latitudeE6) {
        this.latitudeE6 = latitudeE6;
    }

    public int getLongitudeE6() {
        return longitudeE6;
    }

    public void setLongitudeE6(int longitudeE6) {
        this.longitudeE6 = longitudeE6;
    }
    
    public GeoPoint toGeoPoint() {
        return new GeoPoint (latitudeE6, longitudeE6);
    }
}
