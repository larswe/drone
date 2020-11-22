package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class What3WordsLocation {

    private final String firstWord, secondWord, thirdWord;
    private final String threeWordsString;
    private final String country, language, mapWebsite, nearestPlace;

    private final Polygon square;
    private final Point position;

    public What3WordsLocation(String words, String country, String language, String mapWebsite, String nearestPlace,
            Polygon square, Point position) {

        var w3wParts = words.split("\\.");
        assert (w3wParts.length == 3);

        this.firstWord = w3wParts[0];
        this.secondWord = w3wParts[1];
        this.thirdWord = w3wParts[2];

        this.threeWordsString = words;

        this.country = country;
        this.language = language;
        this.mapWebsite = mapWebsite;
        this.nearestPlace = nearestPlace;

        this.square = square;
        this.position = position;
    }

    public String toString() {
        return firstWord + "." + secondWord + "." + thirdWord;
    }

    public String getFirstWord() {
        return firstWord;
    }
    
    public String getSecondWord() {
        return secondWord;
    }
    
    public String getThirdWord() {
        return thirdWord;
    }
    
    public String getThreeWordsString() {
        return threeWordsString;
    }

    public String getCountry() {
        return country;
    }

    public String getLanguage() {
        return language;
    }

    public String getMapWebsite() {
        return mapWebsite;
    }

    public String getNearestPlace() {
        return nearestPlace;
    }

    public Polygon getSquare() {
        return square;
    }

    public Point getPosition() {
        return position;
    }

}
