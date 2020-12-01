package uk.ac.ed.inf.aqmaps.map;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Polygon;

/**
 * TwoDimensionalMapObjects are characterised by a polygon (without holes, in
 * practice, but this is not enforced by this class) and a name. In our
 * scenario, we have two types of 2D map objects: The confinement area of the
 * drone, as well as the no fly zones.
 * 
 * Note: Although these two things are not identical conceptually (in fact, they
 * are complementary), we have no reason not to treat them in the same way. A
 * drone is not allowed to cross their boundaries.
 *
 */
public class TwoDimensionalMapObject {
    /* The polygon field captures the shape and position of the object */
    private Polygon polygon;

    private final String name;

    /**
     * The first and more straight-forward constructor of the
     * TwoDimensionalMapObject class. Takes a polygon and name and simply assigns
     * them to the according attributes.
     * 
     * @param polygon the polygon that defines shape and position of the object
     * @param name    the name of the object
     */
    public TwoDimensionalMapObject(Polygon polygon, String name) {
        this.polygon = polygon;
        this.name = name;
    }

    /**
     * The second and slightly more contrived constructor of the
     * TwoDimensionalMapObject class. Takes a feature, which is assumed to have a
     * Mapbox Geometry that is a Mapbox Polygon, as well as a String property called
     * "name". The two attributes of the instance to be created is then assigned the
     * values that can be extracted from the feature.
     * 
     * @param feature the feature that defines the map object 
     */
    public TwoDimensionalMapObject(Feature feature) {
        this.name = feature.getStringProperty("name");

        var geometry = feature.geometry();

        try {
            if (geometry instanceof Polygon && ((Polygon) geometry).coordinates().size() == 1) {
                this.polygon = (Polygon) geometry;
            } else {
                throw new IllegalArgumentException("One of the specified No-Fly-Zones is not a (solid) polygon!");
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    /* Getters */
    
    public Polygon getPolygon() {
        return polygon;
    }

    public String getName() {
        return name;
    }
}
