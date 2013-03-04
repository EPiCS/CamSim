package epics.ai;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

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
    private static boolean DEBUG_HIST = false;
    public static final String KEY_DEBUG_HIST = "DebugHist";
	
	/** If we have never seen an object before, we don't have an avgTS, so we 
	 * cannot calculate the bid coefficient. This is the default in that case. */
	public static final double DEFAULT_PRE_INSTANTIATION_BID_COEFFICIENT = 20.0;
	public static final String KEY_PRE_INSTANTIATION_BID_COEFFICIENT = "PreInstantiationBidCoefficient";
	private double preInstantiationBidCoefficient = DEFAULT_PRE_INSTANTIATION_BID_COEFFICIENT;
	
	/** If the avgTS is 5 but an object is still around after 10 TS, the bid
	 * coefficient is not calculable by the standard formula, so use this default */
    public static final double DEFAULT_OVERSTAY_BID_COEFFICIENT = 0.0;
	public static final String KEY_OVERSTAY_BID_COEFFICIENT = "OverstayBidCoefficient"; 
    private double overstayBidCoefficient = DEFAULT_OVERSTAY_BID_COEFFICIENT;
	
    public static final boolean DEFAULT_CLASSIFICATION_ENABLED = true;
	public static final String KEY_CLASSIFICATION_ENABLED = "ClassificationEnabled"; 
    private boolean classificationEnabled = DEFAULT_CLASSIFICATION_ENABLED;
	
    /** 
     * Classification must be enabled for this to have effect.
     * Categories are split into evenly sized segments of angle from south (-180 degrees),
     * clockwise, all the way round back to south (180 degrees). So with two 
     * categories anything from -180 degrees to 0 degrees is category 1, anything 
     * 0 to 180 is category 2.
     * With three categories, anything -180 to -60 is category 1, anything -60 to 60
     * is category 2, anything 60 to 180 is category 3.  */
    public static final int DEFAULT_OBJ_CATEGORIES = 4;
    public static final String KEY_OBJ_CATEGORIES = "ObjectCategories";
    public int objCategories = DEFAULT_OBJ_CATEGORIES;
	
    /** Whether history is taken on a per-category basis for each camera 
     * (rather than a per-camera basis). This means, for example, that we 
     * keep separate values for average number of time steps for objects 
     * depending on the category the object is in. 
     * Note that classification must be enabled for this feature to work */
    public static final boolean DEFAULT_HIST_PER_CATEGORY = true;
	public static final String KEY_HIST_PER_CATEGORY_ENABLED = "HistPerCategoryEnabled"; 
	private boolean histPerCategoryEnabled = DEFAULT_HIST_PER_CATEGORY;
	
	/** Whether hist-based bidding is used */ 
    public static final boolean DEFAULT_HIST_ENABLED = true; 
	public static final String KEY_HIST_ENABLED = "HistEnabled"; 
	private boolean histEnabled = DEFAULT_HIST_ENABLED;
	    
	public static class Active extends ActiveAINodeMulti {
		private HistoricalAINode histNode;
		
		// Overriding AbstractAINode's constructor
	    public Active(int comm, boolean staticVG, 
	    		Map<String, Double> vg, IRegistration r){
	    	super(comm, staticVG, vg, r); // Goes through to instantiateAINode()
	    	histNode = new HistoricalAINode();
	    }

	    @Override
	    public void update() {
	    	// Store current point and get rid of an old one if necessary
	    	histNode.updateHistoricalPoints(this.camController);
	    	super.update();
	    }
		
		@Override
		/** The bid for an object. Not the same as 'confidence' */
		public double calculateValue(ITrObjectRepresentation itro) {
			double superValue = super.calculateValue(itro);
			return histNode.calculateValue(itro, superValue);
		}
		
		@Override
		/** See {@link AbstractAINode#setParam(String, String)} */
		public boolean setParam(String key, String value) {
			if (super.setParam(key, value)) return true; 
			return histNode.setParam(key, value);
		}
		
		@Override
		public Map<String, Double> getDrawableVisionGraph() {
			if (histNode.classificationEnabled) {
				return histNode.getDrawableVisionGraph();
			} else {
				return super.getDrawableVisionGraph();
			}
		}
		
	    @Override
	    public boolean vgContainsKey(String camName, ITrObjectRepresentation itro) { 
	    	if (histNode.classificationEnabled) {
	    		return histNode.vgContainsKey(camName, itro);	
			} else {
				return super.vgContainsKey(camName, itro);
			}
	    }
	    
	    @Override
	    public Collection<Double> vgGetValues(ITrObjectRepresentation itro) {
	    	if (histNode.classificationEnabled) {
	    		return histNode.vgGetValues(itro);
	    	} else {
	    		return super.vgGetValues(itro);
	    	}
	    }
	    
	    @Override
	    public Set<String> vgGetCamSet() {
	    	if (histNode.classificationEnabled) {
	    		return histNode.vgGetCamSet();
	    	} else {
	    		return super.vgGetCamSet();
	    	}
	    }
	    
	    @Override
	    public Double vgGet(String camName, ITrObjectRepresentation itro) {
	    	if (histNode.classificationEnabled) {
	    		return histNode.vgGet(camName, itro);
	    	} else {
	    		return super.vgGet(camName, itro);
	    	}
	    }
	    
	    @Override
	    public void strengthenVisionEdge(String destinationName, ITrObjectRepresentation itro) {
	    	if (histNode.classificationEnabled) {
	    		histNode.strengthenVisionEdge(destinationName, itro);
	    	} else {
	    		super.strengthenVisionEdge(destinationName, itro);
	    	}
	    }
	    
	    @Override
	    public void updateVisionGraph() {
	    	if (histNode.classificationEnabled) {
	    		histNode.updateVisionGraph(EVAPORATIONRATE);
	    	} else {
	    		super.updateVisionGraph();
	    	}
	    }
	}
	
	public static class Passive extends PassiveAINodeMulti {
		private HistoricalAINode histNode;
		
		// Overriding AbstractAINode's constructor
	    public Passive(int comm, boolean staticVG, 
	    		Map<String, Double> vg, IRegistration r){
	    	super(comm, staticVG, vg, r); // Goes through to instantiateAINode()
	    	histNode = new HistoricalAINode();
	    }
	    
	    @Override
	    public void update() {
	    	// Store current point and get rid of an old one if necessary
	    	histNode.updateHistoricalPoints(this.camController);
	    	super.update();
	    }
		
		@Override
		/** The bid for an object. Not the same as 'confidence' */
		public double calculateValue(ITrObjectRepresentation itro) {
			double superValue = super.calculateValue(itro);
			return histNode.calculateValue(itro, superValue);
		}
		
		@Override
		/** See {@link AbstractAINode#setParam(String, String)} */
		public boolean setParam(String key, String value) {
			if (super.setParam(key, value)) return true; 
			return histNode.setParam(key, value);
		}
		
		@Override
		public Map<String, Double> getDrawableVisionGraph() {
			if (histNode.classificationEnabled) {
				return histNode.getDrawableVisionGraph();
			} else {
				return super.getDrawableVisionGraph();
			}
		}
		
	    @Override
	    public boolean vgContainsKey(String camName, ITrObjectRepresentation itro) { 
	    	if (histNode.classificationEnabled) {
	    		return histNode.vgContainsKey(camName, itro);	
			} else {
				return super.vgContainsKey(camName, itro);
			}
	    }
	    
	    @Override
	    public Collection<Double> vgGetValues(ITrObjectRepresentation itro) {
	    	if (histNode.classificationEnabled) {
	    		return histNode.vgGetValues(itro);
	    	} else {
	    		return super.vgGetValues(itro);
	    	}
	    }
	    
	    @Override
	    public Set<String> vgGetCamSet() {
	    	if (histNode.classificationEnabled) {
	    		return histNode.vgGetCamSet();
	    	} else {
	    		return super.vgGetCamSet();
	    	}
	    }
	    
	    @Override
	    public Double vgGet(String camName, ITrObjectRepresentation itro) {
	    	if (histNode.classificationEnabled) {
	    		return histNode.vgGet(camName, itro);
	    	} else {
	    		return super.vgGet(camName, itro);
	    	}
	    }
	    
	    @Override
	    public void strengthenVisionEdge(String destinationName, ITrObjectRepresentation itro) {
	    	if (histNode.classificationEnabled) {
	    		histNode.strengthenVisionEdge(destinationName, itro);
	    	} else {
	    		super.strengthenVisionEdge(destinationName, itro);
	    	}
	    }
	    
	    @Override
	    public void updateVisionGraph() {
	    	if (histNode.classificationEnabled) {
	    		histNode.updateVisionGraph(EVAPORATIONRATE);
	    	} else {
	    		super.updateVisionGraph();
	    	}
	    }
	}
	
	/** Where this object has been over the last few timesteps */
    private Map<ITrObjectRepresentation, LinkedList<Point2D.Double>> historicalLocations = 
    		new HashMap<ITrObjectRepresentation, LinkedList<Point2D.Double>>();
    
	/** How many timesteps in total objects are visible for (divide by counter for avg) */
	private double totalTSVisible = 0;
	/** How many objects have left our FOV (avg = totalTSVisible/tsVisibleCounter) */
	private int tsVisibleCounter = 0;

	/** How many timesteps in total for which objects in this category are 
	 * visible (divide by counter for avg) */
	private HashMap<Integer, Double> totalTSVisibleForCat = new HashMap<Integer, Double>();
	/** How many objects in this cat have left our FOV 
	 * (avg = totalTSVisible/tsVisibleCounter) */
	private HashMap<Integer, Integer> tsVisibleCounterForCat = new HashMap<Integer, Integer>();
	
	private Map<String, Map<Integer, Double>> visionGraph = new HashMap<String, Map<Integer, Double>>();
	
	public HistoricalAINode() { /* Nothing here */ }
	
	/** Store the current point for each visible object. This allows
	 *  bids to be made later based on where the point has been. 
	 *  Also updates how many timesteps this object has been visible for. */
	protected void updateHistoricalPoints(ICameraController camController) {
		// For every object, check for points. If nothing there, create a list first.
		for(ITrObjectRepresentation itro : camController.getVisibleObjects_bb().keySet()) {
			TraceableObjectRepresentation tor = (TraceableObjectRepresentation) itro;
			TraceableObject object = tor.getTraceableObject();
			LinkedList<Point2D.Double> pointsForObject = historicalLocations.get(itro);

			// If new object, start tracking its co-ordinates
			if (pointsForObject == null) {
				pointsForObject = new LinkedList<Point2D.Double>();
				historicalLocations.put(itro, pointsForObject);
			}

			Point2D.Double point = new Point2D.Double(object.getX(), object.getY());
			pointsForObject.add(point);

			if (DEBUG_HIST) {
				System.out.println("\tPoints for object "+tor.getFeatures()+" are: ");
				for (Point2D.Double curPoint : pointsForObject) {
					System.out.printf("\t\t[%.3f, %.3f]\n",curPoint.x,curPoint.y);
				}
				//System.out.println("\tQOM for object "+tor.getFeatures()+" is: "+getQuantityOfMovement(itro));
				
				getCategoryForObject(itro);
			}
		}
		
		// If previously seen objects have disappeared, count their timesteps and remove
		Iterator<ITrObjectRepresentation> iter = historicalLocations.keySet().iterator();
		while (iter.hasNext()) {
			ITrObjectRepresentation itro = iter.next();
			if (! camController.getVisibleObjects_bb().containsKey(itro)) {
				// Object disappeared. Grab visible timesteps
				int visibleTS = historicalLocations.get(itro).size();
				
				// Totals for cam overall
				this.totalTSVisible += visibleTS;
				this.tsVisibleCounter++;
				
				// Totals for category
				if (histPerCategoryEnabled) {
					int category = getCategoryForObject(itro);
					Double curTSVisibleForCat = this.totalTSVisibleForCat.get(category);
					if (curTSVisibleForCat == null) {
						curTSVisibleForCat = 0.0;
					}
					curTSVisibleForCat += visibleTS;
					this.totalTSVisibleForCat.put(category, curTSVisibleForCat);

					Integer curTSVisibleCounterForCat = tsVisibleCounterForCat.get(category);
					if (curTSVisibleCounterForCat == null) {
						curTSVisibleCounterForCat = 0;
					}
					curTSVisibleCounterForCat++;
					this.tsVisibleCounterForCat.put(category, curTSVisibleCounterForCat);
					
					if (DEBUG_HIST) {
						System.out.println("\tTS visible for cat "+category+": "+curTSVisibleForCat+
								" and current avg for cat: "+getAvgVisibleTSForCat(category));
					}
				} else {
					if (DEBUG_HIST) {
						System.out.println("\tTS visible: "+visibleTS+" and current avg: "+getAvgVisibleTS()
								+" ("+tsVisibleCounter+" objects seen so far)");
					}					
				}
				if (DEBUG_HIST) {
					System.out.println("\tObject "+itro.getFeatures()+" lost by "+camController.getName()+". ");
				}
				
				iter.remove(); // Object is accounted for and not in view any more
			}
		}
	}

	/** The average number of timesteps objects remain 
	 * visible for in this camera
	 * If no objects have been seen, returns null. */
	private Double getAvgVisibleTS() {
		Double avg = totalTSVisible/tsVisibleCounter;
		return Double.isNaN(avg) ? null : avg;
	}
	
	/** The average number of timesteps for which objects in this category 
	 * have remained visible in this camera.
	 * If no objects have been seen in this category, returns null. */
	private Double getAvgVisibleTSForCat(int category) {
		if (! this.histPerCategoryEnabled){
			throw new IllegalStateException("Trying to get avgVisibleTSForCat " +
					"without enabling the histPerCategory feature");
		}
		Double curTSVisibleForCat = this.totalTSVisibleForCat.get(category);
		Integer curTSVisibleCounterForCat = tsVisibleCounterForCat.get(category);
		if (curTSVisibleForCat == null || curTSVisibleCounterForCat == null) {
			return null;
		}

		Double avg = curTSVisibleForCat/curTSVisibleCounterForCat;
		return Double.isNaN(avg) ? null : avg;
	}
	
	/** Convenience method for {@link this#getAvgVisibleTSForCat(int)} 
	 * which retrieves the category from the ITRO. */
	private Double getAvgVisibleTSForObject(ITrObjectRepresentation itro) {
		int cat = this.getCategoryForObject(itro);
		return getAvgVisibleTSForCat(cat);
	}
	
	@SuppressWarnings("unused")
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
	public double calculateValue(ITrObjectRepresentation itro, double superValue) {
		// Formula is bid = (avgTS-TSsoFar) * confidence
		// where avgTS is the average timesteps an object is present for
		// and TSsoFar is how many timesteps this object has been in view for
		Double avgTS = null;
		if (histPerCategoryEnabled) {
			avgTS = this.getAvgVisibleTSForObject(itro);
		} else {
			avgTS = this.getAvgVisibleTS();
		}
		double bidCoefficient;
		if (avgTS == null || historicalLocations.get(itro) == null) {
			// Null means we haven't seen an object leave yet, default to this
			bidCoefficient = this.preInstantiationBidCoefficient;
		} else {
			double tsSoFar = historicalLocations.get(itro).size();
			double diff = avgTS-tsSoFar;
			if (diff > 0) {
				bidCoefficient = diff;
			} else {
				// Default value in case avgTS-tsSoFar is negative. See FYP(Overstay Problem)
				bidCoefficient = this.overstayBidCoefficient;
			}
		}
		
		if (histEnabled) {
			return superValue * bidCoefficient;
		} else {
			return superValue;
		}
	}
	
	/** Given an object, look at first two time steps of movement and 
	 * categorise based on the object's direction.
	 * See information around the categories field for information on what
	 * each category means.  */
	public int getCategoryForObject(ITrObjectRepresentation itro) {
		LinkedList<Point2D.Double> pointsForObject = historicalLocations.get(itro);

		// If new object
		if (pointsForObject == null || pointsForObject.size() < 2) {
			return 0;
		}

		Point2D.Double p1 = pointsForObject.get(0);
		Point2D.Double p2 = pointsForObject.get(1);
		//double camHeadingDegrees = Math.toDegrees(camController.getHeading());
		double heading = getObjectHeading(p1, p2);
		
		double degPerCategory = 360.0/(double)objCategories;
		double posHeading = heading + 180.0;
		int category = (int)(posHeading / degPerCategory) + 1;
		
		// Special case. Loops back round to 1 to ensure -180 and +180
		// are put in the same category
		if (posHeading == 360.0) { category = 1; }
		
		if (category < 1 || category > objCategories) {
			throw new IllegalStateException("Categorisation failed."+
					"There were "+objCategories+" categories, and "+degPerCategory+
					" degrees per category. Heading was "+heading+" and category"+
					" was computed as: "+category);
		}
		
		if (DEBUG_HIST) {
			System.out.printf("Heading for object %s: %.2f, category: %d\n",
					itro.getFeatures(),heading,category);
		}
		return category;
	}
	
	/** Given two points representing the positions of the object over the 
	 * last two time steps, get the heading of the object's trajectory (in degrees).
	 * This is relative to the camera with the given angle (in degrees) */
	public static double getObjectHeading(Point2D.Double p1, Point2D.Double p2) {
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
		} else if (KEY_CLASSIFICATION_ENABLED.equalsIgnoreCase(key)) {
			classificationEnabled = Boolean.parseBoolean(value);
			System.out.println("ClassificationEnabled set to: "+classificationEnabled);
			return true;
		} else if (KEY_OBJ_CATEGORIES.equalsIgnoreCase(key)) {
			objCategories = Integer.parseInt(value);
			System.out.println("ObjectCategories set to: "+objCategories);
			return true;
		} else if (KEY_HIST_PER_CATEGORY_ENABLED.equalsIgnoreCase(key)) {
			histPerCategoryEnabled = Boolean.parseBoolean(value);
			if (histPerCategoryEnabled && ! classificationEnabled) {
				throw new IllegalArgumentException("Cannot enable histPerCategory " +
						"without first setting classificationEnabled to true");
			}
			System.out.println("HistPerCategoryEnabled set to: "+histPerCategoryEnabled);
			return true;
		} else if (KEY_HIST_ENABLED.equalsIgnoreCase(key)) {
			histEnabled = Boolean.parseBoolean(value);
			System.out.println("HistEnabled set to: "+histEnabled);
			return true;
		} else {
			System.err.println("Didn't recognise key: "+key);
			return false;
		}
	}

	/** Returns whether object classification is enabled for this AI node */
	public boolean classificationEnabled() {
		return classificationEnabled;
	}
	
	/** Returns whether history is recorded on a per-category basis for each
	 * camera (rather than on a per-camera basis). */
	public boolean histPerCategoryEnabled() {
		return histPerCategoryEnabled;
	}
	
	/** Since we have separate pheromones for each category, the best we can do is 
	 * average out the value for each pheromone so that it can be drawn in the
	 * simulator window. */
	public Map<String, Double> getDrawableVisionGraph() {
		Map<String, Double> dvg = new HashMap<String, Double>();
		
		for (Map.Entry<String,Map<Integer,Double>> camToItros : getVisionGraph().entrySet()) {
			double total = 0.0;
			int count = 0;
    		for (Map.Entry<Integer, Double> itro : camToItros.getValue().entrySet()) {
    			total += itro.getValue();
    			count++;
    		}
    		if (count > 0) {
    			dvg.put(camToItros.getKey(), (total/(double)count));
    		}
		}
		return dvg;
	}
	
	/** The actual vision graph object, with representation according
	 * to object categories. Note that this should not (and cannot yet) be used
	 * by the WorldView to draw links between cameras, since there are multiple,
	 * asymmetric lines between cameras. 
	 * Use {@link HistoricalAINode#getDrawableVisionGraph()} for a drawable 
	 * representation. */
    private Map<String, Map<Integer, Double>> getVisionGraph() {
        return visionGraph;
    }
    
    /** Whether the vision graph has previously entered a value for this 
     * camera-object pair (in reality, the object's category rather than the object) */
    public boolean vgContainsKey(String camName, ITrObjectRepresentation itro) { 
    	return getVisionGraph().containsKey(camName) && 
    			getVisionGraph().get(camName).containsKey(itro);
    }
    
    /** Get all pheromone values for this object, for all cameras (in reality, 
     * the object's category rather than the object itself). This is
     * to be used for averaging, finding the highest, etc., but makes little 
     * sense otherwise (since one object category can exist in the map under 
     * multiple cameras) */
    public Collection<Double> vgGetValues(ITrObjectRepresentation itro) {
    	ArrayList<Double> allValuesForItro = new ArrayList<Double>();
    	for (Map<Integer, Double> map : getVisionGraph().values()) {
    		for (Double value : map.values()) {
    			allValuesForItro.add(value);
    		}
    	}
    	return allValuesForItro;
    }
    
    /** Get all the cameras with pheromone links from this AINode */
    public Set<String> vgGetCamSet() {
    	return getVisionGraph().keySet();
    }
    
    /** Get the pheromone value for this camera-object pair (in reality, 
     * the object's category rather than the object) */
    public Double vgGet(String camName, ITrObjectRepresentation itro) {
    	if (getVisionGraph().get(camName) != null) {
    		return getVisionGraph().get(camName).get(itro);
    	} else {
    		return null;
    	}
    }
    
    /** Called once per time step per AI node in order to maintain the vision
     * graph. Mainly to be used for evaporating the links over time. */
    public void updateVisionGraph(double evaporationRate) {
    	for (Map<Integer, Double> mapOfItroToPheromone : getVisionGraph().values()) {
    		ArrayList<Integer> toRemove = new ArrayList<Integer>();
    		
    		for (Map.Entry<Integer, Double> entry : mapOfItroToPheromone.entrySet()) {
    			double val = entry.getValue();
    			entry.setValue(entry.getValue() * evaporationRate);
    			if (val < 0) {
    				toRemove.add(entry.getKey());
    			}
    		}
    		for (int i = 0; i < toRemove.size(); i++) {
        		mapOfItroToPheromone.remove(toRemove.get(i));
        	}
    	}
    }

    /** Strengthen the pheromone link for this camera-object pair. That is, 
     * add some value to the pheromone in order to strengthen the probability
     * of communication with this camera in future (for some communication 
     * strategies) */
    public void strengthenVisionEdge(String destinationName, ITrObjectRepresentation itro) {
    	double val;
    	if (vgContainsKey(destinationName, itro)) {
    		val = vgGet(destinationName, itro);
    		val = val + 1.0;
    	} else {
    		val = 1.0;
    	}
    	vgPut(destinationName, itro, val);
    }
    
    /** Put a value in the vision graph for this camera-object pair (in reality,  
     * the object's category rather than the object iself). */
    public Double vgPut(String camName, ITrObjectRepresentation itro, Double value) {
    	int category = getCategoryForObject(itro);
    	Map<Integer, Double> catToVal = getVisionGraph().get(camName);
    	if (catToVal == null) {
    		catToVal = new HashMap<Integer, Double>();
    		getVisionGraph().put(camName, catToVal);
    	}
    	return catToVal.put(category, value);
    }
    
    /** All entries in the vision graph */
    public Set<Map.Entry<String, Map<Integer, Double>>> vgEntrySet() {
    	return getVisionGraph().entrySet();
    }	
}
