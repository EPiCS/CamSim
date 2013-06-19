package epics.commpolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import epics.common.AbstractAINode;
import epics.common.ICameraController;
import epics.common.IMessage.MessageType;
import epics.common.ITrObjectRepresentation;
import epics.common.Multicast;

/** 
 * Sends the given message to the neighbouring cameras in the vision graph. 
 * This method does not make use of the strength of the links.
 */
public class Fix extends Multicast {

	Broadcast broadcast;
	
	public Fix(AbstractAINode ai, ICameraController camController) {
		super(ai, camController);
		broadcast = new Broadcast(ai, camController);
	}
	
	public void multicast(MessageType mt, Object o) {
		Map<ITrObjectRepresentation, List<String>> advertised = ai.getAdvertisedObjects();

		List<String> cams = new ArrayList<String>();
		for (String name : ai.vgGetCamSet()) {
            this.camController.sendMessage(name, mt, o);
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
		broadcast.multicast(mt, o);
	}
}
