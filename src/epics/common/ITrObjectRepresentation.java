package epics.common;

import java.util.List;

import epics.camsim.core.Location;

/**
 *
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public interface ITrObjectRepresentation {

    public List<Double> getFeatures();
    public void setPrice(double price);
    public double getPrice();
    public Location getLocation();
    public Location getCenterBasedLocation();
}
