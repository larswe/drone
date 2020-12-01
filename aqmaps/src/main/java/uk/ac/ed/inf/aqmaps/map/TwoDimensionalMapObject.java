package uk.ac.ed.inf.aqmaps.map;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Polygon;

public class TwoDimensionalMapObject {
    protected Polygon polygon;
    protected String name;

    public TwoDimensionalMapObject(Polygon polygon, String name) {
        this.polygon = polygon;
        this.name = name;
    }
    
    public TwoDimensionalMapObject(Feature feature) {
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
    
    public Polygon getPolygon() {
        return polygon;
    }

    public void setPolygon(Polygon polygon) {
        this.polygon = polygon;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}
