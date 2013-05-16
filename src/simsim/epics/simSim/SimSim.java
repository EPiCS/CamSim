package simsim.epics.simSim;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.sun.jmx.snmp.Timestamp;
import com.sun.org.apache.bcel.internal.generic.NEW;

import epics.camsim.core.SimCore;
import epics.camsim.core.SimSettings;
import epics.camsim.core.SimSettings.CameraSettings;
import epics.camwin.SimCoreModel;
import epics.camwin.WindowMain;
import epics.common.RandomNumberGenerator;

/**
 * 
 * @author Lukas Esterle <Lukas.Esterle@aau.at>
 *
 */
enum States{
	ABC,
	AST,
	ASM,
	PBC,
	PST,
	PSM
}

public class SimSim {
	
    public static boolean allStatistics = false;
    public static int runRandomConfigs = 0;
	public static boolean runHomogeneous = false;
	public static boolean runSequential = false;
	public static boolean runByParameter = false;
	private static boolean runAllErrorVersions = false;
	public static boolean runAllPossibleVersions = true;
	public static boolean runBandits = true;
	private static boolean randomSeed = false; // DOES NOT MAKE SENSE TO USE!! SINCE THIS WOULD CHANGE THE PATH OF THE OBJECTS IN EVERY USE!!!
	private static boolean diffSeed = true;
	
	static boolean showgui = false;
	static int duration = 1000; //how many timesteps
	static int runs = 30;      // how many runs of a single simulation are being made - if diffSeed = true, each run uses a different random seed value
	static int startCamError = 20;
	static int endCamError = -1;
	static int camRate = 1;
	static int startReset = 70;
	static int endReset = 100;
	static int resetRate = 5;
	static int startTrackError = 30;
	static int endTrackError = -1;
	static int trackRate = 5;
	static long initialSeed = 10;
	
	static int epsilonRuns = 2; // how many epsilon / temperature values are being tried for the bandits
	static double standardEpsilon = 0.1;
	
	static double banditRuns = 20.0d; //how many different alpha values are being tried out 
	
	static File directory;
	static String totalDirName;
//	static String scenDirName;
//	static SimSettings ss;
	
	static ExecutorService exService;
	static Random ran = new Random(initialSeed);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		File folder;
		if(args.length > 0){
			folder = new File(args[0]);
		}
		else{
			folder = new File("..//..//..//..//scenarios//2Cams");
		}
		
		System.out.println("---> " + folder);
		
		File[] listOfFiles = folder.listFiles();
		
		DateFormat df = new SimpleDateFormat("ddMMYYYY");
		
		if(args.length > 1){
		    totalDirName = args[1] + df.format(new java.util.Date());
        }
        else{
            totalDirName = "..//..//..//..//..//..//Results//" + df.format(new java.util.Date());
        }
		
		directory = new File(totalDirName);
		int count = 0;
		while(!directory.mkdirs()){
			count++;
			directory = new File(totalDirName + "_" + count);
		}
		if(count > 0){
			totalDirName += "_" + count;
		}
		
		totalDirName = totalDirName + "//" + duration;
		
		if(listOfFiles != null){
			for(File f : listOfFiles){
				System.out.println(f.getAbsolutePath());
				//-o output_lukas_1_comm.csv e:\work\JavaStuff\testGIT\handover-simulation\source\scenarios\scenario_1_lukas_SMOOTH.xml
				//f.getName().substring(8, f.getName().indexOf("_", 9));
				String scenName;
				if(f.getName().indexOf("_", 9) != -1){
					scenName = f.getName().substring(8, f.getName().indexOf("_",9));
					
				}
				else{
					if(f.getName().toLowerCase().startsWith("scenario")){
						scenName = f.getName().substring(8, f.getName().length() - 4);
					}
					else{
						scenName = f.getName().substring(0, f.getName().length() - 4);
					}
				}
								
				String scenDirName = totalDirName + "//"+ scenName + "//";
				directory = new File(scenDirName);
				directory.mkdirs(); //for scenario
				
				
				exService = Executors.newFixedThreadPool(30);//newSingleThreadExecutor();
				
				
				if(runAllErrorVersions ){
					runSimulationsForAllErrors(runs, duration, f, scenName);
				}
				
				if(runBandits){
					runBanditSimulations(runs, duration, f, scenName);
				}
				
				if(runAllPossibleVersions){
					runSimulationForAll(runs, duration, f, scenName);
				}
				if(runHomogeneous){
					runHomogeneous(runs, duration, f, scenName);
				}
				if(runByParameter){
					runSimulationsByParameter(runs, duration, f, scenName);
				}
				if(runRandomConfigs > 0){
				    for(int i = 0; i < runRandomConfigs; i++){
				        runRandomStatic(runs, duration, f, scenName);
				    }
				}
			}
//			int nbRunning = 1;
//			while(nbRunning > 0){
//			    nbRunning = 0;
//    			for (Thread t : Thread.getAllStackTraces().keySet()) {
//    			    if (t.getState()==Thread.State.RUNNABLE) nbRunning++;
//    			}
//			}
			exService.shutdown();
//			try {
//				exService.awaitTermination(2, TimeUnit.MINUTES);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
			
			
			
			System.out.println("############ ALL SIMULATIONS COMPLETED! ############");				
		}

	}


	private static void runBanditSimulations(int runs2, int duration2, File f,
			String scenName) {
		long seed = initialSeed;
		String dirName = "";
		String scenDirName = totalDirName + "//"+ scenName + "//";
		SimSettings ss = new SimSettings("", "", 1);
		ss.loadFromXML(f.getAbsolutePath());
		for(int i = 0; i < ss.cameras.size(); i++){
			dirName += "ex";
		}
		
		BigDecimal alphaCoef = BigDecimal.valueOf(1.0d).divide(BigDecimal.valueOf(banditRuns));
		
		BigDecimal epsCoef = new BigDecimal(0.1d); //BigDecimal.valueOf(1.0d).divide(BigDecimal.valueOf(epsilonRuns));
		
		
		
		//Set for all cameras SoftMax bandit solving mechanism
		System.out.println("SOFTMAX");
		for (CameraSettings cs : ss.cameras) {
			cs.bandit = "epics.bandits.SoftMax";
		}
		if(epsCoef.doubleValue() > 0){
			for(int e = 0; e < epsilonRuns; e++){
				double epsilon = BigDecimal.valueOf(e+1).multiply(epsCoef).doubleValue(); 
				for(int i = 0; i <= banditRuns; i++){
					double alpha = alphaCoef.multiply(BigDecimal.valueOf(i)).doubleValue();
					//double alpha = 0.5;
					directory = new File(scenDirName + dirName + "//SoftMax-" +epsilon + "//" + alpha + "//");
					directory.mkdirs();
					//run all scenarios for a certain amount
					for(int r = 0; r < runs; r++){
			        	if (showgui == false) {
			        		
			        		if(randomSeed){
			        			seed = System.currentTimeMillis() % 1000;
			        		}
			        		else{
			        			if(diffSeed){
			        				seed = r;
			        			}
			        		}
			        		
			        		SimCore sim = new SimCore(seed, scenDirName + dirName + "//SoftMax-"+ epsilon+"//" + alpha + "//run" + r + ".csv", ss, false, epsilon, alpha, false, true);
			        				
			                for (int k = 0; k < duration; k++) {
			                    try {
									sim.update();
			                    } catch (Exception x) {
									x.printStackTrace();
								}
			                }
			                sim.close_files();
			            } 
			    	}
				}
			}
		}
		
		
//		//Set for all cameras UCB2 bandit solving mechanism
//		for (CameraSettings cs : ss.cameras) {
//			cs.bandit = "epics.bandits.UCB2";
//		}
//		if(epsCoef.doubleValue() > 0){
//			for(int i = 0; i < banditRuns; i++){
//				double alpha = alphaCoef.multiply(BigDecimal.valueOf(i + 1.0)).doubleValue();
//	//			double alpha = 0.5;
//				directory = new File(scenDirName + dirName + "//UCB2//" + alpha + "//");
//				directory.mkdirs();
//				//run all scenarios for a certain amount
//				for(int r = 0; r < runs; r++){
//		        	if (showgui == false) {
//		        		
//		        		if(randomSeed){
//		        			seed = System.currentTimeMillis() % 1000;
//		        		}
//		        		else{
//		        			if(diffSeed){
//		        				seed = r;
//		        			}
//		        		}
//		        		
//		        		SimCore sim = new SimCore(seed, scenDirName + dirName + "//UCB2//" + alpha + "//run" + r + ".csv", ss, false, 0.01, alpha, false, true);
//		        				
//		                for (int k = 0; k < duration; k++) {
//		                    try {
//								sim.update();
//		                    } catch (Exception x) {
//								x.printStackTrace();
//							}
//		                }
//		                sim.close_files();
//		            } 
//		    	}
//			}
//		}
//				
//		
//		
//		//Set for all cameras UCBTuned bandit solving mechanism
//		for (CameraSettings cs : ss.cameras) {
//			cs.bandit = "epics.bandits.UCBTuned";
//		}
//		if(epsCoef.doubleValue() > 0){
//		    for(int e = 0; e < epsilonRuns; e++){
//		        double epsilon = BigDecimal.valueOf(e+1).multiply(epsCoef).doubleValue();
//		        for(int i = 0; i < banditRuns; i++){
//				    double alpha = alphaCoef.multiply(BigDecimal.valueOf(i + 1.0)).doubleValue();
//	//			    double alpha = 0.5;
//				    directory = new File(scenDirName + dirName + "//UCBTuned-" + epsilon + "//" + alpha + "//");
//				    directory.mkdirs();
//				    //run all scenarios for a certain amount
//				    for(int r = 0; r < runs; r++){
//				        if (showgui == false) {
//				            if(randomSeed){
//				                seed = System.currentTimeMillis() % 1000;
//		        		    }
//		        		    else{
//		        			    if(diffSeed){
//		        				    seed = r;
//		        			    }
//		        		    }
//		        		
//		        		    SimCore sim = new SimCore(seed, scenDirName + dirName + "//UCBTuned-" + epsilon + "//" + alpha + "//run" + r + ".csv", ss, false, 0.01, alpha, false, true);
//    		        				
//    		                for (int k = 0; k < duration; k++) {
//    		                    try {
//    								sim.update();
//    		                    } catch (Exception x) {
//    								x.printStackTrace();
//    							}
//    		                }
//    		                sim.close_files();
//				        }
//		            } 
//		    	}
//			}
//		}
//		
//		
//		
//		
//		
//		
//		Set for all cameras epsilon greedy bandit solving mechanism
		for (CameraSettings cs : ss.cameras) {
			cs.bandit = "epics.bandits.EpsilonGreedy";
		}
		System.out.println("EPSILONGREEDY");
//		if(epsCoef.doubleValue() > 0){
//			for(int e = 0; e < epsilonRuns; e++){
//				double epsilon = BigDecimal.valueOf(e+1).multiply(epsCoef).doubleValue(); 
//				double alpha = 0.5;
//				directory = new File(scenDirName + dirName + "//epsilonGreedy-" +epsilon + "//" + alpha + "//");
//				directory.mkdirs();
//				//run all scenarios for a certain amount
//				for(int r = 0; r < runs; r++){
//		        	if (showgui == false) {
//		        		
//		        		if(randomSeed){
//		        			seed = System.currentTimeMillis() % 1000;
//		        		}
//		        		else{
//		        			if(diffSeed){
//		        				seed = r;
//		        			}
//		        		}
//		        		
//		        		SimCore sim = new SimCore(seed, scenDirName + dirName + "//epsilonGreedy-"+ epsilon+"//" + alpha + "//run" + r + ".csv", ss, false, epsilon, alpha, false, true);
//		        				
//		                for (int k = 0; k < duration; k++) {
//		                    try {
//								sim.update();
//		                    } catch (Exception x) {
//								x.printStackTrace();
//							}
//		                }
//		                sim.close_files();
//		            } 
//		    	}
//			}
//		}
		for(int i = 0; i <= banditRuns; i++){
			double alpha = alphaCoef.multiply(BigDecimal.valueOf(i)).doubleValue();
			directory = new File(scenDirName + dirName + "//epsilonGreedy//" + alpha + "//");
			directory.mkdirs();
			//run all scenarios for a certain amount
			for(int r = 0; r < runs; r++){
	        	if (showgui == false) {
	        		
	        		if(randomSeed){
	        			seed = System.currentTimeMillis() % 1000;
	        		}
	        		else{
	        			if(diffSeed){
	        				seed = r;
	        			}
	        		}
	        				        		
	        		SimCore sim = new SimCore(seed, scenDirName + dirName + "//epsilonGreedy//" + alpha + "//run" + r + ".csv", ss, false, 0.01, alpha, false, true);//output_file, ss, false);
	                for (int k = 0; k < duration; k++) {
	                    try {
							sim.update();
						} catch (Exception e) {
							e.printStackTrace();
						}
	                }
	                sim.close_files();
	            } 
	    	}
		}
		
		
//		for (CameraSettings cs : ss.cameras) {
//			cs.bandit = "epics.bandits.FullRandom";
//		}
//		for(int i = 0; i < banditRuns; i++){
//			//double alpha = alphaCoef.multiply(BigDecimal.valueOf(i + 1.0)).doubleValue();
//			directory = new File(scenDirName + dirName + "//fullRandom//" + i + "//");
//			directory.mkdirs();
//			//run all scenarios for a certain amount
//			for(int r = 0; r < runs; r++){
//	        	if (showgui == false) {
//	        		
//	        		if(randomSeed){
//	        			seed = System.currentTimeMillis() % 1000;
//	        		}
//	        		else{
//	        			if(diffSeed){
//	        				seed = r;
//	        			}
//	        		}
//		        		SimCore sim = new SimCore(seed, scenDirName + dirName + "//fullRandom//"+ i + "//run" + r + ".csv", ss, false, -1, 50, -1, i, false, true);//output_file, ss, false);
//		                for (int k = 0; k < duration; k++) {
//		                    try {
//								sim.update();
//							} catch (Exception e) {
//								e.printStackTrace();
//							}
//		                }
//		                sim.close_files();
//	            } 
//	    	}
//		}
//		
//		
		
		
//		//Set for all cameras epsilon greedy bandit solving mechanism
//		for (CameraSettings cs : ss.cameras) {
//			cs.bandit = "epics.bandits.UCB1e";
//		}
//		if(epsCoef.doubleValue() > 0){
//			for(int e = 0; e < epsilonRuns; e++){
//				double epsilon = BigDecimal.valueOf(e+1).multiply(epsCoef).doubleValue(); 
////				for(int i = 0; i < banditRuns; i++){
//					double alpha = 0.5; //alphaCoef.multiply(BigDecimal.valueOf(i + 1.0)).doubleValue();
//					directory = new File(scenDirName + dirName + "//UCB1e-" +epsilon + "//");
//					directory.mkdirs();
//					//run all scenarios for a certain amount
//					for(int r = 0; r < runs; r++){
//			        	if (showgui == false) {
//			        		
//			        		if(randomSeed){
//			        			seed = System.currentTimeMillis() % 1000;
//			        		}
//			        		else{
//			        			if(diffSeed){
//			        				seed = r;
//			        			}
//			        		}
//			        		
//			        		SimCore sim = new SimCore(seed, scenDirName + dirName + "//UCB1e-"+ epsilon+"//" + alpha +"//run" + r + ".csv", ss, false, epsilon, alpha, false, true);
//			        				
//			                for (int k = 0; k < duration; k++) {
//			                    try {
//									sim.update();
//			                    } catch (Exception x) {
//									x.printStackTrace();
//								}
//			                }
//			                sim.close_files();
//			            } 
//			    	}
////				}
//			}
//		}
//		
//		
//		
		//Set for all cameras epsilon greedy bandit solving mechanism
		for (CameraSettings cs : ss.cameras) {
			cs.bandit = "epics.bandits.UCB1";
		}
		System.out.println("UCB1");
		for(int i = 0; i <= banditRuns; i++){
			double alpha = alphaCoef.multiply(BigDecimal.valueOf(i)).doubleValue();
			directory = new File(scenDirName + dirName + "//ucb1//" + alpha + "//");
			
			//run all scenarios for a certain amount
			for(int r = 0; r < runs; r++){
	        	if (showgui == false) {
	        		
	        		if(randomSeed){
	        			seed = System.currentTimeMillis() % 1000;
	        		}
	        		else{
	        			if(diffSeed){
	        				seed = r;
	        			}
	        		}
	        		if(!runSequential){
	        			exService.execute(new SimRunner(seed, scenDirName + dirName + "//ucb1//" + alpha, "run" + r + ".csv", ss, false, -1, 50, -1, duration, alpha, false, true));
	        		}
	        		else{
	        			directory.mkdirs();
		        		SimCore sim = new SimCore(seed, scenDirName + dirName + "//ucb1//" + alpha + "//run" + r + ".csv", ss, false, -1, 50, -1, alpha, false, true);//output_file, ss, false);
		                for (int k = 0; k < duration; k++) {
		                    try {
								sim.update();
	//							System.out.println(i);
							} catch (Exception e) {
								e.printStackTrace();
							}
		                }
		                sim.close_files();
	        		}
	            } 
	    	}
		}
	}


	private static void runSimulationsForAllErrors(int runs2, int duration2, File f, String scenName) {
		SimSettings ss = new SimSettings("", "", 1);
		ss.loadFromXML(f.getAbsolutePath());
		long seed = initialSeed;
       		
		String dirName = "";
    	String outputfile = dirName;
    	String scenDirName = totalDirName + "//"+ scenName + "//";
    	directory = new File(scenDirName);
		//directory.mkdir(); //directory for currently used setting 
		
		for(int ce = startCamError; ce >= endCamError; ce=ce-camRate){
			for(int re = startReset; re <= endReset; re = re + resetRate){
				for(int te = startTrackError; te >= endTrackError; te = te - trackRate){

					dirName += ce + "_" + re + "_" + te + "//";
					directory = new File(scenDirName + dirName);
					//directory.mkdir(); //directory for currently used setting 
					
					//ADD DIFFERENT KINDS OF RUNS (ASM; ABC; AST; PSM; PBC; PST)...
					ArrayList<CameraSettings> item = ss.cameras; 
			        States[] input = {States.ABC, States.ASM, States.AST, States.PBC, States.PSM, States.PST};
			        String prevDir = dirName; 
					for (int i = 0; i < input.length; i++) {
						//set all algos / communication strategies
						for (int count = 0; count < item.size(); count++) {
							CameraSettings cs = item.get(count);
							switch (input[i]) {
							case ABC: 
								cs.ai_algorithm = "epics.ai.ActiveAINodeMulti";
								cs.comm = 0;
								dirName += "a0";
								break;
							case ASM:
								cs.ai_algorithm = "epics.ai.ActiveAINodeMulti";
								cs.comm = 1;
								dirName += "a1";
								break;
							case AST:
								cs.ai_algorithm = "epics.ai.ActiveAINodeMulti";
								cs.comm = 2;
								dirName += "a2";
								break;
							case PBC:
								cs.ai_algorithm = "epics.ai.PassiveAINodeMulti";
								cs.comm = 0;
								dirName += "p0";
								break;
							case PSM:
								cs.ai_algorithm = "epics.ai.PassiveAINodeMulti";
								cs.comm = 1;
								dirName += "p1";
								break;
							case PST:
								cs.ai_algorithm = "epics.ai.PassiveAINodeMulti";
								cs.comm = 2;
								dirName += "p2";
								break;
							default:
								break;
							}
							item.set(count, cs);
						}
						directory = new File(scenDirName + dirName);
						 //directory for currently used setting 
		            	
		            	//run all scenarios for a certain amount
						for(int r = 0; r < runs; r++){
				        	if (showgui == false) {
				        		
				        		if(randomSeed){
				        			seed = System.currentTimeMillis() % 1000;
				        		}
				        		else{
				        			if(diffSeed){
				        				seed = r;
				        			}
				        		}
				        		if(!runSequential){
				        			exService.execute(new SimRunner(seed, scenDirName + dirName, "run" + r + ".csv", ss, false, ce, re, te, duration, 0.5, false, false));
				        		}
				        		else{
				        			directory.mkdirs();
					                SimCore sim = new SimCore(seed, scenDirName + dirName + "//run" + r + ".csv", ss, false, ce, re, te, 0.5, false, false);//output_file, ss, false);
					
					                for (int dur = 0; dur < duration; dur++) {
					                    try {
											sim.update();
										} catch (Exception e) {
											e.printStackTrace();
										}
					                }
					                sim.close_files();
				        		}
				            } 
				    	}
						dirName = prevDir;
					}
					dirName = "";
				}
			}
		}
	     
	}

	public static void doVariation(LinkedList<ArrayList<CameraSettings>> reps, States[] input, SimSettings ss, int count, String scenDirName){// ArrayList<CameraSettings> item, int count, String scenDirName){
		long seed = initialSeed;
		SimSettings simS = ss.copy();
        if (count < simS.cameras.size()){//item.size()){
            for (int i = 0; i < input.length; i++) {
            	CameraSettings cs = simS.cameras.get(count).clone(); //item.get(count);
            	switch (input[i]) {
				case ABC: 
					cs.ai_algorithm = "epics.ai.ActiveAINodeMulti";
					cs.comm = 0;
					break;
				case ASM:
					cs.ai_algorithm = "epics.ai.ActiveAINodeMulti";
					cs.comm = 1;
					break;
				case AST:
					cs.ai_algorithm = "epics.ai.ActiveAINodeMulti";
					cs.comm = 2;
					break;
				case PBC:
					cs.ai_algorithm = "epics.ai.PassiveAINodeMulti";
					cs.comm = 0;
					break;
				case PSM:
					cs.ai_algorithm = "epics.ai.PassiveAINodeMulti";
					cs.comm = 1;
					break;
				case PST:
					cs.ai_algorithm = "epics.ai.PassiveAINodeMulti";
					cs.comm = 2;
					break;
				default:
					break;
				}
            	simS.cameras.set(count, cs); // item.set(count, cs);
            	
                doVariation(reps, input, simS, count+1, scenDirName); //item, count+1, scenDirName);
            }
        }else{
        	String dirName = "";
        	//ss.cameras = item;
        	for (int i = 0; i < simS.cameras.size(); i++){ //item.size(); i++) {
				if(simS.cameras.get(i).ai_algorithm.equals("epics.ai.ActiveAINodeMulti")){ // item.get(i).ai_algorithm.equals("epics.ai.ActiveAINodeMulti")){
					dirName += "a";
				}
				else{
					dirName += "p";
				}
				dirName += simS.cameras.get(i).comm; // item.get(i).comm;
			}
        	
        	File dir = new File(scenDirName + dirName);
			 //directory for currently used setting 
        	if(!runSequential){
				exService.execute(new SimRunner(seed, scenDirName + dirName, "summary.csv", runs, simS, false, -1, 50, -1, duration, 0.5, false, diffSeed, allStatistics));
			}
			else{
	        	for(int r = 0; r < runs; r++){
		        	if (showgui == false) {
		        		if(randomSeed){
		        			seed = System.currentTimeMillis() % 1000;
		        		}
		        		else{
		        			if(diffSeed){
		        				seed =r;
		        			}
		        		}
		        		
		        		if(!runSequential){
		        			exService.execute(new SimRunner(seed, scenDirName + dirName, "run" + r + ".csv", simS, false, -1, 50, -1, duration, 0.5, false, allStatistics));
		        		}
		        		else{
		        			dir.mkdirs();
		        			
			                SimCore sim = new SimCore(seed, scenDirName + dirName + "//run" + r + ".csv", simS, false, -1, 50, -1, 0.5, false, allStatistics);//output_file, ss, false);

			                for (int i = 0; i < duration; i++) {
			                    try {
									sim.update();
								} catch (Exception e) {
									e.printStackTrace();
								}
			                }
			                sim.close_files();
		        		}
		            } 
	        	}
			}
        }
	}
	
	private static void runSimulationForAll(int runs, int duration, File f, String scenName) {
		SimSettings ss = new SimSettings("", "", 1);
		ss.loadFromXML(f.getAbsolutePath());
		LinkedList<ArrayList<CameraSettings>> items = new LinkedList<ArrayList<CameraSettings>>();
		
//        ArrayList<CameraSettings> item = ss.cameras; 
		
        States[] input = {States.ABC, States.ASM, States.AST, States.PBC, States.PSM, States.PST};
        String scenDirName = totalDirName + "//"+ scenName + "//";
        doVariation(items, input, ss, 0, scenDirName);//item, 0, scenDirName);
    }
	
	private static void runRandomStatic(int runs, int duration, File f, String scenDirName){
	    SimSettings simS = new SimSettings("", "", 1);
        simS.loadFromXML(f.getAbsolutePath());
        LinkedList<ArrayList<CameraSettings>> items = new LinkedList<ArrayList<CameraSettings>>();
        long seed = initialSeed;
	
        File dir = new File(directory.getPath());
        
        String dirName = "";
	    
	    States[] input = {States.ABC, States.ASM, States.AST, States.PBC, States.PSM, States.PST};
	    	
	    do{
	        dirName = "";
            for (int i = 0; i < simS.cameras.size(); i++){ //item.size(); i++) {
                
                CameraSettings cs = simS.cameras.get(i).clone(); //item.get(count);
                switch (input[ran.nextInt(6)]) {
                case ABC: 
                    cs.ai_algorithm = "epics.ai.ActiveAINodeMulti";
                    cs.comm = 0;
                    break;
                case ASM:
                    cs.ai_algorithm = "epics.ai.ActiveAINodeMulti";
                    cs.comm = 1;
                    break;
                case AST:
                    cs.ai_algorithm = "epics.ai.ActiveAINodeMulti";
                    cs.comm = 2;
                    break;
                case PBC:
                    cs.ai_algorithm = "epics.ai.PassiveAINodeMulti";
                    cs.comm = 0;
                    break;
                case PSM:
                    cs.ai_algorithm = "epics.ai.PassiveAINodeMulti";
                    cs.comm = 1;
                    break;
                case PST:
                    cs.ai_algorithm = "epics.ai.PassiveAINodeMulti";
                    cs.comm = 2;
                    break;
                default:
                    break;
                }
                simS.cameras.set(i, cs); // item.set(count, cs);
                
             
                if(simS.cameras.get(i).ai_algorithm.equals("epics.ai.ActiveAINodeMulti")){ // item.get(i).ai_algorithm.equals("epics.ai.ActiveAINodeMulti")){
                    dirName += "a";
                }
                else{
                    dirName += "p";
                }
                dirName += simS.cameras.get(i).comm; // item.get(i).comm;
            }
            
            dir = new File(totalDirName + "//" + scenDirName + "//" + dirName);
	    } while(!dir.mkdirs());
        
         //directory for currently used setting 
        if(!runSequential){
            exService.execute(new SimRunner(seed, scenDirName + dirName, "summary.csv", runs, simS, false, -1, 50, -1, duration, 0.5, false, diffSeed, allStatistics));
        }
        else{
            for(int r = 0; r < runs; r++){
                if (showgui == false) {
                    if(randomSeed){
                        seed = System.currentTimeMillis() % 1000;
                    }
                    else{
                        if(diffSeed){
                            seed =r;
                        }
                    }
                    
                    if(!runSequential){
                        exService.execute(new SimRunner(seed, scenDirName + dirName, "run" + r + ".csv", simS, false, -1, 50, -1, duration, 0.5, false, false));
                    }
                    else{
                    
//                  testing.put(scenDirName + dirName + "//run" + r + ".csv", simS);
                    
                        
                        SimCore sim = new SimCore(seed, totalDirName + "//" + scenDirName + "//" + dirName + "//run" + r + ".csv", simS, false, -1, 50, -1, 0.5, false, allStatistics);//output_file, ss, false);
                        //new SimCore(seed, run, ss, global, camError, camReset, trackError, alpha);
                        for (int i = 0; i < duration; i++) {
                            try {
                                sim.update();
    //                          System.out.println(i);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        sim.close_files();
                    }
                } 
            }
        }
	}
	
	
	private static void runHomogeneous(int runs, int duration, File f, String scenName){
		SimSettings ss = new SimSettings("", "", 1);
		ss.loadFromXML(f.getAbsolutePath());
		long seed = initialSeed;
		
		String scenDirName = totalDirName + "//"+ scenName + "//";
		String algo = "epics.ai.ActiveAINodeMulti";
		
		//for all cameras load the same configuration
		for(int i = 0; i < 2; i++){
			String dirname = "";
			if(i == 0){
				algo = "epics.ai.ActiveAINodeMulti";
			}
			else{
				algo = "epics.ai.PassiveAINodeMulti";
			}
			for(int j = 0; j < 3; j++){
				for(CameraSettings cs : ss.cameras){
					if(i == 0){
						dirname += "a"+j;
					}
					else{
						dirname += "p"+j;
					}
						
					cs.ai_algorithm = algo; 
					cs.comm = j;
				}
				
				for(int r = 0; r < runs; r++){
		        	if (showgui == false) {
		        		if(randomSeed){
		        			seed = System.currentTimeMillis() % 1000;
		        		}
		        		else{
		        			if(diffSeed){
		        				seed =r;
		        			}
		        		}
		        		
		        		if(!runSequential){
		        			exService.execute(new SimRunner(seed, scenDirName + dirname, "run" + r + ".csv", ss, false, -1, 50, -1, duration, 0.5, false, false));
		        		}
		        		else{
		        		
	//	        		testing.put(scenDirName + dirName + "//run" + r + ".csv", simS);
		        			directory = new File(scenDirName + dirname);
		        			directory.mkdirs();
		        			
			                SimCore sim = new SimCore(seed, scenDirName + dirname + "//run" + r + ".csv", ss, false, -1, 50, -1, 0.5, false, true);//output_file, ss, false);
			                //new SimCore(seed, run, ss, global, camError, camReset, trackError, alpha);
			                for (int k = 0; k < duration; k++) {
			                    try {
									sim.update();
		//							System.out.println(i);
								} catch (Exception e) {
									e.printStackTrace();
								}
			                }
			                sim.close_files();
		        		}
		            } 
	        	}
				
				dirname = "";
			}
		}
	}

	@Deprecated
	private static void runSimulationsByParameter(int runs, int duration, File f, String scenName) {
		String commN = "";
		String algoN = "";
		String vgN = "";
		String grcN = "";
		
		String dirName = "";
		String scenDirName = totalDirName + "//"+ scenName + "//";
		directory = new File(scenDirName);
		directory.mkdir(); //for scenario
		String outputfile = dirName;
		
		
		String[] arguments = new String[13];
		arguments[0] = "-o";
		arguments[1] = outputfile; //will be overwritten
		arguments[2] = "--no-gui";
		arguments[3] = ""; //global -- changes
		arguments[4] = "-a";
		arguments[5] = "epics.ai.ActiveAINodeMulti"; //algorithm -- changes
		arguments[6] = "-c";
		arguments[7] = "1"; //communication -- changes
		arguments[8] = "-t";
		arguments[9] = "" + duration;
		arguments[10] = "-v";
		arguments[11] = "0";
		arguments[12] = f.getAbsolutePath();
		
		for(int i = 0; i < runs; i++){ //runs
			
			for(int comm = 0; comm < 3; comm ++){
				
				for(int algo = 0; algo < 2; algo++){ //set algo max to 3 if you want asking algo (original marcin) as well
					
					for(int vg = 1; vg < 2; vg++){ //-1 would mean, that it is defined in the file if static or not...

//						if(!((vg > 0) && (comm == 3))){ // !! do not use static communication with dynamic graphs !!
							try{
								String argString = arguments[0];
								arguments[7] = "" + comm;
//								arguments[9] = "" + duration;
								arguments[11] = "" + vg; 
								//create outputfile-name
								
								dirName = scenDirName;
								
								switch(algo){
									case 0: algoN = "ACTIVE//"; arguments[5] = "epics.ai.ActiveAINodeMulti"; dirName += "a"; break;
									case 1: algoN = "PASSIVE//"; arguments[5] = "epics.ai.PassiveAINodeMulti"; dirName += "p";break;
									case 2: algoN = "ASKER//"; arguments[5]= "asker"; break;
								}
								
								switch (comm){
									case 0: commN = "BROADCAST//"; dirName += "0"; break;
									case 1: commN = "SMOOTH//"; dirName += "1"; break;
									case 2: commN =  "STEP//"; dirName += "2"; break;
									case 3: commN =  "STATIC//"; break;
								}
								
								
								
								switch(vg){ //-1 = as defined in "static" attribute in file, 0 = static VG as predefined in file, 1 = do not use VG if defined in file (dynamic though), 2 = dynamic but starting with VG as predefined in file 
									case -1: vgN = "FiledefVG//"; break;
									case 0: vgN = "SPredefVG//"; break;
									case 1: vgN = "DUndefVG//"; break;
									case 2: vgN = "DPredefVG//"; break;
								}
							
								directory = new File(dirName += "//");
								directory.mkdir();
								
//									switch(glob){
//										case 0: arguments[3] = "-g"; grcN = "GRC//"; break;
//										case 1: arguments[3] = ""; grcN = "NOGRC//"; break;
//									}
								
								if(algo == 2){
									if(comm == 0){
										dirName += commN;
										directory = new File(dirName);
										directory.mkdir(); //for communication
										
										dirName += algoN;
										directory = new File(dirName);
										directory.mkdir(); //for algo
										
										dirName += vgN;
										directory = new File(dirName);
										directory.mkdir(); //for VisionGraph
										
										dirName += grcN;
										directory = new File(dirName);
										directory.mkdir(); //for grc
										
										arguments[1] = dirName + "run" + i + ".csv";
										
										for(int argsindex = 1; argsindex < arguments.length; argsindex++){
											if(!arguments[argsindex].equals(""))
												argString = argString + " " + arguments[argsindex];
										}
										
										if(randomSeed){
											argString += "-s " + System.currentTimeMillis() % 1000;
						        		}
										
										epics.camwin.Main.main(argString.split(" "));
									
									}
								}
								else{
//									boolean skip = false;
////									if((comm == 3) && (vg != 0)){
////										skip = true;
////									}
//									if(!skip){
//										dirName += commN;
//										directory = new File(dirName);
//										directory.mkdir(); //for communication
//										
//										dirName += algoN;
//										directory = new File(dirName);
//										directory.mkdir(); //for algo
//										
//										dirName += vgN;
//										directory = new File(dirName);
//										directory.mkdir(); //for VisionGraph
//										
//										dirName += grcN;
//										directory = new File(dirName);
//										directory.mkdir(); //for grc
										
										arguments[1] = dirName + "run" + i + ".csv";
										
										for(int argsindex = 1; argsindex < arguments.length; argsindex++){
											if(!arguments[argsindex].equals(""))
												argString = argString + " " + arguments[argsindex];
										}
										
										if(randomSeed){
											argString += " -s " + System.currentTimeMillis() % 1000;
						        		}
										
										epics.camwin.Main.main(argString.split(" "));
//									}
								}
								
							} catch (Exception e) {
								e.printStackTrace();
							}
//						}
					}
				}
			}
		}
		
		System.out.println("################## DONE #####################");
		
		
	}

}
