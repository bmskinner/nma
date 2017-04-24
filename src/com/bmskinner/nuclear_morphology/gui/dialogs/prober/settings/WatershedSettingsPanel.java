package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.text.ParseException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;

public class WatershedSettingsPanel  extends DetectionSettingsPanel  {
	
	private static final Integer  DYNAMIC_MIN_RANGE = Integer.valueOf(1);
	private static final Integer  DYNAMIC_MAX_RANGE = Integer.valueOf(255);
	private static final Integer  DYNAMIC_STEP      = Integer.valueOf(1);
	
	private static final Integer  EROSION_MIN_RANGE = Integer.valueOf(1);
	private static final Integer  EROSION_MAX_RANGE = Integer.valueOf(100);
	private static final Integer  EROSION_STEP      = Integer.valueOf(1);
	
	private static final String DYNAMIC_LBL = "Dynamic";
	private static final String EROSION_LBL = "Erosion";
	
	private JSpinner dynamicSpinner;
	private JSpinner erosionSpinner;
	
	public WatershedSettingsPanel(final IMutableDetectionOptions options){
		super(options);
		
		this.add(createPanel(), BorderLayout.CENTER);
		
	}
	
	private JPanel createPanel(){
		JPanel panel = new JPanel(new FlowLayout());
		
		dynamicSpinner = new JSpinner(new SpinnerNumberModel(
				Integer.valueOf( options.getInt("erosion) ),	
				DYNAMIC_MIN_RANGE, 
				DYNAMIC_MAX_RANGE, 
				DYNAMIC_STEP));
		
		erosionSpinner = new JSpinner(new SpinnerNumberModel(
				Integer.valueOf( options.getThreshold() ),	
				EROSION_MIN_RANGE, 
				EROSION_MAX_RANGE, 
				EROSION_STEP));
		
		JLabel lbl = new JLabel(DYNAMIC_LBL);
		
		panel.add(lbl);
		panel.add(thresholdSpinner);
		
		thresholdSpinner.addChangeListener( e ->{
			try {
				thresholdSpinner.commitEdit();	
				options.setThreshold(  ((Integer) thresholdSpinner.getValue()).intValue() );
				fireOptionsChangeEvent();
			} catch (ParseException e1) {
				warn("Parsing error in JSpinner");
				stack("Parsing error in JSpinner", e1);
			}
		});

		return panel;
	}
	
	@Override
	protected void update(){
		super.update();
		isUpdating = true;
		thresholdSpinner.setValue(options.getThreshold());
		isUpdating = false;
	}
	
	@Override
	public void setEnabled(boolean b){
		super.setEnabled(b);
		thresholdSpinner.setEnabled(b);

	}

	@Override
	public void set(IDetectionOptions options) {
		this.options.set(options);
		update();
		
	}

}
