package epics.bandits;

import javax.print.attribute.standard.NumberOfDocuments;

import epics.common.BanditSolver;
import epics.common.IBanditSolver;
import epics.common.RandomNumberGenerator;
import epics.common.RandomUse.USE;

/**
 * Softmax 
 * @author Lukas Esterle
 *
 */
public class SoftMax extends BanditSolver {

	double temperature = 0.01; // could be alpha? or epsilon??
	
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
     * @param temperature the percentage of exploration
     * @param alpha the weighing factor for the utility function
     * @param interval how many timesteps until the bandit solver is being evaluated
     * @param rg the given random number generator for this instance
	 */
	public SoftMax(int numberOfOptions, double temperature, double alpha, int interval, RandomNumberGenerator rg){
		super(numberOfOptions, temperature, alpha, interval, rg);
		this.temperature = temperature;
	}
	
	/**-
	 * @param eg
	 * @param comm
	 * @param algo
	 */
	public SoftMax(BanditSolver eg, int comm, String algo) {
		super(eg, comm, algo);
		temperature = epsilon;
	}
	
	/* (non-Javadoc)
	 * @see epics.common.BanditSolver#selectAction()
	 */
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
	
	/* (non-Javadoc)
	 * @see epics.common.BanditSolver#bestAction()
	 */
	public String bestAction(){
	    return ""+ currentStrategy;
	}
}
