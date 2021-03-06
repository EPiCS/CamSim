package epics.movement;

import java.awt.geom.Point2D;
import java.util.List;

import epics.camsim.core.SimCore;
import epics.common.*;

/**
 * 
 * @author Lukas Esterle <Lukas.Esterle@aau.at>
 *
 */

public class Straight extends AbstractMovement{

    /**
     * Constructor for Straight movement. The Object moves in straight line until it reaches the border of the simulation environment. bounces back in a random direction.
     * @param x x-coordinate of starting point for the object
     * @param y y-coordinate of starting point for the object
     * @param heading initial heading of the object
     * @param speed initial speed
     * @param rg random generator - should be the same for all objects
     * @param sim simulation environment
     */
    public Straight(double x, double y, double heading, double speed, RandomNumberGenerator rg, SimCore sim) {
        super(x, y, heading, speed, rg, sim);
    }

    @Override
    public void update() {
        double x_move = 0;
        double y_move = 0;
        
        x_move = Math.sin(heading) * speed;
        y_move = Math.cos(heading) * speed;
        
        x += x_move;
        y += y_move;
        
        checkBoundaryCollision(x_move, y_move);
    }

    @Override
    public List<Point2D> getWaypoints() {
        return null;
    }

    @Override
    public String toXMLString(String feat) {
        String name = "object";
        String retVal = "<" + name + " features=\"" + feat + "\" speed=\"" + this.speed;
        
        retVal += "\" heading=\"" + this.heading * Math.toRadians(3600) + "\" x=\"" + this.x + "\" y=\"" + this.y + "\"/>";
        
        return retVal;
    }
    
}
