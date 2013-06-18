package epics.bandits;

import epics.common.BanditSolver;
import epics.common.RandomNumberGenerator;
import epics.common.RandomUse.USE;


/**
 * Epsilon Greedy Bandit Problem Solver
 * The Epsilon Greedy bandit problem solver, uses the best strategy in each iteration. The given epsilon value 
 * defines a small probability to explore the other existing strategies. a strategy is a combination of a 
 * communication policy (Broadcast, STEP, SMOOTH) and an auction invitation schedule (ACTIVE or PASSIVE).
 * 
 * @author Lukas Esterle, Peter R. Lewis
 *
 */
public class EpsilonGreedy extends BanditSolver{ // implements IBanditSolver {


	/**
	 * The constructor calling the super constructor {@link BanditSolver BanditSolver}
	 * The bandit solver is given a number of possible configurations. a configuration is 
	 * a combination of communication policy and the auction invitation schedule.
	 * 
	 * epsilon describes the probability of exploring other options. hence, 1-epsilon describes the
	 * the probability of using the best strategy (configuration)
	 * 
	 *  alpha describes the weighting factor for the utility function. the higher alpha, the more focus
	 *  on performance of the tracker, the lower alpha, the more focus on low communication.
	 * 
	 * @param numberOfOptions the number of possible configurations
	 * @param epsilon the percentage of exploration
	 * @param alpha the weighing factor for the utility function
	 * @param interval how many timesteps until the bandit solver is being evaluated
	 * @param rg the given random number generator for this instance
	 */
	public EpsilonGreedy(int numberOfOptions, double epsilon, double alpha, int interval, RandomNumberGenerator rg) {
		super(numberOfOptions, epsilon, alpha, interval, rg);
	}
  
	/**
	 * constructs an epsilon-greedy bandit solver from an existing one
	 * 
	 * auctioning schedules as string should be the full class name
	 * (e.g. epics.ai.ActiveAINodeMulti)
	 *  
	 * @param eg the existing bandit solver
	 * @param comm the communication policy
	 * @param algo the auctioning schedule as string
	 */
	public EpsilonGreedy(BanditSolver eg, int comm, String algo) {
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
		    if (randomG.nextDouble(USE.BANDIT) < epsilon){ 
		    	strategy = selectRandomArm();
		    }
		    else
		    	strategy = selectBestArm();
		    
		    currentStrategy = strategy;

    	}
	    
	    
	    
	    // Return the selected strategy, so that we can monitor what happened from the calling
	    // class.
	    return strategy;
	  }
  

	  /**
	   * Helper methods to select arms - for epsilon greedy
	   * @return the index of the best arm
	   */
	private int selectBestArm() {
	    // Keep track of the best arm so far
		    double bestReward = armsTotalReward[0] / armsCount[0];
		    int bestIndex = 0;
		
		    double thisReward;
		    for (int i = 0; i < armsCount.length; i++) {
		      thisReward = armsTotalReward[i] / armsCount[i];
		      
		      if (thisReward > bestReward) {
		    	  bestReward = thisReward;
		    	  bestIndex = i;
		      }
		    }
	    return bestIndex;
	  }
      
	  /**
	   * returns the index of a random arm
	 * @return the random arm
	 */
	private int selectRandomArm() {
		return randomG.nextInt(armsCount.length, USE.BANDIT);
	    //return (int)Math.round(randomG.nextDouble(USE.BANDIT) * (armsCount.length - 1));
	  }

	  /* (non-Javadoc)
	 * @see epics.common.BanditSolver#bestAction()
	 */
	public String bestAction(){
	      return "" + selectBestArm();
	  }

}
