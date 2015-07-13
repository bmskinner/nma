package no.gui;

import ij.IJ;
import ij.io.DirectoryChooser;
import ij.io.OpenDialog;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JDialog;
import javax.swing.JSpinner;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.SpinnerNumberModel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.BorderLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.io.File;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import no.components.AnalysisOptions;
import no.components.AnalysisOptions.CannyOptions;
import no.nuclei.RoundNucleus;
import no.nuclei.sperm.PigSpermNucleus;
import no.nuclei.sperm.RodentSpermNucleus;

public class AnalysisSetupWindow extends JDialog implements ActionListener, ChangeListener {

	private AnalysisOptions analysisOptions = new AnalysisOptions();

	// types of nucleus
	private static final String RODENT_SPERM_NUCLEUS = "Rodent sperm";
	private static final String PIG_SPERM_NUCLEUS = "Pig sperm";
	private static final String ROUND_NUCLEUS = "Round nucleus";
	
	// types of signal
	
	
	private static final double DEFAULT_CANNY_LOW_THRESHOLD = 0.1;
	private static final double DEFAULT_CANNY_HIGH_THRESHOLD = 1.5;
	
	private static final double DEFAULT_CANNY_TAIL_LOW_THRESHOLD = 0.1;
	private static final double DEFAULT_CANNY_TAIL_HIGH_THRESHOLD = 0.5;
	
	private static final double DEFAULT_CANNY_KERNEL_RADIUS = 2;
	private static final int    DEFAULT_CANNY_KERNEL_WIDTH = 16;
	private static final int    DEFAULT_CLOSING_OBJECT_RADIUS = 5;
	private static final int    DEFAULT_TAIL_CLOSING_OBJECT_RADIUS = 3;
	
	private static final int    DEFAULT_MIN_NUCLEUS_SIZE = 2000;
	private static final int    DEFAULT_MAX_NUCLEUS_SIZE = 10000;
	private static final double DEFAULT_MIN_NUCLEUS_CIRC = 0.2;
	private static final double DEFAULT_MAX_NUCLEUS_CIRC = 0.8;
	private static final int    DEFAULT_NUCLEUS_THRESHOLD = 36;
	
	private static final int    DEFAULT_SIGNAL_THRESHOLD = 70;
	private static final int    DEFAULT_MIN_SIGNAL_SIZE = 5;
	private static final double DEFAULT_MAX_SIGNAL_FRACTION = 0.5;
	
	private static final int    DEFAULT_PROFILE_WINDOW_SIZE = 15;
	
	private static final String DEFAULT_REFOLD_MODE = "Fast";
	

//	private static Map<String, Class<?>>  collectionClassTypes;
	private static Map<String, Class<?>>  nucleusClassTypes;

	static
	{
//		collectionClassTypes = new HashMap<String, Class<?>>();
//		collectionClassTypes.put(RODENT_SPERM_NUCLEUS, RodentSpermNucleusCollection.class);
//		collectionClassTypes.put(PIG_SPERM_NUCLEUS, PigSpermNucleusCollection.class);
//		collectionClassTypes.put(ROUND_NUCLEUS, RoundNucleusCollection.class);

		nucleusClassTypes = new HashMap<String, Class<?>>();
		nucleusClassTypes.put(RODENT_SPERM_NUCLEUS, RodentSpermNucleus.class);
		nucleusClassTypes.put(PIG_SPERM_NUCLEUS, PigSpermNucleus.class);
		nucleusClassTypes.put(ROUND_NUCLEUS, RoundNucleus.class);
	}

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;
	
	
	// nucleus detection method
	private JRadioButton nucleusThresholdButton = new JRadioButton("Threshold");
	private JRadioButton nucleusEdgeButton = new JRadioButton("Edge detection");
	private ButtonGroup nucleusDetectionMethodGroup;

	private JSpinner cannyLowThreshold = new JSpinner(new SpinnerNumberModel(DEFAULT_CANNY_LOW_THRESHOLD,	0, 10, 0.05));
	private JSpinner cannyHighThreshold = new JSpinner(new SpinnerNumberModel(DEFAULT_CANNY_HIGH_THRESHOLD,	0, 20, 0.05));
	private JSpinner cannyKernelRadius = new JSpinner(new SpinnerNumberModel(DEFAULT_CANNY_KERNEL_RADIUS,	0, 20, 0.05));
	private JSpinner cannyKernelWidth = new JSpinner(new SpinnerNumberModel(DEFAULT_CANNY_KERNEL_WIDTH,	1, 50, 1));
	private JSpinner closingObjectRadiusSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_CLOSING_OBJECT_RADIUS, 1,100 , 1));
	private JCheckBox cannyAutoThresholdCheckBox;
	
	// other detection parameters

	private JSpinner txtMinNuclearSize = new JSpinner(new SpinnerNumberModel(DEFAULT_MIN_NUCLEUS_SIZE,	100, 50000, 1));
	private JSpinner txtMaxNuclearSize = new JSpinner(new SpinnerNumberModel(DEFAULT_MAX_NUCLEUS_SIZE,	100, 50000, 1));

	private JSpinner txtProfileWindowSize = new JSpinner(new SpinnerNumberModel(DEFAULT_PROFILE_WINDOW_SIZE,	5, 50, 1));

	private JSpinner nucleusThresholdSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_NUCLEUS_THRESHOLD,	0, 255, 1));
	private JSpinner signalThresholdSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_SIGNAL_THRESHOLD,	0, 255, 1));

	private JSpinner minNuclearCircSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_MIN_NUCLEUS_CIRC,	0, 1, 0.05));
	private JSpinner maxNuclearCircSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_MAX_NUCLEUS_CIRC,	0, 1, 0.05));

	private JSpinner minSignalSizeSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_MIN_SIGNAL_SIZE,	1, 10000, 1));
	private JSpinner maxSignalFractSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_MAX_SIGNAL_FRACTION, 0, 1, 0.05));


	private JComboBox<String> nucleusSelectionBox;

	private JCheckBox refoldCheckBox;
	private JRadioButton refoldFastButton = new JRadioButton("Fast");
	private JRadioButton refoldIntensiveButton = new JRadioButton("Intensive");
	private JRadioButton refoldBrutalButton = new JRadioButton("Brutal");
	private ButtonGroup refoldModeGroup;

	/**
	 * Create the frame.
	 */
	public AnalysisSetupWindow() {
		setModal(true);
		setDefaultOptions();
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
		setBounds(100, 100, 450, 626);
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
		analysisOptions.setSignalThreshold(DEFAULT_SIGNAL_THRESHOLD);

		analysisOptions.setMinNucleusSize(DEFAULT_MIN_NUCLEUS_SIZE);
		analysisOptions.setMaxNucleusSize(DEFAULT_MAX_NUCLEUS_SIZE);

		analysisOptions.setMinNucleusCirc(DEFAULT_MIN_NUCLEUS_CIRC);
		analysisOptions.setMaxNucleusCirc(DEFAULT_MAX_NUCLEUS_CIRC);

		analysisOptions.setMinSignalSize(DEFAULT_MIN_SIGNAL_SIZE);
		analysisOptions.setMaxSignalFraction(DEFAULT_MAX_SIGNAL_FRACTION);

		analysisOptions.setAngleProfileWindowSize(DEFAULT_PROFILE_WINDOW_SIZE);

		analysisOptions.setPerformReanalysis(false);
		analysisOptions.setRealignMode(true);

		analysisOptions.setRefoldNucleus(true);
		analysisOptions.setRefoldMode(DEFAULT_REFOLD_MODE);

		analysisOptions.setXoffset(0);
		analysisOptions.setYoffset(0);
				
//		analysisOptions.setCollectionClass(RodentSpermNucleusCollection.class);
		analysisOptions.setNucleusClass(RodentSpermNucleus.class);
		
		CannyOptions nucleusCannyOptions = analysisOptions.getNucleusCannyOptions();
		
		nucleusCannyOptions.setUseCanny(false);
		nucleusCannyOptions.setCannyAutoThreshold(false);
		nucleusCannyOptions.setLowThreshold( (float) DEFAULT_CANNY_LOW_THRESHOLD);
		nucleusCannyOptions.setHighThreshold((float) DEFAULT_CANNY_HIGH_THRESHOLD);
		nucleusCannyOptions.setKernelRadius((float)DEFAULT_CANNY_KERNEL_RADIUS);
		nucleusCannyOptions.setKernelWidth(DEFAULT_CANNY_KERNEL_WIDTH);
		nucleusCannyOptions.setClosingObjectRadius(DEFAULT_CLOSING_OBJECT_RADIUS);
		
		CannyOptions tailCannyOptions = analysisOptions.getTailCannyOptions();
		
		tailCannyOptions.setUseCanny(true);
		tailCannyOptions.setCannyAutoThreshold(false);
		tailCannyOptions.setLowThreshold( (float) DEFAULT_CANNY_TAIL_LOW_THRESHOLD);
		tailCannyOptions.setHighThreshold((float) DEFAULT_CANNY_TAIL_HIGH_THRESHOLD);
		tailCannyOptions.setKernelRadius((float)DEFAULT_CANNY_KERNEL_RADIUS);
		tailCannyOptions.setKernelWidth(DEFAULT_CANNY_KERNEL_WIDTH);
		tailCannyOptions.setClosingObjectRadius(DEFAULT_TAIL_CLOSING_OBJECT_RADIUS);
		
	}

	private JPanel makeNucleusTypePanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.CENTER));

		//		JLabel lblNucleusType = new JLabel("Nucleus type");
		panel.add(new JLabel("Nucleus type"));

		nucleusSelectionBox = new JComboBox<String>(nucleusClassTypes.keySet().toArray(new String[0]));
		nucleusSelectionBox.setSelectedIndex(1);
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
				getImageDirectory();
				AnalysisSetupWindow.this.setVisible(false);
			}
		});

		panel.add(btnOk);

		JButton btnCancel = new JButton("Cancel");
		btnCancel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				AnalysisSetupWindow.this.dispose();
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
	
	private JPanel makeNucleusDetectionSettingsPanel(){
		
		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createTitledBorder("Nucleus detection"));
		panel.setLayout(new GridBagLayout());
		
		nucleusThresholdButton.setSelected(true);
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
		
		GridBagConstraints c = new GridBagConstraints();
		Dimension minSize = new Dimension(10, 5);
		Dimension prefSize = new Dimension(10, 5);
		Dimension maxSize = new Dimension(Short.MAX_VALUE, 5);
		c.gridwidth = GridBagConstraints.REMAINDER; //next-to-last
		c.fill = GridBagConstraints.NONE;      //reset to default
		c.weightx = 0.0;                       //reset to default
		panel.add(new Box.Filler(minSize, prefSize, maxSize),c);
		
		cannyAutoThresholdCheckBox = new JCheckBox("Canny auto threshold");
		cannyAutoThresholdCheckBox.setSelected(false);
		cannyAutoThresholdCheckBox.setActionCommand("CannyAutoThreshold");
		cannyAutoThresholdCheckBox.addActionListener(this);
		panel.add(cannyAutoThresholdCheckBox);
		panel.add(new Box.Filler(minSize, prefSize, maxSize),c);
		
		
		// add the canny settings
		JLabel[] labels = new JLabel[6];
		JSpinner[] fields = new JSpinner[6];
		
		labels[0] = new JLabel("Nucleus threshold");
		labels[1] = new JLabel("Canny low threshold");
		labels[2] = new JLabel("Canny high threshold");
		labels[3] = new JLabel("Canny kernel radius");
		labels[4] = new JLabel("Canny kernel width");
		labels[5] = new JLabel("Closing radius");

		fields[0] = nucleusThresholdSpinner;
		fields[1] = cannyLowThreshold;
		fields[2] = cannyHighThreshold;
		fields[3] = cannyKernelRadius;
		fields[4] = cannyKernelWidth;
		fields[5] = closingObjectRadiusSpinner;
		
		// add the change listeners
		nucleusThresholdSpinner.addChangeListener(this);
		cannyLowThreshold.addChangeListener(this);
		cannyHighThreshold.addChangeListener(this);
		cannyKernelRadius.addChangeListener(this);
		cannyKernelWidth.addChangeListener(this);
		closingObjectRadiusSpinner.addChangeListener(this);
		
		// stating default is threshold on
		cannyAutoThresholdCheckBox.setEnabled(false);
		cannyLowThreshold.setEnabled(false);
		cannyHighThreshold.setEnabled(false);
		cannyKernelRadius.setEnabled(false);
		cannyKernelWidth.setEnabled(false);
		closingObjectRadiusSpinner.setEnabled(false);
		
		addLabelTextRows(labels, fields, new GridBagLayout(), panel );
		
		return panel;
	}
	

	private JPanel makeDetectionSettingsPanel(){

		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createTitledBorder("Detection settings"));
		panel.setLayout(new GridBagLayout());

		JLabel[] labels = new JLabel[8];
		JSpinner[] fields = new JSpinner[8];

//		labels[0] = new JLabel("Nucleus threshold");
		labels[0] = new JLabel("Signal threshold");
		labels[1] = new JLabel("Min nucleus size");
		labels[2] = new JLabel("Max nucleus size");
		labels[3] = new JLabel("Min nucleus circ");
		labels[4] = new JLabel("Max nucleus circ");
		labels[5] = new JLabel("Min signal size");
		labels[6] = new JLabel("Max signal fraction");
		labels[7] = new JLabel("Profile window");


//		fields[0] = nucleusThresholdSpinner;
		fields[0] = signalThresholdSpinner;
		fields[1] = txtMinNuclearSize;
		fields[2] = txtMaxNuclearSize;
		fields[3] = minNuclearCircSpinner;
		fields[4] = maxNuclearCircSpinner;
		fields[5] = minSignalSizeSpinner;
		fields[6] = maxSignalFractSpinner;
		fields[7] = txtProfileWindowSize;
		
//		nucleusThresholdSpinner.addChangeListener(this);
		signalThresholdSpinner.addChangeListener(this);
		txtMinNuclearSize.addChangeListener(this);
		txtMaxNuclearSize.addChangeListener(this);
		minNuclearCircSpinner.addChangeListener(this);
		maxNuclearCircSpinner.addChangeListener(this);
		minSignalSizeSpinner.addChangeListener(this);
		maxSignalFractSpinner.addChangeListener(this);
		txtProfileWindowSize.addChangeListener(this);

		addLabelTextRows(labels, fields, new GridBagLayout(), panel );

		return panel;
	}

	private JPanel makeRefoldSettingsPanel(){

		JPanel panel = new JPanel();
		panel.setBorder(BorderFactory.createTitledBorder("Refold settings"));

		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		refoldCheckBox = new JCheckBox("Refold nucleus");
		refoldCheckBox.setSelected(true);
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

		return panel;
	}

	/**
	 * Currently not enabled. Can hold settings for automated remapping of pre- and 
	 * post-FISHed nuclei
	 * @return
	 */
	private JPanel makeRenappingSettingsPanel(){

		JPanel panel = new JPanel();
		//	panel.setLayout(new GridBagLayout());


		//----------------------
		// make remapping panel
		//----------------------

		//	panel.add(new Box.Filler(minSize, prefSize, maxSize));
		//	
		//	JLabel lblReanalysisSettings = new JLabel("Refolding settings");
		//	panel.add(lblRefoldSettings);
		//	JPanel refoldPanel = new JPanel();


		return panel;
	}	

	private void addLabelTextRows(JLabel[] labels,
			JSpinner[] textFields,
			GridBagLayout gridbag,
			Container container) {
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.EAST;
		int numLabels = labels.length;

		for (int i = 0; i < numLabels; i++) {
			c.gridwidth = 1; //next-to-last
			c.fill = GridBagConstraints.NONE;      //reset to default
			c.weightx = 0.0;                       //reset to default
			container.add(labels[i], c);

			Dimension minSize = new Dimension(10, 5);
			Dimension prefSize = new Dimension(10, 5);
			Dimension maxSize = new Dimension(Short.MAX_VALUE, 5);
			c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
			c.fill = GridBagConstraints.NONE;      //reset to default
			c.weightx = 0.0;                       //reset to default
			container.add(new Box.Filler(minSize, prefSize, maxSize),c);

			c.gridwidth = GridBagConstraints.REMAINDER;     //end row
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1.0;
			container.add(textFields[i], c);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {

		if(e.getActionCommand().equals("NucleusDetectionThreshold")){
			this.analysisOptions.getNucleusCannyOptions().setUseCanny(false);
			cannyAutoThresholdCheckBox.setEnabled(false);
			cannyLowThreshold.setEnabled(false);
			cannyHighThreshold.setEnabled(false);
			cannyKernelRadius.setEnabled(false);
			cannyKernelWidth.setEnabled(false);
			closingObjectRadiusSpinner.setEnabled(false);
			nucleusThresholdSpinner.setEnabled(true);
			
		}
		
		if(e.getActionCommand().equals("NucleusDetectionEdge")){
			this.analysisOptions.getNucleusCannyOptions().setUseCanny(true);
			cannyAutoThresholdCheckBox.setEnabled(true);
			cannyLowThreshold.setEnabled(true);
			cannyHighThreshold.setEnabled(true);
			cannyKernelRadius.setEnabled(true);
			cannyKernelWidth.setEnabled(true);
			closingObjectRadiusSpinner.setEnabled(true);
			nucleusThresholdSpinner.setEnabled(false);
			
		}
		
		if(e.getActionCommand().equals("CannyAutoThreshold")){

			if(cannyAutoThresholdCheckBox.isSelected()){
				analysisOptions.getNucleusCannyOptions().setCannyAutoThreshold(true);
				cannyLowThreshold.setEnabled(false);
				cannyHighThreshold.setEnabled(false);
			} else {
				analysisOptions.getNucleusCannyOptions().setCannyAutoThreshold(false);
				cannyLowThreshold.setEnabled(true);
				cannyHighThreshold.setEnabled(true);
			}
		}
		
		if(e.getActionCommand().equals("Nucleus type")){
			String type = (String) nucleusSelectionBox.getSelectedItem();
			this.analysisOptions.setNucleusClass(nucleusClassTypes.get(type));
//			this.analysisOptions.setCollectionClass(collectionClassTypes.get(type));
			
			if(type.equals(ROUND_NUCLEUS)){
				this.analysisOptions.setMinNucleusCirc(  0.0 );
				this.analysisOptions.setMaxNucleusCirc(  1.0 );
				minNuclearCircSpinner.setValue(0.0);
				maxNuclearCircSpinner.setValue(1.0);
				
			}
			if(type.equals(RODENT_SPERM_NUCLEUS)){
				this.analysisOptions.setMinNucleusCirc(  0.2 );
				this.analysisOptions.setMaxNucleusCirc(  0.8 );
				minNuclearCircSpinner.setValue(0.2);
				maxNuclearCircSpinner.setValue(0.8);
			}
			if(type.equals(PIG_SPERM_NUCLEUS)){
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

			if(e.getSource()==signalThresholdSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				this.analysisOptions.setSignalThreshold(  (Integer) j.getValue());
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
			
			if(e.getSource()==minSignalSizeSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				
				// ensure never larger than the largest nucleus
//				if( (Integer) j.getValue() > (Integer) txtMaxNuclearSize.getValue() ){
//					j.setValue( txtMaxNuclearSize.getValue() );
//				}
				
				this.analysisOptions.setMinSignalSize(  (Integer) j.getValue());
			}
			
			if(e.getSource()==maxSignalFractSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				this.analysisOptions.setMaxSignalFraction(  (Double) j.getValue());
			}
			
			if(e.getSource()==txtProfileWindowSize){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				this.analysisOptions.setAngleProfileWindowSize(  (Integer) j.getValue());
			}
			
			if(e.getSource()==cannyLowThreshold){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				
				if( (Double) j.getValue() > (Double) cannyHighThreshold.getValue() ){
					j.setValue( cannyHighThreshold.getValue() );
				}
				Double doubleValue = (Double) j.getValue();
				this.analysisOptions.getNucleusCannyOptions().setLowThreshold(    doubleValue.floatValue() );
			}
			
			if(e.getSource()==cannyHighThreshold){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				
				if( (Double) j.getValue() < (Double) cannyLowThreshold.getValue() ){
					j.setValue( cannyLowThreshold.getValue() );
				}
				Double doubleValue = (Double) j.getValue();
				this.analysisOptions.getNucleusCannyOptions().setHighThreshold( doubleValue.floatValue() );
			}
			
			if(e.getSource()==cannyKernelRadius){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				Double doubleValue = (Double) j.getValue();
				this.analysisOptions.getNucleusCannyOptions().setKernelRadius( doubleValue.floatValue());
			}
			
			if(e.getSource()==cannyKernelWidth){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				this.analysisOptions.getNucleusCannyOptions().setKernelWidth( (Integer) j.getValue());
			}
			
			if(e.getSource()==closingObjectRadiusSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				this.analysisOptions.getNucleusCannyOptions().setClosingObjectRadius( (Integer) j.getValue());
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

