package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.bmskinner.nuclear_morphology.components.options.IHoughDetectionOptions.IMutableHoughDetectionOptions;

/**
 * Set parameters for Hough circle detection
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class HoughSettingsPanel extends SettingsPanel {
	
	public static final Integer HOUGH_MIN_RADIUS_MIN    = Integer.valueOf(5);
	public static final Integer HOUGH_MIN_RADIUS_MAX    = Integer.valueOf(100);
	public static final Integer HOUGH_MIN_RADIUS_STEP   = Integer.valueOf(1);
	
	public static final Integer HOUGH_MAX_RADIUS_MIN    = Integer.valueOf(5);
	public static final Integer HOUGH_MAX_RADIUS_MAX    = Integer.valueOf(100);
	public static final Integer HOUGH_MAX_RADIUS_STEP   = Integer.valueOf(1);
	
	public static final Integer NUMBER_OF_CIRCLES_MIN  = Integer.valueOf(0);
	public static final Integer NUMBER_OF_CIRCLES_MAX  = Integer.valueOf(10);
	public static final Integer NUMBER_OF_CIRCLES_STEP = Integer.valueOf(1);
	
	public static final Integer THRESHOLD_MIN  = Integer.valueOf(-1);
	public static final Integer THRESHOLD_MAX  = Integer.valueOf(255);
	public static final Integer THRESHOLD_STEP = Integer.valueOf(1);
		
	private static final String MIN_RADIUS_LBL        = "Min radius";
	private static final String MAX_RADIUS_LBL        = "Max radius";
	private static final String NUMBER_OF_CIRCLES_LBL = "Number of circles";
	private static final String THRESHOLD_LBL         = "Threshold";

	private JSpinner minRadiusSpinner;
	private JSpinner maxRadiusSpinner;
	private JSpinner numCirclesSpinner;
	private JSpinner thresholdSpinner;

	private IMutableHoughDetectionOptions options;
	
	public HoughSettingsPanel(final IMutableHoughDetectionOptions options){
		this.options = options;
		createSpinners();
		createPanel();
	}
	
	/**
	 * Create the spinners with the default options in the CannyOptions
	 * CannyOptions must therefore have been assigned defaults
	 */
	private void createSpinners(){
		
		minRadiusSpinner  = new JSpinner(new SpinnerNumberModel(
				Integer.valueOf(options.getMinRadius()),	
				HOUGH_MIN_RADIUS_MIN, 
				HOUGH_MIN_RADIUS_MAX, 
				HOUGH_MIN_RADIUS_STEP));
		
		maxRadiusSpinner  = new JSpinner(new SpinnerNumberModel(
				Integer.valueOf(options.getMaxRadius()),	
				HOUGH_MAX_RADIUS_MIN, 
				HOUGH_MAX_RADIUS_MAX, 
				HOUGH_MAX_RADIUS_STEP));
		
		numCirclesSpinner  = new JSpinner(new SpinnerNumberModel(
				Integer.valueOf(options.getNumberOfCircles()),	
				NUMBER_OF_CIRCLES_MIN, 
				NUMBER_OF_CIRCLES_MAX, 
				NUMBER_OF_CIRCLES_STEP));
		
		thresholdSpinner  = new JSpinner(new SpinnerNumberModel(
				Integer.valueOf(options.getHoughThreshold()),	
				THRESHOLD_MIN, 
				THRESHOLD_MAX, 
				THRESHOLD_STEP));
		
		// add the change listeners
		minRadiusSpinner.addChangeListener( e -> {
			try {
				JSpinner j = (JSpinner) e.getSource();
				minRadiusSpinner.commitEdit();

//				if( (Double) j.getValue() > (Double) maxRadiusSpinner.getValue() ){
//					minRadiusSpinner.setValue( maxRadiusSpinner.getValue() );
//				}
				Integer value = (Integer) j.getValue();
				options.setMinRadius( value.intValue() );
				fireOptionsChangeEvent();
			} catch(ParseException e1){
				warn("Parsing exception");
				stack("Parsing error in JSpinner", e1);
			}
			
		});
				
		// add the change listeners
		maxRadiusSpinner.addChangeListener( e -> {
			try {
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();

//				if( (Double) j.getValue() < (Double) minRadiusSpinner.getValue() ){
//					j.setValue( minRadiusSpinner.getValue() );
//				}
				Integer value = (Integer) j.getValue();
				options.setMaxRadius( value.intValue() );
				fireOptionsChangeEvent();
			} catch(ParseException e1){
				warn("Parsing exception");
				stack("Parsing error in JSpinner", e1);
			}

		});
		
		numCirclesSpinner.addChangeListener( e -> {
			try {
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				Integer value = (Integer) j.getValue();
				options.setNumberOfCircles(value.intValue());
				
				if(value >0){
					thresholdSpinner.setEnabled(false);
				} else {
					thresholdSpinner.setEnabled(true);
				}
				
				fireOptionsChangeEvent();
			} catch(ParseException e1){
				warn("Parsing exception");
				stack("Parsing error in JSpinner", e1);
			}

		});
		
		thresholdSpinner.addChangeListener( e -> {
			try {
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				Integer value = (Integer) j.getValue();
				options.setHoughThreshold(value.intValue());
				
				if(value > -1){
					numCirclesSpinner.setEnabled(false);
				} else {
					numCirclesSpinner.setEnabled(true);
				}
				
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
		
		labelList.add(new JLabel(MIN_RADIUS_LBL));
		labelList.add(new JLabel(MAX_RADIUS_LBL));
		labelList.add(new JLabel(NUMBER_OF_CIRCLES_LBL));
		labelList.add(new JLabel(THRESHOLD_LBL));
		
		JLabel[] labels = labelList.toArray(new JLabel[0]);
		
		fieldList.add(minRadiusSpinner);
		fieldList.add(maxRadiusSpinner);
		fieldList.add(numCirclesSpinner);
		fieldList.add(thresholdSpinner);
		
		JComponent[] fields = fieldList.toArray(new JComponent[0]);

					
		addLabelTextRows(labels, fields, this );
		
	}
	
	/**
	 * Update the display to the given options 
	 * @param options the options values to be used
	 */
	protected void update(){
		super.update();

		minRadiusSpinner.setValue(  Double.valueOf(options.getMinRadius()));
		maxRadiusSpinner.setValue(  Double.valueOf(options.getMaxRadius()));
		numCirclesSpinner.setValue( Integer.valueOf(options.getNumberOfCircles()));
		thresholdSpinner.setValue( Integer.valueOf(options.getHoughThreshold()));
	}
}
