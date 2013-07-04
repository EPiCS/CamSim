package epics.movement;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import epics.camsim.core.SimCore;
import epics.common.AbstractMovement;
import epics.common.RandomNumberGenerator;
import epics.common.RandomUse.USE;

public class DirectedBrownian extends AbstractMovement{
    public DirectedBrownian(double x, double y, double heading, double speed, RandomNumberGenerator rg, SimCore sim){
        super(x, y, heading, speed, rg, sim);
    }

    @Override
    public List<Point2D> getWaypoints() {
        return new ArrayList<Point2D>();
    }

    @Override
    public void update() {
        double x_move = 0;
        double y_move = 0;
        
        double sran = randomGen.nextDouble(USE.MOVE);
        double hran = (randomGen.nextDouble(USE.MOVE));
        speed += (sran - 0.5) / 10;
        heading += (hran - 0.5) / 2; 
        
        System.out.println(sran + " - " + hran);
        
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
