package epics.ai;

import java.util.Map;

import epics.common.IRegistration;
import epics.common.ITrObjectRepresentation;

/** An AI node which takes into account historical trends in objects'
 * appearance and time steps seen for. 
 * 
 * This is a shell class, containing shallow methods which call through
 * to the HistoricalNodeHelper which contains the real logic and state.
 * 
 * PLEASE NOTE THAT THIS CLASS HAS A TWIN: HistoricalAINodePassive 
 * AND CHANGES IN EITHER ONE MUST BE REPLICATED IN THE OTHER.
 * THIS IS UGLY BUT NECESSARY GIVEN THE ARCHITECTURE. 
 * IF THERE ARE DIFFERENCES BETWEEN THE TWO OTHER THAN THE CLASS
 * BEING EXTENDED, PLEASE RECTIFY THEM SOONER RATHER THAN LATER! */
public class HistoricalAINodeActive extends ActiveAINodeMulti {
	
	private HistoricalNodeHelper nodeHelper;
	
    // Overriding AbstractAINode's constructor
    public HistoricalAINodeActive(int comm, boolean staticVG, 
    		Map<String, Double> vg, IRegistration r){
    	this(comm, staticVG, vg, r, 
    			HistoricalNodeHelper.DEFAULT_PRE_INSTANTIATION_BID_COEFFICIENT,
    			HistoricalNodeHelper.DEFAULT_OVERSTAY_BID_COEFFICIENT); // Goes through to instantiateAINode()
    }
    
    public HistoricalAINodeActive(int comm, boolean staticVG, 
    		Map<String, Double> vg, IRegistration r,
    		double preInstantiationBidCoefficient, double overstayBidCoefficient){
    	super(comm, staticVG, vg, r); // Goes through to instantiateAINode()
    	nodeHelper = new HistoricalNodeHelper(
    			preInstantiationBidCoefficient, overstayBidCoefficient);
    }

    @Override
    public void update() {
    	// Store current point and get rid of an old one if necessary
    	nodeHelper.updateHistoricalPoints(this.camController);
    	super.update();
    }
	
	@Override
	/** The bid for an object. Not the same as 'confidence' */
	public double calculateValue(ITrObjectRepresentation target) {
		double superValue = super.calculateValue(target);
		return nodeHelper.calculateValue(target, superValue);
	}
	
	@Override
	public boolean setParam(String key, String value) {
		if (super.setParam(key, value)) return true; 
		return nodeHelper.setParam(key, value);
	}
}
