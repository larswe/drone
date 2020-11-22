package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class Drone {

    /* Made final to ensure the position is updated in the intended way */
    private Point position;
    private int stepsMade;

    private static final double MOVE_DISTANCE = 0.0003;
    private static final double MAX_READ_DISTANCE = 0.0002;
    /* Radius of circle in which drone can land, returning to the starting point */
    private static final double MAX_LANDING_DISTANCE = 0.0003;
    private static final int MAX_MOVES = 150;

    private static ArrayList<Sensor> sensorTour;

    public Drone(double longitude, double latitude, ArrayList<Sensor> tour) {
        this.position = Point.fromLngLat(longitude, latitude);
        this.stepsMade = 0;
        this.sensorTour = tour;
    }

    public void completeTour() {
        // Main method of sorts.. .TODO
    }

    public boolean canGetToDestinationInStraightLine(Point destination) {
        
        // TODO
        
        return false;
    }
    
    private void makeMove(Point nextPos) {
        if (!this.canMove(nextPos)) {
            // TODO: Error Handling
            return;
        } else {
            this.position = nextPos;
            this.stepsMade++;
        }
    }

    /*
     * Assuming that the drone is neither started outside the confinement area, nor
     * inside a No-Fly-Zone, the drone is able to move to a point at the appropriate
     * distance if and only if none of the borders of any of these polygons are
     * crossed.
     */
    private boolean canMove(Point nextPos) {

        var moveLineSegment = new LineSegment(this.position, nextPos);

        var forbiddenPolygons = new ArrayList<Polygon>();

        forbiddenPolygons.add(App.getConfinementArea());

        for (var noFlyZone : App.getNoFlyZones()) {
            forbiddenPolygons.add(noFlyZone.getPolygon());
        }

        for (var forbiddenPolygon : forbiddenPolygons) {
            if (EuclideanUtils.lineSegmentAndPolygonIntersect(moveLineSegment, forbiddenPolygon)) {
                return false;
            }
        }

        /*
         * If we have made it here without returning false, none of the forbidden
         * boundaries are crossed. q.e.d, return true. 
         */
        return true;
    }

    private boolean isInRangeOfSensor(Sensor sensor) {

        // TODO: Implement... maybe also check for all sensors, if it doesn't take too
        // long

        return true;
    }

    public double downloadReadingsFromSensor(Sensor sensor) {

        if (isInRangeOfSensor(sensor)) {
            return sensor.outputReading();
        } else {
            // TODO: Error Handling
            return 0.0;
        }

    }

    public int getStepsMade() {
        return this.stepsMade;
    }

    public static ArrayList<Sensor> getSensorTour() {
        return sensorTour;
    }

    public static void setSensorTour(ArrayList<Sensor> sensorTour) {
        Drone.sensorTour = sensorTour;
    }

}
