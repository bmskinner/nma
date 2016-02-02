/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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

import gui.Labels;
import gui.MainWindow;
import ij.IJ;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

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

import stats.DipTester;
import stats.NucleusStatistic;
import stats.Stats;
import analysis.AnalysisDataset;
import analysis.ClusteringOptions;
import analysis.ClusteringOptions.ClusteringMethod;
import analysis.ClusteringOptions.HierarchicalClusterMethod;
import components.generic.MeasurementScale;

public class HierarchicalTreeSetupDialog extends SettingsDialog implements ActionListener, ChangeListener {

	private static final long serialVersionUID = 1L;

	protected final JPanel contentPanel = new JPanel();
	
	protected JPanel headingPanel;
	protected JPanel optionsPanel;
	protected JPanel footerPanel;
	
	
	protected JComboBox<HierarchicalClusterMethod> hierarchicalClusterMethodCheckBox;
	
	protected AnalysisDataset dataset;
	
	protected JCheckBox includeProfilesCheckBox;
	protected Map<NucleusStatistic, JCheckBox> statBoxMap = new HashMap<NucleusStatistic, JCheckBox>();
	
	protected ClusteringOptions options;
	
	public HierarchicalTreeSetupDialog(MainWindow mw, AnalysisDataset dataset) {
		
		// modal dialog
		super(mw.getProgramLogger(), mw, true);
		this.dataset = dataset;
		this.setTitle("Tree building options");
		setSize(450, 300);
		this.setLocationRelativeTo(null);
		initialise();
		this.pack();
		this.setVisible(true);
		
	}
	
	/**
	 * Constructor that does not make panel visible
	 * @param mw
	 * @param title
	 */
	protected HierarchicalTreeSetupDialog(MainWindow mw, AnalysisDataset dataset, String title){
		super(mw.getProgramLogger(), mw, true);
		this.dataset = dataset;
		this.setTitle(title);
		setSize(450, 300);
		this.setLocationRelativeTo(null);
	}
	
	/**
	 * Set the default options and make the panel
	 */
	protected void initialise(){
		try {
			setDefaults();
			createGUI();

		} catch (Exception e) {
			programLogger.log(Level.SEVERE, "Error making dialog", e);
		}
	}
	
	/**
	 * Get the options saved in this panel
	 * @return
	 */
	public ClusteringOptions getOptions(){
		return this.options;
	}
			
	/**
	 * Set the default options
	 */
	protected void setDefaults(){
		options = new ClusteringOptions(ClusteringSetupDialog.DEFAULT_CLUSTER_METHOD);
		options.setClusterNumber(ClusteringSetupDialog.DEFAULT_MANUAL_CLUSTER_NUMBER);
		options.setHierarchicalMethod(ClusteringSetupDialog.DEFAULT_HIERARCHICAL_METHOD);
		options.setIterations(ClusteringSetupDialog.DEFAULT_EM_ITERATIONS);
		options.setUseSimilarityMatrix(ClusteringSetupDialog.DEFAULT_USE_SIMILARITY_MATRIX);
		options.setIncludeProfile(ClusteringSetupDialog.DEFAULT_INCLUDE_PROFILE);
	}
		
	protected JPanel createHeader(){

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		return panel;
	}
		
	protected void createGUI(){
		
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

	    JPanel optionsPanel = createOptionsPanel();
	   
	    contentPanel.add(optionsPanel, BorderLayout.CENTER);
		//---------------
		// end
		//---------------
//		this.pack();
//		this.setVisible(true);
	}
	
	private JPanel createOptionsPanel(){
		JPanel panel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		panel.setLayout(layout);
		
		List<JLabel> labels = new ArrayList<JLabel>();
		List<Component> fields = new ArrayList<Component>();
		
		

		hierarchicalClusterMethodCheckBox = new JComboBox<HierarchicalClusterMethod>(HierarchicalClusterMethod.values());
		hierarchicalClusterMethodCheckBox.setSelectedItem(ClusteringSetupDialog.DEFAULT_HIERARCHICAL_METHOD);
		hierarchicalClusterMethodCheckBox.addActionListener(this);
		
		
		JLabel clusterLabel = new JLabel("Cluster method");
		clusterLabel.setToolTipText(Labels.HIERARCHICAL_CLUSTER_METHOD);
		labels.add(clusterLabel);
		fields.add(hierarchicalClusterMethodCheckBox);

		includeProfilesCheckBox = new JCheckBox("");
		includeProfilesCheckBox.setSelected(ClusteringSetupDialog.DEFAULT_INCLUDE_PROFILE);
		includeProfilesCheckBox.addChangeListener(this);
		JLabel profileLabel = new JLabel("Include profiles");
		labels.add(profileLabel);
		fields.add(includeProfilesCheckBox);
		
		
		DecimalFormat pf = new DecimalFormat("#0.000"); 
		for(NucleusStatistic stat : NucleusStatistic.values()){
			
			String pval = "";
			try {
				double[] stats = dataset.getCollection().getNuclearStatistics(stat, MeasurementScale.PIXELS);
				double diptest 	= DipTester.getDipTestPValue(stats);
				pval = pf.format(diptest);		
			} catch (Exception e) {
				programLogger.log(Level.SEVERE, "Error getting p-value", e);
			}

			JCheckBox box = new JCheckBox("  p(uni) = "+pval);
			box.setForeground(Color.DARK_GRAY);
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
		
		
		options.setHierarchicalMethod((HierarchicalClusterMethod) hierarchicalClusterMethodCheckBox.getSelectedItem());
						
	}

	@Override
	public void stateChanged(ChangeEvent e) {
//		try {
			
			for(NucleusStatistic stat : NucleusStatistic.values()){
				JCheckBox box = statBoxMap.get(stat);
				options.setIncludeStatistic(stat, box.isSelected());
			}
			
			if(e.getSource()==includeProfilesCheckBox){
				options.setIncludeProfile(includeProfilesCheckBox.isSelected());				
			} 

		
	}
}
