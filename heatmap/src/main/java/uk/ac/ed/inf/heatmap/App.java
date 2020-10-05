package uk.ac.ed.inf.heatmap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

/**
 * This Heatmap Generator program implements an application that takes in a
 * 10x10 grid of predicted air pollution readings on George Square, generates a
 * heat map representing the predictions, before saving it as a .geojson file.
 * 
 * @author Lars Werne
 * @version 0.0.1-SNAPSHOT
 */
public class App {

    /* The expected number of rows and columns in the preparation grid */
    private static final int NUM_ROWS = 10;
    private static final int NUM_COLS = 10;

    /* The smallest and largest legal values of an air pollution prediction */
    private static final int MIN_PREDICTION = 0;
    private static final int MAX_PREDICTION = 255;

    /* The coordinates that define the drone confinement area */
    private static final double MIN_LONGITUDE = -3.192473;
    private static final double MAX_LONGITUDE = -3.184319;
    private static final double MIN_LATITUDE = 55.942617;
    private static final double MAX_LATITUDE = 55.946233;

    /* The destined file name of the output .geojson file */
    private static final String TARGET_JSON_FILE_NAME = "heatmap.geosjon";

    /*
     * A map which assigns to each "air pollution tier" its associated colour as an
     * RGB String. Each pollution tier accounts for an equal part of the legal
     * prediction interval (e.g. for our standard values, tier 0 accounts for values
     * from 0 to 31, etc).
     */
    private static Map<Integer, String> pollutionTierToRgb;
    static {
        pollutionTierToRgb = new HashMap<>();
        pollutionTierToRgb.put(0, "#00ff00");
        pollutionTierToRgb.put(1, "#40ff00");
        pollutionTierToRgb.put(2, "#80ff00");
        pollutionTierToRgb.put(3, "#c0ff00");
        pollutionTierToRgb.put(4, "#ffc000");
        pollutionTierToRgb.put(5, "#ff8000");
        pollutionTierToRgb.put(6, "#ff4000");
        pollutionTierToRgb.put(7, "#ff0000");
    }

    /**
     * The main method, responsible for the functionality of this class. If more or
     * less than one argument is provided, the program exits, "doing nothing".
     * 
     * @param args Command line arguments - should contain only the name of the
     *             prediction file
     */
    public static void main(String[] args) {

        if (args.length != 1) {
            exitDueToIllegalInput("The application expected 1 argument but received " + args.length);
        }

        /*
         * An I/O Error might occur while trying to read the specified prediction file.
         * If that's the case, we simply log the exception and exit.
         */
        int[][] grid = null;
        try {
            grid = readGrid(args[0]);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        FeatureCollection fc = generateFeatureCollection(grid);
        writeFeatureCollectionToFile(fc, TARGET_JSON_FILE_NAME);
    }

    /**
     * This method reads a file containing predicted air pollution readings for each
     * area of the drone confinement area, and returns it as a 2-dimensional array
     * of integers. This fails if the specified file cannot be accessed, if the
     * specified grid does not have the right dimensions, or if it contains a
     * prediction which does not lie in the allowed value range.
     * 
     * @param fileName the name of the prediction file
     * @return a grid of predicted integer air pollution readings
     * @throws IOException if an I/O error occurs while reading the file
     */
    private static int[][] readGrid(String fileName) throws IOException {

        var file = new File(fileName);
        var reader = new BufferedReader(new FileReader(file));

        var grid = new int[NUM_ROWS][NUM_COLS];
        String currentLine = reader.readLine(); // The line we are currently processing.
        int rowIndex = 0; // Index of the row of our grid which we are currently filling in.

        /*
         * Each line of the input is read and processed, until our BufferedReader has
         * reached the end of the file.
         */
        while (currentLine != null) {

            if (rowIndex >= NUM_ROWS) {
                exitDueToIllegalInput("The input file contains more than " + NUM_ROWS + " lines!");
            }

            String[] currentLineArray = currentLine.split(","); // We split the current row into its integer components.

            if (currentLineArray.length != NUM_COLS) {
                exitDueToIllegalInput("Line " + (rowIndex + 1) + " does not contain " + NUM_COLS + " predictions!");
            }

            for (int j = 0; j < NUM_COLS; j++) {

                int x = Integer.parseInt(currentLineArray[j]); // We convert each prediction to type int.
                if (x < MIN_PREDICTION || x > MAX_PREDICTION) {
                    exitDueToIllegalInput(
                            "All predictions must lie between " + MIN_PREDICTION + " and " + MAX_PREDICTION);
                }
                grid[rowIndex][j] = x;
            }

            /*
             * We prepare for the next iteration, by incrementing the row index and reading
             * the next line.
             */
            rowIndex++;
            currentLine = reader.readLine();
        }

        if (rowIndex < NUM_ROWS) {
            exitDueToIllegalInput("The input file contains less than " + NUM_ROWS + " lines of predictions!");
        }

        reader.close(); // We close the stream, releasing any system resources associated with it.
        return grid;
    }

    /**
     * A helper method, which is called when the program was provided with an
     * illegal input, and which makes the program exit with the given error message.
     * 
     * @param errorMessage a message describing why the user's input is illegal
     */
    private static void exitDueToIllegalInput(String errorMessage) {

        var e = new IllegalArgumentException(errorMessage);
        e.printStackTrace();
        System.exit(2);
    }

    /**
     * This method accepts a 2-dimensional array of predicted air pollution
     * readings, turning it into a Mapbox GeoJSON Feature Collection consisting of
     * one rectangle for each area of the drone confinement area. Each rectangle is
     * coloured according to its predicted air pollution.
     * 
     * @param grid the grid of air pollution predictions as integer values
     * @return the feature collection corresponding to the heatmap to be generated
     */
    private static FeatureCollection generateFeatureCollection(int[][] grid) {

        var listOfRectFeatures = new ArrayList<Feature>(); // Using an array of fixed size may be slightly more
                                                           // performant, but this approach was chosen mainly for
                                                           // readability.

        double rectWidth = (MAX_LONGITUDE - MIN_LONGITUDE) / NUM_COLS;
        double rectHeight = (MAX_LATITUDE - MIN_LATITUDE) / NUM_ROWS;

        /*
         * currentLong and currentLat stand for the coordinates of the upper left corner
         * of the rectangle we are currently wanting to construct. We start in the upper
         * left corner of our grid, corresponding to minimum longitude and maximum
         * latitude. We then move through our grid, row for row, from left to right.
         */
        double currentLong, currentLat;

        for (int i = 0; i < NUM_ROWS; i++) {
            for (int j = 0; j < NUM_COLS; j++) {
                currentLat = MAX_LATITUDE - i * rectHeight; // As we move down in our grid, the latitude decreases.
                currentLong = MIN_LONGITUDE + j * rectWidth; // As we move right in our grid, the longitude increases.

                /*
                 * Using a "pipeline" approach, we transform the coordinates and dimensions of
                 * the current rectangle into a Mapbox Feature instance.
                 */
                List<Point> rectCoordinates = getRectCoordinates(currentLong, currentLat, rectWidth, rectHeight);
                Polygon rectPolygon = Polygon.fromLngLats(List.of(rectCoordinates));
                Geometry rectGeometry = (Geometry) rectPolygon;
                Feature rectFeature = Feature.fromGeometry(rectGeometry);

                /*
                 * We assign the appropriate colour properties to the rectangle before adding it
                 * to our list of Feature instances.
                 */
                String rectRgbString = getRgbStringForPrediction(grid[i][j]);
                rectFeature.addNumberProperty("fill-opacity", 0.75);
                rectFeature.addStringProperty("rgb-string", rectRgbString);
                rectFeature.addStringProperty("fill", rectRgbString);
                listOfRectFeatures.add(rectFeature);
            }
        }

        return FeatureCollection.fromFeatures(listOfRectFeatures);
    }

    /**
     * A helper method, which computes the "pollution tier" for a given air
     * pollution prediction, and returns the colour which corresponds to the tier.
     * 
     * @param prediction an air pollution prediction for a single area
     * @return the RGB string of the colour corresponding to the input
     */
    private static String getRgbStringForPrediction(int prediction) {

        int numTiers = pollutionTierToRgb.size();

        /*
         * The following formula works out nicely if the number of tiers divides the
         * max. prediction value plus 1, and if predictions start at 0. Under these
         * assumptions, the prediction range is split into (numTiers) equal parts and
         * the tier corresponds to which of these intervals the prediction falls into.
         * -> E.g. if t=8 and (m+1)=256, x reaches the highest tier 7 iff x/256>=7/8,
         * i.e. iff x>=224
         */
        int pollutionTier = (numTiers * prediction) / (MAX_PREDICTION + 1);

        /*
         * We query the relevant dictionary and return the appropriate colour for the
         * previously computed tier.
         */
        return pollutionTierToRgb.get(pollutionTier);
    }

    /**
     * A helper method, which produces a list of Points from which we can construct
     * a rectangle as a Mapbox GeoJSON Polygon. To do so, it requires the longitude
     * and latitude of the upper left corner of the desired rectangle, as well as
     * its dimensions. The returned list contains 5 points, the first and last of
     * which are identical. Mapbox requires this to generate a Polygon object.
     * 
     * @param upperLeftLong the longitude of the upper left corner of the rectangle
     * @param upperLeftLat  the latitude of the upper left corner of the rectangle
     * @param width         the width of the desired rectangle
     * @param height        the height of the desired rectangle
     * @return a list of Points, denoting the corners of a rectangle
     */
    private static List<Point> getRectCoordinates(double upperLeftLong, double upperLeftLat, double width,
            double height) {

        /*
         * Note that the longitude increases from West to East, but the latitude
         * decreases from North to South.
         */
        Point upperLeftPoint = Point.fromLngLat(upperLeftLong, upperLeftLat);
        Point upperRightPoint = Point.fromLngLat(upperLeftLong + width, upperLeftLat);
        Point lowerRightPoint = Point.fromLngLat(upperLeftLong + width, upperLeftLat - height);
        Point lowerLeftPoint = Point.fromLngLat(upperLeftLong, upperLeftLat - height);

        return List.of(upperLeftPoint, upperRightPoint, lowerRightPoint, lowerLeftPoint, upperLeftPoint);
    }

    /**
     * This method generates a String in JSON format for a given Mapbox GeoJSON
     * FeatureCollection and writes it to a file with the specified name.
     * 
     * @param featCollection a FeatureCollection corresponding to the generated
     *                       heatmap
     * @param fileName       the intended name of the output file
     */
    private static void writeFeatureCollectionToFile(FeatureCollection featCollection, String fileName) {
        String output = featCollection.toJson();

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
}