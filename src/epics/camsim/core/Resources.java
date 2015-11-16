package epics.camsim.core;

/**
 * resource pool for the cameras - NOT USED!!
 * 
 * @author Lukas Esterle <lukas [dot] esterle [at] aau [dot] at>
 *
 */
public class Resources {
	private final double AMOUNT = 1.0;
	private double resources = AMOUNT;
	
	/**
	 * reduce resources by amount
	 * @param amount
	 */
	public void reduceResources(double amount){
		resources = resources - amount;
	}
	
	/**
	 * increase resources by amount
	 * @param amount
	 */
	public void addResources(double amount){
		resources = resources + amount;
		if(resources > AMOUNT){
			resources = AMOUNT;
		}
	}
	
	/**
	 * return currently available resources
	 * @return
	 */
	public double getAvailableResources(){
		return resources;
	}

	/**
	 * return all resrouces (free and used) of camera
	 * @return
	 */
	public double getAllResources() {
		return this.AMOUNT;
	}
	
}
