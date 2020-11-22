package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import uk.ac.ed.inf.aqmaps.Drone.DroneAction;

public class MainDrone extends Drone {

    private static final int MAX_MOVES = 150;

    private static ArrayList<Sensor> sensorTour;
    
    private final Point startingPosition;

    private int currentDestinationIndex = 0;

    /*
     * The "Shadow" of our main drone. See the relevant class documentation for details.
     */
    private ShadowDrone shadow;

    public MainDrone(double longitude, double latitude, ArrayList<Sensor> tour) {
        super(longitude, latitude);
        
        this.startingPosition = this.currentPosition;
        this.sensorTour = tour;
        
        this.shadow = new ShadowDrone(this.currentPosition.longitude(), this.currentPosition.latitude());
    }

    public void completeTour() {
        // Main method of sorts.. .TODO
    }

    @Override
    public boolean canGetToDestinationInStraightLine(Point destination, double maxFinalDistance) {
        
        shadow.setPosition(this.currentPosition);
        shadow.setStepsMade(0);
        return shadow.canGetToDestinationInStraightLine(destination, maxFinalDistance);
        
    }

    private double downloadReadingsFromSensor(Sensor sensor) {

        if (isInRangeOfPoint(sensor.getPosition(), MAX_READ_DISTANCE)) {
            return sensor.outputReading();
        } else {
            // TODO: Error Handling
            return 0.0;
        }

    }

    public static ArrayList<Sensor> getSensorTour() {
        return sensorTour;
    }

    public static void setSensorTour(ArrayList<Sensor> sensorTour) {
        MainDrone.sensorTour = sensorTour;
    }

}
