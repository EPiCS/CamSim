package simsim.epics.simSim;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;

import epics.camsim.core.SimCore;
import epics.camsim.core.SimSettings;
import epics.camsim.core.SimSettings.CameraSettings;

public class SimSimReal {
    
    public static boolean allStatistics = false;
    public static boolean runOnlyHomogeneous = false;
	public static boolean runSequential = true;
	public static boolean runByParameter = false;
	private static boolean runAllErrorVersions = false;
	public static boolean runAllPossibleVersions = true;
	public static boolean runBandits = true;
	private static boolean diffSeed = true;
	
	static int duration = 7120; //14240; //how many timesteps
	static int runs = 30;
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
	static double banditRuns = 20.0d;
	
	static File directory;
	static String totalDirName;
	static String initTotalDirName;
	
	static int epsilonRuns = 2; // how many epsilon / temperature values are being tried for the bandits
	
	static ExecutorService exService;
	
	public static void main(String[] args){
		
				
		DateFormat df = new SimpleDateFormat("ddMMYYYY");
		
		
		initTotalDirName = "E://Results//" + df.format(new java.util.Date());;
		directory = new File(initTotalDirName);
		int count = 0;
		while(!directory.mkdirs()){
			count++;
			directory = new File(initTotalDirName + "_" + count);
		}
		if(count > 0){
		    initTotalDirName += "_" + count;
		}
		
		
		
		//get folder
		File dir = new File("E:\\Scenarios\\RealDataT1.4_AR");
		
		RunAllRealDataScen(dir);
	}

	private static void RunAllRealDataScen(File dir) {
			//for all folders in this folder
		for(File folder : dir.listFiles()){
			//get all files from folder			
			if(folder.isDirectory()){
				
				long seed = initialSeed;
				
				ArrayList<String> names = new ArrayList<String>();
				ArrayList<ArrayList<ArrayList<Double>>> confs = new ArrayList<ArrayList<ArrayList<Double>>>();
				ArrayList<ArrayList<ArrayList<Integer>>> vis = new ArrayList<ArrayList<ArrayList<Integer>>>();
				
				//foreach file, create camera
				for(File f : folder.listFiles()){
					names.add(f.getName());
					ArrayList<ArrayList<Double>> confsCam = new ArrayList<ArrayList<Double>>();
					ArrayList<ArrayList<Integer>> visCam = new ArrayList<ArrayList<Integer>>();
					
					ArrayList<Double> c = new ArrayList<Double>();
					ArrayList<Integer> v = new ArrayList<Integer>();
					//read file for data - add to simsettingsdata
					Scanner s;
					try {
						s = new Scanner(f);
						while(s.hasNextLine()){
							String[] all = s.nextLine().split(",");
							//create a list for each pair of columns
							if(confsCam.isEmpty()){
								for(int i = 0; i < all.length; i=i+2){
									confsCam.add(new ArrayList<Double>());
									visCam.add(new ArrayList<Integer>());
								}
							}
							
							for(int i = 0; i< all.length-1; i=i+2){
								confsCam.get(i/2).add(Double.parseDouble(all[i]));
								visCam.get(i/2).add(Integer.parseInt(all[i+1]));
							}
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
					confs.add(confsCam);
					vis.add(visCam);
				}
				
				SimSettings ss = new SimSettings();
				ss.loadRealFromLists(names, confs, vis);
				
				int si = confs.get(0).get(0).size();
				boolean res = true;
				for(int i = 1; i < confs.size(); i++){
				    if (si != confs.get(i).get(0).size()){
				        res = false;
				    }
				}
				if(res)
				    duration = si;
				
				totalDirName = initTotalDirName + "//" + duration;
				
				if(runAllErrorVersions ){
					runSimulationsForAllErrors(runs, duration, ss, folder.getName());
					System.out.println("############ ALL ERRORS COMPLETED! ############");		
				}
				
				if(runOnlyHomogeneous){
				    runHomogeneous(runs, duration, ss, folder.getName());
				}
				
				if(runBandits){
					runBanditSimulations(runs, duration, ss, folder.getName());
					System.out.println("############ ALL BANDITS COMPLETED! ############");		
				}
								
				if(runAllPossibleVersions){
					runSimulationForAll(runs, duration, ss, folder.getName());
					System.out.println("############ ALL POSSIBLE COMPLETED! ############");		
				}
//				if(runByParameter){
//					runSimulationsByParameter(runs, duration, f, scenName);
//				}
				
				//load Simsettings with List
				
				//camera now uses data as input (nothing else) --> run 'simulation'
				
				System.out.println("############ ALL SIMULATIONS COMPLETED! ############");				
			}
		}
	}

	private static void runSimulationForAll(int runs2, int duration2,
			SimSettings ss, String scenName) {
			
			LinkedList<ArrayList<CameraSettings>> items = new LinkedList<ArrayList<CameraSettings>>();
			
	        States[] input = {States.ABC, States.ASM, States.AST, States.PBC, States.PSM, States.PST};
	        String scenDirName = totalDirName + "//"+ scenName + "//";
	        doVariation(items, input, ss, 0, scenDirName);//item, 0, scenDirName);
	    
	}

	public static void doVariation(LinkedList<ArrayList<CameraSettings>> reps, States[] input, SimSettings ss, int count, String scenDirName){// ArrayList<CameraSettings> item, int count, String scenDirName){
		long seed = initialSeed;
		SimSettings simS = ss.copy();
        if (count < ss.cameras.size()){//item.size()){
            for (int i = 0; i < input.length; i++) {
            	CameraSettings cs = ss.cameras.get(count).clone(); //item.get(count);
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
            	ss.cameras.set(count, cs); 
            	
                doVariation(reps, input, ss, count+1, scenDirName); 
            }
        }else{
        	String dirName = "";
        	//ss.cameras = item;
        	for (int i = 0; i < simS.cameras.size(); i++){ 
				if(simS.cameras.get(i).ai_algorithm.equals("epics.ai.ActiveAINodeMulti")){
					dirName += "a";
				}
				else{
					dirName += "p";
				}
				dirName += simS.cameras.get(i).comm; // item.get(i).comm;
			}
        	
        	directory = new File(scenDirName + dirName);
        	//directory for currently used setting 
            if(!runSequential){
                exService.execute(new SimRunner(seed, scenDirName + dirName, "summary.csv", runs, simS, false, -1, 50, -1, duration, 0.5, false, diffSeed, allStatistics));
            }
            else{	
            	for(int r = 0; r < runs; r++){
    	        	
        			if(diffSeed){
        				seed =r;
        			}
        			
            		if(!runSequential){
            			exService.execute(new SimRunner(seed, scenDirName + dirName, "run" + r + ".csv", simS, false, -1, 50, -1, duration, 0.5, true, false));
            		}
            		else{
            			directory.mkdirs(); //directory for currently used setting 
    	                SimCore sim = new SimCore(seed, scenDirName + dirName + "//run" + r + ".csv", ss, false, -1, 50, -1, 0.5, true, false);
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
	
	private static void runBanditSimulations(int runs2, int duration2,
			SimSettings ss, String scenName) {
		long seed = initialSeed;
		String dirName = "";
		String scenDirName = totalDirName + "//"+ scenName + "//";
		for(int i = 0; i < ss.cameras.size(); i++){
			dirName += "ex";
		}
		
		BigDecimal alphaCoef = BigDecimal.valueOf(1.0d).divide(BigDecimal.valueOf( banditRuns));
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
                       
                        if(diffSeed){
                            seed = r;
                        }
                        
                        
                        SimCore sim = new SimCore(seed, scenDirName + dirName + "//SoftMax-"+ epsilon+"//" + alpha + "//run" + r + ".csv", ss, false, epsilon, alpha, true, true);
                                
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
        
        
//    //Set for all cameras UCB2 bandit solving mechanism
//    for (CameraSettings cs : ss.cameras) {
//        cs.bandit = "epics.bandits.UCB2";
//    }
//    if(epsCoef.doubleValue() > 0){
//        for(int i = 0; i < banditRuns; i++){
//            double alpha = alphaCoef.multiply(BigDecimal.valueOf(i + 1.0)).doubleValue();
//  //          double alpha = 0.5;
//            directory = new File(scenDirName + dirName + "//UCB2//" + alpha + "//");
//            directory.mkdirs();
//            //run all scenarios for a certain amount
//            for(int r = 0; r < runs; r++){
//                if (showgui == false) {
//                    
//                    if(randomSeed){
//                        seed = System.currentTimeMillis() % 1000;
//                    }
//                    else{
//                        if(diffSeed){
//                            seed = r;
//                        }
//                    }
//                    
//                    SimCore sim = new SimCore(seed, scenDirName + dirName + "//UCB2//" + alpha + "//run" + r + ".csv", ss, false, 0.01, alpha, false, true);
//                            
//                    for (int k = 0; k < duration; k++) {
//                        try {
//                            sim.update();
//                        } catch (Exception x) {
//                            x.printStackTrace();
//                        }
//                    }
//                    sim.close_files();
//                } 
//            }
//        }
//    }
//            
//    
//    
//    //Set for all cameras UCBTuned bandit solving mechanism
//    for (CameraSettings cs : ss.cameras) {
//        cs.bandit = "epics.bandits.UCBTuned";
//    }
//    if(epsCoef.doubleValue() > 0){
//        for(int e = 0; e < epsilonRuns; e++){
//            double epsilon = BigDecimal.valueOf(e+1).multiply(epsCoef).doubleValue();
//            for(int i = 0; i < banditRuns; i++){
//                double alpha = alphaCoef.multiply(BigDecimal.valueOf(i + 1.0)).doubleValue();
//  //              double alpha = 0.5;
//                directory = new File(scenDirName + dirName + "//UCBTuned-" + epsilon + "//" + alpha + "//");
//                directory.mkdirs();
//                //run all scenarios for a certain amount
//                for(int r = 0; r < runs; r++){
//                    if (showgui == false) {
//                        if(randomSeed){
//                            seed = System.currentTimeMillis() % 1000;
//                        }
//                        else{
//                            if(diffSeed){
//                                seed = r;
//                            }
//                        }
//                    
//                        SimCore sim = new SimCore(seed, scenDirName + dirName + "//UCBTuned-" + epsilon + "//" + alpha + "//run" + r + ".csv", ss, false, 0.01, alpha, false, true);
//                                
//                        for (int k = 0; k < duration; k++) {
//                            try {
//                                sim.update();
//                            } catch (Exception x) {
//                                x.printStackTrace();
//                            }
//                        }
//                        sim.close_files();
//                    }
//                } 
//            }
//        }
//    }
//    
//    
//    
//    
//    
//    
//    Set for all cameras epsilon greedy bandit solving mechanism
        for (CameraSettings cs : ss.cameras) {
            cs.bandit = "epics.bandits.EpsilonGreedy";
        }
        System.out.println("EPSILONGREEDY");
//    if(epsCoef.doubleValue() > 0){
//        for(int e = 0; e < epsilonRuns; e++){
//            double epsilon = BigDecimal.valueOf(e+1).multiply(epsCoef).doubleValue(); 
//            double alpha = 0.5;
//            directory = new File(scenDirName + dirName + "//epsilonGreedy-" +epsilon + "//" + alpha + "//");
//            directory.mkdirs();
//            //run all scenarios for a certain amount
//            for(int r = 0; r < runs; r++){
//                if (showgui == false) {
//                    
//                    if(randomSeed){
//                        seed = System.currentTimeMillis() % 1000;
//                    }
//                    else{
//                        if(diffSeed){
//                            seed = r;
//                        }
//                    }
//                    
//                    SimCore sim = new SimCore(seed, scenDirName + dirName + "//epsilonGreedy-"+ epsilon+"//" + alpha + "//run" + r + ".csv", ss, false, epsilon, alpha, false, true);
//                            
//                    for (int k = 0; k < duration; k++) {
//                        try {
//                            sim.update();
//                        } catch (Exception x) {
//                            x.printStackTrace();
//                        }
//                    }
//                    sim.close_files();
//                } 
//            }
//        }
//    }
        for(int i = 0; i <= banditRuns; i++){
            double alpha = alphaCoef.multiply(BigDecimal.valueOf(i)).doubleValue();
            directory = new File(scenDirName + dirName + "//epsilonGreedy//" + alpha + "//");
            directory.mkdirs();
            //run all scenarios for a certain amount
            for(int r = 0; r < runs; r++){
                
                if(diffSeed){
                    seed = r;
                }
                
                                        
                SimCore sim = new SimCore(seed, scenDirName + dirName + "//epsilonGreedy//" + alpha + "//run" + r + ".csv", ss, false, 0.01, alpha, true, true);//output_file, ss, false);
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
        
        
//    for (CameraSettings cs : ss.cameras) {
//        cs.bandit = "epics.bandits.FullRandom";
//    }
//    for(int i = 0; i < banditRuns; i++){
//        //double alpha = alphaCoef.multiply(BigDecimal.valueOf(i + 1.0)).doubleValue();
//        directory = new File(scenDirName + dirName + "//fullRandom//" + i + "//");
//        directory.mkdirs();
//        //run all scenarios for a certain amount
//        for(int r = 0; r < runs; r++){
//            if (showgui == false) {
//                
//                if(randomSeed){
//                    seed = System.currentTimeMillis() % 1000;
//                }
//                else{
//                    if(diffSeed){
//                        seed = r;
//                    }
//                }
//                    SimCore sim = new SimCore(seed, scenDirName + dirName + "//fullRandom//"+ i + "//run" + r + ".csv", ss, false, -1, 50, -1, i, false, true);//output_file, ss, false);
//                    for (int k = 0; k < duration; k++) {
//                        try {
//                            sim.update();
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    sim.close_files();
//            } 
//        }
//    }
//    
//    
        
        
//    //Set for all cameras epsilon greedy bandit solving mechanism
//    for (CameraSettings cs : ss.cameras) {
//        cs.bandit = "epics.bandits.UCB1e";
//    }
//    if(epsCoef.doubleValue() > 0){
//        for(int e = 0; e < epsilonRuns; e++){
//            double epsilon = BigDecimal.valueOf(e+1).multiply(epsCoef).doubleValue(); 
////                for(int i = 0; i < banditRuns; i++){
//                double alpha = 0.5; //alphaCoef.multiply(BigDecimal.valueOf(i + 1.0)).doubleValue();
//                directory = new File(scenDirName + dirName + "//UCB1e-" +epsilon + "//");
//                directory.mkdirs();
//                //run all scenarios for a certain amount
//                for(int r = 0; r < runs; r++){
//                    if (showgui == false) {
//                        
//                        if(randomSeed){
//                            seed = System.currentTimeMillis() % 1000;
//                        }
//                        else{
//                            if(diffSeed){
//                                seed = r;
//                            }
//                        }
//                        
//                        SimCore sim = new SimCore(seed, scenDirName + dirName + "//UCB1e-"+ epsilon+"//" + alpha +"//run" + r + ".csv", ss, false, epsilon, alpha, false, true);
//                                
//                        for (int k = 0; k < duration; k++) {
//                            try {
//                                sim.update();
//                            } catch (Exception x) {
//                                x.printStackTrace();
//                            }
//                        }
//                        sim.close_files();
//                    } 
//                }
////                }
//        }
//    }
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
                
                if(diffSeed){
                    seed = r;
                }
                    
                if(!runSequential){
                    exService.execute(new SimRunner(seed, scenDirName + dirName + "//ucb1//" + alpha, "run" + r + ".csv", ss, false, -1, 50, -1, duration, alpha, false, true));
                }
                else{
                    directory.mkdirs();
                    SimCore sim = new SimCore(seed, scenDirName + dirName + "//ucb1//" + alpha + "//run" + r + ".csv", ss, false, -1, 50, -1, alpha, true, true);//output_file, ss, false);
                    for (int k = 0; k < duration; k++) {
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

	private static void runHomogeneous(int runs, int duration, SimSettings ss, String scenName){
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
                    
                    if(diffSeed){
                        seed =r;
                    }
                    
                    
                    if(!runSequential){
                        exService.execute(new SimRunner(seed, scenDirName + dirname, "run" + r + ".csv", ss, false, -1, 50, -1, duration, 0.5, false, false));
                    }
                    else{
                    
//                  testing.put(scenDirName + dirName + "//run" + r + ".csv", simS);
                        directory = new File(scenDirName + dirname);
                        directory.mkdirs();
                        
                        //SimCore sim = new SimCore(seed, scenDirName + dirname + "//run" + r + ".csv", ss, false, -1, 50, -1, 0.5, false, true);//output_file, ss, false);
                        SimCore sim = new SimCore(seed, scenDirName + dirname + "//run" + r + ".csv", ss, false, -1, 50, -1, 0.5, true, false);
                        //new SimCore(seed, run, ss, global, camError, camReset, trackError, alpha);
                        for (int k = 0; k < duration; k++) {
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
                
                
                dirname = "";
            }
        }
    }
	
	private static void runSimulationsForAllErrors(int runs2, int duration2,
			SimSettings ss, String scenName) {
		String dirName = "";
    	String scenDirName = totalDirName + "//"+ scenName + "//";
    	directory = new File(scenDirName);
		directory.mkdir(); //directory for currently used setting 
		
		long seed = initialSeed;
		
		for(int ce = startCamError; ce >= endCamError; ce=ce-camRate){
			for(int re = startReset; re <= endReset; re = re + resetRate){
				for(int te = startTrackError; te >= endTrackError; te = te - trackRate){

					dirName += ce + "_" + re + "_" + te + "//";
					directory = new File(scenDirName + dirName);
					directory.mkdir(); //directory for currently used setting 
					
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
		        			if(diffSeed){
		        				seed = r;
		        			}
			        		
			        		if(!runSequential){
			        			exService.execute(new SimRunner(seed, scenDirName + dirName, "run" + r + ".csv", ss, false, ce, re, te, duration, 0.5, true, true));
			        		}
			        		else{
			        			directory.mkdirs();
				                SimCore sim = new SimCore(seed, scenDirName + dirName + "//run" + r + ".csv", ss, false, ce, re, te, 0.5, true, true);//output_file, ss, false);
				
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
						dirName = prevDir;
					}
					dirName = "";
				}
			}
		}
	}
	
	
}
