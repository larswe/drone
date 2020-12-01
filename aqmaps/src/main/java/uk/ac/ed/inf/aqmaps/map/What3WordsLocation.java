package uk.ac.ed.inf.aqmaps.map;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

/**
 * This class captures the properties of a What 3 Words Location. Adds little
 * functionality when compared to the JsonWhat3WordsStub class, but may be
 * useful for extensibility reasons in the future.
 *
 */
public class What3WordsLocation {

    /* The identifier of this W3W Location */
    private final String threeWordsString;

    /* The region assigned to the identifier by the W3W system */
    private final Polygon square;

    /*
     * A unique position that is assigned to the region. Used for any sensor that is
     * placed at the location.
     */
    private final Point position;

    /**
     * The constructor of the What3WordsLocation class.
     * 
     * @param words    the identifier of the W3W location, assumed to be 3 words
     *                 separated by dots
     * @param square   the region assigned to the identifier
     * @param position the unique position assigned to the region
     */
    public What3WordsLocation(String words, Polygon square, Point position) {

        /*
         * To be sure that we are dealing with a legal W3W identifier, we perform some
         * minor input validation.
         */
        var w3wParts = words.split("\\.");
        assert (w3wParts.length == 3);

        this.threeWordsString = words;

        this.square = square;
        this.position = position;
    }

    /* We override the toString method of the Object class in the most obvious way. */
    @Override
    public String toString() {
        return threeWordsString;
    }

    /* Getters */
    
    public String getThreeWordsString() {
        return threeWordsString;
    }

    public Polygon getSquare() {
        return square;
    }

    public Point getPosition() {
        return position;
    }

}
