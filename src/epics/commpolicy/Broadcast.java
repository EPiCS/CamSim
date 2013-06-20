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
 * Broadcast the message to all nodes
 */
public class Broadcast extends Multicast {

	public Broadcast(AbstractAINode ai, ICameraController camController) {
		super(ai, camController);
	}
	
	public void multicast(MessageType mt, Object o) {
		
		Map<ITrObjectRepresentation, List<String>> advertised = ai.getAdvertisedObjects();
		for (ICameraController icc : camController.getNeighbours()) {
            camController.sendMessage(icc.getName(), mt, o);
            
            if (mt == MessageType.StartSearch) {
                List<String> cams = advertised.get((ITrObjectRepresentation) o);
                ai.incrementSentMessages();
                if (cams != null) {
                    if (!cams.contains(icc.getName())) {
                        cams.add(icc.getName());
                    }
                } else {
                    cams = new ArrayList<String>();
                    cams.add(icc.getName());
                    advertised.put((ITrObjectRepresentation) o, cams);
                }
            }
        }
        if (mt == MessageType.StopSearch) {
            advertised.remove((ITrObjectRepresentation) o);
        }
	}
	
	@Override
	public void broadcast(MessageType mt, Object o) {
		multicast(mt, o);
	}
}
