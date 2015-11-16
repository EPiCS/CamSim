package epics.camwin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.filechooser.FileFilter;

import epics.camsim.core.SimCore;
import epics.camsim.core.SimSettings;

/**
 * 
 * @author Lukas Esterle <Lukas.Esterle@aau.at> & Marcin Bogdanski
 */

public class WindowMain implements ActionListener{
	
    /**
     * main jframe
     */
	public JFrame frame;
	/**
	 * timer of simulator
	 */
	public Timer timer;
	/**
	 * model of simulator
	 */
	public SimCoreModel sim_model;
	boolean modeDemo3 = false;
    boolean modeDemo1 = false;
    /**
     * visual output of simulation
     */
    public WorldView wv;
    /** 
     * predefined scenarios
     */
    @SuppressWarnings("rawtypes")
    JComboBox demos;
    DataView dv;
    JButton save;
    
    /**
     * 
     * Constructor for WindowMain.java
     * @param sm
     * @param input
     */
	public WindowMain(SimCoreModel sm, String input){
		sim_model = sm;

		timer = new Timer(100, new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                timeStep();
            }
        });
	}	
	JDesktopPane desktop;
	
	/**
	 * Creates and displays the GUI
	 */
    public void createAndShowGUI() {
        frame = new JFrame("Camera Simulator");
        frame.setSize(1024, 768);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        desktop = new JDesktopPane();
        int w = 300;
        int h = 300;
        dv = new DataView("data", frame.getSize().width - w -16, frame.getSize().height - h-38, w, h, 1000, 80, sim_model);
        desktop.add(dv);
        frame.setContentPane(desktop);
        frame.setVisible(true);
        
        
        frame.setLayout(new BorderLayout());

        timer = new Timer(100, new ActionListener() {

            public void actionPerformed(ActionEvent ae) {
                timeStep();
            }
        });
     
        JPanel button_panel = createButtons();
        frame.add(button_panel, BorderLayout.NORTH);
        

        frame.setBackground(Color.red);
       
        wv = new WorldView(sim_model);
        frame.add(wv, BorderLayout.CENTER);
        
        frame.setVisible(true);

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
    private JPanel createButtons() {
    	JPanel button_panel = new JPanel();
        
        String[] demoString = {"Select simulation file ...", "Demo I","Demo II","Demo III"};
        
        demos = new JComboBox(demoString);
        demos.setActionCommand("demos");
        demos.addActionListener(this);
        button_panel.add(demos);

        JButton b = new JButton("Start");
        b.setActionCommand("start");
        b.addActionListener(this);
        button_panel.add(b);

        JButton b3 = new JButton("Step");
        b3.setActionCommand("step");
        b3.addActionListener(this);
        button_panel.add(b3);

        JButton b4 = new JButton("Stop");
        b4.setActionCommand("stop");
        b4.addActionListener(this);
        button_panel.add(b4);

        JButton b2 = new JButton("Randomise");
        b2.setActionCommand("reset");
        b2.addActionListener(this);
        button_panel.add(b2);

        JButton b5 = new JButton("Cams++");
        b5.setActionCommand("addcam");
        b5.addActionListener(this);
        button_panel.add(b5);

        JButton b6 = new JButton("Cams--");
        b6.setActionCommand("remcam");
        b6.addActionListener(this);
        button_panel.add(b6);

        JButton b7 = new JButton("Objs++");
        b7.setActionCommand("addobj");
        b7.addActionListener(this);
        button_panel.add(b7);

        JButton b8 = new JButton("Objs--");
        b8.setActionCommand("remobj");
        b8.addActionListener(this);
        button_panel.add(b8);
        
        save = new JButton("Save scenario");
        save.setActionCommand("save");
        save.addActionListener(this);
//        save.setEnabled(false);
        button_panel.add(save);
        
        JButton snapshot = new JButton("Snapshot");
        snapshot.setActionCommand("snap");
        snapshot.addActionListener(this);
        button_panel.add(snapshot);
        
        return button_panel;
	}

	/**
	 * restart timer for simultation
	 */
	public void timeReset() {
        timer.stop();
    }

	/**
	 * forward timer and entire simulation by one step 
	 */
    public void timeStep() {
        try {
			sim_model.update();
		} catch (Exception e) {
			e.printStackTrace();
			timer.stop();
		}
        dv.update();
        frame.repaint();
    }

    /*
     * (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
	@SuppressWarnings("rawtypes")
    @Override
	public void actionPerformed(ActionEvent e) {
		FileFilter scenariosFilter = new FileFilter() {
			@Override
			public String getDescription() {
				return "Scenarios (.xml)";
			}
			
			@Override
			public boolean accept(File f) {
				if(f.isDirectory())
					return true;
				
				if(f.getName().endsWith(".xml")){
					return true;
				} else {
					return false;
				}
			}
		};
		if(e.getActionCommand().equals("save")){
			JFileChooser jfc = new JFileChooser("./");
			jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			
			jfc.setFileFilter(scenariosFilter);
			int retVal = jfc.showSaveDialog(frame);
			if(retVal == JFileChooser.APPROVE_OPTION){
				sim_model.save_to_xml(jfc.getSelectedFile().getAbsolutePath());
			}
		}
		
		if(e.getActionCommand().equals("snap")){
		    try {
                wv.createSnapshot("example.eps");
            } catch (FileNotFoundException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
		}

		if(e.getActionCommand().equals("addobj")){
			if (!modeDemo3) {
                sim_model.add_random_object();
            } else {
                double ln = 27;
                double ss = 3;

                ArrayList<Point2D> waypoints = new ArrayList<Point2D>();
                waypoints.add(new Point2D.Double(-ln, -ss));
                waypoints.add(new Point2D.Double(-ss, -ss));
                waypoints.add(new Point2D.Double(-ss, -ln));
                waypoints.add(new Point2D.Double(ss, -ln));
                waypoints.add(new Point2D.Double(ss, -ss));
                waypoints.add(new Point2D.Double(ln, -ss));
                waypoints.add(new Point2D.Double(ln, ss));
                waypoints.add(new Point2D.Double(ss, ss));

                waypoints.add(new Point2D.Double(ss, ln));
                waypoints.add(new Point2D.Double(-ss, ln));
                waypoints.add(new Point2D.Double(-ss, ss));
                waypoints.add(new Point2D.Double(-ln, ss));

                sim_model.add_object(0.9, waypoints, SimCore.getNextID());
            }
            frame.repaint();
		}
		if(e.getActionCommand().equals("remobj")){
			sim_model.remove_random_object();
            frame.repaint();
		}
		if(e.getActionCommand().equals("addcam")){
			sim_model.add_random_camera();
            frame.repaint();
		}
		if(e.getActionCommand().equals("remcam")){
            sim_model.remove_random_camera();
            frame.repaint();
		}
		if(e.getActionCommand().equals("reset")){
			modeDemo3 = false;
            modeDemo1 = false;
            timeReset();
            sim_model.recreate_cameras();
            
//            curL.setText("Currently runnign: Random");
//            save.setEnabled(true);
            frame.repaint();
		}
		if(e.getActionCommand().equals("start")){
			timer.start();
		}
		if(e.getActionCommand().equals("stop")){
			timer.stop();
		}
		if(e.getActionCommand().equals("step")){
			timeStep();
		}
		if(e.getActionCommand().equals("demos")){
			switch(((JComboBox)e.getSource()).getSelectedIndex()){
				case 1: this.loadDemo1();
						break;
				case 2: this.loadDemo2();
						break;
				case 3: this.loadDemo3();
						break;
				case 0: 
						JFileChooser jfc = new JFileChooser("./");
						jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
						jfc.setFileFilter(scenariosFilter);
						int retVal = jfc.showOpenDialog(frame);
						
						if(retVal == JFileChooser.APPROVE_OPTION){
							String input_file = jfc.getSelectedFile().getAbsolutePath(); 
							//TODO load file
							SimSettings simsettings = new SimSettings();
					        boolean success = simsettings.loadFromXML(input_file);
	
					        if (!success) {
					            System.err.println("Error, Could not load " + input_file);
					        }
					        else{
					        	sim_model.getCameras().clear();
				              	sim_model.getObjects().clear();
					        	sim_model.interpretFile(simsettings);					        	
					        }
					        
						}
						frame.repaint();
						break;
			}
			wv.setModel(sim_model);
		}
	}

	/**
	 * loads first demo
	 */
	public void loadDemo1() {
		modeDemo3 = false;
        modeDemo1 = true;
        timeReset();

        sim_model.loadDemo(1);
        
        frame.repaint(); 
	}

	private void loadDemo2() {
		modeDemo3 = false;
        modeDemo1 = false;
        timeReset();
        
        sim_model.loadDemo(2);
        
        frame.repaint();
	}

	private void loadDemo3() {
		modeDemo3 = true;
      	modeDemo1 = false;
      	timeReset();
      	
      	sim_model.loadDemo(3);
        
      	frame.repaint();
	}
}
