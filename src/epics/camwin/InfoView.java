package epics.camwin;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import epics.camsim.core.CameraController;

public class InfoView implements TreeSelectionListener {
	JFrame infoView;
	JTree tree;
	SimCoreModel sim_core;
	JPanel infostuff;
	
	public InfoView(SimCoreModel sm){
		sim_core = sm;
		buildAndShow();
	}

	private void buildAndShow() {
		infoView = new JFrame();
		infoView.setSize(200, 768);
		
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("Cameras");
		tree = new JTree(node);
		tree.addTreeSelectionListener(this);
		
		for (CameraController c : sim_core.getCameras()) {
			DefaultMutableTreeNode subnode = new DefaultMutableTreeNode(c);
			
			subnode.setUserObject(c);
			node.add(subnode);
		}
		
		JScrollPane scp = new JScrollPane(tree);
		
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		
		infostuff= new JPanel();
		
		split.add(scp);
		split.add(infostuff);
		
		infoView.add(split);
		infoView.setVisible(true);
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                tree.getLastSelectedPathComponent();
		if (node == null) return;
		Object nodeInfo = node.getUserObject();
				
		if(!node.isRoot()){
			System.out.println(node.getParent());
	        if (node.getParent().toString().equals("Cameras")) {
	            infostuff = new CameraPanel((CameraController) nodeInfo);
	        } 
		}
		else{
			System.out.println("root");
		}
        
	}
}

class CameraPanel extends JPanel{
	public CameraPanel(CameraController cc){
		JLabel n = new JLabel("Name: ");
		this.add(n);
		JTextField jtf = new JTextField(cc.getName());
		this.add(jtf);
		
		System.out.println(cc.getName());
		
		this.setVisible(true);
	}
}
