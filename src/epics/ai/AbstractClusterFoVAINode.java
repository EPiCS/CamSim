package epics.ai;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

import epics.common.Coordinate2D;
import epics.common.RandomNumberGenerator;
import epics.common.AbstractAINode;
import epics.common.IRegistration;
import epics.common.IBanditSolver;
import epics.common.ITrObjectRepresentation;

import epics.ai.kmeans.*;

public abstract class AbstractClusterFoVAINode extends AbstractAINode {
	SequentialKMeans kmeans;

//	protected Map<String, Double> visionGraph = new HashMap<String, Double>();

	// the idea: we cluster the FoV based on the hand-over positions.
	// therefor, 
	//   we should get the visionGraph based on the current position of the object
	//   we should update the K-Means when a handover takes place

	// connnnstructor
	public AbstractClusterFoVAINode(AbstractAINode old){
		super(old);
		initKMeans();
	}

	/**
	 * Creates an AI Node WITHOUT bandit solver 
	 * for switching to another node automatically.
	 * This constructor simply calls instantiateAINode(). Overriding classes
     * should only call super and do real handling in instantiateAINode().
     * This is painful but is to enforce these arguments in the constructor. 
     * 
	 * @param comm communication used
	 * @param staticVG if true, only static vision graph is used
	 * @param vg initial vision graph
	 * @param r global registration component
	 * @param rg random number generator for this node
	 */
	public AbstractClusterFoVAINode(boolean staticVG, Map<String, Double> vg, IRegistration r, RandomNumberGenerator rg){
		super(staticVG, vg, r, rg);
		initKMeans();
    }
	
    /**
     * Creates an AI Node WITHOUT bandit solver for switching to another node automatically
     * @param comm the used communication policy
     * @param staticVG if static vision graph or not
     * @param vg the initial vision graph
     * @param r the global registration component - can be null
     * @param auctionDuration the duration of auctions
     * @param rg the random number generator for this instance
     */
    public AbstractClusterFoVAINode(
            boolean staticVG, 
    		Map<String, Double> vg, IRegistration r, int auctionDuration, RandomNumberGenerator rg) {
    	super(staticVG, vg, r, auctionDuration, rg);
		initKMeans();
    }
	
	/**
	 * Standard constructor including bandit solver
	 * @param comm communication used
     * @param staticVG if true, only static vision graph is used
     * @param vg initial vision graph
     * @param r global registration component
     * @param rg random number generator for this node
	 * @param bs the bandit solver to find the best strategy
	 */
	public AbstractClusterFoVAINode(
	        boolean staticVG, Map<String, Double> vg,
			IRegistration r, RandomNumberGenerator rg, IBanditSolver bs){
		super(staticVG, vg, r, rg, bs);
		initKMeans();
	}
	
	
	public AbstractClusterFoVAINode(
            boolean staticVG, 
			Map<String, Double> vg, IRegistration r, int auctionDuration, RandomNumberGenerator rg, IBanditSolver bs) {
		super(staticVG, vg, r, auctionDuration, rg, bs);
		initKMeans();
	}

	private void initKMeans() {
		this.kmeans = new SequentialKMeans(2); // 2 features/dimension, -> the position of the object at handover
	}
	
    /**
     * copies the given AbstractClusterFoVAINode
     * @param ai the given AiNode
     */
    public void instantiateAINode(AbstractClusterFoVAINode ai){
    	super.instantiateAINode(ai);
    	this.kmeans = ai.kmeans;
    }


	protected Coordinate2D toCameraSpace(final Coordinate2D pointPos){
		final Coordinate2D camPos = camController.getPostion();
		final double camAngle = -camController.getHeading();
		final double dx = pointPos.getX()-camPos.getX();
		final double dy = pointPos.getY()-camPos.getY();
		final double x = dx * cos(camAngle) + dy * sin(camAngle);
		final double y = -dx * sin(camAngle) + dy * cos(camAngle);
		return new Coordinate2D(x, y);
	}
	
	
	protected Map<String, Double> getVisionGraphForObject(ITrObjectRepresentation rto) {
		Coordinate2D coords = toCameraSpace(rto.getTraceableObject().getCurrentPosition());
		double[] co = new double[2];
		co[0] = coords.getX();
		co[1] = coords.getY();
		
		Cluster c = this.kmeans.getClosestCluster( new ClusterPoint(co) );
		System.out.println("For obj at "+coords+", got "+c);
		return c.visionGraph;
	}


	// this should be the moment of handover...
	protected void removeTrackedObject(ITrObjectRepresentation rto) {
		super.removeTrackedObject(rto);
		System.out.println("THIS IS Hand-overrrrrr");
		
		Coordinate2D coords = toCameraSpace(rto.getTraceableObject().getCurrentPosition());
		double[] co = new double[2];
		co[0] = coords.getX();
		co[1] = coords.getY();
		
		this.kmeans.add(new ClusterPoint(co));
	}



    /**
     * updates the vision graph. reduces every link by the evaporationrate
     */
    protected void updateVisionGraph() {
        if(!staticVG){
        	Iterator<Cluster> clusters = this.kmeans.getClusterIterator();
        	while (clusters.hasNext()) {
        		Cluster cluster = clusters.next();
        		
				ArrayList<String> toRemove = new ArrayList<String>();
				for (Map.Entry<String, Double> e : vgEntrySet(cluster)) {
					double val = e.getValue();
					e.setValue( e.getValue() * EVAPORATIONRATE); //0.995);
					if (val < 0) {
						toRemove.add(e.getKey());
					}
				}
				for (int i = 0; i < toRemove.size(); i++) {
					vgRemove(toRemove.get(i), cluster);
				}
        	} // for each cluster
        }
    }


	
	/**
	 * returns the vision graph
	 * the graph consists of cameras (key) and the corresponding strength (value) in a map
	 * @return the vision graph
	 */
	public Map<String, Double> getVisionGraph() {
        return null;
    }
	public Map<String, Double> getVisionGraph(ITrObjectRepresentation itro) {
        return this.getVisionGraphForObject(itro);
    }

    /** Whether the key exists for this cam name (ignoring object here) */
    public boolean vgContainsKey(String camName, ITrObjectRepresentation itro) { 
    	return getVisionGraph(itro).containsKey(camName);
    }
    
	/** Get all values in the vision graph (ignoring object here) */
    public Collection<Double> vgGetValues(ITrObjectRepresentation itro) {
    	return getVisionGraph(itro).values();
    }

	/** Get all cameras with values in the vision graph */
    public Set<String> vgGetCamSet() {
    	return null;
    }
    public Set<String> vgGetCamSet(ITrObjectRepresentation itro) {
    	return getVisionGraph(itro).keySet();
    }

    
	/** Get the pheromone value for this camera name (ignoring object here) */
    public Double vgGet(String camName, ITrObjectRepresentation itro) {
    	return getVisionGraph(itro).get(camName);
    }

	// TODO
    
    /** Put a value in the vision graph under this camera name (ignoring 
     * object here) */
    public Double vgPut(String camName, ITrObjectRepresentation itro, Double value) {
    	return getVisionGraph(itro).put(camName, value);
    }

    /** Get all entries (key-value pairs) in the vision graph */
    public Set<Map.Entry<String, Double>> vgEntrySet() {
    	return null;
    }
    public Set<Map.Entry<String, Double>> vgEntrySet(ITrObjectRepresentation itro) {
    	return getVisionGraph(itro).entrySet();
    }

    public Set<Map.Entry<String, Double>> vgEntrySet(Cluster cluster) {
    	return cluster.visionGraph.entrySet();
    }
    
    /** Remove from the vision graph the key-value pair for the given key */
    public Double vgRemove(String name) {
    	return null;
    }
    public Double vgRemove(String name, ITrObjectRepresentation itro) {
    	return getVisionGraph(itro).remove(name);
    }
    public Double vgRemove(String name, Cluster cluster) {
    	return cluster.visionGraph.remove(name);
    }

    private void printVisionGraph() {
    	// for all vision graphs print
//    	String neighbours = "";
//    	for (String neighbour : visionGraph.keySet()) {
//			neighbours += "; " + neighbour; 
//		}
//    	System.out.println(this.camController.getName() + 
//    			" has the following neighbours:" + neighbours);
    }

		
}

