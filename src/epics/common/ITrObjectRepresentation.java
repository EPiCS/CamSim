package epics.common;

import java.util.List;

import epics.camsim.core.TraceableObject;

/**
 *
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public interface ITrObjectRepresentation {

    public List<Double> getFeatures();
    public void setPrice(double price);
    public double getPrice();
    public TraceableObject getTraceableObject();
}
