/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 *///

package epics.camwin;

/**
 *
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */

import java.awt.geom.Ellipse2D;

/**
 * A point in 2D space, used for drawing.
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public class Point extends Ellipse2D.Double
{
	public Point(double x, double y, double r)
	{
		super(x-r, y-r, 2*r, 2*r);
	}
}