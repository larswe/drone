package uk.ac.ed.inf.aqmaps.webserver;

/*
 * This class is an incomplete implementation of a Sensor - it is used as a simple means of parsing
 * the relevant attributes from an air-quality.json file, to be used as a pattern for the creating
 * of the corresponding Sensor object
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
