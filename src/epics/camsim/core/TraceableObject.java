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
    private double minX = 0;
    private double maxX = 0;
    private double minY = 0;
    private double maxY = 0;
	
    /**
     * Constructor
     * @param id Unique identifier for an object, that is how object
     * distinguish between objects.
     * @param sim 
     * @param x Object position on X axis
     * @param y Object position on Y axis
     * @param heading Object heading, 0 is north, PI/2 is east )
     * @param speed
     * @param rg 
     */
    public TraceableObject( double id, SimCore sim, double x, double y, double heading, double speed, RandomNumberGenerator rg ){
        this.move = new Straight(x, y, heading, speed, rg, sim);
        maxX=sim.get_max_x();
        minX=sim.get_min_x();
        maxY=sim.get_max_y();
        minY=sim.get_min_y();
        this.features.add(id);
    }
    
    /**
     * Constructor for TraceableObject
     * @param id unique id of object
     * @param move movement behaviour of object
     */
    public TraceableObject(double id, AbstractMovement move){
        this.features.add(id);
        maxX=move.getBoundaries()[0];
        minX=move.getBoundaries()[1];
        maxY=move.getBoundaries()[2];
        minY=move.getBoundaries()[3];
        this.move = move;
    }
    
    /**
     * Constructor for TraceableObject
     * @param id unique id of object
     * @param sim simulation environment
     * @param speed movement speed of object
     * @param waypoints waypoints the object moves along
     * @param rg random number generator
     */
    public TraceableObject( double id, SimCore sim, double speed, List<Point2D> waypoints, RandomNumberGenerator rg){
        assert( waypoints.size() >= 2 );
        this.features.add(id);
        maxX=sim.get_max_x();
        minX=sim.get_min_x();
        maxY=sim.get_max_y();
        minY=sim.get_min_y();
        move = new Waypoints(waypoints.get(0).getX(), waypoints.get(0).getY(), speed, -1.0, rg, waypoints, sim);
    }

    void update() {      
        move.update();
    }
    
    /**
     * x-coordinate of object
     * @return
     */
    public double getX(){
    	return move.getX();
    }
    /**
     * y-coordinate of object
     * @return
     */
    public double getY(){
    	return move.getY();
    }
    
    /**
     * calculates x position based on upper left corner as (0,0)
     * @return
     */
    public double getTotalX(){
        return move.getX()-(minX);
    }
    /**
     * calculates y position based on upper left corner as (0,0)
     * @return
     */
    public double getTotalY(){
        return (minY+move.getY())*(-1);
    }

    /**
     * heading of object
     * @return
     */
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
     * entries (i.e. objects with waypoints) 
     * @return */
	public String toXMLString() {
		String feat = "" + this.features;
    	feat = feat.substring(1, feat.length()-1);
    	
    	String retVal = move.toXMLString(feat);
    	
    	return retVal;
	}

}
