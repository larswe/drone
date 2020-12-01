package uk.ac.ed.inf.aqmaps.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mapbox.geojson.Point;

import uk.ac.ed.inf.aqmaps.drone.Drone;
import uk.ac.ed.inf.aqmaps.drone.MainDrone;

public class TourPlanner {

    private int numPoints;
    private int[][] distanceMatrix;
    private List<Point> initialPoints;
    private int[] currentPointPermutation;

    public TourPlanner(List<Point> points) {
        this.distanceMatrix = computeDistanceMatrix(points);
        this.initialPoints = points;
        this.numPoints = points.size();

        /* Initialize the permutation of nodes to be the identity */
        currentPointPermutation = new int[numPoints];
        for (int i = 0; i < numPoints; i++) {
            currentPointPermutation[i] = i;
        }
    }

    /*
     * The last point in the given input list is assumed to be the starting/landing
     * point of the drone.
     */
    public int[] findShortestTour() {

        applyTwoOptAlgorithm();

        /*
         * Lastly we remove the starting point from the permutation of indices since it
         * is implied.
         */
        var shortestSensorTourIndices = new int[numPoints - 1];
        var j = 0;
        for (int i = 0; i < numPoints; i++) {
            if (currentPointPermutation[i] != numPoints - 1) {
                shortestSensorTourIndices[j] = currentPointPermutation[i];
                j++;
            }
        }

        return shortestSensorTourIndices;
    }

    private int[][] computeDistanceMatrix(List<Point> points) {

        var numPoints = points.size();
        var distanceMatrix = new int[numPoints][numPoints];

        /*
         * First, we compute the distance between each sensor pair naively, without
         * considering No-Fly-Zones.
         */
        for (int i = 0; i < numPoints; i++) {
            for (int j = 0; j < numPoints; j++) {
                var pointA = points.get(i);
                var pointB = points.get(j);

                /*
                 * Create a drone which flies from point A to point B. See how many steps it
                 * needs. That is the relevant distance.
                 * 
                 * To this end, we create a mock sensor at point B.
                 */
                var w3w = new What3WordsLocation("a.b.c", "", "", "", "", null, pointB);
                Sensor destinationSensor = new Sensor(0.0f, 0.0, w3w);
                var listContainingDestinationSensor = new ArrayList<Sensor>(Arrays.asList(destinationSensor));

                MainDrone drone = new MainDrone(pointA, listContainingDestinationSensor);

                /*
                 * If the destination is the last point in our list, it is the starting/landing
                 * location and requires a different "action range" - the area around the point
                 * our drone aims at.
                 */
                if (j == numPoints - 1) {
                    drone.setCurrentActionRange(Drone.getMaxLandingDistance());
                }

                var stepsNeeded = drone.flyToCurrentDestination();

                /*
                 * If the two sensors are in fact "in range" of each other, we need to call the
                 * drone's park method, because two sensors cannot be read on the same turn,
                 * thus requiring one or two in between moves.
                 */
                if (stepsNeeded == 0 && i != j) {
                    drone.park();
                }
                distanceMatrix[i][j] = drone.getStepsMade();
                
                System.out.println("i " + i + " , j " + j + " : " + drone.getStepsMade());
            }
        }

        return distanceMatrix;
    }

    private int tourValue(int[] tourIndices) {
        var tourCostCount = 0;

        /*
         * Note : Here we start at the first point of the tour and wrap around at the
         * end to account for the fact that we are dealing with a closed cycle.
         */
        for (int i = 0; i < numPoints; i++) {
            tourCostCount += distanceMatrix[tourIndices[i]][tourIndices[(i + 1) % numPoints]];
        }

        return tourCostCount;
    }

    /*
     * Consider the effect of reversing the segment between the current i-th and
     * j-th points of our tour. If this improves the tour value, commit to the
     * reversal and return true. Otherwise leave the tour unchanged and return
     * false.
     */
    private boolean tryReverse(int i, int j) {

        /*
         * Replace the edges from (i-1) to i and j to (j+1) by the ones from (i+1) to j
         * and i to (j+1). We make the assumption that all other costs stay the same.
         */
        var lastOfStart = currentPointPermutation[(i - 1 + numPoints) % numPoints];
        var firstOfSegment = currentPointPermutation[i];
        var lastOfSegment = currentPointPermutation[j];
        var firstOfEnd = currentPointPermutation[(j + 1) % numPoints];

        var oldCost = distanceMatrix[lastOfStart][firstOfSegment] + distanceMatrix[lastOfSegment][firstOfEnd];
        var newCost = distanceMatrix[lastOfStart][lastOfSegment] + distanceMatrix[firstOfSegment][firstOfEnd];

        if (newCost < oldCost) {
            /* Everything before the reversal stays the same */
            var unchangedStart = new int[i];
            for (int k = 0; k < i; k++) {
                unchangedStart[k] = currentPointPermutation[k];
            }

            /* The reversed part is reversed */
            var reversedPart = new int[j - i + 1];
            for (int k = 0; k < j - i + 1; k++) {
                reversedPart[k] = currentPointPermutation[j - k];
            }

            /* Everything after the reversal stays the same */
            var unchangedEnd = new int[numPoints - j - 1];
            for (int k = 0; k < numPoints - j - 1; k++) {
                unchangedEnd[k] = currentPointPermutation[j + 1 + k];
            }

            /* The new tour consists of the 3 parts we just defined, glued to one another */
            var newTour = new int[numPoints];
            for (int k = 0; k < i; k++) {
                newTour[k] = unchangedStart[k];
            }
            for (int k = 0; k < j - i + 1; k++) {
                newTour[i + k] = reversedPart[k];
            }
            for (int k = 0; k < numPoints - j - 1; k++) {
                newTour[j + 1 + k] = unchangedEnd[k];
            }

            this.currentPointPermutation = newTour;
            return true;
        } else {
            /*
             * If reversing the specified part would not increase the cost, we don't have to
             * bother changing anything.
             */
            return false;
        }

    }

    /* Applies the 2-Opt algorithm to our Traveling Salesman Problem */
    private void applyTwoOptAlgorithm() {
        var improvedTourOnPreviousLoop = true;

        while (improvedTourOnPreviousLoop) {
            improvedTourOnPreviousLoop = false;
            for (int j = 0; j < numPoints - 1; j++) {
                for (int i = 0; i < j; i++) {
                    if (tryReverse(i, j)) {
                        improvedTourOnPreviousLoop = true;
                    }
                }
            }
        }
    }

}