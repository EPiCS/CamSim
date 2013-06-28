/* by Hedde Bosman */

package epics.ai.kmeans;


import java.util.Map;
import java.util.HashMap;

// this class is not 'generic' anymore... due to the vision graph being added here...

public class Cluster {
	private ClusterPoint center; // the mean/center of the cluster
	private double count; // the number of cluster points associated to this mean
	private int relevance;

	// again, public = lazy...
	public Map<String, Double> visionGraph;
	
	public Cluster(int numCoordinates) {
		count = 0.0;
		center = new ClusterPoint(numCoordinates);
		visionGraph = new HashMap<String, Double>();
		this.resetRelevance();
	}
	public Cluster(ClusterPoint x) {
		count = 1.0;
		center = x;
		visionGraph = new HashMap<String, Double>();
		this.resetRelevance();
	}
	
	// initiate the cluster center by using random values between [-1,1]
	public void initRandomCenter() {
		for (int i = 0; i < center.coordinates.length; i++) {
			center.coordinates[i] = Math.random()*2.0-1.0; // random() is 0-1
		}
	}
	
	// get the distance from the cluster center to point x
	public double distanceTo(ClusterPoint x) {
		return center.distanceTo(x);
	}
	public double distanceTo(Cluster x) {
		return center.distanceTo(x.center);
	}
	
	// should get called every time a point has been added to THIS cluster
	public void resetRelevance() {
		this.relevance = SequentialKMeans.CLUSTER_LIFETIME;
	}
	// should get called every time a point has been added to ANY cluster
	public boolean decrementRelevance() {
		return (--relevance < 0);
	}
	
	/////////////////////////////////////////////////////////
	// functions to add a point to this cluster
	/////////////////////////////////////////////////////////
	
	// if factor is fixed, we forget
	public void addForgetfull(ClusterPoint x, double factor) {
		// for each coordinate (x,y,...)
		for (int i = 0; i < center.coordinates.length; i++) {
			// add factor * diff(this,x)^2
			center.coordinates[i] += factor * (x.coordinates[i] - center.coordinates[i]);
		}
		this.resetRelevance();
	}

	// fixed forgetting factor
	public void addForgetfull(ClusterPoint x) {
		this.addForgetfull(x, 0.01);
	}
	
	// changing forgetting factor (learning rate is slower when number of data points increases)
	public void add(ClusterPoint x) {
		count += 1.0;
		double factor = 1.0 / count;
		this.addForgetfull(x, factor);
	}
	
	public String toString() {
		String r = "Cluster, center=[ ";
		for (int i = 0; i < this.center.coordinates.length; i++) {
			r += this.center.coordinates[i] + " ";
		}
		r += "]; count="+this.count;
		return r;
	}
}

