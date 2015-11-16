package epics.common;

/**
 * Bid Interface
 * @author Lukas Esterle <lukas [dot] esterle [at] aau [dot] at>
 *
 */
public interface IBid {
    /**
     * object for bid
     * @return object
     */
	public ITrObjectRepresentation getTrObject();
	
	/**
	 * value for bid
	 * @return value
	 */
	public double getBid();
}
