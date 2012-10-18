package epics.camsim.core;

import epics.common.IBid;
import epics.common.ITrObjectRepresentation;

public class Bid implements IBid {

	private ITrObjectRepresentation foundObject;
	private double bid;
	
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
