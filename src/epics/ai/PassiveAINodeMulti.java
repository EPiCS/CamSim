package epics.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import epics.common.CmdLogger;
import epics.common.ICameraController;
import epics.common.IMessage;
import epics.common.IMessage.MessageType;
import epics.common.IRegistration;
import epics.common.ITrObjectRepresentation;

public class PassiveAINodeMulti extends ActiveAINodeMulti {

	private static final int DEFAULT_AUCTION_DURATION = 1;
	
	public PassiveAINodeMulti(int comm, boolean staticVG, Map<String, Double> vg, IRegistration r){
    	super(comm, staticVG, vg, r, DEFAULT_AUCTION_DURATION); // Goes through to instantiateAINode()
    }

    @Override
    protected Object handle_startTracking(String from,
            ITrObjectRepresentation content) {
    	
    	if(reg != null){
    		reg.objectTrackedBy(content, this.camController);
    	}
    	
    	if(!VISION_ON_BID){
	    	if(VISION_RCVER_BOUND || BIDIRECTIONAL_VISION){
	//    	if(!VISION_ON_FOUND) //strengthen link at handover
	    		strengthenVisionEdge(from);
	    	}
    	}
        this.startTracking(content);
        this.stopSearch(content);
        return null;
    }

    protected double handle_askConfidence(String from, ITrObjectRepresentation iTrObjectRepresentation) {

    	if(VISION_ON_BID && BIDIRECTIONAL_VISION){
    		strengthenVisionEdge(from);
    	}
    	
    	if(trackingPossible()){
	        Pair pair = findSimiliarObject(iTrObjectRepresentation);
	
	        if (pair == null) {
	        	
	            return 0;
	        }
	
	        ITrObjectRepresentation found = pair.itro;
	        if (DEBUG_CAM) {
	            CmdLogger.println(this.camController.getName() + "->" + from + ": My confidence for object " + found.getFeatures() + ": " + pair.confidence);
	        }
	        
	        addedObjectsInThisStep ++;
	        
	        return this.calculateValue(iTrObjectRepresentation);
	        
//	        return pair.confidence;

    	}
    	else{
    		return 0.0;
    	}

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

    	
//    	String output = this.camController.getName() + " traces objects [real name] (identified as): ";    	
//    	
////    	ITrObjectRepresentation realITO;
//    	for (Map.Entry<List<Double>, ITrObjectRepresentation> kvp : tracedObjects.entrySet()) {
//    		String wrong = "NONE";
//    		String real = "" + kvp.getValue().getFeatures();
//    		if(wrongIdentified.containsValue(kvp.getValue())){
//    			//kvp.getValue is not real... find real...
//    			for(Map.Entry<ITrObjectRepresentation, ITrObjectRepresentation> kvpWrong : wrongIdentified.entrySet()){
//    				if(kvpWrong.getValue().equals(kvp.getValue())){
//    					wrong = "" + kvp.getValue().getFeatures();
//    					real = "" + kvpWrong.getKey().getFeatures();
//    					break;
//    				}
//    				else{
//    					wrong = "ERROR";
//    				}
//    			}
//    		}
//			output = output + real + "(" + wrong + "); ";
//		}
//    	System.out.println(output);
    	
    	
//    	String searched = this.camController.getName() + " searches for: ";
//    	for(Map.Entry<ITrObjectRepresentation, ICameraController> entry : searchForTheseObjects.entrySet()){
//    		searched += entry.getKey().getFeatures() + "; ";
//    	}
//    	System.out.println(searched);
    	
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

    protected void checkBidsForObjects() {
    	 if (this.searchForTheseObjects.containsValue(this.camController)) { 
             List<ITrObjectRepresentation> delete = new ArrayList<ITrObjectRepresentation>(); 
             for (Map.Entry<ITrObjectRepresentation, ICameraController> entry : this.searchForTheseObjects.entrySet()) {
            	 if (entry.getValue() != null) { // object is searched for by camera
                	 
                	 if (entry.getValue().getName().equals(this.camController.getName())) {  //this camera initiated the auction --> own value is calculated
                		 
                		 //if auction has no duration or if auction is over
                    	 if((AUCTION_DURATION <= 0) || (runningAuction.containsKey(entry.getKey()) && (runningAuction.get(entry.getKey()) <= 0))){
	                         ITrObjectRepresentation tor = entry.getKey();
	                         double highest = 0;
	
	                         highest = this.getConfidence(entry.getKey());
	                         ICameraController giveTo = null;
	
	                         Map<ICameraController, Double> bids = this.getBiddingsFor(tor);
	
	                         if (bids != null) {
	                             for (Map.Entry<ICameraController, Double> e : bids.entrySet()) {
	                            	 if(!e.getKey().getName().equals("Offline")){
	                            		 
		                            	 if (e.getKey().getName().equals(this.camController.getName())) {
		                            		 if(giveTo == null){
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
	                             
	                             if (giveTo.getName().equals(this.camController.getName())) {
	                                 this.startTracking(tor);
	                                 sendMessage(MessageType.StopSearch, tor);
	                                 stepsTillBroadcast.remove(tor);
	                                 
	//                                 if(USE_MULTICAST_STEP){
	//                                	 multicast(MessageType.StopSearch, tor);
	//                                 }
	//                                 else{
	//                                	 broadcast(MessageType.StopSearch, tor);
	//                                 }
	                                 
	                             } else {
	                            	 IMessage reply = this.camController.sendMessage(giveTo.getName(), MessageType.StartTracking, tor);
	                            	 
	                                 if(reply != null){
	                                	 delete.remove(tor);
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
	                                     
	//                                	 if(USE_MULTICAST_STEP){
	//                                    	 multicast(MessageType.StopSearch, tor);
	//                                     }
	//                                     else{
	//                                    	 broadcast(MessageType.StopSearch, tor);
	//                                     }
	                                 }
	                             }
	                         }
	                         //runningAuction.remove(tor);
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
	
	public void checkConfidences() {
    	if (!this.getAllTracedObjects_bb().isEmpty()) {
            for (ITrObjectRepresentation io : this.getAllTracedObjects_bb().values()) {
            	double conf = 0.0;
            	double lastConf = 0.0;
            	if(wrongIdentified.containsValue(io)){
            		for(Map.Entry<ITrObjectRepresentation, ITrObjectRepresentation> kvp : wrongIdentified.entrySet()){
            			if (kvp.getValue().equals(io)){
            				conf = this.getConfidence(kvp.getKey());
            				lastConf = this.getLastConfidenceFor(kvp.getKey());
            			}
            		}	
            	}
            	else{
	                conf = this.getConfidence(io);
	                lastConf = this.getLastConfidenceFor(io);
            	}
                if (conf < 0.1 && conf < lastConf) {               	
                	callForHelp(io, 2);	
                }
                this.addLastConfidence(io, conf);
            }
        }
	}

	@Override
	public void printBiddings(){
    	for (Map.Entry<ITrObjectRepresentation, ICameraController> entry : this.searchForTheseObjects.entrySet()) {
    		ITrObjectRepresentation tor = entry.getKey();
    		Map<ICameraController, Double> bids = this.getBiddingsFor(tor);
    		if(bids!= null){
		    	String bidString = this.camController.getName() + " biddings for object " + tor.getFeatures() + ": ";
		    	if(!bids.isEmpty()){
			        for (Map.Entry<ICameraController, Double> e : bids.entrySet()) {
			            bidString += e.getKey().getName() + ": " + e.getValue() + "; ";
			        }
		    	}
		    	
		    	if(runningAuction.get(tor) == null){
		        	System.out.println("bid exists but no auction is running... how can that happen?? passive " + COMMUNICATION + " for object " + tor.getFeatures());
		        }
		    	
		    	bidString += " - AUCTION DURATION LEFT: " + runningAuction.get(tor);
		    	if(DEBUG_CAM)
		    		System.out.println(bidString);
    		}
    	}
    }
    
	@Override
    public void callForHelp(ITrObjectRepresentation io, int index) {
//    	if(wrongIdentified.containsKey(io)){
//    		io = wrongIdentified.get(io);
//    	}
    	
        if (DEBUG_CAM) {
            CmdLogger.println(this.camController.getName() + "->ALL: I'M LOOSING OBJECT ID:" + io.getFeatures() + "!! Can anyone take over? (my confidence: " + getConfidence(io)+ ", value: "+ calculateValue(io) +") index " + index );
        }
//        if(index == 0){
//        	if(DEBUG_CAM){
//        		System.out.println("################### BROADCASTING WHICH MIGHT HAVE BEEN ADVERTISED!! ");
//        	}
//        	addSearched(io, this.camController);
//        	broadcast(MessageType.StartSearch, io);
//        }
//        else{
	        this.addSearched(io, this.camController);
	        sendMessage(MessageType.StartSearch, io);
//        }
        if(reg != null){
        	reg.objectIsAdvertised(io);
        }
	}
    
    private void printVisionGraph(){
    	String neighs = "";
    	for (String neighbour : visionGraph.keySet()) {
			neighs = neighs + "; " + neighbour; 
		}
    	
    	System.out.println(this.camController.getName() + " has the following " + neighs);
    }

    @Override
    public double getUtility() {
        double utility = 0.0;
        double classifier_confidence = 1;
        double enabled = 1;  // 0/1 only
        double visibility = 0.0;
        double resources = MIN_RESOURCES_USED;
        for (ITrObjectRepresentation obj : this.getAllTracedObjects_bb().values()) {

        	visibility = this.getConfidence(obj);
//            utility += calculateValue(obj); 
        	resources = reservedResources.get(obj);
            utility += visibility * classifier_confidence * enabled * resources;
        }

        return utility;
    }
    
    @Override
	public double calculateValue(ITrObjectRepresentation target){
		double value = 0.0;
		double conf = this.getConfidence(target);
		double res = calcResources();
		double enabled = 1;  // 0/1 only
		
		double reservedRes = 0.0;
		for(Double res1 : reservedResources.values()){
			reservedRes +=res1;
		}
		
//		if(res != 0.0){
////			System.err.println(this.camController.getName() + " has reserved " + reservedRes + " and would like to reserve now: " + res);
//			this.reserveResources(target, res);
//		}
		
		value = conf * res * enabled;
		
		return value;
	}
}
