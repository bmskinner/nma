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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import utility.Constants;
import no.components.AnalysisOptions;
import no.components.AnalysisOptions.NuclearSignalOptions;

public class SignalDetectionSettingsWindow extends SettingsDialog implements ChangeListener {

	private JPanel contentPanel;
	private static final long serialVersionUID = 1L;
	private static final int    DEFAULT_SIGNAL_THRESHOLD = 70;
	private static final int    DEFAULT_MIN_SIGNAL_SIZE = 5;
	private static final double DEFAULT_MAX_SIGNAL_FRACTION = 0.1;
	private static final double DEFAULT_MIN_CIRC = 0.0;
	private static final double DEFAULT_MAX_CIRC = 1.0;
			
//	private NuclearSignalOptions options;
	private AnalysisOptions options;
	
	private JComboBox<String> channelSelection;
	private JTextField groupName;
	private String finalGroupName = null;
	private int channel;
		
	private JSpinner minSizeSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_MIN_SIGNAL_SIZE,	1, 10000, 1));
	private JSpinner maxFractSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_MAX_SIGNAL_FRACTION, 0, 1, 0.05));
	
	private JSpinner thresholdSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_SIGNAL_THRESHOLD,	0, 255, 1));
	
	private JSpinner minCircSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_MIN_CIRC,	0, 1, 0.05));
	private JSpinner maxCircSpinner = new JSpinner(new SpinnerNumberModel(DEFAULT_MAX_CIRC,	0, 1, 0.05));
	
	private JRadioButton forwardThresholding = new JRadioButton("Forward");
	private JRadioButton reverseThresholding = new JRadioButton("Reverse");
	private JRadioButton histogramThresholding = new JRadioButton("Adaptive");
	private ButtonGroup thresholdModeGroup;

	/**
	 * Create the dialog.
	 */
	public SignalDetectionSettingsWindow(AnalysisOptions a) {
		
		setModal(true);
		this.options = a;
		createGUI();
		
		pack();
		setVisible(true);
	}
	
	
	private void createGUI(){
		setTitle("Signal detection");
		setBounds(100, 100, 450, 300);
		
		getContentPane().setLayout(new BorderLayout());
		
		contentPanel = new JPanel();

		contentPanel.setLayout(new GridBagLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		makePanel();
		
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		
		getContentPane().add(makeLowerButtonPanel(), BorderLayout.SOUTH);
	}
	
	private void makePanel(){
		
		channelSelection = new JComboBox<String>(channelOptionStrings);
		
		groupName = new JTextField();
		
		JLabel[] labels = new JLabel[7];
		Component[] fields = new Component[7];

		labels[0] = new JLabel("Channel");
		labels[1] = new JLabel("Min signal threshold");
		labels[1].setToolTipText("No pixels with lower intensity than this will be considered signal");
		
		labels[2] = new JLabel("Min signal size");
		labels[3] = new JLabel("Max signal fraction");
		labels[4] = new JLabel("Min signal circ");
		labels[5] = new JLabel("Max signal circ");
		labels[6] = new JLabel("Group name");

		fields[0] = channelSelection;
		fields[1] = thresholdSpinner;
		fields[2] = minSizeSpinner;
		fields[3] = maxFractSpinner;
		fields[4] = minCircSpinner;
		fields[5] = maxCircSpinner;
		fields[6] = groupName;

		thresholdSpinner.addChangeListener(this);
		minSizeSpinner.addChangeListener(this);
		maxFractSpinner.addChangeListener(this);
		minCircSpinner.addChangeListener(this);
		maxCircSpinner.addChangeListener(this);

		addLabelTextRows(labels, fields, new GridBagLayout(), contentPanel );
		
		thresholdModeGroup = new ButtonGroup();
		thresholdModeGroup.add(forwardThresholding);
		thresholdModeGroup.add(reverseThresholding);
		thresholdModeGroup.add(histogramThresholding);
		
		forwardThresholding.setSelected(true);
		
		GridBagConstraints columnConstraints = new GridBagConstraints();
		columnConstraints.gridwidth = GridBagConstraints.REMAINDER;     //end row
		columnConstraints.fill = GridBagConstraints.HORIZONTAL;
		columnConstraints.weightx = 1.0;
		
		contentPanel.add(new JLabel("Threshold type"), columnConstraints);
		contentPanel.add(forwardThresholding, columnConstraints);
		contentPanel.add(reverseThresholding, columnConstraints);
		contentPanel.add(histogramThresholding, columnConstraints);
		
	}
	
	/**
	 * Create the panel with ok and cancel buttons
	 * @return the panel
	 */
	private JPanel makeLowerButtonPanel(){
		JPanel panel = new JPanel();
		panel.setLayout(new FlowLayout());

		JButton btnOk = new JButton("OK");
		btnOk.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				
				// check the selected name is valid - is the key for the options
				finalGroupName = checkGroupName(groupName.getText());

				// create the options object with the given name
				options.addNuclearSignalOptions(finalGroupName);
				
				// assign the options
				NuclearSignalOptions ns = options.getNuclearSignalOptions(finalGroupName);
				assignSettings(ns);
				
				
				SignalDetectionSettingsWindow.this.setVisible(false);
			}
		});

		panel.add(btnOk);

		JButton btnCancel = new JButton("Cancel");
		btnCancel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				SignalDetectionSettingsWindow.this.dispose();
			}
		});
		panel.add(btnCancel);
		return panel;
	}
	
	/**
	 * Check that the selected signal group name is not already
	 * used. If present or blank, requests a new name 
	 * @param name the name to check
	 * @return a valid name
	 */
	private String checkGroupName(String name){

//		IJ.log("Checking "+name);
		if(options.hasSignalDetectionOptions(name) || name.equals("")){
			String newName = (String) JOptionPane.showInputDialog("Enter another signal group name");
			name = checkGroupName(newName);
		}
		return name;
	}
	
	/**
	 * Assign the current settings to the nuclear signal analysis options
	 * @param ns the options to assign to 
	 */
	private void assignSettings(NuclearSignalOptions ns){
		
		ns.setThreshold(  (Integer) thresholdSpinner.getValue());
		ns.setMinSize(  (Integer) minSizeSpinner.getValue());
		ns.setMaxFraction(  (Double) maxFractSpinner.getValue());
		ns.setMinCirc(  (Double) minCircSpinner.getValue());
		ns.setMaxCirc(  (Double) maxCircSpinner.getValue());
		
		
		if(forwardThresholding.isSelected()){
			ns.setMode(NuclearSignalOptions.FORWARD);
		} 
		if(reverseThresholding.isSelected()){
			ns.setMode(NuclearSignalOptions.REVERSE);
		} 
		
		if(histogramThresholding.isSelected()){
			ns.setMode(NuclearSignalOptions.HISTOGRAM);
		} 
		
		this.channel = channelSelection.getSelectedItem().equals("Red") 
				? Constants.RGB_RED
						: channelSelection.getSelectedItem().equals("Green") 
						? Constants.RGB_GREEN
								: Constants.RGB_BLUE;
	}
	
	public String getSignalGroupName(){
		return this.finalGroupName;
	}
	
	/**
	 * Get the integer RGB channel
	 * @return the channel
	 */
	public int getChannel(){
		return this.channel;
	}
		
	@Override
	public void stateChanged(ChangeEvent e) {

		try{

			if(e.getSource()==thresholdSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();	
			}


			if(e.getSource()==minSizeSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
			}
			
			if(e.getSource()==maxFractSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
			}
			
			if(e.getSource()==minCircSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				if((Double) j.getValue() > (Double) maxCircSpinner.getValue()   ){
					j.setValue( maxCircSpinner.getValue() );
				}
			}
			
			if(e.getSource()==maxCircSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				
				if((Double) j.getValue()< (Double) minCircSpinner.getValue()  ){
					j.setValue( minCircSpinner.getValue() );
				}
			}

		} catch(Exception e1){
			IJ.log("Error getting signal values: "+e1.getMessage());
			for(StackTraceElement e2 : e1.getStackTrace()){
				IJ.log(e2.toString());
			}
		}
	}

}
