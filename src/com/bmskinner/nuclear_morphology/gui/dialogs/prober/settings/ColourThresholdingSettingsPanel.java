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

package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.options.PreprocessingOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions;

@SuppressWarnings("serial")
public class ColourThresholdingSettingsPanel extends SettingsPanel  {
	
	public static final Integer THRESHOLD_MIN  = Integer.valueOf(0);
	public static final Integer THRESHOLD_MAX  = Integer.valueOf(255);
	public static final Integer THRESHOLD_STEP = Integer.valueOf(1);
		
	private static final String USE_THRESHOLD_LBL  = "Threshold on colour";

	private static final String MIN_HUE_LBL       = "Min hue";
	private static final String MAX_HUE_LBL       = "Max hue";
	private static final String MIN_SAT_LBL       = "Min saturation";
	private static final String MAX_SAT_LBL       = "Max saturation";
	private static final String MIN_BRI_LBL       = "Min brightness";
	private static final String MAX_BRI_LBL       = "Max brightness";
	
	private JCheckBox 	useThresholdCheckBox;
	
	private JSpinner 	minHueSpinner;
	private JSpinner 	maxHueSpinner;
	private JSpinner 	minSatSpinner;
	private JSpinner 	maxSatSpinner;
	private JSpinner 	minBriSpinner;
	private JSpinner 	maxBriSpinner;
	
	private PreprocessingOptions options;
	
	public ColourThresholdingSettingsPanel(final IMutableDetectionOptions options){

		try {
			this.options = (PreprocessingOptions) options.getSubOptions(IDetectionSubOptions.BACKGROUND_OPTIONS);
		} catch (MissingOptionException e) {
			warn("Missing background options");
			stack(e.getMessage(), e);
		}
		
		createSpinners();
		createPanel();
	}
	
	/**
	 * Create the spinners with the default options in the CannyOptions
	 * CannyOptions must therefore have been assigned defaults
	 */
	private void createSpinners(){
		
		minHueSpinner  = new JSpinner(new SpinnerNumberModel(
				Integer.valueOf(options.getInt(PreprocessingOptions.MIN_HUE)),	
				THRESHOLD_MIN, 
				THRESHOLD_MAX, 
				THRESHOLD_STEP));
		
		maxHueSpinner  = new JSpinner(new SpinnerNumberModel(
				Integer.valueOf(options.getInt(PreprocessingOptions.MAX_HUE)),	
				THRESHOLD_MIN, 
				THRESHOLD_MAX, 
				THRESHOLD_STEP));
		
		minSatSpinner  = new JSpinner(new SpinnerNumberModel(
				Integer.valueOf(options.getInt(PreprocessingOptions.MIN_SAT)),	
				THRESHOLD_MIN, 
				THRESHOLD_MAX, 
				THRESHOLD_STEP));
		
		maxSatSpinner  = new JSpinner(new SpinnerNumberModel(
				Integer.valueOf(options.getInt(PreprocessingOptions.MAX_SAT)),	
				THRESHOLD_MIN, 
				THRESHOLD_MAX, 
				THRESHOLD_STEP));
		
		minBriSpinner  = new JSpinner(new SpinnerNumberModel(
				Integer.valueOf(options.getInt(PreprocessingOptions.MIN_BRI)),	
				THRESHOLD_MIN, 
				THRESHOLD_MAX, 
				THRESHOLD_STEP));
		
		maxBriSpinner  = new JSpinner(new SpinnerNumberModel(
				Integer.valueOf(options.getInt(PreprocessingOptions.MAX_BRI)),	
				THRESHOLD_MIN, 
				THRESHOLD_MAX, 
				THRESHOLD_STEP));

		useThresholdCheckBox = new JCheckBox("", options.getBoolean(PreprocessingOptions.USE_COLOUR_THRESHOLD));
		useThresholdCheckBox.addActionListener( e -> {
			boolean isActive = useThresholdCheckBox.isSelected();
			options.setUseColourThreshold(isActive);
			minHueSpinner.setEnabled(isActive);
			maxHueSpinner.setEnabled(isActive);
			minSatSpinner.setEnabled(isActive);
			maxSatSpinner.setEnabled(isActive);
			minBriSpinner.setEnabled(isActive);
			maxBriSpinner.setEnabled(isActive);
			fireOptionsChangeEvent();
		});
		
				
		minHueSpinner.addChangeListener( e -> {
			try {
				
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				Integer value = (Integer) j.getValue();
				
				if(value.intValue() >= (int) maxHueSpinner.getValue()){ 
					j.setValue(value.intValue() - 1); // Cannot be above max 

				} 
								
				options.setHueThreshold( (int) j.getValue(), (int) maxHueSpinner.getValue());
				fireOptionsChangeEvent();
			} catch(ParseException e1){
				warn("Parsing exception");
				stack("Parsing error in JSpinner", e1);
			}

		});
		
		maxHueSpinner.addChangeListener( e -> {
			try {
				
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				Integer value = (Integer) j.getValue();
				
				if(value.intValue() <= (int) minHueSpinner.getValue()){ 
					j.setValue(value.intValue() + 1); // Cannot be above max 

				} 
								
				options.setHueThreshold( (int) minHueSpinner.getValue(), (int) j.getValue());
				fireOptionsChangeEvent();
			} catch(ParseException e1){
				warn("Parsing exception");
				stack("Parsing error in JSpinner", e1);
			}

		});
		
		minSatSpinner.addChangeListener( e -> {
			try {
				
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				Integer value = (Integer) j.getValue();
				
				if(value.intValue() >= (int) maxSatSpinner.getValue()){ 
					j.setValue(value.intValue() - 1); // Cannot be above max 

				} 
								
				options.setSaturationThreshold( (int) j.getValue(), (int) maxSatSpinner.getValue());
				fireOptionsChangeEvent();
			} catch(ParseException e1){
				warn("Parsing exception");
				stack("Parsing error in JSpinner", e1);
			}

		});
		
		maxSatSpinner.addChangeListener( e -> {
			try {
				
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				Integer value = (Integer) j.getValue();
				
				if(value.intValue() <= (int) minSatSpinner.getValue()){ 
					j.setValue(value.intValue() + 1); // Cannot be above max 

				} 
								
				options.setSaturationThreshold( (int) minSatSpinner.getValue(), (int) j.getValue());
				fireOptionsChangeEvent();
			} catch(ParseException e1){
				warn("Parsing exception");
				stack("Parsing error in JSpinner", e1);
			}

		});
		
		minBriSpinner.addChangeListener( e -> {
			try {
				
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				Integer value = (Integer) j.getValue();
				
				if(value.intValue() >= (int) maxBriSpinner.getValue()){ 
					j.setValue(value.intValue() - 1); // Cannot be above max 

				} 
								
				options.setBrightnessThreshold( (int) j.getValue(), (int) maxBriSpinner.getValue());
				fireOptionsChangeEvent();
			} catch(ParseException e1){
				warn("Parsing exception");
				stack("Parsing error in JSpinner", e1);
			}

		});
		
		maxBriSpinner.addChangeListener( e -> {
			try {
				
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				Integer value = (Integer) j.getValue();
				
				if(value.intValue() <= (int) minBriSpinner.getValue()){ 
					j.setValue(value.intValue() + 1); // Cannot be above max 

				} 
								
				options.setBrightnessThreshold( (int) minBriSpinner.getValue(), (int) j.getValue());
				fireOptionsChangeEvent();
			} catch(ParseException e1){
				warn("Parsing exception");
				stack("Parsing error in JSpinner", e1);
			}

		});

		
	}
		
	private void createPanel(){
		
		this.setLayout(new GridBagLayout());

		List<JLabel> labelList	   = new ArrayList<JLabel>();
		List<JComponent> fieldList = new ArrayList<JComponent>();

//		labelList.add(new JLabel(USE_THRESHOLD_LBL));
		labelList.add(new JLabel(MIN_HUE_LBL));
		labelList.add(new JLabel(MAX_HUE_LBL));
		labelList.add(new JLabel(MIN_SAT_LBL));
		labelList.add(new JLabel(MAX_SAT_LBL));
		labelList.add(new JLabel(MIN_BRI_LBL));
		labelList.add(new JLabel(MAX_BRI_LBL));
		
		
		JLabel[] labels = labelList.toArray(new JLabel[0]);

//		fieldList.add(useThresholdCheckBox);
		fieldList.add(minHueSpinner);
		fieldList.add(maxHueSpinner);
		fieldList.add(minSatSpinner);
		fieldList.add(maxSatSpinner);
		fieldList.add(minBriSpinner);
		fieldList.add(maxBriSpinner);
		
		JComponent[] fields = fieldList.toArray(new JComponent[0]);

					
		addLabelTextRows(labels, fields, this );
		
	}
	
	@Override
	public void setEnabled(boolean b){
		super.setEnabled(b);
		
		useThresholdCheckBox.setEnabled(b);
		minHueSpinner.setEnabled(b);
		maxHueSpinner.setEnabled(b);
		minSatSpinner.setEnabled(b);
		maxSatSpinner.setEnabled(b);
		minBriSpinner.setEnabled(b);
		maxBriSpinner.setEnabled(b);
		

	}
	
}
