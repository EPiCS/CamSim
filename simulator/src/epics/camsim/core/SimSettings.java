package epics.camsim.core;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.naming.LimitExceededException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author Lukas Esterle <Lukas.Esterle@aau.at> & Marcin Bogdanski <mxb039@cs.bham.ac.uk>
 */
public class SimSettings {
    
    public static final int FIX_LIMIT = 0;
    public int usePredefVG = -1; //-1 = as defined in "static" attribute in file, 0 = static VG as predefined in file, 1 = do not use VG if defined in file (dynamic though), 2 = dynamic but starting with VG as predefined in file 
    private boolean fixAlgo = false;
    private boolean fixComm = false;
    
    public class CameraSettings {

        public String name;
        public Double x;
        public Double y;
        public Double heading;
        public Double viewing_angle;
        public Double range;
        public String ai_algorithm;
        public Integer comm;
        public Integer limit;

        CameraSettings(){}

        CameraSettings(
                String name,
                double x, double y,
                double heading,
                double viewing_angle,
                double range,
                String ai_algorithm,
                int comm,
                int limit) {

            this.name = name;
            this.x = x;
            this.y = y;
            this.heading = heading;
            this.viewing_angle = viewing_angle;
            this.range = range;
            this.ai_algorithm = ai_algorithm;
            this.comm = comm;
            this.limit = limit;
        }

        public void printSelfToCMD(){
            System.out.println("{ name: " + name );
            System.out.println("  x: " + x );
            System.out.println("  y: " + y );
            System.out.println("  heading: " + heading );
            System.out.println("  viewing_angle: " + viewing_angle );
            System.out.println("  range: " + range );
            System.out.println("  ai_algorithm: " + ai_algorithm);
            switch(this.comm){
            case 0: System.out.println("  communication: broadcast "); break;
            case 1: System.out.println("  communication: SMOOTH "); break;
            case 2: System.out.println("  communication: STEP"); break;
            case 3: System.out.println("  communication: STATIC"); break;
            default: System.out.println("  communication: UNKNOWN "); break;
            }
            System.out.println(" limit: " + limit + " }");
        }
        
        public CameraSettings clone(){
        	return new CameraSettings(name, x, y, heading, viewing_angle, range, ai_algorithm, comm, limit);
        }
    }

    public class TrObjectSettings {

        public Double x;
        public Double y;
        public Double heading;
        public Double speed;

        /*
         * TODO: Implement features as collection
         */
        public Double features;

        TrObjectSettings(){}

        TrObjectSettings(
                double x, double y,
                double heading,
                double speed,
                double features) {

            this.x = x;
            this.y = y;
            this.heading = heading;
            this.speed = speed;
            this.features = features;
        }

        public void printSelfToCMD(){
            System.out.println("{ x: " + x );
            System.out.println("  y: " + y );
            System.out.println("  heading: " + heading );
            System.out.println("  speed: " + speed + " }");
        }
    }
    
    public class StaticVisionGraph{
    	Map<String, ArrayList<String>> vg;
    	
    	boolean isStatic = false;
    	boolean usePredefVG;
    	
    	StaticVisionGraph(){
    		vg = new HashMap<String, ArrayList<String>>();
    	}
    	
    	public void addValue(String node, ArrayList<String> neighbours){
    		vg.put(node, neighbours);
    	}
    	
    	public void printSelfToCMD(){
    		System.out.println("visiongraph stays static: " + usePredefVG);
    		for(Map.Entry<String, ArrayList<String>> kvp : vg.entrySet()){
    			System.out.print("node: " + kvp.getKey() + " - neighbours: ");
    			for(String s : kvp.getValue()){
    				System.out.print(s + "; ");
    			}
    			System.out.println("");
    		}
    	}
    }
    
    public class Event{
    	int timestep;
    	int participant; //1 = camera, 2 = object, 3=GRC
    	String name;
    	String event;
    	int duration;
    	double range;
    	double angle;
    	double heading;
    	double speed;
    	double x;
    	double y;
    	int limit;
    	int comm;
    	ArrayList<Point2D> waypoints;
    	
    	public Event(){
    	}
    	
    	public Event(int ts, int part, String n, String ev, int dur){
    		timestep = ts;
    		participant = part;
    		name = n;
    		event = ev;
    		duration = dur;
    	}
    	
    	public Event(int ts, int part, String n, String ev, double heading, double ran, double ang, double xPos, double yPos, int limit, int comm){
    		timestep = ts;
    		participant = part;
    		name = n;
    		event = ev;
    		
    		this.heading = heading;
    		range = ran;
    		angle = ang;
    		this.limit = limit;
    		x = xPos;
    		y = yPos;
    		this.comm = comm;
    	}
    	
    	public Event(int ts, int part, String n, String ev, double head, double sp, double xPos, double yPos, ArrayList<Point2D> waypoints){
    		this.waypoints = waypoints;
    		timestep = ts;
    		participant = part;
    		name = n;
    		event = ev;
    		
    		heading = head;
    		speed = sp;
    		x = xPos;
    		y = yPos;
    	}

		public void printSelfToCMD() {
			if(participant == 1){
				System.out.println("participant: camera");
			}
			else{
				System.out.println("participant: object");
			}
			System.out.println("at timestep: " + timestep);
			System.out.println("event: " + event);
			System.out.println("duration: " + duration);			
			System.out.println("x: " + x);
			System.out.println("y: " + y);
			System.out.println("name: " + name);
			System.out.println("comm: " + comm);
			System.out.println("range: " + range);
			System.out.println("angle: " + angle);
			System.out.println("heading: " + heading);
			System.out.println("speed: " + speed + "\n");
			
		}
    	
    }
    
    public Double min_x;
    public Double max_x;
    public Double min_y;
    public Double max_y;
    public ArrayList<CameraSettings> cameras = new ArrayList<CameraSettings>();
    public ArrayList<TrObjectSettings> objects = new ArrayList<TrObjectSettings>();
    public ArrayList<Event> events = new ArrayList<Event>();
    public StaticVisionGraph visionGraph;
    
    
    private String algorithm = "";
    private int communication = 0;

    public SimSettings(){}
    
    public SimSettings(String algo, String comm, int staticVG){
    	usePredefVG = staticVG;
    	if(!algo.equals("")){
    		fixAlgo = true;
    		algorithm = algo;
    	}
    	if(!comm.equals("")){
    		try{
    			communication = Integer.parseInt(comm);
    			fixComm = true;
    		}
    		catch(Exception ex){
    			fixComm =false;
    		}
    	}
    }
    
    public SimSettings(double min_x, double max_x, double min_y, double max_y, int staticVG) {
    	usePredefVG = staticVG;

        this.min_x = min_x;
        this.min_y = min_y;
        this.max_x = max_x;
        this.max_y = max_y;

        cameras.add(
                new CameraSettings(
                "Cam_01",
                10, 10,
                90,
                60,
                20,
                "passive", 0, 0));

        cameras.add(
                new CameraSettings(
                "Cam_02",
                20, 20,
                90,
                60,
                20,
                "passive", 0, 0));

        objects.add(
                new TrObjectSettings(
                0, 0,
                90,
                1,
                1));

        objects.add(
                new TrObjectSettings(
                10, 10,
                90,
                1,
                2));

    }

    public void printSelfToCMD(){

        System.out.println("min_x" + min_x);
        System.out.println("max_x" + max_x);
        System.out.println("min_y" + min_y);
        System.out.println("max_y" + max_y);

        System.out.println("Cameras:");
        for ( CameraSettings cs : this.cameras ){
            cs.printSelfToCMD();
        }

        System.out.println("Objects:");
        for ( TrObjectSettings tros : this.objects ){
            tros.printSelfToCMD();
        }
        
        
        System.out.println("\nVisionGraph:");
        if(visionGraph != null){
        	visionGraph.printSelfToCMD();
        }
        else{
        	System.out.println("VisionGraph is built dynamically!");
        }
        
        
        System.out.println("\nEvents:");
        for(Event ev : this.events){
        	ev.printSelfToCMD();
        }
        if(events.isEmpty()){
        	System.out.println("no planned events!");
        }
        System.out.println("");

    }

    public boolean loadFromXML(String filename) {

        this.cameras.clear();
        this.objects.clear();

        try {

            File fXmlFile = new File(filename);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();

            Element eRoot = doc.getDocumentElement();

            NodeList nList = doc.getElementsByTagName("simulation");
            Node nSimulation = nList.item(0);
            Element eSimulation = (Element) nSimulation;

            this.min_x = Double.parseDouble(eSimulation.getAttribute("min_x"));
            this.min_y = Double.parseDouble(eSimulation.getAttribute("min_y"));
            this.max_x = Double.parseDouble(eSimulation.getAttribute("max_x"));
            this.max_y = Double.parseDouble(eSimulation.getAttribute("max_y"));

            
            //read cameras from xml
            NodeList nCameras = doc.getElementsByTagName("camera");
            for (int temp = 0; temp < nCameras.getLength(); temp++) {

                Node nCamera = nCameras.item(temp);
                Element eCamera = (Element)nCamera;

                CameraSettings cs = new CameraSettings();

                cs.name = eCamera.getAttribute("name");
                cs.x = Double.parseDouble(eCamera.getAttribute("x"));
                cs.y = Double.parseDouble(eCamera.getAttribute("y"));
                cs.heading = Double.parseDouble(eCamera.getAttribute("heading"));
                cs.viewing_angle = Double.parseDouble(eCamera.getAttribute("viewing_angle"));
                cs.range = Double.parseDouble(eCamera.getAttribute("range"));
                if(fixAlgo){
                	cs.ai_algorithm = algorithm;
                }
                else{
                	cs.ai_algorithm = eCamera.getAttribute("ai_algorithm");
                }
                if(fixComm){
                	cs.comm = communication;
                }
                else{
                	cs.comm = Integer.parseInt(eCamera.getAttribute("comm"));
                }
                if(eCamera.hasAttribute("limit")){
                	cs.limit = Integer.parseInt(eCamera.getAttribute("limit"));
                }
                else{
                	cs.limit = new Integer(FIX_LIMIT);
                }
                
                this.cameras.add(cs);

            }

            //read objects from xml
            NodeList nObjects = doc.getElementsByTagName("object");
            for (int temp = 0; temp < nObjects.getLength(); temp++) {

                Node nObject = nObjects.item(temp);
                Element eObject = (Element)nObject;

                TrObjectSettings tros = new TrObjectSettings();

                tros.x = Double.parseDouble(eObject.getAttribute("x"));
                tros.y = Double.parseDouble(eObject.getAttribute("y"));
                tros.heading = Double.parseDouble(eObject.getAttribute("heading"));
                tros.speed = Double.parseDouble(eObject.getAttribute("speed"));
                tros.features = Double.parseDouble(eObject.getAttribute("features"));

                this.objects.add(tros);

            }
            
            //read vision graph from xml
            if(usePredefVG != 1){
	            NodeList visionG = doc.getElementsByTagName("visiongraph");
	            Node nVisionG = visionG.item(0);
	            Element eVisionG = (Element) nVisionG;
	            
	            NodeList nGraph = doc.getElementsByTagName("graphnode");
	            if(usePredefVG == -1){ //as defined in xml
		            if(nGraph.getLength() > 0){
		            	this.visionGraph = new StaticVisionGraph();
		            	visionGraph.isStatic = Boolean.parseBoolean(eVisionG.getAttribute("static"));
		            }
	            }
	            else if(usePredefVG == 0){
	            	if(nGraph.getLength() > 0){ // static VG - does not change
		            	this.visionGraph = new StaticVisionGraph();
		            	visionGraph.isStatic = true;
		            }
	            }
	            else if(usePredefVG == 2){
	            	if(nGraph.getLength() > 0){ // not a static VG (changes over time)
		            	this.visionGraph = new StaticVisionGraph();
		            	visionGraph.isStatic = false;
		            }
	            }
	            
	            for(int i = 0; i < nGraph.getLength(); i++){
	            	Node graphnode = nGraph.item(i);
	            	Element eGraphnode = (Element) graphnode;
	            	String nodeName = eGraphnode.getAttribute("name");
	            	
	            	ArrayList<String> neighbourList = new ArrayList<String>();
	            	
	            	NodeList nNeighbours = eGraphnode.getElementsByTagName("neighbour");
	            	for(int j = 0; j < nNeighbours.getLength(); j++){
	            		Node neighbour = nNeighbours.item(j);
	            		Element eNeigh = (Element) neighbour;
	            		neighbourList.add(eNeigh.getAttribute("name"));
	            	}
	            	this.visionGraph.addValue(nodeName, neighbourList);
	            }
            }
            
            //read events from xml
            NodeList nEvents = doc.getElementsByTagName("event");
            for(int i = 0; i < nEvents.getLength(); i++){
            	Node nEvent = nEvents.item(i);
            	Element eEvent = (Element) nEvent;
            	
            	int ts = Integer.parseInt(eEvent.getAttribute("timestep"));
            	
            	int part;
            	String name = eEvent.getAttribute("name");
            	String event = eEvent.getAttribute("event");
            	String dur = eEvent.getAttribute("duration");
            	
            	Event e;
            	
            	if(eEvent.getAttribute("participant").equals("camera")){
            		part = 1;
            		if(event.equals("add")){
            			double range = Double.parseDouble(eEvent.getAttribute("range"));
            			double angle = Double.parseDouble(eEvent.getAttribute("viewing_angle"));
            			double heading = Double.parseDouble(eEvent.getAttribute("heading"));
            			double x = Double.parseDouble(eEvent.getAttribute("x"));
            			double y = Double.parseDouble(eEvent.getAttribute("y"));
            			int comm = Integer.parseInt( eEvent.getAttribute("comm"));
            			int limit = FIX_LIMIT;
            			if(eEvent.hasAttribute("limit")){
            				limit = Integer.parseInt(eEvent.getAttribute("limit"));
            			}
            			
            			e = new Event(ts, part, name, event, heading, range, angle, x, y, limit, comm);
            			events.add(e);
            		}
            		else if(event.equals("change")){
            			double head = Double.parseDouble(eEvent.getAttribute("heading"));
            			double range = Double.parseDouble(eEvent.getAttribute("range"));
            			double angle = Double.parseDouble(eEvent.getAttribute("viewing_angle"));
            			double x = Double.parseDouble(eEvent.getAttribute("x"));
            			double y = Double.parseDouble(eEvent.getAttribute("y"));
            			int comm = -1;
            			double heading = Double.parseDouble(eEvent.getAttribute("heading"));
            			int limit = FIX_LIMIT;
            			e = new Event(ts, part, name, event, heading, range, angle, x, y, limit, comm);
            			events.add(e);
            		}
            		else{
            			//int duration = Integer.parseInt(dur);
            			int d = Integer.parseInt(dur);
            			e = new Event(ts, part, name, event, d);
            			events.add(e);
            		}
            	}
            	else if(eEvent.getAttribute("participant").equals("object")){
            		part = 2;
            		if(eEvent.getAttribute("event").equals("add")){
            			double head = Double.parseDouble(eEvent.getAttribute("heading"));
            			double speed = Double.parseDouble(eEvent.getAttribute("speed"));
            			double x = Double.parseDouble(eEvent.getAttribute("x"));
            			double y = Double.parseDouble(eEvent.getAttribute("y"));
            			
            			NodeList nPoints = eEvent.getElementsByTagName("waypoint");
            			ArrayList<Point2D> waypoints = null;
            			if(nPoints.getLength() > 0){
            				waypoints = new ArrayList<Point2D>();
            			}
            			for(int k = 0; k < nPoints.getLength(); k++){
            				Node waypoint = nPoints.item(k);
            				Element eWP = (Element) waypoint;
            				double xP = Double.parseDouble(eWP.getAttribute("x"));
            				double yP = Double.parseDouble(eWP.getAttribute("y"));
            				Point2D wp = new Point2D.Double(xP, yP);
            				waypoints.add(wp);
            			}
            			
            			e = new Event(ts, part, name, event, head, speed, x, y, waypoints);
            			events.add(e);
            		}
            		else{

            		   int d = Integer.parseInt(dur);
            		    
            		    e = new Event(ts, part, name, event, d);
            		    events.add(e);
            		}
            	}
            	else if(eEvent.getAttribute("participant").equals("grc")){
            		part = 3;
            		if(eEvent.getAttribute("event").equals("error")){
            			e = new Event(ts, part, "", event, Integer.parseInt(dur));
            			events.add(e);
            		}
            	}
            }
            return true;

            
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    private String getTagValue(String sTag, Element eElement) {
        NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
        Node nValue = (Node) nlList.item(0);

        return nValue.getNodeValue();
    }

    public void saveToXML(String filename) {

        try {

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            //root elements
            Document doc = docBuilder.newDocument();
            Element sim = doc.createElement("simulation");
            doc.appendChild(sim);

            sim.setAttribute("min_x", min_x.toString());
            sim.setAttribute("max_x", max_x.toString());
            sim.setAttribute("min_y", min_y.toString());
            sim.setAttribute("max_y", max_y.toString());

            Element eCamerasCollection = doc.createElement("cameras");
            sim.appendChild(eCamerasCollection);

            for (CameraSettings cs : cameras) {

                Element eCamera = doc.createElement("camera");
                eCamerasCollection.appendChild(eCamera);

                eCamera.setAttribute("name", cs.name.toString());
                eCamera.setAttribute("x", cs.x.toString());
                eCamera.setAttribute("y", cs.y.toString());
                eCamera.setAttribute("heading", cs.heading.toString());
                eCamera.setAttribute("viewing_angle", cs.viewing_angle.toString());
                eCamera.setAttribute("range", cs.range.toString());
                eCamera.setAttribute("ai_algorithm", cs.ai_algorithm.toString());
                eCamera.setAttribute("comm", cs.comm.toString());
                eCamera.setAttribute("limit", cs.limit.toString());

            }

            Element eObjectsCollection = doc.createElement("objects");
            sim.appendChild(eObjectsCollection);

            for (TrObjectSettings tros : objects) {

                Element eTrObject = doc.createElement("object");
                eObjectsCollection.appendChild(eTrObject);

                eTrObject.setAttribute("x", tros.x.toString());
                eTrObject.setAttribute("y", tros.y.toString());
                eTrObject.setAttribute("heading", tros.heading.toString());
                eTrObject.setAttribute("speed", tros.speed.toString());
                eTrObject.setAttribute("features", tros.features.toString());

            }

            //write the content into xml file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(filename));
            transformer.transform(source, result);

        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
        } catch (TransformerException tfe) {
            tfe.printStackTrace();
        }

    }
}
