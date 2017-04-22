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

@SuppressWarnings("serial")
public class ThresholdSettingsPanel extends DetectionSettingsPanel  {
	
	private static final Integer  MIN_RANGE = Integer.valueOf(0);
	private static final Integer  MAX_RANGE = Integer.valueOf(255);
	private static final Integer  STEP      = Integer.valueOf(1);
	
	private static final String THRESHOLD_LBL = "Threshold";
	
	private JSpinner thresholdSpinner;
	
	public ThresholdSettingsPanel(final IMutableDetectionOptions options){
		super(options);
		
		this.add(createPanel(), BorderLayout.CENTER);
		
	}
	
	private JPanel createPanel(){
		JPanel panel = new JPanel(new FlowLayout());
		
		thresholdSpinner = new JSpinner(new SpinnerNumberModel(
				Integer.valueOf( options.getThreshold() ),	
				MIN_RANGE, 
				MAX_RANGE, 
				STEP));
		
		JLabel lbl = new JLabel(THRESHOLD_LBL);
		
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
