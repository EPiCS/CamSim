package epics.common;

import java.awt.geom.Point2D;
import java.util.List;

import epics.camsim.core.SimCore;

public abstract class AbstractMovement {
    protected double x; // meters (horizontal)
    protected double y; // meters (vertical)
    protected double heading; // radians ( 0 is north, PI/2 is east )
    protected double speed; // meters per second
    protected RandomNumberGenerator randomGen;
    protected SimCore sim;
    
    public AbstractMovement(double x, double y, double heading, double speed, RandomNumberGenerator rg, SimCore sim){
        this.x = x;
        this.y = y;
        this.heading = heading;
        this.speed = speed;
        this.randomGen = rg;
        this.sim = sim;
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
    
    public double[] getBoundaries(){
        double[] b = new double[4];
        b[0]=sim.get_max_x();
        b[1]=sim.get_min_x();
        b[2]=sim.get_max_y();
        b[3]=sim.get_min_y();
        return b;
    }
    
    public void checkBoundaryCollision(double x_move, double y_move){
     // If we breach any boundary, bounce off at a slightly randomised angle
        if (this.x > sim.get_max_x() || this.x < sim.get_min_x() ||
                this.y > sim.get_max_y() || this.y < sim.get_min_y()){
            this.heading += getTurnaroundAngle(); 
            
            // Undo move across boundary
            this.x -= x_move; 
            this.y -= y_move;
        }
    }
    
    public abstract List<Point2D> getWaypoints();
    
    public abstract void update();

    public abstract String toXMLString(String feat);
}
