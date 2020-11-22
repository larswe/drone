package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import uk.ac.ed.inf.aqmaps.jsonstubs.JsonSensorStub;
import uk.ac.ed.inf.aqmaps.jsonstubs.JsonWhat3WordsStub;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;

public class App {

    /* The smallest and largest legal values of an air pollution prediction */
    private static final int MIN_PREDICTION = 0;
    private static final int MAX_PREDICTION = 255;

    /* The coordinates that define the drone confinement area */
    private static final double MIN_LONGITUDE = -3.192473;
    private static final double MAX_LONGITUDE = -3.184319;
    private static final double MIN_LATITUDE = 55.942617;
    private static final double MAX_LATITUDE = 55.946233;

    private static Polygon confinementArea;
    private static int port, seed;

    /*
     * A map which assigns to each "air pollution tier" its associated colour as an
     * RGB String. Each pollution tier accounts for an equal part of the legal
     * prediction interval (e.g. for our standard values, tier 0 accounts for values
     * from 0 to 31, etc).
     */
    private static Map<Integer, Marker> pollutionTierToRgb;
    private static List<NoFlyZone> noFlyZones;
    private static List<Sensor> sensorsToBeReadToday;

    static {
        pollutionTierToRgb = new HashMap<>();
        pollutionTierToRgb.put(0, new Marker("#00ff00", Symbol.LIGHTHOUSE));
        pollutionTierToRgb.put(1, new Marker("#40ff00", Symbol.LIGHTHOUSE));
        pollutionTierToRgb.put(2, new Marker("#80ff00", Symbol.LIGHTHOUSE));
        pollutionTierToRgb.put(3, new Marker("#c0ff00", Symbol.LIGHTHOUSE));
        pollutionTierToRgb.put(4, new Marker("#ffc000", Symbol.DANGER));
        pollutionTierToRgb.put(5, new Marker("#ff8000", Symbol.DANGER));
        pollutionTierToRgb.put(6, new Marker("#ff4000", Symbol.DANGER));
        pollutionTierToRgb.put(7, new Marker("#ff0000", Symbol.DANGER));
        pollutionTierToRgb.put(-1, new Marker("#000000", Symbol.CROSS));
        pollutionTierToRgb.put(404, new Marker("#aaaaaa", Symbol.NONE));

        var upperLeftPoint = Point.fromLngLat(MIN_LONGITUDE, MAX_LATITUDE);
        var upperRightPoint = Point.fromLngLat(MAX_LONGITUDE, MAX_LATITUDE);
        var lowerRightPoint = Point.fromLngLat(MAX_LONGITUDE, MIN_LATITUDE);
        var lowerLeftPoint = Point.fromLngLat(MIN_LONGITUDE, MIN_LATITUDE);
        var confinementCoordinates = List.of(upperLeftPoint, upperRightPoint, lowerRightPoint, lowerLeftPoint,
                upperLeftPoint);
        confinementArea = Polygon.fromLngLats(List.of(confinementCoordinates));
    }

    public static void main(String[] args) {

        // TODO: Load Data
        var startInfo = parseInputArguments(args);
        port = startInfo.getPort();
        seed = startInfo.getSeed();

        noFlyZones = loadNoFlyZonesFromServer(startInfo.getPort());
        
        var sensorStubs = loadSensorStubsForDateFromServer(startInfo.getDay(), startInfo.getMonth(),
                startInfo.getYear());

        for (var s : sensorStubs) {
            System.out.println(s.getReading());
        }

        var sensors = processSensorStubs(sensorStubs);

        for (Sensor s : sensors) {
            System.out.println(s.getPosition());
        }

        // TODO: Find best path
        SensorTourPlanner.findShortestSensorTour(sensors);

        // TODO: Actually make drone fly
        Drone drone = new Drone(startInfo.getDroneStartLongitude(), startInfo.getDroneStartLatitude(), null);

        // TODO: Print output to file

    }

    private static AppStartInfo parseInputArguments(String[] args) {
        var day = Integer.parseInt(args[0]);
        var month = Integer.parseInt(args[1]);
        var year = Integer.parseInt(args[2]);
        var latitude = Double.parseDouble(args[3]);
        var longitude = Double.parseDouble(args[4]);
        var seed = Integer.parseInt(args[5]);
        var port = Integer.parseInt(args[6]);

        return new AppStartInfo(day, month, year, latitude, longitude, seed, port);
    }

    private static ArrayList<NoFlyZone> loadNoFlyZonesFromServer(int port) {
        /* Load Geo-JSON file from server and extract FeatureCollection */
        var jsonNoFlyZonesString = WebServerFileFetcher.getBuildingsGeojsonFromServer(port);
        var noFlyZonesFeatCol = FeatureCollection.fromJson(jsonNoFlyZonesString);

        /*
         * Instantiate NoFlyZone objects from FeatureCollection and add to static list
         */
        var noFlyZones = new ArrayList<NoFlyZone>();
        for (Feature feat : noFlyZonesFeatCol.features()) {
            NoFlyZone building = new NoFlyZone(feat);
            noFlyZones.add(building);
        }

        return noFlyZones;
    }

    private static ArrayList<JsonSensorStub> loadSensorStubsForDateFromServer(int day, int month, int year) {
        var jsonSensorsString = WebServerFileFetcher.getSensorsGeojsonFromServer(day, month, year, port);

        /*
         * We deserialize the String into an object of type ArrayList<JsonSensorStub>,
         * using reflection.
         */
        Type listType = new TypeToken<ArrayList<JsonSensorStub>>() {
        }.getType();
        ArrayList<JsonSensorStub> sensorStubs = new Gson().fromJson(jsonSensorsString, listType);

        return sensorStubs;
    }

    private static ArrayList<Sensor> processSensorStubs(ArrayList<JsonSensorStub> sensorStubs) {

        var sensors = new ArrayList<Sensor>();

        for (var sensorStub : sensorStubs) {

            var battery = Double.parseDouble(sensorStub.getBattery());

            var readingString = sensorStub.getReading();

            double readingDouble;
            /*
             * There is an argument to be made against using try-catch-blocks as
             * "logical operators". In this case however, I found this approach to
             * converting the reading String to double much more readable than e.g. checking
             * the String against a regex to see if it's a floating point number.
             */
            try {
                readingDouble = Double.parseDouble(readingString);
            } catch (NumberFormatException e) {
                readingDouble = Double.NaN;
            }

            var w3wLocation = processW3wString(sensorStub.getLocation());

            var sensor = new Sensor(battery, readingDouble, w3wLocation);
            sensors.add(sensor);
        }

        return sensors;

    }

    private static What3WordsLocation processW3wString(String w3wString) {
        var w3wParts = w3wString.split("\\.");
        assert (w3wParts.length == 3);
        /*
         * We split the w3w identifier into its 3 parts before fetching the according
         * information from the server.
         */
        var first = w3wParts[0];
        var second = w3wParts[1];
        var third = w3wParts[2];
        var jsonW3wString = WebServerFileFetcher.getW3wJsonFromServer(first, second, third, port);

        // TODO: Turn into w3wStub, then proper object. Then make proper sensor object.
        var w3wStub = new Gson().fromJson(jsonW3wString, JsonWhat3WordsStub.class);

        var w3wLocation = processW3wStub(w3wStub);
        return w3wLocation;
    }

    private static What3WordsLocation processW3wStub(JsonWhat3WordsStub stub) {

        var northEastLng = stub.getSquare().getNortheast().getLng();
        var northEastLat = stub.getSquare().getNortheast().getLat();
        var southWestLng = stub.getSquare().getSouthwest().getLng();
        var southWestLat = stub.getSquare().getSouthwest().getLat();

        var upperLeftPoint = Point.fromLngLat(southWestLng, northEastLat);
        var upperRightPoint = Point.fromLngLat(northEastLng, northEastLat);
        var lowerRightPoint = Point.fromLngLat(northEastLng, southWestLat);
        var lowerLeftPoint = Point.fromLngLat(southWestLng, southWestLat);
        var squareCoordinates = List.of(upperLeftPoint, upperRightPoint, lowerRightPoint, lowerLeftPoint,
                upperLeftPoint);

        var square = Polygon.fromLngLats(List.of(squareCoordinates));

        var longitude = stub.getCoordinates().getLng();
        var latitude = stub.getCoordinates().getLat();
        var position = Point.fromLngLat(longitude, latitude);

        var words = stub.getWords();
        var country = stub.getCountry();
        var language = stub.getLanguage();
        var map = stub.getMap();
        var nearestPlace = stub.getNearestPlace();

        var w3wLocation = new What3WordsLocation(words, country, language, map, nearestPlace, square, position);
        return w3wLocation;
    }

    public static Polygon getConfinementArea() {
        return confinementArea;
    }

    public static List<Sensor> getSensorsToBeReadToday() {
        return sensorsToBeReadToday;
    }

    public static void setSensorsToBeReadToday(List<Sensor> sensors) {
        sensorsToBeReadToday = sensors;
    }

    public static List<NoFlyZone> getNoFlyZones() {
        return noFlyZones;
    }

}
