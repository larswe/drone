package uk.ac.ed.inf.aqmaps;

public class Sensor {
    
    private final double battery;
    private final double reading;
    private final CoordinatePair coordinates;
    private final What3WordsLocation w3wLocation;
    
    public Sensor(double battery, double reading, What3WordsLocation w3wLocation) {
        this.battery = battery;
        this.reading = reading;
        this.w3wLocation = w3wLocation;
        
        double longitude = w3wLocation.getCoordinates().getLongitude();
        double latitude = w3wLocation.getCoordinates().getLatitude();
        this.coordinates = new FixedCoordinatePair(longitude, latitude);
    }
    
    public SensorInfo outputReading() {
        
        double reading;
        
        if (this.battery < 0.1) {
            reading = Double.NaN;
        } else {
            reading = 128.0;
         // TODO: Produce actual reading value
        }
        
        return new SensorInfo(reading, this.battery);
    }
    
    public CoordinatePair getCoordinates() {
        return coordinates;
    }

    public double getReading() {
        return reading;
    }

}
