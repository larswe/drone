package uk.ac.ed.inf.aqmaps.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mapbox.geojson.Point;

import uk.ac.ed.inf.aqmaps.drone.Drone;
import uk.ac.ed.inf.aqmaps.drone.MainDrone;

/**
 * The TourPlanner class solves the Travelling Salesman Problem that is induced
 * by a list of sensors that needs to be visited in an order that is as
 * efficient as possible.
 */
public class TourPlanner {

    /* The number of nodes of the tour */
    private int numPoints;

    /*
     * The distance matrix, where entry (i,j) is an estimation of the number of
     * steps a drone needs from sensor i to sensor j.
     */
    private int[][] distanceMatrix;

    /*
     * We use this attribute to keep track of the best tour found so far. Its (i-1)
     * entry is the initial index of the node that is visited i-th.
     */
    private int[] currentPointPermutation;

    /**
     * The constructor of the TourPlanner class. I decided to initialise a
     * TourPlanner instance for a list of nodes to be "sorted", and view the
     * distance matrix that defines the problem as an attribute of the planner, as
     * opposed to e.g. using a static approach.
     * 
     * @param points the list of points that leads to the Travelling Salesman
     *               Problem
     */
    public TourPlanner(List<Point> points) {
        this.distanceMatrix = computeDistanceMatrix(points);
        this.numPoints = points.size();

        /* Initialise the permutation of nodes to be the identity */
        this.currentPointPermutation = new int[numPoints];
        for (int i = 0; i < numPoints; i++) {
            currentPointPermutation[i] = i;
        }
    }

    /**
     * This method computes the order in which the list of points described by the
     * distance matrix of this object shall be visited by our drone.
     * 
     * The last point in the given input list is assumed to be the starting/landing
     * point of the drone, and ultimately removed. That is because it is simply not
     * needed in our scenario - our drone will know when to return to its starting
     * point.
     * 
     * Note: Admittedly, the name is slightly misleading, because a shorter tour may
     * exist. But since this is the shortest tour that our algorithm finds, I deemed
     * this name to be appropriate nevertheless.
     * 
     * @return the array of indices that defines the tour across the nodes that is
     *         deemed shortest
     */
    public int[] findShortestTour() {

        applyTwoOptAlgorithm();

        /*
         * Lastly we remove the starting point from the tour since it is implied.
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

    /**
     * An instance of the Travelling Salesman Problem can be identified by its
     * distance matrix. This method computes that matrix by simulating how long it
     * will take our main drone to get from each node i to each node j, pairwise.
     * 
     * @param points the initial list of nodes, where the last point is assumed to
     *               be the start/end point of the tour
     * 
     * @return the distance matrix for the Travelling Salesman Problem we are facing
     */
    private int[][] computeDistanceMatrix(List<Point> points) {

        var numPoints = points.size();
        var distanceMatrix = new int[numPoints][numPoints];

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
                var w3w = new What3WordsLocation("a.b.c", null, pointB);
                var destinationSensor = new Sensor(0.0f, 0.0, w3w);
                var listContainingDestinationSensor = new ArrayList<Sensor>(Arrays.asList(destinationSensor));

                var drone = new MainDrone(pointA, listContainingDestinationSensor, false);

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
            }
        }

        return distanceMatrix;
    }

    /**
     * This method applies the 2-opt algorithm to our tour.
     * 
     * This algorithm was chosen because it is rather simple, yields decent results
     * and is very fast (compared to more complex algorithms, such as
     * Lin-Kernighan).
     * 
     * We consider each pair of indices and see if reverting the path between the
     * two nodes yields a better cost overall. We stop as soon as no reversion
     * improves the tour cost.
     */
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

    /**
     * This method considers the effect of reversing the segment between the points
     * of our tour that currently have index i and j. If this lowers the tour cost,
     * commit to the reversal and return true. Otherwise leave the tour unchanged
     * and return false.
     * 
     * @param i the index at which the attempted reversal starts
     * @param j the index at which the attempted reversal ends
     * 
     * @return whether the reversal leads to a better tour cost
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

            currentPointPermutation = newTour;
            return true;
        } else {
            /*
             * If reversing the specified part would not increase the cost, we don't have to
             * bother changing anything.
             */
            return false;
        }

    }

}