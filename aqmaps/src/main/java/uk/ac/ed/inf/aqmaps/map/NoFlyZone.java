package uk.ac.ed.inf.aqmaps.map;

import java.util.ArrayList;
import java.util.List;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

/*
 * Buildings are represented as a single polygon. In the real world, buildings may rarely consist
 * of more than one "block", but for our drone, each such block simply represents one obstacle to 
 * be avoided. 
 */
public class NoFlyZone extends TwoDimensionalMapObject {

    private String rgbColourString;
    private List<Point> corners;

    public NoFlyZone(Feature feature) {
        super(feature);

        /*
         * If no exception was thrown, we have a legal No Fly Zone on our hands and can
         * continue parsing
         */
        this.name = feature.getStringProperty("name");
        this.rgbColourString = feature.getStringProperty("fill");
        /*
         * We convert the Mapbox points into instances of our own Location class, for
         * consistency
         */
        corners = new ArrayList<Point>();
        var pointList = polygon.coordinates().get(0);
        for (Point p : pointList) {
            var corner = Point.fromLngLat(p.longitude(), p.latitude());
            corners.add(corner);
        }

        /* Printing confirmation to console */
        System.out.println("Created NoFlyZone instance:\n" + this.toString() + "\n");
    }

    /*
     * A simple toString method for testing purposes
     */
    public String toString() {
        return "Name: " + this.name + "\nVisualization Colour: " + this.rgbColourString + "\nCoordinates: "
                + this.corners.toString();
    }

    public Polygon getPolygon() {
        return this.polygon;
    }
    
}
