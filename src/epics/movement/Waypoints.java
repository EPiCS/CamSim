package epics.movement;

import java.awt.geom.Point2D;
import java.util.List;

import epics.camsim.core.SimCore;
import epics.common.AbstractMovement;
import epics.common.RandomNumberGenerator;

/**
 * Movement behaviour based on defiend waypoints. the objects starts at a given starting point and moves towards the first waypoint. each waypoint is traversed as given in the list in a direct line. 
 * if the starting point is not on a waypoint (or on any point laying on the line between two waypoints), this starting point is never reached again.  
 * @author Lukas Esterle <Lukas.Esterle@aau.at>
 *
 */
public class Waypoints extends AbstractMovement{
    private List<Point2D> waypoints;
    private int currentWaypoint = 0;

    /**
     * 
     * Constructor for Waypoints to be traversed by object
     * @param x x-coordinate of starting point of the object - moves towards first waypoint from here
     * @param y y-coordinate of starting point of the object - moves towards first waypoint from here
     * @param speed initial speed of object
     * @param heading initial heading of object
     * @param rg random number generator
     * @param waypoints set of waypoints tobe traveresed
     * @param sim
     */
    public Waypoints(double x, double y, double speed, double heading, RandomNumberGenerator rg, List<Point2D> waypoints, SimCore sim)
    {
        super(x, y, heading, speed, rg, sim);
        this.waypoints = waypoints;
        currentWaypoint = 1;
    }
    
    /*
     * (non-Javadoc)
     * @see epics.common.AbstractMovement#update()
     */
    @Override
    public void update() {
        double x_move = 0;
        double y_move = 0;
        
        double dist = Point2D.distance(this.x, this.y, waypoints.get(currentWaypoint).getX(), waypoints.get(currentWaypoint).getY());
        double epsilon = 0.00001;
        if (dist < (1 - epsilon)){ // Less than one (with epsilon) 
            this.currentWaypoint = this.currentWaypoint + 1;
            if ( this.currentWaypoint >= this.waypoints.size()){
                this.currentWaypoint = 0;
            }
        }
        double delta_x = waypoints.get(currentWaypoint).getX() - this.x;
        double delta_y = waypoints.get(currentWaypoint).getY() - this.y;

        // Radians
        this.heading = Math.atan2(1,0) - Math.atan2(delta_y, delta_x);

        x_move = Math.sin(this.heading) * speed;
        y_move = Math.cos(this.heading) * speed;
        
        x += x_move;
        y += y_move;
        
        checkBoundaryCollision(x_move, y_move);
    }
    
    /*
     * (non-Javadoc)
     * @see epics.common.AbstractMovement#getWaypoints()
     */
    @Override
    public List<Point2D> getWaypoints() {
        return this.waypoints;
    }

    /*
     * (non-Javadoc)
     * @see epics.common.AbstractMovement#toXMLString(java.lang.String)
     */
    @Override
    public String toXMLString(String feat) {
        String indent = "                    ";
        String name = "brownian_motion";
        String retVal = "<" + name + " features=\"" + feat + "\" speed=\"" + this.speed;
        
        retVal += "\">";
        for (Point2D waypoint : this.getWaypoints()) {
            retVal += "\n"+indent+"<waypoint x=\"" + waypoint.getX() + "\" y=\"" + waypoint.getY() + "\"/>";
        }
        retVal += "\n"+indent+"</brownian_motion>";
        
        return retVal;
    }
}
