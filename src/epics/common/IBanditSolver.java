package epics.common;

/**
 * Public interface for banditSolver class.
 * 
 * @author Lukas Esterle <lukas [dot] esterle [at] aau [dot] at>
 *
 */
public interface IBanditSolver {

    /**
     * returns the actual BanditSolver
     * @return used BanditSolver
     */
	public abstract IBanditSolver getBanditSolver();

	/** 
	 * This method selects an action and takes it. It stores the current reward but does not assign it.
	 * @return the selected action
	 */
	public int selectAction();

	
	/**
	 * Assigns the reward for a certain strategy
	 * @param strategy the strategy to assign the utility to
	 * @param performance the achieved performance
	 * @param communication the required communication
	 * @return the reward of the strategy
	 */
	public double setRewardForStrategy(int strategy, double performance, double communication);

	/**
	 * set the already calculated reward
	 * @param strategy the strategy to assign the reward
	 * @param reward the reward to be assigned
	 * @return current reward for the given strategy
	 */
	public double setRewardForStrategy(int strategy, double reward);
	
	
	/**
	 * Calculates the reward based on the following function 
	 * reward = alpha * tracking_performance - (1-alpha)*communication
	 * @param tracking_performance is the current tracking_performance + the received payments - the payments made
	 * @param communication is the communication overhead. communication = (auction_invitations - received_bids) / auction_invitations
	 * @return
	 */
	public void setCurrentReward(double tracking_performance, double communication);
	
	/**
	 * same as setRewardForStrategy but setting the reward for the current strategy
	 * @param tracking_performance
	 * @param communication
	 * @param nrObjects
	 */
	public void setCurrentReward(double tracking_performance, double communication, double nrObjects);
		
	/**
	 * returns the total reward for all available arms
	 * @return list of rewards for all arms
	 */
	public double[] getTotalReward();
	
	/**
	 * returns how often each arm has been used
	 * @return amount of each arms usage
	 */
	public int[] getTotalArms();
	
	/**
	 * returns results of each arm
	 * @return
	 */
	public java.util.ArrayList<java.util.ArrayList<Double>> getResults();

	/**
	 * returns the currently best action
	 * @return best action
	 */
    String bestAction();

    /**
     * returns the currently used action
     * @return current action
     */
    public abstract int getCurrentStrategy();

    /**
     * selects a new arm without setting a reward
     * @return
     */
    public abstract int selectActionWithoutReward();
    
    /**
     * the set alpha value for this bandit solver
     * @return the used alpha value
     */
    public double getAlpha();
    
    /**
     * the set beta value for this bandit solver
     * @return the used beta value
     */
    public double getBeta();
    
    /**
     * the set gamma value for this bandit solver
     * @return the used gamma value
     */
    public double getGamma();
    
    /**
     * initiates the bandit solver with a number of options
     * @param numberOfOptions the number of available options
     */
    public void init(int numberOfOptions);

}