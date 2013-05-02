package epics.bandits;

import epics.ai.ActiveAINodeMulti;
import epics.common.BanditSolver;
import epics.common.IBanditSolver;
import epics.common.RandomNumberGenerator;

public class UCB1 extends BanditSolver {




	public UCB1(int numberOfOptions, double epsilon, double alpha,
			int interval, RandomNumberGenerator rg) {
		super(numberOfOptions, epsilon, alpha, interval, rg);
	}
  
	public UCB1(BanditSolver eg, int comm, String algo) {
		super(eg, comm, algo);
	}

/* (non-Javadoc)
 * @see epics.learning.IBanditSolver#selectAction()
 */
  public int selectAction() {
      int strategy;

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
}
