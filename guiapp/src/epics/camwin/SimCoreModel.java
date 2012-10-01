/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package epics.camwin;

import epics.camsim.core.CameraController;
import epics.camsim.core.SimCore;
import epics.camsim.core.SimSettings;
import epics.camsim.core.TraceableObject;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
/**
 *
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public class SimCoreModel extends Observable {

    private SimCore sim;

    public CoordinateSystemTransformer createCoordinateSystemTransformer( double size_x, double size_y){
        return new CoordinateSystemTransformer(sim.get_min_x(), sim.get_max_x(), sim.get_min_y(), sim.get_max_y(), size_x, size_y);
    }

    public SimCoreModel( SimCore sim ){
        this.sim = sim;
    }

    ArrayList<TraceableObject> getObjects() {
        return sim.getObjects();
    }

    ArrayList<CameraController> getCameras() {
        return sim.getCameras();
    }

    CameraController getCameraByName(String name){
        return sim.getCameraByName(name);
    }

    public void add_random_camera(){
        sim.add_random_camera();
    }

    public void add_camera( double x_pos, double y_pos,  double heading_degrees, double angle_degrees, double range, int comm, int limit, Map<String, Double> vg){
        sim.add_camera("C" + sim.getNextID(), x_pos, y_pos, heading_degrees, angle_degrees, range, comm, limit, vg);
    }

    public void recreate_cameras(){
        sim.recreate_cameras();
    }

    public void remove_random_camera(){
        sim.remove_random_camera();
    }

    public void add_random_object(){
        sim.add_random_object();
    }

    public void add_object( double pos_x, double pos_y, double heading_degrees, double speed){
        sim.add_object(pos_x, pos_y, heading_degrees, speed);
    }

    public void add_object( double speed, List<Point2D> waypoints, double id ){
        sim.add_object(speed, waypoints, id);
    }

    public void remove_random_object(){
        sim.remove_random_object();
    }

    public double computeUtility(){
        return sim.computeUtility();
    }

    public void remove_camera( String name ){
        sim.remove_camera( name );
    }

    public void remove_camera_index( int remove_index ){
        sim.remove_camera_index(remove_index);
    }

    public void update() throws Exception {
        sim.update();
    }

	public void interpretFile(SimSettings simsettings) {
		sim.interpretFile(simsettings);
		
	}

}
