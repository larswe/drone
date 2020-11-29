package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.Arrays;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import uk.ac.ed.inf.aqmaps.Drone.DroneAction;

public class MainDrone extends Drone {

    private ArrayList<Sensor> sensorTour;

    private final Point startingPosition;

    private int currentDestinationIndex;
    private double currentActionRange;

    private boolean[] sensorsVisitedArray;
    private double[] readingsForAllSensors;
    private int[] stepsAfterWhichSensorsWereRead;

    private ArrayList<Point> positionHistory;

    public MainDrone(Point startingPoint, ArrayList<Sensor> tour) {
        super(startingPoint);

        this.startingPosition = this.currentPosition;
        var startingPosAsList = Arrays.asList(startingPosition);
        this.positionHistory = new ArrayList<Point>(startingPosAsList);

        this.sensorTour = tour;
        this.currentDestinationIndex = 0;
        this.sensorsVisitedArray = new boolean[sensorTour.size()];
        this.readingsForAllSensors = new double[sensorTour.size()];
        this.stepsAfterWhichSensorsWereRead = new int[sensorTour.size()];

        if (tour == null) {
            System.out.println(tour);
        }

        if (tour.size() > 0) {
            super.currentDestination = tour.get(0).getPosition();
            currentActionRange = MAX_READ_DISTANCE;
        }
    }

    public void completeTour() {
        while (this.currentDestinationIndex < this.sensorTour.size()) {
            this.currentDestination = this.sensorTour.get(currentDestinationIndex).getPosition();
            var stepsToGetToSensor = this.flyToCurrentDestination();

            /*
             * We can only take one reading per move. Thus, if the drone is already in range
             * of the next sensor, we need to make a waiting move (or two, if staying in
             * range of the next sensor turns out to be impossible).
             */
            if (stepsToGetToSensor == 0) {
                this.park(this.currentActionRange);
            }

            this.downloadReadingsFromSensor(this.sensorTour.get(this.currentDestinationIndex));

            /* Preparing drone for next sensor to be read */
            this.currentDestinationIndex++;
        }

        /* Once all sensors have been read, return to starting position */
        this.currentActionRange = MAX_LANDING_DISTANCE;
        this.currentDestination = this.startingPosition;
        this.flyToCurrentDestination();

        System.out.println("Successfully finished the tour after " + this.stepsMade + " steps!");
    }

    public int flyToCurrentDestination() {

        var moveCountAtStart = this.stepsMade;

        while (!this.isInRangeOfPoint(currentDestination, currentActionRange)) {

            if (this.canGetTowardsDestinationInStraightLine()) {
                var straightPath = new LineSegment(this.currentPosition, this.currentDestination);
                var exactAngle = straightPath.getAngleInDegrees();
                double scaledAngle = exactAngle / ANGLE_GRANULARITY;
                double roundedAngle = (ANGLE_GRANULARITY * (int) Math.rint(scaledAngle) + 360) % 360;

                this.makeMove(roundedAngle);
            } else {
                this.avoidObstacle(this.obstacleInOurWay);
            }

        }

        var moveCountAtEnd = this.stepsMade;

        /*
         * Return the number of steps it takes to get from A to B
         */
        return moveCountAtEnd - moveCountAtStart;

    }

    @Override
    public boolean canGetTowardsDestinationInStraightLine() {

        var shadow = new ShadowDrone(this.currentPosition, this.currentDestination);

        shadow.setPosition(this.currentPosition);
        shadow.setStepsMade(0);

        var answer = shadow.canGetTowardsDestinationInStraightLine();
        this.obstacleInOurWay = shadow.getObstacleInOurWay();

        return answer;
    }

    public void avoidObstacle(Polygon obstacle) {

        var leftShadow = new ShadowDrone(this.currentPosition, this.currentDestination);

        var approxDistanceClockwiseAvoid = leftShadow.distToAvoidObstacleClockwise(obstacle, this.currentActionRange);

        System.out.println("left" + approxDistanceClockwiseAvoid);
        
        var rightShadow = new ShadowDrone(this.currentPosition, this.currentDestination);

        var approxDistanceCounterClockwiseAvoid = rightShadow.distToAvoidObstacleCounterClockwise(obstacle,
                this.currentActionRange);

        System.out.println("right:" + approxDistanceCounterClockwiseAvoid);
        
        ArrayList<Double> anglesToFlyAt;

        if (approxDistanceClockwiseAvoid < approxDistanceCounterClockwiseAvoid) {
            // System.out.println();
            // System.out.println("Chose left rotation to avoid obstacle");
            // System.out.println();
            anglesToFlyAt = leftShadow.getMoveAngleHistory();
        } else {
            // System.out.println();
            // System.out.println("Chose right rotation to avoid obstacle");
            // System.out.println();
            anglesToFlyAt = rightShadow.getMoveAngleHistory();
        }

        for (var angle : anglesToFlyAt) {
            makeMove(angle);
        }
    }

    private void downloadReadingsFromSensor(Sensor sensor) {

        if (isInRangeOfPoint(sensor.getPosition(), MAX_READ_DISTANCE)) {
            System.out.println("Read sensor " + currentDestinationIndex);
            this.sensorsVisitedArray[currentDestinationIndex] = true;
            this.readingsForAllSensors[currentDestinationIndex] = sensor.outputReading();
            this.stepsAfterWhichSensorsWereRead[currentDestinationIndex] = this.stepsMade;

        } else {
            System.out.println("The drone was asked to read a sensor that was actually out of range!");
            System.exit(1);
        }

    }

    protected void makeMove(double angle) {
        
        /*
         * System.out.println(); System.out.println(this.currentDestinationIndex);
         * System.out.println(this.currentPosition);
         * System.out.println(this.currentDestination);
         * System.out.println(this.stepsMade); System.out.println();
         */

        if (!this.canMove(angle)) {
            System.out.println("The main drone was told to make an impossible move.");
            System.exit(1);
        } else if (this.stepsMade >= this.MAX_MOVES) {
            System.out.println("The main drone has run out of battery and has crashed!");
            System.exit(1);
        } else {
            var nextPos = EuclideanUtils.getNextPosition(this.currentPosition, angle, MOVE_DISTANCE);
            this.currentPosition = nextPos;
            this.positionHistory.add(nextPos);
            this.stepsMade++;
        }
    }

    /*
     * This method is needed when we want another class to tell the drone to land at
     * its next destination, even if that is not its starting point, or if it has
     * not completed its tour yet.
     * 
     * In our scenario, that only happens if we use a drone to find the distance
     * between a sensor and the starting point via our TourPlanner class.
     */
    public void setActionRangeToLanding() {
        this.currentActionRange = MAX_LANDING_DISTANCE;
    }

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
    
    public int[] getStepsAfterWhichSensorsWereRead() {
        return this.stepsAfterWhichSensorsWereRead;
    }

}
