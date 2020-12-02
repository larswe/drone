package uk.ac.ed.inf.aqmaps.drone;

import java.util.ArrayList;
import java.util.Arrays;

import com.mapbox.geojson.Point;

import uk.ac.ed.inf.aqmaps.geometry.EuclideanUtils;
import uk.ac.ed.inf.aqmaps.map.Sensor;
import uk.ac.ed.inf.aqmaps.map.TwoDimensionalMapObject;

/**
 * The MainDrone class captures the properties of the drone that is used to take
 * the sensor readings. It is different from the "ShadowDrone" in that there
 * will only ever be one main drone, whereas shadow drones simply simulate the
 * future steps of the main drone, thereby aiding its decision making.
 *
 */
public class MainDrone extends Drone {

    /*
     * The list of sensors that the drone is supposed to read in exactly the given
     * order.
     */
    private ArrayList<Sensor> sensorTour;

    /*
     * The MainDrone needs to remember its starting position to return to it later.
     */
    private final Point startingPosition;

    /* The index of the sensor the drone is meant to read next */
    private int currentDestinationIndex;

    /*
     * Once a critical error has occurred, the drone is assumed to have crashed and
     * can no longer make moves. This is preferable to simply exiting the
     * application, because in real scenarios, the drone may e.g. run out of
     * battery, in which case we would still like to see the output up to this
     * point.
     */
    private boolean hasCrashed = false;

    /*
     * Log information about the sensors the drone has or hasn't read.
     */
    private boolean[] sensorsVisitedArray;
    private double[] readingsForAllSensors;
    private ArrayList<String> sensorReadHistory;

    /*
     * All positions the drone had at some point over the course of the current day.
     */
    private ArrayList<Point> positionHistory;

    /**
     * The constructor of the MainDrone class
     * 
     * @param startingPosition the position the drone starts at on move 0
     * @param tour             the list of sensors the drone is expected to read
     * @param verbose          whether we want the drone to print success messages
     *                         to standard output
     */
    public MainDrone(Point startingPosition, ArrayList<Sensor> tour, boolean verbose) {
        super(startingPosition, verbose);

        this.startingPosition = currentPosition;
        var startingPosAsList = Arrays.asList(this.startingPosition);
        this.positionHistory = new ArrayList<Point>(startingPosAsList);
        this.sensorTour = tour;

        /* Initialising instance variables the obvious way */
        currentDestinationIndex = 0;
        sensorsVisitedArray = new boolean[tour.size()];
        readingsForAllSensors = new double[tour.size()];
        sensorReadHistory = new ArrayList<String>();

        if (tour.size() > 0) {
            currentDestination = tour.get(0).getPosition();
            currentActionRange = MAX_READ_DISTANCE;
        } else {
            System.out.println("A MainDrone was given an empty tour!");
            currentDestination = startingPosition;
            currentActionRange = MAX_LANDING_DISTANCE;
        }
    }

    /**
     * This acts as the "main method" of this class. The drone visits and reads all
     * sensors in the given order and eventually returns to its starting location
     * (if it hasn't crashed).
     */
    public void completeTour() {
        while (currentDestinationIndex < sensorTour.size() && !hasCrashed) {
            currentDestination = sensorTour.get(currentDestinationIndex).getPosition();
            var stepsToGetToSensor = flyToCurrentDestination();

            /*
             * We can only take one reading per move. Thus, if the drone is already in range
             * of the next sensor, we need to make a waiting move (or two, if staying in
             * range of the next sensor turns out to be impossible).
             */
            if (stepsToGetToSensor == 0) {
                this.park();
            }

            readSensor(sensorTour.get(currentDestinationIndex));

            /* Preparing drone for next sensor to be read */
            this.currentDestinationIndex++;
        }

        if (!hasCrashed) {
            /* Once all sensors have been read, return to starting position */
            currentActionRange = MAX_LANDING_DISTANCE;
            currentDestination = startingPosition;
            flyToCurrentDestination();

            if (verbose) {
                System.out.println("Successfully finished the tour after " + stepsMade + " steps!");
            }
        } else {
            System.out.println("Sadly, the drone crashed.");
        }

    }

    /**
     * This high-level method guides the drone to its next destination, returning
     * the number of steps it took to get there after the method call was made.
     * 
     * @return The number of steps the drone made to reach its destination
     */
    public int flyToCurrentDestination() {

        var moveCountAtStart = stepsMade;

        while (!isInRangeOfDestination() && !hasCrashed) {

            if (EuclideanUtils.computeDistance(currentPosition, currentDestination) <= MOVE_DISTANCE) {
                park();
            } else if (canMoveTowardsGoal()) {
                makeMoveTowardsGoal();
            } else {
                avoidObstacle(obstacleInOurWay);
            }

        }

        var moveCountAtEnd = stepsMade;

        /*
         * The difference of the move counter before and after flying to the goal is the
         * number of steps it took to get there.
         */
        return moveCountAtEnd - moveCountAtStart;

    }

    /**
     * Given an obstacle which keeps the drone from moving straight towards its
     * destination, this method lets the main drone spawn shadow drones to compare
     * the cost of dodging the obstacle in a left/clockwise and in a
     * right/counterclockwise manner.
     * 
     * The moves of the shadow drone that yielded the more promising solution are
     * then adopted.
     * 
     * If both shadow drones fail their task for whatever reason, the drone crashes.
     * This however did not happen once during testing.
     * 
     * @param obstacleInOurWay the obstacle which last kept the drone from moving
     *                         towards its destination.
     */
    public void avoidObstacle(TwoDimensionalMapObject obstacleInOurWay) {

        if (verbose) {
            System.out.println("The main drone tries to avoid " + obstacleInOurWay.getName());
        }

        /* Estimate the cost of a left rotation */
        var leftShadow = new ShadowDrone(currentPosition, currentDestination, verbose);
        var approxCostLeftAvoid = leftShadow.costOfAvoidingObstacle(obstacleInOurWay, currentActionRange, true);

        if (verbose) {
            System.out.println("A clockwise rotation leads to estimated cost: " + approxCostLeftAvoid);
        }

        /* Estimate the cost of a right rotation */
        var rightShadow = new ShadowDrone(currentPosition, currentDestination, verbose);
        var approxCostRightAvoid = rightShadow.costOfAvoidingObstacle(obstacleInOurWay, currentActionRange, false);

        if (verbose) {
            System.out.println("A counter-clockwise rotation leads to estimated cost: " + approxCostRightAvoid);
        }

        /*
         * If both shadow drones returned infinite cost, we have no way out - the drone
         * crashes
         */
        if (Double.isInfinite(approxCostLeftAvoid) && Double.isInfinite(approxCostRightAvoid)) {
            System.out.println("The drone can not find away to get around the obstacle " + obstacleInOurWay);
            hasCrashed = true;
            return;
        }

        /*
         * If things have gone well, we copy the moves of the drone that gave the
         * cheaper solution.
         */
        ArrayList<Integer> anglesToFlyAt;
        if (approxCostLeftAvoid < approxCostRightAvoid) {
            if (verbose) {
                System.out.println("Chose clockwise rotation to avoid obstacle " + obstacleInOurWay.getName());
            }
            anglesToFlyAt = leftShadow.getMoveAngleHistory();
        } else {
            if (verbose) {
                System.out.println("Chose counter-clockwise rotation to avoid obstacle " + obstacleInOurWay.getName());
            }
            anglesToFlyAt = rightShadow.getMoveAngleHistory();
        }
        for (var angle : anglesToFlyAt) {
            makeMove(angle);
        }
    }

    /**
     * This method is used to take a reading from a given sensor. This only works if
     * the sensor is indeed in the required range. The reading is stored in order to
     * later serve as part of the output of this application.
     * 
     * @param sensor the sensor to be read
     */
    private void readSensor(Sensor sensor) {

        if (EuclideanUtils.computeDistance(currentPosition, sensor.getPosition()) <= MAX_READ_DISTANCE) {
            if (verbose) {
                System.out.println("Read sensor " + currentDestinationIndex);
            }
            sensorsVisitedArray[currentDestinationIndex] = true;
            readingsForAllSensors[currentDestinationIndex] = sensor.outputReading();
            sensorReadHistory.set(stepsMade - 1, sensor.getW3wLocation().toString());

        } else {
            System.out.println("The drone was asked to read a sensor that was out of range! "
                    + "Because this is an illegal action, the drone crashes.");
            hasCrashed = true;
        }

    }

    /**
     * This method is the only way in which our main drone can move. If this is
     * impossible for whatever reason, the drone gives the appropriate response.
     * 
     * @param angle the angle that specifies in what direction the drone should
     *              move.
     */
    protected void makeMove(int angle) {
        /* The first two options can only ever occur if our program is buggy. */
        if (hasCrashed) {
            System.out.println("The main drone has crashed! It can no longer move! "
                    + "Stopping program execution to avoid an infinite loop.");
            System.exit(1);
        } else if (!canMove(angle) || angle % ANGLE_GRANULARITY != 0) {
            System.out.println("The main drone was told to make an impossible move.");
            hasCrashed = true;
        } else if (this.stepsMade >= MAX_MOVES) {
            System.out.println("The main drone has run out of battery and has crashed!");
            hasCrashed = true;
        } else {
            var nextPos = EuclideanUtils.getNextPosition(this.currentPosition, angle, MOVE_DISTANCE);
            currentPosition = nextPos;
            moveAngleHistory.add(angle);
            /*
             * We add null to the sensor reading log - if we do take a reading on this step,
             * we will update this value in the downloadReadingsFromSensor method.
             */
            sensorReadHistory.add("null");
            positionHistory.add(nextPos);
            stepsMade++;
        }
    }

    /* Getters and Setters */

    public ArrayList<Point> getPositionHistory() {
        return positionHistory;
    }

    public ArrayList<Sensor> getSensorTour() {
        return sensorTour;
    }

    public void setSensorTour(ArrayList<Sensor> sensorTour) {
        this.sensorTour = sensorTour;
    }

    public boolean[] getSensorsVisitedArray() {
        return this.sensorsVisitedArray;
    }

    public double[] getReadingsForAllSensors() {
        return this.readingsForAllSensors;
    }

    public ArrayList<String> getSensorReadHistory() {
        return this.sensorReadHistory;
    }

}
