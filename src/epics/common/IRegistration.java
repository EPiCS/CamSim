package epics.common;

import epics.camsim.core.CameraController;
//import epics.camsim.core.TraceableObject;

public interface IRegistration {

	void objectIsAdvertised(ITrObjectRepresentation to);

	void addCamera(ICameraController cc);

	void advertiseGlobally(ITrObjectRepresentation tc);

	void objectTrackedBy(ITrObjectRepresentation to, ICameraController cc);

	void update();

	void removeCamera(ICameraController cc);

	void setOffline(int duration);
	
}
