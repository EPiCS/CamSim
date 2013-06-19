package epics.commpolicy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import epics.common.AbstractAINode;
import epics.common.ICameraController;
import epics.common.IMessage.MessageType;
import epics.common.ITrObjectRepresentation;
import epics.common.Multicast;
import epics.common.RandomNumberGenerator;
import epics.common.RandomUse;

/** 
 * Sends the given message based on the ratio of its link 
 * strength and the strongest link in the current vision graph.
 */
public class Smooth extends Multicast {

	Broadcast broadcast;
	
	public Smooth(AbstractAINode ai, ICameraController camController) {
		super(ai, camController);
		broadcast = new Broadcast(ai, camController);
	}
	
	public void multicast(MessageType mt, Object o) {
		
		Map<ITrObjectRepresentation, List<String>> advertised = ai.getAdvertisedObjects();
		Map<ITrObjectRepresentation, Integer> stepsTillBroadcast = ai.getStepsTillBroadcast();
		RandomNumberGenerator randomGen = ai.getRandomGen();
		
		if(mt == MessageType.StartSearch){
			ITrObjectRepresentation io = (ITrObjectRepresentation) o;
			if(AbstractAINode.USE_BROADCAST_AS_FAILSAVE){
				if(!stepsTillBroadcast.containsKey(io)){
					stepsTillBroadcast.put(io, AbstractAINode.STEPS_TILL_BROADCAST);
				}
			}
			//get max strength
			double highest = 0;
			Collection<Double> vgValues = ai.vgGetValues(io); 
			for(Double d : vgValues){
				if(d > highest){
					highest = d;
				}
			}

			if(highest > 0){
				double ran = randomGen.nextDouble(RandomUse.USE.COMM);
				int sent = 0;
				for(ICameraController icc : camController.getNeighbours()){
					String name = icc.getName();
					double ratPart = 0.0;
					if(ai.vgContainsKey(name, io)){
						ratPart = ai.vgGet(name, io);
					}
					double ratio = (1+ratPart)/(1+highest);
					if(ratio > ran){
						sent ++;
						ai.incrementSentMessages();
						camController.sendMessage(name, mt, o);
						List<String> cams = advertised.get((ITrObjectRepresentation) o);
						if(cams != null){
							if(!cams.contains(name))
								cams.add(name);
						}
						else{
							cams = new ArrayList<String>();
							cams.add(name);
							advertised.put((ITrObjectRepresentation) o, cams);
						}
					}
				}
				if(sent == 0){
					if(AbstractAINode.DEBUG_CAM){
						System.out.println(camController.getName() + " tried to MC --> now BC");
					}
					
					broadcast(mt, o);
				}
			}
			else{
				if(AbstractAINode.DEBUG_CAM){
					System.out.println(camController.getName() + " tried to MC --> now BC 2");
				}
				broadcast(mt, o);   
			}
		} else {
			if(mt == MessageType.StopSearch){
				if(advertised.isEmpty()){
					broadcast(mt, o);
				}
				else{
					if(advertised.get((ITrObjectRepresentation) o) != null){
						for (String name : advertised.get((ITrObjectRepresentation) o)){
							camController.sendMessage(name, mt, o);
						}
						advertised.remove(o); 
					}
				}
			}
		}
	}
	
	@Override
	public void broadcast(MessageType mt, Object o) {
		broadcast.multicast(mt, o);
	}
}
