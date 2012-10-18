/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package epics.camsim.core;

import epics.common.ICameraController;
import epics.common.ITrObjectRepresentation;
import java.util.List;

/**
 *  This class is a representation of an TraceabeObject FROM THE POINT OF VIEW
 * OF ONE SPECIFIC CAMERA
 * @author Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public class TraceableObjectRepresentation implements ITrObjectRepresentation {

    TraceableObject to; // Not accesible through interface!

    // private double x/y relative to camera?
    private List<Double> features;

    public TraceableObjectRepresentation( TraceableObject to, List<Double> features ){
        this.to = to;
        this.features = features;
    }

    public TraceableObject getTraceableObject(){
        return this.to;
    }

    public List<Double> getFeatures() {
        return this.features;
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
    
}
