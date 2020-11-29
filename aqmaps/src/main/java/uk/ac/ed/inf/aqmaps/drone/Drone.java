package uk.ac.ed.inf.aqmaps.drone;

import java.util.ArrayList;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import uk.ac.ed.inf.aqmaps.application.App;
import uk.ac.ed.inf.aqmaps.geometry.EuclideanUtils;
import uk.ac.ed.inf.aqmaps.geometry.LineSegment;
import uk.ac.ed.inf.aqmaps.map.NoFlyZone;

public abstract class Drone {

    protected static enum DroneAction {
        READ, LAND
    };

    protected Point currentPosition;
    protected int stepsMade;

    protected ArrayList<Double> moveAngleHistory;
    
    protected Polygon obstacleInOurWay;
    protected Point currentDestination;

    protected static final int MAX_MOVES = 350; //TODO

    /* Our drone can move at an angle of 10, 20,..., but not e.g. 26 degrees */
    protected static final double ANGLE_GRANULARITY = 10.0;
    protected static final double MOVE_DISTANCE = 0.0003;
    protected static final double MAX_READ_DISTANCE = 0.0002;
    /* Radius of circle in which drone can land, returning to the starting point */
    protected static final double MAX_LANDING_DISTANCE = 0.0003;

    public Drone(Point startingPoint) {
        this.currentPosition = startingPoint;
        this.stepsMade = 0;
        this.moveAngleHistory = new ArrayList<Double>();
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
    public abstract boolean canGetTowardsDestinationInStraightLine();

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

    /*
     * This is a sort of last resort - if a sensor is very close to a building or an
     * edge of the confinement zone, accessing it may be very tricky, because the
     * move distance is greater than the radius in which we can read the sensor. In
     * this (quite literally) edge case, we will need an "in between move" to enter
     * the radius, because flying straight at the destination will make the drone
     * crash.
     * 
     * This is also relevant if we need to make a waiting move because we cannot
     * take two sensor readings on the same step.
     * 
     * The idea is simple: Just try all angles! As soon as we find one that lets us
     * enter the required range in two (or preferably only one) steps, that one will
     * do.
     */
    public boolean park(double maxFinalDistance) {

        var numAngles = 360 / ANGLE_GRANULARITY;

        Double chosenInBetweenMoveAngle = null;
        Double chosenParkingMoveAngle = null;

        for (int i = 0; i < numAngles; i++) {
            var angleForCandidateInBetweenMove = i * ANGLE_GRANULARITY;

            /*
             * If the in between move we would like to consider is not possible, we don't
             * need to consider it as an option. Try the next angle without further ado!
             */
            if (canMove(angleForCandidateInBetweenMove)) {
                var posAfterCandidateInBetweenMove = EuclideanUtils.getNextPosition(this.currentPosition,
                        angleForCandidateInBetweenMove, MOVE_DISTANCE);

                var shadowForSecondStep = new ShadowDrone(posAfterCandidateInBetweenMove, this.currentDestination);
                
                /*
                 * Maybe this random step was actually the one we needed - that would be even
                 * better! Then we would only need one step that does not aim directly at the
                 * destination but takes us into the relevant range.
                 * 
                 * Otherwise, we check if a two-move parking maneuver is possible - but only if
                 * none has been found yet.
                 */
                if (shadowForSecondStep.isInRangeOfPoint(this.currentDestination, maxFinalDistance)) {
                    this.makeMove(angleForCandidateInBetweenMove);
                    System.out.println("Successful parking attempt in 1 move");
                    return true;

                } else if (chosenInBetweenMoveAngle == null) {
                    var posAfterParkingAttempt = shadowForSecondStep.getNextPosTowardsGoal();

                    if (EuclideanUtils.computeDistance(posAfterParkingAttempt,
                            this.currentDestination) <= maxFinalDistance) {
                        var parkingAttempt = new LineSegment(shadowForSecondStep.currentPosition, posAfterParkingAttempt);
                        var parkingAngle = parkingAttempt.getAngleInDegrees();

                        if (shadowForSecondStep.canMove(parkingAngle)) {
                            chosenInBetweenMoveAngle = angleForCandidateInBetweenMove;
                            chosenParkingMoveAngle = parkingAngle;
                        }
                    }
                }
            }
        }

        /*
         * If we get here, no single move was enough to bring us into the required range
         * - but hopefully we have found a 2-step maneuver, which we now go for.
         */
        if (chosenInBetweenMoveAngle == null) {
            System.out.println("The parking attempt was not successful.");
            return false;
        } else {
            makeMove(chosenInBetweenMoveAngle);
            makeMove(chosenParkingMoveAngle);
            System.out.println("Successful parking attempt in 2 moves");
            return true;
        }

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
    
    /*
     * This method is needed because the tour planner calls the drone's flyToCurrentDestination method
     */
    public static double getMaxReadDistance() {
        return MAX_READ_DISTANCE;
    }
    
    public ArrayList<Double> getMoveAngleHistory() {
        return moveAngleHistory;
    }

}
