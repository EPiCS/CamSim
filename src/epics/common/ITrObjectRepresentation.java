package epics.common;

import java.util.List;

/**
 *
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public interface ITrObjectRepresentation {

    public List<Double> getFeatures();
    public void setPrice(double price);
    public double getPrice();
}
