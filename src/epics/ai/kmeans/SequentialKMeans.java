/* by Hedde Bosman */

/*
	implemented follwing the pseudo-code at:
		http://www.cs.princeton.edu/courses/archive/fall08/cos436/Duda/C/sk_means.htm
*/

package epics.ai.kmeans;

import java.util.Vector;
import java.util.Iterator;
import java.util.HashMap;

// this class is not generic anymore... it clones the vision graph when adding a new cluster

public class SequentialKMeans {
	private int maxNumClusters;
	private Vector<Cluster> clusters; // the clusters :P
	
	// if a cluster has not seen a new point associated with it for CLUSTER_LIFETIME datapoints, we remove it...
	public final static int CLUSTER_LIFETIME = 100;
	
	public final static double MINIMUM_INTER_CLUSTER_DISTANCE = 0.05;
	// CLUSTER_ADD_FACTOR = 0.5 => 0.5*(maxDistance between clusters), so maximum non-overlapping radius
	//   any larger values means that they all overlap
	public final static double CLUSTER_ADD_FACTOR = 0.5;
	

	public SequentialKMeans(int numCoordinates) {
		clusters = new Vector<Cluster>();
		// only add one cluster, the adaptive 'add' should add new ones on demand
		Cluster c = new Cluster(numCoordinates);
		c.initRandomCenter();
		clusters.add(c);
		
		maxNumClusters = 10;
	}
	// constructor, requires #clusters and #coordinates
	public SequentialKMeans(int numCoordinates, int numClusters) {
		clusters = new Vector<Cluster>();

		for (int i = 0; i < numClusters; i++) {
			Cluster c = new Cluster(numCoordinates);
			c.initRandomCenter();
			clusters.add(c);
		}

		maxNumClusters = 10;
	}
	
	public double getMaximumInterClusterDistance() {
		double dist = MINIMUM_INTER_CLUSTER_DISTANCE;
		for (int i = 0; i < clusters.size(); i++) {
			for (int j = i+1; j < clusters.size(); j++) {
				double tmpDist = clusters.get(i).distanceTo(clusters.get(j));
				if (dist < tmpDist)
					dist = tmpDist;
			}
		}
		return dist;
	}
	
	public int getClosestClusterIndex(ClusterPoint x) {
		double dist = Double.MAX_VALUE;
		int idx = -1;
		
		// for each cluster
		for (int i = 0; i < clusters.size(); i++) {
			// determine if the center is closer than other clusters
			double tmpDist = clusters.get(i).distanceTo(x);
			if (dist > tmpDist) {
				// if so, this is the closest clusters
				dist = tmpDist;
				idx = i;
			}
		}
		return idx;
	}
	public Cluster getClosestCluster(ClusterPoint x) {
		return clusters.get( getClosestClusterIndex(x) );
	}
	
	public Iterator<Cluster> getClusterIterator() {
		return clusters.iterator();
	}
	
	// add a single point to the cluster it is closest to
	public void add(ClusterPoint x) {
		int idx = getClosestClusterIndex(x);
		// yes, less efficient than inlining the getClosestClusterIndex
		// but, saves some code duplication
		double dist = clusters.get(idx).distanceTo(x); 
		
		
		if (dist < CLUSTER_ADD_FACTOR*this.getMaximumInterClusterDistance()) {
			// add it to the closest cluster
			clusters.get(idx).add(x);
		} else {
			// should add a new cluster, because it's outside of the radius of any other, i.e. an outlier
			if (clusters.size() < maxNumClusters) {
				// yes, we are not yet at our maximum number of clusters!
				Cluster c = new Cluster(x);
				c.visionGraph = (HashMap) ((HashMap)clusters.get(idx).visionGraph).clone(); // clone the vision graph of the closest cluster
				clusters.add(c);
//				System.out.println("CLUSTER ADD "+c);
			} else {
				// we're already at the max number of clusters, so add it to the closest cluster
				// this is basically where we become a voronoi diagram
				clusters.get(idx).add(x);
			}
		}//*/
		
		// keep track of cluster 'relevance' and remove irrelevant clusters
		if (clusters.size() > 1) { // at least have one cluster... :P
			Iterator<Cluster> it = clusters.iterator();
			while (it.hasNext()) {
				Cluster c = it.next();
				if (c.decrementRelevance()) {
					//System.out.println("Removing cluster "+c);
					it.remove();
//					System.out.println("CLUSTER REMOVE "+c);
					// maybe merge it?
				}
			}
		}
	}
	
	public String toString() {
		String r = "";
		
		for (int i = 0; i < clusters.size(); i++) {
			r += clusters.get(i).toString() + "\n";
		}

		return r;
	}
};

