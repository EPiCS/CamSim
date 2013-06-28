/* by Hedde Bosman */

package epics.ai.kmeans;

public class ClusterPoint {
	public double[] coordinates; // public = being lazy
	
	// constructor by number of coordinates (no initalization)
	public ClusterPoint(int numCoordinates) {
		this.coordinates = new double[numCoordinates];
	}
	
	// constructor by array of doubles that represent coordinates
	public ClusterPoint(double[] coords) {
		this.coordinates = coords;
	}
	
	// determine euclidian distance from this point to point y
	// returns NaN when number of coordinates do not match (different spaces)
	public double distanceTo(ClusterPoint y) {
		if (y.coordinates.length != this.coordinates.length) {
			return Double.NaN;
		}
		
		double dist = 0.0;
		
		for (int i = 0; i < this.coordinates.length; i++) {
			dist += Math.pow((this.coordinates[i]-y.coordinates[i]), 2);
		}
		dist = Math.sqrt(dist);
		
		return dist;
	}
	
	public String toString() {
		String r = "[ ";
		for (int i = 0; i < this.coordinates.length; i++) {
			r += this.coordinates[i]+" ";
		}
		r += "]";
		return r;
	}
}

