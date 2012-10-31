package epics.common;

import java.util.ArrayList;

/**
 *
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public class CmdLogger {

    private static ArrayList<String> printQueue = new ArrayList<String>();

    public static void println( String str ){
        System.out.println( str );
        //printQueue.add( str );
    }

    public static boolean hasSomething(){
        return printQueue.size() > 0;
    }

    public static void update(){

        if ( printQueue.size() > 0 ){

            String str = printQueue.get(0);
            printQueue.remove(0);
            System.out.println( str );

        }

    }


}
