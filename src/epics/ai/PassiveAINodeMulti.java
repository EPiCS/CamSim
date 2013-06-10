package epics.ai;

import java.util.HashMap;
import java.util.Map;

import epics.common.IRegistration;
import epics.common.ITrObjectRepresentation;

public class PassiveAINodeMulti extends ActiveAINodeMulti {

	private static final int DEFAULT_AUCTION_DURATION = 0;
	
	/** The confidence for an object below which we advertise */
	public static final double CONF_THRESHOLD = 0.1;
	
	/** Keep track of confidence from last time step to find whether 
	 * it has increased or decreased */
	private Map<ITrObjectRepresentation, Double> lastConfidence = new HashMap<ITrObjectRepresentation, Double>();
	
	public PassiveAINodeMulti(int comm, boolean staticVG, Map<String, Double> vg, IRegistration r){
    	super(comm, staticVG, vg, r, DEFAULT_AUCTION_DURATION); // Goes through to instantiateAINode()
    }

	@Override
    public void checkBidsForObjects() {
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
		                                		 strengthenVisionEdge(giveTo.getName(), tor);
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
			} else {
				conf = this.getConfidence(io);
				lastConf = this.getLastConfidenceFor(io);
			}
			
			if (conf < CONF_THRESHOLD && conf < lastConf) {               	
				callForHelp(io, 2);	
			}
			this.addLastConfidence(io, conf);
		}
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
}
