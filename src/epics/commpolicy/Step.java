package epics.commpolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import epics.common.AbstractAINode;
import epics.common.ICameraController;
import epics.common.ITrObjectRepresentation;
import epics.common.RandomNumberGenerator;
import epics.common.RandomUse;
import epics.common.IMessage.MessageType;
import epics.common.Multicast;

/**
 * Sends the given message to another camera if the link 
 * strength is above a given threshold. 
 * Otherwise communicating with a camera has a very low probability 
 */
public class Step extends Multicast {

	Broadcast broadcast;
	
	public Step(AbstractAINode ai, ICameraController camController) {
		super(ai, camController);
		broadcast = new Broadcast(ai, camController);
	}
	
	public void multicast(MessageType mt, Object o) {
		Map<ITrObjectRepresentation, List<String>> advertised = ai.getAdvertisedObjects();
		Map<ITrObjectRepresentation, Integer> stepsTillBroadcast = ai.getStepsTillBroadcast();
		RandomNumberGenerator randomGen = ai.getRandomGen();
		
		if (mt == MessageType.StartSearch) {
			ITrObjectRepresentation io = (ITrObjectRepresentation) o;
			if (AbstractAINode.USE_BROADCAST_AS_FAILSAVE) {
				if (!stepsTillBroadcast.containsKey(io)) {
					stepsTillBroadcast.put(io, AbstractAINode.STEPS_TILL_BROADCAST);
				}
			}
			
			int sent = 0;
			double ran = randomGen.nextDouble(RandomUse.USE.COMM);
			
			for (ICameraController icc : this.camController.getNeighbours()) {
				String name = icc.getName();
				double prop = 0.1;
				if (ai.vgContainsKey(name, io)) {
					prop = ai.vgGet(name, io);
				}
				if (prop > ran) {
					sent ++;
					ai.incrementSentMessages();
					this.camController.sendMessage(name, mt, o);
					List<String> cams = advertised.get((ITrObjectRepresentation) o);
					if (cams != null) {
						if (!cams.contains(name)) {
							cams.add(name);
						}
					} else {
						cams = new ArrayList<String>();
						cams.add(name);
						advertised.put((ITrObjectRepresentation) o, cams);
					}
				}
			}

			if(sent == 0){
				if(AbstractAINode.DEBUG_CAM){
					System.out.println(this.camController.getName() + " tried to MC --> now BC");
				}
				broadcast(mt, o);
			}
		} else {
			if (mt == MessageType.StopSearch) {
				if (advertised.isEmpty()) {
					broadcast(mt, o);
				} else if (advertised.get((ITrObjectRepresentation) o) != null) {
					for (String name : advertised.get((ITrObjectRepresentation) o)) {
						this.camController.sendMessage(name, mt, o);
					}
					advertised.remove(o); 
				}
			}
		}
	}
	
	@Override
	public void broadcast(MessageType mt, Object o) {
		broadcast.multicast(mt, o);
	}
}
