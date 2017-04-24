package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
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
		JPanel panel = new JPanel(new GridBagLayout());

		List<JLabel> labelList	   = new ArrayList<JLabel>();
		List<JComponent> fieldList = new ArrayList<JComponent>();
		
		dynamicSpinner = new JSpinner(new SpinnerNumberModel(
				Integer.valueOf( options.getInt(IDetectionOptions.DYNAMIC) ),	
				DYNAMIC_MIN_RANGE, 
				DYNAMIC_MAX_RANGE, 
				DYNAMIC_STEP));
		
		erosionSpinner = new JSpinner(new SpinnerNumberModel(
				Integer.valueOf( options.getInt(IDetectionOptions.EROSION) ),	
				EROSION_MIN_RANGE, 
				EROSION_MAX_RANGE, 
				EROSION_STEP));
		
		JLabel dynabmicLbl = new JLabel(DYNAMIC_LBL);
		
		labelList.add(dynabmicLbl);
		fieldList.add(dynamicSpinner);
		
		dynamicSpinner.addChangeListener( e ->{
			try {
				dynamicSpinner.commitEdit();	
				options.setInt( IDetectionOptions.DYNAMIC,  ((Integer) dynamicSpinner.getValue()).intValue()  );
				fireOptionsChangeEvent();
			} catch (ParseException e1) {
				warn("Parsing error in JSpinner");
				stack("Parsing error in JSpinner", e1);
			}
		});
		
		
		JLabel erosionLbl = new JLabel(EROSION_LBL);
		
		labelList.add(erosionLbl);
		fieldList.add(erosionSpinner);
		
		erosionSpinner.addChangeListener( e ->{
			try {
				erosionSpinner.commitEdit();	
				options.setInt( IDetectionOptions.EROSION,  ((Integer) erosionSpinner.getValue()).intValue()  );
				fireOptionsChangeEvent();
			} catch (ParseException e1) {
				warn("Parsing error in JSpinner");
				stack("Parsing error in JSpinner", e1);
			}
		});
		
		addLabelTextRows(labelList, fieldList, panel );

		return panel;
	}
	
	@Override
	protected void update(){
		super.update();
		isUpdating = true;
		dynamicSpinner.setValue(options.getInt(IDetectionOptions.DYNAMIC));
		erosionSpinner.setValue(options.getInt(IDetectionOptions.EROSION));
		isUpdating = false;
	}
	
	@Override
	public void setEnabled(boolean b){
		super.setEnabled(b);
		dynamicSpinner.setEnabled(b);
		erosionSpinner.setEnabled(b);

	}

	@Override
	public void set(IDetectionOptions options) {
		this.options.set(options);
		update();
		
	}

}
