package epics.movement;

import java.awt.geom.Point2D;
import java.util.List;

import epics.camsim.core.SimCore;
import epics.common.AbstractMovement;
import epics.common.RandomNumberGenerator;

/**
 * 
 * @author Lukas Esterle <Lukas.Esterle@aau.at>
 *
 */
public class Waypoints extends AbstractMovement{
    private List<Point2D> waypoints;
    private int currentWaypoint = 0;

    public Waypoints(double x, double y, double speed, double heading, RandomNumberGenerator rg, List<Point2D> waypoints, SimCore sim)
    {
        super(x, y, heading, speed, rg, sim);
        this.waypoints = waypoints;
        currentWaypoint = 1;
    }
    
//    public Waypoints(double x, double y, double speed, double heading, RandomNumberGenerator rg, SimCore sim, object)
//    {
//        super(x, y, heading, speed, rg, sim);
//    }

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

        //System.out.println( "WAYPOINT n=" + this.currentWaypoint + " of " + this.waypoints.size() + " [x" + waypoints.get(currentWaypoint).getX() + ", y" +waypoints.get(currentWaypoint).getY() + " ]");

        double delta_x = waypoints.get(currentWaypoint).getX() - this.x;
        double delta_y = waypoints.get(currentWaypoint).getY() - this.y;

        // Magic way to compute our new heading :)
        // Radians
        this.heading = Math.atan2(1,0) - Math.atan2(delta_y, delta_x);

        x_move = Math.sin(this.heading) * speed;
        y_move = Math.cos(this.heading) * speed;
        
        x += x_move;
        y += y_move;
        
        checkBoundaryCollision(x_move, y_move);
    }
    
    @Override
    public List<Point2D> getWaypoints() {
        return this.waypoints;
    }

    @Override
    public String toXMLString(String feat) {
        String indent = "                    ";
        String name = "brownian_motion";
        String retVal = "<" + name + " features=\"" + feat + "\" speed=\"" + this.speed;
        
        /*
         * <object_with_waypoints features="2.0" speed="1.0">
                <waypoint x="-28.0" y="2.0"/>
            </object_with_waypoints>
         */
        retVal += "\">";
        for (Point2D waypoint : this.getWaypoints()) {
            retVal += "\n"+indent+"<waypoint x=\"" + waypoint.getX() + "\" y=\"" + waypoint.getY() + "\"/>";
        }
        retVal += "\n"+indent+"</brownian_motion>";
        
        return retVal;
    }
}
