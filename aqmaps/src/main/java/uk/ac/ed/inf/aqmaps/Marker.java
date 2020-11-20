package uk.ac.ed.inf.aqmaps;

public class Marker {

    private String colour;
    private Symbol symbol;
    
    public Marker(String colour, Symbol symbol) {
        this.colour = colour;
        this.symbol = symbol;
    }

    // TODO: Map Symbol to actual symbol from Maki Icon set 
    
    public String getColour() {
        return colour;
    }

    public Symbol getSymbol() {
        return symbol;
    }
    
}
