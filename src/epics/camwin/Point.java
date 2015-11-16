package epics.camwin;

import java.awt.geom.Ellipse2D;

/**
 * A point in 2D space, used for drawing.
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public class Point extends Ellipse2D.Double
{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for Point
     * @param x
     * @param y
     * @param r
     */
    public Point(double x, double y, double r)
	{
		super(x-r, y-r, 2*r, 2*r);
	}
}