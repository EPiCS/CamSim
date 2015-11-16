package epics.common;

import java.util.ArrayList;

/**
 * output for console
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public class CmdLogger {

    private static ArrayList<String> printQueue = new ArrayList<String>();

    /**
     * prints a string to console
     * @param str
     */
    public static void println( String str ){
        System.out.println( str );
        //printQueue.add( str );
    }

    /**
     * checks if print queue has remaining elements
     * @return status of remaining elements in print queue 
     */
    public static boolean hasSomething(){
        return printQueue.size() > 0;
    }

    /**
     * updates the print queue
     */
    public static void update(){

        if ( printQueue.size() > 0 ){

            String str = printQueue.get(0);
            printQueue.remove(0);
            System.out.println( str );

        }

    }


}
