package epics.common;

import java.awt.geom.Point2D;
import java.util.List;

import epics.camsim.core.SimCore;

/**
 * AbstractMovement class to implement differnt types of movement models for different objects
 * @author Lukas Esterle <lukas [dot] esterle [at] aau [dot] at>
 */
public abstract class AbstractMovement {
    protected double x; // meters (horizontal)
    protected double y; // meters (vertical)
    protected double heading; // radians ( 0 is north, PI/2 is east )
    protected double speed; // meters per second
    protected RandomNumberGenerator randomGen;
    protected SimCore sim;
    
    /**
     * 
     * Constructor for AbstractMovement
     * @param x x-coordinate current (starting) point of object
     * @param y y-coordinate current (starting) point of object
     * @param heading direction the object heads towards in radians
     * @param speed speed of the objects
     * @param rg random number generator - usually the same for all objects
     * @param sim simulation core of the current simulation
     */
    public AbstractMovement(double x, double y, double heading, double speed, RandomNumberGenerator rg, SimCore sim){
        this.x = x;
        this.y = y;
        this.heading = heading;
        this.speed = speed;
        this.randomGen = rg;
        this.sim = sim;
    }
    
    /**
     * Gets an angle to turn around by with some partial added randomness  
     * @return angle to bounce of wall
     */
    public double getTurnaroundAngle() {
        // Turn around 180 degrees, add a bit of angle for randomness
        double angle = Math.PI; // Turn 180 degrees
        angle += (randomGen.nextDouble(RandomUse.USE.TURN)*2-1.0) * Math.PI / 6.0;
        return angle;
    }
    
    /**
     * x-coordinate of current location
     * @return x-coordinate
     */
    public double getX(){
        return this.x;
    }
    
    /**
     * y-coordinate of current location
     * @return y-coordinate
     */
    public double getY(){
        return this.y;
    }

    /**
     * current heading
     * @return heading
     */
    public double getHeading(){
        return this.heading;
    }
    
    /**
     * returns the boundaries of the current simulation environment.
     * max x, min x, max y, min y
     * @return array with the boundaryies (max x, min x, max y, min y)
     */
    public double[] getBoundaries(){
        double[] b = new double[4];
        b[0]=sim.get_max_x();
        b[1]=sim.get_min_x();
        b[2]=sim.get_max_y();
        b[3]=sim.get_min_y();
        return b;
    }
    
    /**
     * checks if a boundary collision will occur after the next move and makes the accoring bounce if necessary
     * @param x_move future move towards x
     * @param y_move future move towards y
     */
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
    
    /**
     * Returns the list of waypoints used by the object
     * @return list of waypoints
     */
    public abstract List<Point2D> getWaypoints();
    
    /**
     * moves the object
     */
    public abstract void update();

    /**
     * writes the movement information to xml
     * @param feat
     * @return
     */
    public abstract String toXMLString(String feat);
}
