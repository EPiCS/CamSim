package epics.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import epics.camsim.core.Bid;
import epics.common.AbstractAINode;
import epics.common.CmdLogger;
import epics.common.IBid;
import epics.common.ICameraController;
import epics.common.IMessage;
import epics.common.IMessage.MessageType;
import epics.common.IRegistration;
import epics.common.ITrObjectRepresentation;
import epics.common.RandomNumberGenerator;

public class ActiveAINodeMulti extends AbstractAINode {
	
    public static final double USE_RESOURCES = 0.05; //percentages of available resources used
    public static final double MIN_RESOURCES_USED = 0.01; //how much resources have to be used at least
    public static final int DETECTIONRATE = 100;
    public static final int MISIDENTIFICATION = -1; //percentage of misidentified object. -1 = no misidentification
    public static final int STEPS_TILL_RESOURCES_FREED = 5;
    
    public static final double EVAPORATIONRATE = 0.995;
	
	public static final boolean DEBUG_CAM = true;
	public static final boolean VISION_ON_BID = false;
	public static final boolean VISION_RCVER_BOUND = false; //receiver builds up VG --> does not make much sense... 
	public static final boolean BIDIRECTIONAL_VISION = false;
	public static final boolean DECLINE_VISION_GRAPH = true;
    //protected static boolean USE_MULTICAST_STEP = false;
    //protected static boolean MULTICAST_SMOOTH = true;
	public static int COMMUNICATION = 0;
    
	public static final int STEPS_TILL_BROADCAST = 5;
	public static boolean USE_BROADCAST_AS_FAILSAVE = false;
    
	public static final int DELAY_COMMUNICATION = 0;
	public static final int DELAY_FOUND = 0;
	public final int AUCTION_DURATION;
    
    IRegistration reg;
    
    public ActiveAINodeMulti(int comm, boolean staticVG, 
    		Map<String, Double> vg, IRegistration r) {
    	super(comm, staticVG, vg, r); // Goes through to instantiateAINode()
    	AUCTION_DURATION = 0;
    }
    
    public ActiveAINodeMulti(int comm, boolean staticVG, 
    		Map<String, Double> vg, IRegistration r, int auctionDuration) {
    	super(comm, staticVG, vg, r); // Goes through to instantiateAINode()
    	AUCTION_DURATION = auctionDuration;
    }
    
    @Override
    public void instantiateAINode(int comm, boolean staticVG,
    		Map<String, Double> vg, IRegistration r) {

    	reg = r;
    	if(vg != null){
    		visionGraph = vg;
    	}
    	
    	this.staticVG = staticVG;
    	
//    	if(staticVG){
//    		COMMUNICATION = 3;
//    	}
//    	else{
    	if(comm == 3){
    		USE_BROADCAST_AS_FAILSAVE = false;
    	}
    	COMMUNICATION = comm;
    	
//	    	switch(comm){
//	    	case 0: USE_MULTICAST_STEP = false; break;
//	    	case 1: USE_MULTICAST_STEP = true; MULTICAST_SMOOTH = true; break;
//	    	case 2: USE_MULTICAST_STEP = true; MULTICAST_SMOOTH = false; break;
//	    	default: USE_MULTICAST_STEP = false; break;
//	    	}
//    	}
    }
    
    boolean staticVG = false;
    
    Map<String, Double> visionGraph = new HashMap<String, Double>();
    protected Map<ITrObjectRepresentation, Double> lastConfidence = new HashMap<ITrObjectRepresentation, Double>();
    protected Map<List<Double>, ITrObjectRepresentation> tracedObjects = new HashMap<List<Double>, ITrObjectRepresentation>();
    protected Map<ITrObjectRepresentation, ICameraController> searchForTheseObjects = new HashMap<ITrObjectRepresentation, ICameraController>();
    Map<ITrObjectRepresentation, Map<ICameraController, Double>> biddings = new HashMap<ITrObjectRepresentation, Map<ICameraController, Double>>();
    protected Map<ITrObjectRepresentation, List<String>> advertised = new HashMap<ITrObjectRepresentation, List<String>>();
    protected Map<ITrObjectRepresentation, Integer> runningAuction = new HashMap<ITrObjectRepresentation, Integer>();
    protected Map<ITrObjectRepresentation, Double> reservedResources = new HashMap<ITrObjectRepresentation, Double>();
    protected Map<ITrObjectRepresentation, Integer> stepsTillFreeResources = new HashMap<ITrObjectRepresentation, Integer>();
	protected Map<ITrObjectRepresentation, Integer> stepsTillBroadcast = new HashMap<ITrObjectRepresentation, Integer>();
    protected Map<ITrObjectRepresentation, ITrObjectRepresentation> wrongIdentified = new HashMap<ITrObjectRepresentation, ITrObjectRepresentation>();
    protected Map<IMessage, Integer> delayedCommunication = new HashMap<IMessage, Integer>();
    
    protected int addedObjectsInThisStep = 0;
 
    protected class Pair {
        ITrObjectRepresentation itro;
        double confidence;

        protected Pair(ITrObjectRepresentation itro, double confidence) {
            this.itro = itro;
            this.confidence = confidence;
        }
    }
    ICameraController camController;
    ITrObjectRepresentation trObject;
    double last_confidence = 0;

    @Deprecated
    @Override
    public ITrObjectRepresentation getTrackedObject() {
        return this.trObject;
    }

    @Override
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
        	return this.camController.createMessage(message.getFrom(), MessageType.ResponseToAskIfCanTrace, result);
        }
    }

    protected Object handle_startTracking(String from,
            ITrObjectRepresentation content) {
    	if(!VISION_ON_BID){
	    	if(VISION_RCVER_BOUND || BIDIRECTIONAL_VISION){
	//    	if(!VISION_ON_FOUND) //strengthen link at handover
	    		strengthenVisionEdge(from);
	    	}
    	}
    	if(DEBUG_CAM)
    		CmdLogger.println(this.camController.getName() + " received Start Tracking msg from: " + from + " for object " + content.getFeatures());
    	
        this.startTracking(content);
        this.stopSearch(content);
        return null;
    }

    protected void startTracking(ITrObjectRepresentation target) {
    	this.useResources(target);
        tracedObjects.put(target.getFeatures(), target);
    }

    protected Object handle_Found(String from, IBid content) {
//    	if(VISION_RCVER_BOUND || BIDIRECTIONAL_VISION){
    	if(VISION_ON_BID) {
    		strengthenVisionEdge(from);
    	}
        this.foundObject(content, from);
        return null;
    }

    protected void foundObject(IBid bid, String from) {
    	ITrObjectRepresentation target = bid.getTrObject();
    	double conf = bid.getBid();
//    	IMessage rst = this.camController.sendMessage(from, MessageType.AskConfidence, target);
//    	if((rst != null)&& (rst.getContent() != null)){
//    		double conf = (Double) rst.getContent(); //c.getVisibleObjects_bb().get(target);
    	
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
//    	}
    }

    protected Object handle_stopSearch(String from, ITrObjectRepresentation content) {
    	this.stopSearch(content);
        return null;
    }

    protected void stopSearch(ITrObjectRepresentation content) {
    	this.searchForTheseObjects.remove(content);
    }

    protected Object handle_startSearch(String from, ITrObjectRepresentation content) {
        this.searchFor(content, from);
        return null;
    }

    protected void searchFor(ITrObjectRepresentation content, String from) {
        if (from.equals("")) {
            searchForTheseObjects.put(content, null);
        } else {
            for (ICameraController cc : this.camController.getNeighbours()) {
                if (cc.getName().equals(from)) {
                    //if (!searchForTheseObjects.containsKey(content)) {
                        searchForTheseObjects.put(content, cc);
                   //}
                    break;
                }
            }
        }
    }

    protected boolean checkEquality(ITrObjectRepresentation trA, ITrObjectRepresentation iTrObjectRepresentation) {

        boolean result = true;
        if (trA == null) {
            result = false;
        } else if (iTrObjectRepresentation == null) {
            result = false;
        } else {

            List<Double> featuresSelf = trA.getFeatures();
            List<Double> featuresOther = iTrObjectRepresentation.getFeatures();

            assert (featuresSelf.size() == featuresOther.size());

            /*
             * TODO: Double comparison with ==. Will break as soon as we put
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

    protected double handle_askConfidence(String from, ITrObjectRepresentation iTrObjectRepresentation) {
    	if(VISION_ON_BID && BIDIRECTIONAL_VISION){
    		strengthenVisionEdge(from);
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
	        double test = pair.confidence;
	        return conf; //pair.confidence;//this.calculateValue(iTrObjectRepresentation);
    	}
    	else{
    		return 0.0;
    	}
    }

    protected boolean trackingPossible() {
    	if(this.camController.getLimit() == 0){
    		//check if enough resources
    		if(this.enoughResourcesForOneMore()){
    			return true;
    		}
    		else{
    			return false;
    		}
    	}
		if(this.camController.getLimit() > this.addedObjectsInThisStep + this.tracedObjects.size()){
			//check if enough resources
			if(this.enoughResourcesForOneMore()){
				return true;
			}
			else{
				return false;
			}
		}
		else{
			return false;
		}
	}

	protected Pair findSimiliarObject(ITrObjectRepresentation pattern) {
        Map<ITrObjectRepresentation, Double> traced_list = this.camController.getVisibleObjects_bb();

        for (Map.Entry<ITrObjectRepresentation, Double> e : traced_list.entrySet()) {
	    ITrObjectRepresentation key = e.getKey();
            boolean found = checkEquality(pattern, key);

            if (found) {
            	double confidence = e.getValue();
                return new Pair(key, confidence);
            }
        }

        return null;
    }

    @Override
    public void addVisibleObject(ITrObjectRepresentation rto) {

    }

    @Override
    public void removeVisibleObject(ITrObjectRepresentation rto) {
    	if(wrongIdentified.containsKey(rto)){
    		ITrObjectRepresentation original = rto;
    		ITrObjectRepresentation wrong = wrongIdentified.get(rto);
    		wrongIdentified.remove(original);
    		rto = wrong;
    	}
    	
    	if (this.isTraced(rto)) {
            this.removeTracedObject(rto);
            callForHelp(rto, 0);
        }
    }

    protected boolean isTraced(ITrObjectRepresentation rto) {
        if (this.tracedObjects.containsKey(rto.getFeatures())) {
            return true;
        } else {
            return false;
        }
    }

    protected void removeTracedObject(ITrObjectRepresentation rto) {
        tracedObjects.remove(rto.getFeatures());
        this.freeResources(rto);
    }

    protected void addSearched(ITrObjectRepresentation rto, ICameraController cam) {
    	this.searchForTheseObjects.put(rto, cam);
    }

    @Override
    public Map<String, Double> getVisionGraph() {
        return this.visionGraph;
    }

    @Override
    public void setController(ICameraController controller) {
        this.camController = controller;
    }

    @Override
    public void update() {
    	if(DECLINE_VISION_GRAPH)
    		this.updateVisionGraph();
    	
//    	double resRes =0;
//    	for(Double res : reservedResources.values()){
//			resRes +=res;
//		}
//    	double totRes = resRes + this.camController.getResources();
//    	System.out.println(this.camController.getName() + " resources reserved: " + resRes + " available: " + this.camController.getResources() + " total: " + totRes);

    	if(DEBUG_CAM) {
			String output = this.camController.getName();
			if(this.camController.isOffline()){
				output += " should be offline!! but";
			}
			output += " traces objects [real name] (identified as): ";    	
    	
			//    	ITrObjectRepresentation realITO;
			for (Map.Entry<List<Double>, ITrObjectRepresentation> kvp : tracedObjects.entrySet()) {
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
    	
    	updateReceivedDelay();
    	updateAuctionDuration();
    	
    	addedObjectsInThisStep = 0;
    	
        checkIfSearchedIsVisible();
        
        checkIfTracedGotLost();

        checkConfidences();
        
        printBiddings();
        
        checkBidsForObjects();       
        
        updateReservedResources();
        
        if(USE_BROADCAST_AS_FAILSAVE)
        	updateBroadcastCountdown();	
    }
    
    protected void updateReceivedDelay(){
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
    
    protected void updateAuctionDuration(){
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

	/** Looks at objects owned by this camera that are desired by other cameras
	 * and evaluates bids from other cameras. Gives the object to the highest 
	 * bidder (which can be itself). */
	protected void checkBidsForObjects() {
    	 if (this.searchForTheseObjects.containsValue(this.camController)) { //this camera is looking for an object --> is owner of at least one object that is searched for by the network
             List<ITrObjectRepresentation> delete = new ArrayList<ITrObjectRepresentation>(); 
             for (Map.Entry<ITrObjectRepresentation, ICameraController> entry : this.searchForTheseObjects.entrySet()) { 
                 if (entry.getValue() != null) { // object is searched for by camera
                	                  
                	 if (entry.getValue().getName().equals(this.camController.getName())) {  //this camera initiated the auction --> own value is calculated
                		 
                		 //TODO CHECK HERE IF AUCTION IS OVER ??
                    	 if((AUCTION_DURATION <= 0) || (runningAuction.containsKey(entry.getKey()) && (runningAuction.get(entry.getKey()) <= 0))){
	                         ITrObjectRepresentation tor = entry.getKey();
	                         double highest = 0;
	
	                         highest = this.calculateValue(entry.getKey()); //this.getConfidence(entry.getKey());
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
	                                 this.startTracking(tor);
	                                 sendMessage(MessageType.StopSearch, tor);
	                                 stepsTillBroadcast.remove(tor);
	                                 //runningAuction.remove(tor);
	//                                 if(USE_MULTICAST_STEP){
	//                                	 multicast(MessageType.StopSearch, tor);
	//                                 }
	//                                 else{
	//                                	 broadcast(MessageType.StopSearch, tor);
	//                                 }
	                             } else {
	                                 IMessage reply = this.camController.sendMessage(giveTo.getName(), MessageType.StartTracking, tor);
	                                 
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
		                                		 strengthenVisionEdge(giveTo.getName());
	                                	 }
	                                	 this.removeTracedObject(tor);
	                                	 
	                                	 List<String> cams = advertised.get(tor);
	                                	 if(cams != null){
		                                	 if(cams.contains(giveTo.getName())){
		                                    	 cams.remove(giveTo.getName());
		                                     }
	                                	 }
	                                	 
	                                	 sendMessage(MessageType.StopSearch, tor);
	                                	 stepsTillBroadcast.remove(tor);
	                                	 //runningAuction.remove(tor);
	                                	 
	//                                	 if(USE_MULTICAST_STEP){
	//                                    	 multicast(MessageType.StopSearch, tor);
	//                                     }
	//                                     else{
	//                                    	 broadcast(MessageType.StopSearch, tor);
	//                                     }
	                                 }
	                             }
	                         }
	                         //int map = runningAuction.remove(tor);
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

	protected void removeRunningAuction(ITrObjectRepresentation tor) {
		//int before = runningAuction.size();
		runningAuction.remove(tor);
		//int after = runningAuction.size();
		
		biddings.remove(tor);
	}

	protected void checkConfidences() {
    	if (!this.getAllTracedObjects_bb().isEmpty()) {
            for (ITrObjectRepresentation io : this.getAllTracedObjects_bb().values()) {
            	double conf = 0.0;
            	double lastConf = 0.0;
            	if(wrongIdentified.containsValue(io)){
            		for(Map.Entry<ITrObjectRepresentation, ITrObjectRepresentation> kvp : wrongIdentified.entrySet()){
            			if (kvp.getValue().equals(io)){
            				conf = this.calculateValue(kvp.getKey()); //this.getConfidence(kvp.getKey());
            				lastConf = this.getLastConfidenceFor(kvp.getKey());
            			}
            		}	
            	}
            	else{
	                conf = this.calculateValue(io); //this.getConfidence(io);
	                lastConf = this.getLastConfidenceFor(io);
            	}
            	
            	
               	callForHelp(io, 2);	
                this.addLastConfidence(io, conf);
            }
        }
	}

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
		        	System.out.println("bid exists but no auction is running... how can that happen?? active " + COMMUNICATION + " element " + tor.getFeatures());
		        }
		        
		        bidString += " -- AUCTION DURATION LEFT: " + runningAuction.get(tor);
		        if(DEBUG_CAM)
		        	System.out.println(bidString);
    		}
    	}
    }
    
    protected void callForHelp(ITrObjectRepresentation io, int index) {
    	if(wrongIdentified.containsKey(io)){
    		io = wrongIdentified.get(io);
    	}
    	
        if (DEBUG_CAM) {
            CmdLogger.println(this.camController.getName() + "->ALL: I'M LOSING OBJECT ID:" + io.getFeatures() + "!! Can anyone take over? (my confidence: " + getConfidence(io)+ ", value: "+ calculateValue(io) +") index " + index );
        }
        
//        if(index == 0){
//        	if(DEBUG_CAM){
//        		System.out.println("################### BROADCASTING WHICH MIGHT HAVE BEEN ADVERTISED!! ");
//        	}
//	        this.addSearched(io, this.camController);
//        	broadcast(MessageType.StartSearch, io);
//        }
//        else{
	        this.addSearched(io, this.camController);
	        sendMessage(MessageType.StartSearch, io);
//        }
        
        if(reg != null){
        	reg.objectIsAdvertised(io);
        }
        
//        if(USE_MULTICAST_STEP){
//        	multicast(MessageType.StartSearch, io);
//        }
//        else{
//        	broadcast(MessageType.StartSearch, io);
//        }
	}
	
	protected void checkIfTracedGotLost() {
		List<ITrObjectRepresentation> del = new ArrayList<ITrObjectRepresentation>();
		for(ITrObjectRepresentation itor : this.tracedObjects.values()){
			
			ITrObjectRepresentation mapped = itor;
			if(wrongIdentified.containsValue(itor)){
				for(Map.Entry<ITrObjectRepresentation, ITrObjectRepresentation> kvp : wrongIdentified.entrySet()){
					if(kvp.getValue().equals(itor)){
						mapped = kvp.getKey();
						break;
					}
				}
			}
			
			if(!this.camController.getVisibleObjects_bb().containsKey(mapped)){ //wrongIdentified.get(mapped))){
				callForHelp(mapped, 4);
				del.add(mapped);
			}
		}
		
		for(ITrObjectRepresentation tor : del){
			this.removeTracedObject(tor);
		}
	}
	
	protected void sendMessage(MessageType mt, Object o){
		switch(COMMUNICATION){
			case 0: broadcast(mt, o); break;
			case 1: multicastSmooth(mt, o); break;
			case 2: multicastStep(mt, o); break;
			case 3: multicastFix(mt, o); break;
		}
	}
	
	protected void multicastFix(MessageType mt, Object o){
		
		List<String> cams = new ArrayList<String>();
		for(String name : visionGraph.keySet()){
			this.camController.sendMessage(name, mt, o);
			cams.add(name);
		}
		
		if(mt == MessageType.StartSearch){
			advertised.put((ITrObjectRepresentation) o, cams);
		}
		
		if(mt == MessageType.StopSearch){
			advertised.remove((ITrObjectRepresentation) o);
		}
		
//		for(ICameraController icc : this.camController.getNeighbours()){
//			String name = icc.getName();
//			if(visionGraph.containsKey(name)){
//				this.camController.sendMessage(name, mt, o);
//			}
//		}
	}
	
	protected void multicastSmooth(MessageType mt, Object o){
		if(mt == MessageType.StartSearch){
			ITrObjectRepresentation io = (ITrObjectRepresentation) o;
			if(USE_BROADCAST_AS_FAILSAVE){
				if(!stepsTillBroadcast.containsKey(io)){
					stepsTillBroadcast.put(io, STEPS_TILL_BROADCAST);
				}
			}
			//get max strength
			double highest = 0;
			for(Double d : visionGraph.values()){
				if(d > highest){
					highest = d;
				}
			}
			
			if(highest > 0){
				double ran = RandomNumberGenerator.nextDouble();
				int sent = 0;
				for(ICameraController icc : this.camController.getNeighbours()){
					String name = icc.getName();
					double ratPart = 0.0;
					if(visionGraph.containsKey(name)){
						ratPart = visionGraph.get(name);
					}
					double ratio = (1+ratPart)/(1+highest);
					if(ratio > ran){
						sent ++;
		    			this.camController.sendMessage(name, mt, o);
		    			List<String> cams = advertised.get((ITrObjectRepresentation) o);
		    			if(cams != null){
		    				if(!cams.contains(name))
		    					cams.add(name);
		    			}
		    			else{
		    				cams = new ArrayList<String>();
		    				cams.add(name);
		    				advertised.put((ITrObjectRepresentation) o, cams);
		    			}
					}
				}
				if(sent == 0){
					if(DEBUG_CAM){
						System.out.println(this.camController.getName() + " tried to MC --> now BC");
					}
					broadcast(mt, o);
				}
			}
			else{
				if(DEBUG_CAM){
					System.out.println(this.camController.getName() + " tried to MC --> now BC 2");
				}
				broadcast(mt, o); 	
			}
		}
		else{
			if(mt == MessageType.StopSearch){
				if(advertised.isEmpty()){
		    		broadcast(mt, o);
		    	}
				else{
					if(advertised.get((ITrObjectRepresentation) o) != null){
						for (String name : advertised.get((ITrObjectRepresentation) o)){
							this.camController.sendMessage(name, mt, o);
						}
						advertised.remove(o); 
	    			}
				}
			}
		}
	}

	
	protected void multicastStep(MessageType mt, Object o){
		if(mt == MessageType.StartSearch){
			ITrObjectRepresentation io = (ITrObjectRepresentation) o;
			if(USE_BROADCAST_AS_FAILSAVE){
				if(!stepsTillBroadcast.containsKey(io)){
					stepsTillBroadcast.put(io, STEPS_TILL_BROADCAST);
				}
			}
			int sent = 0;
			double ran = RandomNumberGenerator.nextDouble();
			for(ICameraController icc : this.camController.getNeighbours()){
				String name = icc.getName();
				double prop = 0.1;
				if(visionGraph.containsKey(name)){
					prop = visionGraph.get(name);
				}
				if(prop > ran){
					sent ++;
	    			this.camController.sendMessage(name, mt, o);
	    			List<String> cams = advertised.get((ITrObjectRepresentation) o);
	    			if(cams != null){
	    				if(!cams.contains(name))
	    					cams.add(name);
	    			}
	    			else{
	    				cams = new ArrayList<String>();
	    				cams.add(name);
	    				advertised.put((ITrObjectRepresentation) o, cams);
	    			}
				}
			}
			
			if(sent == 0){
				if(DEBUG_CAM){
					System.out.println(this.camController.getName() + " tried to MC --> now BC");
				}
				broadcast(mt, o);
			}
		}
		else{
			if(mt == MessageType.StopSearch){
				if(advertised.isEmpty()){
		    		broadcast(mt, o);
		    	}
				else{
					if(advertised.get((ITrObjectRepresentation) o) != null){
						for (String name : advertised.get((ITrObjectRepresentation) o)){
							this.camController.sendMessage(name, mt, o);
						}
						advertised.remove(o); 
					}
				}
			}
		}
	}
	
	
	protected void broadcast(MessageType mt, Object o){
    	for (ICameraController icc : this.camController.getNeighbours()) {
            this.camController.sendMessage(icc.getName(), mt, o);
            if(mt == MessageType.StartSearch){
            	List<String> cams = advertised.get((ITrObjectRepresentation) o);
	            if(cams != null){
	            	if(!cams.contains(icc.getName()))
	            		cams.add(icc.getName());
				}
				else{
					cams = new ArrayList<String>();
					cams.add(icc.getName());
					advertised.put((ITrObjectRepresentation) o, cams);
				}
            }
//            else{
//            	if(mt == MessageType.StopSearch){
//            		advertised.remove((ITrObjectRepresentation)o);
////            		if(advertised.get((ITrObjectRepresentation) o) != null)
////            			advertised.get((ITrObjectRepresentation) o).remove(icc.getName());
//            	}
//            }
        }
    	if(mt == MessageType.StopSearch){
    		advertised.remove((ITrObjectRepresentation) o);
    	}
    }
   
	protected void removeFromBiddings(ITrObjectRepresentation o) {
        this.biddings.remove(o);
    }

	protected Map<ICameraController, Double> getBiddingsFor(ITrObjectRepresentation tor) {
        return this.biddings.get(tor);
    }

	protected void addLastConfidence(ITrObjectRepresentation io, double conf) {
        this.lastConfidence.put(io, conf);

    }

	protected double getLastConfidenceFor(ITrObjectRepresentation io) {
        if (lastConfidence.containsKey(io)) {
            return lastConfidence.get(io);
        } else {
            return 0.0;
        }
    }

	protected Map<List<Double>, ITrObjectRepresentation> getAllTracedObjects_bb() {
        return this.tracedObjects;
    }
    
    protected void checkIfSearchedIsVisible() {
    	ArrayList<ITrObjectRepresentation> found = new ArrayList<ITrObjectRepresentation>(); 

    	for (ITrObjectRepresentation visible : this.camController.getVisibleObjects_bb().keySet()) {
    		
    		if(wrongIdentified.containsKey(visible)){
    			visible = wrongIdentified.get(visible);
    		}
    		
    		if (!this.tracedObjects.containsKey(visible.getFeatures())) {
    			if(!wrongIdentified.containsValue(visible)){
		    		ITrObjectRepresentation wrong = visibleIsMissidentified(visible);
		    		if(wrong != null){ //missidentified 
		    			visible = wrong;
		    		}
	    		}
    		
    			if(this.searchForTheseObjects.containsKey(visible)){
	            
	            	ICameraController searcher = this.searchForTheseObjects.get(visible);
	            	
            		if (searcher != null) {
            			double conf = this.calculateValue(visible); //this.getConfidence(visible);
                        if (this.camController.getName().equals(searcher.getName())) {
                            this.addOwnBidFor(visible, conf);
                        } else {
                        	this.camController.sendMessage(searcher.getName(), MessageType.Found, new Bid(visible, conf));
                        }
                    } else {
                    	if(trackingPossible()){
	                    	this.startTracking(visible);
	                    	found.add(visible);
	                    	broadcast(MessageType.StopSearch, visible);
	                    	//sendMessage(MessageType.StopSearch, visible);
	                    	
//	                    	if(USE_MULTICAST_STEP){
//	                        	multicast(MessageType.StopSearch, visible);
//	                        }
//	                        else{
//	                        	broadcast(MessageType.StopSearch, visible);
//	                        }
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
    
//    private boolean visibleIsSearched(ITrObjectRepresentation visible) {
//		if(this.searchForTheseObjects.containsKey(visible)){
//			//visible is searched --> decide if identified correctly
//			if(this.foundObjectIsCorrect()){
//				return true;
//			}
//			else{
//				return false;
//			}
//		}
//		else{
//			return false;
//		}
//    }
		
	protected ITrObjectRepresentation visibleIsMissidentified(ITrObjectRepresentation visible){
		//object is not visible --> would send wrong bid!
		
		int random = RandomNumberGenerator.nextInt(100);
		if(random <= MISIDENTIFICATION){
			if(this.searchForTheseObjects.size() > 0){
				random = RandomNumberGenerator.nextInt(this.searchForTheseObjects.size());
				int x = 0;
				for (ITrObjectRepresentation tr : this.searchForTheseObjects.keySet()) {
					if(x == random){
						if(!tr.equals(visible)){
							if (DEBUG_CAM) {
					            CmdLogger.println(this.camController.getName() + " missidentified object " + visible.getFeatures() + " as " + tr.getFeatures());
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

//	private void printSearched(){
//    	String searchedString = this.camController.getName() + " has in its search-list: ";
//    	for(Map.Entry<ITrObjectRepresentation, ICameraController> searched : this.searchForTheseObjects.entrySet()){
//    		if(searched.getValue() != null){
//    			searchedString += searched.getKey().getFeatures() + " by " + searched.getValue().getName() + " ; ";
//    		}
//    		else{
//    			searchedString += searched.getKey().getFeatures() + " by GLOBAL; ";
//    		}
//    	}
//    	System.out.println(searchedString);
//    }
	
    protected void addOwnBidFor(ITrObjectRepresentation target, double conf) {
        Map<ICameraController, Double> bids = this.biddings.get(target);
        if (bids == null) {
            bids = new HashMap<ICameraController, Double>();
        }
        //TODO modified (removed ! befor runningAuction.containsKey) and added ELSE ! 23/05/2012 - takes over object, if noone took it
        if(runningAuction.containsKey(target)) {
        	double value = this.calculateValue(target);
        	//runningAuction.put(target, 0);
	        bids.put(this.camController, value);// conf);
	        biddings.put(target, bids);
    	} else {
        	if(!tracedObjects.containsKey(target)){
        		startTracking(target);
        		stopSearch(target);
        		sendMessage(MessageType.StopSearch, target);
           	 	stepsTillBroadcast.remove(target);
        	}
        }
    }
    

    protected void updateVisionGraph() {
    	if(!staticVG){
	    	ArrayList<String> toRemove = new ArrayList<String>();
	        for (Map.Entry<String, Double> e : visionGraph.entrySet()) {
	            double val = e.getValue();
	            e.setValue( e.getValue() * EVAPORATIONRATE); //0.995);
	            if (val < 0) {
	                toRemove.add(e.getKey());
	            }
	        }
	        for (int i = 0; i < toRemove.size(); i++) {
	            visionGraph.remove(toRemove.get(i));
	        }
    	}
    }

    @Override
    public void strengthenVisionEdge(String destinationName) {
    	if(!staticVG){
	        if (this.visionGraph.containsKey(destinationName)) {
	            double val = this.visionGraph.get(destinationName);
	            this.visionGraph.put(destinationName, val + 1);
	        } else {
	            this.visionGraph.put(destinationName, 1.0);
	        }
    	}
    }

    public double getConfidence(ITrObjectRepresentation target) {
        Pair pair = findSimiliarObject(target);
        if (pair == null) {
            return 0;
        } else {
            return pair.confidence;
        }
    }

    @Override
    public double getUtility() {
        double utility = 0.0;
        double resources = MIN_RESOURCES_USED;
        double enabled = 1;  // 0/1 only
        if (enabled == 1) {
            double visibility = 0.0;
            double classifier_confidence = 1;
	        for (ITrObjectRepresentation obj : this.getAllTracedObjects_bb().values()) {
	        	visibility = this.getConfidence(obj);
	//            utility += calculateValue(obj); 
	            utility += visibility * classifier_confidence * enabled;// * resources;
	        }
        }
        return utility;
    }

	@Override
	public Map<List<Double>, ITrObjectRepresentation> getTracedObjects() {
		
		//TODO make sure all traced objects are really existent within FoV --> if missidentified, send real anyway --> map first ;)
		
		Map<List<Double>, ITrObjectRepresentation> retVal = new HashMap<List<Double>, ITrObjectRepresentation>();
		for(Map.Entry<List<Double>, ITrObjectRepresentation> kvp : tracedObjects.entrySet()){
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
	
	@Override
	public Map<ITrObjectRepresentation, ICameraController> getSearchedObjects(){
		return this.searchForTheseObjects;
	}
	
	public int getComm(){
//		int retVal = 0;
//		
//		if(USE_MULTICAST_STEP){
//			if(MULTICAST_SMOOTH){
//				retVal = 1;
//			}
//			else{
//				retVal = 2;
//			}
//		}
//		else{
//			retVal = 0;
//		}
		
		return COMMUNICATION;
	}
	
	public double calculateValue(ITrObjectRepresentation target){
		double value = this.getConfidence(target); 
		double res = calcResources(); // Probably not necessary
		return value;
	}

	protected void reserveResources(ITrObjectRepresentation target, double resources) {
//		double res = calcResources();
		
		this.reservedResources.put(target, resources);
		this.stepsTillFreeResources.put(target, STEPS_TILL_RESOURCES_FREED);
	}
	
	protected boolean enoughResourcesForOneMore(){
		double res = calcResources();
		if(res > 0){
			return true;
		}
		else{
			return false;
		}
	}

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
	
	protected void freeResources(ITrObjectRepresentation target){
		if(this.reservedResources.containsKey(target)){
			double resRes = reservedResources.remove(target);//reservedResources.get(target);
			this.camController.addResources(resRes);
		}	
	}
	
	@Override
	public int currentlyMissidentified() {
		return this.wrongIdentified.size();
	}
}
