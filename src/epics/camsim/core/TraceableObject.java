package epics.camsim.core;

import epics.common.*;
import epics.movement.*;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;



/**
 *
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>, refactored by Lukas Esterle <Lukas.Esterle@aau.at>
 */
public class TraceableObject{

    private ArrayList<Double> features = new ArrayList<Double>();
    private AbstractMovement move;
	
    /**
     * Constructor
     * @param id Unique identifier for an object, that is how object
     * distinguish between objects.
     * @param x Object position on X axis
     * @param y Object position on Y axis
     * @param heading Object heading, 0 is north, PI/2 is east )
     * @param speed
     */
    public TraceableObject( double id, SimCore sim, double x, double y, double heading, double speed, RandomNumberGenerator rg ){
        this.move = new Straight(x, y, heading, speed, rg, sim);
        this.features.add(id);
    }
    
    public TraceableObject(double id, AbstractMovement move){
        this.features.add(id);
        this.move = move;
    }
    
    public TraceableObject( double id, SimCore sim, double speed, List<Point2D> waypoints, RandomNumberGenerator rg){
        assert( waypoints.size() >= 2 );
        this.features.add(id);
        move = new Waypoints(waypoints.get(0).getX(), waypoints.get(0).getY(), speed, -1.0, rg, waypoints, sim);
    }

    void update() {      
        move.update();
    }
    
    public double getX(){
    	return move.getX();
    }
    public double getY(){
    	return move.getY();
    }

    public double getHeading(){
    	return move.getHeading();
    }

    List<Double> getFeatures() {
    	return this.features;
    }
    
    @Override
    public String toString(){
    	return this.features.toString();
    }

    /** Provides the XML string representing this object. Requires the 
     * indentation string as an argument in order to indent multi-line 
     * entries (i.e. objects with waypoints) */
	public String toXMLString() {
		String feat = "" + this.features;
    	feat = feat.substring(1, feat.length()-1);
    	
    	String retVal = move.toXMLString(feat);
    	
    	return retVal;
	}

}
