package epics.common;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractAINode {

	/** This constructor simply calls instantiateAINode(). Overriding classes
	 * should only call super and do real handling in instantiateAINode().
	 * This is painful but is to enforce these arguments in the constructor. */
	public AbstractAINode(int comm, boolean staticVG, Map<String, Double> vg, IRegistration r){
    	instantiateAINode(comm, staticVG, vg, r);
    }

	/** Subclasses implement this instead of a constructor. */
	public abstract void instantiateAINode(int comm, boolean staticVG, 
			Map<String, Double> vg, IRegistration r);
	
	public abstract ITrObjectRepresentation getTrackedObject();
    
    public abstract Map<List<Double>, ITrObjectRepresentation> getTracedObjects();

    public abstract IMessage receiveMessage(IMessage message);

    public abstract void addVisibleObject(ITrObjectRepresentation rto);

    public abstract void removeVisibleObject(ITrObjectRepresentation rto);

    public abstract void setController(ICameraController controller);

    public abstract void update();

    public abstract double getUtility();
    
    public abstract Map<ITrObjectRepresentation, ICameraController> getSearchedObjects();
    
    public abstract int getComm();

	public abstract int currentlyMissidentified();
	
	/** Get a mapping between camera names and values for the pheromone links
	 * between the AI node and those cameras. This is used for drawing the 
	 * vision graph. */
	public abstract Map<String, Double> getDrawableVisionGraph();
    
	/** Whether the vision graph contains a value for this key */
    public abstract boolean vgContainsKey(String camName, ITrObjectRepresentation itro); 
    
    /** Get all values in the vision graph for this object */
    public abstract Collection<Double> vgGetValues(ITrObjectRepresentation itro);
    
    /** Get the set of cameras existent in the vision graph */
    public abstract Set<String> vgGetCamSet();
    
    /** Get the pheromone value for the given camera name and object.
     * It is not required to use both parameters in implementing classes. */
    public abstract Double vgGet(String camName, ITrObjectRepresentation itro);
    
    /** Called when communication is made with the given camera about the 
     * given object, in order to strengthen the pheromone link. */
    public abstract void strengthenVisionEdge(String name, ITrObjectRepresentation itro);
	
	/** For specifying params of an AI node after construction time. 
	 * For example, setting a 'debug' field to true. This method should handle
	 * strings for keys and convert the value string to the appropriate type.
	 * This method should return whether the param was successfully applied. */
	public abstract boolean setParam(String key, String value);
}
