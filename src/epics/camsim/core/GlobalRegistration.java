package epics.camsim.core;

import java.util.*;

import epics.common.*;
import epics.common.IMessage.MessageType;

/**
 * 
 * @author Lukas Esterle <Lukas.Esterle@aau.at>
 *
 */

public class GlobalRegistration implements IRegistration {
	
	private static final int CHECKINTERVAL = 5;
	
	Map<ITrObjectRepresentation, Integer> checkObjects = new HashMap<ITrObjectRepresentation, Integer>();
	List<ICameraController> allCameras = new ArrayList<ICameraController>();
	List<ITrObjectRepresentation> advertisedObjects = new ArrayList<ITrObjectRepresentation>();
	Map<ITrObjectRepresentation, ICameraController> tracked = new HashMap<ITrObjectRepresentation, ICameraController>();
	
	boolean offline = false;
	int offlineFor = -1;
	
	@Override
	public void objectTrackedBy(ITrObjectRepresentation to, ICameraController cc){
		if(!allCameras.contains(cc))
			allCameras.add(cc);
		tracked.put(to, cc);
		checkObjects.put(to, CHECKINTERVAL); 
		advertisedObjects.remove(to);
	}
	
	@Override
	public void update(){
		
		//printAllInformation();
		if(!offline){
			for (Integer i : checkObjects.values()) {
				i--;
			}
			Map<ITrObjectRepresentation, Integer> updated = new HashMap<ITrObjectRepresentation, Integer>();
			for(Map.Entry<ITrObjectRepresentation, Integer> kvp : checkObjects.entrySet()){
				int old = kvp.getValue();
				old --;
				updated.put(kvp.getKey(), old);
			}
			
			checkObjects = updated;
			
			List<ITrObjectRepresentation> adv = new ArrayList<ITrObjectRepresentation>();
			for (Map.Entry<ITrObjectRepresentation, Integer> kvp : checkObjects.entrySet()) {
				double conf = 1.0;
				if(kvp.getValue() < 1){
					conf = askCameraFor(kvp.getKey());
				}
				if(conf == 0.0){
					//advertise globally
					adv.add(kvp.getKey());
	//				advertiseGlobally(kvp.getKey());
				}
			}
			
			for(ITrObjectRepresentation tr : adv){
				advertiseGlobally(tr);
			}
		}
		else{
			if(offlineFor > 0){
				offlineFor --;
			}
			else if(offlineFor == 0){
				offline = false;
			}
		}
	}
	
	
	private double askCameraFor(ITrObjectRepresentation key) {
		double retVal = 0.0;
		CameraController cc = (CameraController) tracked.get(key);
		if(cc != null){
			if(!cc.isOffline()){
				IMessage reply = cc.getAINode().receiveMessage(new Message("", "", MessageType.AskConfidence, key));
				if(reply != null){
					if(reply.getType().equals(MessageType.ResponseToAskIfCanTrack)){
						retVal = (Double) reply.getContent();
					}
				}
			}
		}
		return retVal;
	}

	@Override
	public void objectIsAdvertised(ITrObjectRepresentation to){
		checkObjects.remove(to);
		if(!advertisedObjects.contains(to))
			advertisedObjects.add(to);
	}
	
	@Override
	public void addCamera(ICameraController cc){
		allCameras.add(cc);
	}
	
	@Override
	public void removeCamera(ICameraController cc){
		allCameras.remove(cc);
		if(tracked.containsValue(cc)){
			for (Map.Entry<ITrObjectRepresentation, ICameraController> kvp : tracked.entrySet()) {
				if(kvp.getValue().equals(cc)){
					advertiseGlobally(kvp.getKey());
				}
			}
		}
	}
	
	@Override
	public void advertiseGlobally(ITrObjectRepresentation tc){
		for (ICameraController cc : allCameras){
			((CameraController) cc).getAINode().receiveMessage(new Message("", cc.getName(), MessageType.StartSearch , tc));			
		}
		objectIsAdvertised(tc);
	}
	
	private void printAllInformation(){
		System.out.println("==================== PRINTING GLOBAL REGISTRATION INFORMATION ===========================");
		String cams = "-- CAMERAS: ";
		for (ICameraController cc : allCameras) {
			cams += cc.getName() + "; ";
		}
		System.out.println(cams);
		String track = "-- TRACKED: ";
		for (Map.Entry<ITrObjectRepresentation, ICameraController> kvp : tracked.entrySet()) {
			track += kvp.getKey().getFeatures() + " --> " + kvp.getValue().getName() + "; ";
		}
		System.out.println(track);
		
		String adv = "-- ADVERTISED: ";
		for(ITrObjectRepresentation tr : advertisedObjects){
			adv += tr.getFeatures() + "; ";
		}
		System.out.println(adv);
		
		System.out.println("================================== END GLOBAL REGISTRATION INFORMATION ================================");
		
	}

	@Override
	public void setOffline(int duration) {
		if(duration == -1){
			offline = true;
		}
		else if(duration == 0){
			offlineFor = 0;
			offline = false;
		}
		else{
			offlineFor = duration;
		}
	}
}
