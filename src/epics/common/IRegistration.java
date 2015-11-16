package epics.common;

/**
 * 
 * 
 * @author Lukas Esterle <lukas [dot] esterle [at] aau [dot] at>
 *
 */
public interface IRegistration {

    /**
     * 
     * @param to
     */
	void objectIsAdvertised(ITrObjectRepresentation to);

	/**
	 * 
	 * @param cc
	 */
	void addCamera(ICameraController cc);

	/**
	 * 
	 * @param tc
	 */
	void advertiseGlobally(ITrObjectRepresentation tc);
	
	/**
	 * 
	 * @param to
	 * @param cc
	 */
	void objectTrackedBy(ITrObjectRepresentation to, ICameraController cc);

	/**
	 * 
	 */
	void update();

	/**
	 * 
	 * @param cc
	 */
	void removeCamera(ICameraController cc);

	/**
	 * 
	 * @param duration
	 */
	void setOffline(int duration);
	
}
