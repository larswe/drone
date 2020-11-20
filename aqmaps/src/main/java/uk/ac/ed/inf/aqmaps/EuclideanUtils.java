package uk.ac.ed.inf.aqmaps;

public class EuclideanUtils {

    
    /* Private constructor that prevents this class from being instantiated */
    private EuclideanUtils() {
    }
    
    public static double computeDistance(CoordinatePair a, CoordinatePair b) {
        return Math.sqrt(Math.pow(a.getLongitude()-b.getLongitude(), 2)+
                Math.pow(a.getLatitude()-b.getLatitude(), 2));
    }

    /*
     * This method returns a VariableCoordinatePair as opposed to a Fixed one - which is intuitive, because
     * for there to be a next position, there had to be a first position. But because Position fields
     * in this program are generally final by design (position changes happen by setting the attributes
     * of a VariableCoordinatePair object), this does not make an actual difference. 
     */
    public static VariableCoordinatePair getNextPosition(CoordinatePair position, int angle, double distance) {
        
        double newLong = position.getLongitude() + distance * Math.cos(angle);
        double newLat = position.getLatitude() + distance * Math.sin(angle);
        
        return new VariableCoordinatePair(newLong, newLat);
    }
    
}
