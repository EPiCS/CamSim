package epics.auctionSchedules;

import java.util.HashMap;
import java.util.Map;

import epics.camsim.core.Location;
import epics.common.AbstractAuctionSchedule;
import epics.common.IBanditSolver;
import epics.common.ICameraController;
import epics.common.IRegistration;
import epics.common.ITrObjectRepresentation;
import epics.common.RandomNumberGenerator;

/**
 * Passive auction schedule implementation of the AbstractAuctionSchedule Class
 * 
 * @author Lukas Esterle <lukas [dot] esterle [at] aau [dot] at>
 *
 */
public class PassiveAINodeMulti extends AbstractAuctionSchedule { //ActiveAINodeMulti {
	
	/** The confidence for an object below which we advertise */
	public static final double CONF_THRESHOLD = 0.1;
	
	/** Keep track of confidence from last time step to find whether 
	 * it has increased or decreased */
	private Map<ITrObjectRepresentation, Double> lastConfidence = new HashMap<ITrObjectRepresentation, Double>();
	
	/**
	 * constructor for this node. calls its super constructor and sets the DEFAULT_AUCTION_DURATION
	 * @param comm the communication policy
	 * @param staticVG if static or dynamic vision graph
	 * @param vg the initial vision graph
	 * @param r the global registration component - can be null
	 * @param rg the random number generator for this instance
	 */
	public PassiveAINodeMulti(boolean staticVG, Map<String, Double> vg, IRegistration r, RandomNumberGenerator rg){
    	super(staticVG, vg, r, rg);
    }
	
	/**
	 * constructor for this node. calls its super constructor and sets the DEFAULT_AUCTION_DURATION
	 * @param comm the communication policy
     * @param staticVG if static or dynamic vision graph
     * @param vg the initial vision graph
     * @param r the global registration component - can be null
     * @param rg the random number generator for this instance
	 * @param bs the bandit solver to decide the best communication policy and auctioning schedule
	 */
	public PassiveAINodeMulti(boolean staticVG, Map<String, Double> vg, IRegistration r, RandomNumberGenerator rg, IBanditSolver bs){
    	super(staticVG, vg, r, rg, bs);
    }
	
	public PassiveAINodeMulti(boolean staticVG, 
    		Map<String, Double> vg, IRegistration r, int auctionDuration, RandomNumberGenerator rg) {
    	super(staticVG, vg, r, auctionDuration, rg);
    }
	
	public PassiveAINodeMulti(boolean staticVG, 
    		Map<String, Double> vg, IRegistration r, int auctionDuration, RandomNumberGenerator rg, IBanditSolver bs) {
    	super(staticVG, vg, r, auctionDuration, rg, bs);
    }

	/**
	 * creates a passive ai instance from another existing instance
	 * @param ai the given existing AI instance
	 */
	public PassiveAINodeMulti(AbstractAuctionSchedule ai){
		super(ai);
		if (ai instanceof PassiveAINodeMulti) {
			PassiveAINodeMulti pass = (PassiveAINodeMulti) ai;
			lastConfidence = pass.lastConfidence;
		}
	}
	
	@Override
	public void advertiseTrackedObjects() {
		for (ITrObjectRepresentation io : this.getAllTrackedObjects_bb().values()) {
			double conf = this.getVisibility(io);
			double lastConf = this.getLastConfidenceFor(io);
			
			if (this.camController.realObjectsUsed()){
				if(this.camController.objectIsVisible(io) == 1){
					callForHelp(io);	
				}
			} else if (this.camController.objectIsVisible(io) == -1){
				if (conf < CONF_THRESHOLD && (conf == 0 || conf < lastConf)) {    
//			    if (conf < CONF_THRESHOLD && conf < lastConf) {    
					callForHelp(io);	
				}
			}
			this.addLastConfidence(io, conf);
        }
	}
	
	protected void addLastConfidence(ITrObjectRepresentation io, double conf) {
        this.lastConfidence.put(io, conf);
    }

	protected double getLastConfidenceFor(ITrObjectRepresentation io) {
        if (lastConfidence.containsKey(io)) {
            return lastConfidence.get(io);
        } else {
            return 0.0;
        }
    }

    @Override
    protected void noBids(ITrObjectRepresentation tor) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void handedOver(ITrObjectRepresentation tor,
            ICameraController giveTo) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void overlappingLocation(ITrObjectRepresentation tor) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public Map<Location, Double> getHandoverLocations() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Location, Double> getNoBidLocations() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<Location, Double> getOverlapLocation() {
        // TODO Auto-generated method stub
        return null;
    }
}
