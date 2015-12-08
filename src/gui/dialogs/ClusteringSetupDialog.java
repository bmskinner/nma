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
import stats.NucleusStatistic;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.ButtonGroup;
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

import analysis.ClusteringOptions.ClusteringMethod;
import analysis.ClusteringOptions.HierarchicalClusterMethod;

@SuppressWarnings("serial")
public class ClusteringSetupDialog extends HierarchicalTreeSetupDialog implements ActionListener, ChangeListener {
	
	static final int DEFAULT_MANUAL_CLUSTER_NUMBER = 2;
	static final ClusteringMethod DEFAULT_CLUSTER_METHOD = ClusteringMethod.HIERARCHICAL;
	static final HierarchicalClusterMethod DEFAULT_HIERARCHICAL_METHOD = HierarchicalClusterMethod.WARD;
	static final int DEFAULT_EM_ITERATIONS = 100;
	static final int DEFAULT_MODALITY_REGIONS = 2;
	static final boolean DEFAULT_USE_MODALITY = true;
	static final boolean DEFAULT_USE_SIMILARITY_MATRIX = false;
	static final boolean DEFAULT_INCLUDE_AREA = false;
	static final boolean DEFAULT_INCLUDE_ASPECT = false;
	static final boolean DEFAULT_INCLUDE_PROFILE = true;
	
	private JPanel 		cardPanel;
	
	private JSpinner clusterNumberSpinner;
	
	private JRadioButton clusterHierarchicalButton;
	private JRadioButton clusterEMButton;
		
	private JSpinner iterationsSpinner;

	
	public ClusteringSetupDialog(MainWindow mw) {

		super(mw, "Clustering options");
		this.initialise();
		this.pack();
		this.setVisible(true);

	}
	
	@Override
	protected void initialise(){
		try {
			setDefaults();
			createGUI();

		} catch (Exception e) {
			programLogger.log(Level.SEVERE, "Error making dialog", e);
		}
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
				new SpinnerNumberModel(ClusteringSetupDialog.DEFAULT_MANUAL_CLUSTER_NUMBER, //initial value
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
				new SpinnerNumberModel(ClusteringSetupDialog.DEFAULT_EM_ITERATIONS, //initial value
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
	
	
	@Override
	protected void createGUI(){
		
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
	    	    
	    JPanel includePanel = createOptionsPanel();

	    optionsPanel.add(methodPanel, BorderLayout.NORTH);
	    optionsPanel.add(cardPanel, BorderLayout.CENTER);
	    optionsPanel.add(includePanel, BorderLayout.SOUTH);
	   
	    
	    contentPanel.add(optionsPanel, BorderLayout.CENTER);
	}
	
	private JPanel createOptionsPanel(){
		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);
		
		List<JLabel> labels = new ArrayList<JLabel>();
		List<Component> fields = new ArrayList<Component>();
		

		includeProfilesCheckBox = new JCheckBox("");
		includeProfilesCheckBox.setSelected(ClusteringSetupDialog.DEFAULT_INCLUDE_PROFILE);
		includeProfilesCheckBox.addChangeListener(this);
		JLabel profileLabel = new JLabel("Include profiles");
		labels.add(profileLabel);
		fields.add(includeProfilesCheckBox);
		
		for(NucleusStatistic stat : NucleusStatistic.values()){
			JCheckBox box = new JCheckBox("");
			box.setSelected(false);
			box.addChangeListener(this);
			JLabel label = new JLabel(stat.toString());
			labels.add(label);
			fields.add(box);
			statBoxMap.put(stat, box);
		}
		
		this.addLabelTextRows(labels, fields, layout, panel);
		return panel;
		
	}


	@Override
	public void actionPerformed(ActionEvent arg0) {
		super.actionPerformed(arg0);
		
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

				
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		super.stateChanged(e);
		
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
			
		}catch (ParseException e1) {
			programLogger.log(Level.SEVERE, "Error in spinners for Clustering options", e1);
		}	
		
	}
}
