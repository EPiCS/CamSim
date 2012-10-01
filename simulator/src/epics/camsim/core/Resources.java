package epics.camsim.core;

public class Resources {
	private final double AMOUNT = 1.0;
	private double resources = AMOUNT;
	
	public void reduceResources(double amount){
		resources = resources - amount;
	}
	
	public void addResources(double amount){
		resources = resources + amount;
		if(resources > AMOUNT){
			resources = AMOUNT;
		}
	}
	
	public double getAvailableResources(){
		return resources;
	}

	public double getAllResources() {
		return this.AMOUNT;
	}
	
}
