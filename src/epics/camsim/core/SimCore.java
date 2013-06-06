package epics.camsim.core;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import epics.common.IMessage.MessageType;
import epics.common.IRegistration;
import epics.common.RandomNumberGenerator;
import epics.common.RandomUse;
import epics.common.RunParams;

/**
 *
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */

public class SimCore {

	int TRACKERERROR = -1; //percent of missdetected objects
	int CAMERRORRATE = -1; //percent of camera error. -1 = no camera error
	int RESETRATE = 50; //looses knowledge about everything - happens in x percentage of cameraerror (only when error occurs, knowledgeloss can happen)
	static final boolean BIDIRECTIONAL_VISION = true;
	boolean USEGLOBAL = false;
	
	
    static int camIDGenerator = 0;
    public static int getNextID(){ return camIDGenerator++; }

    /*
     * Simulation area
     */
    private double min_x;     public double get_min_x(){return min_x;}
    private double max_x;     public double get_max_x(){return max_x;}
    private double min_y;     public double get_min_y(){return min_y;}
    private double max_y;     public double get_max_y(){return max_y;}
    private long sim_time;    public long get_sim_time(){return sim_time;}
    private String ai_alg; 	  public boolean staticVG = false;
   
    private boolean firstUpdate;
    private ArrayList<SimSettings.Event> events;
    
    private IRegistration reg = null;
    private SimSettings settings;
    private String paramFile;

    private void checkCoordInRange( double x, double y ){
        if ( x < min_x || x > max_x || y < min_y || y > max_y ){
            throw new IllegalArgumentException("x/y value out of simulation field.");
        }
    }

    private ArrayList<CameraController> cameras = new ArrayList<CameraController>();
    private ArrayList<TraceableObject> objects = new ArrayList<TraceableObject>();
	private int _comm = -1;
    
	public SimCore( long seed, String output, SimSettings ss, boolean global, int camError, int camReset, int trackError) {
	    this.RESETRATE = camReset;
	    this.CAMERRORRATE = camError;
	    this.TRACKERERROR = trackError;
		
		USEGLOBAL = global;
		    	
	    if(USEGLOBAL){
	    	reg = new GlobalRegistration();
	    }
		
	    Statistics.init(output, "sum_"+output);
	    RandomNumberGenerator.init(seed);
		this.interpretFile(ss);
		ss.printSelfToCMD();   
		
	}
	
    public SimCore(long seed, String output, String summaryFile, String paramFile, 
    		SimSettings ss, boolean global, int camError, int camReset, 
    		int trackError) {
	    this.RESETRATE = camReset;
	    this.CAMERRORRATE = camError;
	    this.TRACKERERROR = trackError;
	    
    	USEGLOBAL = global;
    	    	
        if (USEGLOBAL) {
        	reg = new GlobalRegistration();
        }
    	
        Statistics.init(output, summaryFile);
        RandomNumberGenerator.init(seed);
        this.paramFile = paramFile;

        // Setup should be done before this call, which 
        // constructs cameras and the vision graph
    	this.interpretFile(ss);
    	ss.printSelfToCMD();   
    }
    
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
    	
    		
    	for ( SimSettings.CameraSettings cs : ss.cameras ){
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
                    cs.range, cs.ai_algorithm, cs.comm, cs.limit, vg);
        }
    	

        for (SimSettings.TrObjectSettings tro : ss.objects){
            this.add_object(tro.x, tro.y, tro.heading, tro.speed, tro.features);
        }
        
        for (TrObjectWithWaypoints objWithWP : ss.objectsWithWaypoints) {
        	this.add_object(objWithWP.speed, objWithWP.waypoints, objWithWP.features);
        }
        
        events = ss.events;
    }

    public void close_files(){
        Statistics.close();
    }

    public void add_camera(
            String name,
            double x_pos, double y_pos,
            double heading_degrees,
            double angle_degrees,
            double range,
            String ai_algorithm,
            int comm, int limit, Map<String, Double> vg){

        ai_alg = ai_algorithm;
        add_camera(
        	name, x_pos, y_pos, heading_degrees, angle_degrees,
            range, comm, limit, vg);
    }

    public void add_camera(String name,
            double x_pos, double y_pos,
            double heading_degrees, double angle_degrees, double range, int comm, int limit, Map<String, Double> vg){

    	if(_comm == -1){
    		_comm = comm;
    	}
    	
        checkCoordInRange(x_pos, y_pos);
        
        AbstractAINode aiNode = null;
    	try {
    		aiNode = newAINodeFromName(ai_alg, comm, staticVG, vg, reg);
    		if (paramFile != null) {
    			this.applyParamsToAINode(aiNode, paramFile);
    		}
    	} catch (Exception e) {
    		System.out.println("Invalid ai_algorithm parameter in scenario file: "+ai_alg);
    		System.out.println("Must be a fully qualified class name e.g. 'epics.ai.ActiveAINodeMulti'");
    		e.printStackTrace();
    		System.exit(1);
    	}
        
        CameraController cc = new CameraController(
        		name,
                x_pos, y_pos,
                Math.toRadians(heading_degrees),
                Math.toRadians(angle_degrees),
                range, aiNode, limit, 100 - TRACKERERROR);

        if(USEGLOBAL){
        	reg.addCamera(cc);
    	}
        
        this.getCameras().add( cc );
        for ( CameraController c1 : this.cameras ){
            c1.addCamera( cc );
            cc.addCamera(c1);
        }
	}
    
    /** Given a node's class name, dynamically loads the class and 
     * instantiates a new node of that type. */
    public AbstractAINode newAINodeFromName(String fullyQualifiedClassName, 
    		int comm, boolean staticVG, Map<String, Double> vg, IRegistration r) 
    				throws ClassNotFoundException, SecurityException, NoSuchMethodException, 
    				IllegalArgumentException, InstantiationException, IllegalAccessException, 
    				InvocationTargetException {
    	Class<?> nodeType = Class.forName(fullyQualifiedClassName);
    	Class<?>[] constructorTypes = {int.class, boolean.class, Map.class, IRegistration.class};
    	Constructor<?> cons = nodeType.getConstructor(constructorTypes);
    	AbstractAINode node = (AbstractAINode) cons.newInstance(comm, staticVG, vg, r);
    	return node;
    }

    /** Given a file which contains parameters for our run, we run through 
     * the params and apply each one to the AI node. This is mainly to aid
     * running of experiments, where the necessary parameters can be applied
     * to the node for a particular run, then the params file is changed for 
     * the next run */
    public void applyParamsToAINode(AbstractAINode node, String paramsFilepath) throws IOException {
    	RunParams.loadIfNotLoaded(paramsFilepath);
    	
    	Set<Entry<Object,Object>> props = RunParams.getAllProperties();
    	for (Entry<Object, Object> prop : props) {
    		String key = (String) prop.getKey();
    		String value = (String) prop.getValue();
    		if (! node.setParam(key, value)) {
    			throw new IllegalStateException("Param "+key+" could not be applied");
    		}
    	}
    }
    
  	public void add_random_camera(){
        this.add_camera(
        		"C"+getNextID(),
                RandomNumberGenerator.nextDouble(RandomUse.USE.UNIV) * (max_x - min_x) + min_x,
                RandomNumberGenerator.nextDouble(RandomUse.USE.UNIV) * (max_y - min_y) + min_y,
                RandomNumberGenerator.nextDouble(RandomUse.USE.UNIV) * 360,
                RandomNumberGenerator.nextDouble(RandomUse.USE.UNIV) * 90 + 15,
                RandomNumberGenerator.nextDouble(RandomUse.USE.UNIV) * 20 + 10, 
                0, 
                0, null);//RandomNumberGenerator.nextInt(5));
    }

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

    public void remove_random_camera(){
        if ( this.cameras.isEmpty()){
            return;
        }
        int rnd_int = RandomNumberGenerator.nextInt( this.cameras.size(), RandomUse.USE.UNIV );
        CameraController cam_to_remove = this.cameras.get(rnd_int);
        for ( CameraController c : this.cameras ){
            c.removeCamera( cam_to_remove );
        }
        this.cameras.remove(cam_to_remove);
        if(USEGLOBAL){
        	reg.removeCamera(cam_to_remove);
    	}
    }

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
//        	else{
//		        for(CameraController cc : this.getCameras()){
//		    		if(!(cc.getAINode() instanceof ActiveAINodeMultiAsker)){ //(cc.getAINode() instanceof PassiveAINodeMulti)||(cc.getAINode() instanceof PassiveAINodeSingle)){
//		    			if(!cc.isOffline())
//		    				cc.getAINode().receiveMessage(new Message("", cc.getName(), MessageType.StartSearch, new TraceableObjectRepresentation(to, to.getFeatures())));
//			    	}
//		    	}
//        	}
        }
    }

    public void add_object(
            double pos_x, double pos_y,
            double heading_degrees, double speed,
            double features ){
    	TraceableObject to = new TraceableObject(features, this, pos_x, pos_y, Math.toRadians(heading_degrees), speed);
        this.getObjects().add(to);
        if(USEGLOBAL){
        	reg.advertiseGlobally(new TraceableObjectRepresentation(to, to.getFeatures()));
        }
        else{
	        for(CameraController cc : this.getCameras()){
//	    		if(!(cc.getAINode() instanceof ActiveAINodeMultiAsker)){ //(cc.getAINode() instanceof PassiveAINodeMulti)||(cc.getAINode() instanceof PassiveAINodeSingle)){
	    			if(!cc.isOffline())
	    				cc.getAINode().receiveMessage(new Message("", cc.getName(), MessageType.StartSearch, new TraceableObjectRepresentation(to, to.getFeatures())));
//		    	}
	    	}
        }
        
    }

    public void add_object( double pos_x, double pos_y, double heading_degrees, double speed ){
        double id = 0.111 * getNextID();
        TraceableObject to = new TraceableObject(id, this, pos_x, pos_y, Math.toRadians(heading_degrees), speed);
        this.getObjects().add(to); 
        if(USEGLOBAL){
        	reg.advertiseGlobally(new TraceableObjectRepresentation(to, to.getFeatures()));
        }
        else{
	        for(CameraController cc : this.getCameras()){
//	    		if(!(cc.getAINode() instanceof ActiveAINodeMultiAsker)){ //(cc.getAINode() instanceof PassiveAINodeMulti)||(cc.getAINode() instanceof PassiveAINodeSingle)){
	    			if(!cc.isOffline())
	    				cc.getAINode().receiveMessage(new Message("", cc.getName(), MessageType.StartSearch, new TraceableObjectRepresentation(to, to.getFeatures())));
//		    	}
	    	}  
        }
    }

    public void add_object( double speed, List<Point2D> waypoints, double id){
//        double id = 0.111 * getNextID();
        TraceableObject to = new TraceableObject(id, this, speed, waypoints);
        this.getObjects().add(to);
        if(USEGLOBAL){
        	reg.advertiseGlobally(new TraceableObjectRepresentation(to, to.getFeatures()));
        }
        else{
	        for(CameraController cc : this.getCameras()){
//	    		if(!(cc.getAINode() instanceof ActiveAINodeMultiAsker)){ //((cc.getAINode() instanceof PassiveAINodeMulti)||(cc.getAINode() instanceof PassiveAINodeSingle)){
	    			if(!cc.isOffline())
	    				cc.getAINode().receiveMessage(new Message("", cc.getName(), MessageType.StartSearch, new TraceableObjectRepresentation(to, to.getFeatures())));
//		    	}
	    	}
        }
    }

    public void add_random_object(){
        add_object(
                RandomNumberGenerator.nextDouble(RandomUse.USE.UNIV) * (max_x - min_x) + min_x,
                RandomNumberGenerator.nextDouble(RandomUse.USE.UNIV) * (max_y - min_y) + min_y,
                RandomNumberGenerator.nextDouble(RandomUse.USE.UNIV) * 360,
                RandomNumberGenerator.nextDouble(RandomUse.USE.UNIV) * 0.6 + 0.4);
    }

    public void remove_random_object(){
    	if(this.objects.isEmpty()){
    		return;
    	}
    	
        int rnd = RandomNumberGenerator.nextInt( objects.size() , RandomUse.USE.UNIV);
        
        TraceableObject obj_to_remove = this.objects.get(rnd);

       
        for ( CameraController c : this.cameras ){
            c.removeObject(obj_to_remove.getFeatures());
        }
        this.objects.remove(rnd);

    }

    public void update() throws Exception{
    	    	
        // Print messages on the screen, one per step
        if( CmdLogger.hasSomething() ){
            CmdLogger.update();
            System.out.println("W    T    F   !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            return;
        }

        //TODO check events - process event
        checkAndProcessEvent(Statistics.get_time_step());
        
        if(USEGLOBAL){
        	reg.update();
        }
        
        // Update all traceable objects (move them around)
        for ( TraceableObject o : this.objects ){
            o.update();
        }

        // random camera select - random timespan to go offline...
		int random = RandomNumberGenerator.nextInt(100, RandomUse.USE.ERROR);
        
		if(random <= CAMERRORRATE){
        	//select random camera and set it offline for a random number of timesteps
			int ranCam = RandomNumberGenerator.nextInt(this.cameras.size(), RandomUse.USE.ERROR);
        	int sleepFor = RandomNumberGenerator.nextInt(10, RandomUse.USE.ERROR);
        	
        	CameraController cc = cameras.get(ranCam);
        	cc.setOffline(sleepFor);
        	int ranReset = RandomNumberGenerator.nextInt(100, RandomUse.USE.ERROR);
        	if(ranReset > RESETRATE){
        		cc.resetCamera();
        	}
        }
        
        for( CameraController c : this.cameras ){
        	if(!c.isOffline()){
	            for ( TraceableObject o : this.objects ){
	            	c.update_confidence( o );
	            }
	            
	            if(!c.getVisibleObjects_bb().isEmpty()){
	            	Statistics.addVisible();
	            }
        	}
            
//            if(detectedFalseObejct()){
//            	addFalseObject(c);
//            }
        }

        // Place all bids before updateAI() is called in the next loop
        for(CameraController c : this.cameras){
        	c.getAINode().updateReceivedDelay();
        	c.getAINode().updateAuctionDuration();
        	c.getAINode().checkIfSearchedIsVisible();
        	c.forwardMessages(); // Push messages to relevant nodes
        }
        
        for(CameraController c : this.cameras){
        	c.updateAI();
        	Statistics.addMissidentified(c.currentlyMissidentified());
        }
        
        Statistics.addUtility( this.computeUtility() );
        Statistics.nextTimeStep();
    }

	private void checkAndProcessEvent(int currentTimeStep) {
		for(SimSettings.Event e : events){
			if(e.timestep == currentTimeStep){
				//process event
				if(e.event.equals("add")){
					if(e.participant == 1){ // camera
						this.add_camera(e.name, e.x, e.y, e.heading, e.angle, e.range, e.comm, e.limit, null);
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

    	double heading_degrees = RandomNumberGenerator.nextDouble(RandomUse.USE.UNIV) * 360;
    	double speed = RandomNumberGenerator.nextDouble(RandomUse.USE.UNIV) * 0.6 + 0.4;
    	TraceableObject to = new TraceableObject(id, this, pos_x, pos_y, Math.toRadians(heading_degrees), speed);
    	
	}

//	private boolean detectedFalseObejct() {
//		int res = RandomNumberGenerator.nextInt(100);
//		if(res < TRACKERERROR){
//			return true;
//		}
//		return false;
//	}


	private void printObjects() throws Exception {
		Map<TraceableObject, List<CameraController>> traced = new HashMap<TraceableObject, List<CameraController>>();
		Map<TraceableObject, List<CameraController>> searched = new HashMap<TraceableObject, List<CameraController>>(); 
		for(CameraController c : this.cameras){
    		for(epics.common.ITrObjectRepresentation to : c.getAINode().getTracedObjects().values()){
    			TraceableObjectRepresentation tor = (TraceableObjectRepresentation) to;
    			if(traced.containsKey(tor.getTraceableObject())){
    				traced.get(tor.getTraceableObject()).add(c);
    			}
    			else{
    				List<CameraController> list = new ArrayList<CameraController>();
    				list.add(c);
    				traced.put(tor.getTraceableObject(), list);
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
		int sum = traced.size() + searched.size();
		System.out.println("searched size: " + searched.size() + " + traced size: " + traced.size() + " = " + sum + " should be: " + this.objects.size());
//		if((traced.size() + searched.size()) != this.objects.size())
//			throw new Exception("INCONSISTENCY: " + traced.size() + searched.size() + " is not " + this.objects.size());
		System.out.println("");
		System.out.println("object + searched + traced");
		for(TraceableObject to : this.objects){
			String output = "Object " + to.getFeatures() + " searched by ";
			if(searched.containsKey(to)){
				for(CameraController c : searched.get(to)){
					output += c.getName() + ", ";
				}
			}
			output += " traced by ";
			if(traced.containsKey(to)){
				for(CameraController c : traced.get(to)){
					output += c.getName() + ", ";
				}
			}
			System.out.println(output);
		}
		System.out.println("############################ END OBJECT INFO #########################################");
	}

	public void checkConsistency() throws Exception{
    	
    	Map<TraceableObject, Boolean> tracing = new HashMap<TraceableObject, Boolean>();
    	Map<TraceableObject, Boolean> searching = new HashMap<TraceableObject, Boolean>();
    	
    	for(CameraController c : this.cameras){
    		for(epics.common.ITrObjectRepresentation to : c.getAINode().getTracedObjects().values()){
    			TraceableObjectRepresentation tor = (TraceableObjectRepresentation) to;
    			tracing.put(tor.getTraceableObject(), true);
    			if(c.getVisibleObjects().containsKey(tor)){
    				throw new Exception("wait what? inconsistent - if its not visible, it cant be traced!!");
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
//    		else{
//    			if(searching.containsKey(to)){
//    				throw new Exception("INCONSISTENCY!!!");
//    			}
//    		}
    	}
    }

    public double computeUtility(){
        double utility_sum = 0;
        for (CameraController c : this.cameras){
        	if(!c.isOffline())
        		utility_sum += c.getAINode().getUtility();
        }
        return utility_sum;
    }

    /**
     * @return the cameras
     */
    public ArrayList<CameraController> getCameras() {
        return cameras;
    }

    public CameraController getCameraByName( String name ){
        for ( int i = 0; i < cameras.size(); i++ ){
            if ( cameras.get(i).getName().compareTo(name) == 0 ){
                return cameras.get(i);
            }
        }
        return null;
    }

    /**
     * @return the objects
     */
    public ArrayList<TraceableObject> getObjects() {
        return objects;
    }

    /** Save the scenario currently active in the simulation to an XML file.
     * Note that this does not fully support scenario XML features such as 
     * objects with waypoints. It also does not represent angles 100% correctly. */
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void reset() {
		this.min_x = -70;
		this.max_x = 70;
		this.min_y = -70;
		this.max_y = 70;	
	}
}
