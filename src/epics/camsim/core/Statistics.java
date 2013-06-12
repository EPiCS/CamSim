package epics.camsim.core;

import java.io.File;
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
	private static String summary = null;
    private static int time_step = 0;

    public static int get_time_step() {
        return time_step;
    }
    private static double util_tmp = 0;
    private static double comm_tmp = 0;
    private static double handover_tmp = 0;
    private static int ident_tmp = 0;
    private static int visible_tmp = 0;
    private static double util_cumulative = 0;
    private static double comm_cumulative = 0;
    private static double handover_cumulative = 0;
    private static Map<String, Map<String,Double>> tmp_camUtil = new HashMap<String, Map<String,Double>>();
    private static ArrayList<Integer> time = new ArrayList<Integer>();
    private static ArrayList<Double> utility = new ArrayList<Double>();
    private static ArrayList<Double> communication = new ArrayList<Double>();
    private static ArrayList<Double> handover = new ArrayList<Double>();
    private static ArrayList<Integer> identification = new ArrayList<Integer>();
    private static ArrayList<Integer> visible = new ArrayList<Integer>();
    		
    static {
        init(null, null);
    }

    public static void init(String outputFile, String summaryFile) {
        output = outputFile;
        summary = summaryFile;
        visible_tmp = 0;
        time_step = 0;
        util_tmp = 0;
        comm_tmp = 0;
        handover_tmp = 0;
        ident_tmp = 0;
        util_cumulative = 0;
        comm_cumulative = 0;
        handover_cumulative = 0;
        time.clear();
        utility.clear();
        communication.clear();
        handover.clear();
        identification.clear();
        visible.clear();
    }

    public static void close() {
        if (output != null) {

        	PrintWriter out = null;
        	PrintWriter sumOut = null;
            try {
                FileWriter outFile = new FileWriter(output);
                out = new PrintWriter(outFile);

                out.println( "time;utility;communication;visible;");
                String outString = "";
                for ( int i = 0; i < time.size(); i++ ){
                    outString = (time.get(i) + ";" + utility.get(i) + ";" + communication.get(i) + ";" + visible.get(i));
                    
                    out.println(outString);
                }
                
                if (summary != null && ! summary.equals("")) {
                	File sumFile = new File(summary);
                	File f1 = new File(sumFile.getParent());
                	File f2 = new File(f1.getParent());
                	boolean creat2 = f2.mkdir();
                	boolean creat = f1.mkdir();
                	boolean existed = sumFile.exists();
                	FileWriter sumFileWriter = new FileWriter(sumFile, true); // Append
                    sumOut = new PrintWriter(sumFileWriter);

                    if (! existed) {
                    	sumOut.println(getSummaryDesc(false));
                    }
                    sumOut.println(getSummary(false));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            finally{
                if (out != null) {
                    out.close();
                }
                if (sumOut != null) {
                	sumOut.close();
                }
            }
        }
    }

    public static void nextTimeStep() {
        time.add(time_step);
        utility.add(util_tmp);
        communication.add(comm_tmp);
        handover.add(handover_tmp);
        identification.add(ident_tmp);
        visible.add(visible_tmp);
        util_cumulative += util_tmp;
        comm_cumulative += comm_tmp;
        handover_cumulative += handover_tmp;

        System.out.println(getSummaryDesc(true));
        System.out.println(getSummary(true));
        System.out.println("--------------------------------------------------------------------------");
        
        time_step++;
        util_tmp = 0;
        comm_tmp = 0;
        handover_tmp = 0;
        ident_tmp = 0;
        visible_tmp = 0;
        tmp_camUtil = new HashMap<String, Map<String,Double>>();
    }

    /** Spaces=true inserts a space after each value for human-readable version */
    public static String getSummary(boolean spaces) {
    	String comma = spaces ? ", " : ",";
    	String summary = time_step + comma 
    			+ util_tmp + comma 
    			+ util_cumulative + comma 
    			+ comm_tmp + comma 
    			+ comm_cumulative + comma 
    			+ handover_tmp + comma 
    			+ handover_cumulative + comma
    			+ ident_tmp + comma 
    			+ visible_tmp;
    	return summary;
    }
    
    /** Spaces=true inserts a space after each field name for human-readable version */
    public static String getSummaryDesc(boolean spaces) {
    	String comma = spaces ? ", " : ",";
    	String desc = "TIME" + comma 
    			+ "GLOBAL_UTILITY" + comma 
    			+ "CUMULATIVE_UTILITY" + comma
//    			+ "COMMUNICATION" + comma 
//    			+ "CUMULATIVE_COMM" + comma 
    			+ "HANDOVER" + comma
    			+ "CUMULATIVE_HANDOVER" + comma
    			+ "MISIDENTIFICATION" + comma 
    			+ "VISIBLE";
    	return desc;
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

    public static void addHandover(double handover) {
        handover_tmp += handover;
    }

	public static void addMissidentified(int currentlyMissidentified) {
		ident_tmp += currentlyMissidentified;
	}

	public static void addCamUtility(String cam, Map<String, Double> camUtility) {
		tmp_camUtil.put(cam, camUtility);
	}
}
