package no.gui;

import ij.IJ;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.awt.FlowLayout;

import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JRadioButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.SpinnerNumberModel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import no.analysis.AnalysisCreator;
import no.collections.PigSpermNucleusCollection;
import no.collections.RodentSpermNucleusCollection;
import no.collections.RoundNucleusCollection;
import no.components.AnalysisOptions;
import no.nuclei.RoundNucleus;
import no.nuclei.sperm.PigSpermNucleus;
import no.nuclei.sperm.RodentSpermNucleus;

public class AnalysisSetupWindow extends JDialog implements ActionListener, ChangeListener {

	private AnalysisOptions analysisOptions = new AnalysisOptions();

	private static final String RODENT_SPERM_NUCLEUS = "Rodent sperm";
	private static final String PIG_SPERM_NUCLEUS = "Pig sperm";
	private static final String ROUND_NUCLEUS = "Round nucleus";

	private static Map<String, Class<?>>  collectionClassTypes;
	private static Map<String, Class<?>>  nucleusClassTypes;

	static
	{
		collectionClassTypes = new HashMap<String, Class<?>>();
		collectionClassTypes.put(RODENT_SPERM_NUCLEUS, RodentSpermNucleusCollection.class);
		collectionClassTypes.put(PIG_SPERM_NUCLEUS, PigSpermNucleusCollection.class);
		collectionClassTypes.put(ROUND_NUCLEUS, RoundNucleusCollection.class);

		nucleusClassTypes = new HashMap<String, Class<?>>();
		nucleusClassTypes.put(RODENT_SPERM_NUCLEUS, RodentSpermNucleus.class);
		nucleusClassTypes.put(PIG_SPERM_NUCLEUS, PigSpermNucleus.class);
		nucleusClassTypes.put(ROUND_NUCLEUS, RoundNucleus.class);
	}

	private static final long serialVersionUID = 1L;
	private JPanel contentPane;

	private JSpinner txtMinNuclearSize = new JSpinner(new SpinnerNumberModel(500,	100, 50000, 1));
	private JSpinner txtMaxNuclearSize = new JSpinner(new SpinnerNumberModel(10000,	100, 50000, 1));
	//	private JTextField txtXOffset;
	//	private JTextField txtYOffset;

	private JSpinner txtProfileWindowSize = new JSpinner(new SpinnerNumberModel(15,	5, 50, 1));

	private JSpinner nucleusThresholdSpinner = new JSpinner(new SpinnerNumberModel(36,	0, 255, 1));
	private JSpinner signalThresholdSpinner = new JSpinner(new SpinnerNumberModel(70,	0, 255, 1));

	private JSpinner minNuclearCircSpinner = new JSpinner(new SpinnerNumberModel(0,	0, 1, 0.05));
	private JSpinner maxNuclearCircSpinner = new JSpinner(new SpinnerNumberModel(1,	0, 1, 0.05));

	private JSpinner minSignalSizeSpinner = new JSpinner(new SpinnerNumberModel(5,	1, 10000, 1));
	private JSpinner maxSignalFractSpinner = new JSpinner(new SpinnerNumberModel(0.5, 0, 1, 0.05));

	//	private String[] nucleusTypes = { "Round nucleus", "Rodent sperm", "Pig sperm"};
	private JComboBox nucleusSelectionBox;

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


		// add the other settings
		JPanel settingsPanel = new JPanel();
		settingsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));

		settingsPanel.add(makeSettingsPanel());
		contentPane.add(settingsPanel, BorderLayout.CENTER);

	}

	public void setDefaultOptions(){
		analysisOptions.setNucleusThreshold(36);
		analysisOptions.setSignalThreshold(70);

		analysisOptions.setMinNucleusSize(500);
		analysisOptions.setMaxNucleusSize(10000);

		analysisOptions.setMinNucleusCirc(0.0);
		analysisOptions.setMaxNucleusCirc(1.0);

		analysisOptions.setMinSignalSize(5);
		analysisOptions.setMaxSignalFraction(0.5);

		analysisOptions.setAngleProfileWindowSize(15);

		analysisOptions.setPerformReanalysis(false);
		analysisOptions.setRealignMode(true);

		analysisOptions.setRefoldNucleus(true);
		analysisOptions.setRefoldMode("Fast");

		analysisOptions.setXoffset(0);
		analysisOptions.setYoffset(0);
	}

	private JPanel makeNucleusTypePanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout(FlowLayout.CENTER));

		//		JLabel lblNucleusType = new JLabel("Nucleus type");
		panel.add(new JLabel("Nucleus type"));

		nucleusSelectionBox = new JComboBox(nucleusClassTypes.keySet().toArray(new String[0]));
		nucleusSelectionBox.setSelectedIndex(0);
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
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		JLabel lblDetectionSettings = new JLabel("Detection settings", JLabel.CENTER);
		panel.add(lblDetectionSettings);
		panel.add(makeDetectionSettingsPanel());

		Dimension minSize = new Dimension(10, 20);
		Dimension prefSize = new Dimension(20, 20);
		Dimension maxSize = new Dimension(Short.MAX_VALUE, 20);
		panel.add(new Box.Filler(minSize, prefSize, maxSize));

		JLabel lblRefoldSettings = new JLabel("Refolding settings", JLabel.CENTER);
		panel.add(lblRefoldSettings);
		panel.add(makeRefoldSettingsPanel());

		return panel;
	}

	private JPanel makeDetectionSettingsPanel(){

		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());

		JLabel[] labels = new JLabel[9];
		JSpinner[] fields = new JSpinner[9];

		labels[0] = new JLabel("Nucleus threshold");
		labels[1] = new JLabel("Signal threshold");
		labels[2] = new JLabel("Min nucleus size");
		labels[3] = new JLabel("Max nucleus size");
		labels[4] = new JLabel("Min nucleus circ");
		labels[5] = new JLabel("Max nucleus circ");
		labels[6] = new JLabel("Min signal size");
		labels[7] = new JLabel("Max signal fraction");
		labels[8] = new JLabel("Profile window");


		fields[0] = nucleusThresholdSpinner;
		fields[1] = signalThresholdSpinner;
		fields[2] = txtMinNuclearSize;
		fields[3] = txtMaxNuclearSize;
		fields[4] = minNuclearCircSpinner;
		fields[5] = maxNuclearCircSpinner;
		fields[6] = minSignalSizeSpinner;
		fields[7] = maxSignalFractSpinner;
		fields[8] = txtProfileWindowSize;
		
		nucleusThresholdSpinner.addChangeListener(this);
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

			Dimension minSize = new Dimension(5, 5);
			Dimension prefSize = new Dimension(5, 5);
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

		if(e.getActionCommand().equals("Nucleus type")){
			String type = (String) nucleusSelectionBox.getSelectedItem();
			this.analysisOptions.setNucleusClass(nucleusClassTypes.get(type));
			this.analysisOptions.setCollectionClass(collectionClassTypes.get(type));
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
				if( (Integer) j.getValue() > (Integer) txtMaxNuclearSize.getValue() ){
					j.setValue( txtMaxNuclearSize.getValue() );
				}
				
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
			
			
		} catch (ParseException e1) {
			IJ.log("Parsing error in JSpinner");
		}


	}

}

