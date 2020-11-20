package uk.ac.ed.inf.aqmaps;

public class DroneMove {

    private final int angle;
    private final Sensor sensorToBeRead;
    
    public DroneMove(int angle, Sensor sensor) {
        this.angle = angle;
        this.sensorToBeRead = sensor;
    }
    
}
