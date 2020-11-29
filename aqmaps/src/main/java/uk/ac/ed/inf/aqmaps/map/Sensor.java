package uk.ac.ed.inf.aqmaps.map;

import com.mapbox.geojson.Point;

public class Sensor {

    private final float battery;
    private final double reading;
    private final Point position;
    private final What3WordsLocation w3wLocation;
    
    private static final float TRUST_THRESHOLD = 0.1f;

    /*
     * This field denotes whether the sensor has been read since the start of the
     * day. When a Sensor object is initialized, we want its value to be false. It
     * is set to true once it is read by our drone.
     */
    private boolean read = false;

    public Sensor(float battery, double reading, What3WordsLocation w3wLocation) {
        this.battery = battery;
        this.reading = reading;
        this.w3wLocation = w3wLocation;

        double longitude = w3wLocation.getPosition().longitude();
        double latitude = w3wLocation.getPosition().latitude();
        this.position = Point.fromLngLat(longitude, latitude);
    }

    public double outputReading() {

        double officialReading;

        /*
         * If the battery is too low, the reading should already be NaN. But for the
         * case in which the data on the server was faulty in this regard, we make sure
         * whether the battery is low. 
         */
        if (this.battery < TRUST_THRESHOLD) {
            officialReading = Double.NaN;
        } else {
            officialReading = this.reading;
        }

        return officialReading;
    }

    public Point getPosition() {
        return position;
    }

    public double getReading() {
        return reading;
    }

    public What3WordsLocation getW3wLocation() {
        return w3wLocation;
    }

}
