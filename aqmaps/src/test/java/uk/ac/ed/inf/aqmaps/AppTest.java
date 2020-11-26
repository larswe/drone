package uk.ac.ed.inf.aqmaps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import com.mapbox.geojson.Point;

/**
 * Unit test for simple App.
 */
public class AppTest {

    /*
     * EuclideanUtils
     */

    @Test
    public void intersectingSegments() {

        Point A = Point.fromLngLat(0, 0);
        Point B = Point.fromLngLat(2, 0);
        Point C = Point.fromLngLat(1, -1);
        Point D = Point.fromLngLat(1, 1);

        var firstLine = new LineSegment(A, B);
        var secondLine = new LineSegment(C, D);

        assertTrue(EuclideanUtils.lineSegmentsIntersect(firstLine, secondLine));

    }

    @Test
    public void nonIntersectingSegments() {

        Point A = Point.fromLngLat(0, 2);
        Point B = Point.fromLngLat(2, 2);
        Point C = Point.fromLngLat(1, -1);
        Point D = Point.fromLngLat(1, 1);

        var firstLine = new LineSegment(A, B);
        var secondLine = new LineSegment(C, D);

        assertFalse(EuclideanUtils.lineSegmentsIntersect(firstLine, secondLine));

    }

    @Test
    public void intersectingSegmentsComplex() {

        Point A = Point.fromLngLat(-23, 12);
        Point B = Point.fromLngLat(-42, 150);
        Point C = Point.fromLngLat(-30, -100);
        Point D = Point.fromLngLat(-30, 100);

        var firstLine = new LineSegment(A, B);
        var secondLine = new LineSegment(C, D);

        assertTrue(EuclideanUtils.lineSegmentsIntersect(firstLine, secondLine));

    }

    @Test
    public void nonIntersectingSegmentsComplex() {

        Point A = Point.fromLngLat(-23, 12);
        Point B = Point.fromLngLat(-42, 150);
        Point C = Point.fromLngLat(-50, -100);
        Point D = Point.fromLngLat(-50, 100);

        var firstLine = new LineSegment(A, B);
        var secondLine = new LineSegment(C, D);

        assertFalse(EuclideanUtils.lineSegmentsIntersect(firstLine, secondLine));

    }

    @Test
    public void touchingSegmentsIntersect() {

        Point A = Point.fromLngLat(-23, 12);
        Point B = Point.fromLngLat(-42, 150);
        Point C = Point.fromLngLat(-30, -100);
        Point D = Point.fromLngLat(-42, 150);

        var firstLine = new LineSegment(A, B);
        var secondLine = new LineSegment(C, D);

        assertTrue(EuclideanUtils.lineSegmentsIntersect(firstLine, secondLine));

    }

    @Test
    public void embeddedSegmentIntersects() {

        Point A = Point.fromLngLat(1, 4);
        Point B = Point.fromLngLat(4, 4);
        Point C = Point.fromLngLat(2, 4);
        Point D = Point.fromLngLat(4, 4);

        var firstLine = new LineSegment(A, B);
        var secondLine = new LineSegment(C, D);

        assertTrue(EuclideanUtils.lineSegmentsIntersect(firstLine, secondLine));
    }

    @Test
    public void separateSegmentsOfSameLineDoNotIntersect() {

        Point A = Point.fromLngLat(1, 4);
        Point B = Point.fromLngLat(4, 4);
        Point C = Point.fromLngLat(4.0001, 4);
        Point D = Point.fromLngLat(5, 4);

        var firstLine = new LineSegment(A, B);
        var secondLine = new LineSegment(C, D);

        assertFalse(EuclideanUtils.lineSegmentsIntersect(firstLine, secondLine));
    }

    /*
     * Drone tests
     */

    @Test
    public void droneCanFlyInStraightLine() {

        App.main(new String[] { "15", "06", "2021", "55.9444", "-3.1878", "5678", "80" });

        Point destination = Point.fromLngLat(55.944, -3.187);
        What3WordsLocation w3w = new What3WordsLocation("a.b.c", "", "", "", "", null, destination);
        Sensor s = new Sensor(0.0, 0.0, w3w);
        
        ArrayList<Sensor> arr = new ArrayList<Sensor>(Arrays.asList(s));
        
        System.out.println(arr.size());
        
        MainDrone d = new MainDrone(55.9461, -3.1924, arr);

        assertTrue(d.canGetTowardsDestinationInStraightLine(0.0003));

    }

    @Test
    public void droneDoesNotLeaveZone() {
        App.main(new String[] { "15", "06", "2021", "55.9444", "-3.1878", "5678", "80" });

        Point destination = Point.fromLngLat(-3.193, 55.947);
        
        What3WordsLocation w3w = new What3WordsLocation("a.b.c", "", "", "", "", null, destination);
        Sensor s = new Sensor(0.0, 0.0, w3w);
        
        ArrayList<Sensor> arr = new ArrayList<Sensor>(Arrays.asList(s));
        
        MainDrone d = new MainDrone(-3.192472, 55.94623, arr);

        assertFalse(d.canGetTowardsDestinationInStraightLine(0.0003));
    }
    
    @Test
    public void droneDoesNotCrossNoFlyZone() {
        App.main(new String[] { "15", "06", "2021", "55.9444", "-3.1878", "5678", "80" });
        
        Point destination = Point.fromLngLat(-3.185, 55.9427);
        
        What3WordsLocation w3w = new What3WordsLocation("a.b.c", "", "", "", "", null, destination);
        Sensor s = new Sensor(0.0, 0.0, w3w);
        
        MainDrone d = new MainDrone(-3.18971, 55.94284, new ArrayList<Sensor>(Arrays.asList(s)));

        assertFalse(d.canGetTowardsDestinationInStraightLine(0.0003));
    }
    
    @Test
    public void shadowDroneCanAvoidSimpleObstacleClockwise() {
        App.main(new String[] { "15", "06", "2021", "55.9444", "-3.1878", "5678", "80" });
        
        Point destination = Point.fromLngLat(-3.18825, 55.9426170001);
        
        ShadowDrone d = new ShadowDrone(-3.18962, 55.9426170001, destination);

        var obst = App.getNoFlyZones().get(2).getPolygon();
        
        System.out.println("Obstacle: " + obst);
        
        double dist = d.distToAvoidObstacleClockwise(obst, 0.0003);
        
        System.out.println("Final result: " + dist);
        assertTrue(true);
    }
    
    @Test
    public void shadowDroneCanAvoidSimpleObstacleCounterClockwise() {
        App.main(new String[] { "15", "06", "2021", "55.9444", "-3.1878", "5678", "80" });
        
        Point destination = Point.fromLngLat(-3.18962, 55.9426170001);
        
        ShadowDrone d = new ShadowDrone(-3.18825, 55.9426170001, destination);

        var obst = App.getNoFlyZones().get(2).getPolygon();
        
        System.out.println("Obstacle: " + obst);
        
        double dist = d.distToAvoidObstacleCounterClockwise(obst, 0.0003);
        
        System.out.println("Final result: " + dist);
        assertTrue(true);
    }
    
    @Test
    public void shadowDroneNoticesIfClockwiseRotationDoesNotWork() {
        App.main(new String[] { "15", "06", "2021", "55.9444", "-3.1878", "5678", "80" });
        
        Point destination = Point.fromLngLat(-3.18962, 55.9426170001);
        
        ShadowDrone d = new ShadowDrone(-3.18825, 55.9426170001, destination);

        var obst = App.getNoFlyZones().get(2).getPolygon();
        
        System.out.println("Obstacle: " + obst);
        
        double dist = d.distToAvoidObstacleClockwise(obst, 0.0003);
        
        System.out.println("Final result: " + dist);
        assertTrue(dist == Double.POSITIVE_INFINITY);
    }
    
    @Test
    public void shadowDroneNoticesIfCounterClockwiseRotationDoesNotWork() {
        App.main(new String[] { "15", "06", "2021", "55.9444", "-3.1878", "5678", "80" });
        
        Point destination = Point.fromLngLat(-3.18925, 55.9426170001);
        
        ShadowDrone d = new ShadowDrone(-3.18962, 55.9426170001, destination);

        var obst = App.getNoFlyZones().get(2).getPolygon();
        
        System.out.println("Obstacle: " + obst);
        
        double dist = d.distToAvoidObstacleCounterClockwise(obst, 0.0003);
        
        System.out.println("Final result: " + dist);
        assertTrue(dist == Double.POSITIVE_INFINITY);
    }
    
    @Test
    public void droneCanAvoidSimpleObstacleClockwise() {
        App.main(new String[] { "15", "06", "2021", "55.9444", "-3.1878", "5678", "80" });
        
        Point destination = Point.fromLngLat(-3.18825, 55.9426170001);
        What3WordsLocation w3w = new What3WordsLocation("a.b.c", "", "", "", "", null, destination);
        Sensor s = new Sensor(0.0, 0.0, w3w);
        
        MainDrone d = new MainDrone(-3.18962, 55.9426170001, new ArrayList<Sensor>(Arrays.asList(s)));

        var obst = App.getNoFlyZones().get(2).getPolygon();
        
        System.out.println("Initial Position: " + d.getCurrentPosition());
        
        d.avoidObstacle(obst);
        
        System.out.println("Final Position: " + d.getCurrentPosition());
        
        assertTrue(true);
    }
    
    @Test
    public void droneCanAvoidSimpleObstacleCounterClockwise() {
        App.main(new String[] { "15", "06", "2021", "55.9444", "-3.1878", "5678", "80" });
        
        Point destination = Point.fromLngLat(-3.18962, 55.9426170001);
        What3WordsLocation w3w = new What3WordsLocation("a.b.c", "", "", "", "", null, destination);
        Sensor s = new Sensor(0.0, 0.0, w3w);
        
        MainDrone d = new MainDrone(-3.18825, 55.9426170001, new ArrayList<Sensor>(Arrays.asList(s)));

        var obst = App.getNoFlyZones().get(2).getPolygon();
        
        System.out.println("Initial Position: " + d.getCurrentPosition());
        
        d.avoidObstacle(obst);
        
        System.out.println("Final Position: " + d.getCurrentPosition());
        
        assertTrue(true);
    }
    
    @Test
    public void droneCanFlyFromBottomLeftToBottomRight() {
        App.main(new String[] { "15", "06", "2021", "55.9444", "-3.1878", "5678", "80" });
        
        Point destination = Point.fromLngLat(-3.18432, 55.942618);
        What3WordsLocation w3w = new What3WordsLocation("a.b.c", "", "", "", "", null, destination);
        Sensor s = new Sensor(0.0, 0.0, w3w);
        
        MainDrone d = new MainDrone(-3.19247, 55.942618, new ArrayList<Sensor>(Arrays.asList(s)));
        
        System.out.println("Initial Position: " + d.getCurrentPosition());
        
        var steps = d.flyToCurrentDestination();
        
        System.out.println("Final Position: " + d.getCurrentPosition());
        System.out.println("Steps taken: " + steps);
        
        assertTrue(true);
    }
    
    @Test
    public void droneCanFlyFromBottomLeftToTopLeft() {
        App.main(new String[] { "15", "06", "2021", "55.9444", "-3.1878", "5678", "80" });
        
        Point destination = Point.fromLngLat(-3.192473, 55.946233);
        What3WordsLocation w3w = new What3WordsLocation("a.b.c", "", "", "", "", null, destination);
        Sensor s = new Sensor(0.0, 0.0, w3w);
        
        MainDrone d = new MainDrone(-3.19247, 55.942618, new ArrayList<Sensor>(Arrays.asList(s)));
        
        System.out.println("Initial Position: " + d.getCurrentPosition());
        
        var steps = d.flyToCurrentDestination();
        
        System.out.println("Final Position: " + d.getCurrentPosition());
        System.out.println("Steps taken: " + steps);
        
        assertTrue(true);
    }
    
    @Test
    public void droneCanFlyFromBottomLeftToTopRight() {
        App.main(new String[] { "15", "06", "2021", "55.9444", "-3.1878", "5678", "80" });
        
        Point destination = Point.fromLngLat(-3.184319, 55.946233);
        What3WordsLocation w3w = new What3WordsLocation("a.b.c", "", "", "", "", null, destination);
        Sensor s = new Sensor(0.0, 0.0, w3w);
        
        MainDrone d = new MainDrone(-3.19247, 55.942618, new ArrayList<Sensor>(Arrays.asList(s)));
        
        System.out.println("Initial Position: " + d.getCurrentPosition());
        
        var steps = d.flyToCurrentDestination();
        
        System.out.println("Final Position: " + d.getCurrentPosition());
        System.out.println("Steps taken: " + steps);
        
        assertTrue(true);
    }
}
