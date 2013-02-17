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
	
	public abstract Map<String, Double> getDrawableVisionGraph();
    
    public abstract boolean vgContainsKey(String camName, ITrObjectRepresentation itro); 
    
    public abstract Collection<Double> vgGetValues(ITrObjectRepresentation itro);
    
    public abstract Set<String> vgGetCamSet();
    
    public abstract Double vgGet(String camName, ITrObjectRepresentation itro);
    
    public abstract void strengthenVisionEdge(String name, ITrObjectRepresentation itro);
	
	/** For specifying params of an AI node after construction time. 
	 * For example, setting a 'debug' field to true. This method should handle
	 * strings for keys and convert the value string to the appropriate type.
	 * This method should return whether the param was successfully applied. */
	public abstract boolean setParam(String key, String value);
}
