package epics.common;

import epics.common.IMessage.MessageType;

/**
 * An abstract class for the implementation of communication policies.
 * A communication policy is an algorithm which decides the nodes with
 * which to communicate. 
 */
public abstract class Multicast {

	protected AbstractAINode ai; 
	protected ICameraController camController;
	
	public Multicast(AbstractAINode ai, ICameraController camController) {
		this.ai = ai;
		this.camController = camController;
	}
	
	/**
	 * This method allows an implementing class to decide -- based 
	 * on the vision graph or otherwise -- which nodes to send the
	 * message to.
	 * @param mt the given message to be sent to other cameras
     * @param o the object related to the sent message
	 */
	public abstract void multicast(MessageType mt, Object o);
	
	/** 
	 * Broadcast the message to all nodes, without using
	 * a special multicast policy 
	 */
	public abstract void broadcast(MessageType mt, Object o);
}
