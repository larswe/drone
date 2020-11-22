package uk.ac.ed.inf.aqmaps;

import java.util.List;

public class SensorTourPlanner {

    public static List<Sensor> findShortestSensorTour(List<Sensor> sensors) {

        var distanceMatrix = computeDistanceMatrix(sensors);

        return null; // TODO
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
                var positionA = sensors.get(i).getPosition();
                var positionB = sensors.get(j).getPosition();
                distanceMatrix[i][j] = EuclideanUtils.computeDistance(positionA, positionB);
                System.out.println(i + " , " + j + " : " + distanceMatrix[i][j]);
            }
        }
        
        // TODO: Actually, we should just create a drone and let it fly from A to B, see how long it takes. 

        return distanceMatrix;
    }

}
