package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.text.ParseException;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;

@SuppressWarnings("serial")
public class ThresholdSettingsPanel extends DetectionSettingsPanel implements ChangeListener {
	
	private static final int  MIN_RANGE = 0;
	private static final int  MAX_RANGE = 255;
	private static final int  STEP      = 1;
	
	private static final String THRESHOLD_LBL = "Threshold";
	
	private JSpinner thresholdSpinner;
	
	public ThresholdSettingsPanel(final IMutableDetectionOptions options){
		super(options);
		
		this.add(createPanel(), BorderLayout.CENTER);
		
	}
	
	private JPanel createPanel(){
		JPanel panel = new JPanel(new FlowLayout());
		
		thresholdSpinner = new JSpinner(new SpinnerNumberModel(
				options.getThreshold(),	
				MIN_RANGE, 
				MAX_RANGE, 
				STEP));
		
		JLabel lbl = new JLabel(THRESHOLD_LBL);
		
		panel.add(lbl);
		panel.add(thresholdSpinner);
		thresholdSpinner.addChangeListener(this);
		
		return panel;
	}

	@Override
	public void stateChanged(ChangeEvent e) {
		
		try {
			if(e.getSource()==thresholdSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();	
				options.setThreshold(  (Integer) j.getValue());
			}
			
			fireOptionsChangeEvent();
		} catch (ParseException e1) {
			stack("Parsing error in JSpinner", e1);
		}
		
		
	}
	
	@Override
	protected void update(){
		super.update();
		thresholdSpinner.setValue(options.getThreshold());
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
