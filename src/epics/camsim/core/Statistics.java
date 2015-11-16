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

    private boolean quiet = true;
    
	private String output = null;
	private String summary = null;
    private int time_step = 0;

    /**
     * return current time step
     * @return
     */
    public int get_time_step() {
        return time_step;
    }
    
    private double util_tmp = 0;
    private double comm_tmp = 0;
    private double handover_tmp = 0;
    private int visible_tmp = 0;
    private double util_cumulative = 0;
    private double comm_cumulative = 0;
    private double handover_cumulative = 0;
    private double overlap_cumulative = 0;
    private double overlap_tmp = 0;
    private Map<String, Map<String,Double>> tmp_camUtil = new HashMap<String, Map<String,Double>>();
    private Map<String, Statistics> perCam = new HashMap<String, Statistics>();
    private ArrayList<Integer> time = new ArrayList<Integer>();
    private ArrayList<Double> utility = new ArrayList<Double>();
    private ArrayList<Double> communication = new ArrayList<Double>();
    private ArrayList<Double> handover = new ArrayList<Double>();
    private ArrayList<Integer> visible = new ArrayList<Integer>();
    private ArrayList<Integer> strategy = new ArrayList<Integer>();
    private ArrayList<Double> overlap = new ArrayList<Double>();
    
	private Integer strategy_tmp;
	private double tmp_totutil;
	private ArrayList<Double> totUtil = new ArrayList<Double>();
	private ArrayList<Double> totComOH = new ArrayList<Double>();
	private boolean allStatistics;
	private double comm_oh_tmp;
	private double comm_oh_cumulative;
	private boolean addPerCam = false;
    private long _randSeed;

    private ArrayList<Double> proportion = new ArrayList<Double>();

    private double proportion_cumulative = 0;

    private double proportion_tmp = 0;

    private double confidence_cumulative = 0;

    private double confidence_tmp = 0;

    private ArrayList<Double> confidence = new ArrayList<Double>();

    
    /**
     * Constructor for Statistics.java
     * @param outputFile
     * @param summaryFile
     * @param allStatistics
     * @param randSeed
     */
    public Statistics(String outputFile, String summaryFile, boolean allStatistics, long randSeed) {
    	init(outputFile, summaryFile, allStatistics, randSeed);
    }

    /**
     * initiate statistics - reset everything
     * @param outputFile
     * @param summaryFile
     * @param allStatistics
     * @param randSeed
     */
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
        util_cumulative = 0;
        comm_cumulative = 0;
        handover_cumulative = 0;
        comm_oh_cumulative = 0;
        comm_oh_tmp = 0;
        time.clear();
        utility.clear();
        communication.clear();
        handover.clear();
        visible.clear();
        strategy.clear();
        totComOH.clear();
        totUtil.clear();
        overlap.clear();
        overlap_cumulative = 0;
        overlap_tmp = 0;
        proportion.clear();
        proportion_cumulative = 0;
        proportion_tmp = 0;
        confidence.clear();
        confidence_cumulative = 0;
        confidence_tmp = 0;
    }

    /**
     * close current statistics. write everything to file
     * @throws Exception
     */
    public void close() throws Exception{
        if (output != null) {

        	PrintWriter out = null;
        	PrintWriter sumOut = null;
            try {
            	if(allStatistics){
	                FileWriter outFile = new FileWriter(output);
	                out = new PrintWriter(outFile);
	
	                out.println( "time;utility;communication;visible;strategy;totUtil;totComm;overlap;confidence;proportion");
	                String outString = "";
	                for ( int i = 0; i < time.size(); i++ ){
	                    outString = (time.get(i) + ";" + utility.get(i) + ";" + communication.get(i) + ";" + visible.get(i));
	                    outString = outString  + ";" + (strategy.get(i) == null ? 0 : strategy.get(i))+ ";" + totUtil.get(i) + ";" + totComOH.get(i) + ";" + overlap.get(i) + ";" + confidence.get(i) + ";" + proportion.get(i);
	                    
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

    /**
     * prepare for next timestep - save everything, and reset temporary values
     * @throws Exception
     */
    public void nextTimeStep() throws Exception{
        time.add(time_step);
        utility.add(util_tmp);
        communication.add(comm_tmp);
        handover.add(handover_tmp);
        visible.add(visible_tmp);
        util_cumulative += util_tmp;
        comm_cumulative += comm_tmp;
        handover_cumulative += handover_tmp;
        comm_oh_cumulative += comm_oh_tmp;
        strategy.add(strategy_tmp);
        totUtil.add(tmp_totutil);
        totComOH.add(comm_oh_tmp);
        overlap.add(overlap_tmp);
        overlap_cumulative += overlap_tmp;
        proportion.add(proportion_tmp);
        proportion_cumulative += proportion_tmp;
        confidence.add(confidence_tmp);
        confidence_cumulative += confidence_tmp;

        if(!quiet){
            System.out.println(getSummaryDesc(true));
            System.out.println(getSummary(true));
            System.out.println("--------------------------------------------------------------------------");
        }
    
        time_step++;
        util_tmp = 0;
        comm_tmp = 0;
        handover_tmp = 0;
        comm_oh_tmp = 0;
        visible_tmp = 0;
        tmp_totutil = 0.0;
        tmp_camUtil = new HashMap<String, Map<String,Double>>();
        strategy_tmp = 0;
        overlap_tmp = 0;
        proportion_tmp = 0;
        confidence_tmp = 0;
        
        for(Statistics s : perCam.values()){
        	s.nextTimeStep();
        }
    }

    /** 
     * Spaces=true inserts a space after each value for human-readable version 
     * @param spaces 
     * @return 
     * @throws Exception 
     **/
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
    			+ visible_tmp + comma
    			+ overlap_cumulative + comma
    			+ confidence_cumulative + comma
    			+ proportion_cumulative; 
    	return summary;
    }
    
    /** 
     * Spaces=true inserts a space after each field name for human-readable version 
     * @param spaces 
     * @return 
     * @throws Exception 
     **/
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
    			+ "CUMULATIVE_COMM_OH" + comma 
    			+ "VISIBLE" + comma
    			+ "OVERLAP" + comma
    			+ "CONFIDENCE" + comma
    			+ "PROPORTION";
    	return desc;
    }
    
    /**
     * 
     * @throws Exception
     */
    public void addVisible() throws Exception{
    }

    /**
     * add utility for one timestep
     * @param utility
     * @param camName
     * @throws Exception
     */
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

    /**
     * add communication for one timestep
     * @param communication
     * @param camName
     * @throws Exception
     */
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
    
    /**
     * add confidence for one timestep
     * @param conf
     * @param camName
     * @throws Exception
     */
    public void addConfidence(double conf, String camName) throws Exception{
        confidence_tmp += conf;
        
        if(addPerCam){
            if(!camName.isEmpty()){
                if(!perCam.containsKey(camName)){
                    createCamStatistics(camName);   
                }
                perCam.get(camName).addConfidence(conf, "");
            }
        }
    }
    
    /**
     * add proportion for one timestep
     * 
     * @param prop
     * @param camName
     * @throws Exception
     */
    public void addProportion(double prop, String camName) throws Exception{
        proportion_tmp += prop;
        
        if(Double.isNaN(proportion_tmp)){
            System.out.println("AAAHAHA");
        }
        
        if(addPerCam){
            if(!camName.isEmpty()){
                if(!perCam.containsKey(camName)){
                    createCamStatistics(camName);   
                }
                perCam.get(camName).addProportion(prop, "");
            }
        }
    }
    
    /**
     * add communication overhead for one timestep 
     * @param overhead
     * @param camName
     * @throws Exception
     */
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

    /**
     * add handovers for current timestep
     * @param handover
     */
    public void addHandover(double handover) {
        handover_tmp += handover;
    }

    /**
     * add utility of individual cameras for this timestep.
     * @param cam
     * @param camUtility
     * @param camName
     * @throws Exception
     */
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
	
	/**
	 * 
	 * @param strat
	 * @param camName
	 */
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

	/**
	 * set reward of this timestep
	 * @param utility2
	 * @param commOverhead
	 * @param camName
	 */
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
	
	/**
	 * add overlap for this timestep
	 * @param en
	 * @param camName
	 */
	public void addOverlap(double en, String camName){
	    overlap_tmp += en;
        
        if(addPerCam){
            if(!camName.isEmpty()){
                if(!perCam.containsKey(camName)){
                    createCamStatistics(camName);   
                }
                perCam.get(camName).addOverlap(en, "");
            }
        }
	}
	
	/**
	 * no output on command line
	 * @param q
	 */
	public void setQuiet(boolean q){
	    quiet = q;
	}
	
	/**
	 * print information per camera to files
	 * @param camName
	 */
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
