package epics.bandits;

import epics.common.AbstractBanditSolver;
import epics.common.RandomNumberGenerator;

public class UCB2 extends AbstractBanditSolver {

	int[] selected ;
	double special_alpha = 0.5; //could this be epsilon??
	
	public UCB2(int numberOfOptions, double epsilon, double alpha,
			int interval, RandomNumberGenerator rg) {
		super(numberOfOptions, epsilon, alpha, interval, rg);
		special_alpha = epsilon;
		selected = new int[numberOfOptions];
	}
  
	public UCB2(AbstractBanditSolver eg, int comm, String algo) {
		super(eg, comm, algo);
		this.special_alpha = ((UCB2) eg).special_alpha;
		this.selected = ((UCB2) eg).selected.clone();
	}
	
	
	@Override
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
			
			
			// Find the strategy which maximises the UCB2 equation
			double thisAverageReward = armsTotalReward[0] / armsCount[0];
			double tau = Math.ceil(Math.pow(1+special_alpha, selected[0]));
			double anr = Math.sqrt(((1+special_alpha)*Math.log(Math.E * armsCount[0]/tau))/(2*tau));
			  
			// The UCB2 confidence bound
			double thisConfidenceBound = thisAverageReward + anr;
			
			double highestConfidenceBound = thisConfidenceBound;
		    int bestIndex = 0;
			
			for (int i = 1; i < armsCount.length; i++) {
		        thisAverageReward = armsTotalReward[i] / armsCount[i];
		        tau = Math.ceil(Math.pow(1+special_alpha, selected[i]));
		        anr = Math.sqrt(((1 + special_alpha) * Math.log(((Math.E * armsCount[i]) / tau)))
		        		/ (2 * tau));
		        
		        thisConfidenceBound = thisAverageReward + anr;
		        
		        if (thisConfidenceBound > highestConfidenceBound) {
			    	highestConfidenceBound = thisConfidenceBound;
			    	bestIndex = i;
		        }
			}
			
			//for how many steps this strategy should be played
			double pow1 = Math.pow(1.0d+special_alpha, selected[bestIndex]+1.0);
			double c1 = Math.ceil(pow1);
			double pow2 = Math.pow(1.0d+special_alpha, selected[bestIndex]);
			double c2 =Math.ceil(pow2);
	    	double duration = (c1 - c2);
			
	    	selected[bestIndex] ++;
	    	
			_interval = (int) duration;
			currentStrategy = bestIndex;
		}
		
		
		
		return currentStrategy;
	}

	public String bestAction(){
	    return ""+ currentStrategy;
	}
}
