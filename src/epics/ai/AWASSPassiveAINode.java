package epics.ai;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

import java.util.HashMap;
import java.util.Map;

import epics.camsim.core.TraceableObject;
import epics.common.AbstractAINode;
import epics.common.Coordinate2D;
import epics.common.IBanditSolver;
import epics.common.IRegistration;
import epics.common.ITrObjectRepresentation;
import epics.common.RandomNumberGenerator;
import epics.common.RandomUse.USE;

public class AWASSPassiveAINode extends AbstractAINode { //ActiveAINodeMulti {
	
	/** The confidence for an object below which we advertise */
	public static final double CONF_THRESHOLD = 0.1;
	private static final int LOOK_AHEAD = 2;
	private static final double AUCT_PROB = 0.2;
	
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
	public AWASSPassiveAINode(boolean staticVG, Map<String, Double> vg, IRegistration r, RandomNumberGenerator rg){
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
	public AWASSPassiveAINode(boolean staticVG, Map<String, Double> vg, IRegistration r, RandomNumberGenerator rg, IBanditSolver bs){
    	super(staticVG, vg, r, rg, bs);
    }
	
	public AWASSPassiveAINode(boolean staticVG, 
    		Map<String, Double> vg, IRegistration r, int auctionDuration, RandomNumberGenerator rg) {
    	super(staticVG, vg, r, auctionDuration, rg);
    }
	
	public AWASSPassiveAINode(boolean staticVG, 
    		Map<String, Double> vg, IRegistration r, int auctionDuration, RandomNumberGenerator rg, IBanditSolver bs) {
    	super(staticVG, vg, r, auctionDuration, rg, bs);
    }

	/**
	 * creates a passive ai instance from another existing instance
	 * @param ai the given existing AI instance
	 */
	public AWASSPassiveAINode(AbstractAINode ai){
		super(ai);
		if (ai instanceof AWASSPassiveAINode) {
			AWASSPassiveAINode pass = (AWASSPassiveAINode) ai;
			lastConfidence = pass.lastConfidence;
		}
	}
	
	private Coordinate2D toCameraSpace(final Coordinate2D pointPos){
		final Coordinate2D camPos = camController.getPostion();
		final double camAngle = -camController.getHeading();
		final double dx = pointPos.getX()-camPos.getX();
		final double dy = pointPos.getY()-camPos.getY();
		final double x = dx * cos(camAngle) + dy * sin(camAngle);
		final double y = -dx * sin(camAngle) + dy * cos(camAngle);
		return new Coordinate2D(x, y);
	}
	

	@Override
	public void advertiseTrackedObjects() {
		for (ITrObjectRepresentation io : this.getAllTrackedObjects_bb().values()) {
			double conf = this.getConfidence(io);
			double lastConf = this.getLastConfidenceFor(io);
			if (this.camController.objectIsVisible(io) == -1){
				if (conf < CONF_THRESHOLD && (conf == 0 || conf < lastConf)) {
					final TraceableObject obj = io.getTraceableObject();
					//if(camController.isObjectInFOV(obj)){
					final Coordinate2D expectedNext = toCameraSpace(obj.esteemNext(LOOK_AHEAD));
					if((conf != 0 && !camController.isCoordinateInFOV(expectedNext)) || randomGen.nextDouble(USE.UNIV) < AUCT_PROB){
						/*
						 * Start an auction if:
						 * - the object is in FOV and it's expected to exit the FOV in near future
						 * - the object is not in FOV with probability AUCT_PROB
						 */
						callForHelp(io);
					}
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
