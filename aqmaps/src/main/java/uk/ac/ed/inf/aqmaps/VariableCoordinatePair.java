package uk.ac.ed.inf.aqmaps;

public class VariableCoordinatePair extends CoordinatePair {

    public VariableCoordinatePair(double longitude, double latitude) {
        super(longitude, latitude);
    }
    
    private void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    
    private void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    
    public void setPosition(CoordinatePair newPos) {
        this.setLongitude(newPos.getLongitude());
        this.setLatitude(newPos.getLatitude());
    }

}
