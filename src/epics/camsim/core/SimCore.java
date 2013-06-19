package epics.camsim.core;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import epics.camsim.core.SimSettings.TrObjectWithWaypoints;
import epics.common.AbstractAINode;
import epics.common.CmdLogger;
import epics.common.IBanditSolver;
import epics.common.IMessage;
import epics.common.IMessage.MessageType;
import epics.common.IRegistration;
import epics.common.RandomNumberGenerator;
import epics.common.RandomUse;
import epics.common.RunParams;

/**
 * SimCore represents the main core of the simulation. each object and camera is controlled from here. 
 * the SimCore drives the simulation in discrete time steps. 
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public class SimCore {

	int CAMERRORRATE = -1; //percent of camera error. -1 = no camera error
	int RESETRATE = 50; //looses knowledge about everything - happens in x percentage of cameraerror (only when error occurs, knowledgeloss can happen)
	static final boolean BIDIRECTIONAL_VISION = true;
	boolean USEGLOBAL = false;
	String EPSILONGREEDY = "epics.learning.EpsilonGreedy";
	private double epsilon = 0.1;
	double alpha = 0.5;
	private int selectInterval = 0; //if < 1, a new strategy is selected every timestep
	private int currentSelectInt = 0;
	
	Statistics stats;
	RandomNumberGenerator randomGen;
	
	int interval = 1;
	
    static int camIDGenerator = 0;
    public static int getNextID(){ return camIDGenerator++; }

    /*
     * Simulation area
     */
    private double min_x;     
    /**
     * returns the minimum x value for the simulation environment
     * @return minimum x of simulation environment
     */
    public double get_min_x(){return min_x;}
    private double max_x;     
    /**
     * returns the maximum x value for the simulation environment
     * @return maximum x of simulation environment
     */
    public double get_max_x(){return max_x;}
    private double min_y;     
    /**
     * returns the minimum y value for the simulation environment
     * @return minimum y of simulation environment
     */
    public double get_min_y(){return min_y;}
    private double max_y;     
    /**
     * returns the maximum y value for the simulation environment
     * @return maximum y of simulation environment
     */
    public double get_max_y(){return max_y;}
    private long sim_time;    
    /**
     * returns the maximum simulation time
     * @return maximum simulation time
     */
    public long get_sim_time(){return sim_time;}
    private String ai_alg; 	  public boolean staticVG = false;
   
    private boolean firstUpdate;
    private ArrayList<SimSettings.Event> events;
    private boolean _runReal;
    private int step;
    
    private IRegistration reg = null;
    private SimSettings settings;
    private String paramFile;


    /**
     * checks if the given coordinates are in range
     * throws a IllegalArgumentException if they are not
     * @param x
     * @param y
     */
    private void checkCoordInRange( double x, double y ){
        if ( x < min_x || x > max_x || y < min_y || y > max_y ){
            throw new IllegalArgumentException("x/y value out of simulation field.");
        }
    }

    private ArrayList<CameraController> cameras = new ArrayList<CameraController>();
    private ArrayList<TraceableObject> objects = new ArrayList<TraceableObject>();
	private int _comm = -1;
	private String outputFile;
    

	/**
	 * Constructor pure simulation
	 * @param seed for the random number generators
	 * @param output outputfilename for statistics
	 * @param ss settings of simulations - generated from an scenariofile
	 * @param global global coordination used
	 * @param camError the probability of failing cameras
	 * @param camReset probability of a reset after a camera failed
	 */
	public SimCore( long seed, String output, SimSettings ss, boolean global){
		initSimCore(seed, output, global, -1, 50, alpha, false, false, "", null);
		this.interpretFile(ss);
	}
	

	/**
	 * Constructor  pure simulation
	 * @param seed for the random number generators
     * @param output outputfilename for statistics
     * @param ss settings of simulations - generated from an scenariofile
     * @param global global coordination used
	 * @param banditParam the epsilon/temperature value for bandit solvers
	 * @param alpha the alpha value for the weighted reward function used in bandit solvers
	 */
	public SimCore( long seed, String output, SimSettings ss, boolean global, double banditParam, double alpha){
		initSimCore(seed, output, global, -1, 50, alpha, false, false, "", null);
		this.interpretFile(ss);
	}

	/**
	 * Constructor
     * @param seed for the random number generators
     * @param output outputfilename for statistics
     * @param ss settings of simulations - generated from an scenariofile
     * @param global global coordination used
     * @param banditParam the epsilon/temperature value for bandit solvers
     * @param alpha the alpha value for the weighted reward function used in bandit solvers
	 * @param realData indicates if real data has been used
	 * @param allStatistics indicates if statistics are also taken for each camera seperately
	 */
	public SimCore( long seed, String output, SimSettings ss, boolean global, double epsilon, double alpha, boolean realData, boolean allStatistics){
		initSimCore(seed, output, global, -1, 50, alpha, realData, allStatistics, "", null);
		this.epsilon = epsilon;
		this.interpretFile(ss);
	}
	
	/**
     * Constructor pure simulation
     * @param seed for the random number generators
     * @param output outputfilename for statistics 
     * @param ss settings of simulations - generated from an scenariofile
     * @param global global coordination used
     * @param camError the probability of failing cameras
     * @param camReset probability of a reset after a camera failed
     * @param alpha the alpha value for the weighted reward function used in bandit solvers
     * @param realData indicates if real data has been used
     * @param allStatistics indicates if statistics are also taken for each camera seperately
	 */
	public SimCore( long seed, String output, SimSettings ss, 
			boolean global, int camError, int camReset, double alpha, boolean realData, boolean allStatistics) {
	    initSimCore(seed, output, global, camError, camReset,
				alpha, realData, allStatistics, "", null);
		this.interpretFile(ss);
	}
	
    /**
     * Constructor pure simulation
     * @param seed for the random number generators
     * @param output outputfilename for statistics
	 * @param summaryFile File for a summarised statistics file
	 * @param paramFile parameterfile for simulations
	 * @param ss settings of simulations - generated from an scenariofile
     * @param global global coordination used
     * @param camError the probability of failing cameras
     * @param camReset probability of a reset after a camera failed
	 * @param realData indicates if real data has been used
     * @param allStatistics indicates if statistics are also taken for each camera seperately
     */
	public SimCore(long seed, String output, String summaryFile, String paramFile, SimSettings ss, 
    		boolean global, int camError, int camReset, boolean realData, boolean allStatistics) {
    	initSimCore(seed, output, global, camError, camReset,
				alpha, realData, allStatistics, summaryFile, paramFile);
    	this.interpretFile(ss);
    }

	/**
	 * Initiation method for the simcore. Sets all the parameters
     * @param seed for the random number generators
     * @param output outputfilename for statistics
	 * @param global global coordination used
     * @param camError the probability of failing cameras
     * @param camReset probability of a reset after a camera failed
	 * @param alpha the alpha value for the weighted reward function used in bandit solvers
     * @param realData indicates if real data has been used
     * @param allStatistics indicates if statistics are also taken for each camera seperately
	 * @param summaryFile File for a summarised statistics file
	 * @param paramFile parameterfile for simulations
	 */
	private void initSimCore(long seed, String output, boolean global,
			int camError, int camReset, double alpha,
			boolean realData, boolean allStatistics, String summary, String paramFile) {
		this.RESETRATE = camReset;
	    this.CAMERRORRATE = camError;
	    this.alpha = alpha;
		
		USEGLOBAL = global;
		
		_runReal = realData;
		step = 0;
		firstUpdate = true;
		
	    if(USEGLOBAL){
	    	reg = new GlobalRegistration();
	    }
	    
	    randomGen = new RandomNumberGenerator(seed);
		outputFile = output;
		if(summary == ""){
			stats = new Statistics(output, "E://Results//sum_result/" + output.substring(output.indexOf('/')+1), allStatistics, randomGen.getSeed());
		}
		else{
			stats = new Statistics(output, summary, allStatistics, randomGen.getSeed());
		}
	    
	}
	
    /**
     * Interprets the SimSettings object and creates cameras and trackable objects with their corresponding behaviour
     * @param ss stores the settings for this simulation
     */
    public void interpretFile(SimSettings ss){
    	settings = ss;
    	
    	if(ss.min_x != null){
    		this.min_x = ss.min_x;
    	}
    	else{
    		this.min_x = -30;
    	}
    	if(ss.max_x != null){
    		this.max_x = ss.max_x;
    	}
    	else{
    		this.max_x = 30;
    	}
    	if(ss.min_y != null){
    		this.min_y = ss.min_y;
    	}
    	else{
    		this.min_y = -30;
    	}
    	if(ss.max_y != null){
    		this.max_y = ss.max_y;
    	}
    	else{
    		this.max_y = 30;
    	}
    	
    	if(ss.visionGraph != null){
    		staticVG = ss.visionGraph.isStatic;
    	}
    	
    		
    	for (SimSettings.CameraSettings cs : ss.cameras){
    		Map<String, Double> vg = null;
    		
    		if(ss.visionGraph != null){
    			vg = new HashMap<String, Double>();
	    		if(ss.visionGraph.vg.containsKey(cs.name)){
	    			ArrayList<String> neighs = ss.visionGraph.vg.get(cs.name);
	    			for(String s : neighs){
	    				vg.put(s, 1.0);
	    			}
	    		}
	    		
	    		if(staticVG){
	    			for(Map.Entry<String, ArrayList<String>> all : ss.visionGraph.vg.entrySet()){
	    				if(all.getValue().contains(cs.name))
	    					vg.put(all.getKey(), 1.0);
	    			}
	    		}
	    		
    		}
    		
            this.add_camera(
                    cs.name, cs.x, cs.y,
                    cs.heading, cs.viewing_angle,
                    cs.range, cs.ai_algorithm, cs.comm, cs.limit, vg, cs.bandit, cs.predefConfidences, cs.predefVisibility);
        }
    	

        for (SimSettings.TrObjectSettings tro : ss.objects){
            this.add_object(tro.x, tro.y, tro.heading, tro.speed, tro.features);
        }
        
        for (TrObjectWithWaypoints objWithWP : ss.objectsWithWaypoints) {
        	this.add_object(objWithWP.speed, objWithWP.waypoints, objWithWP.features);
        }
        
        events = ss.events;
    }

    /**
     * writes statistics and closes all statistics files  
     */
    public void close_files(){
        try {
			stats.close();
			//printAllBanditResults();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    /**
     * allows to print all information from all bandit solvers into files
     */
    private void printAllBanditResults(){
    	for(CameraController cc : this.cameras){
			IBanditSolver bs = cc.getAINode().getBanditSolver();
			if(bs != null){
				ArrayList<ArrayList<Double>> results = bs.getResults();
				printResults(results, outputFile + "_" + cc.getName() + ".csv");
			}
			
		}
    }
    
    /**
     * actually prints arrayLists of ArrayLists into specific files
     * @param res results to be stored in file
     * @param filename the filename to store results to
     */
    private void printResults(ArrayList<ArrayList<Double>> res, String filename){
		File f = new File(filename);
		PrintWriter out;
		try {
			FileWriter fw = new FileWriter(f);
			out = new PrintWriter(fw);
			int size = res.get(0).size();
			for(int j = 0; j < size; j++){
				for(int i = 0; i < res.size()-1; i++){
					out.print(res.get(i).get(j) + ";");
				}
				out.println(res.get(res.size()-1).get(j));
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

    /**
     * crates a new camera and adds it to the list of cameras WITH an AINODE as parameter
     * @param name defines the name of the camera
     * @param x_pos defines the x position in the internal coordinates
     * @param y_pos defines the y position in the internal coordinates
     * @param heading_degrees defines the direction of the viewing point
     * @param angle_degrees defines the with of the viewing angle
     * @param range defines the range of the camera
     * @param ai_algorithm defines the initial algorithm approach used
     * @param comm defines the initial/predefined communication strategy
     * @param limit sets limit for amount of objects being tracked (0 = unlimited)
     * @param vg contains the predefined vision graph
     * @param bandit defines the used bandit solver algorithm 
     * @param predefConfidences defines a list of objects represented by an ArrayList of their confidences where each element is for one frame/timestep 
	 * @param predefVisibility defines a list of objects represented by an ArrayList of their visibility (0 = visible, 1 = not visible or at touching border) where each element is for one frame/timestep
	 */
    public void add_camera(
            String name,
            double x_pos, double y_pos,
            double heading_degrees,
            double angle_degrees,
            double range,
            String ai_algorithm,
            int comm, int limit, Map<String, Double> vg, String bandit, ArrayList<ArrayList<Double>> predefConfidences, ArrayList<ArrayList<Integer>> predefVisibility){

        ai_alg = ai_algorithm;
        add_camera(
        	name, x_pos, y_pos, heading_degrees, angle_degrees,
            range, comm, limit, vg, bandit, predefConfidences, predefVisibility);
    }

    /**
     * crates a new camera and adds it to the list of cameras all having the same predefined aiNode
     * @param name defines the name of the camera
     * @param x_pos defines the x position in the internal coordinates
     * @param y_pos defines the y position in the internal coordinates
     * @param heading_degrees defines the direction of the viewing point
     * @param angle_degrees defines the with of the viewing angle
     * @param range defines the range of the camera
     * @param comm defines the initial/predefined communication strategy
     * @param limit sets limit for amount of objects being tracked (0 = unlimited)
     * @param vg contains the predefined vision graph
     * @param bandit defines the used bandit solver algorithm 
     * @param predefConfidences defines a list of objects represented by an ArrayList of their confidences where each element is for one frame/timestep 
     * @param predefVisibility defines a list of objects represented by an ArrayList of their visibility (0 = visible, 1 = not visible or at touching border) where each element is for one frame/timestep
     */
    public void add_camera(String name,
            double x_pos, double y_pos,
            double heading_degrees, 
            double angle_degrees, 
            double range, 
            int comm, int limit, Map<String, Double> vg, String bandit, ArrayList<ArrayList<Double>> predefConfidences, ArrayList<ArrayList<Integer>> predefVisibility){

    	if(_comm == -1){
    		_comm = comm;
    	}
    	
        checkCoordInRange(x_pos, y_pos);
        
        AbstractAINode aiNode = null;
    	try {
    		aiNode = newAINodeFromName(ai_alg, comm, staticVG, vg, reg, bandit);
    	} catch (Exception e) {
    		System.out.println("Couldn't initialise AI Node from name given in scenario file: "+ai_alg);
    		System.out.println("Is it a fully qualified class name? e.g. 'epics.ai.ActiveAINodeMulti'");
    		e.printStackTrace();
    		System.exit(1);
    	}
    	
        CameraController cc = new CameraController(
        		name,
                x_pos, y_pos,
                Math.toRadians(heading_degrees),
                Math.toRadians(angle_degrees),
                range, aiNode, limit, stats, randomGen, predefConfidences, predefVisibility);

    	try {
    		if (paramFile != null) {
    			this.applyParamsToAINode(aiNode, paramFile);
    		}
    	} catch (IOException e) {
    		System.out.println("Couldn't read ParamFile: " + paramFile);
    		e.printStackTrace();
    		System.exit(1);
    	}
        
        if(USEGLOBAL){
        	reg.addCamera(cc);
    	}
        
        this.getCameras().add(cc);
        for (CameraController c1 : this.cameras){
            c1.addCamera(cc);
            cc.addCamera(c1);
        }
	}
    
    /** Given a node's class name, dynamically loads the class and 
     * instantiates a new node of that type. 
     * @param fullyQualifiedClassName the class name - has to include package name if not in the same package. eg.: epics.ai.ActiveAINodeMulti
     * @param comm the communication policy: 0 = Broadcast, 1 = Smooth, 2 = step
     * @param staticVG defines if VG is static as predefined or can change dynamically
     * @param vg a predefined VG - may or may not change over time
     * @param r a global registration
     * @param banditS the class name of a bandit solver
     * @return AbstractAINode the created AINode
     * @throws ClassNotFoundException if the class for the AINode or the BanditSolver wasn't found
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public AbstractAINode newAINodeFromName(String fullyQualifiedClassName, 
    		int comm, boolean staticVG, Map<String, Double> vg, IRegistration r, String banditS) 
    				throws ClassNotFoundException, SecurityException, NoSuchMethodException, 
    				IllegalArgumentException, InstantiationException, IllegalAccessException, 
    				InvocationTargetException {
    	IBanditSolver bs = null;
    	try {
    		if(banditS != null){
	    		if(!banditS.equals("")){
					Class<?> banditType = Class.forName(banditS);
					Class<?>[] banditConstructorTypes = {int.class, double.class, double.class, int.class, RandomNumberGenerator.class};
					Constructor<?> banditCons = banditType.getConstructor(banditConstructorTypes);
					bs =  (IBanditSolver) banditCons.newInstance(6, epsilon, alpha, interval, randomGen);
	    		}	    		
    		}
		} catch (ClassNotFoundException e) {
			if(!banditS.equals(""))
			System.out.println("AAAHHH " + banditS + " not found...");
		}
    	
    	Class<?> nodeType = Class.forName(fullyQualifiedClassName);
    	Class<?>[] constructorTypes = {int.class, boolean.class, Map.class, IRegistration.class, RandomNumberGenerator.class, IBanditSolver.class};
    	Constructor<?> cons = nodeType.getConstructor(constructorTypes);
    	AbstractAINode node = (AbstractAINode) cons.newInstance(comm, staticVG, vg, r, randomGen, bs);
    	return node;
    }
    
    /**
     * @param fullyQualifiedClassName
     * @param comm
     * @param ai
     * @return
     * @throws ClassNotFoundException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public AbstractAINode newAINodeFromName(String fullyQualifiedClassName, 
    		int comm, AbstractAINode ai)
    				throws ClassNotFoundException, SecurityException, NoSuchMethodException, 
    				IllegalArgumentException, InstantiationException, IllegalAccessException, 
    				InvocationTargetException {
    	Class<?> nodeType = Class.forName(fullyQualifiedClassName);
    	Class<?>[] constructorTypes = {AbstractAINode.class};
    	Constructor<?>[] allCons = nodeType.getDeclaredConstructors();
    	
    	Constructor<?> cons = nodeType.getConstructor(constructorTypes);
    	AbstractAINode node = (AbstractAINode) cons.newInstance(ai);
    	node.setComm(comm);
    	return node;
    }

    /** Given a file which contains parameters for our run, we run through 
     * the params and apply each one to the AI node. This is mainly to aid
     * running of experiments, where the necessary parameters can be applied
     * to the node for a particular run, then the params file is changed for 
     * the next run */
    /**
     * @param node
     * @param paramsFilepath
     * @throws IOException
     */
    public void applyParamsToAINode(AbstractAINode node, String paramsFilepath) throws IOException {
    	RunParams.loadIfNotLoaded(paramsFilepath);
    	System.out.println("Setting params for " + node.getName() + "...");
    	Set<Entry<Object,Object>> props = RunParams.getAllProperties();
    	for (Entry<Object, Object> prop : props) {
    		String key = (String) prop.getKey();
    		String value = (String) prop.getValue();
    		if (! node.setParam(key, value)) {
    			throw new IllegalStateException("Param "+key+" could not be applied");
    		}
    	}
    }
    
  	/**
  	 * 
  	 */
  	public void add_random_camera(){
        this.add_camera(
        		"C"+getNextID(),
                randomGen.nextDouble(RandomUse.USE.UNIV) * (max_x - min_x) + min_x,
                randomGen.nextDouble(RandomUse.USE.UNIV) * (max_y - min_y) + min_y,
                randomGen.nextDouble(RandomUse.USE.UNIV) * 360,
                randomGen.nextDouble(RandomUse.USE.UNIV) * 90 + 15,
                randomGen.nextDouble(RandomUse.USE.UNIV) * 20 + 10, 
                0, 
                0, null, "", null, null);//RandomNumberGenerator.nextInt(5));
    }

    /**
     * @param remove_index
     */
    public void remove_camera_index( int remove_index ){
    	CameraController cc = null;
        if ( remove_index < this.cameras.size() ){
            cc = this.cameras.remove(remove_index);
        }
        
        if(USEGLOBAL){
        	if(cc != null)
        		reg.removeCamera(cc);
    	}
    }

    /**
     * @param name
     */
    public void remove_camera( String name ){
        int remove_index = -1;
        for ( int i = 0; i < this.cameras.size(); i++ ){
            System.out.println(this.cameras.get(i).getName());
            if ( this.cameras.get(i).getName().equals(name) ){
                remove_index = i;
            }
        }
        CameraController cc = null;
        if ( remove_index != -1 ){
            cc = this.cameras.remove(remove_index);
        }
        if(USEGLOBAL){
        	if(cc != null)
        		reg.removeCamera(cc);
    	}
    }

    /**
     * 
     */
    public void remove_random_camera(){
        if ( this.cameras.isEmpty()){
            return;
        }
        int rnd_int = randomGen.nextInt( this.cameras.size(), RandomUse.USE.UNIV );
        CameraController cam_to_remove = this.cameras.get(rnd_int);
        for ( CameraController c : this.cameras ){
            c.removeCamera( cam_to_remove );
        }
        this.cameras.remove(cam_to_remove);
        if(USEGLOBAL){
        	reg.removeCamera(cam_to_remove);
    	}
    }

    /**
     * 
     */
    public void recreate_cameras(){
        int num_camers = cameras.size();
        cameras.clear();
        for ( int i = 0; i < num_camers; i++ ){
            this.add_random_camera();
        }
        for(TraceableObject to : this.getObjects()){
        	if(USEGLOBAL){
        		reg.advertiseGlobally(new TraceableObjectRepresentation(to, to.getFeatures()));
        	}
        }
    }

    /**
     * @param pos_x
     * @param pos_y
     * @param heading_degrees
     * @param speed
     * @param features
     */
    public void add_object(
            double pos_x, double pos_y,
            double heading_degrees, double speed,
            double features ){
    	TraceableObject to = new TraceableObject(features, this, pos_x, pos_y, Math.toRadians(heading_degrees), speed, randomGen);
        this.getObjects().add(to);
        if(USEGLOBAL){
        	reg.advertiseGlobally(new TraceableObjectRepresentation(to, to.getFeatures()));
        }
        else{
	        for(CameraController cc : this.getCameras()){
	        	if(!cc.isOffline())
	        		cc.getAINode().receiveMessage(new Message("", cc.getName(), MessageType.StartSearch, new TraceableObjectRepresentation(to, to.getFeatures())));
	        }
        }
        
    }

    /**
     * @param pos_x
     * @param pos_y
     * @param heading_degrees
     * @param speed
     */
    public void add_object( double pos_x, double pos_y, double heading_degrees, double speed ){
        double id = 0.111 * getNextID();
        TraceableObject to = new TraceableObject(id, this, pos_x, pos_y, Math.toRadians(heading_degrees), speed, randomGen);
        this.getObjects().add(to); 
        if(USEGLOBAL){
        	reg.advertiseGlobally(new TraceableObjectRepresentation(to, to.getFeatures()));
        }
        else{
	        for(CameraController cc : this.getCameras()){
	        	if(!cc.isOffline())
	        		cc.getAINode().receiveMessage(new Message("", cc.getName(), MessageType.StartSearch, new TraceableObjectRepresentation(to, to.getFeatures())));
	        }  
        }
    }

    /**
     * @param speed
     * @param waypoints
     * @param id
     */
    public void add_object( double speed, List<Point2D> waypoints, double id){
//        double id = 0.111 * getNextID();
        TraceableObject to = new TraceableObject(id, this, speed, waypoints, randomGen);
        this.getObjects().add(to);
        if(USEGLOBAL){
        	reg.advertiseGlobally(new TraceableObjectRepresentation(to, to.getFeatures()));
        }
        else{
        	for(CameraController cc : this.getCameras()){
        		if(!cc.isOffline())
        			cc.getAINode().receiveMessage(new Message("", cc.getName(), MessageType.StartSearch, new TraceableObjectRepresentation(to, to.getFeatures())));
        	}
        }
    }

    /**
     * 
     */
    public void add_random_object(){
        add_object(
        		randomGen.nextDouble(RandomUse.USE.UNIV) * (max_x - min_x) + min_x,
        		randomGen.nextDouble(RandomUse.USE.UNIV) * (max_y - min_y) + min_y,
        		randomGen.nextDouble(RandomUse.USE.UNIV) * 360,
        		randomGen.nextDouble(RandomUse.USE.UNIV) * 0.6 + 0.4);
    }

    /**
     * 
     */
    public void remove_random_object(){
    	if(this.objects.isEmpty()){
    		return;
    	}

        int rnd = randomGen.nextInt( objects.size() , RandomUse.USE.UNIV);
        
        TraceableObject obj_to_remove = this.objects.get(rnd);

       
        for (CameraController c : this.cameras){
            c.removeObject(obj_to_remove.getFeatures());
        }
        this.objects.remove(rnd);

    }

    /**
     * @throws Exception
     */
    public void update_original() throws Exception{
    	
        /*
         * Print messages on the screen, one per step
         */
        if( CmdLogger.hasSomething() ){
            CmdLogger.update();
            System.out.println("shouldn't be possible...  !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            return;
        }

        /*
         * Update all traceable objects (move them around)
         */
        for ( TraceableObject o : this.objects ){
            o.update();
        }

        for( CameraController c : this.cameras ){
            for ( TraceableObject o : this.objects ){
            	c.update_confidence( o );
            }
        }

        for( CameraController c : this.cameras ){
            c.updateAI();
        }
        
//        checkConsistency();
//        printObjects();
        
        this.computeUtility();
        stats.nextTimeStep();
    }

    /**
     * @throws Exception
     */
    public void update() throws Exception{
    	if(_runReal){
    		updateReal();
    	}
    	else{
    		updateSim();
    	}
    	firstUpdate = false;
    	step ++;
    }
    
    /**
     * @throws Exception
     */
    public void updateReal() throws Exception{
    	if(firstUpdate)
    		setSearchFor();
    	
    	// Update all traceable objects (move them around) --> not really, just get the confidence for all objects
		for( CameraController c : this.cameras ){
			c.nextStep();
			if(!c.isOffline()){
				for ( TraceableObject o : this.objects ){
					c.update_confidence_real(step, o );
		        }
		        if(!c.getVisibleObjects_bb().isEmpty()){
		        	stats.addVisible();
		        }
			}
		

         	//run BanditSolver, select next method, set AI! hope it works ;)
         	AbstractAINode ai = c.getAINode();
         	int prevComm = ai.getComm();
         	IBanditSolver bs = ai.getBanditSolver();
         	int strategy = -1;
         	if(bs != null){
//         		if(doSelection)
         			int prevStrat = getStratForAI(ai);
         			strategy = bs.selectAction();
         			if(prevStrat != strategy)
         				stats.setStrat(strategy, c.getName());
         	}
         	
//         	System.out.println(c.getName() + " current: " + ai.getClass() + ai.getComm() + " - next: " + strategy);
         	switch (strategy) {
 			case 0:	//ABC
 				AbstractAINode newAI1 = newAINodeFromName("epics.ai.ActiveAINodeMulti", 0, ai); //staticVG, ai.getVisionGraph(), reg);
 				c.setAINode(newAI1);
 				break;
 			case 1:	//ASM
 				AbstractAINode newAI2 = newAINodeFromName("epics.ai.ActiveAINodeMulti", 1, ai); // newAINodeFromName("epics.ai.ActiveAINodeMulti", 1, staticVG, ai.getVisionGraph(), reg);
 				c.setAINode(newAI2);
 				break;
 			case 2:	//AST
 				AbstractAINode newAI3 = newAINodeFromName("epics.ai.ActiveAINodeMulti", 2,ai); // staticVG, ai.getVisionGraph(), reg);
 				c.setAINode(newAI3);
 				break;
 			case 3: //PBC
 				AbstractAINode newAI4 = newAINodeFromName("epics.ai.PassiveAINodeMulti", 0, ai); //staticVG, ai.getVisionGraph(), reg);
 				c.setAINode(newAI4);
 				break;
 			case 4: //PSM
 				AbstractAINode newAI5 = newAINodeFromName("epics.ai.PassiveAINodeMulti", 1, ai); //staticVG, ai.getVisionGraph(), reg);
 				c.setAINode(newAI5);
 				break;
 			case 5: //PST
 				AbstractAINode newAI6 = newAINodeFromName("epics.ai.PassiveAINodeMulti", 2, ai); //staticVG, ai.getVisionGraph(), reg);
 				c.setAINode(newAI6);
 				break;
 			default:
 				//STICK TO OLD
 			}
         }

         //do trading for all cameras
         for( CameraController c : this.cameras ){
 		    c.updateAI();
 		    
// 		    System.out.println(c.getName() + " util: " + c.getAINode().getUtility() + " rcved " + c.getAINode().getReceivedUtility() + " paid " + c.getAINode().getPaidUtility() + " comm " + c.getAINode().getComm() + " bids " + c.getAINode().getNrOfBids());
 		    
 		    //check if bandit solvers are used
 			IBanditSolver bs = c.getAINode().getBanditSolver();
 			if(bs != null){
// 				if(doSelection){
// 					int nrMessages = c.getAINode().getTmpTotalComm(); 
// 					double commOverhead = 0.0;
// 					if(nrMessages > 0){
// 						commOverhead = (nrMessages-c.getAINode().getTmpTotalBids()) / nrMessages; 
// 					}
// 					double utility = c.getAINode().getTmpTotalUtility() + c.getAINode().getTmpTotalRcvPay() - c.getAINode().getTmpTotalPaid(); 
// 					bs.setCurrentReward(utility, commOverhead); 
// 					currentSelectInt = 0;
// 				}
 					
 					int nrMessages = c.getAINode().getSentMessages();
 					double commOverhead = 0.0;
 					if(nrMessages > 0){
 						commOverhead = (nrMessages-c.getAINode().getNrOfBids()) / nrMessages; //
 					}
 					double utility = c.getAINode().getUtility()+c.getAINode().getReceivedUtility() - c.getAINode().getPaidUtility();
 					stats.setReward(utility, commOverhead, c.getName());
 					bs.setCurrentReward(utility, commOverhead); 
 					//currentSelectInt = 0;
 				
 				//bs.setCurrentReward(utility, commOverhead);
 			}
         }
      
         this.computeUtility();
         stats.nextTimeStep();
    }
    
    /**
     * 
     */
    private void setSearchFor() {
    	IMessage im = new Message("", "3.cvs", MessageType.StartSearch, new TraceableObjectRepresentation(this.objects.get(0), this.objects.get(0).getFeatures()));
    	CameraController cc = this.cameras.get(2);
    	AbstractAINode ai = cc.getAINode();
		ai.receiveMessage(im);
	}

	/**
	 * @throws Exception
	 */
	public void updateSim() throws Exception{
    	    	
        // Print messages on the screen, one per step
        if( CmdLogger.hasSomething() ){
            CmdLogger.update();
            System.out.println("W    T    F   !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            return;
        }
        
        boolean doSelection = false;
        //update interval for selecting new strategy
        if(selectInterval > 1){
	        if(currentSelectInt >= selectInterval){
	        	doSelection = true;
	        	currentSelectInt = 0;
	        }
	        else{
	        	currentSelectInt ++;
	        }
        }
        else{ 
        	doSelection = true; 
        }

        //check events - process event
        checkAndProcessEvent(stats.get_time_step());
        
        if(USEGLOBAL){
        	reg.update();
        }
        
        // Update all traceable objects (move them around)
        for (TraceableObject o : this.objects){
            o.update();
        }

        // random camera select - random timespan to go offline...
		int random = randomGen.nextInt(100, RandomUse.USE.ERROR);
        
		if(random <= CAMERRORRATE){
        	//select random camera and set it offline for a random number of timesteps
			int ranCam = randomGen.nextInt(this.cameras.size(), RandomUse.USE.ERROR);
        	int sleepFor = randomGen.nextInt(10, RandomUse.USE.ERROR);
        	
        	CameraController cc = cameras.get(ranCam);
        	cc.setOffline(sleepFor);
        	int ranReset = randomGen.nextInt(100, RandomUse.USE.ERROR);
        	if(ranReset > RESETRATE){
        		cc.resetCamera();
        	}
        }
        
		//update all objects position in the world view
		//select a new ai if bandit solvers are used
        for(CameraController c : this.cameras){
        	if(!c.isOffline()){
	            for (TraceableObject o : this.objects){
	            	c.update_confidence(o);
	            }
	            if(!c.getVisibleObjects_bb().isEmpty()){
	            	stats.addVisible();
	            }
        	}

        	//run BanditSolver, select next method, set AI! hope it works ;)
        	AbstractAINode ai = c.getAINode();
        	int prevComm = ai.getComm();
        	IBanditSolver bs = ai.getBanditSolver();
        	int strategy = -1;
        	if(bs != null){
//        		if(doSelection)
        			int prevStrat = getStratForAI(ai);
        			strategy = bs.selectAction();
        			///System.out.println(step + "-" + c.getName() + ": " + strategy);
        			if(prevStrat != strategy)
        				stats.setStrat(strategy, c.getName());
        	}
        	
//        	System.out.println(c.getName() + " current: " + ai.getClass() + ai.getComm() + " - next: " + strategy);
        	switch (strategy) {
			case 0:	//ABC
				AbstractAINode newAI1 = newAINodeFromName("epics.ai.ActiveAINodeMulti", 0, ai); //staticVG, ai.getVisionGraph(), reg);
				c.setAINode(newAI1);
				break;
			case 1:	//ASM
				AbstractAINode newAI2 = newAINodeFromName("epics.ai.ActiveAINodeMulti", 1, ai); // newAINodeFromName("epics.ai.ActiveAINodeMulti", 1, staticVG, ai.getVisionGraph(), reg);
				c.setAINode(newAI2);
				break;
			case 2:	//AST
				AbstractAINode newAI3 = newAINodeFromName("epics.ai.ActiveAINodeMulti", 2,ai); // staticVG, ai.getVisionGraph(), reg);
				c.setAINode(newAI3);
				break;
			case 3: //PBC
				AbstractAINode newAI4 = newAINodeFromName("epics.ai.PassiveAINodeMulti", 0, ai); //staticVG, ai.getVisionGraph(), reg);
				c.setAINode(newAI4);
				break;
			case 4: //PSM
				AbstractAINode newAI5 = newAINodeFromName("epics.ai.PassiveAINodeMulti", 1, ai); //staticVG, ai.getVisionGraph(), reg);
				c.setAINode(newAI5);
				break;
			case 5: //PST
				AbstractAINode newAI6 = newAINodeFromName("epics.ai.PassiveAINodeMulti", 2, ai); //staticVG, ai.getVisionGraph(), reg);
				c.setAINode(newAI6);
				break;
			default:
				//STICK TO OLD
			}
        }

        // Advertise each camera's owned objects
        for(CameraController c : this.cameras){
        	c.getAINode().advertiseTrackedObjects();
        }
        
        // Place all bids before updateAI() is called in the next loop
        for(CameraController c : this.cameras){
        	c.getAINode().updateReceivedDelay();
        	c.getAINode().updateAuctionDuration();
        	c.getAINode().checkIfSearchedIsVisible();
        	c.forwardMessages(); // Push messages to relevant nodes
        }
        
        //do trading for all cameras
        for( CameraController c : this.cameras ){
		    c.updateAI();

		    int nrMessages = c.getAINode().getSentMessages();
			double commOverhead = 0.0;
//			if(nrMessages > 0){
//				commOverhead = (nrMessages-c.getAINode().getNrOfBids()) / nrMessages; //
//			}
			
			commOverhead = nrMessages;
			
			stats.setCommunicationOverhead(commOverhead, c.getName());
		    
		    //check if bandit solvers are used
			IBanditSolver bs = c.getAINode().getBanditSolver();
			if(bs != null){
				double utility = c.getAINode().getUtility()+c.getAINode().getReceivedUtility() - c.getAINode().getPaidUtility();
				stats.setReward(utility, commOverhead, c.getName());
				bs.setCurrentReward(utility, commOverhead, ((double) c.getAINode().getTrackedObjects().size())); 
				//currentSelectInt = 0;
				
				//bs.setCurrentReward(utility, commOverhead);
			}
		    
//		    if(step == 999){
//		        String armsCount = ""; 
//		        IBanditSolver bso = c.getAINode().getBanditSolver();
//		        if(bso != null){
//    		        for(int i = 0; i < bso.getTotalArms().length; i++){
//    		            armsCount = armsCount + bso.getTotalArms()[i] + "; ";
//    		        }
//		        }
//		        System.out.println(alpha + " - " + epsilon + " - " + c.getName() + "; " + armsCount);//+ getStratForAI(c.getAINode()));
//		    }
        }
     
        double ut = this.computeUtility();
        //System.out.println(stats.getSummary(false));
        stats.nextTimeStep();
    }

	/**
	 * @param ai
	 * @return
	 */
	private int getStratForAI(AbstractAINode ai) {
		if(ai.getClass() == epics.ai.ActiveAINodeMulti.class){
			switch (ai.getComm()) {
				case 0:	return 0;
				case 1: return 1;
				case 2: return 2;
			}
		}
		else{
			if(ai.getClass() == epics.ai.PassiveAINodeMulti.class){
				switch (ai.getComm()) {
					case 0:	return 3;
					case 1: return 4;
					case 2: return 5;
				}
			}
		}
		return -1;
	}

	/**
	 * @param currentTimeStep
	 */
	private void checkAndProcessEvent(int currentTimeStep) {
		for(SimSettings.Event e : events){
			if(e.timestep == currentTimeStep){
				//process event
				if(e.event.equals("add")){
					if(e.participant == 1){ // camera
						this.add_camera(e.name, e.x, e.y, e.heading, e.angle, e.range, e.comm, e.limit, null, e.bandit, null, null);
					}
					else{ //object 
						if(e.waypoints == null){
							this.add_object(e.x, e.y, e.heading, e.speed, Double.parseDouble(e.name));
						}
						else{
							this.add_object(e.speed, e.waypoints, Double.parseDouble(e.name));
						}
					}
				}
				else if(e.event.equals("error")){
					if(e.participant == 1){ // camera
						if(e.duration == -1){
							if(USEGLOBAL){
								reg.removeCamera(this.getCameraByName(e.name));
							}
							
							if(!getCameraByName(e.name).isOffline())
								this.getCameraByName(e.name).setOffline(-1);
						}
						else{
							if(!getCameraByName(e.name).isOffline())
								this.getCameraByName(e.name).setOffline(e.duration);
						}
						
					}
					else if(e.participant == 2){ //object
						int remove = -1;
						
						String[] fs = e.name.split(";");
						List<Double> ownFeatures = new ArrayList<Double>();
						for(int j = 0; j < fs.length; j++){
							ownFeatures.add(Double.parseDouble(fs[j]));
						}
						boolean found = false;
						
						//TraceableObject tor = new TraceableObject(ownFeatures.get(0), this, 0, 0, 0, 0);
						
						for (CameraController cc : this.cameras) {
							cc.removeObject(ownFeatures);
						}
						
						for(int i = 0; i < getObjects().size(); i++){
							List<Double> otherFeatures = getObjects().get(i).getFeatures();
							found = true;
							if(ownFeatures.size() == otherFeatures.size()){
								for(int k = 0; k < ownFeatures.size(); k++){
									if(!ownFeatures.get(k).equals(otherFeatures.get(k))){
										found = false;
									}
								}
							}
							else{
								found = false;
							}
							
							if(found == true){
								remove = i;
								break;
							}
						}
						
						if(found){							
							this.objects.remove(remove);
							//getObjects().remove(remove);
						}
					}
					else if(e.participant == 3){ //GRC
						if(reg != null){
							reg.setOffline(e.duration);
						}
					}
				}
				else if(e.event.equals("change")){
					if(e.participant == 1){ // camera
						for ( int i = 0; i < this.cameras.size(); i++ ){
				            CameraController cc = this.cameras.get(i);
				            if ( cc.getName().equals(e.name) ){
				                cc.change(e.x, e.y, e.heading, e.angle, e.range);
				            }
				        }
					}
				}
			}
		}
	}

	/**
	 * @param c
	 */
	private void addFalseObject(CameraController c) {
    	double id = 0.111 * getNextID();

    	double tmp_x = 0;
        double tmp_y = -1;
        double tmp_heading = c.getHeading();
        double tmp_range = c.getRange();
        
        double tmp_cos = Math.cos(tmp_heading);
        double tmp_sin = Math.sin(tmp_heading);
        
    	double vcx = tmp_x * Math.cos(tmp_heading) - tmp_y * Math.sin(tmp_heading);
        double vcy = tmp_x * Math.sin(tmp_heading) - tmp_y * Math.cos(tmp_heading);
 
        double pos_x = c.getX() + 10 * vcx; //tmp_heading;
        double pos_y = c.getY() + 10 * vcy; //tmp_heading;

    	double heading_degrees = randomGen.nextDouble(RandomUse.USE.UNIV) * 360;
    	double speed = randomGen.nextDouble(RandomUse.USE.UNIV) * 0.6 + 0.4;
    	TraceableObject to = new TraceableObject(id, this, pos_x, pos_y, Math.toRadians(heading_degrees), speed, randomGen);
	}

	/**
	 * @throws Exception
	 */
	private void printObjects() throws Exception {
		Map<TraceableObject, List<CameraController>> tracked = new HashMap<TraceableObject, List<CameraController>>();
		Map<TraceableObject, List<CameraController>> searched = new HashMap<TraceableObject, List<CameraController>>(); 
		for(CameraController c : this.cameras){
    		for(epics.common.ITrObjectRepresentation to : c.getAINode().getTrackedObjects().values()){
    			TraceableObjectRepresentation tor = (TraceableObjectRepresentation) to;
    			if(tracked.containsKey(tor.getTraceableObject())){
    				tracked.get(tor.getTraceableObject()).add(c);
    			}
    			else{
    				List<CameraController> list = new ArrayList<CameraController>();
    				list.add(c);
    				tracked.put(tor.getTraceableObject(), list);
    			}
    		}
    		if(c.getAINode().getSearchedObjects() != null){
	    		for(epics.common.ITrObjectRepresentation to : c.getAINode().getSearchedObjects().keySet()){
	    			TraceableObjectRepresentation tor = (TraceableObjectRepresentation) to;
	    			if(searched.containsKey(tor.getTraceableObject())){
	    				searched.get(tor.getTraceableObject()).add(c);
	    			}
	    			else{
	    				List<CameraController> list = new ArrayList<CameraController>();
	    				list.add(c);
	    				searched.put(tor.getTraceableObject(), list);
	    			}
	    		}
    		}
    	}
		
		System.out.println("############################ PRINT OBJECT INFO #########################################");
		int sum = tracked.size() + searched.size();
		System.out.println("searched size: " + searched.size() + " + tracked size: " + tracked.size() + " = " + sum + " should be: " + this.objects.size());
//		if((tracked.size() + searched.size()) != this.objects.size())
//			throw new Exception("INCONSISTENCY: " + tracked.size() + searched.size() + " is not " + this.objects.size());
		System.out.println("");
		System.out.println("object + searched + tracked");
		for(TraceableObject to : this.objects){
			String output = "Object " + to.getFeatures() + " searched by ";
			if(searched.containsKey(to)){
				for(CameraController c : searched.get(to)){
					output += c.getName() + ", ";
				}
			}
			output += " tracked by ";
			if(tracked.containsKey(to)){
				for(CameraController c : tracked.get(to)){
					output += c.getName() + ", ";
				}
			}
			System.out.println(output);
		}
		System.out.println("############################ END OBJECT INFO #########################################");
	}

	/**
	 * @throws Exception
	 */
	public void checkConsistency() throws Exception{
    	
    	Map<TraceableObject, Boolean> tracing = new HashMap<TraceableObject, Boolean>();
    	Map<TraceableObject, Boolean> searching = new HashMap<TraceableObject, Boolean>();
    	
    	for(CameraController c : this.cameras){
    		for(epics.common.ITrObjectRepresentation to : c.getAINode().getTrackedObjects().values()){
    			TraceableObjectRepresentation tor = (TraceableObjectRepresentation) to;
    			tracing.put(tor.getTraceableObject(), true);
    			if(c.getVisibleObjects().containsKey(tor)){
    				throw new Exception("wait what? inconsistent - if its not visible, it cant be tracked!!");
    			}
    		}
    		if(c.getAINode().getSearchedObjects() != null){
	    		for(epics.common.ITrObjectRepresentation to : c.getAINode().getSearchedObjects().keySet()){
	    			TraceableObjectRepresentation tor = (TraceableObjectRepresentation) to;
	    			searching.put(tor.getTraceableObject(), true);
	    		}
    		}
    		
    		
    	}
    	
    	for(TraceableObject to : this.objects){
    		if(!tracing.containsKey(to)){
    			if(!searching.containsKey(to)){
    				throw new Exception("INCONSISTENCY!!");
    			}
    		}
    	}
    }

    /**
     * @return
     */
    public double computeUtility(){
        double utility_sum = 0;
        for (CameraController c : this.cameras){
        	if(!c.isOffline()){
        		utility_sum += c.getAINode().getUtility();
        		try {
					stats.addUtility( c.getAINode().getUtility(), c.getName());
				} catch (Exception e) {
					e.printStackTrace();
				}
        	}
        }
        return utility_sum;
    }

    /**
     * @return
     */
    public ArrayList<CameraController> getCameras() {
        return cameras;
    }

    /**
     * @param name
     * @return
     */
    public CameraController getCameraByName( String name ){
        for ( int i = 0; i < cameras.size(); i++ ){
            if ( cameras.get(i).getName().compareTo(name) == 0 ){
                return cameras.get(i);
            }
        }
        return null;
    }

    /**
     * @return
     */
    public ArrayList<TraceableObject> getObjects() {
        return objects;
    }

    /** Save the scenario currently active in the simulation to an XML file.
     * Note that this does not fully support scenario XML features such as 
     * objects with waypoints. It also does not represent angles 100% correctly. */
	/**
	 * @param absolutePath
	 */
	public void save_to_xml(String absolutePath) {
		File f = new File(absolutePath + ".xml");
		
		FileWriter fw;
		try {
			fw = new FileWriter(f);
			String s = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"+System.getProperty( "line.separator" )+"<root>"+System.getProperty( "line.separator" )+"	    " +
				"<simulation max_x=\"" + max_x + "\" max_y=\"" + max_y + "\" min_x=\"" + min_x + "\" min_y=\"" + min_y + "\">"+ System.getProperty( "line.separator" )+ "	        <cameras>"+ System.getProperty( "line.separator" );
			fw.write(s);
			
			for (CameraController cam : cameras) {
				fw.write("	        	     "+ cam.toString() + System.getProperty("line.separator"));
			}
			fw.write("	        </cameras>"+System.getProperty( "line.separator" )+"        <objects>"+System.getProperty( "line.separator" ));
			for(TraceableObject to : objects){
				fw.write("	        	     "+ to.toXMLString("	        	     ") + System.getProperty("line.separator"));
			}
			fw.write("        </objects>"+System.getProperty("line.separator")+"    </simulation>"+System.getProperty("line.separator")+"</root>");
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	public void reset() {
		this.min_x = -70;
		this.max_x = 70;
		this.min_y = -70;
		this.max_y = 70;	
	}

	/**
	 * @param spaces
	 * @return
	 * @throws Exception
	 */
	public String getStatSummary(boolean spaces) throws Exception{
	    return stats.getSummary(spaces);
	}
	
	/**
	 * @param spaces
	 * @return
	 * @throws Exception
	 */
	public String getStatSumDesc(boolean spaces) throws Exception{
	    return stats.getSummaryDesc(spaces);
	}
}
