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

    /**
     * Constructor
     * @param id Unique identifier for an object, that is how object
     * distinguish between objects.
     * @param x Object position on X axis
     * @param y Object position on Y axis
     * @param heading Object heading, 0 is north, PI/2 is east )
     * @param speed
     */
    public TraceableObject( double id, SimCore sim, double x, double y, double heading, double speed ){
        this.sim = sim;
        this.x = x;
        this.y = y;
        this.heading = heading;
        this.speed = speed;
        this.features.add(id);
    }
    
    private List<Point2D> waypoints;
    private int currentWaypoint = 0;

    public TraceableObject( double id, SimCore sim, double speed, List<Point2D> waypoints ){
        assert( waypoints.size() >= 2 );
        this.sim = sim;
        this.features.add(id);
        this.speed = speed;
        this.waypoints = waypoints;
        this.x = waypoints.get(0).getX();
        this.y = waypoints.get(0).getY();
        this.currentWaypoint = 1;
    }

    void update() {

        double x_move = 0;
        double y_move = 0;
        if (hasWaypoints()){

            double dist = Point2D.distance(this.x, this.y, waypoints.get(currentWaypoint).getX(), waypoints.get(currentWaypoint).getY());
            //System.out.println("     DISTANCE     " + dist);
            if (dist < 1){
                //System.out.println(" CHANGING WAYPOINT !!");
                this.currentWaypoint = this.currentWaypoint + 1;
                if ( this.currentWaypoint >= this.waypoints.size()){
                    this.currentWaypoint = 0;
                }
            }

            //System.out.println( "WAYPOINT n=" + this.currentWaypoint + " of " + this.waypoints.size() + " [x" + waypoints.get(currentWaypoint).getX() + ", y" +waypoints.get(currentWaypoint).getY() + " ]");

            //System.out.println( "POS: x:" + this.x + " y:" + this.y );

            /*
             * Direction we should be going
             */
            double dir_x = waypoints.get(currentWaypoint).getX() -this.x;
            double dir_y = waypoints.get(currentWaypoint).getY() -this.y;

            //System.out.println( "DIR: dir_x:" + dir_x + " dir_y:" + dir_y );

            // Dot product of [0,1] vector and our direction
            double dot = 0 * dir_x + 1 * dir_y;

            // Magic way to compute our new heading :)
            double ang = ( Math.atan2( 1, 0 ) - Math.atan2( dir_y, dir_x ) ) * 180 / Math.PI;

            //System.out.println( "ANG: " + ang );

            this.heading = ang;

            x_move = Math.sin( (heading * Math.PI) / 180 ) * speed;
            y_move = Math.cos( (heading * Math.PI) / 180 ) * speed;
        } else {
            x_move = Math.sin( heading ) * speed;
            y_move = Math.cos( heading ) * speed;
        }

        //System.out.println( "MOVE: x:" + x_move + " y:" + y_move );

        this.x += x_move;
        this.y += y_move;

        if ( this.x > sim.get_max_x() ){
            //System.out.println( "       KICKING IN       ");
            this.heading += getTurnaroundAngle(); this.x -= speed;
        }
        if ( this.x < sim.get_min_x() ){
            //System.out.println( "       KICKING IN       ");
            this.heading += getTurnaroundAngle(); this.x += speed;
        }

        if ( this.y > sim.get_max_y() ){
            //System.out.println( "       KICKING IN       ");
            this.heading += getTurnaroundAngle(); this.y -= speed;
        }
        if ( this.y < sim.get_min_y() ){
            //System.out.println( "       KICKING IN       ");
            this.heading += getTurnaroundAngle(); this.y += speed;
        }
    }

    /** Gets an angle to turn around by with some partial added randomness */
    public double getTurnaroundAngle() {
    	// Turn around 180 degrees, add a bit of angle for randomness
    	return Math.PI + RandomNumberGenerator.nextDouble(RandomUse.USE.TURN) * Math.PI / 6.0;
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
