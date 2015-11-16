package epics.bandits;

import java.util.ArrayList;

import epics.common.AbstractBanditSolver;
import epics.common.RandomNumberGenerator;


/**
 * UCB1-Tuned implementation to approach the idealised problem of explore vs. exploit.
 * @see <a href="http://link.springer.com/article/10.1023%2FA%3A1013689704352?LI=true#page-1">UCB1-Tuned</a>
 * 
 * 
 * @author Lukas Esterle <lukas [dot] esterle [at] aau [dot] at>
 *
 */
public class UCBTuned extends AbstractBanditSolver {
    
    /**
     * Constructor for UCB1-Tuned - with two-fold utility function.
     * alpha = [0,1]. Alpha is the priority of the first parameter, 1-alpha is the priority for the second parameter.
     * @param numberOfOptions number of options for the bandit solver to choose from
     * @param epsilon not used in this implementation 
     * @param alpha the alpha value to trade of communication vs utility
     * @param interval the interval the bandit solver explores/exploits options
     * @param rg the random number generator
     */
	public UCBTuned(int numberOfOptions, double epsilon, double alpha,
			int interval, RandomNumberGenerator rg) {
		super(numberOfOptions, epsilon, alpha, interval, rg);
	}
  
	/**
     * Constructor for UCB1 from another bandit solver
     * @param eg other bandit solver to create UCB1 from
     * @param comm communication strategy
     * @param algo auction schedule
     */
	public UCBTuned(AbstractBanditSolver eg, int comm, String algo) {
		super(eg, comm, algo);
	}
	
	/*
	 * (non-Javadoc)
	 * @see epics.common.AbstractBanditSolver#selectAction()
	 */
	public int selectAction() {
//	      int strategy;

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

		      // Find the strategy which maximises the UCBTuned equation
		  
		      double thisAverageReward = armsTotalReward[0] / armsCount[0];
		      double std = CalculateStdFor(allResults.get(0), thisAverageReward); //calculates standard deviation for this strategy
		      double thisConfidenceBound = thisAverageReward + Math.sqrt(
		          (Math.log(totalArmsCount)) / (armsCount[0]) * 
		          Math.min(1.0/4.0, std + Math.sqrt((2.0*Math.log(totalArmsCount)) / (armsCount[0]))));
		      double highestConfidenceBound = thisConfidenceBound;
		      
		      int bestIndex = 0;
		      
		      for (int i = 1; i < armsCount.length; i++) {
		        thisAverageReward = armsTotalReward[i] / armsCount[i];
		        std = CalculateStdFor(allResults.get(i), thisAverageReward); //calculates standard deviation for this strategy 
			    
		        // The UCBTuned confidence bound
		        thisConfidenceBound = thisAverageReward + Math.sqrt(
				          (Math.log(totalArmsCount)) / (armsCount[i]) * 
				          Math.min((1.0/4.0), std + Math.sqrt((2.0*Math.log(totalArmsCount)) / (armsCount[i]))));
			        
		        if (thisConfidenceBound > highestConfidenceBound) {
		            highestConfidenceBound = thisConfidenceBound;
		            bestIndex = i;
		        }
		      }
		      
		      currentStrategy = bestIndex;	    	  
	      }
	      
	      // Return the selected strategy, so that we can monitor what happened from the calling
	      // class.
	      return currentStrategy;   
	}
	
	public String bestAction(){
	    return ""+ currentStrategy;
	}

	private double CalculateStdFor(ArrayList<Double> arrayList, double mean) {
		double total = 0;
		for(Double d : arrayList){
		    total += Math.pow(d - mean, 2.0);
		}
		if(total == 0){
			return 0;
		}
		
		return Math.sqrt(total/arrayList.size());
	}

    @Override
    public int selectActionWithoutReward() {
        int strategy;

        if(count >= _interval){
            //Calculate total number of trials of all arms
            double totalArmsCount = 0.0;
            for (int i = 0; i < armsCount.length; i++)
              totalArmsCount += armsCount[i];

            // Find the strategy which maximises the UCBTuned equation
        
            double thisAverageReward = armsTotalReward[0] / armsCount[0];
            double std = CalculateStdFor(allResults.get(0), thisAverageReward); //calculates standard deviation for this strategy
            double thisConfidenceBound = thisAverageReward + Math.sqrt(
                (Math.log(totalArmsCount)) / (armsCount[0]) * 
                Math.min(1.0/4.0, std + Math.sqrt((2.0*Math.log(totalArmsCount)) / (armsCount[0]))));
            double highestConfidenceBound = thisConfidenceBound;
            
            int bestIndex = 0;
            
            for (int i = 1; i < armsCount.length; i++) {
              thisAverageReward = armsTotalReward[i] / armsCount[i];
              std = CalculateStdFor(allResults.get(i), thisAverageReward); //calculates standard deviation for this strategy 
              
              // The UCBTuned confidence bound
              thisConfidenceBound = thisAverageReward + Math.sqrt(
                        (Math.log(totalArmsCount)) / (armsCount[i]) * 
                        Math.min((1.0/4.0), std + Math.sqrt((2.0*Math.log(totalArmsCount)) / (armsCount[i]))));
                  
              if (thisConfidenceBound > highestConfidenceBound) {
                  highestConfidenceBound = thisConfidenceBound;
                  bestIndex = i;
              }
            }
            
            currentStrategy = bestIndex;            
        }
        
        // Return the selected strategy, so that we can monitor what happened from the calling
        // class.
        return currentStrategy;   
    }

}
