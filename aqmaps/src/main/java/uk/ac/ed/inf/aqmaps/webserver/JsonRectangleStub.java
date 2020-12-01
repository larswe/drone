package uk.ac.ed.inf.aqmaps.webserver;

/**
 * This class is used to extract the defining properties of a rectangle from the
 * GeoJSON file corresponding to a What3Words Location. It thus captures part of
 * the inner structure of those files.
 * 
 * A rectangle that qualifies as a W3W Region is uniquely defined by its
 * Northeast and Southwest corners, because its orientation is implied.
 */
public class JsonRectangleStub {
    /*
     * The two relevant corners of the rectangle are captured using the
     * JsonPointStub class, and are thus complex inner structures themselves.
     */
    private JsonPointStub southwest, northeast;

    public JsonPointStub getSouthwest() {
        return southwest;
    }

    public JsonPointStub getNortheast() {
        return northeast;
    }
}
