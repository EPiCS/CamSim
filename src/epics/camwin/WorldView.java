
package epics.camwin;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import javax.swing.JPanel;

import net.sf.epsgraphics.ColorMode;
import net.sf.epsgraphics.EpsGraphics;

import com.sun.javafx.geom.Arc2D;

import epics.camsim.core.*;
import epics.commpolicy.*;

/**
 * Draws a spatial network on a panel
 */
public class WorldView extends JPanel implements Observer {
    private static final boolean SHOW_LABELS = true;
    private static final boolean SHOW_RES_LABELS = false;
	private SimCoreModel sim_model;
    private CoordinateSystemTransformer cst;

    public void setModel( SimCoreModel model ){
        this.sim_model = model;
        this.cst = this.sim_model.createCoordinateSystemTransformer(100, 100);
    }
	
    public WorldView(SimCoreModel model) {
		super();
		setBackground(Color.white);
        this.setModel(model);
        setToolTipText("test");
    }
    
    @Override
    public void processMouseMotionEvent(MouseEvent e) {
    	double dx = cst.winToSimX(e.getLocationOnScreen().x);
    	double dy = cst.winToSimY(e.getLocationOnScreen().y);
    	setToolTipText(e.getLocationOnScreen().x + "," + e.getLocationOnScreen().y);
    	
    	//System.out.println("dx: " + dx + ", dy: " + dy + ", min " + e.getLocationOnScreen().x + ", max " + e.getLocationOnScreen().y);
    }
    
    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
		int height = getHeight();
		int width = getWidth();
	
        int MIN_THICKNESS = 3;

        this.cst.setWindowHeight(height);
        this.cst.setWindowWidth(width);
  
    	g2.setColor(Color.white);
    	g2.fill( new Rectangle( 0, 0, width, height ) );
    	
    	
    	g2.setColor(Color.BLUE);
    	int bby = (int) this.cst.getRealHeight();
    	int bbx = (int) this.cst.getRealWidth();
    	
    	
        g2.drawRect(0, 0, bbx, bby);
        
        
//    	g2.setColor(Color.green);
//
//	
//		g2.setColor(Color.white);
//		g2.fill(new Rectangle(0, 0, width, height));
		
        g2.setColor(Color.GREEN);
        ArrayList<CameraController> cameras = sim_model.getCameras();
        for(CameraController c : cameras) {

            /*
             * Camera dot
             */

        	if(c.isOffline()) {
        		g2.setColor(Color.GRAY);
        	} else {
        		g2.setColor(Color.GREEN);
        	}

            Point p = new Point(this.cst.simToWindowX(c.getX()), this.cst.simToWindowY(c.getY()), 8); //draw spots
            g2.fill(p);
            
           // g.drawOval((int) this.cst.simToWindowX(0), (int)this.cst.simToWindowY(0), 2, 2);
            
            if(SHOW_LABELS) {
                if(!c.isOffline()){
    	            g2.setColor(Color.BLACK);
    	            Font f = new Font("Arial", Font.PLAIN, 10);
    	            g2.setFont(f);
    	            String algo = c.getAINode().getClass().getSimpleName();
    	            if(algo.contains("Passive")) {
    	            	algo = "P";
    	            } else if(algo.contains("Active")) {
    	            	algo = "A";
    	            } // Else actual name
    	            
    	            String comm = "";
    	            if(c.getAINode().getComm() instanceof Broadcast)
    	                comm = "BC";
    	            if(c.getAINode().getComm() instanceof Smooth)
                        comm = "SM";
    	            if(c.getAINode().getComm() instanceof Step)
                        comm = "ST";
    	                
    	            if(SHOW_RES_LABELS) {
    	            	if(c.isOffline()){
    	            		g2.setColor(Color.ORANGE);
    	            		drawString(g2, "OFFLINE: " + c.getName() + "\n Comm: " + comm + " Res: " + c.getAvailableResources(), (int) this.cst.simToWindowX(c.getX()), (int) this.cst.simToWindowY(c.getY()));
    	            	}
    	            	else{
    	            		drawString(g2, c.getName() + " \n Algo: " + algo + "\n Comm: " + comm + "\n Res: " + c.getAvailableResources(), (int) this.cst.simToWindowX(c.getX()), (int) this.cst.simToWindowY(c.getY()));
    	            	}
    	            } else{
    	            	if(c.isOffline()) {
    	            		g2.setColor(Color.ORANGE);
    	            		drawString(g2, "OFFLINE", (int) this.cst.simToWindowX(c.getX()), (int) this.cst.simToWindowY(c.getY()));
    	            	} else {
    	            		drawString(g2, c.getName() + " \n Algo: " + algo+ "\n Comm: " + comm, (int) this.cst.simToWindowX(c.getX()), (int) this.cst.simToWindowY(c.getY())+5);
    	            	}
    	            }
                }
	            else{
                    g2.setColor(Color.ORANGE);
                    Font f = new Font("Arial", Font.PLAIN, 10);
                    g2.setFont(f);
                    drawString(g2, "OFFLINE", (int) this.cst.simToWindowX(c.getX()), (int) this.cst.simToWindowY(c.getY()));
                }
            } else {
            	if(c.isOffline()){
            		g2.setColor(Color.ORANGE);
    	            Font f = new Font("Arial", Font.PLAIN, 10);
    	            g2.setFont(f);
    	            drawString(g2, "OFFLINE", (int) this.cst.simToWindowX(c.getX()), (int) this.cst.simToWindowY(c.getY()));
            	}
            }
            

            /*
             * Vision graph here
             */

            Map<String,Double> vg = c.getDrawableVisionGraph();
            for (Map.Entry<String,Double> e : vg.entrySet()){

                CameraController cc = sim_model.getCameraByName(e.getKey());
                if (cc == null){
                    continue;
                }
                double ccX = this.cst.simToWindowX(cc.getX());
                double ccY = this.cst.simToWindowY(cc.getY());

                double val = e.getValue();
                int col = (int)(val * 255);
                if (col > 255) { col = 255; }

                int thickness = 0;
                if(val > 0) {
                    thickness = MIN_THICKNESS;
                }
                if(val > 1) {
                    thickness = MIN_THICKNESS + 2;
                }
                if(thickness > 6 ) { thickness = 6; }
                if( thickness < MIN_THICKNESS) {  thickness = MIN_THICKNESS;}

                g2.setStroke(new BasicStroke(thickness));

                // Fading red
                g2.setColor(new Color(255, 255-col, 255-col ));
                
                Line2D.Double edge = new Line2D.Double(
                    this.cst.simToWindowX(c.getX()), this.cst.simToWindowY(c.getY()),
                    this.cst.simToWindowX(cc.getX()), this.cst.simToWindowY(cc.getY()) );
                g2.draw(edge);
            } 

            g2.setStroke(new BasicStroke(MIN_THICKNESS));


            /*
             * rest of stuff
             */

            double headingA = c.getHeading() + c.getAngle()/2; // Math.toRadians(45);
            double headingB = c.getHeading() - c.getAngle()/2;
            double cx = c.getX();
            double cy = c.getY();
            double range = c.getRange();

            double x = 0;
            double y = -1;

            double xpA = x * Math.cos( headingA ) - y * Math.sin( headingA );
            double ypA = x * Math.sin( headingA ) - y * Math.cos( headingA );

            double xpB = x * Math.cos( headingB ) - y * Math.sin( headingB );
            double ypB = x * Math.sin( headingB ) - y * Math.cos( headingB );

            xpA = xpA * range;
            ypA = ypA * range;
            xpB = xpB * range;
            ypB = ypB * range;

            if(( c.getNumVisibleObjects() < 1 )||(c.isOffline())){
                g2.setColor(Color.LIGHT_GRAY);
            }
            else{
                g2.setColor(Color.YELLOW);
            }
            
            if(c.isOffline()){
            	g2.setColor(new Color(255, 150, 0));
            }
            
            boolean polygon = false;

            if(polygon) {
	            Polygon poly = new Polygon();
	            poly.addPoint( (int)this.cst.simToWindowX(cx), (int)this.cst.simToWindowY(cy));
	            poly.addPoint( (int)this.cst.simToWindowX(cx+xpA), (int)this.cst.simToWindowY(cy+ypA));
	            poly.addPoint( (int)this.cst.simToWindowX(cx+xpB), (int)this.cst.simToWindowY(cy+ypB));
	            g2.fillPolygon(poly);

            } else {
                
	            Line2D.Double q = new Line2D.Double(
	                    this.cst.simToWindowX(cx), this.cst.simToWindowY(cy),
	                    this.cst.simToWindowX(cx+xpA), this.cst.simToWindowY(cy+ypA) );
	            g2.draw(q);
	
	            q = new Line2D.Double(
	                    this.cst.simToWindowX(cx), this.cst.simToWindowY(cy),
	                    this.cst.simToWindowX(cx+xpB), this.cst.simToWindowY(cy+ypB) );
	            g2.draw(q);

	            double headingMiddle = c.getHeading();
	            double xpM = x * Math.cos( headingMiddle ) - y * Math.sin( headingMiddle );
	            double ypM = x * Math.sin( headingMiddle ) - y * Math.cos( headingMiddle );
	            xpM = xpM * range;
	            ypM = ypM * range;
	            
	             
//	            QuadCurve2D curve = new QuadCurve2D.Float();
//	            curve.setCurve(
//	            		this.cst.simToWindowX(cx+xpA), this.cst.simToWindowY(cy+ypA),  	//from
//	            		this.cst.simToWindowX(cx+xpM), this.cst.simToWindowY(cy+ypM), 			//control
//	            		this.cst.simToWindowX(cx+xpB), this.cst.simToWindowY(cy+ypB) );	//to
//	            g2.draw(curve);
	            
	            q = new Line2D.Double(
	                    this.cst.simToWindowX(cx+xpA), this.cst.simToWindowY(cy+ypA),
	                    this.cst.simToWindowX(cx+xpB), this.cst.simToWindowY(cy+ypB) );
	            g2.draw(q);
	
	            
//	            //++++++++DRAW CIRCLE SEGMENT
//	            g2.setColor(Color.red);
//	            java.awt.geom.Arc2D arc = new java.awt.geom.Arc2D.Double((int)this.cst.simToWindowX(c.getX()-c.getRange()), (int)this.cst.simToWindowY(c.getY()+c.getRange()), (int)this.cst.simToWindowX(c.getRange()/2), (int)this.cst.simToWindowX(c.getRange()/2), 
////	                   (Math.toDegrees(c.getHeading()) < 0 ? Math.toDegrees(c.getHeading())+Math.toDegrees(c.getAngle())/1.27 : Math.toDegrees(c.getHeading()+180)+Math.toDegrees(c.getAngle())/1.27)
//	                    Math.toDegrees(c.getHeading())*(-1)+Math.toDegrees(c.getAngle())/1.27
//	                   , Math.toDegrees(c.getAngle()), Arc2D.PIE);
//	            g2.draw(arc);
	            
	            
//	            //ALMOST --> ONLY THE RANGE DOES NOT WORK YET! RANGE IS DEPENDENT ON THE ORIENTATION OF THE CAMERA AS THIS DEFINES HOW MUCH IT NEEDS TO BE EXPANDED TOWARDS X AND Y AXIS
//	            g2.setColor(Color.blue);
//	            java.awt.geom.Arc2D arc2 = new java.awt.geom.Arc2D.Double();
//	            double chead = 90 + (Math.toDegrees(headingMiddle)*(-1)); 
//	            double head = chead - Math.toDegrees(c.getAngle())/2;
//	            double r = c.getRange();
//	            arc2.setArcByCenter((int)this.cst.simToWindowX(c.getX()), (int)this.cst.simToWindowY(c.getY()), (int)this.cst.simToWindowX((c.getRange()))*0.595, head, Math.toDegrees(c.getAngle()), Arc2D.PIE);
//	            g2.draw(arc2); 
	            
          	}
	
            Map<TraceableObject, Double> objs = c.getVisibleObjects();
           
            for (Map.Entry<TraceableObject, Double> e : objs.entrySet()){
                TraceableObject key = e.getKey();
                double confidence = e.getValue();

                //int col = (int)(confidence * 200 + 150);
                int col = (int)(confidence * 128 + 50);
                if ( col > 255 ){ col = 255; }
	            
//                if(c.getAINode() instanceof epics.ai.ActiveAINodeSingleAsker){
//	                if( key == c.getTraced()){
//	                    g2.setColor( Color.green );
//	
//	                    col = (int)(confidence * 128 + 128);
//	                    if ( col > 255 ){ col = 255; }
//	
//	                    g2.setColor( new Color(255-col, 255, 255-col )  );
//	
//	                    Line2D.Double q = new Line2D.Double(
//	                    this.cst.simToWindowX(cx), this.cst.simToWindowY(cy),
//	                    this.cst.simToWindowX(key.getX()), this.cst.simToWindowY(key.getY()) );
//	                    g2.draw(q);
//	                }
//	                else{
//	                    /*
//	
//	                    g2.setColor( new Color(255-col, 255-col, 255-col )  );
//	
//	                    Line2D.Double q = new Line2D.Double(
//	                        this.cst.simToWindowX(cx), this.cst.simToWindowY(cy),
//	                        this.cst.simToWindowX(key.getX()), this.cst.simToWindowY(key.getY()) );
//	                    g2.draw(q);
//	                     * 
//	                     */
//	                }
//	            }
//                else{
                	for(TraceableObject tracked : c.getTrackedObjects().values()){
                		if(key.equals(tracked)){
                            g2.setColor( Color.green );

                            col = (int)(confidence * 128 + 128);
                            if ( col > 255 ){ col = 255; }

                            g2.setColor( new Color(255-col, 255, 255-col )  );

                            Line2D.Double q = new Line2D.Double(
                            this.cst.simToWindowX(cx), this.cst.simToWindowY(cy),
                            this.cst.simToWindowX(key.getX()), this.cst.simToWindowY(key.getY()) );
                            g2.draw(q);
                    	}
                	}
//                }
            }
            
            g2.setColor(Color.PINK);
            p = new Point(this.cst.simToWindowX(c.getVisualCenter().getX()), this.cst.simToWindowY(c.getVisualCenter().getY()), 2);
            g2.fill(p);
            
            
            
            Map<Location, Double> nbLoc = c.getAINode().getNoBidLocations();
            if(nbLoc != null){
                for (Map.Entry<Location, Double> e : nbLoc.entrySet()){//Location loc : nbLoc.keySet()) {
                    Location loc = e.getKey();
                    float f = (float)((255-(192f*e.getValue()%255))/192);
                    g2.setColor(new Color(f, f, f));
                    p = new Point(this.cst.simToWindowX(cst.toCenterBasedX(loc.getX())), this.cst.simToWindowY(cst.toCenterBasedY(loc.getY())), 2);
                    g2.fill(p);
                }
            }
            
            Map<Location, Double> hoLoc = c.getAINode().getHandoverLocations();
            if(hoLoc != null){
                for ( Map.Entry<Location, Double> e : hoLoc.entrySet()){
                    Location loc = e.getKey();
                    int colourcode = (int) (255-(255*e.getValue()%255));
                    g2.setColor(new Color(colourcode, colourcode, 255));
                    p = new Point(this.cst.simToWindowX(cst.toCenterBasedX(loc.getX())), this.cst.simToWindowY(cst.toCenterBasedY(loc.getY())), 3);
                    g2.fill(p);
                }
            }
            
            Map<Location, Double> olLoc = c.getAINode().getOverlapLocation();
            if(olLoc != null){
                for ( Map.Entry<Location, Double> e : olLoc.entrySet()){
                    Location loc = e.getKey();
                    float colourcode2 = (float)((255-(255*e.getValue()%255))/255);
                    g2.setColor(new Color(1.0f,colourcode2,colourcode2));
                  p = new Point(this.cst.simToWindowX(cst.toCenterBasedX(loc.getX())), this.cst.simToWindowY(cst.toCenterBasedY(loc.getY())), 2);
                  g2.fill(p);
//                    g2.drawRect((int)p.getCenterX()-1, (int)p.getCenterY()-1, 2, 2);
                }
            }
            
        }

        
        
        
        // Draw objects which move through the scene
        g2.setColor(Color.BLACK);
        ArrayList<TraceableObject> objects = sim_model.getObjects();
        for( TraceableObject tc : objects ){
            Point p = new Point( this.cst.simToWindowX(tc.getX()), this.cst.simToWindowY(tc.getY()), 5);		//draw spots
            //System.out.println( "coord x: " + this.cst.simToWindowX(tc.getX()) + " coord y: " + 20 );
            g2.fill(p);
            if(SHOW_LABELS)
            	g2.drawString(tc.toString(), (int) this.cst.simToWindowX(tc.getX()), (int) this.cst.simToWindowY(tc.getY()));
        }
         
    }
    
    private void drawString(Graphics g, String text, int x, int y) {
        for (String line : text.split("\n"))
            g.drawString(line, x, y += g.getFontMetrics().getHeight());
    }
    
    public void update(Observable obs, Object obj) {
    	repaint();
    }

    public void createSnapshot(String filename) throws FileNotFoundException, IOException {
        int bby = (int) this.cst.getRealHeight();
        int bbx = (int) this.cst.getRealWidth();
        EpsGraphics g2 = new EpsGraphics("EpsTools Drawable Export", 
                new FileOutputStream(filename), 0, 0, bbx, bby, ColorMode.COLOR_CMYK);//.COLOR_RGB);
                
        int height = getHeight();
        int width = getWidth();
    
        int MIN_THICKNESS = 3;

        this.cst.setWindowHeight(height);
        this.cst.setWindowWidth(width);
  
        g2.setColor(Color.white);
        g2.fill( new Rectangle( 0, 0, width, height ) );
        
        
        g2.setColor(Color.BLUE);       
        
        g2.drawRect(0, 0, bbx, bby);
        
        
//      g2.setColor(Color.green);
//
//  
//      g2.setColor(Color.white);
//      g2.fill(new Rectangle(0, 0, width, height));
        
        g2.setColor(Color.GREEN);
        ArrayList<CameraController> cameras = sim_model.getCameras();
        for(CameraController c : cameras) {

            /*
             * Camera dot
             */
            Random rand = new Random();
            
            float r = rand.nextFloat();
            float g = rand.nextFloat();
            float b = rand.nextFloat();
            Color camCol = new Color(r, g, b, 1.0f);

            if(c.isOffline()) {
                g2.setColor(Color.GRAY);
            } else {
                g2.setColor(camCol);//Color.GREEN);
            }

            Point p = new Point(this.cst.simToWindowX(c.getX()), this.cst.simToWindowY(c.getY()), 8); //draw spots
            g2.fill(p);
            
           // g.drawOval((int) this.cst.simToWindowX(0), (int)this.cst.simToWindowY(0), 2, 2);
            
            if(SHOW_LABELS) {
                g2.setColor(Color.BLACK);
                Font f = new Font("Arial", Font.PLAIN, 10);
                g2.setFont(f);
                String algo = c.getAINode().getClass().getSimpleName();
                if(algo.contains("Passive")) {
                    algo = "P";
                } else if(algo.contains("Active")) {
                    algo = "A";
                } // Else actual name
                
                String comm = "";
                if(c.getAINode().getComm() instanceof Broadcast)
                    comm = "BC";
                if(c.getAINode().getComm() instanceof Smooth)
                    comm = "SM";
                if(c.getAINode().getComm() instanceof Step)
                    comm = "ST";
                    
                if(SHOW_RES_LABELS) {
                    if(c.isOffline()){
                        g2.setColor(Color.ORANGE);
                        drawString(g2, "OFFLINE: " + c.getName() + "\n Comm: " + comm + " Res: " + c.getAvailableResources(), (int) this.cst.simToWindowX(c.getX()), (int) this.cst.simToWindowY(c.getY()));
                    }
                    else{
                        drawString(g2, c.getName() + " \n Algo: " + algo + "\n Comm: " + comm + "\n Res: " + c.getAvailableResources(), (int) this.cst.simToWindowX(c.getX()), (int) this.cst.simToWindowY(c.getY()));
                    }
                } else{
                    if(c.isOffline()) {
                        g2.setColor(Color.ORANGE);
                        drawString(g2, "OFFLINE", (int) this.cst.simToWindowX(c.getX()), (int) this.cst.simToWindowY(c.getY()));
                    } else {
                        drawString(g2, c.getName() + " \n Algo: " + algo+ "\n Comm: " + comm, (int) this.cst.simToWindowX(c.getX()), (int) this.cst.simToWindowY(c.getY())+5);
                    }
                }
            } else {
                if(c.isOffline()){
                    g2.setColor(Color.ORANGE);
                    Font f = new Font("Arial", Font.PLAIN, 10);
                    g2.setFont(f);
                    drawString(g2, "OFFLINE", (int) this.cst.simToWindowX(c.getX()), (int) this.cst.simToWindowY(c.getY()));
                }
            }
            

//            /*
//             * Vision graph here
//             */
//
//            Map<String,Double> vg = c.getDrawableVisionGraph();
//            for (Map.Entry<String,Double> e : vg.entrySet()){
//
//                CameraController cc = sim_model.getCameraByName(e.getKey());
//                if (cc == null){
//                    continue;
//                }
//                double ccX = this.cst.simToWindowX(cc.getX());
//                double ccY = this.cst.simToWindowY(cc.getY());
//
//                double val = e.getValue();
//                int col = (int)(val * 255);
//                if (col > 255) { col = 255; }
//
//                int thickness = 0;
//                if(val > 0) {
//                    thickness = MIN_THICKNESS;
//                }
//                if(val > 1) {
//                    thickness = MIN_THICKNESS + 2;
//                }
//                if(thickness > 6 ) { thickness = 6; }
//                if( thickness < MIN_THICKNESS) {  thickness = MIN_THICKNESS;}
//
//                g2.setStroke(new BasicStroke(thickness));
//
//                // Fading red
//                g2.setColor(new Color(255, 255-col, 255-col ));
//                
//                Line2D.Double edge = new Line2D.Double(
//                    this.cst.simToWindowX(c.getX()), this.cst.simToWindowY(c.getY()),
//                    this.cst.simToWindowX(cc.getX()), this.cst.simToWindowY(cc.getY()) );
//                g2.draw(edge);
//            }
//
//            g2.setStroke(new BasicStroke(MIN_THICKNESS));


            /*
             * rest of stuff
             */

            double headingA = c.getHeading() + c.getAngle()/2; // Math.toRadians(45);
            double headingB = c.getHeading() - c.getAngle()/2;
            double cx = c.getX();
            double cy = c.getY();
            double range = c.getRange();

            double x = 0;
            double y = -1;

            double xpA = x * Math.cos( headingA ) - y * Math.sin( headingA );
            double ypA = x * Math.sin( headingA ) - y * Math.cos( headingA );

            double xpB = x * Math.cos( headingB ) - y * Math.sin( headingB );
            double ypB = x * Math.sin( headingB ) - y * Math.cos( headingB );

            xpA = xpA * range;
            ypA = ypA * range;
            xpB = xpB * range;
            ypB = ypB * range;

            if(( c.getNumVisibleObjects() < 1 )||(c.isOffline())){
                g2.setColor(Color.LIGHT_GRAY);
            }
            else{
                g2.setColor(Color.YELLOW);
            }
            
            if(c.isOffline()){
                g2.setColor(new Color(255, 150, 0));
            }
            
            boolean polygon = false;

            if(polygon) {
                Polygon poly = new Polygon();
                poly.addPoint( (int)this.cst.simToWindowX(cx), (int)this.cst.simToWindowY(cy));
                poly.addPoint( (int)this.cst.simToWindowX(cx+xpA), (int)this.cst.simToWindowY(cy+ypA));
                poly.addPoint( (int)this.cst.simToWindowX(cx+xpB), (int)this.cst.simToWindowY(cy+ypB));
                g2.fillPolygon(poly);

            } else {
                
                Line2D.Double q = new Line2D.Double(
                        this.cst.simToWindowX(cx), this.cst.simToWindowY(cy),
                        this.cst.simToWindowX(cx+xpA), this.cst.simToWindowY(cy+ypA) );
                g2.draw(q);
    
                q = new Line2D.Double(
                        this.cst.simToWindowX(cx), this.cst.simToWindowY(cy),
                        this.cst.simToWindowX(cx+xpB), this.cst.simToWindowY(cy+ypB) );
                g2.draw(q);

                double headingMiddle = c.getHeading();
                double xpM = x * Math.cos( headingMiddle ) - y * Math.sin( headingMiddle );
                double ypM = x * Math.sin( headingMiddle ) - y * Math.cos( headingMiddle );
                xpM = xpM * range;
                ypM = ypM * range;
                
                 
//              QuadCurve2D curve = new QuadCurve2D.Float();
//              curve.setCurve(
//                      this.cst.simToWindowX(cx+xpA), this.cst.simToWindowY(cy+ypA),   //from
//                      this.cst.simToWindowX(cx+xpM), this.cst.simToWindowY(cy+ypM),           //control
//                      this.cst.simToWindowX(cx+xpB), this.cst.simToWindowY(cy+ypB) ); //to
//              g2.draw(curve);
                
                q = new Line2D.Double(
                        this.cst.simToWindowX(cx+xpA), this.cst.simToWindowY(cy+ypA),
                        this.cst.simToWindowX(cx+xpB), this.cst.simToWindowY(cy+ypB) );
                g2.draw(q);
    
                
////              //++++++++DRAW CIRCLE SEGMENT
//                g2.setColor(Color.blue);
//                java.awt.geom.Arc2D arc2 = new java.awt.geom.Arc2D.Double();
//                double chead = 90 + (Math.toDegrees(headingMiddle)*(-1)); 
//                double head = chead - Math.toDegrees(c.getAngle())/2;
//                double r = c.getRange();
//                arc2.setArcByCenter((int)this.cst.simToWindowX(c.getX()), (int)this.cst.simToWindowY(c.getY()), (int)this.cst.simToWindowX((c.getRange()))/2, head, Math.toDegrees(c.getAngle()), Arc2D.PIE);
//                g2.draw(arc2); 
                
            }
    
            //+++++++++++++++GET OBJECTS AND DRAW GREEN TRCKING LINES
            //++++++++++++++++++++++++++++++++++++++++++++++++++++++++
//            Map<TraceableObject, Double> objs = c.getVisibleObjects();
//           
//            for (Map.Entry<TraceableObject, Double> e : objs.entrySet()){
//                TraceableObject key = e.getKey();
//                double confidence = e.getValue();
//
//                //int col = (int)(confidence * 200 + 150);
//                int col = (int)(confidence * 128 + 50);
//                if ( col > 255 ){ col = 255; }
//                
////                if(c.getAINode() instanceof epics.ai.ActiveAINodeSingleAsker){
////                  if( key == c.getTraced()){
////                      g2.setColor( Color.green );
////  
////                      col = (int)(confidence * 128 + 128);
////                      if ( col > 255 ){ col = 255; }
////  
////                      g2.setColor( new Color(255-col, 255, 255-col )  );
////  
////                      Line2D.Double q = new Line2D.Double(
////                      this.cst.simToWindowX(cx), this.cst.simToWindowY(cy),
////                      this.cst.simToWindowX(key.getX()), this.cst.simToWindowY(key.getY()) );
////                      g2.draw(q);
////                  }
////                  else{
////                      /*
////  
////                      g2.setColor( new Color(255-col, 255-col, 255-col )  );
////  
////                      Line2D.Double q = new Line2D.Double(
////                          this.cst.simToWindowX(cx), this.cst.simToWindowY(cy),
////                          this.cst.simToWindowX(key.getX()), this.cst.simToWindowY(key.getY()) );
////                      g2.draw(q);
////                       * 
////                       */
////                  }
////              }
////                else{
//                    for(TraceableObject tracked : c.getTrackedObjects().values()){
//                        if(key.equals(tracked)){
//                            g2.setColor( Color.green );
//
//                            col = (int)(confidence * 128 + 128);
//                            if ( col > 255 ){ col = 255; }
//
//                            g2.setColor( new Color(255-col, 255, 255-col )  );
//
//                            Line2D.Double q = new Line2D.Double(
//                            this.cst.simToWindowX(cx), this.cst.simToWindowY(cy),
//                            this.cst.simToWindowX(key.getX()), this.cst.simToWindowY(key.getY()) );
//                            g2.draw(q);
//                        }
//                    }
////                }
//            }
            
            //CENTER OF FOV
//            g2.setColor(Color.PINK);
//            p = new Point(this.cst.simToWindowX(c.getVisualCenter().getX()), this.cst.simToWindowY(c.getVisualCenter().getY()), 2);
//            g2.fill(p);
            
           
            Color test = new Color(r, g, b, 0.1f);
            
            Map<Location, Double> nbLoc = c.getAINode().getNoBidLocations();
            if(nbLoc != null){
                for (Location loc : nbLoc.keySet()) {
                    g2.setColor(test);//camCol.brighter()); // Color.LIGHT_GRAY);
                    p = new Point(this.cst.simToWindowX(cst.toCenterBasedX(loc.getX())), this.cst.simToWindowY(cst.toCenterBasedY(loc.getY())), 2);
                    g2.fill(p);
                    
                }
            }
                        
            Map<Location, Double> hoLoc = c.getAINode().getHandoverLocations();
            if(hoLoc != null){
                for (Location loc : hoLoc.keySet()) {
                    g2.setColor(camCol); //Color.BLUE);
                    p = new Point(this.cst.simToWindowX(cst.toCenterBasedX(loc.getX())), this.cst.simToWindowY(cst.toCenterBasedY(loc.getY())), 3);
//                    g2.fill(p);
                    g2.drawLine((int)p.getCenterX()-2, (int) p.getCenterY()-2, (int)p.getCenterX()+2, (int) p.getCenterY()+2);
                    g2.drawLine((int)p.getCenterX()-2, (int) p.getCenterY()+2, (int)p.getCenterX()+2, (int) p.getCenterY()-2);
                }
            }
            
            
            Map<Location, Double> olLoc = c.getAINode().getOverlapLocation();
            if(olLoc != null){
                for (Location loc : olLoc.keySet()){
                    g2.setColor(test); //camCol.brighter()); //Color.RED);
                    p = new Point(this.cst.simToWindowX(cst.toCenterBasedX(loc.getX())), this.cst.simToWindowY(cst.toCenterBasedY(loc.getY())), 3);
//                  g2.fill(p);
                    g2.drawRect((int)p.getCenterX()-2, (int)p.getCenterY()-1, 2, 2);
                }
            }
            
        }

        
        
        
        // Draw objects which move through the scene
//        g2.setColor(Color.BLACK);
//        ArrayList<TraceableObject> objects = sim_model.getObjects();
//        for( TraceableObject tc : objects ){
//            Point p = new Point( this.cst.simToWindowX(tc.getX()), this.cst.simToWindowY(tc.getY()), 5);        //draw spots
//            //System.out.println( "coord x: " + this.cst.simToWindowX(tc.getX()) + " coord y: " + 20 );
//            g2.fill(p);
//            if(SHOW_LABELS)
//                g2.drawString(tc.toString(), (int) this.cst.simToWindowX(tc.getX()), (int) this.cst.simToWindowY(tc.getY()));
//        }
        g2.flush();
        g2.close();
    }
}