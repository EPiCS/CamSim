package epics.common;

import java.util.List;
import java.util.Map;

import epics.camsim.core.Location;

/**
 *
 * @author Lukas Esterle & Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public interface ICameraController{

    /**
     * unique name of camera
     * @return camera name
     */
    public String getName();

    /**
     * all objects visible to this camera. does not mean these objects are actually tracked!
     * @return set of visible objects
     */
    public Map<ITrObjectRepresentation,Double> getVisibleObjects_bb();

    /**
     * create a message
     * @param to receiver of message
     * @param msgType message type
     * @param content content of message
     * @return IMessage 
     */
    public IMessage createMessage(String to, IMessage.MessageType msgType, Object content);
    /**
     * sends message to receiver
     * @param to receiver of message
     * @param msgType message type
     * @param content content of message
     * @return sent IMessage
     */
    public IMessage sendMessage(String to, IMessage.MessageType msgType, Object content);
    
    /**
     * Limit of trackable objects
     * @return limit
     */
    public int getLimit();
    
    /**
     * currently available resources
     * @return currently available resources
     */
    public double getAvailableResources();
    /**
     * reduces resources by a certain amount
     * @param amount to reduce resources by
     */
    public void reduceResources(double amount);
    
    /**
     * increase resources by certain amount
     * @param amount to increase resources by
     */
    public void addResources(double amount);

    /**
     * all cameras in the network -- THIS IS ONLY USED INTERNALLY!
     * @return all cameras in the network
     */
    public List<ICameraController> getNeighbours();

    /**
     * amount of all resources
     * @return
     */
	public double getAllResources();
	/**
	 * sets the camera as offline 
	 * @param time number of timesteps the camera is offline (-1 = infinite)
	 */
	public void setOffline(int time);
	
	/**
	 * checks if camera is offline or online
	 * @return
	 */
	public boolean isOffline();

	/**
	 * removes an object from the list of objects
	 * @param features object to be removed
	 */
	void removeObject(List<Double> features);
	
	/**
	 * change the location, heading, angel, or range of camera
	 * @param xCoord
	 * @param yCoord
	 * @param head
	 * @param angle
	 * @param range
	 */
	public void change(double xCoord, double yCoord, double head, double angle, double range); // e.x, e.y, e.heading, e.angle, e.range, e.comm, e.limit

	/**
	 * set the ai node of the camera
	 * @param ai
	 */
	public void setAINode(AbstractAuctionSchedule ai);
	
	/**
	 * checks if object is visible - returns the utility
	 * !! USED FOR SIMULATING REAL WORLD FOOTAGE !!
	 * @param tor
	 * @return utility of obejct (trackign confidence)
	 */
	public double objectIsVisible(ITrObjectRepresentation tor);
	
	/***
	 * checks if real objects are used (from real world footage)
	 * @return 
	 */
	public boolean realObjectsUsed();
	
	/**
	 * returns the visual center (the point in the middle between the camera and the maximum visibility of the camera along the heading of the camera)
	 * @return
	 */
	public Location getVisualCenter();
	
	/**
	 * center direction the camera looks at
	 * @return
	 */
	public double getHeading();
	
	/**
	 * viewing angle of the camera.
	 * represents the width of the anglge of the FOV of the camera
	 * @return
	 */
	public double getAngle();
	
	/**
	 * the normalised zoom value for this camera (normalised by the maximum value)
	 * @return
	 */
	public double getZoom();

	/**
	 * increase the range a camera can 'see' -- corresponds to zooming in
	 * @param increaseBy
	 */
    public void increaseRange(double increaseBy);

    /** 
     * returns the location of the camera
     * @return
     */
    public Location getLocation();

    /**
     * returns the distance the camera can currently 'see'
     * @return
     */
    public double getRange();
    
    /**
     * returns the maximum distance the camera could 'see'
     * @return
     */
    public double getMaxRange();
}
