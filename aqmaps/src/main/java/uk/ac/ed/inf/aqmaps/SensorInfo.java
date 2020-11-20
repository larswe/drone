package uk.ac.ed.inf.aqmaps;

public class SensorInfo {

    // Made final to avoid tampering with readings
    private final double reading, battery;
    
    public SensorInfo(double reading, double battery) {
        this.reading = reading;
        this.battery = battery;
    }

    public double getReading() {
        return reading;
    }

    public double getBattery() {
        return battery;
    }
    
    
}
