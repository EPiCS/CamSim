package epics.camsim.core;

import java.util.List;

import epics.common.ITrObjectRepresentation;

/**
 *  This class is a representation of an TraceabeObject FROM THE POINT OF VIEW
 * OF ONE SPECIFIC CAMERA
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public class TraceableObjectRepresentation implements ITrObjectRepresentation {

    TraceableObject to; // Not accessible through interface!

    // private double x/y relative to camera?
    private List<Double> features;

	private double _price;

	/**
	 * Constructor for TraceableObjectRepresentation - camera internal represenation of object
	 * @param to actual traced object
	 * @param features unique features/id of object
	 */
    public TraceableObjectRepresentation( TraceableObject to, List<Double> features ){
        this.to = to;
        _price = 0;
        this.features = features;
    }

    /**
     * returns the actual object
     * @return
     */
    public TraceableObject getTraceableObject(){
        return this.to;
    }

    public List<Double> getFeatures() {
        return this.features;
    }
    
    public Location getLocation(){
        return new Location(to.getTotalX(), to.getTotalY()); // (to.getX(), to.getY()); // 
    }

    public Location getCenterBasedLocation() {
        return new Location(to.getX(), to.getY()); 
    }
    
    @Override
    public boolean equals(Object o){
    	if(o instanceof TraceableObjectRepresentation){
//    		if(this.getTraceableObject().equals(((TraceableObjectRepresentation) o).getTraceableObject())){
    		if(this.hashCode() == (((TraceableObjectRepresentation) o).hashCode())){
    			return true;
    		}
    		else{
    			return false;
    		}
    	}
    	else{
    		return super.equals(o);
    	}
    }

    @Override
    public int hashCode(){
//    	return to.hashCode(); //super.hashCode();
    	int hash = 7;
    	  hash = 31 * hash + 
    	    (null == this.to ? 0 : this.to.hashCode());
    	  hash = 31 * hash + 
    	    (null == this.features ? 0 : this.features.hashCode());

    	  return hash;
    }

	@Override
	public void setPrice(double price) {
		_price = price;
	}

	@Override
	public double getPrice() {
		return _price;
	}

    
    
    
}


