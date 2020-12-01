package uk.ac.ed.inf.aqmaps.application;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;

import uk.ac.ed.inf.aqmaps.drone.MainDrone;
import uk.ac.ed.inf.aqmaps.map.Sensor;

/**
 * This class contains static methods that can be used to turn the data gathered
 * by our drone into output files.
 */
public class OutputGenerator {

    /*
     * Maps which assign to each "air pollution tier" its associated colour and
     * marker symbols as Strings. Each pollution tier accounts for an equal part of
     * the legal prediction interval (e.g. for our standard values, tier 0 accounts
     * for values from 0 to 31.9999, etc).
     */
    private static Map<Integer, String> pollutionTierToRgb;
    private static Map<Integer, String> pollutionTierToMarkerSymbol;

    static {
        /* Initialising the tier-to-colour map */
        pollutionTierToRgb = new HashMap<Integer, String>();
        pollutionTierToRgb.put(0, "#00ff00");
        pollutionTierToRgb.put(1, "#40ff00");
        pollutionTierToRgb.put(2, "#80ff00");
        pollutionTierToRgb.put(3, "#c0ff00");
        pollutionTierToRgb.put(4, "#ffc000");
        pollutionTierToRgb.put(5, "#ff8000");
        pollutionTierToRgb.put(6, "#ff4000");
        pollutionTierToRgb.put(7, "#ff0000");
        pollutionTierToRgb.put(-1, "#000000");
        pollutionTierToRgb.put(404, "#aaaaaa");

        /* Initialising the tier-to-symbol map */
        pollutionTierToMarkerSymbol = new HashMap<Integer, String>();
        pollutionTierToMarkerSymbol.put(0, "lighthouse");
        pollutionTierToMarkerSymbol.put(1, "lighthouse");
        pollutionTierToMarkerSymbol.put(2, "lighthouse");
        pollutionTierToMarkerSymbol.put(3, "lighthouse");
        pollutionTierToMarkerSymbol.put(4, "danger");
        pollutionTierToMarkerSymbol.put(5, "danger");
        pollutionTierToMarkerSymbol.put(6, "danger");
        pollutionTierToMarkerSymbol.put(7, "danger");
        pollutionTierToMarkerSymbol.put(-1, "cross");
        pollutionTierToMarkerSymbol.put(404, "");
    }

    /**
     * Given the list of relevant sensors and our main drone after it has returned
     * from its tour, this method turns the gathered data, consisting of visited
     * locations and sensor readings, into a Mapbox FeatureCollection.
     * 
     * @param sensors the relevant sensors, assumed to be in the order in which the
     *                drone visited them
     * @param drone   our main drone, assumed to have returned from its tour
     * @return a FeatureCollection corresponding to the gathered data
     */
    static FeatureCollection generateFeatureCollection(List<Sensor> sensors, MainDrone drone) {
        var positionHistory = drone.getPositionHistory();
        var visitedArr = drone.getSensorsVisitedArray();
        var allReadings = drone.getReadingsForAllSensors();

        var listOfFeatures = new ArrayList<Feature>();

        /*
         * Draw a line for each adjacent pair of points in the given position history of
         * the main drone.
         */
        var dronePath = LineString.fromLngLats(positionHistory);
        var dronePathGeometry = (Geometry) dronePath;
        var dronePathFeature = Feature.fromGeometry(dronePathGeometry);
        listOfFeatures.add(dronePathFeature);

        /*
         * Add markers for all sensors
         */
        for (int i = 0; i < visitedArr.length; i++) {
            var sensor = sensors.get(i);
            var marker = sensor.getPosition();
            var markerGeometry = (Geometry) marker;
            var markerFeature = Feature.fromGeometry(markerGeometry);

            int pollutionTier;
            if (!visitedArr[i]) {
                pollutionTier = 404;
            } else if (Double.isNaN(allReadings[i])) {
                pollutionTier = -1;
            } else {
                pollutionTier = computeTierForReading(allReadings[i]);
            }

            markerFeature.addStringProperty("location", sensor.getW3wLocation().toString());
            markerFeature.addStringProperty("rgb-string", pollutionTierToRgb.get(pollutionTier));
            markerFeature.addStringProperty("marker-symbol", pollutionTierToMarkerSymbol.get(pollutionTier));
            markerFeature.addStringProperty("marker-color", pollutionTierToRgb.get(pollutionTier));

            listOfFeatures.add(markerFeature);
        }

        /*
         * Add all features to a collection and return it.
         */
        var featCollection = FeatureCollection.fromFeatures(listOfFeatures);
        return featCollection;
    }

    /**
     * This method computes the "pollution tier" of a given air quality measurement.
     * The tier later decides the colour and symbol corresponding to the according
     * sensor.
     * 
     * @param reading the reading corresponding to a sensor on a given day (already
     *                having taken into account the battery)
     * @return the pollution tier corresponding to the reading (0 for 0-32, 1 for
     *         32-64, ..., 7 for 224-256)
     */
    private static int computeTierForReading(double reading) {
        /*
         * We need the number of pollution tiers that correspond to legal readings -
         * this excludes the tiers -1(Battery low) and 404(Reading missing).
         */
        var numLegalReadingTiers = pollutionTierToRgb.size() - 2;
        var tierSize = App.getMaxReading() / numLegalReadingTiers;

        for (int i = 1; i <= numLegalReadingTiers; i++) {
            if (reading < i * tierSize) {
                return i - 1;
            }
        }

        /*
         * If the reading is greater than the expected maximum, we simply return the
         * value corresponding to the "missing" tier
         */
        return 404;
    }

    /**
     * This method generates a String in JSON format for a given Mapbox GeoJSON
     * FeatureCollection and writes it to a file with the specified name.
     * 
     * @param featCollection a FeatureCollection corresponding to the sensor
     *                       information and drone flight path
     * @param fileName       the intended name of the output file
     */
    static void writeFeatureCollectionToFile(FeatureCollection featCollection, int day, int month, int year) {
        var output = featCollection.toJson();

        var fileName = generateGeoJSONFileName(day, month, year);

        /*
         * We catch possible IO Exceptions immediately, without throwing them back to
         * our main method first. This differs from the approach I chose in the
         * readGrid() method, where try-catch blocks would decrease readability.
         */
        try {
            var fileWriter = new FileWriter(fileName);
            var printWriter = new PrintWriter(fileWriter);
            printWriter.print(output);
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * This helper method generates the name of the GeoJSON file corresponding to
     * the drone's output, in the appropriate format.
     * 
     * @param day   the day of month of the drone's tour
     * @param month the month of the drone's tour
     * @param year  the year of the drone's tour
     * 
     * @return the file name of the output .geojson file
     */
    private static String generateGeoJSONFileName(int day, int month, int year) {
        var stringBuilder = new StringBuilder();
        stringBuilder.append("readings");
        stringBuilder.append("-");
        stringBuilder.append(String.format("%02d", day));
        stringBuilder.append("-");
        stringBuilder.append(String.format("%02d", month));
        stringBuilder.append("-");
        stringBuilder.append(String.format("%04d", year));
        stringBuilder.append(".geojson");
        return stringBuilder.toString();
    }

    /**
     * This method writes the "move history" of our main drone (in the required
     * format) to a .txt output file, after the drone has returned from its tour.
     * 
     * @param sensors the relevant sensors, assumed to be in the order in which the
     *                drone visited them
     * @param drone   our main drone, assumed to have returned from its tour
     * @param day     the day of month of the drone's tour
     * @param month   the month of the drone's tour
     * @param year    the year of the drone's tour
     * 
     * @throws IOException
     */
    static void writeFlightPathToFile(List<Sensor> sensors, MainDrone drone, int day, int month, int year)
            throws IOException {

        /*
         * Generate the appropriate file name and get a PrintWriter object. Note: A
         * PrintWriter isn't strictly necessary but may prove useful if we ever decide
         * to e.g. change the (currently slightly unhandy) format of the floating point
         * numbers in the output.
         */
        var fileName = String.format("flightpath-%02d-%02d-%4d.txt", day, month, year);
        var fileWriter = new FileWriter(fileName);
        var printWriter = new PrintWriter(fileWriter);

        /* Get the relevant data gathered by the drone */
        var positionHistory = drone.getPositionHistory();
        var angleHistory = drone.getMoveAngleHistory();
        var sensorReadHistory = drone.getSensorReadHistory();
        var allReadings = drone.getReadingsForAllSensors();

        var numMoves = positionHistory.size() - 1;
        var numSensors = allReadings.length;

        if (numMoves != angleHistory.size() || numMoves != sensorReadHistory.size()) {
            System.out.println("Critical failure while writing flight path to file: " + "Inconsistent number of moves");
            System.exit(1);
        }

        /* Add a corresponding line for each move our drone made. */ 
        for (int i = 1; i <= numMoves; i++) {
            var dronePosBefore = positionHistory.get(i - 1);
            var angleOfMove = (int) Math.round(angleHistory.get(i - 1));
            var dronePosAfter = positionHistory.get(i);
            var sensorW3wLoc = sensorReadHistory.get(i - 1);

            var stringBuilder = new StringBuilder();

            stringBuilder.append(i);
            stringBuilder.append(",");
            stringBuilder.append(dronePosBefore.longitude());
            stringBuilder.append(",");
            stringBuilder.append(dronePosBefore.latitude());
            stringBuilder.append(",");
            stringBuilder.append(angleOfMove);
            stringBuilder.append(",");
            stringBuilder.append(dronePosAfter.longitude());
            stringBuilder.append(",");
            stringBuilder.append(dronePosAfter.latitude());
            stringBuilder.append(",");
            stringBuilder.append(sensorW3wLoc);

            printWriter.println(stringBuilder.toString());
        }

        printWriter.close();
    }

}
