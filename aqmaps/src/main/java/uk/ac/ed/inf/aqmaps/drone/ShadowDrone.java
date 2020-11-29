package uk.ac.ed.inf.aqmaps.drone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import uk.ac.ed.inf.aqmaps.geometry.EuclideanUtils;
import uk.ac.ed.inf.aqmaps.geometry.LineSegment;

/*
 * A fascinating tool our main drone has at its disposal - To plan its precise
 * path to its current destination, before actually flying anywhere, it can
 * command a "shadow drone". Its shadow drone has the same flight properties as
 * the main drone, but e.g. it does not need to obey any "Max Moves"
 * restrictions, and we can change its current position at will.
 */
public class ShadowDrone extends Drone {

    private static final int MAX_MOVES_TO_AVOID_OBSTACLE = 15;

    public ShadowDrone(Point startingPoint, Point destination) {
        super(startingPoint);
        this.currentDestination = destination;
    }

    @Override
    public boolean canGetTowardsDestinationInStraightLine() {

        var startPos = this.currentPosition;
        this.stepsMade = 0;

        // System.out.println();
        // System.out.println("Destination: " + currentDestination);
        // System.out.println("Position: " + this.currentPosition);
        // System.out.println(
        // "Distance: " + EuclideanUtils.computeDistance(this.currentPosition,
        // this.currentDestination));

        var straightPath = new LineSegment(this.currentPosition, this.currentDestination);
        var exactAngle = straightPath.getAngleInDegrees();
        /*
         * Because our drone can only move at angles that are multiples of 10, we round
         * accordingly.
         */
        double scaledAngle = exactAngle / ANGLE_GRANULARITY; // e.g. 17.4 for 174 degrees and a granularity of 10
        double roundedAngle = (ANGLE_GRANULARITY * (int) Math.rint(scaledAngle) + 360) % 360; // e.g. 170 for the
                                                                                              // above

        // System.out.println("Angle: " + exactAngle);

        /*
         * Try to approach the destination in the most straight-forward way. If we can,
         * we do it. If we cannot, this approach evidently does not work and we return
         * false.
         */
        if (this.canMove(roundedAngle)) {
            this.makeMove(roundedAngle);
        } else {
            // System.out.println("Straight line approach will fail after " + this.stepsMade
            // + " steps!");
            // System.out.println("Failed because of " + this.obstacleInOurWay);
            // System.out.println("The exact angle was " + exactAngle);
            // System.out.println("Tried to move from " + this.currentPosition + " to "
            // + EuclideanUtils.getNextPosition(this.currentPosition, roundedAngle,
            // MOVE_DISTANCE));
            // System.out.println();

            this.setPosition(startPos);

            return false;
        }

        /*
         * If we got here, the drone can move straight towards its goal without having
         * to adjust its course due to obstacles.
         */
        this.currentPosition = startPos;
        return true;
    }

    public double distToAvoidObstacleClockwise(Polygon obstacle, double maxFinalDistance) {

        double distToAvoidObstacle = 0.0;
        var numAttemptedMoves = 0;
        int previousAngle = 0;

        while (!this.avoidsObstacle(obstacle, maxFinalDistance)) {
            var lineToDestination = new LineSegment(this.currentPosition, this.currentDestination);
            var exactAngle = lineToDestination.getAngleInDegrees();

            double scaledAngle = exactAngle / ANGLE_GRANULARITY;
            var roundedCurrentAngle = (ANGLE_GRANULARITY * Math.rint(scaledAngle) + 360) % 360;

            var madeMove = false;
            for (int i = 0; i <= 180 / ANGLE_GRANULARITY; i++) {
                var adjustedAngle = (roundedCurrentAngle + i * ANGLE_GRANULARITY + 360) % 360;

                /*
                 * Instead of repeating the same 2 moves back and forth indefinitely, we never
                 * fly in the opposite direction to the previous step. This keeps us from being
                 * stuck in an infinite loop.
                 */
                if (numAttemptedMoves > 0 && previousAngle % 180 == adjustedAngle % 180
                        && previousAngle % 360 != adjustedAngle % 360) {
                    continue;
                }

                if (canMove(adjustedAngle)) {
                    makeMove(adjustedAngle);
                    distToAvoidObstacle += MOVE_DISTANCE;
                    madeMove = true;
                    numAttemptedMoves++;
                    previousAngle = (int) adjustedAngle;
                    break;
                }
            }

            if (!madeMove || numAttemptedMoves > MAX_MOVES_TO_AVOID_OBSTACLE) {
                System.out.println("Clockwise rotation does not work.");
                return Double.POSITIVE_INFINITY;
            }
        }

        // System.out.println("A right rotation has a cost of " + (distToAvoidObstacle
        // + 1.1 * EuclideanUtils.computeDistance(this.currentPosition,
        // this.currentDestination)));
        return distToAvoidObstacle
                + 1.1 * EuclideanUtils.computeDistance(this.currentPosition, this.currentDestination);

    }

    public double distToAvoidObstacleCounterClockwise(Polygon obstacle, double maxFinalDistance) {

        double distToAvoidObstacle = 0.0;
        var numAttemptedMoves = 0;
        int previousAngle = 0;

        while (!this.avoidsObstacle(obstacle, maxFinalDistance)) {
            var lineToDestination = new LineSegment(this.currentPosition, this.currentDestination);
            var exactAngle = lineToDestination.getAngleInDegrees();

            double scaledAngle = exactAngle / ANGLE_GRANULARITY;
            var roundedCurrentAngle = (ANGLE_GRANULARITY * Math.rint(scaledAngle) + 360) % 360;

            var madeMove = false;
            for (int i = 0; i <= 180 / ANGLE_GRANULARITY; i++) {
                var adjustedAngle = (roundedCurrentAngle - i * ANGLE_GRANULARITY + 360) % 360;

                if (numAttemptedMoves > 0 && previousAngle % 180 == adjustedAngle % 180
                        && previousAngle % 360 != adjustedAngle % 360) {
                    continue;
                }

                if (canMove(adjustedAngle)) {
                    makeMove(adjustedAngle);
                    distToAvoidObstacle += MOVE_DISTANCE;
                    madeMove = true;
                    numAttemptedMoves++;
                    previousAngle = (int) adjustedAngle;
                    break;
                }
            }

            if (!madeMove || numAttemptedMoves > MAX_MOVES_TO_AVOID_OBSTACLE) {
                System.out.println("Counter-clockwise rotation does not work.");
                return Double.POSITIVE_INFINITY;
            }
        }

        // System.out.println("A right rotation has a cost of " + (distToAvoidObstacle
        // + 1.1 * EuclideanUtils.computeDistance(this.currentPosition,
        // this.currentDestination)));
        return distToAvoidObstacle
                + 1.1 * EuclideanUtils.computeDistance(this.currentPosition, this.currentDestination);
    }

    /*
     * Computes whether the drone can fly straight towards the destination without
     * hitting the obstacle to be avoided. This method is needed if more than one
     * obstacle lies in the way of the drone, but we want to find out whether one
     * specific obstacle has been avoided.
     */
    public boolean avoidsObstacle(Polygon obstacle, double maxFinalDistance) {
        var startPos = this.currentPosition;
        int stepsChecked = 0;

        while (!this.isInRangeOfPoint(this.currentDestination, maxFinalDistance)) {

            if (this.isInRangeOfPoint(currentDestination, MOVE_DISTANCE)) {
                var parkingShadow = new ShadowDrone(currentPosition, currentDestination);
                var result = parkingShadow.park(MOVE_DISTANCE);
                this.currentPosition = startPos;
                return result;
            }

            var nextPos = getNextPosTowardsGoal();
            var moveLineSegment = new LineSegment(this.currentPosition, nextPos);

            if (!EuclideanUtils.lineSegmentAndPolygonIntersect(moveLineSegment, obstacle)) {
                /*
                 * NOTE: Here, we use the setPosition method of the Shadow Drone subclass,
                 * instead of the general makeMove method. That is because we do not care if the
                 * move is actually legal - only if it hits the specific obstacle we are trying
                 * to dodge. We also don't count the moves as being actual moves of the drone.
                 * We just count how many moves we have checked.
                 */
                this.setPosition(nextPos);
                stepsChecked++;
            } else {
                this.setPosition(startPos);
                // System.out.println("Does not avoid obstacle with an angle of " +
                // moveLineSegment.getAngleInDegrees());
                return false;
            }
        }

        /*
         * If we got here, the obstacle in question does not stop our drone from making
         * way towards its destination. It is possible that another obstacle is now in
         * the way, but the current obstacle is behind us.
         */
        // System.out.println("Avoided the obstacle " + obstacle);
        // System.out.println();
        this.currentPosition = startPos;
        return true;
    }

    public Point getNextPosTowardsGoal() {
        var straightPath = new LineSegment(this.currentPosition, this.currentDestination);

        var exactAngle = straightPath.getAngleInDegrees();
        double scaledAngle = exactAngle / ANGLE_GRANULARITY; // e.g. 17.4 for 174 degrees and a granularity of 10
        double roundedAngle = (ANGLE_GRANULARITY * (int) Math.rint(scaledAngle) + 360) % 360; // e.g. 170 for the above

        var nextPos = EuclideanUtils.getNextPosition(this.currentPosition, roundedAngle, MOVE_DISTANCE);

        return nextPos;
    }

    protected void makeMove(double angle) {

        if (!this.canMove(angle)) {
            System.out.println("The shadow drone was told to make an impossible move.");
            System.exit(1);
        } else if (this.stepsMade >= 150) {
            System.out.println("A shadow drone has run out of moves! This should never happen.");
            System.exit(1);
        } else {
            var nextPos = EuclideanUtils.getNextPosition(this.currentPosition, angle, MOVE_DISTANCE);
            moveAngleHistory.add(angle);
            this.currentPosition = nextPos;
            this.stepsMade++;
        }
    }

    public void setStepsMade(int stepsMade) {
        this.stepsMade = stepsMade;
    }

    public void setPosition(Point position) {
        this.currentPosition = position;
    }

}
