package epics.bandits;

import javax.print.attribute.standard.NumberOfDocuments;

import epics.common.BanditSolver;
import epics.common.IBanditSolver;
import epics.common.RandomNumberGenerator;
import epics.common.RandomUse.USE;

public class SoftMax extends BanditSolver {

	double temperature = 0.01; // could be alpha? or epsilon??
	
	public SoftMax(int numberOfOptions, double epsilon, double alpha, int interval, RandomNumberGenerator rg){
		super(numberOfOptions, epsilon, alpha, interval, rg);
		temperature = epsilon;
	}
	
	public SoftMax(BanditSolver eg, int comm, String algo) {
		super(eg, comm, algo);
		temperature = epsilon;
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

			//sum exp(q(b)/temperature)
			double totalActionProbability = 0.0;
			double[] p = new double[armsTotalReward.length];
			
			for(int i = 0; i < armsTotalReward.length; i++){
				p[i] = Math.exp((armsTotalReward[i] / armsCount[i])/temperature);
				totalActionProbability += p[i];
			}
			
			double random_number = randomG.nextDouble(USE.BANDIT) * totalActionProbability;
	  
			int bestIndex = 0;
			for(;bestIndex < p.length && random_number > 0; bestIndex++)
				random_number -= p[bestIndex];
			
			currentStrategy = bestIndex-1;
		}
		
		return currentStrategy;
	}
	
	public String bestAction(){
	    return ""+ currentStrategy;
	}
}
