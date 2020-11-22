package uk.ac.ed.inf.aqmaps;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.mapbox.geojson.Point;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
 
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
}
