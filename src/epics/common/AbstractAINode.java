package epics.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import epics.camsim.core.Bid;
import epics.common.IMessage.MessageType;
import epics.commpolicy.Broadcast;
import epics.commpolicy.Fix;
import epics.commpolicy.Smooth;
import epics.commpolicy.Step;

/**
 * Abstract Class of AI - implements all application based methods of a camera.
 * @author Lukas Esterle & Marcin Bogdanski, refactored by Horatio Cane & Lukas Esterle
 */
/**
 * @author Lukas
 *
 */
public abstract class AbstractAINode {

    protected class Pair {
        public ITrObjectRepresentation itro;
        public double confidence;

        protected Pair(ITrObjectRepresentation itro, double confidence) {
            this.itro = itro;
            this.confidence = confidence;
        }
    }

    public static final boolean DEBUG_CAM = false;
    
	protected RandomNumberGenerator randomGen;
	
	public static final double USE_RESOURCES = 0.00005; //percentages of available resources used
    public static final double MIN_RESOURCES_USED = 0.000001; //how much resources have to be used at least
    public static final int STEPS_TILL_BROADCAST = 5;
    public static boolean USE_BROADCAST_AS_FAILSAVE = false;
    public static final int DELAY_COMMUNICATION = 0;
    public static final int DELAY_FOUND = 0;
    public static final int MISIDENTIFICATION = -1; //percentage of misidentified object. -1 = no misidentification
    
    public int AUCTION_DURATION;
    
    public static final int STEPS_TILL_RESOURCES_FREED = 5;
    public static final boolean DECLINE_VISION_GRAPH = true;
    public static final double EVAPORATIONRATE = 0.995;
    public static final boolean VISION_ON_BID = false;
    public static final boolean VISION_RCVER_BOUND = false; //receiver builds up VG --> does not make much sense... 
    public static final boolean BIDIRECTIONAL_VISION = false;
    
    boolean staticVG = false;
    private int communication;
	protected Map<String, Double> visionGraph = new HashMap<String, Double>();
    protected Map<List<Double>, ITrObjectRepresentation> trackedObjects = new HashMap<List<Double>, ITrObjectRepresentation>();
    protected Map<ITrObjectRepresentation, ICameraController> searchForTheseObjects = new HashMap<ITrObjectRepresentation, ICameraController>();
    protected Map<ITrObjectRepresentation, Map<ICameraController, Double>> biddings = new HashMap<ITrObjectRepresentation, Map<ICameraController, Double>>();
    protected Map<ITrObjectRepresentation, List<String>> advertised = new HashMap<ITrObjectRepresentation, List<String>>();
    protected Map<ITrObjectRepresentation, Integer> runningAuction = new HashMap<ITrObjectRepresentation, Integer>();
    protected Map<ITrObjectRepresentation, Double> reservedResources = new HashMap<ITrObjectRepresentation, Double>();
    protected Map<ITrObjectRepresentation, Integer> stepsTillFreeResources = new HashMap<ITrObjectRepresentation, Integer>();
	protected Map<ITrObjectRepresentation, Integer> stepsTillBroadcast = new HashMap<ITrObjectRepresentation, Integer>();
    protected Map<ITrObjectRepresentation, ITrObjectRepresentation> wrongIdentified = new HashMap<ITrObjectRepresentation, ITrObjectRepresentation>();
    protected Map<IMessage, Integer> delayedCommunication = new HashMap<IMessage, Integer>();
    protected ICameraController camController;
    protected ITrObjectRepresentation trObject;
    private Multicast multicast = null;
	protected int sentMessages;
	public IRegistration reg;
	
	protected double _receivedUtility;
	protected int _nrBids;
	protected double _paidUtility;
	protected int tmpTotalComm;
	protected double tmpTotalUtil;
	protected double tmpTotalRcvdPay;
	protected double tmpTotalPaid;
	protected int tmpTotalBids;
    
	protected IBanditSolver banditSolver;
    
    protected int addedObjectsInThisStep = 0;
	
	public AbstractAINode(AbstractAINode old){
		AUCTION_DURATION = 0;
		instantiateAINode(old);
	}

	/**
	 * Creates an AI Node WITHOUT bandit solver 
	 * for switching to another node automatically.
	 * This constructor simply calls instantiateAINode(). Overriding classes
     * should only call super and do real handling in instantiateAINode().
     * This is painful but is to enforce these arguments in the constructor. 
     * 
	 * @param comm communication used
	 * @param staticVG if true, only static vision graph is used
	 * @param vg initial vision graph
	 * @param r global registration component
	 * @param rg random number generator for this node
	 */
	public AbstractAINode(int comm, boolean staticVG, Map<String, Double> vg, IRegistration r, RandomNumberGenerator rg){
        AUCTION_DURATION = 0;
        reg = r;
        if(vg != null){
            visionGraph = vg;
        }
                
        if(comm == 3){
            USE_BROADCAST_AS_FAILSAVE = false;
        }
        randomGen = rg;
    }
	
    /**
     * Creates an AI Node WITHOUT bandit solver for switching to another node automatically
     * @param comm the used communication policy
     * @param staticVG if static vision graph or not
     * @param vg the initial vision graph
     * @param r the global registration component - can be null
     * @param auctionDuration the duration of auctions
     * @param rg the random number generator for this instance
     */
    public AbstractAINode(int comm, boolean staticVG, 
    		Map<String, Double> vg, IRegistration r, int auctionDuration, RandomNumberGenerator rg) {
    	this(comm, staticVG, vg, r, rg);
    	AUCTION_DURATION = auctionDuration;
    }
	
	/**
	 * Standard constructor including bandit solver
	 * @param comm communication used
     * @param staticVG if true, only static vision graph is used
     * @param vg initial vision graph
     * @param r global registration component
     * @param rg random number generator for this node
	 * @param bs the bandit solver to find the best strategy
	 */
	public AbstractAINode(int comm, boolean staticVG, Map<String, Double> vg,
			IRegistration r, RandomNumberGenerator rg, IBanditSolver bs){
		this(comm, staticVG, vg, r, rg);
		communication = comm;
		banditSolver = bs;
	}
	
	
	public AbstractAINode(int comm, boolean staticVG, 
			Map<String, Double> vg, IRegistration r, int auctionDuration, RandomNumberGenerator rg, IBanditSolver bs) {
		this(comm, staticVG, vg, r, rg, bs);
		AUCTION_DURATION = auctionDuration;
	}
	
    /**
     * copies the given abstractAINode
     * @param ai the given AiNode
     */
    public void instantiateAINode(AbstractAINode ai){
		this.trackedObjects = ai.trackedObjects;
		this.searchForTheseObjects = ai.searchForTheseObjects;
		this.biddings = ai.biddings;
		this.advertised = ai.advertised;
		this.runningAuction = ai.runningAuction;
		this.reservedResources = ai.reservedResources;
		this.stepsTillFreeResources = ai.stepsTillFreeResources;
		this.stepsTillBroadcast = ai.stepsTillBroadcast;
		this.wrongIdentified = ai.wrongIdentified;
		this.delayedCommunication = ai.delayedCommunication;
		this.trObject = ai.trObject;
		this.camController = ai.camController;
		this.visionGraph = ai.visionGraph;
		this.sentMessages = ai.sentMessages;
		this.randomGen = ai.randomGen;
		this._receivedUtility = ai._receivedUtility;
		this._nrBids = ai._nrBids;
		this._paidUtility = ai._paidUtility;
		this.tmpTotalComm = ai.tmpTotalComm;
		this.tmpTotalUtil = ai.tmpTotalUtil;
		this.tmpTotalRcvdPay = ai.tmpTotalRcvdPay;
		this.tmpTotalPaid = ai.tmpTotalPaid;
		this.tmpTotalBids = ai.tmpTotalBids;
		
		this.banditSolver = ai.banditSolver;
    }

    /**
     * adds a bid to a running auction for a certain object or creates a new auction if non exists
     * @param target the target object for which the bid is made for
     * @param conf the confidence for this object aka. the bid aka the value
     */
    protected void addOwnBidFor(ITrObjectRepresentation target, double conf) {
        Map<ICameraController, Double> bids = this.biddings.get(target);
        if (bids == null) {
            bids = new HashMap<ICameraController, Double>();
        }
        if(runningAuction.containsKey(target)) {
            double value = this.calculateValue(target);
            bids.put(this.camController, value);// conf);
            biddings.put(target, bids);
        } else {
            if(!trackedObjects.containsKey(target)){
                startTracking(target);
                stopSearch(target);
                sendMessage(MessageType.StopSearch, target);
                stepsTillBroadcast.remove(target);
            }
        }
    }

    /**
     * adds an object to the list of seached objects. also adds the camera which initiated the search
     * therefore, when the object is found, the 'ownin' camera can be contacted
     * @param rto the object to look for
     * @param cam the camera which initiated the search
     */
    protected void addSearched(ITrObjectRepresentation rto, ICameraController cam) {
        this.searchForTheseObjects.put(rto, cam);
    }

    /**
     * Calculates remaining resources on this camera
     * using a very simple resource consumption model
     * @return the remaining resources
     */
    protected double calcResources() {
        double allPossibleRes = this.camController.getAllResources();
        double reservedRes = 0.0;
        for(Double res : reservedResources.values()){
            reservedRes +=res;
        }
        
        double useRes = (allPossibleRes-reservedRes) * USE_RESOURCES;
        if(useRes < MIN_RESOURCES_USED){
            useRes = 0.0;
            if((allPossibleRes-reservedRes) >= MIN_RESOURCES_USED){
                useRes = MIN_RESOURCES_USED;
            }
        }
        return useRes;
    }

    /**
     * calculates the value of an object - currently just the confidence
     * @param target the object the values should be calculated for
     * @return the value for the given object
     */
    public double calculateValue(ITrObjectRepresentation target){
        double value = this.getConfidence(target); 
        return value;
    }
    
    /**
     * this message is used to notify other cameras to search for a given object. the object is advertised to other cameras, this camera is then waiting for incoming auctions but will keep track of the object as long as possible
     * @param io the object which this camera tries to find with the help of other cameras
     */
    public void callForHelp(ITrObjectRepresentation io) {
        if(wrongIdentified.containsKey(io)){
            io = wrongIdentified.get(io);
        }
        
        if (DEBUG_CAM) {
            CmdLogger.println(this.camController.getName() + "->ALL: I'M LOSING OBJECT ID:" + io.getFeatures() + "!! Can anyone take over? (my confidence: " + getConfidence(io)+ ", value: "+ calculateValue(io) +")" );
        }
        this.addSearched(io, this.camController);
        sendMessage(MessageType.StartSearch, io);

        if(reg != null){
            reg.objectIsAdvertised(io);
        }
    }

    /**
     * compares two objects for equality. returens true if they are the same
     * @param first first object to be compared
     * @param second second object to be compared
     * @return return true if objects are the same
     */
    protected boolean checkEquality(ITrObjectRepresentation first, ITrObjectRepresentation second) {

        boolean result = true;
        if (first == null) {
            result = false;
        } else if (second == null) {
            result = false;
        } else {

            List<Double> featuresSelf = first.getFeatures();
            List<Double> featuresOther = second.getFeatures();

            assert (featuresSelf.size() == featuresOther.size());

            /*
             * Double comparison with ==. Will break as soon as we put
             * real values inside. We need some acceptible epsilon error.
             */
            for (int i = 0; i < featuresSelf.size(); i++) {
                if (featuresSelf.get(i) != featuresOther.get(i)) {
                    result = false;
                    break;
                }
            }

        }

        return result;
    }

    
    /**
     * Checks if ANY searched object is visible. if so, sends a bid to the search-initiating camera. 
     */
    public void checkIfSearchedIsVisible() {
		addedObjectsInThisStep = 0;
        ArrayList<ITrObjectRepresentation> found = new ArrayList<ITrObjectRepresentation>(); 

        for (ITrObjectRepresentation visible : this.camController.getVisibleObjects_bb().keySet()) {
            
            if(wrongIdentified.containsKey(visible)){
                visible = wrongIdentified.get(visible);
            }
            
            if (!this.trackedObjects.containsKey(visible.getFeatures())) {
                if(!wrongIdentified.containsValue(visible)){
                    ITrObjectRepresentation wrong = visibleIsMisidentified(visible);
                    if(wrong != null){ //misidentified 
                        visible = wrong;
                    }
                }
            
                if(this.searchForTheseObjects.containsKey(visible)){
                
                    ICameraController searcher = this.searchForTheseObjects.get(visible);
                    
                    if (searcher != null) {
                        double bidValue = this.calculateValue(visible); //this.getConfidence(visible);
                        if (this.camController.getName().equals(searcher.getName())) {
                            this.addOwnBidFor(visible, bidValue);
                        } else {
                            this.camController.sendMessage(searcher.getName(), MessageType.Found, new Bid(visible, bidValue));
                        }
                    } else {
                        if(trackingPossible()){
                            this.startTracking(visible);
                            found.add(visible);
                            broadcast(MessageType.StopSearch, visible);
                            addedObjectsInThisStep++;
                        }
                    }
                }
            }
            
        }
        for(ITrObjectRepresentation foundElement : found){
            this.stopSearch(foundElement);
        }
    }

    /**
     * returns the number of currently misidentified objects
     */
    public int currentlyMisidentified() {
        return this.wrongIdentified.size();
    }
    
	/**
	 * checks if there are enough resources left on this camera to track another object
	 * @return true if there are enough resources left, false otherwise
	 */
	protected boolean enoughResourcesForOneMore(){
        double res = calcResources();
        if(res > 0){
            return true;
        }
        else{
            return false;
        }
    }
    
    /**
     * tries to find an object similar/equal to the given object
     * @param pattern the given object to find a similar one for
     * @return the pair-object containing both objects
     */
    protected Pair findSimiliarObject(ITrObjectRepresentation pattern) {
        Map<ITrObjectRepresentation, Double> tracked_list = this.camController.getVisibleObjects_bb();

        for (Map.Entry<ITrObjectRepresentation, Double> e : tracked_list.entrySet()) {
        ITrObjectRepresentation key = e.getKey();
            boolean found = checkEquality(pattern, key);

            if (found) {
                double confidence = e.getValue();
                return new Pair(key, confidence);
            }
        }

        return null;
    }
    
    /**
     * This method is called when an object has been found and a bid has been received from another camera
     * @param bid the bid from another camera
     * @param from the camera which sent this bid
     */
    protected void foundObject(IBid bid, String from) {
        ITrObjectRepresentation target = bid.getTrObject();
        double conf = bid.getBid();

        //if object is searched - if not searched, do not add to auctions
        for (ICameraController c : this.camController.getNeighbours()) {
        	if (c.getName().equals(from)) {
        		if(this.advertised.containsKey(target)){
        			Map<ICameraController, Double> bids = biddings.get(target);

        			if (bids == null) {
        				bids = new HashMap<ICameraController, Double>();
        			}
        			if(!runningAuction.containsKey(target)){
        				runningAuction.put(target, AUCTION_DURATION);
        			}
        			if(!bids.containsKey(c)){
        				bids.put(c, conf);
        				biddings.put(target, bids);
        			}
        		}
        	}
        }
    }
    
    /**
     * frees resources for a certain object
     * @param target the given object to free resources for
     */
    protected void freeResources(ITrObjectRepresentation target){
        if(this.reservedResources.containsKey(target)){
            double resRes = reservedResources.remove(target);//reservedResources.get(target);
            this.camController.addResources(resRes);
        }   
    }
    
    /**
     * returns all currently tracked objects
     * @return all tracked objects
     */
    protected Map<List<Double>, ITrObjectRepresentation> getAllTrackedObjects_bb() {
        return this.trackedObjects;
    }

    /**
     * returns the currently used bandit solver
     * @return the bandit solver
     */
    public IBanditSolver getBanditSolver() {
		return banditSolver;
	}
    
    /**
     * returns the bids already received for a given object
     * @param tor the object for which the bids are being returned
     * @return the already received bids
     */
    protected Map<ICameraController, Double> getBiddingsFor(ITrObjectRepresentation tor) {
        return this.biddings.get(tor);
    }
	
    /**
     * returns the currently used communication policy
     * @return the communication policy
     */
    public int getComm(){  
        return communication;
    }

    /**
     * returns the confidence for given object based on the similarty to another object (see {@link AbstractAINode#findSimiliarObject(ITrObjectRepresentation) findSimilarObject})
     * @param target the given object the confidence is searched for
     * @return the calculated confidence
     */
    public double getConfidence(ITrObjectRepresentation target) {
        Pair pair = findSimiliarObject(target);
        if (pair == null) {
            return 0;
        } else {
            return pair.confidence;
        }
    }

    /**
     * returns the number of bids received overall by this camera
     */
    public int getNrOfBids() {
        return _nrBids;
    }
    
    /**
     * returns the utility paid to other cameras for buying objects in the current step
     * @return this steps paid utility
     */
    public double getPaidUtility() {
        return _paidUtility;
    }
    
    /**
     * the utility received from other cameras for selling objects in this timestep
     */
    public double getReceivedUtility() {
        return _receivedUtility;
    }

	/**
	 * returns all currently searched objects in this camera and the corresponing cameras which initiated the search
	 * @return the objects searched in this cameras and the corresponding searching cameras
	 */
	public Map<ITrObjectRepresentation, ICameraController> getSearchedObjects(){
        return this.searchForTheseObjects;
    }
	
	/**
	 * get the number of sent messages in this timestep
	 */
	public int getSentMessages(){
		return sentMessages;
	}
	
	/** Adds one to the number of sent messages in this time step */
	public int incrementSentMessages() {
		sentMessages++;
		return sentMessages;
	}
	
	/**
	 * the number of total bids. AUTOMATICALLY SETS THEM TO ZERO AFTERWARDS!
	 * @return the bids since the last check of total bids
	 */
	public int getTmpTotalBids(){
        int tot = tmpTotalBids;
        tmpTotalBids = 0;
        return tot;
    }

	/**
	 * returns total communication since it was checked last time   AUTOMATICALLY SETS THEM TO ZERO AFTERWARDS!
	 * @return the sent messages since last check
	 */
	public int getTmpTotalComm(){
        int tot = tmpTotalComm;
        tmpTotalComm = 0;
        return tot;
    }
	
	/**
	 * returns the total paid utilities since the last time this method was called  AUTOMATICALLY SETS THEM TO ZERO AFTERWARDS!
	 * @return the total paid utilities since this method was called last
	 */
	public double getTmpTotalPaid(){
        double tot = tmpTotalPaid;
        tmpTotalPaid = 0.0;
        return tot;
    }
    
    /**
     * the total received utilities since this method was called last.  AUTOMATICALLY SETS THEM TO ZERO AFTERWARDS!
     * @return the received utilities since this method was called last
     */
    public double getTmpTotalRcvPay(){
        double tot = tmpTotalRcvdPay;
        tmpTotalRcvdPay = 0.0;
        return tot;
    }
    
   
    /**
     * the total utility generated since this method was called last.  AUTOMATICALLY SETS THEM TO ZERO AFTERWARDS! 
     * @return the total utilty generated since the method was called last
     */
    public double getTmpTotalUtility(){
        double tot = tmpTotalUtil;
        tmpTotalUtil = 0.0;
        return tot;
    }
    
    
    /**
     * returns a map of all currently tracked (owned) objects by this camera
     * @return returns all currently tracked objects by this camera
     */
    public Map<List<Double>, ITrObjectRepresentation> getTrackedObjects() {
        
        //make sure all tracked objects are really existent within FoV --> if misidentified, send real anyway --> map first ;)
        
        Map<List<Double>, ITrObjectRepresentation> retVal = new HashMap<List<Double>, ITrObjectRepresentation>();
        for(Map.Entry<List<Double>, ITrObjectRepresentation> kvp : trackedObjects.entrySet()){
            if(wrongIdentified.containsValue(kvp.getValue())){
                for(Map.Entry<ITrObjectRepresentation, ITrObjectRepresentation> wrongSet : wrongIdentified.entrySet()){
                    if(wrongSet.getValue().equals(kvp.getValue())){
                        retVal.put(wrongSet.getKey().getFeatures(), wrongSet.getKey());
                        break;
                    }
                }
            }
            else{
                retVal.put(kvp.getKey(), kvp.getValue());
            }
        }
        
        return retVal;
    }
    
    public abstract void advertiseTrackedObjects();
	
	/**
	 * Depricated - returned a single tracked object. the one object that has been added latest.
	 * @return latest added object
	 */
	@Deprecated
    public ITrObjectRepresentation getTrackedObject() {
        return this.trObject;
    }
	
	/**
	 * calculates the utility for all objects currently tracked by this camera
	 * utility_per_object = visibility + confidence + tracking_decision
	 * the visibility is the inverse euclidean distance, the confidence is currently alwas optimal (= 1) and the tracking_decision is either 0 or 1
	 * @return the utility for all tracked objects
	 */
    public double getUtility() {
        double utility = 0.0;
        @SuppressWarnings("unused")
        double resources = MIN_RESOURCES_USED;
        double enabled = 1;  // 0/1 only
        if (enabled == 1) {
            double visibility = 0.0;
            double classifier_confidence = 1;
            for (ITrObjectRepresentation obj : this.getAllTrackedObjects_bb().values()) {
                visibility = this.getConfidence(obj);
    //            utility += calculateValue(obj); 
                utility += visibility * classifier_confidence * enabled;// * resources;
            }
        }
        return utility;
    }
	
	/**
	 * returns the vision graph
	 * the graph consists of cameras (key) and the corresponding strength (value) in a map
	 * @return the vision graph
	 */
	public Map<String, Double> getVisionGraph() {
        return this.visionGraph;
    }
	
	/**
	 * handles the request for the confidence of a certain object
	 * depricated - because confidences are handled via message exchange now.
	 * @param from
	 * @param iTrObjectRepresentation
	 * @return
	 */
    protected double handle_askConfidence(String from, ITrObjectRepresentation iTrObjectRepresentation) {
        if(VISION_ON_BID && BIDIRECTIONAL_VISION){
            strengthenVisionEdge(from, iTrObjectRepresentation);
        }
        
        if(trackingPossible()){
            Pair pair = findSimiliarObject(iTrObjectRepresentation);
    
            if (pair == null) {
                return 0.0;
            }
            
            double conf = this.calculateValue(iTrObjectRepresentation);
    
            ITrObjectRepresentation found = pair.itro;
            if (DEBUG_CAM) {
                CmdLogger.println(this.camController.getName() + "->" + from + ": My confidence for object " + found.getFeatures() + ": " + conf);//pair.confidence);
            }
            
            addedObjectsInThisStep ++;
            return conf;
        }
        else{
            return 0.0;
        }
    }

    /**
     * informs a camera, the object has been found. if VISION_ON_BID = true the camera will strenghten the vision graph link to the given camera 
     * @param from the ID of the camera which has found an object in question
     * @param content the Bid for a object containing the object and the value
     * @return null
     */
    protected Object handle_Found(String from, IBid content) {
//      if(VISION_RCVER_BOUND || BIDIRECTIONAL_VISION){
        if(VISION_ON_BID) {
            strengthenVisionEdge(from, content.getTrObject());
        }
        this.foundObject(content, from);
        return null;
    }
	
	/**
	 * handles the start search message from a given camera. 
	 * @param from the camera initiating the search for a given object
	 * @param content the object to be searched for
	 * @return null
	 */
	protected Object handle_startSearch(String from, ITrObjectRepresentation content) {
        this.searchFor(content, from);
        return null;
    }
    
    /**
     * handles the start tracking message from a given camera
     * @param from the ID from the camera initiating the trackign command
     * @param content the object to be tracked
     * @return null
     */
    //@SuppressWarnings("unused")
    protected Object handle_startTracking(String from,
            ITrObjectRepresentation content) {
        if(!VISION_ON_BID){
            if(VISION_RCVER_BOUND || BIDIRECTIONAL_VISION){
                strengthenVisionEdge(from, content);
            }
        }
        if(DEBUG_CAM)
            CmdLogger.println(this.camController.getName() + " received Start Tracking msg from: " + from + " for object " + content.getFeatures());
        
        this.startTracking(content);
        this.stopSearch(content);
        return null;
    }
    
    /**
     * handles the stop search message initiated from a certain camera. this will stop this camera to search for the given object
     * @param from the camera initiating the stop-search call
     * @param content the object to stop search for
     * @return null
     */
    protected Object handle_stopSearch(String from, ITrObjectRepresentation content) {
        this.stopSearch(content);
        return null;
    }

    /**
     * decides if a given object is being tracked by this camera
     * @param rto the object in question if tracked or not
     * @return true if tracked, false otherwise
     */
    protected boolean isTracked(ITrObjectRepresentation rto) {
        return this.trackedObjects.containsKey(rto.getFeatures());
    }
    
    /** Returns the mapping from owned objects to the neighbouring cameras 
     * to which those objects are being advertised */
    public Map<ITrObjectRepresentation, List<String>> getAdvertisedObjects() {
    	return advertised;
    }
    
    /** Get the stepsTillBroadcast object */
    public Map<ITrObjectRepresentation, Integer> getStepsTillBroadcast() {
    	return stepsTillBroadcast;
    }
    
    /** Get the random number generator object for this AI node */
    public RandomNumberGenerator getRandomGen() {
    	return randomGen;
    }
    
    /**
     * Helper method - prints all current biddings
     */
    protected void printBiddings(){
        
        for (Map.Entry<ITrObjectRepresentation, ICameraController> entry : this.searchForTheseObjects.entrySet()) {
            ITrObjectRepresentation tor = entry.getKey();
            Map<ICameraController, Double> bids = this.getBiddingsFor(tor);
            if(bids != null){
                String bidString = this.camController.getName() + " biddings for object " + tor.getFeatures() + ": ";
                for (Map.Entry<ICameraController, Double> e : bids.entrySet()) {
                    if(!e.getKey().getName().equals("Offline")){
                        bidString += e.getKey().getName() + ": " + e.getValue() + "; ";
                    }
                }
                if(runningAuction.get(tor) == null){
                    System.out.println("bid exists but no auction is running... how can that happen?? active " + getComm() + " element " + tor.getFeatures());
                }
                
                bidString += " -- AUCTION DURATION LEFT: " + runningAuction.get(tor);
                if(DEBUG_CAM)
                    System.out.println(bidString);
            }
        }
    }
    
    /**
     * helper method - prints the current status of this camera
     */
    protected void printStatus() {
        String output = this.camController.getName();
        if(this.camController.isOffline()){
            output += " should be offline!! but";
        }
        output += " tracked objects [real name] (identified as): ";      
    
        //      ITrObjectRepresentation realITO;
        for (Map.Entry<List<Double>, ITrObjectRepresentation> kvp : trackedObjects.entrySet()) {
            String wrong = "NONE";
            String real = "" + kvp.getValue().getFeatures();
            if(wrongIdentified.containsValue(kvp.getValue())){
                //kvp.getValue is not real... find real...
                for(Map.Entry<ITrObjectRepresentation, ITrObjectRepresentation> kvpWrong : wrongIdentified.entrySet()){
                    if(kvpWrong.getValue().equals(kvp.getValue())){
                        wrong = "" + kvp.getValue().getFeatures();
                        real = "" + kvpWrong.getKey().getFeatures();
                        break;
                    }
                    else{
                        wrong = "ERROR";
                    }
                }
            }
            output = output + real + "(" + wrong + "); ";
        }
        System.out.println(output);
    }

    /**
	 * processes all messages. checks their status and calls the corresponding handle method
	 * @param message the received message
	 * @return an outgoing message
	 */
	public IMessage processMessage(IMessage message){
        Object result = null;

        switch (message.getType()) {
            case AskConfidence:
                result = handle_askConfidence(message.getFrom(), (ITrObjectRepresentation) message.getContent());
                break;
            case StartSearch:
                result = handle_startSearch(message.getFrom(), (ITrObjectRepresentation) message.getContent());
                break;
            case StopSearch:
                result = handle_stopSearch(message.getFrom(), (ITrObjectRepresentation) message.getContent());
                break;
            case Found:
                result = handle_Found(message.getFrom(), (IBid) message.getContent());
                break;
            case StartTracking:
                result = handle_startTracking(message.getFrom(), (ITrObjectRepresentation) message.getContent());
        }

        if (result == null) {
            return null;
        } else {
            return this.camController.createMessage(message.getFrom(), MessageType.ResponseToAskIfCanTrack, result);
        }
    }
	
	/**
	 * Receive method allows to introduce a delay between sending and actually receiving the message.
	 * the delay can be set by increasing the DELAY_COMMUNICATION value
	 * @param message the sent/received message 
	 * @return a reply-message
	 */
    public IMessage receiveMessage(IMessage message) {
        
        if(DELAY_COMMUNICATION > 0){
            switch(message.getType()){
            case AskConfidence: processMessage(message); break; //delayedCommunication.put(message, DELAY_COMMUNICATION); break;
            case StartSearch: delayedCommunication.put(message, DELAY_COMMUNICATION); break;
            case StopSearch: delayedCommunication.put(message,DELAY_COMMUNICATION); break;
            case Found: delayedCommunication.put(message, DELAY_COMMUNICATION+DELAY_FOUND); break;
            case StartTracking: delayedCommunication.put(message, DELAY_COMMUNICATION); break;
            default: return processMessage(message);
            }
        }
        else{
            return processMessage(message);
        }
        return null;
    }
	
	/**
	 * removes all bids for a given object 
	 * @param o the object to remove the bids for
	 */
	protected void removeFromBiddings(ITrObjectRepresentation o) {
        this.biddings.remove(o);
    }
	
	/**
	 * removes an entire auction for a given object. 
	 * @param tor the object the auction has to be removed for
	 */
	protected void removeRunningAuction(ITrObjectRepresentation tor) {
        //int before = runningAuction.size();
        runningAuction.remove(tor);
        //int after = runningAuction.size();
        
        biddings.remove(tor);
    }
	
	/**
	 * removes a currently tracked object and frees all reserved resources 
	 * @param rto the object to be removed from the tracked ones
	 */
	protected void removeTrackedObject(ITrObjectRepresentation rto) {
        trackedObjects.remove(rto.getFeatures());
        this.freeResources(rto);
    }

	
	/**
	 * removes a given object from the list of currently visible objects.
	 * in case the object is currently tracked, it is removed from tracked objects as well
	 * @param rto the object not visible anymore
	 */
	public void removeVisibleObject(ITrObjectRepresentation rto) {
        if(wrongIdentified.containsKey(rto)){
            ITrObjectRepresentation original = rto;
            ITrObjectRepresentation wrong = wrongIdentified.get(rto);
            wrongIdentified.remove(original);
            rto = wrong;
        }
    }
	
	
	/**
	 * reserves a certain amount of resources for a given object
	 * @param target the obeject to reserve resources for
	 * @param resources the amount of resources to reserve 
	 */
	protected void reserveResources(ITrObjectRepresentation target, double resources) {
        this.reservedResources.put(target, resources);
        this.stepsTillFreeResources.put(target, STEPS_TILL_RESOURCES_FREED);
    }
	
	/**
	 * starts to search for a certain object. keeps track which camera initiated the search.
	 * 
	 * this method is invoked when the camera receives the request to search for a specific object from another camera. 
	 * 
	 * @param content object to search for 
	 * @param from the camera which initiated the search
	 */
	protected void searchFor(ITrObjectRepresentation content, String from) {
        if (from.equals("")) {
            searchForTheseObjects.put(content, null);
        } else {
            for (ICameraController cc : this.camController.getNeighbours()) {
                if (cc.getName().equals(from)) {
                	searchForTheseObjects.put(content, cc);
                	break;
                }
            }
        }
    }
	
	/**
	 * sends a message to other cameras. the messagetype and the object the message is related to is needed.
	 * based on the selected communication, the message is either broadcasted, multicasted using smooth, step or fixed recipients. 
	 * the type of communciation can be set using {@link AbstractAINode#setComm(int) setComm}
	 * @param mt the type of message being sent
	 * @param o the object this message is related to
	 */
	protected void sendMessage(MessageType mt, Object o){
		if (multicast == null) {
			initialiseMulticast();
		}
		multicast.multicast(mt, o);
    }
		
    /**
     * broadcasts a given message to all other cameras in the network
     * @param mt the message to be sent to other cameras 
     * @param o the object this message relates to
     * @throws NullPointerException in case the object can not be casted correctly
     */
    protected void broadcast(MessageType mt, Object o) throws ClassCastException{
    	if (multicast == null) {
    		initialiseMulticast();
    	}
    	multicast.broadcast(mt, o);
    }
	
    /** 
     * Initialises the multicast object used for communication within 
     * this node. This will create a new object regardless of whether
     * one already exists.
     */
    protected void initialiseMulticast() {
    	switch(this.communication){
			case 0: multicast = new Broadcast(this, this.camController); break;
			case 1: multicast = new Smooth(this, this.camController); break;
			case 2: multicast = new Step(this, this.camController); break;
			case 3: multicast = new Fix(this, this.camController); break;
    	}
    	if (DEBUG_CAM) { 
    		System.out.println("Created new multicast object");
    	}
    }
    
	/**
	 * sets the communication policy:
	 * 0 = broadcast
	 * 1 = multicast smooth
	 * 2 = multicast step
	 * 3 = multicast fix 
	 * @param com the communication policy
	 */
	public void setComm(int com) {
        communication = com;
        initialiseMulticast();
    }
	
	/**
	 * sets the actual camera for this AI for calling controller functions
	 * @param controller to be set
	 */
	public void setController(ICameraController controller) {
        this.camController = controller;
    }

    /**
     * start tracking reserves resources for the given target, pays the utility price and starts to track the object
     * @param target the object to start tracking
     */
    protected void startTracking(ITrObjectRepresentation target) {
        this.useResources(target);
        _paidUtility = target.getPrice();
        
        trackedObjects.put(target.getFeatures(), target);
    }

    /**
     * stops to search the given object. this should happen when the object has been found in another camera
     * @param content the object to NOT search anymore
     */
    protected void stopSearch(ITrObjectRepresentation content) {
        this.searchForTheseObjects.remove(content);
    }

	/** Called when communication is made with the given camera about the 
     * given object, in order to strengthen the pheromone link.
     * @param destinationName the name of the remote camera and link to be strengthened
     */
    public void strengthenVisionEdge(String destinationName, ITrObjectRepresentation itro) {
    	if(!staticVG){
    		double val;
	        if (vgContainsKey(destinationName, itro)) {
	            val = vgGet(destinationName, itro);
	            val = val + 1.0;
	        } else {
	            val = 1.0;
	        }
	        vgPut(destinationName, itro, val);
    	}
    }
	
	/**
	 * decides if another object can be tracked
	 * @return true if another object can be tracked
	 */
	protected boolean trackingPossible() {
        if(this.camController.getLimit() == 0){
            //check if enough resources
            return this.enoughResourcesForOneMore();
        }
        if(this.camController.getLimit() > this.addedObjectsInThisStep + this.trackedObjects.size()){
            //check if enough resources
            return this.enoughResourcesForOneMore();
        }
        else{
            return false;
        }
    }

	/**
	 * Called in every timestep
	 * 
	 * the following is handled:
	 *     the delay for receiving messages is updated
	 *     the left duration of auctions is updated
	 *     checks if a searched object is now visible
	 *     checks if a tracked object got lost
	 *     updates all confidences
	 *     checks if any new biddings have arrived for any objects, or if an auction has ended
	 *     updates the reserved resources
	 * 
	 * also, all temporary variables (received payments, paid payments and so on) are reset.
	 */
    public void update() {
        sentMessages = 0;
        _nrBids = 0;
        _receivedUtility = 0.0;
        _paidUtility = 0.0;
        if(DECLINE_VISION_GRAPH)
            this.updateVisionGraph();

        if(DEBUG_CAM) {
            printStatus();
        }
        
        printBiddings();
        
        for (Map.Entry<ITrObjectRepresentation, Map<ICameraController, Double>> bids : biddings.entrySet()) {
            _nrBids += bids.getValue().size();
        }
        
        checkBidsForObjects();       
        
        updateReservedResources();
        
        if(USE_BROADCAST_AS_FAILSAVE)
            updateBroadcastCountdown(); 
        
        updateTotalUtilComm();
    }
    
    /**
     * Checks if there are bids for any objects owned by this camera and if 
     * the corresponding auctions have already ended or if they are still running.
     * If the auction has ended, the winner is selected, the needed payments 
     * have to be announced. The winner is notified, this camera stops tracking.
     * All non-winning cameras are notified to stop searching for the given object.
     */
    protected void checkBidsForObjects() {
   	 if (this.searchForTheseObjects.containsValue(this.camController)) { //this camera is looking for an object --> is owner of at least one object that is searched for by the network
            List<ITrObjectRepresentation> delete = new ArrayList<ITrObjectRepresentation>(); 
            for (Map.Entry<ITrObjectRepresentation, ICameraController> entry : this.searchForTheseObjects.entrySet()) { 
                if (entry.getValue() != null) { // object is searched for by camera
               	                  
               	 if (entry.getValue().getName().equals(this.camController.getName())) {  //this camera initiated the auction --> own value is calculated
               		 
               		 //CHECK HERE IF AUCTION IS OVER 
                   	 if((AUCTION_DURATION <= 0) || (runningAuction.containsKey(entry.getKey()) && (runningAuction.get(entry.getKey()) <= 0))){
	                         ITrObjectRepresentation tor = entry.getKey();
	                         double highest = 0;
	                         double secondHighest = 0;
	                         highest = this.calculateValue(entry.getKey()); //this.getConfidence(entry.getKey());
	                         secondHighest = highest;
	                         ICameraController giveTo = null;
	
	                         Map<ICameraController, Double> bids = this.getBiddingsFor(tor); // Bids from other cams
	                         
	                         if (bids != null) {
	                             for (Map.Entry<ICameraController, Double> e : bids.entrySet()) { // iterate over bids
	                            	 if(!e.getKey().getName().equals("Offline")){
	                            		 
		                            	 if (e.getKey().getName().equals(this.camController.getName())) {
		                            		 if(giveTo == null){
		                            			 // If this cam's bid is still highest, claim object for now
		                            		     giveTo = e.getKey();
		                                     }
		                                 }
		                                 if (e.getValue() > highest) {
		                                	 secondHighest = highest;
		                                     highest = e.getValue();
		                                     giveTo = e.getKey();
		                                 }
	                            	 }
	                             }
	                         }
	
	                         if (giveTo != null) {
	                             delete.add(tor);
	                             
	                             // If this cam won auction
	                             if (giveTo.getName().equals(this.camController.getName())) { 
	                            	 tor.setPrice(secondHighest);
	                                 this.startTracking(tor);
	                                 _receivedUtility += secondHighest;
	                                 //_paidUtility += secondHighest;
	                                 sendMessage(MessageType.StopSearch, tor);
	                                 stepsTillBroadcast.remove(tor);
	                             } else {
	                            	 tor.setPrice(secondHighest);
	                                 IMessage reply = this.camController.sendMessage(giveTo.getName(), MessageType.StartTracking, tor);
	                                 _receivedUtility += secondHighest; //highest;
	                                 if(DEBUG_CAM)
	                             		CmdLogger.println(this.camController.getName() + " sent StartTracking msg to: " + giveTo.getName() + " for object " + tor.getFeatures());
	                                 
	                                 if(reply != null){
	                                	 if(reg != null){
	                                		 reg.objectTrackedBy(tor, this.camController);
	                                	 }
	                                	 
	                                	 delete.remove(tor); // do not delete if other camera does not respond (or responds irrationally)
	                                	 this.getBiddingsFor(tor).remove(giveTo);
	                                 }
	                                 else{
	                                	 if(!VISION_ON_BID){
		                                	 if(BIDIRECTIONAL_VISION || (!VISION_RCVER_BOUND))
		                                		 strengthenVisionEdge(giveTo.getName(), tor);
	                                	 }
	                                	 this.removeTrackedObject(tor);
	                                	 
	                                	 List<String> cams = advertised.get(tor);
	                                	 if(cams != null){
		                                	 if(cams.contains(giveTo.getName())){
		                                    	 cams.remove(giveTo.getName());
		                                     }
	                                	 }
	                                	 
	                                	 sendMessage(MessageType.StopSearch, tor);
	                                	 stepsTillBroadcast.remove(tor);
	                                 }
	                             }
	                         }
	                         removeRunningAuction(tor); 
                   	 }
                    }
                }
            }
        
            for (ITrObjectRepresentation o : delete) {
                this.removeFromBiddings(o); 
                this.stopSearch(o);
            }
        }
	}

	/**
	 * reduces the duration of all auctions
	 * needed to identify when auctions have ended.
	 */
	public void updateAuctionDuration(){
        if(AUCTION_DURATION > 0){
            for(Map.Entry<ITrObjectRepresentation, Integer> entry : runningAuction.entrySet()){
                int dur = entry.getValue();
                dur--;
                entry.setValue(dur);
                if(dur < 0){
                    System.out.println("------------------------- negative duration!");
                }
            }
        }
    }
	
	/**
	 * reduces the broadcast countdown for all objects
	 * if the coundown reaches 0, the start search message is broadcasted. this is only important if multicast smooth or step are being used
	 * and should not be used at all when fixd communication is used.
	 */
	protected void updateBroadcastCountdown(){
        List<ITrObjectRepresentation> bc = new ArrayList<ITrObjectRepresentation>();
        for (Map.Entry<ITrObjectRepresentation, Integer> kvp : stepsTillBroadcast.entrySet()) {
            int i = kvp.getValue();
            i--;
            if(i <= 0){
                bc.add(kvp.getKey());
            }
            else{
                stepsTillBroadcast.put(kvp.getKey(), i);
            }
        }
        
        for (ITrObjectRepresentation iTrObjectRepresentation : bc) {
            broadcast(MessageType.StartSearch, iTrObjectRepresentation);
        }
    }
	
		
    /**
     * reduces the delay counter for receiving messages.
     * if the counter for a messages arrives at 0, the message is received
     * by the receiver
     */
    public void updateReceivedDelay(){
        List<IMessage> rem = new ArrayList<IMessage>();
        for (Map.Entry<IMessage, Integer> entry : delayedCommunication.entrySet()) {
            int dur = entry.getValue();
            dur--;
            entry.setValue(dur);
            
            if(dur<= 0){
                rem.add(entry.getKey());
                processMessage(entry.getKey());
            }
        }
        
        for (int i = 0; i < rem.size(); i++) {
            delayedCommunication.remove(rem.get(i));
        }
    }
	
	/**
	 * updates the reserved resources. when a camera searches for an object, it reserves
	 * resources for this object to be tracked. resources are only reserved for a certain time, afterwards they will be freed again.
	 * if an object is being found after the resources have been freed, the resources will be allocated again, if there are enough of them.
	 */
	protected void updateReservedResources() {
        List<ITrObjectRepresentation> del = new ArrayList<ITrObjectRepresentation>();
        for(Map.Entry<ITrObjectRepresentation, Integer> entry : stepsTillFreeResources.entrySet()){
            int steps = entry.getValue();
            if(steps-1 == 0){
                del.add(entry.getKey());
            }
            else{
                stepsTillFreeResources.put(entry.getKey(), steps-1);
            }
        }
        
        for(ITrObjectRepresentation itr : del){
            stepsTillFreeResources.remove(itr);
        }
    }
	
	/**
	 * updates the all payments made and received, the number of overall messages sent and the number of bids made
	 */
	protected void updateTotalUtilComm() {
        this.tmpTotalComm += getComm();
        this.tmpTotalUtil += getUtility();
        this.tmpTotalRcvdPay += getReceivedUtility();
        this.tmpTotalPaid += getPaidUtility();
        this.tmpTotalBids += getNrOfBids();
    }

	/** Get a mapping between camera names and values for the pheromone links
	 * between the AI node and those cameras. This is used for drawing the 
	 * vision graph.
	 * Here, it's the same as the actual vision graph */
    public Map<String, Double> getDrawableVisionGraph() {
        return getVisionGraph();
    }
    
    /**
     * updates the vision graph. reduces every link by the evaporationrate
     */
    protected void updateVisionGraph() {
        if(!staticVG){
            ArrayList<String> toRemove = new ArrayList<String>();
            for (Map.Entry<String, Double> e : vgEntrySet()) {
                double val = e.getValue();
                e.setValue( e.getValue() * EVAPORATIONRATE); //0.995);
                if (val < 0) {
                    toRemove.add(e.getKey());
                }
            }
            for (int i = 0; i < toRemove.size(); i++) {
                vgRemove(toRemove.get(i));
            }
        }
    }
    
    /** Whether the key exists for this cam name (ignoring object here) */
    public boolean vgContainsKey(String camName, ITrObjectRepresentation itro) { 
    	return getVisionGraph().containsKey(camName);
    }
    
	/** Get all values in the vision graph (ignoring object here) */
    public Collection<Double> vgGetValues(ITrObjectRepresentation itro) {
    	return getVisionGraph().values();
    }
    
	/** Get all cameras with values in the vision graph */
    public Set<String> vgGetCamSet() {
    	return getVisionGraph().keySet();
    }
    
	/** Get the pheromone value for this camera name (ignoring object here) */
    public Double vgGet(String camName, ITrObjectRepresentation itro) {
    	return getVisionGraph().get(camName);
    }
    
    /** Put a value in the vision graph under this camera name (ignoring 
     * object here) */
    public Double vgPut(String camName, ITrObjectRepresentation itro, Double value) {
    	return getVisionGraph().put(camName, value);
    }
    
    /** Get all entries (key-value pairs) in the vision graph */
    public Set<Map.Entry<String, Double>> vgEntrySet() {
    	return getVisionGraph().entrySet();
    }
    
    /** Remove from the vision graph the key-value pair for the given key */
    public Double vgRemove(String name) {
    	return getVisionGraph().remove(name);
    }
    
    /**
     * helper class to print a human readable version of the vision graph.
     */
    private void printVisionGraph(){
    	String neighbours = "";
    	for (String neighbour : visionGraph.keySet()) {
			neighbours += "; " + neighbour; 
		}
    	System.out.println(this.camController.getName() + 
    			" has the following neighbours:" + neighbours);
	}
    
	/** For specifying params of an AI node after construction time. 
	 * For example, setting a 'debug' field to true. This method should handle
	 * strings for keys and convert the value string to the appropriate type.
	 * This method should return whether the param was successfully applied. */
	public boolean setParam(String key, String value) {
		return false; // No params settable for AbstractAINode yet
	}
    
    /**
     * allocates and uses resources. removes the counter for reserved resources
     * @param target the object the resources are reserved for
     */
    protected void useResources(ITrObjectRepresentation target){
        double resRes = MIN_RESOURCES_USED; //0.0;
        if(reservedResources.containsKey(target)){
            resRes = reservedResources.get(target);
        }
        else{
            resRes = this.calcResources();
            this.reserveResources(target, resRes);
        }
        this.camController.reduceResources(resRes);
        stepsTillFreeResources.remove(target);
    }
	
	/**
	 * decides if a visible object has been misidentified as a different object.
	 * 
	 * @param visible the object which might have been misidentified
	 * @return the object as which given object has been identified.
	 */
	protected ITrObjectRepresentation visibleIsMisidentified(ITrObjectRepresentation visible){
        //object is not visible --> would send wrong bid!
        
        int random = randomGen.nextInt(100, RandomUse.USE.FALSEOBJ);
        if(random <= MISIDENTIFICATION){
            if(this.searchForTheseObjects.size() > 0){
                random = randomGen.nextInt(this.searchForTheseObjects.size(), RandomUse.USE.FALSEOBJ);
                int x = 0;
                for (ITrObjectRepresentation tr : this.searchForTheseObjects.keySet()) {
                    if(x == random){
                        if(!tr.equals(visible)){
                            if (DEBUG_CAM) {
                                CmdLogger.println(this.camController.getName() + " misidentified object " + visible.getFeatures() + " as " + tr.getFeatures());
                            }
                            wrongIdentified.put(visible, tr);
                            return tr;
                        }
                        else{
                            return null;
                        }
                    }
                    x++;
                }
            }
            return null;
        }
        else{
            return null;
        }
    }
	
	/** Returns the name of the underlying CameraController object */
	public String getName() {
		return this.camController.getName();
	}
}
