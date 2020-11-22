package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;

public class LineSegment {

    private Point startingPoint, endPoint;
    private double length;
    
    public LineSegment(Point start, Point end) {
        this.startingPoint = start;
        this.endPoint = end;
        
        this.length = EuclideanUtils.computeDistance(start, end);
    }

    public Point getStartingPoint() {
        return startingPoint;
    }

    public void setStartingPoint(Point startingPoint) {
        this.startingPoint = startingPoint;
        this.length = EuclideanUtils.computeDistance(startingPoint, endPoint);
    }

    public Point getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(Point endPoint) {
        this.endPoint = endPoint;
        this.length = EuclideanUtils.computeDistance(startingPoint, endPoint);
    }

    public double getLength() {
        return length;
    }
    
}
