package epics.ai;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import epics.camsim.core.CameraController;
import epics.camsim.core.TraceableObject;
import epics.camsim.core.TraceableObjectRepresentation;
import epics.common.AbstractAINode;
import epics.common.ICameraController;
import epics.common.IRegistration;
import epics.common.ITrObjectRepresentation;

/** An AI node which takes into account historical trends in objects'
 * appearance and time steps seen for. 
 * 
 * This class is not the AI node itself, but a container for historical
 * nodes, which are nested within. This is necessary due to these historical
 * nodes needing to extend different parents. The nested classes themselves
 * do not maintain state or logic, though. That is done in this class.
 * 
 * PLEASE NOTE THAT NESTED CLASSES ARE IDENTICAL SIBLINGS
 * AND CHANGES IN EITHER ONE MUST BE REPLICATED IN THE OTHER.
 * THIS IS UGLY BUT NECESSARY GIVEN THE ARCHITECTURE. 
 * IF THERE ARE DIFFERENCES BETWEEN THE TWO OTHER THAN THE CLASS
 * BEING EXTENDED, PLEASE RECTIFY THEM SOONER RATHER THAN LATER! 
 * 
 * All state must be maintained in HistoricalAINode and not in the children. */
public class HistoricalAINode {

	/** Whether to display debug msgs about historical positions/bidding */
    private static boolean DEBUG_HIST = true;
    public static final String KEY_DEBUG_HIST = "DebugHist";
	
	/** If we have never seen an object before, we don't have an avgTS, so we 
	 * cannot calculate the bid coefficient. This is the default in that case. */
	public static final double DEFAULT_PRE_INSTANTIATION_BID_COEFFICIENT = 4.0;
	public static final String KEY_PRE_INSTANTIATION_BID_COEFFICIENT = "PreInstantiationBidCoefficient";
	private double preInstantiationBidCoefficient;
	
	/** If the avgTS is 5 but an object is still around after 10 TS, the bid
	 * coefficient is not calculable by the standard formula, so use this default */
    public static final double DEFAULT_OVERSTAY_BID_COEFFICIENT = 8.0;
	public static final String KEY_OVERSTAY_BID_COEFFICIENT = "OverstayBidCoefficient"; 
    private double overstayBidCoefficient;
	
	public static class Active extends ActiveAINodeMulti {
		private HistoricalAINode histNode;
		
	    // Overriding AbstractAINode's constructor
	    public Active(int comm, boolean staticVG, 
	    		Map<String, Double> vg, IRegistration r){
	    	this(comm, staticVG, vg, r, 
	    			DEFAULT_PRE_INSTANTIATION_BID_COEFFICIENT,
	    			DEFAULT_OVERSTAY_BID_COEFFICIENT); // Goes through to instantiateAINode()
	    }
	    
	    public Active(int comm, boolean staticVG, 
	    		Map<String, Double> vg, IRegistration r,
	    		double preInstantiationBidCoefficient, double overstayBidCoefficient){
	    	super(comm, staticVG, vg, r); // Goes through to instantiateAINode()
	    	histNode = new HistoricalAINode(
	    			preInstantiationBidCoefficient, overstayBidCoefficient);
	    }

	    @Override
	    public void update() {
	    	// Store current point and get rid of an old one if necessary
	    	histNode.updateHistoricalPoints(this.camController);
	    	super.update();
	    }
		
		@Override
		/** The bid for an object. Not the same as 'confidence' */
		public double calculateValue(ITrObjectRepresentation target) {
			double superValue = super.calculateValue(target);
			return histNode.calculateValue(target, superValue);
		}
		
		@Override
		/** See {@link AbstractAINode#setParam(String, String)} */
		public boolean setParam(String key, String value) {
			if (super.setParam(key, value)) return true; 
			return histNode.setParam(key, value);
		}
	}
	
	public static class Passive extends PassiveAINodeMulti {
		private HistoricalAINode histNode;
		
		// Overriding AbstractAINode's constructor
	    public Passive(int comm, boolean staticVG, 
	    		Map<String, Double> vg, IRegistration r){
	    	this(comm, staticVG, vg, r,
	    			DEFAULT_PRE_INSTANTIATION_BID_COEFFICIENT,
	    			DEFAULT_OVERSTAY_BID_COEFFICIENT); // Goes through to instantiateAINode()
	    }
	    
	    public Passive(int comm, boolean staticVG, 
	    		Map<String, Double> vg, IRegistration r,
	    		double preInstantiationBidCoefficient, double overstayBidCoefficient){
	    	super(comm, staticVG, vg, r); // Goes through to instantiateAINode()
	    	histNode = new HistoricalAINode(
	    			preInstantiationBidCoefficient, overstayBidCoefficient);
	    }

	    @Override
	    public void update() {
	    	// Store current point and get rid of an old one if necessary
	    	histNode.updateHistoricalPoints(this.camController);
	    	super.update();
	    }
		
		@Override
		/** The bid for an object. Not the same as 'confidence' */
		public double calculateValue(ITrObjectRepresentation target) {
			double superValue = super.calculateValue(target);
			return histNode.calculateValue(target, superValue);
		}
		
		@Override
		/** See {@link AbstractAINode#setParam(String, String)} */
		public boolean setParam(String key, String value) {
			if (super.setParam(key, value)) return true; 
			return histNode.setParam(key, value);
		}
	}
	
	/** Where this object has been over the last few timesteps */
    private Map<ITrObjectRepresentation, LinkedList<Point2D.Double>> historicalLocations = 
    		new HashMap<ITrObjectRepresentation, LinkedList<Point2D.Double>>();
    
	/** How many timesteps in total objects are visible for (divide by counter for avg) */
	private double totalTSVisible = 0;
	/** How many objects have left our FOV (avg = totalTSVisible/tsVisibleCounter) */
	private int tsVisibleCounter = 0;

    /** Categories are split into evenly sized segments of angle from south (-180 degrees),
     * clockwise, all the way round back to south (180 degrees). So with two 
     * categories anything from -180 degrees to 0 degrees is category 1, anything 
     * 0 to 180 is category 2.
     * With three categories, anything -180 to -60 is category 1, anything -60 to 60
     * is category 2, anything 60 to 180 is category 3.  */
    public static final int OBJ_CATEGORIES = 2;
    
	public HistoricalAINode(double preInstantiationBidCoefficient, 
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
				
				getCategoryForObject(itro, (CameraController)camController);
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
	
	/** Given an object, look at past two time steps of movement and 
	 * categorise based on the object's direction.
	 * See information around the categories field for information on what
	 * each category means.  */
	public int getCategoryForObject(ITrObjectRepresentation obj, CameraController camController) {
		LinkedList<Point2D.Double> pointsForObject = historicalLocations.get(obj);

		// If new object
		if (pointsForObject == null || pointsForObject.size() < 2) {
			return 0;
		}

		Point2D.Double p1 = pointsForObject.get(pointsForObject.size()-2);
		Point2D.Double p2 = pointsForObject.get(pointsForObject.size()-1);
		double camHeadingDegrees = Math.toDegrees(camController.getHeading());
		double heading = getObjectHeading(p1, p2, camHeadingDegrees);
		
		double degPerCategory = 360.0/(double)OBJ_CATEGORIES;
		double posHeading = heading + 180.0;
		int category = (int)(posHeading / degPerCategory) + 1;
		
		// Special case. Loops back round to 1 to ensure -180 and +180
		// are put in the same category
		if (posHeading == 360.0) { category = 1; }
		
		if (category < 1 || category > OBJ_CATEGORIES) {
			throw new IllegalStateException("Categorisation failed."+
					"There were "+OBJ_CATEGORIES+" categories, and "+degPerCategory+
					" degrees per category. Heading was "+heading+" and category"+
					" was computed as: "+category);
		}
		
		System.out.println("Heading for object: "+heading+", category: "+category);
		return category;
	}
	
	/** Given two points representing the positions of the object over the 
	 * last two time steps, get the heading of the object's trajectory (in degrees).
	 * This is relative to the camera with the given angle (in degrees) */
	public static double getObjectHeading(Point2D.Double p1, Point2D.Double p2, 
			double cameraHeading) {
		double heading = getAngleFromOtherPoint(p1.x, p1.y, p2.x, p2.y);
		//heading += cameraHeading; // Make angle relative to camera
		if (heading > 180) {
			heading = -180 + (heading % 180); 
		} else if (heading < -180) {
			heading += 360;
		}
		return heading;
	}
	
	/** Computes the angle (degrees) of x2,y2 using x1,y1 as the origin. This uses
	 * the arctangent, so angle goes from -180 to 180 */
	public static double getAngleFromOtherPoint(double x1, double y1, double x2, double y2) {
		double radians = Math.atan2(x2-x1, y2-y1);
		double degrees = Math.toDegrees(radians);
		return degrees;
	}
	
	/** Applies params for general Hist-based functions. Used by nested classes.
	 * See {@link AbstractAINode#setParam(String, String)} */
	public boolean setParam(String key, String value) {
		if (KEY_OVERSTAY_BID_COEFFICIENT.equalsIgnoreCase(key)) {
			overstayBidCoefficient = Double.parseDouble(value);
			System.out.println("OverstayBidCoefficient set to: "+overstayBidCoefficient);
			return true;
		} else if (KEY_PRE_INSTANTIATION_BID_COEFFICIENT.equalsIgnoreCase(key)) {
			preInstantiationBidCoefficient = Double.parseDouble(value);
			System.out.println("PreInstantiationBidCoefficient set to: "+preInstantiationBidCoefficient);
			return true;
		} else if (KEY_DEBUG_HIST.equalsIgnoreCase(key)) {
			DEBUG_HIST = Boolean.parseBoolean(value);
			System.out.println("DebugHist set to: "+DEBUG_HIST);
			return true;
		} else {
			System.err.println("Didn't recognise key: "+key);
			return false;
		}
	}
}
