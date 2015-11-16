package epics.movement;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import epics.camsim.core.SimCore;
import epics.common.AbstractMovement;
import epics.common.RandomNumberGenerator;
import epics.common.RandomUse.USE;

/**
 * DirectedBrownian is based on the well-known Brownian movement. The main difference is, the object may change its direction and speed only within a small variation window instead of any direction with any speed. This shoule reflect a more natural movement of people. 
 * 
 * @author Lukas Esterle <lukas [dot] esterle [at] aau [dot] at>
 *
 */
public class DirectedBrownian extends AbstractMovement{
    /**
     * Constructor for DirectedBrownian
     * @param x x-coordinate for initial (starting) position
     * @param y y-coordinate for initial (starting) position
     * @param heading initial direction of the object
     * @param speed initial speed of the obejct
     * @param rg random number generator
     * @param sim simulation environment
     */
    public DirectedBrownian(double x, double y, double heading, double speed, RandomNumberGenerator rg, SimCore sim){
        super(x, y, heading, speed, rg, sim);
    }

    /*
     * (non-Javadoc)
     * @see epics.common.AbstractMovement#getWaypoints()
     */
    @Override
    public List<Point2D> getWaypoints() {
        return new ArrayList<Point2D>();
    }

    /*
     * (non-Javadoc)
     * @see epics.common.AbstractMovement#update()
     */
    @Override
    public void update() {
        double x_move = 0;
        double y_move = 0;
        
        double sran = randomGen.nextGaussian(USE.MOVE);//.nextDouble(USE.MOVE);
        double hran = randomGen.nextGaussian(USE.MOVE);//(randomGen.nextDouble(USE.MOVE));
        speed += (sran / 10); //- 0.5) / 10;
        heading += (hran / 2);// - 0.5) / 2; 
        
//        System.out.println(sran + " - " + hran);
        
        x_move = Math.sin(heading) * speed;
        y_move = Math.cos(heading) * speed;
        
        x += x_move;
        y += y_move;
        
        checkBoundaryCollision(x_move, y_move);
    }

    @Override
    public String toXMLString(String feat) {
        String name = "directed_brownian";
        String retVal = "<" + name + " features=\"" + feat + "\" speed=\"" + this.speed;
        
        retVal += "\" heading=\"" + this.heading * Math.toRadians(3600) + "\" x=\"" + this.x + "\" y=\"" + this.y + "\"/>";
        
        return retVal;
    }
}
