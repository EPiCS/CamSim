package epics.camsim.core;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public class Statistics {

    private static String output = null;
    private static int time_step = 0;

    public static int get_time_step() {
        return time_step;
    }
    private static double util_tmp = 0;
    private static double comm_tmp = 0;
    private static int ident_tmp = 0;
    private static int visible_tmp = 0;
    private static Map<String, Map<String,Double>> tmp_camUtil = new HashMap<String, Map<String,Double>>();
    private static ArrayList<Integer> time = new ArrayList<Integer>();
    private static ArrayList<Double> utility = new ArrayList<Double>();
    private static ArrayList<Double> communication = new ArrayList<Double>();
    private static ArrayList<Integer> identification = new ArrayList<Integer>();
    private static ArrayList<Integer> visible = new ArrayList<Integer>();
    private static ArrayList<Map<String, Map<String, Double>>> utilCam = new ArrayList<Map<String,Map<String,Double>>>();
    		
    static {
        init(null);
    }

    public static void init(String output_file) {
        output = output_file;
        visible_tmp = 0;
        time_step = 0;
        util_tmp = 0;
        comm_tmp = 0;
        ident_tmp = 0;
        time.clear();
        utility.clear();
        communication.clear();
        identification.clear();
        visible.clear();
    }

    public static void close() {
        if (output != null) {

            PrintWriter out = null;
            try {

                FileWriter outFile = new FileWriter(output);
                out = new PrintWriter(outFile);

                out.println( "time;utility;communication;visible;");
                String outString = "";
                for ( int i = 0; i < time.size(); i++ ){
                    outString = (time.get(i) + ";" + utility.get(i) + ";" + communication.get(i) + ";" + visible.get(i));
                    
                    out.println(outString);
                }
                

            } catch (IOException e) {
                e.printStackTrace();
            }
            finally{
                if ( out != null ){
                    out.close();
                }
            }
        }
    }

    public static void nextTimeStep() {
        time.add(time_step);
        utility.add(util_tmp);
        communication.add(comm_tmp);
        identification.add(ident_tmp);
        visible.add(visible_tmp);

        System.out.println("TIME, GLOBAL_UTILITY, COMMUNICATION, MISIDENTIFICATION, VISIBLE:\n    " + time_step + " , " + util_tmp + " , " + comm_tmp + " , " + ident_tmp + " , " + visible_tmp);

        time_step++;
        util_tmp = 0;
        comm_tmp = 0;
        ident_tmp = 0;
        visible_tmp = 0;
        tmp_camUtil = new HashMap<String, Map<String,Double>>();
    }
    
    public static void addVisible(){
    	visible_tmp ++;
    }

    public static void addUtility(double utility) {
        util_tmp += utility;
    }

    public static void addCommunication(double communication) {
        comm_tmp += communication;
    }

	public static void addMissidentified(int currentlyMissidentified) {
		ident_tmp += currentlyMissidentified;
	}

	public static void addCamUtility(String cam, Map<String, Double> camUtility) {
		tmp_camUtil.put(cam, camUtility);
	}
}
