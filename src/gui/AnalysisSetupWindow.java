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
package gui;

import ij.IJ;
import ij.io.DirectoryChooser;
import ij.io.OpenDialog;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.ParseException;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import analysis.AnalysisOptions;
import analysis.AnalysisOptions.CannyOptions;
import components.nuclear.NucleusType;

public class AnalysisSetupWindow extends SettingsDialog implements ActionListener, ChangeListener {

	private AnalysisOptions analysisOptions = new AnalysisOptions();
	
	private static final int    DEFAULT_MIN_NUCLEUS_SIZE = 2000;
	private static final int    DEFAULT_MAX_NUCLEUS_SIZE = 10000;
	private static final double DEFAULT_MIN_NUCLEUS_CIRC = 0.2;
	private static final double DEFAULT_MAX_NUCLEUS_CIRC = 0.8;
	private static final int    DEFAULT_NUCLEUS_THRESHOLD = 36;
		
	private static final int    DEFAULT_PROFILE_WINDOW_SIZE = 15;
	
	private static final double    DEFAULT_SCALE = 1.0;
	
	private static final String DEFAULT_REFOLD_MODE = "Fast";
	
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	
	private CannyPanel 	nucleusCannyPanel;
	private JPanel 		nucleusThresholdPanel;
	private JPanel 		cardPanel;
	
	
	// nucleus detection method
	private JRadioButton nucleusThresholdButton = new JRadioButton("Threshold");
	private JRadioButton nucleusEdgeButton = new JRadioButton("Edge detection");
	private ButtonGroup nucleusDetectionMethodGroup;
	
	// other detection parameters

	private JSpinner txtMinNuclearSize = new JSpinner(new SpinnerNumberModel(DEFAULT_MIN_NUCLEUS_SIZE,	100, 50000, 1));
	private JSpinner txtMaxNuclearSize = new JSpinner(new SpinnerNumberModel(DEFAULT_MAX_NUCLEUS_SIZE,	100, 50000, 1));

	private JSpinner txtProfileWindowSize = new JSpinner(new SpinnerNumberModel(DEFAULT_PROFILE_WINDOW_SIZE,	5, 50, 1));

	private JSpinner nucleusThresholdSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_NUCLEUS_THRESHOLD,	0, 255, 1));

	private JSpinner minNuclearCircSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_MIN_NUCLEUS_CIRC,	0, 1, 0.05));
	private JSpinner maxNuclearCircSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_MAX_NUCLEUS_CIRC,	0, 1, 0.05));

	private JComboBox<NucleusType> nucleusSelectionBox;

	private JCheckBox refoldCheckBox;
	private JRadioButton refoldFastButton = new JRadioButton("Fast");
	private JRadioButton refoldIntensiveButton = new JRadioButton("Intensive");
	private JRadioButton refoldBrutalButton = new JRadioButton("Brutal");
	private ButtonGroup refoldModeGroup;
	
	private JSpinner scaleSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_SCALE,	0, 100, 0.001));


	/**
	 * Create the frame.
	 */
	public AnalysisSetupWindow(Logger logger) {
		super(logger);
		setModal(true); // ensure nothing happens until this window is closed
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); // disable the 'X'; we need to use the footer buttons 
		setDefaultOptions();
		createAndShowGUI();
		pack();
		setVisible(true);
	}
	
	/**
	 * Create the dialog with an existing set of options
	 * Allows settings to be reloaded.
	 */
	public AnalysisSetupWindow(AnalysisOptions options, Logger logger) {
		super(logger);
		setModal(true); // ensure nothing happens until this window is closed
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		analysisOptions = options;
		createAndShowGUI();
		pack();
		setVisible(true);
	}

	/**
	 * Get the current options 
	 * @return an AnalysisOptions
	 */
	public AnalysisOptions getOptions(){
		return this.analysisOptions;
	}

	public void createAndShowGUI(){
		setTitle("Create new analysis");
		setBounds(200, 100, 450, 626);
		this.setLocationRelativeTo(null);
		contentPane = new JPanel();
		contentPane.setLayout(new BorderLayout());
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);

		// add the combo box for nucleus type
		contentPane.add(makeNucleusTypePanel(), BorderLayout.NORTH);

		// add the buttons
		contentPane.add(makeLowerButtonPanel(), BorderLayout.SOUTH);
		
		contentPane.add(makeSettingsPanel(), BorderLayout.CENTER);	

	}

	/**
	 * Populate the analysis options with the defaults specified above
	 */
	public void setDefaultOptions(){
		analysisOptions.setNucleusThreshold(DEFAULT_NUCLEUS_THRESHOLD);

		analysisOptions.setMinNucleusSize(DEFAULT_MIN_NUCLEUS_SIZE);
		analysisOptions.setMaxNucleusSize(DEFAULT_MAX_NUCLEUS_SIZE);

		analysisOptions.setMinNucleusCirc(DEFAULT_MIN_NUCLEUS_CIRC);
		analysisOptions.setMaxNucleusCirc(DEFAULT_MAX_NUCLEUS_CIRC);

		analysisOptions.setAngleProfileWindowSize(DEFAULT_PROFILE_WINDOW_SIZE);

		analysisOptions.setPerformReanalysis(false);
		analysisOptions.setRealignMode(true);

		analysisOptions.setRefoldNucleus(false);
		analysisOptions.setRefoldMode(DEFAULT_REFOLD_MODE);

		analysisOptions.setXoffset(0);
		analysisOptions.setYoffset(0);
		
		analysisOptions.setScale(DEFAULT_SCALE);
				
		analysisOptions.setNucleusType(NucleusType.RODENT_SPERM);
		
		CannyOptions nucleusCannyOptions = analysisOptions.getCannyOptions("nucleus");
		
		nucleusCannyOptions.setUseCanny(true);
		nucleusCannyOptions.setCannyAutoThreshold(false);
		nucleusCannyOptions.setLowThreshold( (float) CannyOptions.DEFAULT_CANNY_LOW_THRESHOLD);
		nucleusCannyOptions.setHighThreshold((float) CannyOptions.DEFAULT_CANNY_HIGH_THRESHOLD);
		nucleusCannyOptions.setKernelRadius((float) CannyOptions.DEFAULT_CANNY_KERNEL_RADIUS);
		nucleusCannyOptions.setKernelWidth(CannyOptions.DEFAULT_CANNY_KERNEL_WIDTH);
		nucleusCannyOptions.setClosingObjectRadius(CannyOptions.DEFAULT_CLOSING_OBJECT_RADIUS);
		
		nucleusCannyOptions.setUseKuwahara(CannyOptions.DEFAULT_USE_KUWAHARA);
		nucleusCannyOptions.setKuwaharaKernel(CannyOptions.DEFAULT_KUWAHARA_KERNEL_RADIUS);
		nucleusCannyOptions.setFlattenImage(CannyOptions.DEFAULT_FLATTEN_CHROMOCENTRES);
		nucleusCannyOptions.setFlattenThreshold(CannyOptions.DEFAULT_FLATTEN_THRESHOLD);
		
		
		CannyOptions tailCannyOptions = analysisOptions.getCannyOptions("tail");
		
		tailCannyOptions.setUseCanny(true);
		tailCannyOptions.setCannyAutoThreshold(false);
		tailCannyOptions.setLowThreshold( (float) CannyOptions.DEFAULT_CANNY_TAIL_LOW_THRESHOLD);
		tailCannyOptions.setHighThreshold((float) CannyOptions.DEFAULT_CANNY_TAIL_HIGH_THRESHOLD);
		tailCannyOptions.setKernelRadius((float) CannyOptions.DEFAULT_CANNY_KERNEL_RADIUS);
		tailCannyOptions.setKernelWidth( CannyOptions.DEFAULT_CANNY_KERNEL_WIDTH);
		tailCannyOptions.setClosingObjectRadius( CannyOptions.DEFAULT_TAIL_CLOSING_OBJECT_RADIUS);
		
		tailCannyOptions.setUseKuwahara(false);
		tailCannyOptions.setKuwaharaKernel(CannyOptions.DEFAULT_KUWAHARA_KERNEL_RADIUS);
		tailCannyOptions.setFlattenImage(false);
		tailCannyOptions.setFlattenThreshold(CannyOptions.DEFAULT_FLATTEN_THRESHOLD);
		
	}

	private JPanel makeNucleusTypePanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.CENTER));

		//		JLabel lblNucleusType = new JLabel("Nucleus type");
		panel.add(new JLabel("Nucleus type"));

		nucleusSelectionBox = new JComboBox<NucleusType>(NucleusType.values());
		nucleusSelectionBox.setSelectedItem(NucleusType.RODENT_SPERM);
//		nucleusSelectionBox.setSelectedIndex(1);
		nucleusSelectionBox.setActionCommand("Nucleus type");
		nucleusSelectionBox.addActionListener(this);
		panel.add(nucleusSelectionBox);
		return panel;
	}

	private JPanel makeLowerButtonPanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());

		JButton btnOk = new JButton("OK");
		btnOk.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(getImageDirectory()){
					
					// probe the first image
					// show the results of the current settings
					ImageProber p = new NucleusDetectionImageProber(analysisOptions, programLogger, analysisOptions.getFolder());
					if(p.getOK()==false){
					
						
					} else {
						
						// ok, close the window
						AnalysisSetupWindow.this.setVisible(false);
					}
					
				} else {
					analysisOptions = null;
					AnalysisSetupWindow.this.setVisible(false);
				}
				
			}
		});

		panel.add(btnOk);

		JButton btnCancel = new JButton("Cancel");
		btnCancel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				analysisOptions = null;
				AnalysisSetupWindow.this.setVisible(false);
			}
		});
		panel.add(btnCancel);
		return panel;
	}

	private JPanel makeSettingsPanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = GridBagConstraints.REMAINDER;     //end row

		panel.add(makeNucleusDetectionSettingsPanel(),c);
		panel.add(makeDetectionSettingsPanel(), c);
		
		panel.add(makeRefoldSettingsPanel(), c);

		return panel;
	}
	
	/**
	 * When threshold is the chosen option, show a spinner for the 
	 * threshold value
	 * @return
	 */
	private JPanel makeNucleusDetectionSettingsPanel(){
		
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createTitledBorder("Nucleus detection"));
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		
		JPanel detectionSwitchPanel = makeNucleusDetectionSwitchPanel();
		panel.add(detectionSwitchPanel);
		
		nucleusCannyPanel = new CannyPanel(analysisOptions.getCannyOptions("nucleus"));
		
		nucleusThresholdPanel = makeNucleusThresholdPanel();
		
		cardPanel = new JPanel(new CardLayout());
		cardPanel.add(nucleusCannyPanel, "CannyPanel");
		cardPanel.add(nucleusThresholdPanel, "ThresholdPanel");
		CardLayout cl = (CardLayout)(cardPanel.getLayout());
	    cl.show(cardPanel, "CannyPanel");
	    
	    panel.add(cardPanel);
			
		return panel;
	}
	
	
	/**
	 * A panel with the radio buttons to choose edge detection or
	 * threshold for the nucleus
	 * @return
	 */
	private JPanel makeNucleusDetectionSwitchPanel(){
		JPanel panel = new JPanel(new FlowLayout());
		
		nucleusThresholdButton.setSelected(false);
		nucleusEdgeButton.setSelected(true);
		nucleusThresholdButton.setActionCommand("NucleusDetectionThreshold");
		nucleusEdgeButton.setActionCommand("NucleusDetectionEdge");

		//Group the radio buttons.
		nucleusDetectionMethodGroup = new ButtonGroup();
		nucleusDetectionMethodGroup.add(nucleusThresholdButton);
		nucleusDetectionMethodGroup.add(nucleusEdgeButton);
		
		nucleusThresholdButton.addActionListener(this);
		nucleusEdgeButton.addActionListener(this);
		
		
		panel.add(nucleusThresholdButton);
		panel.add(nucleusEdgeButton);
		
		return panel;
	}
	
	private JPanel makeNucleusThresholdPanel(){
		JPanel panel = new JPanel(new FlowLayout());
		JLabel nucleusThresholdLabel = new JLabel("Nucleus threshold");
		
		panel.add(nucleusThresholdLabel);
		panel.add(nucleusThresholdSpinner);
		nucleusThresholdSpinner.addChangeListener(this);
		
		return panel;
	}
	

	private JPanel makeDetectionSettingsPanel(){

		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createTitledBorder("Detection settings"));
		panel.setLayout(new GridBagLayout());

		JLabel[] labels = new JLabel[6];
		JSpinner[] fields = new JSpinner[6];

		labels[0] = new JLabel("Min nucleus size");
		labels[1] = new JLabel("Max nucleus size");
		labels[2] = new JLabel("Min nucleus circ");
		labels[3] = new JLabel("Max nucleus circ");
		labels[4] = new JLabel("Profile window");
		labels[5] = new JLabel("Scale (microns/pixel)");


		fields[0] = txtMinNuclearSize;
		fields[1] = txtMaxNuclearSize;
		fields[2] = minNuclearCircSpinner;
		fields[3] = maxNuclearCircSpinner;
		fields[4] = txtProfileWindowSize;
		fields[5] = scaleSpinner;
		
		txtMinNuclearSize.addChangeListener(this);
		txtMaxNuclearSize.addChangeListener(this);
		minNuclearCircSpinner.addChangeListener(this);
		maxNuclearCircSpinner.addChangeListener(this);
		txtProfileWindowSize.addChangeListener(this);
		scaleSpinner.addChangeListener(this);

		addLabelTextRows(labels, fields, new GridBagLayout(), panel );

		return panel;
	}

	private JPanel makeRefoldSettingsPanel(){

		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createTitledBorder("Refold settings"));

		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		refoldCheckBox = new JCheckBox("Refold nucleus");
		refoldCheckBox.setSelected(false);
		refoldCheckBox.setActionCommand("Refold nucleus");
		refoldCheckBox.addActionListener(this);
		panel.add(refoldCheckBox);


		//Create the radio buttons.
		refoldFastButton.setSelected(true);
		
		refoldFastButton.setActionCommand("Fast");
		refoldIntensiveButton.setActionCommand("Intensive");
		refoldBrutalButton.setActionCommand("Brutal");

		//Group the radio buttons.
		refoldModeGroup = new ButtonGroup();
		refoldModeGroup.add(refoldFastButton);
		refoldModeGroup.add(refoldIntensiveButton);
		refoldModeGroup.add(refoldBrutalButton);
		
		panel.add(refoldFastButton);
		panel.add(refoldIntensiveButton);
		panel.add(refoldBrutalButton);
		
		refoldFastButton.setEnabled(false);
		refoldIntensiveButton.setEnabled(false);
		refoldBrutalButton.setEnabled(false);

		return panel;
	}

	/**
	 * Currently not enabled. Can hold settings for automated remapping of pre- and 
	 * post-FISHed nuclei
	 * @return
	 */
//	private JPanel makeRenappingSettingsPanel(){
//
//		JPanel panel = new JPanel();
//		//	panel.setLayout(new GridBagLayout());
//
//
//		//----------------------
//		// make remapping panel
//		//----------------------
//
//		//	panel.add(new Box.Filler(minSize, prefSize, maxSize));
//		//	
//		//	JLabel lblReanalysisSettings = new JLabel("Refolding settings");
//		//	panel.add(lblRefoldSettings);
//		//	JPanel refoldPanel = new JPanel();
//
//
//		return panel;
//	}	

	@Override
	public void actionPerformed(ActionEvent e) {

		if(e.getActionCommand().equals("NucleusDetectionThreshold")){
			this.analysisOptions.getCannyOptions("nucleus").setUseCanny(false);
			
			CardLayout cl = (CardLayout)(cardPanel.getLayout());
		    cl.show(cardPanel, "ThresholdPanel");

		}
		
		if(e.getActionCommand().equals("NucleusDetectionEdge")){
			this.analysisOptions.getCannyOptions("nucleus").setUseCanny(true);
			CardLayout cl = (CardLayout)(cardPanel.getLayout());
		    cl.show(cardPanel, "CannyPanel");
		}
		
//		if(e.getActionCommand().equals("CannyAutoThreshold")){
//
//			if(cannyAutoThresholdCheckBox.isSelected()){
//				analysisOptions.getCannyOptions("nucleus").setCannyAutoThreshold(true);
//				cannyLowThreshold.setEnabled(false);
//				cannyHighThreshold.setEnabled(false);
//			} else {
//				analysisOptions.getCannyOptions("nucleus").setCannyAutoThreshold(false);
//				cannyLowThreshold.setEnabled(true);
//				cannyHighThreshold.setEnabled(true);
//			}
//		}
		
		if(e.getActionCommand().equals("Nucleus type")){
			NucleusType type = (NucleusType) nucleusSelectionBox.getSelectedItem();
			this.analysisOptions.setNucleusType(type);
			
			if(type.equals(NucleusType.ASYMMETRIC)){
				this.analysisOptions.setNucleusType(NucleusType.ROUND); // not set up for this yet
				this.analysisOptions.setMinNucleusCirc(  0.0 );
				this.analysisOptions.setMaxNucleusCirc(  1.0 );
				minNuclearCircSpinner.setValue(0.0);
				maxNuclearCircSpinner.setValue(1.0);
			}

			
			if(type.equals(NucleusType.ROUND)){
				this.analysisOptions.setMinNucleusCirc(  0.0 );
				this.analysisOptions.setMaxNucleusCirc(  1.0 );
				minNuclearCircSpinner.setValue(0.0);
				maxNuclearCircSpinner.setValue(1.0);
				
			}
			if(type.equals(NucleusType.RODENT_SPERM)){
				this.analysisOptions.setMinNucleusCirc(  0.2 );
				this.analysisOptions.setMaxNucleusCirc(  0.8 );
				minNuclearCircSpinner.setValue(0.2);
				maxNuclearCircSpinner.setValue(0.8);
			}
			if(type.equals(NucleusType.PIG_SPERM)){
				this.analysisOptions.setMinNucleusCirc(  0.2 );
				this.analysisOptions.setMaxNucleusCirc(  0.8 );
				minNuclearCircSpinner.setValue(0.2);
				maxNuclearCircSpinner.setValue(0.8);
			}
		}


		if(e.getActionCommand().equals("Refold nucleus")){
			if(refoldCheckBox.isSelected()){
				// toggle radios on
				refoldFastButton.setEnabled(true);
				refoldIntensiveButton.setEnabled(true);
				refoldBrutalButton.setEnabled(true);
				this.analysisOptions.setRefoldNucleus(true);
				this.analysisOptions.setRefoldMode( refoldModeGroup.getSelection().getActionCommand()  );
				
			} else {
				// toggle radios off
				refoldFastButton.setEnabled(false);
				refoldIntensiveButton.setEnabled(false);
				refoldBrutalButton.setEnabled(false);
				this.analysisOptions.setRefoldNucleus(false);
			}

		}
		
		if(e.getActionCommand().equals("Fast")){
			this.analysisOptions.setRefoldMode( "Fast"  );
		}
		
		if(e.getActionCommand().equals("Intensive")){
			this.analysisOptions.setRefoldMode( "Intensive"  );
		}
		
		if(e.getActionCommand().equals("Brutal")){
			this.analysisOptions.setRefoldMode( "Brutal"  );
		}

	}

	@Override
	public void stateChanged(ChangeEvent e) {

		try{

			if(e.getSource()==nucleusThresholdSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();	
				this.analysisOptions.setNucleusThreshold(  (Integer) j.getValue());
			}

			if(e.getSource()==txtMinNuclearSize){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				
				// ensure never larger than max
				if( (Integer) j.getValue() > (Integer) txtMaxNuclearSize.getValue() ){
					j.setValue( txtMaxNuclearSize.getValue() );
				}
				
				this.analysisOptions.setMinNucleusSize(  (Integer) j.getValue());
			}
			
			if(e.getSource()==txtMaxNuclearSize){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				
				// ensure never smaller than min
				if( (Integer) j.getValue() < (Integer) txtMinNuclearSize.getValue() ){
					j.setValue( txtMinNuclearSize.getValue() );
				}
				
				this.analysisOptions.setMaxNucleusSize(  (Integer) j.getValue());
			}
			
			if(e.getSource()==minNuclearCircSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				
				// ensure never larger than max
				if( (Double) j.getValue() > (Double) maxNuclearCircSpinner.getValue() ){
					j.setValue( maxNuclearCircSpinner.getValue() );
				}
				
				this.analysisOptions.setMinNucleusCirc(  (Double) j.getValue());
			}
			
			if(e.getSource()==maxNuclearCircSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				
				// ensure never smaller than min
				if( (Double) j.getValue() < (Double) minNuclearCircSpinner.getValue() ){
					j.setValue( minNuclearCircSpinner.getValue() );
				}
				
				this.analysisOptions.setMaxNucleusCirc(  (Double) j.getValue());
			}
						
			if(e.getSource()==txtProfileWindowSize){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				this.analysisOptions.setAngleProfileWindowSize(  (Integer) j.getValue());
			}	
			
			if(e.getSource()==scaleSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				this.analysisOptions.setScale(  (Double) j.getValue());
			}	
			
		} catch (ParseException e1) {
			IJ.log("Parsing error in JSpinner");
		}


	}

	
	private boolean getImageDirectory(){

		DirectoryChooser localOpenDialog = new DirectoryChooser("Select directory of images...");
		String folderName = localOpenDialog.getDirectory();

		if(folderName==null) return false; // user cancelled
		analysisOptions.setFolder( new File(folderName));

		if(analysisOptions.isReanalysis()){
			OpenDialog fileDialog = new OpenDialog("Select a mapping file...");
			String fileName = fileDialog.getPath();
			if(fileName==null) return false;
			analysisOptions.setMappingFile(new File(fileName));
		}
		return true;
	}
}

