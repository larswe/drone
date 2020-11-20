package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.List;

public class Drone {
    
    /* Made final to ensure the coordinates is updated in the intended way*/
    private final VariableCoordinatePair coordinates;
    private int stepsMade;
    
    private final double MOVE_DISTANCE = 0.0003;
    private final double MAX_READ_DISTANCE = 0.0002;
    /* Radius of circle in which drone can land, returning to the starting point */ 
    private final double MAX_LANDING_DISTANCE = 0.0003;
    private static final int MAX_MOVES = 150;
    
    public Drone(double longitude, double latitude) {
        this.coordinates = new VariableCoordinatePair(longitude, latitude);
        this.stepsMade = 0;
    }

    public void makeMove(CoordinatePair nextPos) {
        if (!this.canMove(nextPos)) {
            // TODO: Error Handling
            return;
        } else {
            this.coordinates.setPosition(nextPos);
            this.stepsMade++;
        }
    }
    
    public boolean canMove(CoordinatePair nextPos) {
        // TODO: Check if drone in confinement area
        // TODO: Check if building is hit
        return true;
    }
    
    public List<CoordinatePair> generatePossibleNextPositions() {
        var positions = new ArrayList<CoordinatePair>();
        
        for (int angle = 0; angle <= 350; angle+=10) {
            
            var nextPos = EuclideanUtils.getNextPosition(this.coordinates, angle, this.MOVE_DISTANCE);
            
            if (this.canMove(nextPos)) {
                positions.add(this.coordinates);
            }
        }
        
        return positions;
        
    }
    
    private boolean isInRangeOfSensor(Sensor sensor) {
        
        // TODO: Implement... maybe also check for all sensors, if it doesn't take too long
        
        return true;
    }
    
    public SensorInfo downloadReadingsFromSensor(Sensor sensor) {
        
        if (isInRangeOfSensor(sensor)) {
            return sensor.outputReading();
        } else {
            // TODO: Error Handling
            return null;
        }
        
    }


}
