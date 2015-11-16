package epics.common;

import java.util.Random;

/**
 *
 * @author Lukas Esterle and Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public class RandomNumberGenerator {
    
    private Random ranUniversal = null;
    private Random ranFalseObj = null;
    private Random ranTurn = null;
    private Random ranComm = null;
    private Random ranError = null;
    private Random ranBandit = null;
    private Random ranNormal = null;
    private Random ranDynamicSchedule = null;
    private long _seed;
    
    /**
     * 
     * Constructor for RandomNumberGenerator
     * @param seed initial seed
     */
    public RandomNumberGenerator(long seed) {
    	init(seed);
        _seed = seed;
	}
    
    private void init( long seed ){
    	ranUniversal = new Random( seed );
    	ranFalseObj = new Random(seed);
    	ranTurn = new Random(seed);
    	ranComm = new Random(seed);
    	ranError = new Random(seed);
    	ranBandit = new Random(seed);
    	ranNormal = new Random(seed);
    	ranDynamicSchedule = new Random(seed);
        _seed = seed;
    }

    /**
     * get a random double number for specific rng
     * @param u type of random number generator
     * @return
     */
    public double nextDouble(RandomUse.USE u)  {
    	switch (u) {
			case UNIV:
				return ranUniversal.nextDouble();
			case FALSEOBJ:
				return ranFalseObj.nextDouble();
			case TURN: 
				return ranTurn.nextDouble();
			case COMM:
				return ranComm.nextDouble();
			case ERROR:
				return ranError.nextDouble();
			case BANDIT:
				return ranBandit.nextDouble();
			case MOVE:
			    return ranNormal.nextDouble();
			case DYNSCHED:
			    return ranDynamicSchedule.nextDouble();
			default:
				return ranUniversal.nextDouble();
		}
    }
    
    /**
     * get an integer for a specific rng
     * @param u type of random number generator
     * @return
     */
    public int nextInt(RandomUse.USE u){
    	switch (u) {
			case UNIV:
				return ranUniversal.nextInt();
			case FALSEOBJ:
				return ranFalseObj.nextInt();
			case TURN: 
				return ranTurn.nextInt();
			case COMM:
				return ranComm.nextInt();
			case ERROR:
				return ranError.nextInt();
			case BANDIT:
				return ranBandit.nextInt();
			case MOVE:
                return ranNormal.nextInt();
			case DYNSCHED:
                return ranDynamicSchedule.nextInt();
			default:
				return ranUniversal.nextInt();
		}
    }
    
 /**
  * get an integer smaller n for specific rng
  * @param n maximum random number
  * @param u type of random number generator
  * @return
  */
    public int nextInt( int n, RandomUse.USE u ){
    	switch (u) {
			case UNIV:
				return ranUniversal.nextInt(n);
			case FALSEOBJ:
				return ranFalseObj.nextInt(n);
			case TURN: 
				return ranTurn.nextInt(n);
			case COMM:
				return ranComm.nextInt(n);
			case ERROR:
				return ranError.nextInt(n);
			case BANDIT:
				return ranBandit.nextInt(n);
			case MOVE:
                return ranNormal.nextInt(n);
			case DYNSCHED:
                return ranDynamicSchedule.nextInt(n);
			default:
				return ranUniversal.nextInt(n);
		}
    }
    
    /**
     * returns a normaldistributed random number with standarddeviation of std and mean value of mean
     * 
     * @param std the standardeviation of this normaldistribution
     * @param mean the mean value of the normaldistribution
     * @param u the used random number generator
     * @return the random, normaldistributed number
     */
    public double nextGaussian(double std, double mean, RandomUse.USE u){
        return mean+nextGaussian(u)*std;
    }
    
    /**
     * returns a standardnormaldistributed random number
     * @param u the used random number generator
     * @return the random, standardnormaldistributed number
     */
    public double nextGaussian(RandomUse.USE u){
        switch (u) {
            case UNIV:
                return ranUniversal.nextGaussian();
            case FALSEOBJ:
                return ranFalseObj.nextGaussian();
            case TURN: 
                return ranTurn.nextGaussian();
            case COMM:
                return ranComm.nextGaussian();
            case ERROR:
                return ranError.nextGaussian();
            case BANDIT:
                return ranBandit.nextGaussian();
            case MOVE:
                return ranNormal.nextGaussian();
            case DYNSCHED:
                return ranDynamicSchedule.nextGaussian();
            default:
                return ranUniversal.nextGaussian();
        }
    }

    /**
     * return initial seed
     * @return
     */
    public long getSeed() {
        return _seed;
    }
    

}
