package epics.camsim.core;

import epics.common.IBid;
import epics.common.ITrObjectRepresentation;

/**
 * Actual implementation of a bid 
 * @author Lukas Esterle
 *
 */
public class Bid implements IBid {

	private ITrObjectRepresentation foundObject;
	private double bid;
	
	/**
	 * 
	 * Constructor for Bid
	 * @param obj object to bid for
	 * @param bid value of the bid
	 */
	public Bid(ITrObjectRepresentation obj, double bid){
		foundObject = obj;
		this.bid = bid;
	}
	
	@Override
	public ITrObjectRepresentation getTrObject() {
		return foundObject;
	}

	@Override
	public double getBid() {
		return bid;
	}

}
