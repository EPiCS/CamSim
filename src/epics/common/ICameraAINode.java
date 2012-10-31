package epics.common;

import java.util.List;
import java.util.Map;

/**
 *
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public interface ICameraAINode {

    public ITrObjectRepresentation getTrackedObject();
    
    public Map<List<Double>, ITrObjectRepresentation> getTracedObjects();

    public IMessage receiveMessage( IMessage message );

    public void addVisibleObject(ITrObjectRepresentation rto);

    public void removeVisibleObject(ITrObjectRepresentation rto);

    public Map<String,Double> getVisionGraph();

    public void setController( ICameraController controller );

    public void update();

    public double getUtility();

    public void strengthenVisionEdge( String name );

    public Map<ITrObjectRepresentation, ICameraController> getSearchedObjects();
    
    public int getComm();

	public int currentlyMissidentified();
	
	//public double getObjectUtility(ITrObjectRepresentation tor);
}
