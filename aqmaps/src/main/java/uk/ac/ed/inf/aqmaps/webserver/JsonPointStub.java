package uk.ac.ed.inf.aqmaps.webserver;

/**
 * This class is used to extract the defining properties of a point from the
 * GeoJSON file corresponding to a What3Words Location. It thus captures part of
 * the inner structure of those files.
 */
public class JsonPointStub {
    /* Longitude and latitude of the point */
    private double lng, lat;

    public double getLng() {
        return lng;
    }

    public double getLat() {
        return lat;
    }
}
