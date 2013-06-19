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

	private String output = null;
	private String summary = null;
    private int time_step = 0;

    public int get_time_step() {
        return time_step;
    }
    
    private double util_tmp = 0;
    private double comm_tmp = 0;
    private double handover_tmp = 0;
    private int ident_tmp = 0;
    private int visible_tmp = 0;
    private double util_cumulative = 0;
    private double comm_cumulative = 0;
    private double handover_cumulative = 0;
    private Map<String, Map<String,Double>> tmp_camUtil = new HashMap<String, Map<String,Double>>();
    private Map<String, Statistics> perCam = new HashMap<String, Statistics>();
    private ArrayList<Integer> time = new ArrayList<Integer>();
    private ArrayList<Double> utility = new ArrayList<Double>();
    private ArrayList<Double> communication = new ArrayList<Double>();
    private ArrayList<Double> handover = new ArrayList<Double>();
    private ArrayList<Integer> identification = new ArrayList<Integer>();
    private ArrayList<Integer> visible = new ArrayList<Integer>();
    private ArrayList<Integer> strategy = new ArrayList<Integer>();
    
    private long threadId;
	private Integer strategy_tmp;
	private double tmp_totutil;
	private ArrayList<Double> totUtil = new ArrayList<Double>();
	private ArrayList<Double> totComOH = new ArrayList<Double>();
	private boolean allStatistics;
	private double comm_oh_tmp;
	private double comm_oh_cumulative;
	private boolean addPerCam = false;
    private long _randSeed;
//    static {
//        init(null, null);
//    }
    
    public Statistics(String outputFile, String summaryFile, boolean allStatistics, long randSeed) {
    	init(outputFile, summaryFile, allStatistics, randSeed);
    }

    public void init(String outputFile, String summaryFile, boolean allStatistics, long randSeed) {
    	this.allStatistics = allStatistics;
    	_randSeed = randSeed;
        output = outputFile;
        if(outputFile.contains("/")){
        	summary = outputFile.substring(0, outputFile.lastIndexOf('/')) + "summary.csv";// summaryFile.substring(0, summaryFile.lastIndexOf('/')) + "summary.csv"; //summaryFile;
        }
        else{
        	summary = "summary.csv";
        }
        strategy_tmp = 0;
        visible_tmp = 0;
        time_step = 0;
        util_tmp = 0;
        comm_tmp = 0;
        handover_tmp = 0;
        ident_tmp = 0;
        util_cumulative = 0;
        comm_cumulative = 0;
        handover_cumulative = 0;
        comm_oh_cumulative = 0;
        comm_oh_tmp = 0;
        time.clear();
        utility.clear();
        communication.clear();
        handover.clear();
        identification.clear();
        visible.clear();
        strategy.clear();
        totComOH.clear();
        totUtil.clear();
    }

    public void close() throws Exception{
//    	if(threadId != Thread.currentThread().getId()){
//			throw new Exception("thread " + Thread.currentThread().getId() + "not equal to initiator thread ("+ threadId +")");
//    	}
        if (output != null) {

        	PrintWriter out = null;
        	PrintWriter sumOut = null;
            try {
            	if(allStatistics){
	                FileWriter outFile = new FileWriter(output);
	                out = new PrintWriter(outFile);
	
	                out.println( "time;utility;communication;visible;strategy;totUtil;totComm");
	                String outString = "";
	                for ( int i = 0; i < time.size(); i++ ){
	                    outString = (time.get(i) + ";" + utility.get(i) + ";" + communication.get(i) + ";" + visible.get(i));
	                    outString = outString  + ";" + (strategy.get(i) == null ? 0 : strategy.get(i))+ ";" + totUtil.get(i) + ";" + totComOH.get(i);
	                    
	                    out.println(outString);
	                }
            	}
            	for(Statistics s : perCam.values()){
            		s.close();
            	}
                
                if (summary != null && ! summary.equals("")) {
                	File sumFile = new File(summary);
                	File f1 = new File(sumFile.getAbsoluteFile().getParent());
                	f1.mkdirs();
                	if (! f1.exists()) {
                		throw new IOException("Error: Parent directory of summary file could not be created: "+
                				f1.getAbsolutePath());
                	}
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

    public void nextTimeStep() throws Exception{
//    	if(threadId != Thread.currentThread().getId()){
//    		throw new Exception("thread " + Thread.currentThread().getId() + "not equal to initiator thread ("+ threadId +")");
//    	}
        time.add(time_step);
        utility.add(util_tmp);
        communication.add(comm_tmp);
        handover.add(handover_tmp);
        identification.add(ident_tmp);
        visible.add(visible_tmp);
        util_cumulative += util_tmp;
        comm_cumulative += comm_tmp;
        handover_cumulative += handover_tmp;
        comm_oh_cumulative += comm_oh_tmp;
        strategy.add(strategy_tmp);
        totUtil.add(tmp_totutil);
        totComOH.add(comm_oh_tmp);

        System.out.println(getSummaryDesc(true));
        System.out.println(getSummary(true));
        System.out.println("--------------------------------------------------------------------------");
        
        time_step++;
        util_tmp = 0;
        comm_tmp = 0;
        handover_tmp = 0;
        comm_oh_tmp = 0;
        ident_tmp = 0;
        visible_tmp = 0;
        tmp_totutil = 0.0;
        tmp_camUtil = new HashMap<String, Map<String,Double>>();
        strategy_tmp = 0;
        
        for(Statistics s : perCam.values()){
        	s.nextTimeStep();
        }
    }

    /** Spaces=true inserts a space after each value for human-readable version */
    public String getSummary(boolean spaces) throws Exception{
//    	if(threadId != Thread.currentThread().getId()){
//    		throw new Exception("thread " + Thread.currentThread().getId() + "not equal to initiator thread ("+ threadId +")");
//    	}
    	String comma = spaces ? ", " : ",";
    	String summary = _randSeed + comma 
    	        + time_step + comma 
    			+ util_tmp + comma 
    			+ util_cumulative + comma 
    			+ comm_tmp + comma 
    			+ comm_cumulative + comma 
    			+ handover_tmp + comma 
    			+ handover_cumulative + comma
    			+ comm_oh_cumulative + comma
    			+ ident_tmp + comma 
    			+ visible_tmp;
    	return summary;
    }
    
    /** Spaces=true inserts a space after each field name for human-readable version */
    public String getSummaryDesc(boolean spaces) throws Exception{
//    	if(threadId != Thread.currentThread().getId()){
//    		throw new Exception("thread " + Thread.currentThread().getId() + "not equal to initiator thread ("+ threadId +")");
//    	}
    	String comma = spaces ? ", " : ",";
    	String desc = "SEED" + comma 
    	        + "TIME" + comma 
    			+ "GLOBAL_UTILITY" + comma 
    			+ "CUMULATIVE_UTILITY" + comma
    			+ "COMMUNICATION" + comma
    			+ "CUMULATIVE_COMM" + comma 
    			+ "HANDOVER" + comma
    			+ "CUMULATIVE_HANDOVER" + comma
    			+ "COMMUNICATION" + comma 
    			+ "CUMULATIVE_COMM" + comma
    			+ "CUMULATIVE_COMM_OH" + comma 
    			+ "MISIDENTIFICATION" + comma 
    			+ "VISIBLE";
    	return desc;
    }
    
    public void addVisible() throws Exception{
    }

    public void addUtility(double utility, String camName) throws Exception{
//    	if(threadId != Thread.currentThread().getId()){
//    		throw new Exception("thread " + Thread.currentThread().getId() + " not equal to initiator thread ("+ threadId +")");
//    	}
        util_tmp += utility;
        
        if(addPerCam){
	        if(!camName.isEmpty()){
	        	if(!perCam.containsKey(camName)){
	        		createCamStatistics(camName);	
	        	}
	        	perCam.get(camName).addUtility(utility, "");
	        }
    	}
    }

    public void addCommunication(double communication, String camName) throws Exception{
//    	if(threadId != Thread.currentThread().getId()){
//    		throw new Exception("thread " + Thread.currentThread().getId() + " not equal to initiator thread ("+ threadId +")");
//    	}
        comm_tmp += communication;
        
        if(addPerCam){
	        if(!camName.isEmpty()){
	        	if(!perCam.containsKey(camName)){
	        		createCamStatistics(camName);	
	        	}
	        	perCam.get(camName).addCommunication(communication, "");
	        }
        }
    }
    
    public void setCommunicationOverhead(double overhead, String camName) throws Exception{
    	comm_oh_tmp += overhead;
    	if(addPerCam){
	    	if(!camName.isEmpty()){
	        	if(!perCam.containsKey(camName)){
	        		createCamStatistics(camName);	
	        	}
	        	perCam.get(camName).setCommunicationOverhead(overhead, "");
	        }
    	}
    }

    public void addHandover(double handover) {
        handover_tmp += handover;
    }

	public void addMissidentified(int currentlyMissidentified, String camName)  throws Exception{
		ident_tmp += currentlyMissidentified;
		
		if(addPerCam){
			if(!camName.isEmpty()){
	        	if(!perCam.containsKey(camName)){
	        		createCamStatistics(camName);	
	        	}
	        	perCam.get(camName).addMissidentified(currentlyMissidentified, "");
	        }
		}
	}

	public void addCamUtility(String cam, Map<String, Double> camUtility, String camName)  throws Exception{
//    	if(threadId != Thread.currentThread().getId()){
//    		throw new Exception("thread " + Thread.currentThread().getId() + " not equal to initiator thread ("+ threadId +")");
//    	}
		tmp_camUtil.put(cam, camUtility);
		if(addPerCam){
			if(!camName.isEmpty()){
	        	if(!perCam.containsKey(camName)){
	        		createCamStatistics(camName);	
	        	}
	        	perCam.get(camName).addCamUtility(cam, camUtility, "");
	        }
		}
	}
	
	public void setStrat(int strat, String camName){
		strategy_tmp ++;
		
		if(addPerCam){
			if(!camName.isEmpty()){
	        	if(!perCam.containsKey(camName)){
	        		createCamStatistics(camName);	
	        	}
	        	perCam.get(camName).setStrat(strat, "");
	        }
		}
	}

	public void setReward(double utility2, double commOverhead, String camName) {
		tmp_totutil += utility2;
		
		if(addPerCam){
			if(!camName.isEmpty()){
	        	if(!perCam.containsKey(camName)){
	        		createCamStatistics(camName);	
	        	}
	        	perCam.get(camName).setReward(utility2, commOverhead, "");
	        }
		}
	}

	private void createCamStatistics(String camName) {
		if(output.contains("/")){
		    String ocam = output.substring(0, output.lastIndexOf('/')-1) + "_" + camName + output.substring(output.lastIndexOf('/')+1);
			perCam.put(camName, new Statistics(ocam, output.substring(0, output.lastIndexOf('/')-1) + "_" + camName + "_summary.csv", true, _randSeed));
		}
		else{
			perCam.put(camName, new Statistics(camName + output, camName + "_summary.csv", true, _randSeed));
		}
	}
}
