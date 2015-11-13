package epics.common;

import java.util.List;

import epics.camsim.core.Location;

/**
 *
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public interface ITrObjectRepresentation {

    /**
     * returns the features of the object (unique identifier)
     * @return
     */
    public List<Double> getFeatures();
    
    /**
     * price to be paid for object
     * @param price
     */
    public void setPrice(double price);
    
    /**
     * price to be paid for object
     * @return
     */
    public double getPrice();
    
    /**
     * location of object
     * @return
     */
    public Location getLocation();
    
    /**
     * location of object based on center of simulation
     * @return
     */
    public Location getCenterBasedLocation();
}
