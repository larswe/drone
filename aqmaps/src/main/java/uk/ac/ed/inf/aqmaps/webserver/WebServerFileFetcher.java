package uk.ac.ed.inf.aqmaps.webserver;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

/**
 * Instances of this class are directly responsible for retrieving files from
 * the web server that is used to store data relevant to this application.
 */
public class WebServerFileFetcher {

    /*
     * The address of the web server on which our research data is stored, as well
     * as other constants that point us to the location of each relevant file on the
     * server.
     */
    private static final String SERVER = "localhost";
    private static final String BUILDINGS_FOLDER_PATH = "/buildings";
    private static final String BUILDINGS_FILE_NAME = "no-fly-zones.geojson";
    private static final String SENSOR_MAP_FOLDER_PATH = "/maps";
    private static final String SENSOR_MAP_FILE_NAME = "air-quality-data.json";
    private static final String WORDS_FOLDER_PATH = "/words";
    private static final String WORDS_FILE_NAME = "details.json";

    private int port;

    /**
     * The constructor of the WebServerFileFetcher class. All information it needs
     * to retrieve files is static, except the port, which depends on the command
     * line arguments used when running this application.
     * 
     * @param port the port the web server runs on.
     */
    public WebServerFileFetcher(int port) {
        this.port = port;
    }

    /**
     * This method can be used to retrieve the file contents corresponding to the no
     * fly zones used in this scenario from the web server.
     * 
     * @return the no-fly-zones.geojson file contents as a String
     */
    public String getBuildingsGeojsonFromServer() {
        var stringBuilder = new StringBuilder();
        stringBuilder.append(BUILDINGS_FOLDER_PATH);
        stringBuilder.append("/");
        stringBuilder.append(BUILDINGS_FILE_NAME);

        return extractStringFromFile(stringBuilder.toString());
    }

    /**
     * This method can be used to retrieve the file contents corresponding to the
     * sensors to be read on a given date from the web server.
     * 
     * @param day   the day of month corresponding to this program execution
     * @param month the month corresponding to this program execution
     * @param year  the year corresponding to this program execution
     * 
     * @return the String contents of the air-quality-data.json file for the given
     *         date
     */
    public String getSensorsGeojsonFromServer(int day, int month, int year) {
        var stringBuilder = new StringBuilder();
        stringBuilder.append(SENSOR_MAP_FOLDER_PATH);
        stringBuilder.append("/");
        stringBuilder.append(String.format("%04d", year));
        stringBuilder.append("/");
        stringBuilder.append(String.format("%02d", month));
        stringBuilder.append("/");
        stringBuilder.append(String.format("%02d", day));
        stringBuilder.append("/");
        stringBuilder.append(SENSOR_MAP_FILE_NAME);

        return extractStringFromFile(stringBuilder.toString());
    }

    /**
     * This method can be used to retrieve the file contents corresponding to the
     * W3W location with the given identifier.
     * 
     * @param first  the first word of the W3W identifier
     * @param second the second word of the W3W identifier
     * @param third  the third word of the W3W identifier
     * 
     * @return the String contents of the details.json file for the given location
     */
    public String getW3wJsonFromServer(String first, String second, String third) {
        var stringBuilder = new StringBuilder();
        stringBuilder.append(WORDS_FOLDER_PATH);
        stringBuilder.append("/");
        stringBuilder.append(first);
        stringBuilder.append("/");
        stringBuilder.append(second);
        stringBuilder.append("/");
        stringBuilder.append(third);
        stringBuilder.append("/");
        stringBuilder.append(WORDS_FILE_NAME);

        return extractStringFromFile(stringBuilder.toString());
    }

    /**
     * This helper method is used to extract the string contents of the file with
     * the given path.
     * 
     * @param filePath the path of the file to be retrieved
     * 
     * @return the String contents of the file
     */
    private String extractStringFromFile(String filePath) {
        var request = generateHttpRequest(filePath);
        var responseBody = getResponseBodyForRequest(request);

        return responseBody;
    }

    /**
     * This helper method generates a HTTP request to access the file that lies on
     * the web server at the specified path
     * 
     * @param filePath the path of the file to be retrieved
     * 
     * @return the HttpRequest instance capturing the request
     */
    private HttpRequest generateHttpRequest(String filePath) {
        var stringBuilder = new StringBuilder();
        stringBuilder.append("http://");
        stringBuilder.append(SERVER);
        stringBuilder.append(":");
        stringBuilder.append(port);
        stringBuilder.append("/");
        stringBuilder.append(filePath);
        var url = stringBuilder.toString();

        /* The client assumes the following is a GET request by default */
        var request = HttpRequest.newBuilder().uri(URI.create(url)).build();

        return request;
    }

    /**
     * This helper method takes the previously generated HTTP request, sends it to
     * the server and returns the according response body.
     * 
     * @param request a HTTP request to be sent to the server
     * 
     * @return the body of the server's response
     */
    private String getResponseBodyForRequest(HttpRequest request) {
        var client = HttpClient.newHttpClient();
        String responseBody = "";
        try {
            var response = client.send(request, BodyHandlers.ofString());
            System.out.println("Http response received with status code " + response.statusCode() + ".");

            /* I am assuming that a 200 status code is the only acceptable response */
            if (response.statusCode() != 200) {
                throw new Exception("Http error with return code " + response.statusCode());
            }

            responseBody = response.body();
        } catch (IOException | InterruptedException e) { // ConnectException is a subclass of IOException
            System.out.println("Fatal error: Unable to connect to " + SERVER + " at port " + port + ".");
            System.exit(418);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        return responseBody;
    }

}
