package uk.ac.ed.inf.aqmaps.application;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

import uk.ac.ed.inf.aqmaps.map.NoFlyZone;
import uk.ac.ed.inf.aqmaps.map.Sensor;
import uk.ac.ed.inf.aqmaps.map.What3WordsLocation;
import uk.ac.ed.inf.aqmaps.webserver.JsonSensorStub;
import uk.ac.ed.inf.aqmaps.webserver.JsonWhat3WordsStub;
import uk.ac.ed.inf.aqmaps.webserver.WebServerFileFetcher;

/**
 * This class contains static methods used by the App class to process the given
 * inputs, whether they are provided directly by the user, or fetched from the
 * file server.
 * 
 * Note: It is not responsible for fetching any files from the web server - it
 * merely extracts the relevant information from them, turning it into
 * appropriate objects.
 */
public class InputProcessor {

    /**
     * This method turns the command line arguments given by the user into an
     * instance of the AppStartInfo class, to be used throughout the runtime of the
     * application.
     * 
     * @param args Command line arguments, consisting of a date, a starting position
     *             for the drone, a random seed and a port.
     */
    static AppStartInfo parseInputArguments(String[] args) {
        var day = Integer.parseInt(args[0]);
        var month = Integer.parseInt(args[1]);
        var year = Integer.parseInt(args[2]);
        var latitude = Double.parseDouble(args[3]);
        var longitude = Double.parseDouble(args[4]);
        var seed = Integer.parseInt(args[5]);
        var port = Integer.parseInt(args[6]);

        return new AppStartInfo(day, month, year, latitude, longitude, seed, port);
    }

    /**
     * This method generates the list of sensors to be visited on a given day.
     * 
     * It does so with the help of the WebServerFileFetcher class, asking it to
     * fetch the sensor information from the web server before processing it.
     * 
     * @param day   the day of month of the drone's tour
     * @param month the month of the drone's tour
     * @param year  the year of the drone's tour
     * @param port  the port that shall be used to connect to the file server
     * 
     * @return a list of stubs that represent the relevant sensors
     */
    static ArrayList<Sensor> getSensorsForDate(int day, int month, int year, int port) {
        var jsonSensorsString = WebServerFileFetcher.getSensorsGeojsonFromServer(day, month, year, port);

        /*
         * We deserialize the String into an object of type ArrayList<JsonSensorStub>,
         * using reflection.
         */
        Type listType = new TypeToken<ArrayList<JsonSensorStub>>() {
        }.getType();
        ArrayList<JsonSensorStub> sensorStubs = new Gson().fromJson(jsonSensorsString, listType);

        var sensors = new ArrayList<Sensor>();

        for (var sensorStub : sensorStubs) {

            var battery = Float.parseFloat(sensorStub.getBattery());

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

            var w3wLocation = processW3wString(sensorStub.getLocation(), port);

            var sensor = new Sensor(battery, readingDouble, w3wLocation);
            sensors.add(sensor);
        }

        return sensors;
    }

    /**
     * This method turns the name of a What3Words Location into a proper instance of
     * the corresponding class, so that it can be used as attribute of a Sensor
     * object. It does so by asking the WebServerFileFetcher to return the relevant
     * JSON String from the server.
     * 
     * @param w3wString the name of the relevant What3Words Location
     * @param port      the port that shall be used to connect to the file server,
     *                  to retrieve the information corresponding to the location
     * @return an instance of the What3WordsLocation class
     */
    private static What3WordsLocation processW3wString(String w3wString, int port) {
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

        /* Turn the textual information into w3wStub, then "proper object" */
        var w3wStub = new Gson().fromJson(jsonW3wString, JsonWhat3WordsStub.class);

        var w3wLocation = InputProcessor.processW3wStub(w3wStub);
        return w3wLocation;
    }

    /**
     * This method turns a JSON stub representing a What 3 Words Location into a
     * proper instance of the according class.
     * 
     * @param stub a JSON stub representing a What 3 Words Location
     * 
     * @return the What3WordsLocation instance that was initially represented by the
     *         stub
     */
    static What3WordsLocation processW3wStub(JsonWhat3WordsStub stub) {

        /* Get coordinates of square corresponding to W3W Location */
        var northEastLng = stub.getSquare().getNortheast().getLng();
        var northEastLat = stub.getSquare().getNortheast().getLat();
        var southWestLng = stub.getSquare().getSouthwest().getLng();
        var southWestLat = stub.getSquare().getSouthwest().getLat();

        /* Turn the coordinates into a Mapbox Polygon */
        var upperLeftPoint = Point.fromLngLat(southWestLng, northEastLat);
        var upperRightPoint = Point.fromLngLat(northEastLng, northEastLat);
        var lowerRightPoint = Point.fromLngLat(northEastLng, southWestLat);
        var lowerLeftPoint = Point.fromLngLat(southWestLng, southWestLat);
        var squareCoordinates = List.of(upperLeftPoint, upperRightPoint, lowerRightPoint, lowerLeftPoint,
                upperLeftPoint);
        var square = Polygon.fromLngLats(List.of(squareCoordinates));

        /* Extract the specific location of any sensor that may be in this area */
        var longitude = stub.getCoordinates().getLng();
        var latitude = stub.getCoordinates().getLat();
        var position = Point.fromLngLat(longitude, latitude);

        /* Extract the remaining information from the JSON stub */
        var words = stub.getWords();
        var country = stub.getCountry();
        var language = stub.getLanguage();
        var map = stub.getMap();
        var nearestPlace = stub.getNearestPlace();

        var w3wLocation = new What3WordsLocation(words, country, language, map, nearestPlace, square, position);
        return w3wLocation;
    }

    /**
     * This method generates a list of No-Fly-Zones that the drone needs to take
     * care of.
     * 
     * It does so by asking the WebServerFileFetcher to load the relevant JSON file,
     * before turning it into a Mapbox FeatureCollection and then a list of
     * NoFlyZone objects with the relevant properties.
     * 
     * @param port the port that shall be used to connect to the file server
     * @return a list of all no fly zones declared on the file server
     */
    static ArrayList<NoFlyZone> loadNoFlyZonesFromServer(int port) {
        /* Load Geo-JSON file from server and extract FeatureCollection */
        var jsonNoFlyZonesString = WebServerFileFetcher.getBuildingsGeojsonFromServer(port);
        var noFlyZonesFeatCol = FeatureCollection.fromJson(jsonNoFlyZonesString);

        /*
         * Instantiate NoFlyZone objects from FeatureCollection and add to list
         */
        var noFlyZones = new ArrayList<NoFlyZone>();
        for (Feature feat : noFlyZonesFeatCol.features()) {
            NoFlyZone building = new NoFlyZone(feat);
            noFlyZones.add(building);
        }

        return noFlyZones;
    }

}
