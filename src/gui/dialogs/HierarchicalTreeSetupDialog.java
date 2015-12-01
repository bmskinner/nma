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
import ij.IJ;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.ParseException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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

import analysis.ClusteringOptions;
import analysis.ClusteringOptions.ClusteringMethod;
import analysis.ClusteringOptions.HierarchicalClusterMethod;

public class HierarchicalTreeSetupDialog extends JDialog implements ActionListener, ChangeListener {

	private static final long serialVersionUID = 1L;


	private final JPanel contentPanel = new JPanel();
	
	private JPanel headingPanel;
	private JPanel optionsPanel;
	private JPanel footerPanel;
	private JPanel 		cardPanel;
	
	
	private JComboBox<HierarchicalClusterMethod> hierarchicalClusterMethodCheckBox;
	
	private JCheckBox useModalityCheckBox;
	private JSpinner modalityPointsSpinner;
	
	private JCheckBox useSimilarityMatrixCheckBox;
	
	private boolean readyToRun = false;
	
	private ClusteringOptions options;
	
	public HierarchicalTreeSetupDialog(MainWindow mw) {
		
		// modal dialog
		super(mw, true);
		this.setTitle("Tree building options");
		this.setLocationRelativeTo(null);
		
		try {
			setDefaults();
			createGUI();

		} catch (Exception e) {
			IJ.log("Error making gui: "+e.getMessage());
		}
		
	}
	
	public ClusteringOptions getOptions(){
		return this.options;
	}
		
	public boolean isReadyToRun(){
		return this.readyToRun;
	}
	
	private void setDefaults(){
		options = new ClusteringOptions(ClusteringSetupWindow.DEFAULT_CLUSTER_METHOD);
		options.setClusterNumber(ClusteringSetupWindow.DEFAULT_MANUAL_CLUSTER_NUMBER);
		options.setHierarchicalMethod(ClusteringSetupWindow.DEFAULT_HIERARCHICAL_METHOD);
		options.setIterations(ClusteringSetupWindow.DEFAULT_EM_ITERATIONS);
		options.setIncludeModality(ClusteringSetupWindow.DEFAULT_USE_MODALITY);
		options.setModalityRegions(ClusteringSetupWindow.DEFAULT_MODALITY_REGIONS);
		options.setUseSimilarityMatrix(ClusteringSetupWindow.DEFAULT_USE_SIMILARITY_MATRIX);
	}
	
	private JPanel createHierarchicalPanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		panel.add(new JLabel("Hierarchical clustering method:"));
		hierarchicalClusterMethodCheckBox = new JComboBox<HierarchicalClusterMethod>(HierarchicalClusterMethod.values());
		hierarchicalClusterMethodCheckBox.setSelectedItem(ClusteringSetupWindow.DEFAULT_HIERARCHICAL_METHOD);
		hierarchicalClusterMethodCheckBox.addActionListener(this);
		panel.add(hierarchicalClusterMethodCheckBox);

		return panel;
	}
	
	private JPanel createHeader(){

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		JLabel headingLabel = new JLabel("Set clustering options");
		panel.add(headingLabel);
		return panel;
	}
	
	private JPanel createFooter(){

		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.CENTER));
		JButton okButton = new JButton("OK");
		okButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				HierarchicalTreeSetupDialog.this.readyToRun = true;
				HierarchicalTreeSetupDialog.this.setVisible(false);			
			}
		});

		panel.add(okButton);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				HierarchicalTreeSetupDialog.this.dispose();			
			}
		});
		panel.add(cancelButton);
		return panel;
	}
	
	private void createGUI(){
		
		setSize(450, 300);
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
		   
	    
	    cardPanel = createHierarchicalPanel();
	    
	    useSimilarityMatrixCheckBox = new JCheckBox("Use similarity matrix");
	    useSimilarityMatrixCheckBox.addChangeListener(this);
	    methodPanel.add(useSimilarityMatrixCheckBox);
	    
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
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		useModalityCheckBox = new JCheckBox("Include modality");
		useModalityCheckBox.setSelected(ClusteringSetupWindow.DEFAULT_USE_MODALITY);
		useModalityCheckBox.addChangeListener(this);
		panel.add(useModalityCheckBox);
		
		panel.add(new JLabel("Number of modality points:"));
		
		SpinnerModel model =
				new SpinnerNumberModel(ClusteringSetupWindow.DEFAULT_MODALITY_REGIONS, //initial value
						1, //min
						20, //max
						1); //step
		
		modalityPointsSpinner = new JSpinner(model);
		modalityPointsSpinner.addChangeListener(this);
		modalityPointsSpinner.setEnabled(ClusteringSetupWindow.DEFAULT_USE_MODALITY);
		panel.add(modalityPointsSpinner);
		
		return panel;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		
		
		options.setHierarchicalMethod((HierarchicalClusterMethod) hierarchicalClusterMethodCheckBox.getSelectedItem());
						
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		try {
						
			if(e.getSource()==useModalityCheckBox){
				options.setIncludeModality(useModalityCheckBox.isSelected());
				if(useModalityCheckBox.isSelected()){
					modalityPointsSpinner.setEnabled(true);
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
					modalityPointsSpinner.setEnabled(true);
				}
				
			} 
			
			
			
			if(e.getSource()==modalityPointsSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				options.setModalityRegions(  (Integer) j.getValue());
			} 
		}catch (ParseException e1) {
			IJ.log("Error in spinners for Clustering options");
		}	
		
	}
}
