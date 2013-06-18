package epics.common;

import java.util.Random;

/**
 *
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public class RandomNumberGenerator {
    
    private Random ranUniversal = null;
    private Random ranFalseObj = null;
    private Random ranTurn = null;
    private Random ranComm = null;
    private Random ranError = null;
    private Random ranBandit = null;
    private long _seed;
    long threadId;

    public RandomNumberGenerator(long seed) {
    	init(seed);
        _seed = seed;
    	threadId = Thread.currentThread().getId();
	}
    
    private void init( long seed ){
    	ranUniversal = new Random( seed );
    	ranFalseObj = new Random(seed);
    	ranTurn = new Random(seed);
    	ranComm = new Random(seed);
    	ranError = new Random(seed);
    	ranBandit = new Random(seed);
        _seed = seed;
    }

    public double nextDouble(RandomUse.USE u)  {
//    	if(threadId != Thread.currentThread().getId()){
//    		System.out.println("thread " + Thread.currentThread().getId() + " not equal to initiator thread ("+ threadId +")");
//    	}
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
			default:
				return ranUniversal.nextDouble();
		}
    }
    

    public int nextInt(RandomUse.USE u){
//    	if(threadId != Thread.currentThread().getId()){
//    		System.out.println("thread " + Thread.currentThread().getId() + " not equal to initiator thread ("+ threadId +")");
//    	}
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
			default:
				return ranUniversal.nextInt();
		}
    }
    
//    public static double nextInt(){
//    	return RandomNumberGenerator.nextInt(RandomUse.USE.UNIV);
//    }

    public int nextInt( int n, RandomUse.USE u ){
//    	if(threadId != Thread.currentThread().getId()){
//    		System.out.println("thread " + Thread.currentThread().getId() + " not equal to initiator thread ("+ threadId +")");
//    	}
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
			default:
				return ranUniversal.nextInt(n);
		}
    }

    public long getSeed() {
        return _seed;
    }
    

}
