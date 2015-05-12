package no.gui;

import ij.IJ;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import no.analysis.NucleusClusterer;

public class ClusteringSetupWindow extends JDialog {

	private static final long serialVersionUID = 1L;
	
	private static final int DEFAULT_MANUAL_CLUSTER_NUMBER = 2;
	private static final int DEFAULT_CLUSTER_METHOD = NucleusClusterer.HIERARCHICAL;

	private final JPanel contentPanel = new JPanel();
	
	private JPanel headingPanel;
	private JPanel optionsPanel;
	private JPanel footerPanel;
	
	private boolean readyToRun = false;

	private Map<String, Object> options = new HashMap<String, Object>();
	
	public ClusteringSetupWindow(MainWindow mw) {
		
		// modal dialog
		super(mw, true);
		
		try {
//			IJ.log("Creating gui");
			setDefaults();
			createGUI();

		} catch (Exception e) {
			IJ.log("Error making gui: "+e.getMessage());
		}
		
	}
	
	public Map<String, Object> getOptions(){
		return this.options;
	}
	
	public boolean isReadyToRun(){
		return this.readyToRun;
	}
	
	private void setDefaults(){
		options.put("type", ClusteringSetupWindow.DEFAULT_CLUSTER_METHOD);
		options.put("-N", ClusteringSetupWindow.DEFAULT_MANUAL_CLUSTER_NUMBER);
	}
	
	private void createGUI(){
		
		setBounds(100, 100, 450, 300);
		contentPanel.setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPanel);
		
		//---------------
		// panel for text labels
		//---------------
		headingPanel = new JPanel();
		headingPanel.setLayout(new BoxLayout(headingPanel, BoxLayout.Y_AXIS));
		JLabel headingLabel = new JLabel("Set clustering options");
		headingPanel.add(headingLabel);
		contentPanel.add(headingPanel, BorderLayout.NORTH);
		//---------------
		// buttons at bottom
		//---------------
		footerPanel = new JPanel();
		footerPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		JButton okButton = new JButton("OK");
		okButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				ClusteringSetupWindow.this.readyToRun = true;
				ClusteringSetupWindow.this.setVisible(false);			
			}
		});

		footerPanel.add(okButton);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				ClusteringSetupWindow.this.dispose();			
			}
		});
		footerPanel.add(cancelButton);
		contentPanel.add(footerPanel, BorderLayout.SOUTH);
//		IJ.log("Added buttons");
		//---------------
		// options in middle
		//---------------
		optionsPanel = new JPanel();
		optionsPanel.setLayout(new BoxLayout(optionsPanel, BoxLayout.Y_AXIS));
		
		//Create the radio buttons.
	    JRadioButton clusterHierarchicalButton = new JRadioButton("Hierarchical");
	    clusterHierarchicalButton.setSelected(true);
	    
	    JRadioButton clusterEMButton = new JRadioButton("Expectation maximisation");

	    //Group the radio buttons.
	    ButtonGroup clusterTypeGroup = new ButtonGroup();
	    clusterTypeGroup.add(clusterHierarchicalButton);
	    clusterTypeGroup.add(clusterEMButton);
	    	    
	    clusterHierarchicalButton.addActionListener(new ActionListener(){
	    	public void actionPerformed(ActionEvent e){

	    		JRadioButton button = (JRadioButton) e.getSource();

	    		// Set enabled based on button text (you can use whatever text you prefer)
	    		if(button.isSelected()){
	    			options.put("type", NucleusClusterer.HIERARCHICAL);
	    		} else {
	    			options.put("type", NucleusClusterer.EM);
	    		}
	    	}
	    });
	    
	    clusterEMButton.addActionListener(new ActionListener(){
	    	public void actionPerformed(ActionEvent e){

	    		JRadioButton button = (JRadioButton) e.getSource();

	    		// Set enabled based on button text (you can use whatever text you prefer)
	    		if(button.isSelected()){
	    			
	    			options.put("type", NucleusClusterer.EM);
	    		} else {
	    			options.put("type", NucleusClusterer.HIERARCHICAL);
	    		}
	    	}
	    });
	    
	    

	    optionsPanel.add(clusterHierarchicalButton);
	    optionsPanel.add(clusterEMButton);
	    
	    JLabel clusterCountLabel = new JLabel("Desired number of clusters:");
	    optionsPanel.add(clusterCountLabel);
	    
	    JRadioButton clusterAutoButton = new JRadioButton("Automatic");
	    clusterAutoButton.setSelected(true);
	    
	    JRadioButton clusterManualButton = new JRadioButton("Manual");
	    
	    ButtonGroup clusterNumberGroup = new ButtonGroup();
	    clusterNumberGroup.add(clusterAutoButton);
	    clusterNumberGroup.add(clusterManualButton);
	    
	    	    
	    optionsPanel.add(clusterAutoButton);
	    optionsPanel.add(clusterManualButton);
	    	    
	    SpinnerModel model =
	            new SpinnerNumberModel(2, //initial value
	                                   1, //min
	                                   100, //max
	                                   1); //step
	    
	    final JSpinner spinner = new JSpinner(model);
	    spinner.setEnabled(false);
	    optionsPanel.add(spinner);
	    
	    
	    clusterManualButton.addActionListener(new ActionListener(){
	    	public void actionPerformed(ActionEvent e){

	    		JRadioButton button = (JRadioButton) e.getSource();

	    		// Set enabled based on button text (you can use whatever text you prefer)
	    		if(button.isSelected()){
	    			spinner.setEnabled(true);
	    			options.put("-N", ClusteringSetupWindow.DEFAULT_MANUAL_CLUSTER_NUMBER);
	    		} else {
	    			spinner.setEnabled(false);
	    			options.remove("-N");
	    		}
	    	}
	    });
	    
	    clusterAutoButton.addActionListener(new ActionListener(){
	    	public void actionPerformed(ActionEvent e){

	    		JRadioButton button = (JRadioButton) e.getSource();

	    		// Set enabled based on button text (you can use whatever text you prefer)
	    		if(button.isSelected()){
	    			spinner.setEnabled(false);
	    			options.remove("-N");
	    		} else {
	    			spinner.setEnabled(true);
	    			options.put("-N", ClusteringSetupWindow.DEFAULT_MANUAL_CLUSTER_NUMBER);
	    		}
	    	}
	    });
	    
	    
	    spinner.addChangeListener(new ChangeListener(){
	    	
	    	public void stateChanged(ChangeEvent e) {
	            SpinnerModel model = spinner.getModel();
	            if (model instanceof SpinnerNumberModel) {
	                options.put("-N", model.getValue());
	            }
	        }
	    	
	    });
	    
	    contentPanel.add(optionsPanel, BorderLayout.CENTER);
//	    IJ.log("Added content");
		//---------------
		// end
		//---------------
		this.pack();
		this.setVisible(true);
	}
}
