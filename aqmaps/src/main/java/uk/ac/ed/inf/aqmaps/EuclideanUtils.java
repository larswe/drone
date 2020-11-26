package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class EuclideanUtils {

    /*
     * The orientation of a triplet of points A,B,C. Only needed in this class when
     * figuring out whether two line segments intersect, therefore made private.
     */
    private enum Orientation {
        CLOCKWISE, COUNTERCLOCKWISE, COLINEAR
    };

    /* Private constructor that prevents this class from being instantiated */
    private EuclideanUtils() {
    }

    public static double computeDistance(Point a, Point b) {
        return Math.sqrt(Math.pow(a.longitude() - b.longitude(), 2) + Math.pow(a.latitude() - b.latitude(), 2));
    }

    public static Point getNextPosition(Point position, double angle, double distance) {

        //System.out.println("GET NEXT POS");
        //System.out.println(position);
        //System.out.println(angle);
        //System.out.println(distance);
        
        var angleInRads = Math.toRadians(angle);
        
        /*
         * Note that a move towards East increases the latitude, thus acting as our conventional "x" in this case.
         * Likewise, longitude corresponds to y, since a move North increases it. 
         */
        double newLong = position.longitude() + distance * Math.cos(angleInRads);
        double newLat = position.latitude() + distance * Math.sin(angleInRads);

        var nextPosition = Point.fromLngLat(newLong, newLat);
        
        //System.out.println(nextPosition);
        //System.out.println();
        
        return nextPosition;
    }

    /*
     * We obtain the line segments which the polygon consists of. For each line
     * segment, we check whether the segments intersect. If none of the lines
     * intersect, the polygons do not intersect. This only works this easily in 2D
     * but that happens to be what we are dealing with, so we are lucky.
     */
    public static boolean lineSegmentAndPolygonIntersect(LineSegment lineSegment, Polygon polygon) {

        var polygonEdges = new ArrayList<LineSegment>();
        var polygonCorners = polygon.coordinates().get(0);
        int n = polygonCorners.size(); // the number of corners/edges of the polygon + 1
        
        /*
         * We add all edges of the polygon to the list.
         */
        for (int i = 0; i < n-1; i++) {
            var startingCorner = polygonCorners.get(i);
            var endCorner = polygonCorners.get(i+1);
            
            polygonEdges.add(new LineSegment(startingCorner, endCorner));
        }
        
        for (var polygonSegment : polygonEdges) {
            if (lineSegmentsIntersect(lineSegment, polygonSegment)) {
                return true; // return true as soon as a collision occurs, for efficiency
            }
        }

        /*
         * If we have not returned true yet, the line segment and polygon do not
         * intersect
         */
        return false;
    }

    /*
     * We use the following idea: Consider line segments from A to B and from C to
     * D. If 3 of these 4 points lie in a straight line, the line segments intersect
     * if and only if the intersection of the lines occurs in between the start and
     * end points of the segments.
     * 
     * Otherwise, we can draw 2 triangles: One through the points ACD, one through
     * the points BCD. Suppose the points A,C,D are listed in clockwise order. If
     * the segments intersect, i.e. if A and B are separated by the segment from C
     * to D, the points B,C,D are listed in counter-clockwise order. Since
     * equivalently, C and D would be separated by AB, the same holds true for CAB,
     * DAB.
     * 
     * On the other hand, if either one of these two pairs of triplets have the same
     * orientation, the segments do not intersect, which is easily checked by
     * drawing.
     * 
     * Note: We could have used Cramer's Rule for this, but we shall keep things
     * simple.
     */
    public static boolean lineSegmentsIntersect(LineSegment firstLine, LineSegment secondLine) {
                
        /*
         * We take care of the special case in which one of the segments is a single
         * point.
         */
        if (firstLine.getLength() == 0.0 || secondLine.getLength() == 0.0) {
            return false;
        }

        /*
         * We deal with the cases in which 3 of the 4 relevant points are colinear...
         * Each such colinear triplet corresponds to a point of one segment + the other
         * segment. If the point lies on the segment, the two segments touch - we count
         * this as an intersection.
         * 
         * Note that this also takes care of the special case in which one segment is
         * completely embedded in the other segment.
         */
        var A = firstLine.getStartingPoint();
        var B = firstLine.getEndPoint();
        var C = secondLine.getStartingPoint();
        var D = secondLine.getEndPoint();

        var orientationACD = computeOrientation(A, C, D);
        var orientationBCD = computeOrientation(B, C, D);
        var orientationCAB = computeOrientation(C, A, B);
        var orientationDAB = computeOrientation(D, A, B);

        boolean observedColinearTriplet = false;
        
        if (orientationACD == Orientation.COLINEAR) {
            if (pointLiesOnColinearSegment(A, secondLine)) {
                return true;
            } else {
                observedColinearTriplet = true;
            }
        }
        if (orientationBCD == Orientation.COLINEAR) {
            if (pointLiesOnColinearSegment(B, secondLine)) {
                return true;
            } else {
                observedColinearTriplet = true;
            }
        }
        if (orientationCAB == Orientation.COLINEAR) {
            if (pointLiesOnColinearSegment(C, firstLine)) {
                return true;
            } else {
                observedColinearTriplet = true;
            }
        }
        if (orientationDAB == Orientation.COLINEAR) {
            if (pointLiesOnColinearSegment(D, firstLine)) {
                return true;
            } else {
                observedColinearTriplet = true;
            }
        }
        
        /*
         * If we observed one or more colinear triplets, but none of them fulfilled the condition that
         * the point lies on the relevant segment, the two segments do not intersect. 
         * 
         * That is because we have either observed only one colinear triplet, in which case this is obvious
         * from visualization, or we have observed four colinear triplets - and since we haven't returned true 
         * yet, we conclude that the two input arguments are completely separate segments of the same line and 
         * do thus not intersect. 
         */

        /*
         * If we have gotten here, none of the triplets are colinear and we can use the triangle 
         * approach that is described in the documentation at the start of this method. 
         */
         if (orientationACD == orientationBCD || orientationCAB == orientationDAB) {
             return false;
         } else {
             return true;
         }
         
         
        
    }

    /*
     * Given a line segment from Point A to Point B, as well as a Point P, such that
     * A,B, P are colinear, this function determines whether P lies on the given
     * segment.
     */
    public static boolean pointLiesOnColinearSegment(Point point, LineSegment segment) {

        var start = segment.getStartingPoint();
        var end = segment.getEndPoint();

        if (point.longitude() > start.longitude()) {
            return (point.longitude() <= end.longitude());
        } else if (point.longitude() < start.longitude()) {
            return (point.longitude() >= end.longitude());
        } else {
            // In this case, the 3 points lie on a vertical line.
            if (point.latitude() > start.latitude()) {
                return (point.latitude() <= end.latitude());
            } else if (point.latitude() < start.latitude()) {
                return (point.latitude() >= end.latitude());
            } else {
                return true; // If we get here, the Point was the start of the segment all along!
            }
        }
    }

    public static Orientation computeOrientation(Point A, Point B, Point C) {

        /*
         * We want to use the slope of the line segments AB, AC to determine their
         * orientation. But first we need to take care of the special cases in which the
         * slope is infinite. The correctness of the conditions I use can easily be
         * confirmed by hand.
         */
        if (B.longitude() == A.longitude()) {
            if (B.latitude() == A.latitude() || C.longitude() == B.longitude()) {
                return Orientation.COLINEAR;
            } else if (B.latitude() > A.latitude() ^ C.longitude() > B.longitude()) {
                return Orientation.COUNTERCLOCKWISE;
            } else {
                return Orientation.CLOCKWISE;
            }
        }

        if (C.longitude() == A.longitude()) {
            if (C.latitude() == A.latitude()) { // Already dealt with case in which B.longitude == A/C.longitude
                return Orientation.COLINEAR;
            } else if (C.latitude() > A.latitude() ^ B.longitude() > C.longitude()) {
                return Orientation.CLOCKWISE;
            } else {
                return Orientation.COUNTERCLOCKWISE;
            }
        }

        /*
         * Now that we know that the slopes are well-defined, we can proceed as planned.
         * Originally, I had planned to split the remaining computations into several
         * cases, depending on whether the longitude of B/C is greater than of A/B,
         * respectively. But then I noticed this boiled down to the following:
         */
        var yDeltaAB = B.latitude() - A.latitude();
        var yDeltaAC = C.latitude() - A.latitude();
        var xDeltaAB = B.longitude() - A.longitude();
        var xDeltaAC = C.longitude() - A.longitude();

        if (yDeltaAC * xDeltaAB > yDeltaAB * xDeltaAC) {
            return Orientation.COUNTERCLOCKWISE;
        } else if (yDeltaAC * xDeltaAB < yDeltaAB * xDeltaAC) {
            return Orientation.CLOCKWISE;
        } else {
            return Orientation.COLINEAR;
        }
    }
}
