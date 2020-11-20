package uk.ac.ed.inf.aqmaps;

public class AppStartInfo {

    /*
     * While it would look nice to use a LocalDate object instead of 3 integers,
     * that would actually be less practical for our purposes. We just need the
     * integers to find the right sensor file.
     */
    private int day;
    private int month;
    private int year;
    
    /*
     * The same goes for longitude and latitude, as opposed to a VariableCoordinatePair object. 
     * I find it safer to pass each relevant constructor longitude and latitude separately and
     * decide "locally" whether the Location is Fixed or Variable. This way it should be easier to 
     * make changes to the code in the future. 
     */
    private double droneStartLongitude;
    private double droneStartLatitude;
    
    private int seed;
    private int port;

    public AppStartInfo(int day, int month, int year, double latitude, double longitude, int seed, int port) {
        this.day = day;
        this.month = month;
        this.year = year;
        this.droneStartLongitude = longitude;
        this.droneStartLatitude = latitude;
        this.seed = seed;
        this.port = port;
    }

    public int getSeed() {
        return seed;
    }

    public int getPort() {
        return port;
    }

    public double getDroneStartLongitude() {
        return droneStartLongitude;
    }

    public double getDroneStartLatitude() {
        return droneStartLatitude;
    }

    public int getDay() {
        return day;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

}
