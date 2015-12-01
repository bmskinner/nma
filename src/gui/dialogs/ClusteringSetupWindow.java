/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package gui.dialogs;

import gui.MainWindow;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import analysis.ClusteringOptions;
import analysis.ClusteringOptions.ClusteringMethod;
import analysis.ClusteringOptions.HierarchicalClusterMethod;

public class ClusteringSetupWindow extends SettingsDialog implements ActionListener, ChangeListener {

	private static final long serialVersionUID = 1L;
	
	static final int DEFAULT_MANUAL_CLUSTER_NUMBER = 2;
	static final ClusteringMethod DEFAULT_CLUSTER_METHOD = ClusteringMethod.HIERARCHICAL;
	static final HierarchicalClusterMethod DEFAULT_HIERARCHICAL_METHOD = HierarchicalClusterMethod.WARD;
	static final int DEFAULT_EM_ITERATIONS = 100;
	static final int DEFAULT_MODALITY_REGIONS = 2;
	static final boolean DEFAULT_USE_MODALITY = true;
	static final boolean DEFAULT_USE_SIMILARITY_MATRIX = false;
	

	private final JPanel contentPanel = new JPanel();
	
	private JPanel headingPanel;
	private JPanel optionsPanel;
	private JPanel footerPanel;
	private JPanel 		cardPanel;
	
	private JSpinner clusterNumberSpinner;
	
	private JRadioButton clusterHierarchicalButton;
	private JRadioButton clusterEMButton;
	
	private JRadioButton clusterNumberAutoButton;
	private JRadioButton clusterManualButton;
	
	private JComboBox<HierarchicalClusterMethod> hierarchicalClusterMethodCheckBox;
	private JSpinner iterationsSpinner;
	
	private JCheckBox useModalityCheckBox;
	private JSpinner modalityPointsSpinner;
	
	private JCheckBox useSimilarityMatrixCheckBox;
	
	private ClusteringOptions options;
	
	public ClusteringSetupWindow(MainWindow mw) {
		
		// modal dialog
		super(mw.getProgramLogger(), mw, true);
		this.setTitle("Clustering options");
		this.setLocationRelativeTo(null);
		
		try {
			setDefaults();
			createGUI();

		} catch (Exception e) {
			programLogger.log(Level.SEVERE, "Error making dialog", e);
		}
		
	}
	
	public ClusteringOptions getOptions(){
		return this.options;
	}
			
	private void setDefaults(){
		options = new ClusteringOptions(ClusteringSetupWindow.DEFAULT_CLUSTER_METHOD);
		options.setClusterNumber(DEFAULT_MANUAL_CLUSTER_NUMBER);
		options.setHierarchicalMethod(DEFAULT_HIERARCHICAL_METHOD);
		options.setIterations(DEFAULT_EM_ITERATIONS);
		options.setIncludeModality(DEFAULT_USE_MODALITY);
		options.setModalityRegions(DEFAULT_MODALITY_REGIONS);
		options.setUseSimilarityMatrix(DEFAULT_USE_SIMILARITY_MATRIX);
	}
	
	private JPanel createHierarchicalPanel(){
		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);
		
		List<JLabel> labels = new ArrayList<JLabel>();
		List<Component> fields = new ArrayList<Component>();

		hierarchicalClusterMethodCheckBox = new JComboBox<HierarchicalClusterMethod>(HierarchicalClusterMethod.values());
		hierarchicalClusterMethodCheckBox.setSelectedItem(DEFAULT_HIERARCHICAL_METHOD);
		hierarchicalClusterMethodCheckBox.addActionListener(this);
		
		labels.add(new JLabel("Cluster method"));
		fields.add(hierarchicalClusterMethodCheckBox);

		SpinnerModel model =
				new SpinnerNumberModel(ClusteringSetupWindow.DEFAULT_MANUAL_CLUSTER_NUMBER, //initial value
						1, //min
						100, //max
						1); //step

		clusterNumberSpinner = new JSpinner(model);
		clusterNumberSpinner.setEnabled(true);

		labels.add(new JLabel("Cluster number"));
		fields.add(clusterNumberSpinner);

		clusterNumberSpinner.addChangeListener(this);
		
		this.addLabelTextRows(labels, fields, layout, panel);
		return panel;
	}
	
	private JPanel createEMPanel(){

		JPanel panel = new JPanel();
		
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);
		
		List<JLabel> labels = new ArrayList<JLabel>();
		List<Component> fields = new ArrayList<Component>();
		
		

		SpinnerModel model =
				new SpinnerNumberModel(ClusteringSetupWindow.DEFAULT_EM_ITERATIONS, //initial value
						1, //min
						1000, //max
						1); //step

		iterationsSpinner = new JSpinner(model);
		iterationsSpinner.addChangeListener(this);
		
		labels.add(new JLabel("Iterations"));
		fields.add(iterationsSpinner);

		this.addLabelTextRows(labels, fields, layout, panel);
		return panel;
	}
	
	private JPanel createHeader(){

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		return panel;
	}
	
	private JPanel createFooter(){

		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.CENTER));
		JButton okButton = new JButton("OK");
		okButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				ClusteringSetupWindow.this.readyToRun = true;
				ClusteringSetupWindow.this.setVisible(false);			
			}
		});

		panel.add(okButton);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				ClusteringSetupWindow.this.dispose();			
			}
		});
		panel.add(cancelButton);
		return panel;
	}
	
	private void createGUI(){
		
		setBounds(100, 100, 450, 300);
		contentPanel.setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPanel);
		
		//---------------
		// panel for text labels
		//---------------
		headingPanel = createHeader();
		contentPanel.add(headingPanel, BorderLayout.NORTH);
		//---------------
		// buttons at bottom
		//---------------
		footerPanel = createFooter();
		contentPanel.add(footerPanel, BorderLayout.SOUTH);

		//---------------
		// options in middle
		//---------------
		optionsPanel = new JPanel();
		optionsPanel.setLayout(new BorderLayout());
		
		JPanel methodPanel = new JPanel(new FlowLayout()); 
		
		//Create the radio buttons.
	    clusterHierarchicalButton = new JRadioButton("Hierarchical");
	    clusterHierarchicalButton.setSelected(true);
	    
	    clusterEMButton = new JRadioButton("Expectation maximisation");

	    //Group the radio buttons.
	    ButtonGroup clusterTypeGroup = new ButtonGroup();
	    clusterTypeGroup.add(clusterHierarchicalButton);
	    clusterTypeGroup.add(clusterEMButton);
	    	    
	    clusterHierarchicalButton.addActionListener(this);
	    clusterEMButton.addActionListener(this);
	    
	    
	    cardPanel = new JPanel(new CardLayout());
		cardPanel.add(createHierarchicalPanel(), "HierarchicalPanel");
		cardPanel.add(createEMPanel(), "EMPanel");
		CardLayout cl = (CardLayout)(cardPanel.getLayout());
	    cl.show(cardPanel, "HierarchicalPanel");


	    methodPanel.add(clusterHierarchicalButton);
	    methodPanel.add(clusterEMButton);
	    	    
	    JPanel modalityPanel = createModalityPanel();

	    optionsPanel.add(methodPanel, BorderLayout.NORTH);
	    optionsPanel.add(cardPanel, BorderLayout.CENTER);
	    optionsPanel.add(modalityPanel, BorderLayout.SOUTH);
	   
	    
	    contentPanel.add(optionsPanel, BorderLayout.CENTER);
		//---------------
		// end
		//---------------
		this.pack();
		this.setVisible(true);
	}
	
	private JPanel createModalityPanel(){
		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);
		
		List<JLabel> labels = new ArrayList<JLabel>();
		List<Component> fields = new ArrayList<Component>();
		
		useModalityCheckBox = new JCheckBox("");
		useModalityCheckBox.setSelected(DEFAULT_USE_MODALITY);
		useModalityCheckBox.addChangeListener(this);
		
		labels.add(new JLabel("Include modality"));
		fields.add(useModalityCheckBox);
		
		SpinnerModel model =
				new SpinnerNumberModel(ClusteringSetupWindow.DEFAULT_MODALITY_REGIONS, //initial value
						1, //min
						20, //max
						1); //step
		
		modalityPointsSpinner = new JSpinner(model);
		modalityPointsSpinner.addChangeListener(this);
		modalityPointsSpinner.setEnabled(DEFAULT_USE_MODALITY);
		labels.add(new JLabel("Modality points"));
		fields.add(modalityPointsSpinner);
		
		useSimilarityMatrixCheckBox = new JCheckBox("");
	    useSimilarityMatrixCheckBox.addChangeListener(this);
	    labels.add(new JLabel("Similarity matrix"));
		fields.add(useSimilarityMatrixCheckBox);
		this.addLabelTextRows(labels, fields, layout, panel);
		return panel;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		
		// Set enabled based on button text (you can use whatever text you prefer)
		if(clusterHierarchicalButton.isSelected()){
			
			CardLayout cl = (CardLayout)(cardPanel.getLayout());
		    cl.show(cardPanel, "HierarchicalPanel");
		    
			options.setType(ClusteringMethod.HIERARCHICAL);
			
			clusterNumberSpinner.setEnabled(true);
			options.setClusterNumber( (Integer) clusterNumberSpinner.getValue());
			options.setHierarchicalMethod((HierarchicalClusterMethod) hierarchicalClusterMethodCheckBox.getSelectedItem());
		} 
		
		if(clusterEMButton.isSelected()){
			
			CardLayout cl = (CardLayout)(cardPanel.getLayout());
		    cl.show(cardPanel, "EMPanel");
			
			options.setType(ClusteringMethod.EM);

		} 
		

		if(clusterManualButton.isSelected()){
			clusterNumberSpinner.setEnabled(true);
		}

		if(clusterNumberAutoButton.isSelected()){
			clusterNumberSpinner.setEnabled(false);
		}
				
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		try {
			if(e.getSource()==clusterNumberSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				options.setClusterNumber(  (Integer) j.getValue());
			} 
			if(e.getSource()==iterationsSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				options.setIterations(  (Integer) j.getValue());
			} 
			
			if(e.getSource()==useModalityCheckBox){
				options.setIncludeModality(useModalityCheckBox.isSelected());
				if(useModalityCheckBox.isSelected()){
					modalityPointsSpinner.setEnabled(true);
					options.setModalityRegions(  (Integer) modalityPointsSpinner.getValue());
				} else {
					modalityPointsSpinner.setEnabled(false);
				}
				
			} 
			
			if(e.getSource()==useSimilarityMatrixCheckBox){
				options.setUseSimilarityMatrix(useSimilarityMatrixCheckBox.isSelected());
				if(useSimilarityMatrixCheckBox.isSelected()){
					useModalityCheckBox.setEnabled(false);
					modalityPointsSpinner.setEnabled(false);
				} else {
					
					useModalityCheckBox.setEnabled(true);
					if(useModalityCheckBox.isSelected()){
						modalityPointsSpinner.setEnabled(true);
					} else {
						modalityPointsSpinner.setEnabled(false);
					}
				}
				
			} 
			
			
			
			if(e.getSource()==modalityPointsSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				options.setModalityRegions(  (Integer) j.getValue());
			} 
		}catch (ParseException e1) {
			programLogger.log(Level.SEVERE, "Error in spinners for Clustering options", e1);
		}	
		
	}
}
