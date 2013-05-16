package epics.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import epics.common.AbstractAINode;
import epics.common.CmdLogger;
import epics.common.IBanditSolver;
import epics.common.ICameraController;
import epics.common.IMessage;
import epics.common.IMessage.MessageType;
import epics.common.IRegistration;
import epics.common.ITrObjectRepresentation;
import epics.common.RandomNumberGenerator;

public class PassiveAINodeMulti extends AbstractAINode { //ActiveAINodeMulti {

	private static final int DEFAULT_AUCTION_DURATION = 0;
	
	public PassiveAINodeMulti(int comm, boolean staticVG, Map<String, Double> vg, IRegistration r, RandomNumberGenerator rg){
    	super(comm, staticVG, vg, r, rg); // Goes through to instantiateAINode()
    	AUCTION_DURATION = DEFAULT_AUCTION_DURATION;
    }
	
	public PassiveAINodeMulti(int comm, boolean staticVG, Map<String, Double> vg, IRegistration r, RandomNumberGenerator rg, IBanditSolver bs){
    	super(comm, staticVG, vg, r, rg, bs); // Goes through to instantiateAINode()
    	AUCTION_DURATION = DEFAULT_AUCTION_DURATION;
    }

	public PassiveAINodeMulti(AbstractAINode ai){
		super(ai);
	}
	
	@Override
    public void instantiateAINode(int comm, boolean staticVG,
            Map<String, Double> vg, IRegistration r, RandomNumberGenerator rg) {
        reg = r;
        if(vg != null){
            visionGraph = vg;
        }
                
        if(comm == 3){
            USE_BROADCAST_AS_FAILSAVE = false;
        }
        randomGen = rg;
    }
    
	
    @Override
    public void update() {
    	sentMessages = 0;
    	_nrBids = 0;
    	_receivedUtility = 0.0;
    	_paidUtility = 0.0;
    	if(DECLINE_VISION_GRAPH)
    		this.updateVisionGraph();
    	
    	
    	updateReceivedDelay();
    	updateAuctionDuration();
    	
    	addedObjectsInThisStep = 0;
    	
        checkIfSearchedIsVisible();
        
        checkIfTracedGotLost();

        checkConfidences();
        
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
	                         double secondHighest = 0;
	                         highest = this.getConfidence(entry.getKey());
	                         secondHighest = highest;
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
		                                	 secondHighest = highest;
		                                     highest = e.getValue();
		                                     giveTo = e.getKey();
		                                 }
	                            	 }
	                             }
	                         }
	
	                         if (giveTo != null) {
	                             delete.add(tor);
	                             
	                             if (giveTo.getName().equals(this.camController.getName())) {
	                            	 tor.setPrice(secondHighest);
	                                 this.startTracking(tor);
	                                 _receivedUtility += secondHighest;
	                                 sendMessage(MessageType.StopSearch, tor);
	                                 stepsTillBroadcast.remove(tor);
	                                 
	//                                 if(USE_MULTICAST_STEP){
	//                                	 multicast(MessageType.StopSearch, tor);
	//                                 }
	//                                 else{
	//                                	 broadcast(MessageType.StopSearch, tor);
	//                                 }
	                                 
	                             } else {
	                            	 tor.setPrice(secondHighest);
	                                 IMessage reply = this.camController.sendMessage(giveTo.getName(), MessageType.StartTracking, tor);
	                                 _receivedUtility += secondHighest;
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
    
	@Override
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
            	
            	if(this.camController.realObjectsUsed()){
	            	if(this.camController.objectIsVisible(io) == 1){
	            		callForHelp(io);	
	            	}
            	}
            	else{
            		if(this.camController.objectIsVisible(io) == -1){
		                if (conf < 0.1 && conf < lastConf) {               	
		                	callForHelp(io);	
		                }
            		}
            	}
                this.addLastConfidence(io, conf);
            }
        }
	}
	
    private void printVisionGraph(){
    	String neighs = "";
    	for (String neighbour : visionGraph.keySet()) {
			neighs = neighs + "; " + neighbour; 
		}
    	
    	System.out.println(this.camController.getName() + " has the following " + neighs);
    }

    

}
