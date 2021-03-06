package epics.movement;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import epics.camsim.core.SimCore;
import epics.common.*;
import epics.common.RandomUse.USE;

/**
 * Brownian movement implementation for objects in the CamSim simulation environment
 * @author Lukas Esterle <lukas [dot] esterle [at] aau [dot] at>
 *
 */
public class Brownian extends AbstractMovement{

    double mean = 0.0;
    double std = 1.0;
    int step = 0;
    
    double initX;
    double initY;
    
    /**
     * Constructor for Brownian
     * @param x x-coordinate of initial (starting) position
     * @param y y-coordinate of initial (starting) position
     * @param heading initial heading direction
     * @param speed initial speed
     * @param rg random number generator
     * @param sim simulation environment
     * @param mean mean value for x and y
     * @param std standard deviation for x and y.
     */
    public Brownian(double x, double y, double heading, double speed, RandomNumberGenerator rg, SimCore sim,  double mean, double std){
        super(x, y, heading, speed, rg, sim);
        this.initX = x;
        this.initY = y;
        this.step = 0;
        this.mean = mean;
        this.std = std;
    }
    
//    public Brownian(double x, double y, double heading, double speed, RandomNumberGenerator rg, SimCore sim){
//        super(x, y, heading, speed, rg, sim);
//    }
    
    @Override
    public List<Point2D> getWaypoints() {
        return new ArrayList<Point2D>();
    }

    @Override
    public void update() {
        double xran = randomGen.nextGaussian(std, mean, USE.MOVE);//  - (std/2);
        double yran = randomGen.nextGaussian(std, mean, USE.MOVE);//  - (std/2);
               
        System.out.println(xran + ";" + yran);
        
        x = x + xran;
        y = y + yran ;
        
        checkBoundaryCollision(xran, yran);
        step ++;
    }

    @Override
    public String toXMLString(String feat) {
        String indent = "                    ";
        String name = "brownian_motion";
        String retVal = "<" + name + " features=\"" + feat + "\" speed=\"" + this.speed+ "\" mean=\"" + this.mean + "\" std=\"" + this.std+ "\"";
        
        retVal += "\n"+indent+"</brownian_motion>";
        
        return retVal;
    }
    
}
