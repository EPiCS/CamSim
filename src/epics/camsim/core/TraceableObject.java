package epics.camsim.core;

import epics.common.RandomNumberGenerator;
import epics.common.RandomUse;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;



/**
 *
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public class TraceableObject{

    private ArrayList<Double> features = new ArrayList<Double>();

    private SimCore sim;
    private double x; // meters (horizontal)
    private double y; // meters (vertical)
    private double heading; // radians ( 0 is north, PI/2 is east )
    private double speed; // meters per second

	private RandomNumberGenerator randomGen;

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
        this.sim = sim;
        this.x = x;
        this.y = y;
        this.heading = heading;
        this.speed = speed;
        this.features.add(id);
        this.randomGen = rg;
    }
    
    private List<Point2D> waypoints;
    private int currentWaypoint = 0;

    public TraceableObject( double id, SimCore sim, double speed, List<Point2D> waypoints, RandomNumberGenerator rg){
        assert( waypoints.size() >= 2 );
        this.sim = sim;
        this.features.add(id);
        this.speed = speed;
        this.waypoints = waypoints;
        this.x = waypoints.get(0).getX();
        this.y = waypoints.get(0).getY();
        this.currentWaypoint = 1;
        this.randomGen = rg;
    }

    void update() {

        double x_move = 0;
        double y_move = 0;
        if (hasWaypoints()){

            double dist = Point2D.distance(this.x, this.y, waypoints.get(currentWaypoint).getX(), waypoints.get(currentWaypoint).getY());
            double epsilon = 0.00001;
            if (dist < (1 - epsilon)){ // Less than one (with epsilon) 
                this.currentWaypoint = this.currentWaypoint + 1;
                if ( this.currentWaypoint >= this.waypoints.size()){
                    this.currentWaypoint = 0;
                }
            }

            //System.out.println( "WAYPOINT n=" + this.currentWaypoint + " of " + this.waypoints.size() + " [x" + waypoints.get(currentWaypoint).getX() + ", y" +waypoints.get(currentWaypoint).getY() + " ]");

            double delta_x = waypoints.get(currentWaypoint).getX() - this.x;
            double delta_y = waypoints.get(currentWaypoint).getY() - this.y;

            // Magic way to compute our new heading :)
            // Radians
            this.heading = Math.atan2(1,0) - Math.atan2(delta_y, delta_x);

            x_move = Math.sin(this.heading) * speed;
            y_move = Math.cos(this.heading) * speed;
        } else {
            x_move = Math.sin(this.heading) * speed;
            y_move = Math.cos(this.heading) * speed;
        }

        this.x += x_move;
        this.y += y_move;

        // If we breach any boundary, bounce off at a slightly randomised angle
        if (this.x > sim.get_max_x() || this.x < sim.get_min_x() ||
        		this.y > sim.get_max_y() || this.y < sim.get_min_y()){
            this.heading += getTurnaroundAngle(); 
            
            // Undo move across boundary
            this.x -= x_move; 
            this.y -= y_move;
        }
    }

    /** Gets an angle to turn around by with some partial added randomness */
    public double getTurnaroundAngle() {
    	// Turn around 180 degrees, add a bit of angle for randomness
    	double angle = Math.PI; // Turn 180 degrees
    	angle += (randomGen.nextDouble(RandomUse.USE.TURN)*2-1.0) * Math.PI / 6.0;
    	return angle;
    }
    
    public double getX(){
    	return this.x;
    }
    public double getY(){
    	return this.y;
    }

    public double getHeading(){
    	return this.heading;
    }

    List<Double> getFeatures() {
    	return this.features;
    }

    public boolean hasWaypoints() {
    	return this.waypoints != null && this.waypoints.size() > 0;
    }
    
    public List<Point2D> getWaypoints() {
    	return waypoints;
    }

    @Override
    public String toString(){
    	return this.features.toString();
    }

    /** Provides the XML string representing this object. Requires the 
     * indentation string as an argument in order to indent multi-line 
     * entries (i.e. objects with waypoints) */
	public String toXMLString(String indent) {
		String feat = "" + this.features;
    	feat = feat.substring(1, feat.length()-1);
    	String name = hasWaypoints() ? "object_with_waypoints" : "object";
    	String retVal = "<" + name + " features=\"" + feat + "\" speed=\"" + this.speed;
    	
    	/*
    	 * <object_with_waypoints features="2.0" speed="1.0">
                <waypoint x="-28.0" y="2.0"/>
            </object_with_waypoints>
    	 */
    	if (hasWaypoints()) {
    		retVal += "\">";
    		for (Point2D waypoint : this.getWaypoints()) {
    			retVal += "\n"+indent+"<waypoint x=\"" + waypoint.getX() + "\" y=\"" + waypoint.getY() + "\"/>";
    		}
    		retVal += "\n"+indent+"</object_with_waypoints>";
    	} else {
    		retVal += "\" heading=\"" + this.heading * Math.toRadians(3600) + "\" x=\"" + this.x + "\" y=\"" + this.y + "\"/>";
    	}
    	return retVal;
	}

}
