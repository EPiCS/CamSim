/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package epics.common;

import java.util.Random;

/**
 *
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public class RandomNumberGenerator {
    
    private static Random r = null;

    public static void init( long seed ){
        r = new Random( seed );
    }

    public static double nextDouble(){
        return r.nextDouble();
    }

    public static int nextInt(){
        return r.nextInt();
    }

    public static int nextInt( int n ){
        return r.nextInt( n );
    }

}
