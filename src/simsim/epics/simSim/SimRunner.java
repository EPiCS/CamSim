package simsim.epics.simSim;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import epics.camsim.core.SimCore;
import epics.camsim.core.SimSettings;

public class SimRunner implements Runnable {

	int duration;
	private long seed;
	private String run;
	private SimSettings ss;
	private boolean global;
	private int camError;
	private int camReset;
	private int trackError;
	private double alpha;
	private boolean realData;
	private String dir;
	private boolean allStatistics;
	private int runs = 0;
	private boolean diffSeed;
	
	private String summary = "";
	private String outputFile = "";
		
	
	public SimRunner(long seed, String dir, String file, SimSettings ss, boolean global, int camError, int camReset, int trackError, int dur, double alpha, boolean real, boolean allStatistics){
		this.seed = new Long(seed);
		this.run = new String(dir + "//" + file);
		this.dir = dir;
		this.ss = ss;//.copy();
		this.global = new Boolean(global);
		this.camError = camError;
		this.camReset = camReset;
		this.trackError = trackError;
		this.alpha = alpha;
		this.realData = real;
		this.allStatistics = allStatistics;
		this.outputFile = dir + "//" + file;
		
		duration = dur;
	}
	
	public SimRunner(long seed, String dir, String file, int runs, SimSettings ss, boolean global, int camError, int camReset, int trackError, int dur, double alpha, boolean real, boolean diffSeed, boolean allStats){
		this.seed = new Long(seed);
		this.run = new String(dir + "//" + file);
		this.runs = runs;
		this.dir = dir;
		this.ss = ss;//.copy();
		this.global = new Boolean(global);
		this.camError = camError;
		this.camReset = camReset;
		this.trackError = trackError;
		this.alpha = alpha;
		this.realData = real;
		this.allStatistics = allStats;
		this.diffSeed = diffSeed;
		this.outputFile = dir + "//" + file;
		
		duration = dur;
	}
	@Override
	public void run() {
		
		File f = new File(dir);
		f.mkdirs();
		
		if(runs == 0){
			ss = ss.copy();
			SimCore sim = new SimCore(seed, run, ss, global, camError, camReset, trackError, alpha, realData, allStatistics);//output_file, ss, false);
			
			for (int i = 0; i < duration; i++) {
	            try {
					sim.update();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }

	        sim.close_files();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
		}
		else {
			for(int r = 0; r < runs; r++){
//	        	System.out.println(dir + " - " + r);
    			if(diffSeed){
    				seed =r;
    			}
    			
                SimCore sim = new SimCore(seed, dir + "//run" + r + ".csv", ss, false, -1, 50, -1, 0.5, realData, true);
                for (int i = 0; i < duration; i++) {
                    try {
						sim.update();
					} catch (Exception e) {
						e.printStackTrace();
					}
                }
                
                if(summary.isEmpty()){
                    try {
                        summary = sim.getStatSumDesc(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                
                try {
                    summary = summary + "\r\n" + sim.getStatSummary(false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
//                sim.close_files();

            }
			
			
			String summaryF = "summary.csv";
			if(outputFile.contains("/")){
	            summaryF = outputFile.substring(0, outputFile.lastIndexOf('/')) + "summary.csv";// summaryFile.substring(0, summaryFile.lastIndexOf('/')) + "summary.csv"; //summaryFile;
	        }
			
			
			PrintWriter sumOut = null;
			try {
    			File sumFile = new File(summaryF);
//                File f1 = new File(sumFile.getParent());
//                boolean create = f1.mkdirs();
                //boolean existed = sumFile.exists();
                FileWriter sumFileWriter;
                
                sumFileWriter = new FileWriter(sumFile, true);
            
                sumOut = new PrintWriter(sumFileWriter);
    
                sumOut.println(summary);
			} catch (IOException e) {
                e.printStackTrace();
			}
			finally{
			    if(sumOut != null){
			        sumOut.flush();
			        sumOut.close();
			    }
			}
		}
		
	}

}
