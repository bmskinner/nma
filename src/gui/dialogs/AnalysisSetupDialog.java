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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import utility.Constants;
import analysis.AnalysisDataset;
import analysis.DefaultAnalysisOptions;
import analysis.DefaultCannyOptions;
import analysis.IAnalysisDataset;
import analysis.IAnalysisOptions;
import analysis.ICannyOptions;
import analysis.IMutableAnalysisOptions;
import analysis.IMutableDetectionOptions;
import analysis.nucleus.DefaultNucleusDetectionOptions;
import components.nuclear.NucleusType;

public class AnalysisSetupDialog extends SettingsDialog implements ActionListener, ChangeListener {

	private IMutableAnalysisOptions analysisOptions = new DefaultAnalysisOptions();
	
	private static final int    DEFAULT_MIN_NUCLEUS_SIZE = 2000;
	private static final int    DEFAULT_MAX_NUCLEUS_SIZE = 10000;
	private static final double DEFAULT_MIN_NUCLEUS_CIRC = 0.2;
	private static final double DEFAULT_MAX_NUCLEUS_CIRC = 0.8;
	private static final int    DEFAULT_NUCLEUS_THRESHOLD = 36;
		
	private static final double    DEFAULT_PROFILE_WINDOW_SIZE = 0.05;
	
	private static final double    DEFAULT_SCALE = 1.0;
	
	private static final String DEFAULT_REFOLD_MODE = "Fast";
	
	private static final String DEFAULT_CHANNEL_NAME = "Blue";
	private static final int    DEFAULT_CHANNEL      = 2;
	
	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	
	private CannyPanel 	nucleusCannyPanel;
	private JPanel 		nucleusThresholdPanel;
	private JPanel 		cardPanel;
	
	
	// nucleus detection method
	private JRadioButton nucleusThresholdButton = new JRadioButton("Threshold");
	private JRadioButton nucleusEdgeButton = new JRadioButton("Edge detection");
	private ButtonGroup nucleusDetectionMethodGroup;
	
	private JComboBox<String> channelSelection = new JComboBox<String>(channelOptionStrings);
	
	// other detection parameters

	private JSpinner txtMinNuclearSize = new JSpinner(new SpinnerNumberModel( new Integer(DEFAULT_MIN_NUCLEUS_SIZE), new Integer(100), null, new Integer(1)));
	private JSpinner txtMaxNuclearSize = new JSpinner(new SpinnerNumberModel( new Integer(DEFAULT_MAX_NUCLEUS_SIZE), new Integer(100), null, new Integer(1)));

	private JSpinner txtProfileWindowSize = new JSpinner(new SpinnerNumberModel(DEFAULT_PROFILE_WINDOW_SIZE,	0, 1, 0.01));

	private JSpinner nucleusThresholdSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_NUCLEUS_THRESHOLD,	0, 255, 1));

	private JSpinner minNuclearCircSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_MIN_NUCLEUS_CIRC,	0, 1, 0.05));
	private JSpinner maxNuclearCircSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_MAX_NUCLEUS_CIRC,	0, 1, 0.05));

	private JComboBox<NucleusType> nucleusSelectionBox;

	private JCheckBox refoldCheckBox;
	
	private JSpinner scaleSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_SCALE,	1, 100000, 1));

	private JCheckBox keepFailedheckBox = new JCheckBox();
	
	private Collection<IAnalysisDataset> openDatasets; // the other datasets open in the program, for copying options

	/**
	 * Create the frame.
	 */
	public AnalysisSetupDialog(Collection<IAnalysisDataset> datasets) {
		this(datasets, null);
	}
	
	/**
	 * Create the frame.
	 */
	public AnalysisSetupDialog(Collection<IAnalysisDataset> datasets, File folder) {
		super();
		
		IMutableDetectionOptions nucleusOptions = new DefaultNucleusDetectionOptions(folder);
		analysisOptions.setDetectionOptions(IAnalysisOptions.NUCLEUS, nucleusOptions);

		openDatasets = datasets;
		setModal(true); // ensure nothing happens until this window is closed
		
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE); // disable the 'X'; we need to use the footer buttons 
		
		this.addWindowListener(new WindowAdapter() {

			public void windowClosing(WindowEvent e) {
				analysisOptions = null;
				AnalysisSetupDialog.this.setVisible(false);
			}


		});
		setDefaultOptions();
		createAndShowGUI();
		pack();
		setVisible(true);
		
	}
	
	/**
	 * Create the dialog with an existing set of options
	 * Allows settings to be reloaded.
	 */
	public AnalysisSetupDialog(List<IAnalysisDataset> datasets, IMutableAnalysisOptions options) {
		super();
		try {
			
			openDatasets = datasets;
			setModal(true); // ensure nothing happens until this window is closed
			this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

			this.addWindowListener(new WindowAdapter() {

				public void windowClosing(WindowEvent e) {
					analysisOptions = null;
					AnalysisSetupDialog.this.setVisible(false);
				}


			});

			analysisOptions = options;
			createAndShowGUI();
			pack();
			setVisible(true);
		} catch(Exception e){
			warn("Cannot make dialog");
			stack("Error making analysis setup", e);
		}
	}

	/**
	 * Get the current options 
	 * @return an AnalysisOptions
	 */
	public IMutableAnalysisOptions getOptions(){
		return this.analysisOptions;
	}

	public void createAndShowGUI(){
		finer("Creating UI");
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
		finer("Created UI");

	}

	/**
	 * Populate the analysis options with the defaults specified above
	 */
	public void setDefaultOptions(){

		finer("Setting default options");
		IMutableDetectionOptions nucleusOptions  = analysisOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS);
		
		if(nucleusOptions==null){
			warn("Options is null");
		}
		
		analysisOptions.setAngleWindowProportion(DEFAULT_PROFILE_WINDOW_SIZE);
		analysisOptions.setRefoldNucleus(false);
		analysisOptions.setNucleusType(NucleusType.RODENT_SPERM);
		
		nucleusOptions.setThreshold(DEFAULT_NUCLEUS_THRESHOLD);

		nucleusOptions.setMinSize(DEFAULT_MIN_NUCLEUS_SIZE);
		nucleusOptions.setMaxSize(DEFAULT_MAX_NUCLEUS_SIZE);

		nucleusOptions.setMinCirc(DEFAULT_MIN_NUCLEUS_CIRC);
		nucleusOptions.setMaxCirc(DEFAULT_MAX_NUCLEUS_CIRC);
		nucleusOptions.setChannel(DEFAULT_CHANNEL);
		nucleusOptions.setScale(DEFAULT_SCALE);
		
		ICannyOptions nucleusCannyOptions = new DefaultCannyOptions();
		
		nucleusCannyOptions.setUseCanny(true);
		nucleusCannyOptions.setCannyAutoThreshold(false);
		nucleusCannyOptions.setLowThreshold( (float) ICannyOptions.DEFAULT_CANNY_LOW_THRESHOLD);
		nucleusCannyOptions.setHighThreshold((float) ICannyOptions.DEFAULT_CANNY_HIGH_THRESHOLD);
		nucleusCannyOptions.setKernelRadius((float) ICannyOptions.DEFAULT_CANNY_KERNEL_RADIUS);
		nucleusCannyOptions.setKernelWidth(ICannyOptions.DEFAULT_CANNY_KERNEL_WIDTH);
		nucleusCannyOptions.setClosingObjectRadius(ICannyOptions.DEFAULT_CLOSING_OBJECT_RADIUS);
		
		nucleusCannyOptions.setUseKuwahara(ICannyOptions.DEFAULT_USE_KUWAHARA);
		nucleusCannyOptions.setKuwaharaKernel(ICannyOptions.DEFAULT_KUWAHARA_KERNEL_RADIUS);
		nucleusCannyOptions.setFlattenImage(ICannyOptions.DEFAULT_FLATTEN_CHROMOCENTRES);
		nucleusCannyOptions.setFlattenThreshold(ICannyOptions.DEFAULT_FLATTEN_THRESHOLD);
		nucleusCannyOptions.setAddBorder(ICannyOptions.DEFAULT_ADD_BORDER);
		nucleusOptions.setCannyOptions(nucleusCannyOptions);
		finer("Set default options");
	}
	
	/**
	 * Set this options object to use the values in the given options
	 * @param options
	 */
	private void setOptions(final IAnalysisOptions options ){
		
		IMutableDetectionOptions templateNucleusOptions  = options.getDetectionOptions(IAnalysisOptions.NUCLEUS);
		IMutableDetectionOptions nucleusOptions  = analysisOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS);
		
		nucleusOptions.setThreshold(templateNucleusOptions.getThreshold());

		nucleusOptions.setMinSize(templateNucleusOptions.getMinSize());
		nucleusOptions.setMaxSize(templateNucleusOptions.getMaxSize());

		nucleusOptions.setMinCirc(templateNucleusOptions.getMinCirc());
		nucleusOptions.setMaxCirc(templateNucleusOptions.getMaxCirc());

		nucleusOptions.setScale(templateNucleusOptions.getScale());	
		nucleusOptions.setChannel(templateNucleusOptions.getChannel());
		nucleusOptions.setCannyOptions(templateNucleusOptions.getCannyOptions());
		
		
		analysisOptions.setAngleWindowProportion(options.getProfileWindowProportion());
		analysisOptions.setRefoldNucleus(options.refoldNucleus());
		analysisOptions.setNucleusType(options.getNucleusType());
		
		// Update the gui
		fine("Updated options object");
		
		try{
			
			channelSelection.setSelectedItem(Constants.channelIntToName(nucleusOptions.getChannel()));
			
			nucleusThresholdSpinner.setValue(nucleusOptions.getThreshold());
		
			txtMinNuclearSize.setValue( (int) nucleusOptions.getMinSize());


			txtMaxNuclearSize.setValue( (int) nucleusOptions.getMaxSize());

			minNuclearCircSpinner.setValue(nucleusOptions.getMinCirc());

			maxNuclearCircSpinner.setValue(nucleusOptions.getMaxCirc());

			scaleSpinner.setValue(nucleusOptions.getScale());
			txtProfileWindowSize.setValue(options.getProfileWindowProportion());
			finest("Updated spinners");


			nucleusSelectionBox.setSelectedItem(options.getNucleusType());
			refoldCheckBox.setSelected(options.refoldNucleus());
			finest("Updated checkboxes");

			nucleusCannyPanel.update(nucleusOptions.getCannyOptions());
			finest("Updated canny panel");
			
			fine("Updated gui");
			
		} catch(Exception e){
			stack("Error updating gui", e);
		}

	}
	
	private boolean hasOpenDatasetTemplates(){
		return openDatasets.size()>0;
	}

	private JPanel makeNucleusTypePanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.CENTER));

		panel.add(new JLabel("Nucleus type"));

		nucleusSelectionBox = new JComboBox<NucleusType>(NucleusType.values());
		nucleusSelectionBox.setSelectedItem(NucleusType.RODENT_SPERM);
		nucleusSelectionBox.setActionCommand("Nucleus type");
		nucleusSelectionBox.addActionListener(this);
		panel.add(nucleusSelectionBox);
		return panel;
	}

	private JPanel makeLowerButtonPanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());
		
		// Button to copy existing dataset options
		JButton btnCopy = new JButton("Copy");
		btnCopy.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseClicked(MouseEvent arg0) {
				
				// display panel of open datasets
				
				IAnalysisDataset[] nameArray = openDatasets.toArray(new AnalysisDataset[0]);

				IAnalysisDataset sourceDataset = (IAnalysisDataset) JOptionPane.showInputDialog(null, 
						"Choose source dataset",
						"Source dataset",
						JOptionPane.QUESTION_MESSAGE, 
						null, 
						nameArray, 
						nameArray[0]);

				
				if(sourceDataset!=null){

					fine("Copying options from dataset: "+sourceDataset.getName());
					setOptions(sourceDataset.getAnalysisOptions());
					
				}	else {
					fine("No dataset selected");
				}
				
			}
			
		});
		
		// Only enable if there are open datasets
		if( ! hasOpenDatasetTemplates()){
			btnCopy.setEnabled(false);
		}
		btnCopy.setToolTipText("Copy from open dataset");
		panel.add(btnCopy);

		JButton btnOk = new JButton("OK");
		btnOk.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				if(getImageDirectory()){
					
					// probe the first image
					// show the results of the current settings
					ImageProber p = new NucleusDetectionImageProber(analysisOptions,  
							analysisOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS).getFolder());
					if(p.getOK()==false){
					
						// Do nothing, revise options
						
					} else {
						
						// ok, close the window
						AnalysisSetupDialog.this.setVisible(false);
					}
					
				} else {
					// Cancelled
					
					// Do nothing, revise options
//					analysisOptions = null;
//					AnalysisSetupDialog.this.setVisible(false);
				}
				
			}
		});

		panel.add(btnOk);

		JButton btnCancel = new JButton("Cancel");
		btnCancel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				analysisOptions = null;
				AnalysisSetupDialog.this.setVisible(false);
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
		
		nucleusCannyPanel = new CannyPanel(analysisOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS).getCannyOptions());
		
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
		
		List<JLabel> labels = new ArrayList<JLabel>();
		labels.add(new JLabel("Image channel"));
		labels.add(new JLabel("Min nucleus size"));
		labels.add(new JLabel("Max nucleus size"));
		labels.add(new JLabel("Min nucleus circ"));
		labels.add(new JLabel("Max nucleus circ"));
		labels.add(new JLabel("Profile window"));
		labels.add(new JLabel("Scale (pixels/micron)"));
		labels.add(new JLabel("Keep filtered nuclei"));
		
		List<Component> fields = new ArrayList<Component>();
		fields.add(channelSelection);
		fields.add(txtMinNuclearSize);
		fields.add(txtMaxNuclearSize);
		fields.add(minNuclearCircSpinner);
		fields.add(maxNuclearCircSpinner);
		fields.add(txtProfileWindowSize);
		fields.add(scaleSpinner);
		fields.add(keepFailedheckBox);
		
		channelSelection.setSelectedItem(DEFAULT_CHANNEL_NAME);
		
		channelSelection.addActionListener(this);
		txtMinNuclearSize.addChangeListener(this);
		txtMaxNuclearSize.addChangeListener(this);
		minNuclearCircSpinner.addChangeListener(this);
		maxNuclearCircSpinner.addChangeListener(this);
		txtProfileWindowSize.addChangeListener(this);
		scaleSpinner.addChangeListener(this);
		keepFailedheckBox.addActionListener(this);
		keepFailedheckBox.setSelected(false);

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
//		refoldFastButton.setSelected(true);
		
//		refoldFastButton.setActionCommand("Fast");
//		refoldIntensiveButton.setActionCommand("Intensive");
//		refoldBrutalButton.setActionCommand("Brutal");
//
//		//Group the radio buttons.
//		refoldModeGroup = new ButtonGroup();
//		refoldModeGroup.add(refoldFastButton);
//		refoldModeGroup.add(refoldIntensiveButton);
//		refoldModeGroup.add(refoldBrutalButton);
//		
//		panel.add(refoldFastButton);
//		panel.add(refoldIntensiveButton);
//		panel.add(refoldBrutalButton);
//		
//		refoldFastButton.setEnabled(false);
//		refoldIntensiveButton.setEnabled(false);
//		refoldBrutalButton.setEnabled(false);

		return panel;
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		
		IMutableDetectionOptions nucleusOptions  = analysisOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS);
		
		if(e.getSource()==channelSelection){
			nucleusOptions.setChannel(channelSelection.getSelectedItem().equals("Red") 
					? Constants.RGB_RED
							: channelSelection.getSelectedItem().equals("Green") 
							? Constants.RGB_GREEN
									: Constants.RGB_BLUE);

		}

		if(e.getActionCommand().equals("NucleusDetectionThreshold")){
			nucleusOptions.getCannyOptions().setUseCanny(false);
			
			CardLayout cl = (CardLayout)(cardPanel.getLayout());
		    cl.show(cardPanel, "ThresholdPanel");

		}
		
		if(e.getActionCommand().equals("NucleusDetectionEdge")){
			nucleusOptions.getCannyOptions().setUseCanny(true);
			CardLayout cl = (CardLayout)(cardPanel.getLayout());
		    cl.show(cardPanel, "CannyPanel");
		}
		
		if(e.getSource()==keepFailedheckBox){
			analysisOptions.setKeepFailedCollections(keepFailedheckBox.isSelected());
		}
				
		if(e.getActionCommand().equals("Nucleus type")){
			NucleusType type = (NucleusType) nucleusSelectionBox.getSelectedItem();
			this.analysisOptions.setNucleusType(type);
			
			
			if(type.equals(NucleusType.ROUND)){
				nucleusOptions.setMinCirc(  0.0 );
				nucleusOptions.setMaxCirc(  1.0 );
				minNuclearCircSpinner.setValue(0.0);
				maxNuclearCircSpinner.setValue(1.0);
				
			}
			if(type.equals(NucleusType.RODENT_SPERM)){
				nucleusOptions.setMinCirc(  0.2 );
				nucleusOptions.setMaxCirc(  0.8 );
				minNuclearCircSpinner.setValue(0.2);
				maxNuclearCircSpinner.setValue(0.8);
			}
			if(type.equals(NucleusType.PIG_SPERM)){
				nucleusOptions.setMinCirc(  0.2 );
				nucleusOptions.setMaxCirc(  0.8 );
				minNuclearCircSpinner.setValue(0.2);
				maxNuclearCircSpinner.setValue(0.8);
			}
		}


		if(e.getActionCommand().equals("Refold nucleus")){
			if(refoldCheckBox.isSelected()){

				this.analysisOptions.setRefoldNucleus(true);
				
			} else {

				this.analysisOptions.setRefoldNucleus(false);
			}

		}
		
	}

	@Override
	public void stateChanged(ChangeEvent e) {

		IMutableDetectionOptions nucleusOptions  = analysisOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS);
		
		
		try{

			if(e.getSource()==nucleusThresholdSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();	
				nucleusOptions.setThreshold(  (Integer) j.getValue());
			}

			if(e.getSource()==txtMinNuclearSize){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				
				// ensure never larger than max
				if( (Integer) j.getValue() > (Integer) txtMaxNuclearSize.getValue() ){
					j.setValue( txtMaxNuclearSize.getValue() );
				}
				
				nucleusOptions.setMinSize(  (Integer) j.getValue());
			}
			
			if(e.getSource()==txtMaxNuclearSize){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				
				// ensure never smaller than min
				if( (Integer) j.getValue() < (Integer) txtMinNuclearSize.getValue() ){
					j.setValue( txtMinNuclearSize.getValue() );
				}
				
				nucleusOptions.setMaxSize(  (Integer) j.getValue());
			}
			
			if(e.getSource()==minNuclearCircSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				
				// ensure never larger than max
				if( (Double) j.getValue() > (Double) maxNuclearCircSpinner.getValue() ){
					j.setValue( maxNuclearCircSpinner.getValue() );
				}
				
				nucleusOptions.setMinCirc(  (Double) j.getValue());
			}
			
			if(e.getSource()==maxNuclearCircSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				
				// ensure never smaller than min
				if( (Double) j.getValue() < (Double) minNuclearCircSpinner.getValue() ){
					j.setValue( minNuclearCircSpinner.getValue() );
				}
				
				nucleusOptions.setMaxCirc(  (Double) j.getValue());
			}
						
			if(e.getSource()==txtProfileWindowSize){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				this.analysisOptions.setAngleWindowProportion(  (Double) j.getValue());
			}	
			
			if(e.getSource()==scaleSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				nucleusOptions.setScale(  (Double) j.getValue());
			}	
			
		} catch (ParseException e1) {
			stack("Parsing error in JSpinner", e1);
		}


	}

	
	private boolean getImageDirectory(){
				
		File defaultDir = analysisOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS).getFolder();
		
		JFileChooser fc = new JFileChooser(defaultDir); // if null, will be home dir

		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);


		int returnVal = fc.showOpenDialog(fc);
		if (returnVal != 0)	{
			return false; // user cancelled
		}
		
		File file = fc.getSelectedFile();

		if( ! file.isDirectory()){
			return false;
		}
		fine("Selected directory: "+file.getAbsolutePath());

		analysisOptions.getDetectionOptions(IAnalysisOptions.NUCLEUS).setFolder( file);

//		if(analysisOptions.isReanalysis()){
//			OpenDialog fileDialog = new OpenDialog("Select a mapping file...");
//			String fileName = fileDialog.getPath();
//			if(fileName==null) return false;
//			analysisOptions.setMappingFile(new File(fileName));
//		}
		return true;
	}
}

