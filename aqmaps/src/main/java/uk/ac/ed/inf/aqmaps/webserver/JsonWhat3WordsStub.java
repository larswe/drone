package uk.ac.ed.inf.aqmaps.webserver;

public class JsonWhat3WordsStub {

    private String country;
    private JsonRectangleStub square;
    private String nearestPlace;
    private JsonPointStub coordinates;
    private String words;
    private String language;
    private String map;
    
    public String getCountry() {
        return country;
    }

    public String getWords() {
        return words;
    }

    public String getLanguage() {
        return language;
    }

    public String getMap() {
        return map;
    }

    public JsonRectangleStub getSquare() {
        return square;
    }

    public JsonPointStub getCoordinates() {
        return coordinates;
    }

    public String getNearestPlace() {
        return nearestPlace;
    }
    
}
