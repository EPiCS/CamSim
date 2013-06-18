package epics.camsim.core;

import epics.common.*;
import epics.common.IMessage.MessageType;

import java.util.*;

/**
 *
 * @author Lukas Esterle & Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public class CameraController implements ICameraController{

	private int LIMIT = 0;
	private int DETECTIONRATE = 80; //percent to detect object that is there
	private AbstractAINode camAINode;

    private String name;

    private int isOfflineFor = 0;
    private double x; // meters
    private double y; // meters
    private double heading; // radians, 0 is north, PI/2 is east
    private double viewing_angle; // radians
    private double range; // meters
    private Resources resources;
    
    private Map<IMessage, Integer> sendOut = new HashMap<IMessage, Integer>();
    private static int DELAY_COMM = 0; 

    private ArrayList<CameraController> neighbours = new ArrayList<CameraController>();

    private Map<TraceableObject, Double> visible_objects
            = new HashMap<TraceableObject, Double>();
	private boolean sleepForever = false;
	
	private Statistics stats;
	private RandomNumberGenerator randomGen;
	
	private ArrayList<ArrayList<Double>> predefConf;
	private ArrayList<ArrayList<Integer>> predefVis;
	private int step;
    
	/**
	 * instantiates a new camera in the simulator
	 * @param name
	 * @param x
	 * @param y
	 * @param heading
	 * @param viewing_angle
	 * @param range
	 * @param ai
	 * @param limit
	 * @param detectionRate
	 * @param stats
	 * @param rg
	 * @param predefConfidences defines a list of objects represented by an ArrayList of their confidences where each element is for one frame/timestep 
	 * @param predefVisibility defines a list of objects represented by an ArrayList of their visibility (0 = visible, 1 = not visible or at touching border) where each element is for one frame/timestep
	 */
    public CameraController( String name, double x, double y,
                double heading, double viewing_angle, double range, AbstractAINode ai, int limit, int detectionRate, Statistics stats, RandomNumberGenerator rg, ArrayList<ArrayList<Double>> predefConfidences, ArrayList<ArrayList<Integer>> predefVisibility){
    	this.DETECTIONRATE = detectionRate;
    	this.LIMIT = limit;
        this.x = x;
        this.y = y;
        this.heading = heading;
        this.viewing_angle = viewing_angle;
        this.range = range;
        this.name = name;
        this.camAINode = ai;
        this.resources = new Resources();
        ai.setController(this);
        this.stats = stats;
        this.randomGen = rg;
        this.predefConf = predefConfidences;
        this.predefVis = predefVisibility;
        this.step = -1;
    }
    
    public double getAvailableResources(){
    	if(isOffline()){
    		return 0;
    	}
    	else{
    		return resources.getAvailableResources();
    	}
    }
    
    public void reduceResources(double amount){
    	if(!isOffline()){
    		resources.reduceResources(amount);
    	}
    }
    
    public void addResources(double amount){
    	if(!isOffline()){
    		resources.addResources(amount);
    	}
    }

    void updateAI() {
    	if(isOffline()){
    		isOfflineFor--;
    	}
    	else{
    		this.camAINode.update();
    		this.forwardMessages();
    	}
    }
    

	public String getName() {
    	if(!isOffline()){
    		return this.name;
    	}
    	else{
    		return "Offline";
    	}
    }

    public void addCamera(CameraController cam) {
    	if(!isOffline()){
	        if (cam != this) {
	            if ( !this.neighbours.contains( cam )){
	                this.neighbours.add(cam);
	            }
	        }
    	}
    }

    public void removeCamera( CameraController cam ){
    	if(!isOffline()){
	        while( this.neighbours.contains(cam)){
	            this.neighbours.remove( cam );
	        }
    	}
    }

    /**
     * The main purpose of this method is to check if TraceableObject o is
     * visible to camera, if yes, then calculate confidence and further call
     * addVisibleObject or removeVisibleObject.
     */
    public double update_confidence(TraceableObject o) {
    	if (!isOffline()) {
	        double result_confidence = 0;
	
	        double cx = this.getX();
	        double cy = this.getY();
	        double ox = o.getX();
	        double oy = o.getY();
	
	        double dist = Math.sqrt((ox - cx) * (ox - cx) + (oy - cy) * (oy - cy));
	
	        if (gotDetection()) {
		        if (dist > this.getRange()) {
		            // Object out of range
		            result_confidence = 0;
		        } else {
		
		            double tmp_x = 0;
		            double tmp_y = -1;
		
		            double tmp_heading = this.getHeading();
		
		            double vcx = tmp_x * Math.cos(tmp_heading) - tmp_y * Math.sin(tmp_heading);
		            double vcy = tmp_x * Math.sin(tmp_heading) - tmp_y * Math.cos(tmp_heading);
		
		            double vox = ox - cx;
		            double voy = oy - cy;
		
		            double vo_len = Math.sqrt(vox * vox + voy * voy);
		
		            vox = vox / vo_len;
		            voy = voy / vo_len;
		
		            double dot = vcx * vox + vcy * voy;
		
		            double angle = Math.acos(dot);
		
		            if (angle < this.getAngle() / 2) {
		                double dist_conf = (this.getRange() - dist) / this.getRange();
		
		                //dist_conf = dist_conf * 5; // so we use more of atan
		                //dist_conf = Math.atan( 1 / dist_conf );
		
		                double ang_conf = (this.getAngle() / 2 - angle) / (this.getAngle() / 2);
		
		                double total_conf = dist_conf * ang_conf;

		                //System.out.println( "result " + total_conf + " = " + dist_conf + " * " + ang_conf + " // total = dist * ang");
		
		                // Object in range and in front of camera
		                result_confidence = total_conf;
		
		            } else {
		                // Object in range, but not in front of camera
		                result_confidence = 0;
		            }
		        }
	        } else {
	        	result_confidence = 0;
	        }
	
	        if (result_confidence > 0 ) {
	            this.addVisibleObject(o, result_confidence);
	        } else {
	            this.removeVisibleObject(o);
	        }
	        
	        return result_confidence;
    	} else {
    		return 0;
    	}
    }

    private boolean gotDetection() {
    	if(!isOffline()){
			int res = randomGen.nextInt(100, RandomUse.USE.UNIV);
			if(res > DETECTIONRATE){
				return false;
			}
			return true;
    	}
    	else{
    		return false;
    	}
	}
    
    private void addVisibleObject( TraceableObject tc, double confidence ){
    	if(!isOffline()){
	        if ( ! this.visible_objects.containsKey(tc)){
	
	            this.visible_objects.put( tc, confidence );
	
//	            this.camAINode.addVisibleObject(
//	                    new TraceableObjectRepresentation(tc, tc.getFeatures()));
	            
	        }else{
	            this.visible_objects.put(tc, confidence);
	        }
    	}
    }
    
    private void removeVisibleObject( TraceableObject tc ){
    	if(!isOffline()){
	        if (this.visible_objects.containsKey(tc)){
	
	            this.camAINode.removeVisibleObject( new TraceableObjectRepresentation(tc, tc.getFeatures()));
	        
	            this.visible_objects.remove(tc);
	        }
    	}
    }
    
    @Override
    public void removeObject(List<Double> features){
    	List<TraceableObject> remove = new ArrayList<TraceableObject>();
    	for (TraceableObject tc : visible_objects.keySet()) {
    		if(tc.getFeatures().equals(features)){
    			remove.add(tc);
    		}
		}
    	
    	for (TraceableObject traceableObject : remove) {
			removeVisibleObject(traceableObject);
		}
    }

    public TraceableObject getTraced(){
    	if(!isOffline()){
	        ITrObjectRepresentation itro = this.camAINode.getTrackedObject();
	        if ( itro == null ){
	            return null;
	        }
	        TraceableObjectRepresentation tro = (TraceableObjectRepresentation)itro;
	        return tro.getTraceableObject();
    	}
    	else{
    		return null;
    	}
    }
    
    public Map<List<Double>, TraceableObject> getTrackedObjects(){
    	Map<List<Double>, TraceableObject> retVal = new HashMap<List<Double>, TraceableObject>();
    	if(!isOffline()){
	    	for(Map.Entry<List<Double>, ITrObjectRepresentation> e : this.camAINode.getTrackedObjects().entrySet()){
	    		retVal.put(e.getKey(), ((TraceableObjectRepresentation)e.getValue()).getTraceableObject());
	    	}	
    	}
    	return retVal;
    }

    public Map<TraceableObject, Double> getVisibleObjects(){
    	if(!isOffline()){
    		return this.visible_objects;
    	}
    	else{
    		return new HashMap<TraceableObject, Double>();
    	}
    }

    public int getNumVisibleObjects(){
    	if(!isOffline()){
    		return this.visible_objects.size();
    	}
    	else{
    		return 0;
    	}
    }

    public double getX() {
    	return this.x;
    }

    public double getY(){
    	return this.y;
    }

    public double getHeading(){
    	return this.heading;
    	
    }

    public double getRange() {
    	return this.range;
    	
    }

    public double getAngle() {
    	return this.viewing_angle;
    }

    @Override
    public Map<ITrObjectRepresentation,Double> getVisibleObjects_bb() {
    	
        Map<ITrObjectRepresentation,Double> result_list = new HashMap<ITrObjectRepresentation,Double>();
        if(!isOffline()){
	        for ( Map.Entry<TraceableObject, Double> e : this.visible_objects.entrySet()) {
	            TraceableObject key = e.getKey();
	            double confidence = e.getValue();
	
	            TraceableObjectRepresentation tor =
	                    new TraceableObjectRepresentation(key, key.getFeatures());
	
	            result_list.put(tor, confidence);
	        }
        }
        return result_list;
    }

    @Override
    public IMessage createMessage(String to, MessageType msgType, Object content) {
    	if(!isOffline()){
    		return new Message( this.name, to, msgType, content);
    	}
    	else{ 
    		return null;
    	}
    }
    
    public void resetCamera(){
    	this.neighbours = new ArrayList<CameraController>();
    	this.resources = new Resources();
    }

    public List<ICameraController> getNeighbours() {
    	
        List<ICameraController> lst = new LinkedList<ICameraController>();
        if(!isOffline()){
	        for ( int i = 0; i < this.neighbours.size(); i++ ){
	            lst.add( this.neighbours.get(i));
	        }
        }
        return lst;
    }

    private CameraController getNeighbourByName( String name ){
    	if(!isOffline()){
	        for ( int i = 0; i < this.neighbours.size(); i++ ){
	            if ( this.neighbours.get(i).getName().compareTo(name) == 0 ){
	                return this.neighbours.get(i);
	            }
	        }
    	}
        return null;
    }

    public IMessage sendMessage( String to, MessageType msgType, Object content){
    	if(DELAY_COMM > 0){
    		IMessage msg = this.createMessage(to, msgType, content);

    		try {
    			if(msgType == MessageType.StartSearch){
    				stats.addCommunication(1.0, this.getName());
    			} else if (msgType == MessageType.StartTracking) {
        			stats.addHandover(1.0);
        		}
			} catch (Exception e) {
				e.printStackTrace();
			}

    		sendOut.put(msg, DELAY_COMM);
    		return null;
    	}
    	else{
	    	String c = content.toString();
	    	try{
	    		TraceableObjectRepresentation tor = (TraceableObjectRepresentation) content;
	    		c = tor.getFeatures().toString();
	    	}
	    	catch(Exception ex){}
	    	
	    	if(!isOffline()){
		        CameraController cc = this.getNeighbourByName(to);
		        if ( cc == null ){
		            return this.createMessage(this.name, MessageType.ErrorBadDestinationAddress, null);
		        }
			    if(!cc.isOffline()){
			    	try {
			    	    if(msgType == MessageType.StartSearch){
			    	        stats.addCommunication(1.0, this.getName());
			    	    } else if (msgType == MessageType.StartTracking) {
			    			stats.addHandover(1.0);
			    		}
					} catch (Exception e) {
						e.printStackTrace();
					}
			        
			        IMessage msg = this.createMessage(to, msgType, content);
			        return cc.getAINode().receiveMessage(msg);
		        }
		        else{
		        	return this.createMessage(this.name, MessageType.ErrorBadDestinationAddress, null);
		        }
	    	}
	    	else{
	    		return null;
	    	}
    	}
    }
    

    protected IMessage forwardMessages() {
    	IMessage retVal = null;
    	List<IMessage> delete = new ArrayList<IMessage>();
		for (Map.Entry<IMessage, Integer> entry : sendOut.entrySet()) {
			int dur = entry.getValue();
			dur --;
			entry.setValue(dur);
			if(dur<= 0){
				CameraController cc = this.getNeighbourByName(entry.getKey().getTo());
				if(cc != null)
					if(!cc.isOffline())
						retVal = cc.getAINode().receiveMessage(entry.getKey());
				delete.add(entry.getKey());
			}
		}
		for (IMessage iMessage : delete) {
			sendOut.remove(iMessage);
		}
		
		return retVal;
	}


    public AbstractAINode getAINode() {
    	if(!isOffline()){
    		return this.camAINode;
    	}
    	else{
    		return null;
    	}
    }

    public Map<String,Double> getDrawableVisionGraph(){
    	if(!isOffline()){
    		return this.camAINode.getDrawableVisionGraph();
    	} else {
    		return new HashMap<String, Double>();
    	}
    }

    
//<camera ai_algorithm="lukas" heading="-180.0" name="Cam_01" range="20.0" viewing_angle="70.0" x="-15.0" y="10.0" comm="2"/>
    @Override
    public String toString(){
    	String retVal = "<camera ai_algorithm=\"";
    	retVal += getAINode().getClass().getCanonicalName() + "\" ";
    	retVal += "heading=\"" + Math.toDegrees(this.heading) + "\" name=\"" + this.name + "\" range=\""+ this.range + "\" viewing_angle=\"" + Math.toDegrees(this.viewing_angle) + "\" x=\"" + this.x +"\" y=\"" + this.y + "\" comm=\"0\"/>"; 
    	return retVal;
    }

	@Override
	public int getLimit() {
		if(!isOffline()){
			return LIMIT;
		}
		else{
			return -1;
		}
	}

	@Override
	public double getAllResources() {
		if(!isOffline()){
			return this.resources.getAllResources();
		}
		else{
			return -1;
		}
	}

	public int currentlyMissidentified() {
		if(!isOffline()){
			return this.camAINode.currentlyMissidentified();
		}
		else{
			return 0;
		}
	}

	public void setOffline(int sleepFor) {
		if(sleepFor == -1){
			sleepForever  = true;
		}
		else{
			isOfflineFor = sleepFor;
		}
	}

	public boolean isOffline() {
		if(sleepForever){
			return true;
		}
		else{
			if(isOfflineFor <= 0){
				return false;
			}
			else{
				return true;
			}
		}
	}

	@Override
	public void change(double xCoord, double yCoord, double head, double angle,
			double range) {
		
		if(head != -1d){
			this.heading = Math.toRadians(head);
		}
		if(xCoord != -1d)
			this.x = xCoord;
		if(yCoord != -1d)
			this.y = yCoord;
		if(angle != -1d)
			this.viewing_angle = Math.toRadians(angle);
		if(range != -1d)
			this.range = range;
		
	}
	
	@Override
	public int hashCode() {
		String s = toString()+" visibleObjects="+this.visible_objects.size() 
				+ " neighbours=" + this.neighbours.size();
		return s.hashCode();
	}

	@Override
	public void setAINode(AbstractAINode ai) {
		camAINode = ai;
	}

	public void update_confidence_real(int step, TraceableObject o) {
		int get = (int) Double.parseDouble(o.getFeatures().get(0).toString());
		ArrayList<Double> c = predefConf.get(get);
		double result_confidence = c.get(step);
		
		if (result_confidence > 0 ){
            this.addVisibleObject(o, result_confidence);
        }else{
            this.removeVisibleObject(o);
        }	
	}
	
	public boolean realObjectsUsed(){
		if(predefConf != null){
			return true;
		}
		else{
			return false;
		}
	}

	public void nextStep() {
		step++;
	}

	@Override
	public int objectIsVisible(ITrObjectRepresentation tor) {
		int get = (int) Double.parseDouble(tor.getFeatures().get(0).toString());
		try{
			if(predefVis != null){
				if(!predefVis.isEmpty()){
					ArrayList<Integer> v = predefVis.get(get);
					return v.get(step);
				}
				else{
					return -1;
				}
			}
			else{
				return -1;
			}
		}
		catch(IndexOutOfBoundsException e){
			return 1;
		}
	}
}
