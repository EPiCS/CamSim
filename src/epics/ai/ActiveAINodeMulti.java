package epics.ai;

import java.awt.font.NumericShaper.Range;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import epics.camsim.core.Bid;
import epics.common.AbstractAINode;
import epics.common.CmdLogger;
import epics.common.IBanditSolver;
import epics.common.IBid;
import epics.common.ICameraController;
import epics.common.IMessage;
import epics.common.IMessage.MessageType;
import epics.common.IRegistration;
import epics.common.ITrObjectRepresentation;
import epics.common.RandomNumberGenerator;
import epics.common.RandomUse;

/**
 * Implementation of AbstractAINode.
 * defines the behaviour of the camera node regarding communication policies and the auction invitation schedules.
 * this class uses the active auction invitation schedule to send invitations to other cameras in every timestep. 
 * @author Marcin Bogdanski & Lukas Esterle, refactored by Horatio Cane 
 */
public class ActiveAINodeMulti extends AbstractAINode {
    
    
    
    public static final int DETECTIONRATE = 100;
    
    public ActiveAINodeMulti(AbstractAINode ai){
    	super(ai);
    	AUCTION_DURATION = 0;
    }
    
    public ActiveAINodeMulti(int comm, boolean staticVG, 
    		Map<String, Double> vg, IRegistration r, int auctionDuration, RandomNumberGenerator rg) {
    	super(comm, staticVG, vg, r, rg); // Goes through to instantiateAINode()
//    	communication = comm;
    	AUCTION_DURATION = auctionDuration;
    }
    
    public ActiveAINodeMulti(int comm, boolean staticVG, 
    		Map<String, Double> vg, IRegistration r, int auctionDuration, RandomNumberGenerator rg, IBanditSolver bs) {
    	super(comm, staticVG, vg, r, rg, bs); // Goes through to instantiateAINode()
//    	communication = comm;
    	AUCTION_DURATION = auctionDuration;
    }
    
    public ActiveAINodeMulti(int comm, boolean staticVG, 
    		Map<String, Double> vg, IRegistration r, RandomNumberGenerator rg) {
    	super(comm, staticVG, vg, r, rg); // Goes through to instantiateAINode()
//    	communication = comm;
    	AUCTION_DURATION = 0;
    }
    
    public ActiveAINodeMulti(int comm, boolean staticVG, 
    		Map<String, Double> vg, IRegistration r, RandomNumberGenerator rg, IBanditSolver bs) {
    	super(comm, staticVG, vg, r, rg, bs); // Goes through to instantiateAINode()
//    	communication = comm;
    	AUCTION_DURATION = 0;
    }
    
    /**
     * Looks at objects owned by this camera that are desired by other cameras
     * and evaluates bids from other cameras. Gives the object to the highest 
     * bidder (which can be itself). 
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
	                                 //runningAuction.remove(tor);
	//                                 if(USE_MULTICAST_STEP){
	//                                	 multicast(MessageType.StopSearch, tor);
	//                                 }
	//                                 else{
	//                                	 broadcast(MessageType.StopSearch, tor);
	//                                 }
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

        if(DEBUG_CAM) {
            printStatus();
        }
        
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
	
}
