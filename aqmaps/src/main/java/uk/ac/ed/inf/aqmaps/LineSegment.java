package uk.ac.ed.inf.aqmaps;

import com.mapbox.geojson.Point;

public class LineSegment {

    private final Point startingPoint, endPoint;
    private final double angleInDegrees;
    private double length;

    public LineSegment(Point start, Point end) {
        this.startingPoint = start;
        this.endPoint = end;

        /*
         * If the segment has infinite slope, its angle to the "x-axis" is 90 or 270
         * degrees, depending on the longitude. Otherwise, we can use atan.
         */
        if (start.latitude() == end.latitude()) {
            this.angleInDegrees = start.longitude() <= end.longitude() ? 90 : 270;
        } else {
            double slope = (end.longitude() - start.longitude()) / (end.latitude() - start.latitude());

            var angleInRadians = Math.atan(slope);

            // System.out.println("Angle Rad: " + angleInRadians);

            var tempAngleInDegrees = Math.toDegrees(angleInRadians);
            
            /*
             * If we are moving from East to West, we need to add 180 degrees to the angle we computed from the slope.
             * That is because it is like we are moving down the graph of a linear function "from right to left". 
             */
            if (start.latitude() > end.latitude()) {
                tempAngleInDegrees += 180;
            }
            
            this.angleInDegrees = (tempAngleInDegrees + 360) % 360;
            
            //System.out.println("Delta  x " + (end.latitude() - start.latitude()));
            //System.out.println("Delta y " + (end.longitude() - start.longitude()));
            //System.out.println("Slope: " + slope);
            //System.out.println("Angle Deg: " + angleInDegrees);
        }

        this.length = EuclideanUtils.computeDistance(start, end);
    }

    public Point getStartingPoint() {
        return startingPoint;
    }

    public Point getEndPoint() {
        return endPoint;
    }

    public double getLength() {
        return length;
    }

    public double getAngleInDegrees() {
        return angleInDegrees;
    }

}
