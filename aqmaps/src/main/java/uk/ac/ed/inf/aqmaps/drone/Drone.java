package uk.ac.ed.inf.aqmaps.drone;

import java.util.ArrayList;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import uk.ac.ed.inf.aqmaps.application.App;
import uk.ac.ed.inf.aqmaps.geometry.EuclideanUtils;
import uk.ac.ed.inf.aqmaps.geometry.LineSegment;
import uk.ac.ed.inf.aqmaps.map.TwoDimensionalMapObject;

/**
 * Our program features two types of drone - this abstract class defines their
 * shared properties, which we would expect each potential other type of drone
 * to incorporate as well.
 *
 */
public abstract class Drone {

    /*
     * We capture the position of objects in our map as points - this eventually
     * makes converting them to GeoJSON features easier.
     */
    protected Point currentPosition;

    /*
     * The "range" of the action the drone wants to complete next. MAX_READ_DISTANCE
     * if the drone wants to read a sensor, MAX_LANDING_DISTANCE if it wants to
     * land.
     */
    protected double currentActionRange;

    /*
     * The number of steps the drone has made since the last reset (initialisation
     * in the MainDrone case)
     */
    protected int stepsMade;

    /*
     * A history of all angles the drone ever chose to move. This is useful for the
     * output of the main drone and for copying the moves of a "shadow drone".
     */
    protected ArrayList<Integer> moveAngleHistory;

    /*
     * This field is updated when a drone finds that it can not advance straight
     * towards its destination because an obstacle is in its way via the
     * canGetTowardsDestinationInStraightLine method.
     */
    protected TwoDimensionalMapObject obstacleInOurWay;

    /*
     * The current Destination of the drone. May correspond to a sensor or the
     * starting point.
     */
    protected Point currentDestination;

    /* After this many moves, the drone runs out of battery and shuts down. */
    protected static final int MAX_MOVES = 150;

    /* Our drone can move at an angle of 10, 20,..., but not e.g. 26 degrees */
    protected static final int ANGLE_GRANULARITY = 10;

    /* The exact distance of any move the drone makes */
    protected static final double MOVE_DISTANCE = 0.0003;
    /* Radius of circle around any sensor in which the drone can read it */
    protected static final double MAX_READ_DISTANCE = 0.0002;
    /* Radius of circle around starting point in which drone is allowed to land */
    protected static final double MAX_LANDING_DISTANCE = 0.0003;

    public Drone(Point startingPoint) {
        this.currentPosition = startingPoint;
        this.currentActionRange = MAX_READ_DISTANCE;
        this.stepsMade = 0;
        this.moveAngleHistory = new ArrayList<Integer>();
    }

    /**
     * This method is the only way in which a drone can move (with the exception of
     * ShadowDrones, which may sometimes be teleported to a different location, when
     * checking whether an obstacle has been avoided).
     * 
     * @param angle because the length of each move is fixed, the angle is all that
     *              defines a move of the drone at its current position
     */
    protected abstract void makeMove(int angle);

    /**
     * This method is a short-cut that tells a drone to move straight towards its
     * destination, which should only be done assuming that no obstacle is keeping
     * it from doing so.
     */
    protected void makeMoveTowardsGoal() {
        var roundedAngle = computeRoundedAngleOfLineToGoal();

        makeMove(roundedAngle);
    }

    /**
     * This method determines whether the the drone can take a step towards its
     * current destination without leaving the confinement area or entering any
     * no-fly-zone.
     * 
     * @return whether the drone can take a step towards its destination
     */
    protected boolean canMoveTowardsGoal() {
        var roundedAngle = computeRoundedAngleOfLineToGoal();

        return canMove(roundedAngle);
    }

    /**
     * This helper method constructs a line from the drone's position to its
     * destination, computes its angle and rounds the result to give the "legal"
     * angle that the drone can use that comes closest to the real direction.
     * 
     * @return the angle of the line towards the drone's destination, rounded to be
     *         a multiple of the angle granularity
     */
    protected int computeRoundedAngleOfLineToGoal() {
        var straightPath = new LineSegment(this.currentPosition, this.currentDestination);

        var exactAngle = straightPath.getAngleInDegrees();
        var scaledAngle = exactAngle / ANGLE_GRANULARITY;
        var roundedAngle = (ANGLE_GRANULARITY * (int) Math.round(scaledAngle) + 360) % 360;

        return roundedAngle;
    }

    /**
     * Assuming that the drone is neither started outside the confinement area, nor
     * inside a No-Fly-Zone, the drone is able to move to a point at the appropriate
     * distance if and only if none of the borders of any of these polygons are
     * crossed.
     * 
     * @param angle the angle that defines the move in question
     * 
     * @return whether the specified move is legal
     */
    protected boolean canMove(int angle) {

        var nextPos = EuclideanUtils.getNextPosition(this.currentPosition, angle, MOVE_DISTANCE);

        var moveLineSegment = new LineSegment(this.currentPosition, nextPos);

        /*
         * We get a list of all 2D objects the boundaries of which may not be crossed.
         */
        var forbiddenObjects = new ArrayList<TwoDimensionalMapObject>();
        forbiddenObjects.add(App.getConfinementArea());
        for (var noFlyZone : App.getNoFlyZones()) {
            forbiddenObjects.add(noFlyZone);
        }

        /*
         * If we are trying to cross the boundaries of a forbidden object, we refrain
         * from doing so and remember which object hindered us.
         */
        for (var forbiddenObject : forbiddenObjects) {
            var forbiddenPolygon = forbiddenObject.getPolygon();
            if (EuclideanUtils.lineSegmentAndPolygonIntersect(moveLineSegment, forbiddenPolygon)) {
                obstacleInOurWay = forbiddenObject;
                return false;
            }
        }

        /*
         * If we have made it here without returning false, none of the forbidden
         * boundaries are crossed.
         */
        return true;
    }

    /**
     * This method is used when a drone is in move range of its destination, in
     * order to take it all the way there in 1 or 2 steps.
     * 
     * The reason being - if a sensor is very close to a building or an edge of the
     * confinement zone, accessing it may be very tricky, because the move distance
     * is greater than the reading range. In this (quite literally) edge case, we
     * will need an "in between move" to enter the radius, because flying straight
     * at the destination will make the drone crash.
     * 
     * This is also relevant if we need to make a waiting move because we cannot
     * take two sensor readings on the same step.
     * 
     * The idea is simple: Just try all angles! As soon as we find one that lets us
     * enter the required range in two (or preferably only one) steps, that one will
     * do.
     * 
     * This is not necessarily efficient, but the movement of our drone is not a
     * bottleneck in the performance of this application anyways.
     * 
     * @return whether the parking manoeuvre was successful
     */
    public boolean park() {

        var numAngles = 360 / ANGLE_GRANULARITY;

        /* The angle that was chosen for the potential in-between-move of the drone */
        Integer chosenInBetweenMoveAngle = null;
        /* The angle that was chosen for the final step of the drone before its goal */
        Integer chosenParkingMoveAngle = null;

        /* Try all possible angles */
        for (int i = 0; i < numAngles; i++) {
            var angleForCandidateInBetweenMove = i * ANGLE_GRANULARITY;

            /*
             * If the in between move we would like to consider is illegal, we don't need to
             * consider it as an option.
             */
            if (canMove(angleForCandidateInBetweenMove)) {
                var posAfterCandidateInBetweenMove = EuclideanUtils.getNextPosition(this.currentPosition,
                        angleForCandidateInBetweenMove, MOVE_DISTANCE);

                var shadowForSecondStep = new ShadowDrone(posAfterCandidateInBetweenMove, this.currentDestination);

                /*
                 * If this single move takes us into the required range, it is all we need from
                 * this method.
                 * 
                 * Otherwise, we check if a two-move parking manoeuvre with this in-between move
                 * is possible - but only if none has been found yet, because any legal 2-move
                 * combination that works... works.
                 */
                if (shadowForSecondStep.isInRangeOfDestination()) {
                    makeMove(angleForCandidateInBetweenMove);
                    return true;
                } else if (chosenInBetweenMoveAngle == null && shadowForSecondStep.canMoveTowardsGoal()) {
                    /*
                     * After the in-between move we take a step straight at the goal and see if this
                     * works. If it does, we record it in case no 1-step manoeuvre is later found.
                     */
                    shadowForSecondStep.makeMoveTowardsGoal();
                    if (shadowForSecondStep.isInRangeOfDestination()) {
                        var parkingAngle = shadowForSecondStep.getMoveAngleHistory().get(0);

                        chosenInBetweenMoveAngle = angleForCandidateInBetweenMove;
                        chosenParkingMoveAngle = parkingAngle;

                    }
                }
            }
        }

        /*
         * If we get here, no single move was enough to bring us into the required range
         * - but hopefully we have found a 2-step manoeuvre, which we now go for.
         */
        if (chosenInBetweenMoveAngle == null) {
            System.out.println("The parking attempt was not successful.");
            return false;
        } else {
            makeMove(chosenInBetweenMoveAngle);
            makeMove(chosenParkingMoveAngle);
            /*
             * 2-move parking attempts are expected to be rare, therefore it's worth taking
             * note of them.
             */
            System.out.println("Successful parking attempt in 2 moves");
            return true;
        }

    }

    /**
     * This method is just a shortcut to find out whether the drone is "close
     * enough" to its current destination.
     */
    protected boolean isInRangeOfDestination() {
        return EuclideanUtils.computeDistance(currentPosition, currentDestination) <= currentActionRange;
    }

    /* The following are just getters and a setter. Nothing unexpected here. */

    public Point getCurrentPosition() {
        return currentPosition;
    }

    public int getStepsMade() {
        return this.stepsMade;
    }

    public TwoDimensionalMapObject getObstacleInOurWay() {
        return obstacleInOurWay;
    }

    /*
     * This method is needed because the tour planner calls the drone's
     * flyToCurrentDestination method
     */
    public static double getMaxReadDistance() {
        return MAX_READ_DISTANCE;
    }

    public static double getMaxLandingDistance() {
        return MAX_LANDING_DISTANCE;
    }

    public ArrayList<Integer> getMoveAngleHistory() {
        return moveAngleHistory;
    }

    public void setCurrentActionRange(double range) {
        currentActionRange = range;
    }

}
