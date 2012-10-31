package epics.camwin;

import java.awt.Dimension;

import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;

public class DataView  extends JInternalFrame{
    static int openFrameCount = 0;
    static final int xOffset = 30, yOffset = 30;
    
    DataPanel dataPanel;
    
    SimCoreModel _sim;
 
    public DataView(String title, int locx, int locy, int w, int h, int valX, int valY, SimCoreModel sim) {
        super(title, 
              true, //resizable
              false, //closable
              false, //maximizable
              false);//iconifiable
 
        _sim = sim;
        
        //...Then set the window size or call pack...
        setSize(w,h);
        
//        setMinimumSize(new Dimension(w,h));
//        setMaximumSize(new Dimension(w, h));
 
        //Set the window's location.
        setLocation(locx, locy);
        
        setLayout(null);
                
        dataPanel = new DataPanel(valX, valY);
        dataPanel.setSize(w - 40, h - 60);
        dataPanel.setLocation(20, 10);
        dataPanel.setAutoscrolls(true);
        
        dataPanel.setPreferredSize(new Dimension(valX, h-60 ));
        
		JScrollPane jsp = new JScrollPane(dataPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		jsp.setMinimumSize(new Dimension(w, h));
		jsp.setPreferredSize(new Dimension(w, h));
		setContentPane(jsp);
		
        
        repaint();
    }
    
	public void update()
    {
		dataPanel.add(_sim.computeUtility());
		repaint();
    }
}
