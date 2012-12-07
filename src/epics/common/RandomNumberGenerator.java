package epics.common;

import java.util.Random;

/**
 *
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public class RandomNumberGenerator {
    
    private static Random ranUniversal = null;
    private static Random ranFalseObj = null;
    private static Random ranTurn = null;
    private static Random ranComm = null;
    private static Random ranError = null;

    public static void init( long seed ){
    	ranUniversal = new Random( seed );
    	ranFalseObj = new Random(seed);
    	ranTurn = new Random(seed);
    	ranComm = new Random(seed);
    	ranError = new Random(seed);
    }

    public static double nextDouble(RandomUse.USE u){
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
			default:
				return ranUniversal.nextDouble();
		}
    }
    
    public static double nextDouble(){
    	return RandomNumberGenerator.nextDouble(RandomUse.USE.UNIV);
    }
    
    public static int nextInt(RandomUse.USE u){
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
			default:
				return ranUniversal.nextInt();
		}
    }
    
    public static double nextInt(){
    	return RandomNumberGenerator.nextInt(RandomUse.USE.UNIV);
    }

    public static int nextInt( int n, RandomUse.USE u ){
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
			default:
				return ranUniversal.nextInt(n);
		}
    }
    
    public static double nextDouble(int n){
    	return RandomNumberGenerator.nextInt(n, RandomUse.USE.UNIV);
    }

}
