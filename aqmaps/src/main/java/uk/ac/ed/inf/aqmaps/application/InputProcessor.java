package uk.ac.ed.inf.aqmaps.application;

import java.lang.reflect.Type;
import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;

import uk.ac.ed.inf.aqmaps.map.Sensor;
import uk.ac.ed.inf.aqmaps.map.TwoDimensionalMapObject;
import uk.ac.ed.inf.aqmaps.map.What3WordsLocation;
import uk.ac.ed.inf.aqmaps.webserver.JsonSensorStub;
import uk.ac.ed.inf.aqmaps.webserver.JsonWhat3WordsStub;
import uk.ac.ed.inf.aqmaps.webserver.WebServerFileFetcher;

/**
 * Instances of this class are used by the App class to process the given
 * inputs, whether they are provided directly by the user, or fetched from the
 * file server.
 * 
 * Note: It is assigned a WebServerFileFetcher instance to retrieve any files
 * from the web server - it then extracts the relevant information from them,
 * turning it into appropriate objects.
 */
public class InputProcessor {

    /*
     * The entity that deals with the actual connection to the file server and
     * returns the needed file contents.
     */
    private final WebServerFileFetcher fileFetcher;

    /**
     * The constructor of the InputProcessor class.
     * 
     * @param port the port needed to initialise the fileFetcher attribute that is
     *             responsible for the connection to the relevant file server
     */
    public InputProcessor(int port) {
        this.fileFetcher = new WebServerFileFetcher(port);
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
     * 
     * @return a list of stubs that represent the relevant sensors
     */
    ArrayList<Sensor> getSensorsForDate(int day, int month, int year) {
        var jsonSensorsString = fileFetcher.getSensorsGeojsonFromServer(day, month, year);

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

            var w3wLocation = processW3wString(sensorStub.getLocation());

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
     * @return an instance of the What3WordsLocation class
     */
    private What3WordsLocation processW3wString(String w3wString) {
        var w3wParts = w3wString.split("\\.");
        assert (w3wParts.length == 3);
        /*
         * We split the w3w identifier into its 3 parts before fetching the according
         * information from the server.
         */
        var first = w3wParts[0];
        var second = w3wParts[1];
        var third = w3wParts[2];
        var jsonW3wString = fileFetcher.getW3wJsonFromServer(first, second, third);

        /* Turn the textual information into w3wStub, then "proper object" */
        var w3wStub = new Gson().fromJson(jsonW3wString, JsonWhat3WordsStub.class);

        var w3wLocation = processW3wStub(w3wStub);
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
    What3WordsLocation processW3wStub(JsonWhat3WordsStub stub) {

        /* Extract the specific location of any sensor that may be in this area */
        var longitude = stub.getCoordinates().getLng();
        var latitude = stub.getCoordinates().getLat();
        var position = Point.fromLngLat(longitude, latitude);

        /* Extract the remaining information from the JSON stub */
        var words = stub.getWords();

        var w3wLocation = new What3WordsLocation(words, position);
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
     * @return a list of all no fly zones declared on the file server
     */
    ArrayList<TwoDimensionalMapObject> loadNoFlyZonesFromServer() {
        /* Load Geo-JSON file from server and extract FeatureCollection */
        var jsonNoFlyZonesString = fileFetcher.getBuildingsGeojsonFromServer();
        var noFlyZonesFeatCol = FeatureCollection.fromJson(jsonNoFlyZonesString);

        /*
         * Instantiate NoFlyZone objects from FeatureCollection and add to list
         */
        var noFlyZones = new ArrayList<TwoDimensionalMapObject>();
        for (Feature feat : noFlyZonesFeatCol.features()) {
            var building = new TwoDimensionalMapObject(feat);
            noFlyZones.add(building);
        }

        return noFlyZones;
    }

}
