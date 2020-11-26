package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SensorTourPlanner {

    public static ArrayList<Sensor> findShortestSensorTour(ArrayList<Sensor> sensors) {

        var distanceMatrix = computeDistanceMatrix(sensors);

        return sensors; // TODO
    }

    private static double[][] computeDistanceMatrix(List<Sensor> sensors) {

        var numSensors = sensors.size();
        var distanceMatrix = new double[numSensors][numSensors];

        /*
         * First, we compute the distance between each sensor pair naively, without
         * considering No-Fly-Zones.
         */
        for (int i = 0; i < numSensors; i++) {
            for (int j = 0; j < numSensors; j++) {
                var pointA = sensors.get(i).getPosition();
                var sensorB = sensors.get(j);
                
                /*
                 * Create a drone which flies from sensor A to sensor B.
                 * See how many steps it needs. 
                 * That is the relevant distance. 
                 */
                var listContainingSensorB = new ArrayList<Sensor>(Arrays.asList(sensorB));
                MainDrone drone = new MainDrone(pointA.longitude(), pointA.latitude(), listContainingSensorB);
                drone.flyToCurrentDestination();
                
                distanceMatrix[i][j] = drone.stepsMade;
                System.out.println("i " + i + " , j " + j + " : " + drone.stepsMade);
            }
        }
        
        // TODO: Actually, we should just create a drone and let it fly from A to B, see how long it takes. 

        return distanceMatrix;
    }

}
