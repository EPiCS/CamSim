package epics.bandits;

import epics.common.AbstractBanditSolver;
import epics.common.RandomNumberGenerator;

/**
 * UCB1 implementation to approach the idealised problem of explore vs. exploit.
 * @see <a href="http://link.springer.com/article/10.1023%2FA%3A1013689704352?LI=true#page-1">UCB1 </a>
 * 
 * 
 * @author Lukas Esterle <lukas [dot] esterle [at] aau [dot] at>
 *
 */
public class UCB1 extends AbstractBanditSolver {

    /**
     * Constructor for UCB1 - with two-fold utility function.
     * alpha = [0,1]. Alpha is the priority of the first parameter, 1-alpha is the priority for the second parameter.
     * @param numberOfOptions number of options for the bandit solver to choose from
     * @param epsilon not used in this implementation 
     * @param alpha the alpha value to trade of communication vs utility
     * @param interval the interval the bandit solver explores/exploits options
     * @param rg the random number generator
     */
	public UCB1(int numberOfOptions, double epsilon, double alpha,
			int interval, RandomNumberGenerator rg) {
		super(numberOfOptions, epsilon, alpha, interval, rg);
	}
	
	/**
	 * 
	 * Constructor for UCB1 - with three-fold utility function
	 * alpha, beta and gamma should all be between 0 and 1. furthermore, alpha + betta + gamma = 1
     * @param numberOfOptions number of options for the bandit solver to choose from
     * @param epsilon not used in this implementation 
     * @param alpha priority on first parameter
	 * @param beta priority on the second parameter
	 * @param gamma priority on the thrid parameter
	 * @param interval
	 * @param rg
	 */
	public UCB1(int numberOfOptions, double epsilon, double alpha, double beta, double gamma,
            int interval, RandomNumberGenerator rg) {
        super(numberOfOptions, epsilon, alpha, interval, rg);
    }
  
	/**
	 * Constructor for UCB1 from another bandit solver
	 * @param eg other bandit solver to create UCB1 from
	 * @param comm communication strategy
	 * @param algo auction schedule
	 */
	public UCB1(AbstractBanditSolver eg, int comm, String algo) {
		super(eg, comm, algo);
	}

/* (non-Javadoc)
 * @see epics.learning.IBanditSolver#selectAction()
 */
  public int selectAction() {
//      int strategy;

      if(count >= _interval){
    	  if(currentStrategy != -1){
    		  setRewardForStrategy(currentStrategy, totalPerformance, totalCommunication);
    		  totalCommunication = 0;
    	      totalPerformance = 0;
    	          
    		  
    		  count = 0;
    	  }
		  //Calculate total number of trials of all arms
	      double totalArmsCount = 0.0;
	      for (int i = 0; i < armsCount.length; i++)
	        totalArmsCount += armsCount[i];

	      // Find the strategy which maximises the UCB equation
	  
	      double thisAverageReward = armsTotalReward[0] / armsCount[0];
	      double thisConfidenceBound = thisAverageReward + Math.sqrt(
	          (2 * Math.log(totalArmsCount)) / (armsCount[0]));
	      double highestConfidenceBound = thisConfidenceBound;
	      int bestIndex = 0;

	      for (int i = 1; i < armsCount.length; i++) {
	        thisAverageReward = armsTotalReward[i] / armsCount[i];
	        
	        // The UCB1 confidence bound
	        thisConfidenceBound = thisAverageReward + Math.sqrt(
	          (2 * Math.log(totalArmsCount)) / (armsCount[i]));

	        if (thisConfidenceBound > highestConfidenceBound) {
	          highestConfidenceBound = thisConfidenceBound;
	          bestIndex = i;
	        }
	      }
	  
	      //prevStrategy = currentStrategy;
	      currentStrategy = bestIndex;
    	  
      }
      
      // Return the selected strategy, so that we can monitor what happened from the calling
      // class.
      return currentStrategy;
    }
  
  public String bestAction(){
      return ""+ currentStrategy;
  }

    @Override
    public int selectActionWithoutReward() {
        //Calculate total number of trials of all arms
        double totalArmsCount = 0.0;
        for (int i = 0; i < armsCount.length; i++)
          totalArmsCount += armsCount[i];

        // Find the strategy which maximises the UCB equation
    
        double thisAverageReward = armsTotalReward[0] / armsCount[0];
        double thisConfidenceBound = thisAverageReward + Math.sqrt(
            (2 * Math.log(totalArmsCount)) / (armsCount[0]));
        double highestConfidenceBound = thisConfidenceBound;
        int bestIndex = 0;

        for (int i = 1; i < armsCount.length; i++) {
          thisAverageReward = armsTotalReward[i] / armsCount[i];
          
          // The UCB1 confidence bound
          thisConfidenceBound = thisAverageReward + Math.sqrt(
            (2 * Math.log(totalArmsCount)) / (armsCount[i]));

          if (thisConfidenceBound > highestConfidenceBound) {
            highestConfidenceBound = thisConfidenceBound;
            bestIndex = i;
          }
        }
    
        //prevStrategy = currentStrategy;
        currentStrategy = bestIndex;
          
        // Return the selected strategy, so that we can monitor what happened from the calling
        // class.
        return currentStrategy;
    }
}








