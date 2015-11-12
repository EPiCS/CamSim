package epics.ai;

import java.util.Map;

import epics.camsim.core.Location;
import epics.common.AbstractAINode;
import epics.common.IBanditSolver;
import epics.common.ICameraController;
import epics.common.IRegistration;
import epics.common.ITrObjectRepresentation;
import epics.common.RandomNumberGenerator;

/**
 * Implementation of AbstractAINode.
 * defines the behaviour of the camera node regarding communication policies and the auction invitation schedules.
 * this class uses the active auction invitation schedule to send invitations to other cameras in every timestep. 
 * @author Marcin Bogdanski & Lukas Esterle, refactored by Horatio Cane 
 */
public class ActiveAINodeMulti extends AbstractAINode {
    
    /**
     * Creates an AI Node with active auction schedule from another, existing ai node
     * @param ai the existing ai node
     */
    public ActiveAINodeMulti(AbstractAINode ai){
    	super(ai);
    }
    
    /**
     * Creates an AI Node with active auction schedule WITHOUT bandit solver for switching to another node automatically
     * @param comm the used communication policy
     * @param staticVG if static vision graph or not
     * @param vg the initial vision graph
     * @param r the global registration component - can be null
     * @param auctionDuration the duration of auctions
     * @param rg the random number generator for this instance
     */
    public ActiveAINodeMulti(boolean staticVG, 
    		Map<String, Double> vg, IRegistration r, int auctionDuration, RandomNumberGenerator rg) {
    	super(staticVG, vg, r, auctionDuration, rg);
    }
    
    /**
     * Creates an AI Node with active auction schedule WITH a bandit solver for switching to another node automatically
     * @param comm the used communication policy
     * @param staticVG if static vision graph or not
     * @param vg the initial vision graph
     * @param r the global registration component - can be null
     * @param auctionDuration the duration of auctions
     * @param rg the random number generator for this instance
     * @param bs the bandit solver 
     */
    public ActiveAINodeMulti(boolean staticVG, 
    		Map<String, Double> vg, IRegistration r, int auctionDuration, RandomNumberGenerator rg, IBanditSolver bs) {
    	super(staticVG, vg, r, auctionDuration, rg, bs);
    }
    
    /**
     * Creates an AI Node with active auction schedule WITHOUT bandit solver for switching to another node automatically
     * @param comm the used communication policy
     * @param staticVG if static vision graph or not
     * @param vg the initial vision graph
     * @param r the global registration component - can be null
     * @param rg the random number generator for this instance
     */
    public ActiveAINodeMulti(boolean staticVG, 
    		Map<String, Double> vg, IRegistration r, RandomNumberGenerator rg) {
    	super(staticVG, vg, r, rg);
    }
    
    /**
     * Creates an AI Node with active auction schedule WITH a bandit solver for switching to another node automatically
     * @param comm the used communication policy
     * @param staticVG if static vision graph or not
     * @param vg the initial vision graph
     * @param r the global registration component - can be null
     * @param rg the random number generator for this instance
     * @param bs the bandit solver 
     */
    public ActiveAINodeMulti(boolean staticVG, 
    		Map<String, Double> vg, IRegistration r, RandomNumberGenerator rg, IBanditSolver bs) {
    	super(staticVG, vg, r, rg, bs);
    }
	
    public void advertiseTrackedObjects() {
    	// Active strategy means all objects are advertised every time
		for (ITrObjectRepresentation io : this.getAllTrackedObjects_bb().values()) {
			callForHelp(io);	
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
