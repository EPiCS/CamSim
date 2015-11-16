package epics.camwin;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;

import javax.swing.JPanel;

/**
 * DataPanel was intended to show information about the different elemetns in the simulation. 
 * !! NOT IN USE !!
 * 
 * @author Lukas Esterle <lukas [dot] esterle [at] aau [dot] at>
 *
 */
public class DataPanel extends JPanel {
	
	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    ArrayList<Double> values = new ArrayList<Double>(1000);
	ArrayList<Integer> steps = new ArrayList<Integer>(1000);
	double maxValue = 0.0;
	int s = 0;

	private final int intX = 50;
	private final int intY = 10;
	
	private int xVal;
	private int yVal;
	
	/**
	 * 
	 * Constructor for DataPanel
	 * @param xval x location
	 * @param yval y location
	 */
	public DataPanel(int xval, int yval){
		xVal = xval;
		yVal = yval;
	}
	
	/**
	 * add new value
	 * @param v
	 */
	public void add(double v){
		values.add(v);		
		steps.add(s++);
		repaint();
	}
	
	/*
	 * (non-Javadoc)
	 * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
	 */
	@Override
	public void paintComponent(Graphics g){ //paintComponent(Graphics g){
				
		g.setColor(Color.black);
					
		g.drawLine(20, getHeight()-20, xVal + 20, getHeight()-20);
		g.drawLine(20, 0, 20, getHeight()-20);
		g.drawString("0", 10, getHeight()-5);
		
		for (int i = 0; i <= xVal/intX; i++) {
			int x = (intX*i)+20;
			g.drawLine(x, getHeight()-18, x, getHeight()-22);
			if(i!= 0)
				g.drawString("" + i*intX, x-10, getHeight()-5);
		}
		
		int actInt = (getHeight()-20) / yVal * intY;
		
		for(int i = 0; i<= yVal/intY; i++){
			
			int y = actInt*i + 20;
			g.drawLine(18, getHeight()-y, 22, getHeight()-y);
			g.drawString("" + i*intY, 5, getHeight()-y);
		}
		
		int oldx = 0;
		double oldy = 20.0;
		int[] paintValues = new int[values.size()];
		int[] paintSteps = new int[steps.size()];
		double sum = 0.0;
		for (int i = 0; i < values.size(); i++) {
			//adapt values to size of window - 20 for hight
			sum = sum + values.get(i);
			paintValues[i] = getHeight() - (int)(((getHeight()-20) / yVal) * (sum)) - 20;
//			System.err.println("sum=" + sum + " interval=" + ((getHeight()-20) / yVal) + " drawPoint=" + sum*((getHeight()-20)/yVal) + " paintvalue = " + paintValues[i]);		
			paintSteps[i] = steps.get(i) + 20;
		}
		
		g.setColor(Color.blue);
		Graphics2D g2 = (Graphics2D) g;
		g2.setStroke(new BasicStroke(3));
		
		g2.drawPolyline(paintSteps, paintValues, paintSteps.length);
		
	}
}
