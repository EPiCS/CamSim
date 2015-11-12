package epics.camsim.core;

public class Location implements Comparable<Location>{
    double x;
    double y;
    
    public double getX(){
        return x;
    }
    public double getY(){
        return y;
    }
    
    public Location(double x, double y){
        this.x = x;
        this.y = y;
    }
    
    @Override
    public Location clone(){
        return new Location(x, y);
    }
    
     
    @Override
    public boolean equals(Object obj){
      if (obj == null) return false;
      if (this == obj) return true;
      if (!(obj instanceof Location)) return false;

      Location that = (Location)obj;
      if(that.x == this.x){
          if(that.y == this.y){
              return true;
          }
          else{
              return false;
          }
      }
      else{
          return false;          
      }
    }
    
    @Override
    public int hashCode(){
      return Double.valueOf(x*y).hashCode();
    }

    @Override
    /**
     * Returns 0 if coordinates are equal. 1 if x is not equal and -1 if x is equal but y is not.
     */
    public int compareTo(Location o) {
        if (this.x == o.x){
            if(this.y == o.y){
                return 0;
            }
            else{
                return -1;
            }
        }
        else{
            return 1;
        }
    }
    
    public double angleTo(Location l){
        double[] d = distanceTo(l);
        double angle = 0.0; // = Math.toDegrees(Math.atan(d[0]/d[1]))*2;
        
        if(d[0] == 0){
            if(d[1] == 0){
                return 0.0;
            }
            else if(d[1] > 0){ //straight up
                return 90;
            }
            else{
             // straight down
                return -90;
            }
        }
        
        if(d[1] == 0){
            if(d[0] == 0){ //same location
                return 0.0;
            }
            else if(d[0] > 0.0){ //left of current point
                return 180;
            }
            else{ //right of current point
                return 0.0;
            }
        }
        
        angle = Math.toDegrees(Math.atan(d[1]/d[0]));
        if(d[0] > 0){
            //left of current
            angle += 180;
        }
//        else if(d[0] < 0){
//            
//        }
        
       
        
        if(Double.isNaN(angle)){
            angle = 0.0;
        }
        if(angle < 0){
            angle += 360;
        }
        
        //System.out.println(" atan: " + Math.atan(d[0]/d[1]) + " atan2: " + Math.atan2(d[0], d[1]) + "\n atan-degree: "+ Math.toDegrees(Math.atan(d[0]/d[1])) + "\n atan2-degree: " + Math.toDegrees(Math.atan2(d[1], d[0])) + "\n FINAL angle: " + angle); 
        
        
        return angle;
    }
    
    @Override
    public String toString(){
        return x + " " + y;
    }
    
    public void moveBy(double x, double y){
        this.x += x;
        this.y += y;
    }
    
    /**
     * returns distances to a given location
     * @param l a distant location the distance is calculated to
     * @return a double-array size 3 containing distances between x, y and euclidean distance between the two points
     *              distances from this point to a distant point l are calculated as this.x - l.x and this.y - l.y respectively.
     *              as the location is based on the upper left corner, a negative outcome means, the other point is located RIGHT of this one (and vice versa) for the x coordinate and BELOW for the y corrdinate.
     */
    public double[] distanceTo(Location l){
        double[] res = new double[3];
        res[0] = this.x - l.x; //adjecent (ankathete)
        res[1] = this.y - l.y; //opposite (gegenkathete) 
        res[2] = Math.sqrt(Math.pow(this.x-l.x, 2) + Math.pow(this.y-l.y, 2)); // hypotenuse
        return res;
    }
}