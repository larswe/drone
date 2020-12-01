package uk.ac.ed.inf.aqmaps.map;

import com.mapbox.geojson.Point;

/**
 * This class models a sensor that our drone can read.
 */
public class Sensor {

    /*
     * The properties of a sensor, obvious from the project specification. Note that
     * we do not have an attribute that tells us whether a sensor has been read -
     * this is instead captured by a field of our main drone. This could have been
     * done either way, both practically and conceptually.
     */
    private final float battery;
    private final double reading;
    private final Point position;
    private final What3WordsLocation w3wLocation;

    /*
     * The trust threshold determines how full the battery of a sensor needs to be
     * for us to trust its reading.
     */
    private static final float TRUST_THRESHOLD = 0.1f;

    /**
     * The constructor of the Sensor class. Assigns a unique position to the sensor,
     * depending on the given What3Words Location.
     * 
     * @param battery     the current battery of the sensor, between 0 and 1
     * @param reading     the air pollution reading of the sensor
     * @param w3wLocation the What3Words Location of the sensor
     */
    public Sensor(float battery, double reading, What3WordsLocation w3wLocation) {
        this.battery = battery;
        this.reading = reading;
        this.w3wLocation = w3wLocation;

        var longitude = w3wLocation.getPosition().longitude();
        var latitude = w3wLocation.getPosition().latitude();
        this.position = Point.fromLngLat(longitude, latitude);
    }

    /**
     * This method gives the reading of the sensor. Determined by the reading
     * attribute, unless the battery status is too low, in which case the reading is
     * not deemed trustworthy.
     * 
     * @return the official reading of the sensor, taking into account its battery
     */
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

    /* Getters, nothing unexpected here */

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
