package epics.ai;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import epics.camsim.core.TraceableObject;
import epics.camsim.core.TraceableObjectRepresentation;
import epics.common.ICameraController;
import epics.common.ITrObjectRepresentation;

public class HistoricalNodeHelper {

	/** Whether to display debug msgs about historical positions/bidding */
    private static boolean DEBUG_HIST = false;
	
	/** Where this object has been over the last few timesteps */
    private Map<ITrObjectRepresentation, LinkedList<Point2D.Double>> historicalLocations = 
    		new HashMap<ITrObjectRepresentation, LinkedList<Point2D.Double>>();
    
	/** How many timesteps in total objects are visible for (divide by counter for avg) */
	private double totalTSVisible = 0;
	/** How many objects have left our FOV (avg = totalTSVisible/tsVisibleCounter) */
	private int tsVisibleCounter = 0;

	/** If we have never seen an object before, we don't have an avgTS, so we 
	 * cannot calculate the bid coefficient. This is the default in that case. */
	public static final double DEFAULT_PRE_INSTANTIATION_BID_COEFFICIENT = 0.2;
	public static final String KEY_PRE_INSTANTIATION_BID_COEFFICIENT = "PreInstantiationBidCoefficient";
	private double preInstantiationBidCoefficient;
	
	/** If the avgTS is 5 but an object is still around after 10 TS, the bid
	 * coefficient is not calculable by the standard formula, so use this default */
    public static final double DEFAULT_OVERSTAY_BID_COEFFICIENT = 2.8;
	public static final String KEY_OVERSTAY_BID_COEFFICIENT = "OverstayBidCoefficient"; 
    private double overstayBidCoefficient;
	
	public HistoricalNodeHelper(double preInstantiationBidCoefficient, 
			double overstayBidCoefficient) {
		this.preInstantiationBidCoefficient = preInstantiationBidCoefficient;
		this.overstayBidCoefficient = overstayBidCoefficient;
	}
	
	/** Store the current point for each visible object. This allows
	 *  bids to be made later based on where the point has been. 
	 *  Also updates how many timesteps this object has been visible for. */
	protected void updateHistoricalPoints(ICameraController camController) {
		// For every object, check for points. If nothing there, create a list first.
		for(ITrObjectRepresentation itro : camController.getVisibleObjects_bb().keySet()) {
			TraceableObjectRepresentation tor = (TraceableObjectRepresentation) itro;
			TraceableObject object = tor.getTraceableObject();
			LinkedList<Point2D.Double> pointsForObject = historicalLocations.get(itro);

			// If new object
			if (pointsForObject == null) {
				pointsForObject = new LinkedList<Point2D.Double>();
				historicalLocations.put(itro, pointsForObject);
			}

			Point2D.Double point = new Point2D.Double(object.getX(), object.getY());
			pointsForObject.add(point);

			if (DEBUG_HIST) {
				System.out.println("\tPoints for object "+tor.getFeatures()+" are: ");
				for (Point2D.Double curPoint : pointsForObject) {
					System.out.println("\t\t"+curPoint);
				}
				System.out.println("\tQOM for object "+tor.getFeatures()+" is: "+getQuantityOfMovement(itro));
			}
		}
		
		// If previously seen objects have disappeared, count their timesteps and remove
		Iterator<ITrObjectRepresentation> iter = historicalLocations.keySet().iterator();
		while (iter.hasNext()) {
			ITrObjectRepresentation itro = iter.next();
			if (! camController.getVisibleObjects_bb().containsKey(itro)) {
				// Object disappeared. Grab visible timesteps
				int visibleTS = historicalLocations.get(itro).size();
				this.totalTSVisible += visibleTS;
				this.tsVisibleCounter++;
				iter.remove(); // Object is accounted for and not in view any more
				
				if (DEBUG_HIST) {
					System.out.println("\tObject "+itro.getFeatures()+" lost by "+camController.getName()+". ");
					System.out.println("\tTS visible: "+visibleTS+" and current avg: "+getAvgVisibleTS()
							+" ("+tsVisibleCounter+" objects seen so far)");
				}
			}
		}
	}

	/** The average number of timesteps objects remain 
	 * visible for in this camera */
	private Double getAvgVisibleTS() {
		Double avg = totalTSVisible/tsVisibleCounter;
		return Double.isNaN(avg) ? null : avg;
	}
	
	private Double getQuantityOfMovement(ITrObjectRepresentation itro) {
		LinkedList<Point2D.Double> pointsForObject = historicalLocations.get(itro);
		double totalDistance = 0;

		// Not enough points to get a speed
		if (pointsForObject.size() < 2) {
			return null;
		}
		for (int i = 0; i < pointsForObject.size() - 1; i++) {
			Point2D.Double curPoint = pointsForObject.get(i);
			Point2D.Double nextPoint = pointsForObject.get(i+1);
			totalDistance += curPoint.distance(nextPoint);
		}
		// Mean of the distances between successive points, 
		// i.e. the mean speed between timesteps
		return totalDistance / ((double)pointsForObject.size()-1.0);
	}
    
	/** The bid for an object. Not the same as 'confidence'.
	 * @param superValue The value obtained from the base node's 
	 * 					 parent class when calling calculateValue() */
	public double calculateValue(ITrObjectRepresentation target, double superValue) {
		// Formula is bid = (avgTS-TSsoFar) * confidence
		// where avgTS is the average timesteps an object is present for
		// and TSsoFar is how many timesteps this object has been in view for
		Double avgTS = this.getAvgVisibleTS();
		double bidCoefficient;
		if (avgTS == null || historicalLocations.get(target) == null) {
			// Null means we haven't seen an object leave yet, default to this
			bidCoefficient = this.preInstantiationBidCoefficient;
		} else {
			double tsSoFar = historicalLocations.get(target).size();
			double diff = avgTS-tsSoFar;
			if (diff > 0) {
				bidCoefficient = diff;
			} else {
				// Default value in case avgTS-tsSoFar is negative. See FYP(Overstay Problem)
				bidCoefficient = this.overstayBidCoefficient;
			}
		}
		
		return superValue * bidCoefficient;
	}
	
	public boolean setParam(String key, String value) {
		if (KEY_OVERSTAY_BID_COEFFICIENT.equalsIgnoreCase(key)) {
			overstayBidCoefficient = Double.parseDouble(value);
			System.out.println("OverstayBidCoefficient set to: "+overstayBidCoefficient);
			return true;
		} else if (KEY_PRE_INSTANTIATION_BID_COEFFICIENT.equalsIgnoreCase(key)) {
			preInstantiationBidCoefficient = Double.parseDouble(value);
			System.out.println("PreInstantiationBidCoefficient set to: "+preInstantiationBidCoefficient);
			return true;
		} else {
			System.err.println("Didn't recognise key: "+key);
			return false;
		}
	}
}
