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
	public void advertiseTrackedObjects() {
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
			
			if (conf < CONF_THRESHOLD && (conf == 0 || conf < lastConf)) {               	
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
