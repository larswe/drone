package uk.ac.ed.inf.aqmaps;

public abstract class CoordinatePair {

    protected double longitude;
    protected double latitude;
    
    public CoordinatePair(double longitude, double latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public String toString() {
        return "(Longitude: " + longitude + ", Latitude: " + latitude + ")";
    }
    
}
