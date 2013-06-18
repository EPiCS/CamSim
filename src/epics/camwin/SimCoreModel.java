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

    public void add_camera( double x_pos, double y_pos,  double heading_degrees, double angle_degrees, double range, int comm, int limit, Map<String, Double> vg, String bs){
        sim.add_camera("C" + sim.getNextID(), x_pos, y_pos, heading_degrees, angle_degrees, range, comm, limit, vg, bs, null, null);
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

	public void save_to_xml(String absolutePath) {
		sim.save_to_xml(absolutePath);
		
	}

	/** Some demo scenarios available in a drop-down menu */
	public void loadDemo(int i) {
		
		sim.reset();
		
		this.getCameras().clear();
		this.getObjects().clear();
		
		switch (i) {
		case 1:
			this.add_camera(-15, 10, -180, 70, 20, 0, 0, null, "");
			this.add_camera(-5, 10, -180, 70, 20, 0, 0, null, "");
			this.add_camera(5, 10, -180, 70, 20, 0, 0, null, "");
			this.add_camera(15, 10, -180, 70, 20, 0, 0, null, "");
			this.add_camera(26, 8, -135, 70, 20, 0, 0, null, "");

			this.add_object(-30, -2, 90, 1);
		
			break;
		case 2:
	        this.add_camera(-15, 10, -180, 70, 20, 0, 0, null, "");
	        this.add_camera(-5, 10, -180, 70, 20, 0, 0, null, "");
	        this.add_camera(5, 10, -180, 70, 20, 0, 0, null, "");
	        this.add_camera(15, 10, -180, 70, 20, 0, 0, null, "");
	        this.add_camera(26, 8, -135, 70, 20, 0, 0, null, "");

	        this.add_object(-30, -2, 90, 1);
	        this.add_object(30, -2, -90, 1);
			break;
		case 3:
	      	double d = 11;

	      	this.add_camera(-20, d, -180, 70, 20, 0, 0, null, "");
	      	this.add_camera(-10, d, -180, 70, 20, 0, 0, null, "");
	      	this.add_camera(10, d, -180, 70, 20, 0, 0, null, "");
	      	this.add_camera(20, d, -180, 70, 20, 0, 0, null, "");

	      	this.add_camera(-20, -d, 0, 70, 20, 0, 0, null, "");
	      	this.add_camera(-10, -d, 0, 70, 20, 0, 0, null, "");
	      	this.add_camera(10, -d, 0, 70, 20, 0, 0, null, "");
	      	this.add_camera(20, -d, 0, 70, 20, 0, 0, null, "");

	      	this.add_camera(d, -20, -90, 70, 20, 0, 0, null, "");
	      	this.add_camera(d, -10, -90, 70, 20, 0, 0, null, "");
	      	this.add_camera(d, 10, -90, 70, 20, 0, 0, null, "");
	      	this.add_camera(d, 20, -90, 70, 20, 0, 0, null, "");

	      	this.add_camera(-d, -20, 90, 70, 20, 0, 0, null, "");
	      	this.add_camera(-d, -10, 90, 70, 20, 0, 0, null, "");
	      	this.add_camera(-d, 10, 90, 70, 20, 0, 0, null, "");
	      	this.add_camera(-d, 20, 90, 70, 20, 0, 0, null, "");

	      	double ln = 27;
	      	double ss = 3;

	      	ArrayList<Point2D> waypoints = new ArrayList<Point2D>();
	      	waypoints.add(new Point2D.Double(-ln, -ss));
	      	waypoints.add(new Point2D.Double(-ss, -ss));
	      	waypoints.add(new Point2D.Double(-ss, -ln));
	      	waypoints.add(new Point2D.Double(ss, -ln));
	      	waypoints.add(new Point2D.Double(ss, -ss));
	      	waypoints.add(new Point2D.Double(ln, -ss));
	      	waypoints.add(new Point2D.Double(ln, ss));
	      	waypoints.add(new Point2D.Double(ss, ss));

	      	waypoints.add(new Point2D.Double(ss, ln));
	      	waypoints.add(new Point2D.Double(-ss, ln));
	      	waypoints.add(new Point2D.Double(-ss, ss));
	      	waypoints.add(new Point2D.Double(-ln, ss));

	      	this.add_object(0.9, waypoints, SimCore.getNextID());
			break;
		default:
			this.add_camera(-15, 10, -180, 70, 20, 0, 0, null, "");
			this.add_camera(-5, 10, -180, 70, 20, 0, 0, null, "");
			this.add_camera(5, 10, -180, 70, 20, 0, 0, null, "");
			this.add_camera(15, 10, -180, 70, 20, 0, 0, null, "");
			this.add_camera(26, 8, -135, 70, 20, 0, 0, null, "");

			this.add_object(-30, -2, 90, 1);
			break;
		}
	}

}
