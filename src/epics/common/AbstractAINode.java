package epics.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractAINode {

	protected RandomNumberGenerator randomGen;
	
	protected Map<String, Double> visionGraph = new HashMap<String, Double>();
    protected Map<ITrObjectRepresentation, Double> lastConfidence = new HashMap<ITrObjectRepresentation, Double>();
    protected Map<List<Double>, ITrObjectRepresentation> tracedObjects = new HashMap<List<Double>, ITrObjectRepresentation>();
    protected Map<ITrObjectRepresentation, ICameraController> searchForTheseObjects = new HashMap<ITrObjectRepresentation, ICameraController>();
    protected Map<ITrObjectRepresentation, Map<ICameraController, Double>> biddings = new HashMap<ITrObjectRepresentation, Map<ICameraController, Double>>();
    protected Map<ITrObjectRepresentation, List<String>> advertised = new HashMap<ITrObjectRepresentation, List<String>>();
    protected Map<ITrObjectRepresentation, Integer> runningAuction = new HashMap<ITrObjectRepresentation, Integer>();
    protected Map<ITrObjectRepresentation, Double> reservedResources = new HashMap<ITrObjectRepresentation, Double>();
    protected Map<ITrObjectRepresentation, Integer> stepsTillFreeResources = new HashMap<ITrObjectRepresentation, Integer>();
	protected Map<ITrObjectRepresentation, Integer> stepsTillBroadcast = new HashMap<ITrObjectRepresentation, Integer>();
    protected Map<ITrObjectRepresentation, ITrObjectRepresentation> wrongIdentified = new HashMap<ITrObjectRepresentation, ITrObjectRepresentation>();
    protected Map<IMessage, Integer> delayedCommunication = new HashMap<IMessage, Integer>();
    protected ICameraController camController;
    protected ITrObjectRepresentation trObject;
    double last_confidence = 0;
	protected int sentMessages;
	
	protected double _receivedUtility;
	protected int _nrBids;
	protected double _paidUtility;
	protected int tmpTotalComm;
	protected double tmpTotalUtil;
	protected double tmpTotalRcvdPay;
	protected double tmpTotalPaid;
	protected int tmpTotalBids;
    
	protected IBanditSolver banditSolver;
    
    
    protected int addedObjectsInThisStep = 0;
	
	
	/** This constructor simply calls instantiateAINode(). Overriding classes
	 * should only call super and do real handling in instantiateAINode().
	 * This is painful but is to enforce these arguments in the constructor. */
	public AbstractAINode(int comm, boolean staticVG, Map<String, Double> vg, IRegistration r, RandomNumberGenerator rg){
    	instantiateAINode(comm, staticVG, vg, r, rg);
    }

	public AbstractAINode(AbstractAINode old){
		//super(old.getBanditSolver(), old.getComm());
		instantiateAINode(old);
	}
	
	public AbstractAINode(int comm, boolean staticVG, Map<String, Double> vg,
			IRegistration r, RandomNumberGenerator rg, IBanditSolver bs){
		//super(6, 0.01, comm);
		banditSolver = bs;
		instantiateAINode(comm, staticVG, vg, r, rg);
	}
	
	public void instantiateAINode(AbstractAINode ai){
    	this.lastConfidence = ai.lastConfidence;
		this.tracedObjects = ai.tracedObjects;
		this.searchForTheseObjects = ai.searchForTheseObjects;
		this.biddings = ai.biddings;
		this.advertised = ai.advertised;
		this.runningAuction = ai.runningAuction;
		this.reservedResources = ai.reservedResources;
		this.stepsTillFreeResources = ai.stepsTillFreeResources;
		this.stepsTillBroadcast = ai.stepsTillBroadcast;
		this.wrongIdentified = ai.wrongIdentified;
		this.delayedCommunication = ai.delayedCommunication;
		this.trObject = ai.trObject;
		this.last_confidence = ai.last_confidence;
		this.camController = ai.camController;
		this.visionGraph = ai.visionGraph;
		this.sentMessages = ai.sentMessages;
		this.randomGen = ai.randomGen;
		this._receivedUtility = ai._receivedUtility;
		this._nrBids = ai._nrBids;
		this._paidUtility = ai._paidUtility;
		this.tmpTotalComm = ai.tmpTotalComm;
		this.tmpTotalUtil = ai.tmpTotalUtil;
		this.tmpTotalRcvdPay = ai.tmpTotalRcvdPay;
		this.tmpTotalPaid = ai.tmpTotalPaid;
		this.tmpTotalBids = ai.tmpTotalBids;
		
		this.banditSolver = ai.banditSolver;
    }
	
	/** Subclasses implement this instead of a constructor. */
	public abstract void instantiateAINode(int comm, boolean staticVG, 
			Map<String, Double> vg, IRegistration r, RandomNumberGenerator rg);
	
	public abstract ITrObjectRepresentation getTrackedObject();
    
    public abstract Map<List<Double>, ITrObjectRepresentation> getTracedObjects();

    public abstract IMessage receiveMessage( IMessage message );

//    public abstract void addVisibleObject(ITrObjectRepresentation rto);

    public abstract void removeVisibleObject(ITrObjectRepresentation rto);

    public abstract Map<String,Double> getVisionGraph();

    public abstract void setController( ICameraController controller );

    public abstract void update();

    public abstract double getUtility();
    
    public abstract double getReceivedUtility();
    
    public abstract int getNrOfBids();

    public abstract void strengthenVisionEdge( String name );

    public abstract Map<ITrObjectRepresentation, ICameraController> getSearchedObjects();
    
    public abstract int getComm();

	public abstract int currentlyMissidentified();
	
	public abstract void setComm(int com);

	public IBanditSolver getBanditSolver() {
		return banditSolver;
	}
	
	public int getSentMessages(){
		return sentMessages;
	}

	public abstract double getPaidUtility();

	public abstract double getTmpTotalUtility();

	public abstract double getTmpTotalPaid();

	public abstract double getTmpTotalRcvPay();

	public abstract int getTmpTotalComm();
	
	public abstract int getTmpTotalBids();
}
