package uk.ac.ed.inf.aqmaps.jsonstubs;

public class JsonWhat3WordsStub {

    private String country;
    private JsonRectangleStub square;
    private String nearestPlace;
    private JsonCoordinatePairStub coordinates;
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

    public JsonCoordinatePairStub getCoordinates() {
        return coordinates;
    }

    public String getNearestPlace() {
        return nearestPlace;
    }
    
}
