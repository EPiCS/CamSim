package epics.bandits;

import java.util.ArrayList;

import epics.common.AbstractBanditSolver;
import epics.common.RandomNumberGenerator;

public class UCBTuned extends AbstractBanditSolver {
	
	public UCBTuned(int numberOfOptions, double epsilon, double alpha,
			int interval, RandomNumberGenerator rg) {
		super(numberOfOptions, epsilon, alpha, interval, rg);
	}
  
	public UCBTuned(AbstractBanditSolver eg, int comm, String algo) {
		super(eg, comm, algo);
	}
	
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
