
package epics.camwin;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JPanel;

import epics.camsim.core.CameraController;
import epics.camsim.core.TraceableObject;

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
		g2.fill(new Rectangle(0, 0, width, height));
		
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
	            g2.setColor(Color.BLACK);
	            Font f = new Font("Arial", Font.PLAIN, 10);
	            g2.setFont(f);
	            String algo = c.getAINode().getClass().getSimpleName();
	            if(algo.contains("Passive")) {
	            	algo = "P";
	            } else if(algo.contains("Active")) {
	            	algo = "A";
	            } // Else actual name
	            
	            if(SHOW_RES_LABELS) {
	            	if(c.isOffline()){
	            		g2.setColor(Color.ORANGE);
	            		g2.drawString("OFFLINE: " + c.getName() + " Res: " + c.getAvailableResources(), (int) this.cst.simToWindowX(c.getX()), (int) this.cst.simToWindowY(c.getY()));
	            	}
	            	else{
	            		g2.drawString(c.getName() + " \n Algo: " + algo + "\n Res: " + c.getAvailableResources(), (int) this.cst.simToWindowX(c.getX()), (int) this.cst.simToWindowY(c.getY()));
	            	}
	            } else{
	            	if(c.isOffline()) {
	            		g2.setColor(Color.ORANGE);
	            		g2.drawString("OFFLINE", (int) this.cst.simToWindowX(c.getX()), (int) this.cst.simToWindowY(c.getY()));
	            	} else {
	            		g2.drawString(c.getName() + " \n Algo: " + algo, (int) this.cst.simToWindowX(c.getX()), (int) this.cst.simToWindowY(c.getY())+5);
	            	}
	            }
            } else {
            	if(c.isOffline()){
            		g2.setColor(Color.ORANGE);
    	            Font f = new Font("Arial", Font.PLAIN, 10);
    	            g2.setFont(f);
    	            g2.drawString("OFFLINE", (int) this.cst.simToWindowX(c.getX()), (int) this.cst.simToWindowY(c.getY()));
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

            //System.out.println( "coord xp: " + xpA + " coord yp: " + ypA );
            //System.out.println( "coord cx: " + cx + " coord cy: " + cy );

            if(( c.getNumVisibleObjects() < 1 )||(c.isOffline())){
                g2.setColor(Color.LIGHT_GRAY);
            }
            else{
                g2.setColor(Color.YELLOW);
            }
            
            if(c.isOffline()){
            	g2.setColor(new Color(255, 150, 0));
            }
            
//            if(c.getAINode().getTracedObjects().size() > 0){
//            	g2.setColor(Color.CYAN);
//            }
            
            boolean polygon = false;

            if(polygon) {

	            //g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
	            Polygon poly = new Polygon();
	            poly.addPoint( (int)this.cst.simToWindowX(cx), (int)this.cst.simToWindowY(cy));
	            poly.addPoint( (int)this.cst.simToWindowX(cx+xpA), (int)this.cst.simToWindowY(cy+ypA));
	            poly.addPoint( (int)this.cst.simToWindowX(cx+xpB), (int)this.cst.simToWindowY(cy+ypB));
	            g2.fillPolygon(poly);
	            //g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));

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
        

            if (false){
            	TraceableObject tracked = c.getTracked();
                g2.setColor( Color.green );

                int col = 5;//(int)(tracked.get * 128 + 50);
                if ( col > 255 ){ col = 255; }

                g2.setColor( new Color(255-col, 255-col, 255-col )  );

                Line2D.Double q = new Line2D.Double(
                    this.cst.simToWindowX(cx), this.cst.simToWindowY(cy),
                    this.cst.simToWindowX(tracked.getX()), this.cst.simToWindowY(tracked.getY()) );
                g2.draw(q);
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
    
    public void update(Observable obs, Object obj) {
    	repaint();
    }
}