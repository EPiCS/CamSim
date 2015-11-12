package epics.bandits;

import java.util.Random;

import epics.common.AbstractBanditSolver;
import epics.common.RandomNumberGenerator;
import epics.common.RandomUse.USE;

/**
 * Full Random Bandit Problem Solver
 * The Full Random Bandit Problem Solver, uses a random strategy in each iteration. A strategy is a combination of a 
 * communication policy (Broadcast, STEP, SMOOTH) and an auction invitation schedule (ACTIVE or PASSIVE).
 * The approach is equal to using Epsilon Greedy Bandit Solver with epsilon = 1.
 * @author Lukas Esterle
 *
 */
public class FullRandom extends AbstractBanditSolver{ // implements IBanditSolver {


	java.util.Random rang;
	
	/**
	 * Constructor for full randomized bandit solver
	 * @param numberOfOptions the number of possible strategies 
	 * @param alpha the weighing factor for the utility function
     * @param interval how many timesteps until the bandit solver is being evaluated
     * @param rg the given random number generator for this instance
	 */
	public FullRandom(int numberOfOptions, double alpha, int interval, RandomNumberGenerator rg) {
		super(numberOfOptions, 1, alpha, interval, rg);
		rang = new java.util.Random();//(long) (alpha * 100));
	}
  
	/**
     * constructs an full random bandit solver from an existing one
     * 
     * auctioning schedules as string should be the full class name
     * (e.g. epics.ai.ActiveAINodeMulti)
     *  
     * @param eg the existing bandit solver
     * @param comm the communication policy
     * @param algo the auctioning schedule as string
	 */
	public FullRandom(AbstractBanditSolver eg, int comm, String algo) {
		super(eg, comm, algo);
	}

	/* (non-Javadoc)
	 * @see epics.learning.IBanditSolver#selectAction()
	 */
	/* (non-Javadoc)
	 * @see epics.common.BanditSolver#selectAction()
	 */
	public int selectAction() {
	    int strategy = currentStrategy;
	
    	if(count >= _interval){
    		if(currentStrategy != -1){
		    	setRewardForStrategy(currentStrategy, totalPerformance, totalCommunication);
	              totalCommunication = 0;
	              totalPerformance = 0;
		    	count = 0;
    		}
	    	// With probability epsilon, select a strategy at random.
		    strategy = selectRandomArm();
		    
		    currentStrategy = strategy;

    	}
	    
	    
	    
	    // Return the selected strategy, so that we can monitor what happened from the calling
	    // class.
	    return strategy;
	  }
  
	/**
	 * selects a completely random strategy from the set of all possible strategies
	 * @return the index of the selected strategy
	 */
	private int selectRandomArm() {
		return rang.nextInt(armsCount.length); //randomG.nextInt(armsCount.length, USE.BANDIT);
	}
	
	/* (non-Javadoc)
	 * @see epics.common.BanditSolver#bestAction()
	 */
	public String bestAction(){
	      return ""+ currentStrategy;
	  }

    @Override
    public int selectActionWithoutReward() {
        return selectRandomArm();
    }
}
