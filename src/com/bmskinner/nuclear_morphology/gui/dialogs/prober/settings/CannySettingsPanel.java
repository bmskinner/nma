package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import com.bmskinner.nuclear_morphology.components.options.ICannyOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableCannyOptions;

/**
 * A panel that allows changes to be made to a CannyOptions
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class CannySettingsPanel extends SettingsPanel implements ActionListener {
	
	public static final double THRESHOLD_STEP_SIZE = 0.05;
	
	public static final double THRESHOLD_MIN      = 0;
	public static final double KERNEL_RADIUS_MIN  = 0;
	
	public static final double LOW_THRESHOLD_MAX  = 10;
	public static final double HIGH_THRESHOLD_MAX = 20;
	public static final double KERNEL_RADIUS_MAX  = 20;
	
	
	public static final Integer CANNY_KERNEL_WIDTH_MIN  = Integer.valueOf(1);
	public static final Integer CANNY_KERNEL_WIDTH_MAX  = Integer.valueOf(50);
	public static final Integer CANNY_KERNEL_WIDTH_STEP = Integer.valueOf(1);
	
	public static final Integer KUWAHARA_WIDTH_MIN  = Integer.valueOf(1);
	public static final Integer KUWAHARA_WIDTH_MAX  = Integer.valueOf(11);
	public static final Integer KUWAHARA_WIDTH_STEP = Integer.valueOf(2);
	
	public static final Integer CLOSING_RADIUS_MIN  = Integer.valueOf(1);
	public static final Integer CLOSING_RADIUS_MAX  = Integer.valueOf(100);
	public static final Integer CLOSING_RADIUS_STEP = Integer.valueOf(1);
	
	public static final Integer FLATTEN_THRESHOLD_MIN  = Integer.valueOf(0);
	public static final Integer FLATTEN_THRESHOLD_MAX  = Integer.valueOf(255);
	public static final Integer FLATTEN_THRESHOLD_STEP = Integer.valueOf(1);
	
	private static final String AUTO_THRESHOLD_ACTION        = "CannyAutoThreshold";
	private static final String USE_KUWAHARA_ACTION          = "UseKuwahara";
	private static final String FLATTEN_CHROMOCENTRES_ACTION = "FlattenChromocentres";
	private static final String ADD_BORDER_ACTION            = "AddBorder";
	
	private static final String AUTO_THRESHOLD_LBL         = "Canny auto threshold";
	private static final String USE_KUWAHARA_LBL           = "Use Kuwahara filter";
	private static final String FLATTEN_CHROMOCENTRES_LBL  = "Flatten chromocentres";
	private static final String ADD_BORDER_LBL             = "Add border to images";
	
	private static final String LOW_THRESHOLD_LBL        = "Canny low threshold";
	private static final String HIGH_THRESHOLD_LBL       = "Canny high threshold";
	private static final String KERNEL_RADIUS_LBL        = "Canny kernel radius";
	private static final String KERNEL_WIDTH_LBL         = "Canny kernel width";
	private static final String CLOSING_RADIUS_LBL       = "Closing radius";
	private static final String KUWAHARA_KERNEL_LBL      = "Kuwahara kernel";
	private static final String FLATTENING_THRESHOLD_LBL = "Flattening threshold";
	
	
	private JSpinner cannyLowThreshold;
	private JSpinner cannyHighThreshold;
	private JSpinner cannyKernelRadius;
	private JSpinner cannyKernelWidth;
	private JSpinner closingObjectRadiusSpinner;
	private JCheckBox cannyAutoThresholdCheckBox;
	
	private JCheckBox 	useKuwaharaCheckBox;
	private JSpinner 	kuwaharaRadiusSpinner;
	
	private JCheckBox 	flattenImageCheckBox;
	private JSpinner 	flattenImageThresholdSpinner;
	
	private JCheckBox 	addBorderCheckBox;
	
	private IMutableCannyOptions options;
	
	public CannySettingsPanel(final IMutableCannyOptions options){
		this.options = options;
		createSpinners();
		createPanel();
	}
	
	/**
	 * Update the display to the given options 
	 * @param options the options values to be used
	 */
	protected void update(){
		super.update();

		cannyLowThreshold.setValue( (double) options.getLowThreshold());
		cannyHighThreshold.setValue( (double) options.getHighThreshold());
		cannyKernelRadius.setValue( (double) options.getKernelRadius());
		cannyKernelWidth.setValue(options.getKernelWidth());
		closingObjectRadiusSpinner.setValue(options.getClosingObjectRadius());
		kuwaharaRadiusSpinner.setValue(options.getKuwaharaKernel());
		flattenImageThresholdSpinner.setValue(options.getFlattenThreshold());

		
		cannyAutoThresholdCheckBox.setSelected(options.isCannyAutoThreshold());
		useKuwaharaCheckBox.setSelected(options.isUseKuwahara());
		flattenImageCheckBox.setSelected(options.isUseFlattenImage());

	}
	
	public void set(final ICannyOptions options){
		
		this.options.set(options);
		update();
		
	}
	
	/**
	 * Create the spinners with the default options in the CannyOptions
	 * CannyOptions must therefore have been assigned defaults
	 */
	private void createSpinners(){
		
		cannyLowThreshold  = new JSpinner(new SpinnerNumberModel(
				options.getLowThreshold(),	
				THRESHOLD_MIN, 
				LOW_THRESHOLD_MAX, 
				THRESHOLD_STEP_SIZE));
		
		cannyHighThreshold = new JSpinner(new SpinnerNumberModel(
				options.getHighThreshold(),
				THRESHOLD_MIN, 
				HIGH_THRESHOLD_MAX, 
				THRESHOLD_STEP_SIZE));
		
		cannyKernelRadius  = new JSpinner(new SpinnerNumberModel(
				options.getKernelRadius(),	
				KERNEL_RADIUS_MIN, 
				KERNEL_RADIUS_MAX, 
				THRESHOLD_STEP_SIZE));
		
		cannyKernelWidth   = new JSpinner(new SpinnerNumberModel(
				Integer.valueOf( options.getKernelWidth() ),	
				CANNY_KERNEL_WIDTH_MIN, 
				CANNY_KERNEL_WIDTH_MAX, 
				CANNY_KERNEL_WIDTH_STEP));
		
		closingObjectRadiusSpinner = new JSpinner(new SpinnerNumberModel(
				Integer.valueOf( options.getClosingObjectRadius() ), 
				CLOSING_RADIUS_MIN, 
				CLOSING_RADIUS_MAX , 
				CLOSING_RADIUS_STEP));
					
		kuwaharaRadiusSpinner = new JSpinner(new SpinnerNumberModel(
				Integer.valueOf( options.getKuwaharaKernel() ), 
				KUWAHARA_WIDTH_MIN,
				KUWAHARA_WIDTH_MAX , 
				KUWAHARA_WIDTH_STEP));
		
		flattenImageThresholdSpinner = new JSpinner(new SpinnerNumberModel(
				Integer.valueOf(options.getFlattenThreshold()), 
				FLATTEN_THRESHOLD_MIN,
				FLATTEN_THRESHOLD_MAX , 
				FLATTEN_THRESHOLD_STEP));
		
		
		cannyAutoThresholdCheckBox = new JCheckBox("", false);
		cannyAutoThresholdCheckBox.setActionCommand(AUTO_THRESHOLD_ACTION);
		cannyAutoThresholdCheckBox.addActionListener(this);

		useKuwaharaCheckBox = new JCheckBox("", options.isUseKuwahara());
		useKuwaharaCheckBox.setActionCommand(USE_KUWAHARA_ACTION);
		useKuwaharaCheckBox.addActionListener(this);
		
		flattenImageCheckBox = new JCheckBox("", options.isUseKuwahara());
		flattenImageCheckBox.setActionCommand(FLATTEN_CHROMOCENTRES_ACTION);
		flattenImageCheckBox.addActionListener(this);
		
		// Add the border adding box
		addBorderCheckBox = new JCheckBox("", options.isAddBorder());
		addBorderCheckBox.setActionCommand(ADD_BORDER_ACTION);
		addBorderCheckBox.addActionListener(this);
		
		// add the change listeners
		cannyLowThreshold.addChangeListener( e -> {
			try {
				JSpinner j = (JSpinner) e.getSource();
				cannyLowThreshold.commitEdit();

				if( (Double) j.getValue() > (Double) cannyHighThreshold.getValue() ){
					cannyLowThreshold.setValue( cannyHighThreshold.getValue() );
				}
				Double doubleValue = (Double) j.getValue();
				options.setLowThreshold(    doubleValue.floatValue() );
				fireOptionsChangeEvent();
			} catch(ParseException e1){
				warn("Parsing exception");
				stack("Parsing error in JSpinner", e1);
			}
			
		});
		
		// add the change listeners
		cannyHighThreshold.addChangeListener( e -> {
			try {
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();

				if( (Double) j.getValue() < (Double) cannyLowThreshold.getValue() ){
					j.setValue( cannyLowThreshold.getValue() );
				}
				Double doubleValue = (Double) j.getValue();
				options.setHighThreshold( doubleValue.floatValue() );
				fireOptionsChangeEvent();
			} catch(ParseException e1){
				warn("Parsing exception");
				stack("Parsing error in JSpinner", e1);
			}

		});
		
		cannyKernelRadius.addChangeListener( e -> {
			try {
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				Double doubleValue = (Double) j.getValue();
				options.setKernelRadius( doubleValue.floatValue());
				fireOptionsChangeEvent();
			} catch(ParseException e1){
				warn("Parsing exception");
				stack("Parsing error in JSpinner", e1);
			}

		});
		
		
		cannyKernelWidth.addChangeListener( e -> {
			try {
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				Integer value = (Integer) j.getValue();
				options.setKernelWidth( value.intValue());
				fireOptionsChangeEvent();
			} catch(ParseException e1){
				warn("Parsing exception");
				stack("Parsing error in JSpinner", e1);
			}

		});
		
		closingObjectRadiusSpinner.addChangeListener( e -> {
			try {
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				options.setClosingObjectRadius( (Integer) j.getValue());
				fireOptionsChangeEvent();
			} catch(ParseException e1){
				warn("Parsing exception");
				stack("Parsing error in JSpinner", e1);
			}

		});
		
		flattenImageThresholdSpinner.addChangeListener( e -> {
			try {
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				options.setFlattenThreshold( (Integer) j.getValue());
				fireOptionsChangeEvent();
			} catch(ParseException e1){
				warn("Parsing exception");
				stack("Parsing error in JSpinner", e1);
			}

		});
		
		kuwaharaRadiusSpinner.addChangeListener( e -> {
			try {
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				Integer value = (Integer) j.getValue();
				
				if(value.intValue() % 2 == 0){ // even
					// only odd values are allowed
					j.setValue(value.intValue() - 1);

				} else {
					options.setKuwaharaKernel(value);
					fireOptionsChangeEvent();
				}
				
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
		
		labelList.add(new JLabel(AUTO_THRESHOLD_LBL));
		labelList.add(new JLabel(LOW_THRESHOLD_LBL));
		labelList.add(new JLabel(HIGH_THRESHOLD_LBL));
		labelList.add(new JLabel(KERNEL_RADIUS_LBL));
		labelList.add(new JLabel(KERNEL_WIDTH_LBL));
		labelList.add(new JLabel(CLOSING_RADIUS_LBL));
		labelList.add(new JLabel(USE_KUWAHARA_LBL));
		labelList.add(new JLabel(KUWAHARA_KERNEL_LBL));
		labelList.add(new JLabel(FLATTEN_CHROMOCENTRES_LBL));
		labelList.add(new JLabel(FLATTENING_THRESHOLD_LBL));
//		labelList.add(new JLabel(ADD_BORDER_LBL));
		
		
		JLabel[] labels = labelList.toArray(new JLabel[0]);
		
		fieldList.add(cannyAutoThresholdCheckBox);
		fieldList.add(cannyLowThreshold);
		fieldList.add(cannyHighThreshold);
		fieldList.add(cannyKernelRadius);
		fieldList.add(cannyKernelWidth);
		fieldList.add(closingObjectRadiusSpinner);
		fieldList.add(useKuwaharaCheckBox);
		fieldList.add(kuwaharaRadiusSpinner);
		fieldList.add(flattenImageCheckBox);
		fieldList.add(flattenImageThresholdSpinner);
//		fieldList.add(addBorderCheckBox);
		
		JComponent[] fields = fieldList.toArray(new JComponent[0]);

					
		addLabelTextRows(labels, fields, this );
		
	}
	
	@Override
	public void setEnabled(boolean b){
		super.setEnabled(b);
		
		if(b){
			cannyLowThreshold.setEnabled(!cannyAutoThresholdCheckBox.isSelected());
			cannyHighThreshold.setEnabled(!cannyAutoThresholdCheckBox.isSelected());
			flattenImageThresholdSpinner.setEnabled(flattenImageCheckBox.isSelected());
			kuwaharaRadiusSpinner.setEnabled(useKuwaharaCheckBox.isSelected());
		} else {
			cannyLowThreshold.setEnabled(false);
			cannyHighThreshold.setEnabled(false);
			flattenImageThresholdSpinner.setEnabled(false);
			kuwaharaRadiusSpinner.setEnabled(false);
		}
		
		
		cannyKernelRadius.setEnabled(b);
		cannyKernelWidth.setEnabled(b);
		closingObjectRadiusSpinner.setEnabled(b);

		cannyAutoThresholdCheckBox.setEnabled(b);
		useKuwaharaCheckBox.setEnabled(b);
		flattenImageCheckBox.setEnabled(b);

	}



	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals(AUTO_THRESHOLD_ACTION)){

			if(cannyAutoThresholdCheckBox.isSelected()){
				options.setCannyAutoThreshold(true);
				cannyLowThreshold.setEnabled(false);
				cannyHighThreshold.setEnabled(false);
			} else {
				options.setCannyAutoThreshold(false);
				cannyLowThreshold.setEnabled(true);
				cannyHighThreshold.setEnabled(true);
			}
		}
		
		if(e.getActionCommand().equals(USE_KUWAHARA_ACTION)){

			if(useKuwaharaCheckBox.isSelected()){
				options.setUseKuwahara(true);
				kuwaharaRadiusSpinner.setEnabled(true);
			} else {
				options.setUseKuwahara(false);
				kuwaharaRadiusSpinner.setEnabled(false);
			}
		}
		
		if(e.getActionCommand().equals(FLATTEN_CHROMOCENTRES_ACTION)){

			if(flattenImageCheckBox.isSelected()){
				options.setFlattenImage(true);
				flattenImageThresholdSpinner.setEnabled(true);
			} else {
				options.setFlattenImage(false);
				flattenImageThresholdSpinner.setEnabled(false);
			}
		}
		
		if(e.getActionCommand().equals(ADD_BORDER_ACTION)){

			options.setAddBorder(addBorderCheckBox.isSelected());
		}
		fireOptionsChangeEvent();
	}
}
