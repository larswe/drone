package uk.ac.ed.inf.aqmaps.application;

import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import uk.ac.ed.inf.aqmaps.drone.MainDrone;

import uk.ac.ed.inf.aqmaps.map.NoFlyZone;
import uk.ac.ed.inf.aqmaps.map.Sensor;
import uk.ac.ed.inf.aqmaps.map.TourPlanner;
import uk.ac.ed.inf.aqmaps.map.TwoDimensionalMapObject;

import java.io.IOException;

/**
 * The main class of this application, responsible for its coordination.
 */
public class App {

    /*
     * The largest legal value of an air pollution reading
     */
    private static final double MAX_READING = 256.0;

    /* The coordinates that define the drone confinement area */
    private static final double MIN_LONGITUDE = -3.192473;
    private static final double MAX_LONGITUDE = -3.184319;
    private static final double MIN_LATITUDE = 55.942617;
    private static final double MAX_LATITUDE = 55.946233;

    /* The confinement area that the drone cannot leave, as a Mapbox Polygon */
    private static TwoDimensionalMapObject confinementArea;
    /* The zones the drone is not allowed to enter */
    private static List<NoFlyZone> noFlyZones;

    static {
        /* Initialising the confinement area from the given boundaries */
        var upperLeftPoint = Point.fromLngLat(MIN_LONGITUDE, MAX_LATITUDE);
        var upperRightPoint = Point.fromLngLat(MAX_LONGITUDE, MAX_LATITUDE);
        var lowerRightPoint = Point.fromLngLat(MAX_LONGITUDE, MIN_LATITUDE);
        var lowerLeftPoint = Point.fromLngLat(MIN_LONGITUDE, MIN_LATITUDE);
        var confinementCoordinates = List.of(upperLeftPoint, upperRightPoint, lowerRightPoint, lowerLeftPoint,
                upperLeftPoint);
        var confinementPolygon = Polygon.fromLngLats(List.of(confinementCoordinates));
        confinementArea = new TwoDimensionalMapObject(confinementPolygon, "Confinement Area");
    }

    /**
     * The main method, responsible for initiating the functionality of this
     * application.
     * 
     * @param args Command line arguments - should contain the date of the relevant
     *             tour, as well as the starting location of our drone, a random
     *             seed and a server port.
     */
    public static void main(String[] args) {

        /* Load/Parse the Input Data */
        var startInfo = InputProcessor.parseInputArguments(args);
        var port = startInfo.getPort();

        var seed = startInfo.getSeed(); // Currently unused but required according to the task specification

        var day = startInfo.getDay();
        var month = startInfo.getMonth();
        var year = startInfo.getYear();
        var droneStartLongitude = startInfo.getDroneStartLongitude();
        var droneStartLatitude = startInfo.getDroneStartLatitude();

        noFlyZones = InputProcessor.loadNoFlyZonesFromServer(startInfo.getPort());

        /* Preparing drone parameters for its tour of the day */
        var droneStartingPoint = Point.fromLngLat(droneStartLongitude, droneStartLatitude);
        var sensors = InputProcessor.getSensorsForDate(day, month, year, port);

        var tourNodes = new ArrayList<Point>();
        for (Sensor s : sensors) {
            System.out.println("Sensor at " + s.getPosition());
            tourNodes.add(s.getPosition());
        }
        tourNodes.add(droneStartingPoint);

        /*
         * Find a suitable tour for the drone to embark on. Disclaimer: Not actually
         * guaranteed to be the shortest tour - that problem is NP-hard!
         */
        var tourPlanner = new TourPlanner(tourNodes);
        var shortestTourIndices = tourPlanner.findShortestTour();
        var shortestTour = new ArrayList<Sensor>();
        for (int i = 0; i < shortestTourIndices.length; i++) {
            shortestTour.add(sensors.get(shortestTourIndices[i]));

            System.out.println("Sensor " + i + " : " + sensors.get(shortestTourIndices[i]).getPosition());
        }

        /* Initialise main drone and send on its tour */
        System.out.println("\nThe main drone embarks on its journey!\n");
        MainDrone mainDrone = new MainDrone(droneStartingPoint, shortestTour);
        mainDrone.completeTour();

        /* Print path & sensor output to GeoJSON file */
        var featCollection = OutputGenerator.generateFeatureCollection(shortestTour, mainDrone);
        OutputGenerator.writeFeatureCollectionToFile(featCollection, day, month, year);

        /* Print flight path to txt file */
        try {
            OutputGenerator.writeFlightPathToFile(shortestTour, mainDrone, day, month, year);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * This getter is needed because OutputGenerator requires this value to compute
     * the pollution tiers and because the attribute itself makes more sense in the
     * App class.
     */
    static double getMaxReading() {
        return MAX_READING;
    }

    /*
     * The following two getters are needed because the Drone class needs to have
     * access to them and because the attributes themselves make slightly more sense
     * in the App class (since they are parameters of the drone only indirectly, but
     * rather of the scenario at hand).
     */
    public static TwoDimensionalMapObject getConfinementArea() {
        return confinementArea;
    }

    public static List<NoFlyZone> getNoFlyZones() {
        return noFlyZones;
    }

}
