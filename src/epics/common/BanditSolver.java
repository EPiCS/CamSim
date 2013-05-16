package epics.common;

import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import epics.common.RandomUse.USE;


public abstract class BanditSolver implements IBanditSolver {

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
	public BanditSolver(int numberOfOptions, double epsilon, double alpha, int interval, RandomNumberGenerator rg) {
		  
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
	 * @param eg
	 * @param comm
	 * @param algo
	 */
	public BanditSolver(BanditSolver eg, int comm, String algo){
		  this.armsCount = eg.armsCount;
		  this.armsTotalReward = eg.armsTotalReward;
		  this.epsilon = eg.epsilon;
		  this._interval = eg._interval;
		  this.count = eg.count;
		  this.randomG = eg.randomG;
		  this.allResults = eg.allResults;
		  
		  int alg = 0;
		    try {
				if(Class.forName(algo).equals(epics.ai.ActiveAINodeMulti.class))
					alg = 0;
				else
					alg = 1;
				
				currentStrategy = DecideStrategy(alg, comm);
			} catch (ClassNotFoundException e) {
				alg = -1;
			}
	  }
	
	/**
	 * @param cls
	 * @param comm
	 * @return
	 */
	private int DecideStrategy(int cls, int comm){
		  if(cls == 0){
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
	public BanditSolver getBanditSolver() {
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
	
	/* (non-Javadoc)
	 * @see epics.common.IBanditSolver#setRewardForStrategy(int, double, double)
	 */
	public double setRewardForStrategy(int strategy, double performance, double communication){ //reward){
	    return setRewardForStrategy(strategy, performance, communication, _nrObjects); 
	}

	/**
	 * @param strategy
	 * @param performance
	 * @param communication
	 * @param nrObjects
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
//	    if(performance > 0.0){
	        armsCount[strategy]++;
    	    if(normalized){
    	            reward = (((alpha * (performance/maxPerf)) - ((1.0-alpha) * (communication/maxComm)))+(1.0-alpha));// reward;
    	    }
    	    else{
    	            reward = ((alpha * (performance/maxPerf)) - ((1.0-alpha) * (communication/maxComm)));
    	    }
    	    armsTotalReward[strategy] += reward;
            
//            for(int i = 0; i< armsTotalReward.length; i++) System.out.println(i + ": " + performance + " [" + performance / maxPerf + "] + " + communication + " [" + communication /maxComm + "] - " + reward + " = " + armsTotalReward[i]/armsCount[i]);
//	    }
//	    else{
//	        System.out.println("no Reward");
//	    }
	    
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
	
	/**
	 * 
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
        
        
//        counterinit ++;
//        System.err.println(" init: " + counterinit);
        
    }

    /* (non-Javadoc)
     * @see epics.common.IBanditSolver#setCurrentReward(double, double)
     */
    public void setCurrentReward(double tracking_performance, double communication){
	    
        totalPerformance += tracking_performance;
	    totalCommunication += communication;
	    
//	    currentReward += (alpha * (tracking_performance/maxPerf)) - ((1-alpha) * (communication/maxComm));//Math.abs((alpha * tracking_performance) - ((1-alpha) * communication));// tracking_performance / communication; //utility;
		count++;
//		return currentReward;
	}
    
    /* (non-Javadoc)
     * @see epics.common.IBanditSolver#setCurrentReward(double, double, double)
     */
    public void setCurrentReward(double tracking_performance, double communication, double nrObjects){
        totalPerformance += tracking_performance;
        totalCommunication += communication;
        _nrObjects = nrObjects;
//      currentReward += (alpha * (tracking_performance/maxPerf)) - ((1-alpha) * (communication/maxComm));//Math.abs((alpha * tracking_performance) - ((1-alpha) * communication));// tracking_performance / communication; //utility;
        count++;
//      return currentReward;
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

}
