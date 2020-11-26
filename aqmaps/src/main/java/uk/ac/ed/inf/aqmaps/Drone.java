package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public abstract class Drone {

    protected static enum DroneAction {
        READ, LAND
    };

    protected Point currentPosition;
    protected int stepsMade;

    protected Polygon obstacleInOurWay;
    protected Point currentDestination;

    protected static final int MAX_MOVES = 350; //TODO

    /* Our drone can move at an angle of 10, 20,..., but not e.g. 26 degrees */
    protected static final double ANGLE_GRANULARITY = 10.0;
    protected static final double MOVE_DISTANCE = 0.0003;
    protected static final double MAX_READ_DISTANCE = 0.0002;
    /* Radius of circle in which drone can land, returning to the starting point */
    protected static final double MAX_LANDING_DISTANCE = 0.0003;
    /*
     * When the drone checks whether it can move in a straight line or needs to make
     * adjustments, it checks this many steps in advance
     */
    protected static final int NUM_MOVES_TO_BE_CHECKED_TOWARDS_GOAL = 1; // TODO - figure out if only checking the next step actually leads to consistent results. Otherwise fix system

    public Drone(double longitude, double latitude) {
        this.currentPosition = Point.fromLngLat(longitude, latitude);
        this.stepsMade = 0;
    }

    /*
     * This method determines whether the the drone can get to its current
     * destination without leaving the confinement area or entering any
     * no-fly-zones. The method is made abstract because our main drone and
     * "shadow drones" have different ways of finding out if this is the case.
     * 
     * Requires radius that defines the max range around the destination which the
     * drone can end up in.
     */
    public abstract boolean canGetTowardsDestinationInStraightLine(double maxFinalDistance);

    protected abstract void makeMove(double angle);

    /*
     * Assuming that the drone is neither started outside the confinement area, nor
     * inside a No-Fly-Zone, the drone is able to move to a point at the appropriate
     * distance if and only if none of the borders of any of these polygons are
     * crossed.
     */
    protected boolean canMove(double angle) {

        var nextPos = EuclideanUtils.getNextPosition(this.currentPosition, angle, MOVE_DISTANCE);

        var moveLineSegment = new LineSegment(this.currentPosition, nextPos);

        var forbiddenPolygons = new ArrayList<Polygon>();

        forbiddenPolygons.add(App.getConfinementArea());

        for (var noFlyZone : App.getNoFlyZones()) {
            forbiddenPolygons.add(noFlyZone.getPolygon());
        }

        for (var forbiddenPolygon : forbiddenPolygons) {
            if (EuclideanUtils.lineSegmentAndPolygonIntersect(moveLineSegment, forbiddenPolygon)) {
                this.obstacleInOurWay = forbiddenPolygon;
                return false;
            }
        }

        /*
         * If we have made it here without returning false, none of the forbidden
         * boundaries are crossed. q.e.d, return true.
         */
        return true;
    }

    protected boolean isInRangeOfPoint(Point point, double radius) {
        return EuclideanUtils.computeDistance(this.currentPosition, point) <= radius;
    }

    public Point getCurrentPosition() {
        return currentPosition;
    }

    public int getStepsMade() {
        return this.stepsMade;
    }

    public Polygon getObstacleInOurWay() {
        return obstacleInOurWay;
    }

}
