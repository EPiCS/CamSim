package epics.common;

public interface IBanditSolver {

	public abstract IBanditSolver getBanditSolver();

	/** This method selects an action and takes it. It stores the current reward but does not assign it.
	 */
	public int selectAction();

	
	/**
	 * Assigns the reward for a certain strategy
	 * @param strategy the strategy to assign the utility to
	 * @param reward to be assigned 
	 * @return
	 */
	public double setRewardForStrategy(int strategy, double performance, double communication);

	/**
	 * Calculates the reward based on the following function 
	 * reward = alpha * tracking_performance - (1-alpha)*communication
	 * @param tracking_performance is the current tracking_performance + the received payments - the payments made
	 * @param communication is the communication overhead. communication = (auction_invitations - received_bids) / auction_invitations
	 * @return
	 */
	public void setCurrentReward(double tracking_performance, double communication);
	
	public void setCurrentReward(double tracking_performance, double communication, double nrObjects);
	
	public double[] getTotalReward();
	public int[] getTotalArms();
	public java.util.ArrayList<java.util.ArrayList<Double>> getResults();

    String bestAction();

}