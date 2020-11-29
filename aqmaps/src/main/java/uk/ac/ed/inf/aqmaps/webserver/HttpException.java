package uk.ac.ed.inf.aqmaps.webserver;

public class HttpException extends Exception {

    /**
     * All exception classes in Java implement the Serializable interface and should
     * therefore have a unique identifier.
     */
    private static final long serialVersionUID = 407776863741608239L;
    
    private int statusCode;

    public HttpException(int statusCode) {
        super("Http error with status code " + statusCode);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

}
