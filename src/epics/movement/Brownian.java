package epics.movement;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import epics.camsim.core.SimCore;
import epics.common.*;
import epics.common.RandomUse.USE;

public class Brownian extends AbstractMovement{

    double mean = 0.0;
    double std = 1.0;
    
    public Brownian(double x, double y, double heading, double speed, RandomNumberGenerator rg, SimCore sim, double mean, double std){
        super(x, y, heading, speed, rg, sim);
        this.mean = mean;
        this.std = std;
    }
    
    @Override
    public List<Point2D> getWaypoints() {
        return new ArrayList<Point2D>();
    }

    @Override
    public void update() {
        x = x + randomGen.nextGaussian(std, mean, USE.NORMALDIST);
        y = y + randomGen.nextGaussian(std, mean, USE.NORMALDIST);
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
