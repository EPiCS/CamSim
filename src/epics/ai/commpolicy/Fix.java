package epics.ai.commpolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import epics.common.AbstractAuctionSchedule;
import epics.common.ICameraController;
import epics.common.IMessage.MessageType;
import epics.common.ITrObjectRepresentation;
import epics.common.AbstractCommunication;

/** 
 * Sends the given message to the neighbouring cameras in the vision graph. 
 * This method does not make use of the strength of the links.
 */
public class Fix extends AbstractCommunication {

	Broadcast broadcast;
	
	/**
	 * Constructor for Fix communication policy
	 * @param ai ai using this communication policy
	 * @param camController camera controller using this policy (via ai)
	 */
	public Fix(AbstractAuctionSchedule ai, ICameraController camController) {
		super(ai, camController);
		broadcast = new Broadcast(ai, camController);
	}
	
	public void multicast(MessageType mt, Object o) {
		Map<ITrObjectRepresentation, List<String>> advertised = ai.getAdvertisedObjects();

		List<String> cams = new ArrayList<String>();
		for (String name : ai.vgGetCamSet()) {
            camController.sendMessage(name, mt, o);
            cams.add(name);
        }
        
        if(mt == MessageType.StartSearch){
            advertised.put((ITrObjectRepresentation) o, cams);
        }
        
        if(mt == MessageType.StopSearch){
            advertised.remove((ITrObjectRepresentation) o);
        }
	}
	
	@Override
	public void broadcast(MessageType mt, Object o) {
		broadcast.broadcast(mt, o);
	}
}
