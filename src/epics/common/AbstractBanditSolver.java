package epics.common;

import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import epics.common.RandomUse.USE;

/**
 * Abstract bandit solver based on the bandit solver interface.
 * The abstract class is used to select strategies as well as zoom levels.
 * 
 * The bandit solver keeps a record of actions and the associated benefit for each action.  Based on the implementation the bandit solver selects an action when triggered.
 * 
 * @author Lukas Esterle
 *
 */
public abstract class AbstractBanditSolver implements IBanditSolver {

	// Place to store the running total of how many times each arm has been tried.
	protected int[] armsCount;
	
	protected int[]usedArms;
	
	// Place to store the running total of the reward from each arm.
	protected double[] armsTotalReward;
	
	protected ArrayList<ArrayList<Double>> allResults;
	
	protected SortedMap<Double, Integer> commHist = new TreeMap<Double, Integer>();
	protected SortedMap<Double, Integer> utilHist = new TreeMap<Double, Integer>();
	
	protected int currentStrategy;
	protected double alpha;
	
	// The epsilon value for this bandit solver.
	protected double epsilon; 
	
	// counter and interval to select strategy
	protected int count;
	protected int _interval;
	
	protected RandomNumberGenerator randomG;

    private double maxComm = Double.MIN_VALUE;
    private double minComm = Double.MAX_VALUE;
    private double maxPerf = Double.MIN_VALUE;
    private double minPerf = Double.MAX_VALUE;
    
    //if normalized, then normalizedByDist has to be false;
    // if normalizedByDist, then normalized has to be false;
    boolean normalized = false;
    private boolean normalizedByDist = true;
    private boolean normalizeConf = false;
    
    protected double totalPerformance;
    protected double totalCommunication;

    private int counterinit = 0;

    private double _nrObjects;

    private double beta = 0.0;

    private double gamma = 0.0;

    
	
    /**
     *  Create a new instance of the BanditSolver. The BanditSolver is initialised with algo and communication so we don't have to select one in the first round.
     *  
     * @param numberOfOptions The number of alternatives (arms) available to the
     * bandit solver.
     * @param epsilon The value epsilon is initialised with
     * @param comm The initial communication
     * @param algo The initial algorithm
     * @param alpha The value alpha is initialised with
     */
	public AbstractBanditSolver(int numberOfOptions, double epsilon, double alpha, int interval, RandomNumberGenerator rg) {
		  
		this.alpha = alpha;
		_interval = interval;
		count = interval;
		randomG = rg;
		
		if(normalized){
    		maxComm = Double.MIN_VALUE;
    	    minComm = Double.MAX_VALUE;
    	    maxPerf = Double.MIN_VALUE;
    	    minPerf = Double.MAX_VALUE;
		}
		else{
		    maxComm = 1.0;
	        minComm = 1.0;
	        maxPerf = 1.0;
	        minPerf = 1.0;
	        _nrObjects = 1.0;
		}
		
		totalCommunication = 0;
		totalPerformance = 0;
		  
	    // Initialise arm counter
	    armsCount = new int[numberOfOptions];
	    usedArms = new int[numberOfOptions];
	    
	    armsTotalReward = new double[numberOfOptions];
	    allResults = new ArrayList<ArrayList<Double>>(numberOfOptions);
	    
	    for(int i = 0; i < numberOfOptions; i++){
	    	allResults.add(new ArrayList<Double>());
	    }
	    
	    reinitialise();	    
	    
	    currentStrategy = -1;
		
	    // Set epsilon
	    this.epsilon = epsilon;
	}
	
	/**
	 * 
	 * Constructor for AbstractBanditSolver when used to select zoom levels
	 * 
	 * @param numberOfOptions number of options to select from
	 * @param epsilon epsilon value for epsilon greedy
	 * @param alpha alpha value to set priority of tracking performance
	 * @param beta beta value to set priority of proportion of tracked objects
	 * @param gamma gamma value to set priority of overlapping fields of view
	 * @param interval interval of selecting a new arm
	 * @param rg random number generator for this bandit solver
	 */
	public AbstractBanditSolver(int numberOfOptions, double epsilon, double alpha, double beta, double gamma, int interval, RandomNumberGenerator rg) {
	    this(numberOfOptions, epsilon, alpha, interval, rg);
	    this.beta = beta;
	    this.gamma = gamma;
	}
	
	/**
	 * Constructor
	 * builds bandit solver from existing solver
	 * @param bandit the existing bandit solver 
	 * @param comm communication number (0 = broadcast, 1 = smooth, 2 = step)
	 * @param algo full qualifier name of auction schedule
	 */
	public AbstractBanditSolver(AbstractBanditSolver bandit, int comm, String algo){
		  this.armsCount = bandit.armsCount;
		  this.armsTotalReward = bandit.armsTotalReward;
		  this.epsilon = bandit.epsilon;
		  this._interval = bandit._interval;
		  this.count = bandit.count;
		  this.randomG = bandit.randomG;
		  this.allResults = bandit.allResults;
		  
		  int alg = 0;
		    try {
				if(Class.forName(algo).equals(epics.auctionSchedules.ActiveAINodeMulti.class))
					alg = 0;
				else
					alg = 1;
				
				currentStrategy = DecideStrategy(alg, comm);
			} catch (ClassNotFoundException e) {
				alg = -1;
			}
	  }
	
	public void init(int numberOfOptions){
	 // Initialise arm counter
        armsCount = new int[numberOfOptions];
        usedArms = new int[numberOfOptions];
        
        armsTotalReward = new double[numberOfOptions];
        allResults = new ArrayList<ArrayList<Double>>(numberOfOptions);
        
        for(int i = 0; i < numberOfOptions; i++){
            allResults.add(new ArrayList<Double>());
        }
        
        reinitialise();
	}
	
	/**
	 * selects the index for an algorithm-communication combination 
	 * @param algo auction schedule 
	 * @param comm communication policy
	 * @return
	 */
	private int DecideStrategy(int algo, int comm){
		  if(algo == 0){
			  switch (comm) {
				case 0:
					return 0;
				case 1:
					return 1;
				case 2:
					return 2;
				default:
					return 0;
			  }
		  }
		  else{
			  switch (comm) {
				case 0:
					return 3;
				case 1:
					return 4;
				case 2:
					return 5;
				default:
					return 0;
			  }
		  }
	  }
	
	/* (non-Javadoc)
	 * @see epics.common.IBanditSolver#getBanditSolver()
	 */
	@Override
	public AbstractBanditSolver getBanditSolver() {
		return this;
	}

	/**
	 * selects the best action based on the implemented method in the abstract class.
	 */
	/* (non-Javadoc)
	 * @see epics.common.IBanditSolver#selectAction()
	 */
	@Override	
	public abstract int selectAction();
	
	/* (non-Javadoc)
	 * @see epics.common.IBanditSolver#bestAction()
	 */
	@Override
    public abstract String bestAction();
	
	/*
	 * (non-Javadoc)
	 * @see epics.common.IBanditSolver#selectActionWithoutReward()
	 */
	@Override 
	public abstract int selectActionWithoutReward();
	
	/* (non-Javadoc)
	 * @see epics.common.IBanditSolver#setRewardForStrategy(int, double, double)
	 */
	public double setRewardForStrategy(int strategy, double performance, double communication){ //reward){
	    return setRewardForStrategy(strategy, performance, communication, _nrObjects); 
	}
	
	public double setRewardForStrategy(int strategy, double reward){
	    armsTotalReward[strategy] += reward;
	    usedArms[strategy]++;
        armsCount[strategy]++;
	    return armsTotalReward[strategy];
	}

	/**
	 * calculates the reward and assigns it to the given strategy
	 * 
	 * Performance is always normalized by the maximum performance.
	 * Communication can be normalised by (i) maximum communication 
	 * or by (ii) distribution using 
	 * 
	 * @param strategy the strategy to assign the reward to 
	 * @param performance the performance of the tracking
	 * @param communication the communication needed
	 * @param nrObjects number of tracked objects - needed if performance normalised by objects
	 * @return
	 */
	public double setRewardForStrategy(int strategy, double performance, double communication, double nrObjects){ //reward){
	    double cHeadValue = 0.0;
        double cTotalValue = 0.0;
        
	    if(normalized){
            if(maxComm < communication){
                maxComm = communication;
//                reinitialise();
            }

            if(maxPerf < performance){
                maxPerf = performance;
//                reinitialise();
            }
        }
	    else{
	        if(normalizedByDist){
    	            SortedMap<Double, Integer> head = commHist.headMap(communication);
    	            
    	            for (Map.Entry<Double, Integer> kvp : head.entrySet()) {
                        if(kvp.getKey() == communication){
                            cHeadValue += (kvp.getValue().doubleValue() / 2.0);
                        }
                        else{
                            cHeadValue += kvp.getValue().doubleValue();
                        }
                    }
    	            
    	            for(Map.Entry<Double, Integer> kvp : commHist.entrySet()){
    	                cTotalValue += kvp.getValue().doubleValue();
    	            }
    	            
    	            head = commHist.tailMap(communication);
	        }
	        if(normalizeConf){
	            maxPerf = nrObjects;
	        }
	    }
	    
        if(commHist.get(communication) != null){
            commHist.put(communication, Integer.valueOf(commHist.get(communication)+1));
        }
        else{
            commHist.put(communication, 1);
        }
        
        if(normalizedByDist){
            Double test = cHeadValue / cTotalValue;
            if(!test.isNaN()){
                communication = test.doubleValue();
            }
            else{
                communication = 0.0;
            }
        }
        
	    //Update our expectations
	    double reward = 0.0;
	    usedArms[strategy]++;
        armsCount[strategy]++;
	    if(normalized){
	            reward = (((alpha * (performance/maxPerf)) - ((1.0-alpha) * (communication/maxComm)))+(1.0-alpha));// reward;
	    }
	    else{
	            reward = ((alpha * (performance/maxPerf)) - ((1.0-alpha) * (communication/maxComm)));
	    }
	    armsTotalReward[strategy] += reward;
            
	    
	    for(int i = 0; i < allResults.size(); i++){
	    	if(strategy == i){
	    	    allResults.get(i).add(reward);//reward);
	    	    
	    	}
	    	else{    		
	    		allResults.get(i).add(0.0);
	    	}
	    }
	    
	    return reward;
	}
	
	public int getCurrentStrategy(){
	    return currentStrategy;
	}
	
	/**
	 * Reinitialises the entire banditsolver resetting all variables
	 */
	private void reinitialise() {
	    int numberOfOptions = armsCount.length;
	    // Initialise arm counter
        
        for (int i = 0; i < armsCount.length; i++) {
          armsCount[i] = 1;
          usedArms[i] = 0;
        }

        // Initialise total reward for each arm. We do this using very small random
        // numbers rather than zero. This simplifies the rest of the
        // algorithm's implementation, since we don't then need a special case for
        // the first iteration of the loop.
        for (int i = 0; i < armsCount.length; i++) {
          armsTotalReward[i] = randomG.nextDouble(USE.BANDIT)/1000000.0;
        }
        
    }

    /* (non-Javadoc)
     * @see epics.common.IBanditSolver#setCurrentReward(double, double)
     */
    public void setCurrentReward(double tracking_performance, double communication){
	    
        totalPerformance += tracking_performance;
	    totalCommunication += communication;
		count++;
	}
    
    /* (non-Javadoc)
     * @see epics.common.IBanditSolver#setCurrentReward(double, double, double)
     */
    public void setCurrentReward(double tracking_performance, double communication, double nrObjects){
        totalPerformance += tracking_performance;
        totalCommunication += communication;
        _nrObjects = nrObjects;
        count++;
    }
	
	/* (non-Javadoc)
	 * @see epics.common.IBanditSolver#getTotalReward()
	 */
	@Override
	public double[] getTotalReward() {
		return armsTotalReward;
	}

	/* (non-Javadoc)
	 * @see epics.common.IBanditSolver#getTotalArms()
	 */
	@Override
	public int[] getTotalArms() {
		return usedArms;//armsCount;
	}
	
	/* (non-Javadoc)
	 * @see epics.common.IBanditSolver#getResults()
	 */
	@Override
	public ArrayList<ArrayList<Double>> getResults(){
		return allResults;
	}

	
	public double getAlpha(){
	    return alpha;
	}
	
	public double getBeta(){
	    return beta;
	}
	
	public double getGamma(){
	    return gamma;
	}
}
