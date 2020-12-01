package uk.ac.ed.inf.aqmaps.drone;

import com.mapbox.geojson.Point;

import uk.ac.ed.inf.aqmaps.geometry.EuclideanUtils;
import uk.ac.ed.inf.aqmaps.geometry.LineSegment;
import uk.ac.ed.inf.aqmaps.map.TwoDimensionalMapObject;

/**
 * Shadow drones are a fascinating tool that our main drone has at its disposal.
 * They are used as "scouts" and allow our main drone to decide how exactly it
 * should avoid an obstacle, without moving an inch. They have some unique
 * properties, defined in this class.
 */
public class ShadowDrone extends Drone {

    /*
     * The maximum number of moves a shadow drone is allowed to spend on trying to
     * avoid an obstacle (in a clockwise / counter-clockwise way) before giving up
     * and deciding that it's not feasible to do so. Avoids infinite loops.
     */
    private static final int MAX_MOVES_TO_AVOID_OBSTACLE = 15;

    /**
     * The constructor of the shadow drone class. Note that, as opposed to our main
     * drone which is given a tour, a shadow drone ever only has a single
     * destination.
     * 
     * @param startingPoint the starting point of the shadow drone.
     * @param destination   the destination of the shadow drone.
     */
    public ShadowDrone(Point startingPoint, Point destination) {
        super(startingPoint);
        this.currentDestination = destination;
    }

    /**
     * Given an obstacle, the max. distance to its destination that our drone has to
     * reach, as well as the way around the obstacle which this shadow drone should
     * attempt, this method computes a way of dodging / rotating around the obstacle
     * in the given direction.
     * 
     * It then returns an estimation of the cost that is involved in doing so.
     * 
     * Note: On each step, we alter our "ideal" flight path in the smallest possible
     * way that still avoids crashing into the obstacle. This allows us to "hug" the
     * obstacle, making for a legal and reasonably efficient path.
     * 
     * Note: We do not allow alterations of more than 180 degrees - for some rather
     * complex polygons a combination of "clockwise" and "counter-clockwise" moves
     * may be necessary to make a rotation work... but our simple approach works
     * perfectly well in the given scenario.
     * 
     * @param obstacle         the object that needs to be avoided
     * @param maxFinalDistance the radius around the destination that our drone
     *                         needs to reach
     * @param clockwise        whether a clockwise rotation should be attempted
     * 
     * @return the estimated number of steps it will take our drone to get to its
     *         current destination, if it chooses the way around the obstacle found
     *         by this shadow drone
     */
    public double costOfAvoidingObstacle(TwoDimensionalMapObject obstacle, double maxFinalDistance, boolean clockwise) {

        var stepsToAvoidObstacle = 0;
        var numAttemptedMoves = 0;

        /* We look for moves that dodge the obstacle until it is no longer in our way */
        while (!this.avoidsObstacle(obstacle)) {
            var roundedCurrentAngle = computeRoundedAngleOfLineToGoal();

            var madeMove = false;
            int adjustedAngle;
            for (int i = 0; i <= 180 / ANGLE_GRANULARITY; i++) {
                if (clockwise) {
                    adjustedAngle = (roundedCurrentAngle + i * ANGLE_GRANULARITY + 360) % 360;
                } else {
                    adjustedAngle = (roundedCurrentAngle - i * ANGLE_GRANULARITY + 360) % 360;
                }

                if (canMove(adjustedAngle)) {
                    makeMove(adjustedAngle);
                    stepsToAvoidObstacle++;
                    madeMove = true;
                    numAttemptedMoves++;
                    break;
                }
            }

            /*
             * If all attempts to avoid the obstacle fail, we give up, notifying the main
             * drone that called this method that a rotation in the given direction is not
             * feasible.
             */
            if (!madeMove || numAttemptedMoves > MAX_MOVES_TO_AVOID_OBSTACLE) {
                if (clockwise) {
                    System.out.println("Clockwise rotation does not work.");
                } else {
                    System.out.println("Counter-Clockwise rotation does not work.");
                }
                return Double.POSITIVE_INFINITY;
            }
        }

        /*
         * An estimation of the number of steps needed for our main drone to get to the
         * goal. Remaining distance is weighted in a way that accounts for additional
         * obstacles that may occur and the coarse-grained angle selection.
         */
        var remainingDistance = EuclideanUtils.computeDistance(currentPosition, currentDestination);
        var remainingDistanceWeight = 1.1;
        return stepsToAvoidObstacle + remainingDistanceWeight / MOVE_DISTANCE * remainingDistance;

    }

    /**
     * This method tells us whether this shadow drone has managed to avoid the
     * obstacle it was given. We make the drone fly to its destination, avoiding all
     * potential obstacles, except the one in question.
     * 
     * If the obstacle is no longer in the way of the shadow drone, this drone has
     * done its job. Other obstacles may still play a role for our main drone, and
     * further alterations to the path may be needed, but that will not be the
     * concern of this shadow drone instance.
     * 
     * Note: Here, we use the setPosition method of the Shadow Drone subclass,
     * instead of the general makeMove method. That is because we do not care if the
     * move is actually legal - only if it hits the given obstacle. We also don't
     * count the moves as being actual moves of the drone, or record the angles. We
     * do not want this method to have any "side effects", which the makeMove method
     * would enforce.
     * 
     * @param obstacle the obstacle to be avoided
     * 
     * @return whether the drone can fly straight towards the destination without
     *         hitting the obstacle in question
     */
    public boolean avoidsObstacle(TwoDimensionalMapObject obstacle) {
        var startPos = currentPosition;
        var forbiddenPolygon = obstacle.getPolygon();

        while (!isInRangeOfDestination()) {

            /*
             * We use the park method to avoid falling prey to any edge cases (see Drone
             * class)
             */
            if (EuclideanUtils.computeDistance(currentPosition, currentDestination) <= MOVE_DISTANCE) {
                var parkingShadow = new ShadowDrone(currentPosition, currentDestination);
                /* Update the action range of the shadow in case we are about to land. */
                parkingShadow.setCurrentActionRange(currentActionRange);
                var result = parkingShadow.park();
                currentPosition = startPos;
                return result;
            }

            var nextPos = computeNextPosTowardsGoal();
            var moveLineSegment = new LineSegment(this.currentPosition, nextPos);

            if (!EuclideanUtils.lineSegmentAndPolygonIntersect(moveLineSegment, forbiddenPolygon)) {
                this.setPosition(nextPos);
            } else {
                this.setPosition(startPos);
                return false;
            }
        }

        /*
         * If we got here, that means that the obstacle in question is no longer in the
         * way of our drone.
         */
        this.currentPosition = startPos;
        return true;
    }

    /**
     * This helper method is a shortcut that computes the next position of our drone
     * if it was to fly straight towards its destination.
     * 
     * @return the next position of the drone on its path straight towards its goal
     */
    private Point computeNextPosTowardsGoal() {
        var roundedAngle = computeRoundedAngleOfLineToGoal();
        var nextPos = EuclideanUtils.getNextPosition(this.currentPosition, roundedAngle, MOVE_DISTANCE);
        return nextPos;
    }

    /**
     * The implementation of the makeMove method for the ShadowDrone differs from
     * the MainDrone one in that there are no "controlled drone crashes". A shadow
     * drone may only ever to fail to move even if the makeMove method is called if
     * there is a severe flaw in our program logic.
     * 
     * Also, shadow drones do not have a "position log" as the main drone does,
     * since the angles alone give us all the information we require.
     * 
     */
    @Override
    protected void makeMove(int angle) {

        if (!this.canMove(angle)) {
            System.out.println("A shadow drone was told to make an impossible move. Exiting the program.");
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

    /**
     * This setter is exclusive to the ShadowDrone subclass. Because shadow drones
     * do not represent physical entities, they are allowed to fly to their
     * destination, just to see how that would go, before returning to their
     * original state.
     * 
     * Using the makeMove method would make this impossible because of its side
     * effects (that are crucial if the shadow drone makes an "actual" move).
     * 
     * @param position the position to be assigned.
     */
    public void setPosition(Point position) {
        this.currentPosition = position;
    }

}
