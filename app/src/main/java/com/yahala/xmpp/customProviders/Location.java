package com.yahala.xmpp.customProviders;

/**
 * Created by user on 6/27/2014.
 */
public class Location {
    public double lon;
    public double lat;

    public Location(double lon, double lat) {
        this.lat = lat;
        this.lon = lon;
    }

    public Location() {
    }
}
