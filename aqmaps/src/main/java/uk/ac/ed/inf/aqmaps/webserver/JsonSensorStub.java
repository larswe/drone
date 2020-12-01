package uk.ac.ed.inf.aqmaps.webserver;

/**
 * This class is used to extract the defining properties of a sensor from an
 * air-quality.json file. Its instances are an incomplete implementation of a
 * sensor, later to be converted into a proper Sensor object.
 */
public class JsonSensorStub {
    private String location;
    private String battery;
    private String reading;

    public String getLocation() {
        return location;
    }

    public String getBattery() {
        return battery;
    }

    public String getReading() {
        return reading;
    }
}
