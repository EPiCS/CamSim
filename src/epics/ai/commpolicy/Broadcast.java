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
 * Broadcast the message to all nodes
 * @author Lukas Esterle
 */
public class Broadcast extends AbstractCommunication {

    /**
     * Constructor for Broadcast
     * @param ai The ai using this broadcast mechanism
     * @param camController the camera controller using this mechanism
     */
	public Broadcast(AbstractAuctionSchedule ai, ICameraController camController) {
		super(ai, camController);
	}
	
	/**
	 * communciates with all individual cameras 
	 * don't be fooled by the name - this methods still performs a broadcast!
	 */
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
