package uk.ac.ed.inf.aqmaps;

/*
 * TODO: Remove class if not needed
 */
public abstract class FixedMapObject {

    private final FixedCoordinatePair position;
    
    protected FixedMapObject(double longitude, double latitude) {
        this.position = new FixedCoordinatePair(longitude, latitude);
    }
    
    public CoordinatePair getPosition() {
        return position;
    }
    
}
