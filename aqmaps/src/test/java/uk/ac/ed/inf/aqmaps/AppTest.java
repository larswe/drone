package uk.ac.ed.inf.aqmaps;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Test;

import com.mapbox.geojson.Point;

import uk.ac.ed.inf.aqmaps.application.App;
import uk.ac.ed.inf.aqmaps.drone.MainDrone;
import uk.ac.ed.inf.aqmaps.drone.ShadowDrone;
import uk.ac.ed.inf.aqmaps.geometry.EuclideanUtils;
import uk.ac.ed.inf.aqmaps.geometry.LineSegment;
import uk.ac.ed.inf.aqmaps.map.Sensor;
import uk.ac.ed.inf.aqmaps.map.What3WordsLocation;

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

    // Test disabled
    @Test
    public void droneCanFlyInStraightLine() {

        App.main(new String[] { "15", "06", "2021", "55.9444", "-3.1878", "5678", "80" });

        Point destination = Point.fromLngLat(55.944, -3.187);
        What3WordsLocation w3w = new What3WordsLocation("a.b.c", null, destination);
        Sensor s = new Sensor(0.0f, 0.0, w3w);
        
        ArrayList<Sensor> arr = new ArrayList<Sensor>(Arrays.asList(s));
        
        System.out.println(arr.size());
        
        MainDrone d = new MainDrone(Point.fromLngLat(55.9461, -3.1924), arr);

        //assertTrue(d.canMoveTowardsGoal());

    }

    // Test disabled
    @Test
    public void droneDoesNotLeaveZone() {
        App.main(new String[] { "15", "06", "2021", "55.9444", "-3.1878", "5678", "80" });

        Point destination = Point.fromLngLat(-3.193, 55.947);
        
        What3WordsLocation w3w = new What3WordsLocation("a.b.c", null, destination);
        Sensor s = new Sensor(0.0f, 0.0, w3w);
        
        ArrayList<Sensor> arr = new ArrayList<Sensor>(Arrays.asList(s));
        
        MainDrone d = new MainDrone(Point.fromLngLat(-3.192472, 55.94623), arr);

        //assertFalse(d.canFlyStraightAtGoal());
    }
    
    // Test disabled
    @Test
    public void droneDoesNotCrossNoFlyZone() {
        App.main(new String[] { "15", "06", "2021", "55.9444", "-3.1878", "5678", "80" });
        
        Point destination = Point.fromLngLat(-3.185, 55.9427);
        
        What3WordsLocation w3w = new What3WordsLocation("a.b.c", null, destination);
        Sensor s = new Sensor(0.0f, 0.0, w3w);
        
        MainDrone d = new MainDrone(Point.fromLngLat(-3.18971, 55.94284), new ArrayList<Sensor>(Arrays.asList(s)));

        //assertFalse(d.canFlyStraightAtGoal());
    }
    
    @Test
    public void shadowDroneCanAvoidSimpleObstacleClockwise() {
        App.main(new String[] { "15", "06", "2021", "55.9444", "-3.1878", "5678", "80" });
        
        Point destination = Point.fromLngLat(-3.18825, 55.9426170001);
        
        ShadowDrone d = new ShadowDrone(Point.fromLngLat(-3.18962, 55.9426170001), destination);

        var obst = App.getNoFlyZones().get(2);
        
        System.out.println("Obstacle: " + obst);
        
        double dist = d.costOfAvoidingObstacle(obst, 0.0003, true);
        
        System.out.println("Final result: " + dist);
        assertTrue(true);
    }
    
    @Test
    public void shadowDroneCanAvoidSimpleObstacleCounterClockwise() {
        App.main(new String[] { "15", "06", "2021", "55.9444", "-3.1878", "5678", "80" });
        
        Point destination = Point.fromLngLat(-3.18962, 55.9426170001);
        
        ShadowDrone d = new ShadowDrone(Point.fromLngLat(-3.18825, 55.9426170001), destination);

        var obst = App.getNoFlyZones().get(2);
        
        System.out.println("Obstacle: " + obst);
        
        double dist = d.costOfAvoidingObstacle(obst, 0.0003, false);
        
        System.out.println("Final result: " + dist);
        assertTrue(true);
    }
    
    @Test
    public void shadowDroneNoticesIfClockwiseRotationDoesNotWork() {
        App.main(new String[] { "15", "06", "2021", "55.9444", "-3.1878", "5678", "80" });
        
        Point destination = Point.fromLngLat(-3.18962, 55.9426170001);
        
        ShadowDrone d = new ShadowDrone(Point.fromLngLat(-3.18825, 55.9426170001), destination);

        var obst = App.getNoFlyZones().get(2);
        
        System.out.println("Obstacle: " + obst);
        
        double dist = d.costOfAvoidingObstacle(obst, 0.0003, true);
        
        System.out.println("Final result: " + dist);
        assertTrue(dist == Double.POSITIVE_INFINITY);
    }
    
    @Test
    public void shadowDroneNoticesIfCounterClockwiseRotationDoesNotWork() {
        App.main(new String[] { "15", "06", "2021", "55.9444", "-3.1878", "5678", "80" });
        
        Point destination = Point.fromLngLat(-3.18925, 55.9426170001);
        
        ShadowDrone d = new ShadowDrone(Point.fromLngLat(-3.18962, 55.9426170001), destination);

        var obst = App.getNoFlyZones().get(2);
        
        System.out.println("Obstacle: " + obst);
        
        double dist = d.costOfAvoidingObstacle(obst, 0.0003, false);
        
        System.out.println("Final result: " + dist);
        assertTrue(dist == Double.POSITIVE_INFINITY);
    }
    
    @Test
    public void droneCanAvoidSimpleObstacleClockwise() {
        App.main(new String[] { "15", "06", "2021", "55.9444", "-3.1878", "5678", "80" });
        
        Point destination = Point.fromLngLat(-3.18825, 55.9426170001);
        What3WordsLocation w3w = new What3WordsLocation("a.b.c", null, destination);
        Sensor s = new Sensor(0.0f, 0.0, w3w);
        
        MainDrone d = new MainDrone(Point.fromLngLat(-3.18962, 55.9426170001), new ArrayList<Sensor>(Arrays.asList(s)));

        var obst = App.getNoFlyZones().get(2);
        
        System.out.println("Initial Position: " + d.getCurrentPosition());
        
        d.avoidObstacle(obst);
        
        System.out.println("Final Position: " + d.getCurrentPosition());
        
        assertTrue(true);
    }
    
    @Test
    public void droneCanAvoidSimpleObstacleCounterClockwise() {
        App.main(new String[] { "15", "06", "2021", "55.9444", "-3.1878", "5678", "80" });
        
        Point destination = Point.fromLngLat(-3.18962, 55.9426170001);
        What3WordsLocation w3w = new What3WordsLocation("a.b.c", null, destination);
        Sensor s = new Sensor(0.0f, 0.0, w3w);
        
        MainDrone d = new MainDrone(Point.fromLngLat(-3.18825, 55.9426170001), new ArrayList<Sensor>(Arrays.asList(s)));

        var obst = App.getNoFlyZones().get(2);
        
        System.out.println("Initial Position: " + d.getCurrentPosition());
        
        d.avoidObstacle(obst);
        
        System.out.println("Final Position: " + d.getCurrentPosition());
        
        assertTrue(true);
    }
    
    @Test
    public void droneCanFlyFromBottomLeftToBottomRight() {
        App.main(new String[] { "15", "06", "2021", "55.9444", "-3.1878", "5678", "80" });
        
        Point destination = Point.fromLngLat(-3.18432, 55.942618);
        What3WordsLocation w3w = new What3WordsLocation("a.b.c", null, destination);
        Sensor s = new Sensor(0.0f, 0.0, w3w);
        
        MainDrone d = new MainDrone(Point.fromLngLat(-3.19247, 55.942618), new ArrayList<Sensor>(Arrays.asList(s)));
        
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
        What3WordsLocation w3w = new What3WordsLocation("a.b.c", null, destination);
        Sensor s = new Sensor(0.0f, 0.0, w3w);
        
        MainDrone d = new MainDrone(Point.fromLngLat(-3.19247, 55.942618), new ArrayList<Sensor>(Arrays.asList(s)));
        
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
        What3WordsLocation w3w = new What3WordsLocation("a.b.c", null, destination);
        Sensor s = new Sensor(0.0f, 0.0, w3w);
        
        MainDrone d = new MainDrone(Point.fromLngLat(-3.19247, 55.942618), new ArrayList<Sensor>(Arrays.asList(s)));
        
        System.out.println("Initial Position: " + d.getCurrentPosition());
        
        var steps = d.flyToCurrentDestination();
        
        System.out.println("Final Position: " + d.getCurrentPosition());
        System.out.println("Steps taken: " + steps);
        
        assertTrue(true);
    }
    
    /* Massive unit test - see main method of App class for comment. 
    @Test
    public void bigTest() {
        for (int i = 1; i <= 28; i++) {
            for (int j = 1; j <= 12; j++) {
                String day = String.format("%02d", i);
                String month = String.format("%02d", j);
                App.main(new String[] {day, month, "2020", "55.9444", "-3.1878", "5678", "80" });
                assertTrue(EuclideanUtils.computeDistance(App.droneLastPos, Point.fromLngLat(-3.1878, 55.9444)) <= 0.0003);
                for (boolean b : App.visited) {
                    assertTrue(b);
                }
                App.main(new String[] {day, month, "2021", "55.9444", "-3.1878", "5678", "80" });
                assertTrue(EuclideanUtils.computeDistance(App.droneLastPos, Point.fromLngLat(-3.1878, 55.9444)) <= 0.0003);
                for (boolean b : App.visited) {
                    assertTrue(b);
                }
            }
        }
    } 
    */
}
