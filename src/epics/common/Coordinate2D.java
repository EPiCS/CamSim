/**
 * 
 */
package epics.common;

import java.io.Serializable;

/**
 * @author Danilo Pianini
 *
 */
public class Coordinate2D implements Serializable {

	private static final long serialVersionUID = -1574428171808612137L;
	
	private final double x, y;
	private String stringCache;
	
	public Coordinate2D(double xc, double yc) {
		x = xc;
		y = yc;
	}

	/**
	 * @return the x
	 */
	public double getX() {
		return x;
	}

	/**
	 * @return the y
	 */
	public double getY() {
		return y;
	}
	
	@Override
	public String toString(){
		if(stringCache == null){
			stringCache = "["+x+","+y+"]";
		}
		return stringCache;
	}

}
