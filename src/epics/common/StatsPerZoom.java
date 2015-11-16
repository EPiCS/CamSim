package epics.common;

import java.util.ArrayList;
import java.util.List;


/**
 * track information gathered for specific zoom.
 * this is stored at the camera level
 * 
 * @author Lukas Esterle <lukas [dot] esterle [at] aau [dot] at>
 *
 */
public class StatsPerZoom implements Comparable<StatsPerZoom>{

    double zoom;
    List<Double> utility;
    List<Integer> trackedObjects;
    List<Integer> wonAuctions;
    List<Integer> lostAuctions;
    double totUtil;
    double totTracked;
    double totWon;
    double totLost;
    
    double updates;
    
    double deviationLimit = 0.5;
    
    /**
     * 
     * Constructor for StatsPerZoom.java
     * @param e
     * @param dev
     */
    public StatsPerZoom(double e, double dev){
        zoom = e;
        init();
        deviationLimit = dev;
    }
    
    /**
     * 
     * Constructor for StatsPerZoom.java
     * @param e
     */
    public StatsPerZoom(double e) {
        zoom = e;
        init();
    }
    
    /**
     * initiates the statsperzoom object
     */
    public void init(){
        utility = new ArrayList<Double>();
        trackedObjects = new ArrayList<Integer>();
        wonAuctions = new ArrayList<Integer>();
        lostAuctions = new ArrayList<Integer>();
        totUtil = 0;
        totTracked = 0.0;
        totWon = 0.0;
        totLost = 0.0;
        updates = 0;
    }
    
    /**
     * update for current timestep
     * @param u
     * @param t
     * @param w
     * @param l
     */
    public void update(double u, int t, int w, int l){
        utility.add(u);
        wonAuctions.add(w);
        lostAuctions.add(l);
        trackedObjects.add(t);
        
        totUtil +=u;
        totTracked += t;
        totWon += w;
        totLost += l;
        
        updates++;
    }
    
    /**
     * returns average utility for this camera per update
     * @return
     */
    public double getAvgUtility(){
        return totUtil/updates;
    }
    
    /**
     * returns the average tracked objects per update
     * @return
     */
    public double getAvgTracked(){
        return totTracked/updates;
    }
    
    /**
     * returns the average won auctions per update
     * @return
     */
    public double getAvgWon(){
        return totWon/updates;
    }
    
    /**
     * returns the averag lost auctions per uupdate
     * @return
     */
    public double getAvgLost(){
        return totLost/updates;
    }
    
    /**
     * returns all utilities achieved by this zoom
     * @return
     */
    public List<Double> getUtility(){
        return utility;
    }
    
    /**
     * returns all number of tracked objects for this zoom 
     * @return
     */
    public List<Integer> getTracked(){
        return trackedObjects;
    }
    
    /**
     * returns all numbers of won auctions for this zoom
     * @return
     */
    public List<Integer> getWon(){
        return wonAuctions;
    }
    
    /**
     * returns all numbers of lost auctions by this zoom
     * @return
     */
    public List<Integer> getLost(){
        return lostAuctions;
    }

    @Override
    public int compareTo(StatsPerZoom o) {
        if(((this.zoom - deviationLimit) < (o.zoom))){
            return -1;
        }
        if(((this.zoom + deviationLimit) > (o.zoom))){
            return +1;
        }
        if(((this.zoom + deviationLimit) > (o.zoom)) && (((this.zoom - deviationLimit) < (o.zoom)))){
            return 0;
        }
        
        return -2;
    }
    
    @Override
    public boolean equals(Object obj){
      if (obj == null) return false;
      if (this == obj) return true;
      if (!(obj instanceof StatsPerZoom)) return false;

      StatsPerZoom o = (StatsPerZoom)obj;
      if(this.zoom == o.zoom){ //((this.energy + deviationLimit) > (o.energy)) && (((this.energy - deviationLimit) < (o.energy)))){
          return true;
      }
      else{
          return false;          
      }
    }
    
    @Override
    public int hashCode(){
      return Double.valueOf(zoom).hashCode();
    }
    

}
