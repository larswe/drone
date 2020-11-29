package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

/*
 * A fascinating tool our main drone has at its disposal - To plan its precise
 * path to its current destination, before actually flying anywhere, it can
 * command a "shadow drone". Its shadow drone has the same flight properties as
 * the main drone, but e.g. it does not need to obey any "Max Moves"
 * restrictions, and we can change its current position at will.
 */
public class ShadowDrone extends Drone {

    private ArrayList<Double> moveAngleHistory;

    public ShadowDrone(Point startingPoint, Point destination) {
        super(startingPoint);
        this.currentDestination = destination;
        this.moveAngleHistory = new ArrayList<Double>();
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

        while (!this.avoidsObstacle(obstacle, maxFinalDistance)) {
            var lineToDestination = new LineSegment(this.currentPosition, this.currentDestination);
            var exactAngle = lineToDestination.getAngleInDegrees();

            // System.out.println("Current Angle: " + exactAngle);

            double scaledAngle = exactAngle / ANGLE_GRANULARITY;
            double roundedCurrentAngle = (ANGLE_GRANULARITY * (int) Math.rint(scaledAngle) + 360) % 360;

            // System.out.println("Rounded Current Angle: " + roundedCurrentAngle);

            var linesToCorners = new ArrayList<LineSegment>();

            var corners = obstacle.coordinates().get(0);
            for (Point corner : corners) {
                var lineToCorner = new LineSegment(this.currentPosition, corner);
                linesToCorners.add(lineToCorner);
            }

            /*
             * We find the outermost angle of the obstacle that's "to the left" of our
             * drone. If there is an angle with less than 180 degrees relative to the
             * straight path towards the destination, that means we only we cannot actually
             * fly towards our goal in a straight line because of the restricted angle
             * selection. In that case we just choose the largest left angle available.
             */
            var obtuseLeftAnglesRelativeToStraightPath = new ArrayList<Double>();
            var reflexLeftAnglesRelativeToStraightPath = new ArrayList<Double>();
            for (LineSegment lineToCorner : linesToCorners) {
                var leftRelativeAngle = (lineToCorner.getAngleInDegrees() - roundedCurrentAngle + 360) % 360;

                // System.out.println("Relative Angle: " + leftRelativeAngle + " , " +
                // lineToCorner.getEndPoint());

                /*
                 * We first consider only turns of at most 180 degrees to be potential
                 * "left turns"
                 */
                if (leftRelativeAngle <= 180) {
                    obtuseLeftAnglesRelativeToStraightPath.add(leftRelativeAngle);
                } else {
                    reflexLeftAnglesRelativeToStraightPath.add(leftRelativeAngle);
                }
            }

            /*
             * If there is a corner to our left, we make the smallest possible adjustment to
             * our course that "dodges" the leftmost corner. Otherwise it can only be the
             * case that the corner of the building is actually right beside us but we need
             * to fly a bit further if we want to make a right turn afterwards. In this
             * case, we simply adjust the course of the drone in the slightest possible way.
             */

            double minLeftAdjustmentExact;

            if (obtuseLeftAnglesRelativeToStraightPath.isEmpty()) {
                minLeftAdjustmentExact = 1 * ANGLE_GRANULARITY;
            } else {
                minLeftAdjustmentExact = Collections.max(obtuseLeftAnglesRelativeToStraightPath);
            }

            var numCogsToTurnLeft = 1 + (int) (minLeftAdjustmentExact / ANGLE_GRANULARITY);
            var necessaryLeftAdjustment = numCogsToTurnLeft * ANGLE_GRANULARITY;
            var adjustedAngle = (roundedCurrentAngle + necessaryLeftAdjustment + 360) % 360;

            // System.out.println("Moved from " + this.currentPosition.coordinates() + " to
            // "
            // + EuclideanUtils.getNextPosition(this.currentPosition, adjustedAngle,
            // MOVE_DISTANCE).coordinates()
            // + " at angle " + adjustedAngle + " with a left adjustment of " +
            // necessaryLeftAdjustment);
            // System.out.println();

            /*
             * If the move we chose does not work, because there's another obstacle in the
             * way, because rotation in this direction is extremely inconvenient (which
             * should never happen with our current no fly zones but may happen with "less
             * convex ones), or because the confinement area ends, we give up on the
             * rotation in this direction.
             */
            if (canMove(adjustedAngle)) {
                makeMove(adjustedAngle);
                distToAvoidObstacle += MOVE_DISTANCE;
            } else {
                // System.out.println("Moving at an angle of " + adjustedAngle
                // + " is not possible, therefore clockwise rotation does not work.");
                return Double.POSITIVE_INFINITY;
            }
        }

        /*
         * We return an estimation of the distance to the drone's destination that
         * remains if a left rotation is chosen.
         */

        // System.out.println("A left rotation has a cost of " + (distToAvoidObstacle
        // + 1.1 * EuclideanUtils.computeDistance(this.currentPosition,
        // this.currentDestination)));
        // System.out.println(this.avoidsObstacle(obstacle, maxFinalDistance));

        return distToAvoidObstacle
                + 1.1 * EuclideanUtils.computeDistance(this.currentPosition, this.currentDestination);

    }

    public double distToAvoidObstacleCounterClockwise(Polygon obstacle, double maxFinalDistance) {

        double distToAvoidObstacle = 0.0;

        while (!this.avoidsObstacle(obstacle, maxFinalDistance)) {
            var lineToDestination = new LineSegment(this.currentPosition, this.currentDestination);
            var exactAngle = lineToDestination.getAngleInDegrees();
            
            double scaledAngle = exactAngle / ANGLE_GRANULARITY;
            double roundedCurrentAngle = (ANGLE_GRANULARITY * (int) Math.rint(scaledAngle) + 360) % 360;

            var linesToCorners = new ArrayList<LineSegment>();

            var corners = obstacle.coordinates().get(0);
            for (Point corner : corners) {
                var lineToCorner = new LineSegment(this.currentPosition, corner);
                linesToCorners.add(lineToCorner);
            }

            var obtuseRightHandAnglesRelativeToStraightPath = new ArrayList<Double>();
            for (LineSegment lineToCorner : linesToCorners) {
                var rightHandRelativeAngle = (roundedCurrentAngle - lineToCorner.getAngleInDegrees() + 360) % 360;

                // System.out.println("Relative Angle: " + rightHandRelativeAngle + " , " +
                // lineToCorner.getEndPoint());

                if (rightHandRelativeAngle <= 180) {
                    obtuseRightHandAnglesRelativeToStraightPath.add(rightHandRelativeAngle);
                }
            }

            double minRightAdjustmentExact;

            if (obtuseRightHandAnglesRelativeToStraightPath.isEmpty()) {
                minRightAdjustmentExact = 1 * ANGLE_GRANULARITY;
                ;
            } else {
                minRightAdjustmentExact = Collections.max(obtuseRightHandAnglesRelativeToStraightPath);
            }

            var numCogsToTurnRight = 1 + (int) (minRightAdjustmentExact / ANGLE_GRANULARITY);
            var necessaryRightAdjustment = numCogsToTurnRight * ANGLE_GRANULARITY;
            var adjustedAngle = (roundedCurrentAngle - necessaryRightAdjustment + 360) % 360;

            // System.out.println("Moved from " + this.currentPosition.coordinates() + " to
            // "
            // + EuclideanUtils.getNextPosition(this.currentPosition, adjustedAngle,
            // MOVE_DISTANCE).coordinates()
            // + " at angle " + adjustedAngle + " with a right adjustment of " +
            // necessaryRightAdjustment);

            if (canMove(adjustedAngle)) {
                makeMove(adjustedAngle);
                distToAvoidObstacle += MOVE_DISTANCE;
            } else {
                // System.out.println("Moving at an angle of " + adjustedAngle
                // + " is not possible, therefore counter-clockwise rotation does not work.");
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
                this.park(maxFinalDistance);
            } else {
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
            this.moveAngleHistory.add(angle);
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

    public ArrayList<Double> getMoveAngleHistory() {
        return moveAngleHistory;
    }

}
