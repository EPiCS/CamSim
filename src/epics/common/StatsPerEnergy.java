package epics.common;

import java.util.ArrayList;
import java.util.List;

import epics.camsim.core.Location;

public class StatsPerEnergy implements Comparable<StatsPerEnergy>{

    double energy;
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
    
    public StatsPerEnergy(double e, double dev){
        energy = e;
        init();
        deviationLimit = dev;
    }
    
    public StatsPerEnergy(double e) {
        energy = e;
        init();
    }
    
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
    
    public double getAvgUtility(){
        return totUtil/updates;
    }
    
    public double getAvgTracked(){
        return totTracked/updates;
    }
    
    public double getAvgWon(){
        return totWon/updates;
    }
    
    public double getAvgLost(){
        return totLost/updates;
    }
    
    public List<Double> getUtility(){
        return utility;
    }
    
    public List<Integer> getTracked(){
        return trackedObjects;
    }
    
    public List<Integer> getWon(){
        return wonAuctions;
    }
    
    public List<Integer> getLost(){
        return lostAuctions;
    }

    @Override
    public int compareTo(StatsPerEnergy o) {
        if(((this.energy - deviationLimit) < (o.energy))){
            return -1;
        }
        if(((this.energy + deviationLimit) > (o.energy))){
            return +1;
        }
        if(((this.energy + deviationLimit) > (o.energy)) && (((this.energy - deviationLimit) < (o.energy)))){
            return 0;
        }
        
        return -2;
    }
    
    @Override
    public boolean equals(Object obj){
      if (obj == null) return false;
      if (this == obj) return true;
      if (!(obj instanceof StatsPerEnergy)) return false;

      StatsPerEnergy o = (StatsPerEnergy)obj;
      if(this.energy == o.energy){ //((this.energy + deviationLimit) > (o.energy)) && (((this.energy - deviationLimit) < (o.energy)))){
          return true;
      }
      else{
          return false;          
      }
    }
    
    @Override
    public int hashCode(){
      return Double.valueOf(energy).hashCode();
    }
    

}
