package uk.ac.ed.inf.aqmaps.application;

/**
 * This small class captures the input arguments given to the application by its
 * user.
 */
public class AppStartInfo {

    /*
     * The day, month and year that together decide what tour our drone embarks on.
     * 
     * Note: While it would look nice to use a LocalDate object instead of 3
     * integers, that would actually be less practical for our purposes. We just
     * need the integers to find the right sensor file.
     */
    private int day, month, year;

    /*
     * I made a similar decision for longitude and latitude, as opposed to a Point.
     */
    private double droneStartLongitude;
    private double droneStartLatitude;

    /*
     * The random seed that our TSP algorithm could use for reproducible results
     * although it does not need one in its current version.
     */
    private int seed;

    /*
     * The port that is used to connect to our file server.
     */
    private int port;

    /**
     * The constructor of the AppStartInfo class.
     * 
     * @param day       the day of month of the drone's journey
     * @param month     the month of the drone's journey
     * @param year      the year of the drone's journey
     * @param latitude  the latitude the drone starts at
     * @param longitude the longitude the drone starts at
     * @param seed      a random seed that could be used for reproducibility if any
     *                  random operations were needed
     * @param port      the port needed to connect to the file server related to
     *                  this program
     */
    public AppStartInfo(int day, int month, int year, double latitude, double longitude, int seed, int port) {
        this.day = day;
        this.month = month;
        this.year = year;
        this.droneStartLongitude = longitude;
        this.droneStartLatitude = latitude;
        this.seed = seed;
        this.port = port;
    }

    /* Getters */

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
