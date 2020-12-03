package uk.ac.ed.inf.aqmaps.webserver;

/**
 * This class is used to extract the defining properties of a sensor from a
 * details.json file. Its instances are an incomplete implementation of a
 * What3Words Location, later to be converted into a proper What3WordsLocation
 * object. (Although in the current state of the application, the difference between
 * proper class and stub is very small)
 * 
 * Note that currently irrelevant attributes from the details file are dropped. 
 */
public class JsonWhat3WordsStub {

    private JsonPointStub coordinates;
    private String words;

    public String getWords() {
        return words;
    }

    public JsonPointStub getCoordinates() {
        return coordinates;
    }

}
