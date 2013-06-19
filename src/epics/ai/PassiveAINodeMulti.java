package epics.ai;

import java.util.HashMap;
import java.util.Map;

import epics.common.AbstractAINode;
import epics.common.IBanditSolver;
import epics.common.IRegistration;
import epics.common.ITrObjectRepresentation;
import epics.common.RandomNumberGenerator;

public class PassiveAINodeMulti extends AbstractAINode { //ActiveAINodeMulti {
	
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
	public PassiveAINodeMulti(int comm, boolean staticVG, Map<String, Double> vg, IRegistration r, RandomNumberGenerator rg){
    	super(comm, staticVG, vg, r, rg);
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
	public PassiveAINodeMulti(int comm, boolean staticVG, Map<String, Double> vg, IRegistration r, RandomNumberGenerator rg, IBanditSolver bs){
    	super(comm, staticVG, vg, r, rg, bs);
    }
	
	public PassiveAINodeMulti(int comm, boolean staticVG, 
    		Map<String, Double> vg, IRegistration r, int auctionDuration, RandomNumberGenerator rg) {
    	super(comm, staticVG, vg, r, auctionDuration, rg);
    }
	
	public PassiveAINodeMulti(int comm, boolean staticVG, 
    		Map<String, Double> vg, IRegistration r, int auctionDuration, RandomNumberGenerator rg, IBanditSolver bs) {
    	super(comm, staticVG, vg, r, auctionDuration, rg, bs);
    }

	/**
	 * creates a passive ai instance from another existing instance
	 * @param ai the given existing AI instance
	 */
	public PassiveAINodeMulti(AbstractAINode ai){
		super(ai);
		if (ai instanceof PassiveAINodeMulti) {
			PassiveAINodeMulti pass = (PassiveAINodeMulti) ai;
			lastConfidence = pass.lastConfidence;
		}
	}
	
	@Override
	public void advertiseTrackedObjects() {
		for (ITrObjectRepresentation io : this.getAllTrackedObjects_bb().values()) {
			double conf = 0.0;
			double lastConf = 0.0;
			if(wrongIdentified.containsValue(io)){
				for(Map.Entry<ITrObjectRepresentation, ITrObjectRepresentation> kvp : wrongIdentified.entrySet()){
					if (kvp.getValue().equals(io)){
						conf = this.getConfidence(kvp.getKey());
						lastConf = this.getLastConfidenceFor(kvp.getKey());
					}
				}
			} else {
				conf = this.getConfidence(io);
				lastConf = this.getLastConfidenceFor(io);
			}
			
			if (this.camController.realObjectsUsed()){
				if(this.camController.objectIsVisible(io) == 1){
					callForHelp(io);	
				}
			} else if (this.camController.objectIsVisible(io) == -1){
				if (conf < CONF_THRESHOLD && (conf == 0 || conf < lastConf)) {               	
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
}
