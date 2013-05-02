package epics.bandits;

import epics.common.BanditSolver;
import epics.common.RandomNumberGenerator;
import epics.common.RandomUse.USE;

public class EpsilonGreedy extends BanditSolver{ // implements IBanditSolver {


	public EpsilonGreedy(int numberOfOptions, double epsilon, double alpha, int interval, RandomNumberGenerator rg) {
		super(numberOfOptions, epsilon, alpha, interval, rg);
	}
  
	public EpsilonGreedy(BanditSolver eg, int comm, String algo) {
		super(eg, comm, algo);
	}

	/* (non-Javadoc)
	 * @see epics.learning.IBanditSolver#selectAction()
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
  
  // Helper methods to select arms - for epsilon greedy
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
      
	  private int selectRandomArm() {
		return randomG.nextInt(armsCount.length, USE.BANDIT);
	    //return (int)Math.round(randomG.nextDouble(USE.BANDIT) * (armsCount.length - 1));
	  }

	  public String bestAction(){
	      return "" + selectBestArm();
	  }

}
