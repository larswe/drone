package uk.ac.ed.inf.aqmaps.geometry;

import java.util.ArrayList;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

/**
 * This class provides static utility methods relating to Euclidean geometry.
 * They are used by the Drone classes, but its methods may be used by other
 * classes in the future. Conceptually, it really would not make sense for a
 * drone to carry all these methods.
 *
 */
public class EuclideanUtils {

    /**
     * The orientation of a triplet of points A,B,C. Used only by the EuclideanUtils
     * class when figuring out whether two line segments intersect, and therefore
     * made private.
     */
    private enum Orientation {
        CLOCKWISE, COUNTERCLOCKWISE, COLINEAR
    }

    /**
     * This private constructor is merely meant to keep this class from ever being
     * instantiated outside of this class, which would simply be unnecessary.
     */
    private EuclideanUtils() {
    }

    /**
     * Computes the Euclidean distance between 2 points in the 2D-plane.
     * 
     * @param a the first point
     * @param b the second point
     * @return the distance between the two points
     */
    public static double computeDistance(Point a, Point b) {
        return Math.sqrt(Math.pow(a.longitude() - b.longitude(), 2) + Math.pow(a.latitude() - b.latitude(), 2));
    }

    /**
     * Given the current position of an object, the angle/direction of its movement
     * vector, as well as the distance it is meant to overcome on this step, this
     * method computes the object's next position using basic trigonometry.
     * 
     * @param position    the current position of the object in question
     * @param angleInDegs the direction of the object's movement
     * @param distance    the length of the move to be computed
     * 
     * @return the position at the end of the object's movement
     */
    public static Point getNextPosition(Point position, double angleInDegs, double distance) {

        var angleInRads = Math.toRadians(angleInDegs);

        /*
         * Note that a move towards East increases the longitude, thus acting as our
         * conventional "x" in this case. Likewise, latitude corresponds to y, since a
         * move North increases it.
         */
        double newLong = position.longitude() + distance * Math.cos(angleInRads);
        double newLat = position.latitude() + distance * Math.sin(angleInRads);

        var nextPosition = Point.fromLngLat(newLong, newLat);

        return nextPosition;
    }

    /**
     * This method computes whether a given line segment and polygon intersect.
     * 
     * We obtain the line segments which the polygon (assumed to have no holes)
     * consists of. For each line segment, we check whether the segments intersect.
     * If none of the lines intersect, the polygons do not intersect. This only
     * works this easily in 2D but that happens to be what we are dealing with, so
     * we are lucky.
     * 
     * @param lineSegment the line segment in question
     * @param polygon     the polygon in question. Assumed to have no holes.
     * 
     * @return whether the two parameters intersect
     */
    public static boolean lineSegmentAndPolygonIntersect(LineSegment lineSegment, Polygon polygon) {

        var polygonEdges = new ArrayList<LineSegment>();
        var polygonCorners = polygon.coordinates().get(0);
        int n = polygonCorners.size(); // the number of corners/edges of the polygon + 1

        /*
         * We add all edges of the polygon to the list.
         */
        for (int i = 0; i < n - 1; i++) {
            var startingCorner = polygonCorners.get(i);
            var endCorner = polygonCorners.get(i + 1);

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

    /**
     * This method determines whether two given line segments intersect. This sounds
     * like it should be an easy task, but it turns out we need to be a bit clever
     * about it.
     * 
     * We use the following idea: Consider line segments from A to B and from C to
     * D. If 3 of these 4 points lie in a straight line, the line segments intersect
     * if the intersection of the lines that the triplet induces occurs in between
     * the start and end points of the segments. Note that we may observe either 1
     * or 4 such triplets, if any are present. If we observe any colinear triplets,
     * but none of them fulfils the condition that would make the segments overlap,
     * it is clear from visualisation that the segments indeed do not overlap.
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
     * Note: We could have used Cramer's Rule for this, but we shall keep things as
     * simple as we can, conceptually.
     * 
     * Note also: We return true if the line segments touch in any way whatsoever.
     * Otherwise the drone could e.g. leave the confinement area via a "clipping
     * glitch" - it could first enter the wall, then leave on another turn.
     * 
     * @param firstLine  the first line segment in question
     * @param secondLine the second line segment in question
     * @return whether the line segments intersect(/touch)
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
         * which happens if the two line segments touch, but do not cross each other -
         * we count this as an intersection. Note that this also takes care of the
         * special case in which one segment is completely embedded in the other
         * segment.
         */
        var a = firstLine.getStartPoint();
        var b = firstLine.getEndPoint();
        var c = secondLine.getStartPoint();
        var d = secondLine.getEndPoint();

        var orientationACD = computeOrientation(a, c, d);
        var orientationBCD = computeOrientation(b, c, d);
        var orientationCAB = computeOrientation(c, a, b);
        var orientationDAB = computeOrientation(d, a, b);

        boolean observedColinearTriplet = false;

        if (orientationACD == Orientation.COLINEAR) {
            if (pointOnSegmentInColinearTriplet(a, secondLine)) {
                return true;
            } else {
                observedColinearTriplet = true;
            }
        }
        if (orientationBCD == Orientation.COLINEAR) {
            if (pointOnSegmentInColinearTriplet(b, secondLine)) {
                return true;
            } else {
                observedColinearTriplet = true;
            }
        }
        if (orientationCAB == Orientation.COLINEAR) {
            if (pointOnSegmentInColinearTriplet(c, firstLine)) {
                return true;
            } else {
                observedColinearTriplet = true;
            }
        }
        if (orientationDAB == Orientation.COLINEAR) {
            if (pointOnSegmentInColinearTriplet(d, firstLine)) {
                return true;
            } else {
                observedColinearTriplet = true;
            }
        }

        if (observedColinearTriplet) {
            return false;
        }

        /*
         * If we have gotten here, none of the triplets are colinear and we can use the
         * triangle approach that is described in the documentation at the start of this
         * method.
         */
        if (orientationACD == orientationBCD || orientationCAB == orientationDAB) {
            return false;
        } else {
            return true;
        }

    }

    /**
     * Given a line segment from Point A to Point B, as well as a Point P, such that
     * A,B, P are colinear, this function determines whether P lies on the given
     * segment AB.
     * 
     * @param point       the point P in question
     * @param lineSegment the line segment from point A to point B in question
     * 
     * @return whether the point lies on the line segment
     */
    public static boolean pointOnSegmentInColinearTriplet(Point point, LineSegment lineSegment) {
        
        var start = lineSegment.getStartPoint();
        var end = lineSegment.getEndPoint();

        if (point.longitude() > start.longitude()) {
            return (point.longitude() <= end.longitude());
        } else if (point.longitude() < start.longitude()) {
            return (point.longitude() >= end.longitude());
        } else {
            /* In this case, the 3 points lie on a vertical line. */
            if (point.latitude() > start.latitude()) {
                return (point.latitude() <= end.latitude());
            } else if (point.latitude() < start.latitude()) {
                return (point.latitude() >= end.latitude());
            } else {
                /* If we get here, the Point was the start of the segment all along! */
                return true;
            }
        }
    }

    /**
     * Given three points A, B and C, this method determines the orientation of the
     * triangle through the points A,B and C (in this order).
     * 
     * The idea to use the slope of the line segments AB, AC to determine their
     * orientation. First we need to take care of the special cases in which the
     * slope is infinite. The conditions I use can be confirmed by hand rather
     * easily.
     * 
     * In the normal (non-colinear) case, we compare the slopes of the line segments
     * to compute the orientation of the triangle. We do so implicitly, but the
     * inequality we using is equivalent to what we are trying to show, and avoids
     * having to split the computation into several cases depending on the relative
     * longitudes of the points.
     * 
     * @param a point A
     * @param b point B
     * @param c point C
     * 
     * @return the orientation of the triangle through the three points
     */
    public static Orientation computeOrientation(Point a, Point b, Point c) {

        /* Take care of the cases in which one of the two slope of AB/AC is infinite */
        if (b.longitude() == a.longitude()) {
            if (b.latitude() == a.latitude() || c.longitude() == b.longitude()) {
                return Orientation.COLINEAR;
            } else if (b.latitude() > a.latitude() ^ c.longitude() > b.longitude()) {
                return Orientation.COUNTERCLOCKWISE;
            } else {
                return Orientation.CLOCKWISE;
            }
        }

        if (c.longitude() == a.longitude()) {
            if (c.latitude() == a.latitude()) { // Already dealt with case in which B.longitude == A/C.longitude
                return Orientation.COLINEAR;
            } else if (c.latitude() > a.latitude() ^ b.longitude() > c.longitude()) {
                return Orientation.CLOCKWISE;
            } else {
                return Orientation.COUNTERCLOCKWISE;
            }
        }

        /*
         * We compare the slopes of the two induces lines AB/AC (implicitly).
         */
        var yDeltaAB = b.latitude() - a.latitude();
        var yDeltaAC = c.latitude() - a.latitude();
        var xDeltaAB = b.longitude() - a.longitude();
        var xDeltaAC = c.longitude() - a.longitude();

        if (yDeltaAC * xDeltaAB > yDeltaAB * xDeltaAC) {
            return Orientation.COUNTERCLOCKWISE;
        } else if (yDeltaAC * xDeltaAB < yDeltaAB * xDeltaAC) {
            return Orientation.CLOCKWISE;
        } else {
            return Orientation.COLINEAR;
        }
    }
}
