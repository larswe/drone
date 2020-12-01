package uk.ac.ed.inf.aqmaps.webserver;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;

public class WebServerFileFetcher {

    /* The address of the web server on which our research data is stored */
    private static final String SERVER = "localhost";
    private static final String BUILDINGS_FOLDER_PATH = "/buildings";
    private static final String BUILDINGS_FILE_NAME = "no-fly-zones.geojson";
    private static final String SENSOR_MAP_FOLDER_PATH = "/maps";
    private static final String SENSOR_MAP_FILE_NAME = "air-quality-data.json";
    private static final String WORDS_FOLDER_PATH = "/words";
    private static final String WORDS_FILE_NAME = "details.json";

    public static String getBuildingsGeojsonFromServer(int port) {
        var stringBuilder = new StringBuilder();
        stringBuilder.append(BUILDINGS_FOLDER_PATH);
        stringBuilder.append("/");
        stringBuilder.append(BUILDINGS_FILE_NAME);

        return getStringFromFileAtPort(stringBuilder.toString(), port);
    }

    public static String getSensorsGeojsonFromServer(int day, int month, int year, int port) {
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

        return getStringFromFileAtPort(stringBuilder.toString(), port);
    }

    public static String getW3wJsonFromServer(String first, String second, String third, int port) {
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

        return getStringFromFileAtPort(stringBuilder.toString(), port);
    }

    private static String getStringFromFileAtPort(String filePath, int port) {
        var request = generateHttpRequest(filePath, port);
        var responseBody = getResponseBodyForRequest(request, port);

        return responseBody;
    }

    private static HttpRequest generateHttpRequest(String filePath, int port) {
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

    /* We send a synchronous request since the files we retrieve are small */
    private static String getResponseBodyForRequest(HttpRequest request, int port) {
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
