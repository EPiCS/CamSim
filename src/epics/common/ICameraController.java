package epics.common;

import java.util.List;
import java.util.Map;

import epics.camsim.core.Location;
import epics.camsim.core.TraceableObjectRepresentation;

/**
 *
 * @author Lukas Esterle & Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public interface ICameraController{

    public String getName();

    public Map<ITrObjectRepresentation,Double> getVisibleObjects_bb();

    public IMessage createMessage(String to, IMessage.MessageType msgType, Object content);
    public IMessage sendMessage(String to, IMessage.MessageType msgType, Object content);
    
    public int getLimit();
    
    public double getAvailableResources();
    public void reduceResources(double amount);
    public void addResources(double amount);

    // TODO: remove:

    public List<ICameraController> getNeighbours();

	public double getAllResources();
	public void setOffline(int time);
	public boolean isOffline();
    //public ICameraAINode getAINode();

	void removeObject(List<Double> features);
	
	public void change(double xCoord, double yCoord, double head, double angle, double range); // e.x, e.y, e.heading, e.angle, e.range, e.comm, e.limit

	public void setAINode(AbstractAINode ai);
	public double objectIsVisible(ITrObjectRepresentation tor);
	
	public boolean realObjectsUsed();
	public Location getVisualCenter();
	
	public double getHeading();
	public double getAngle();
	
	public double getEnergy();

    public void increaseRange(double increaseBy);

    public Location getLocation();

    public double getRange();
    
    public double getMaxRange();
}
