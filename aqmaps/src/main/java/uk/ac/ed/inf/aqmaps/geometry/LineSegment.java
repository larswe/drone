package uk.ac.ed.inf.aqmaps.geometry;

import com.mapbox.geojson.Point;

/**
 * This class models a "line segment", the part of an (infinite) line in the
 * 2D-plane , that starts at point A and ends at point B.
 * 
 * If we were being strict, the name "line segment" may not be geometrically
 * accurate (lines do not have a direction), but for our purposes it is more
 * intuitive than e.g."vector".
 * 
 * And we want to define the direction of the segment in order to be able to
 * model the movement of an object.
 */
public class LineSegment {

    /* The starting and end points of the segments */
    private final Point startPoint, endPoint;

    /*
     * Because the position of a line segment can never change, it makes sense to
     * compute the induced angle relative to the x/longitude-axis once and remember
     * it. The same goes for its length. (Although this is just a design decision)
     */
    private final double angleInDegrees;
    private final double length;

    /**
     * The constructor of the LineSegment class.
     * 
     * Note that we compute the angle and length of the segment right away - we
     * could simply compute them when queried, but this is just a minor design
     * decision. In a way, this is more consistent. Each line has an angle and
     * length and by making them "final", we prohibit tinkering with them after the
     * fact.
     * 
     * @param start the starting point of the line segment
     * @param end the end point of the line segment 
     */
    public LineSegment(Point start, Point end) {
        this.startPoint = start;
        this.endPoint = end;

        /*
         * If the segment has infinite slope, its angle to the "x-axis" is 90 or 270
         * degrees, depending on the longitude. Otherwise, we can use atan.
         */
        if (start.longitude() == end.longitude()) {
            this.angleInDegrees = start.latitude() <= end.latitude() ? 90 : 270;
        } else {
            double slope = (end.latitude() - start.latitude()) / (end.longitude() - start.longitude());

            var angleInRadians = Math.atan(slope);

            var tempAngleInDegrees = Math.toDegrees(angleInRadians);

            /*
             * If we are moving from East to West, we need to add 180 degrees to the angle
             * we computed from the slope. That is because it is like we are moving down the
             * graph of a linear function "from right to left".
             */
            if (start.longitude() > end.longitude()) {
                tempAngleInDegrees += 180;
            }

            this.angleInDegrees = (tempAngleInDegrees + 360) % 360;
        }

        this.length = EuclideanUtils.computeDistance(start, end);
    }

    public Point getStartPoint() {
        return startPoint;
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
