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
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;

/**
 * Holds the Kuwahara and flattening settings for nucleus detection
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class ImagePreprocessingSettingsPanel extends SettingsPanel implements ActionListener {
	
	
	public static final Integer KUWAHARA_WIDTH_MIN  = Integer.valueOf(1);
	public static final Integer KUWAHARA_WIDTH_MAX  = Integer.valueOf(11);
	public static final Integer KUWAHARA_WIDTH_STEP = Integer.valueOf(2);
	
	public static final Integer FLATTEN_THRESHOLD_MIN  = Integer.valueOf(0);
	public static final Integer FLATTEN_THRESHOLD_MAX  = Integer.valueOf(255);
	public static final Integer FLATTEN_THRESHOLD_STEP = Integer.valueOf(1);
	
	private static final String USE_KUWAHARA_ACTION          = "UseKuwahara";
	private static final String FLATTEN_CHROMOCENTRES_ACTION = "FlattenChromocentres";
	private static final String ADD_BORDER_ACTION            = "AddBorder";
	
	
	private static final String USE_KUWAHARA_LBL           = "Use Kuwahara filter";
	private static final String FLATTEN_CHROMOCENTRES_LBL  = "Flatten chromocentres";
	private static final String ADD_BORDER_LBL             = "Add border to images";
	
	private static final String KUWAHARA_KERNEL_LBL      = "Kuwahara kernel";
	private static final String FLATTENING_THRESHOLD_LBL = "Flattening threshold";
	
	private JCheckBox 	useKuwaharaCheckBox;
	private JSpinner 	kuwaharaRadiusSpinner;
	
	private JCheckBox 	flattenImageCheckBox;
	private JSpinner 	flattenImageThresholdSpinner;
	
	private JCheckBox 	addBorderCheckBox;
	
	private IMutableCannyOptions options;
	
	public ImagePreprocessingSettingsPanel(final IMutableDetectionOptions options){
		this.options = options.getCannyOptions();
		createSpinners();
		createPanel();
	}
	
	/**
	 * Update the display to the given options 
	 * @param options the options values to be used
	 */
	protected void update(){
		super.update();

		kuwaharaRadiusSpinner.setValue(options.getKuwaharaKernel());
		flattenImageThresholdSpinner.setValue(options.getFlattenThreshold());

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

		labelList.add(new JLabel(USE_KUWAHARA_LBL));
		labelList.add(new JLabel(KUWAHARA_KERNEL_LBL));
		labelList.add(new JLabel(FLATTEN_CHROMOCENTRES_LBL));
		labelList.add(new JLabel(FLATTENING_THRESHOLD_LBL));
//		labelList.add(new JLabel(ADD_BORDER_LBL));
		
		
		JLabel[] labels = labelList.toArray(new JLabel[0]);

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
			flattenImageThresholdSpinner.setEnabled(flattenImageCheckBox.isSelected());
			kuwaharaRadiusSpinner.setEnabled(useKuwaharaCheckBox.isSelected());
		} else {
			flattenImageThresholdSpinner.setEnabled(false);
			kuwaharaRadiusSpinner.setEnabled(false);
		}

		useKuwaharaCheckBox.setEnabled(b);
		flattenImageCheckBox.setEnabled(b);

	}



	@Override
	public void actionPerformed(ActionEvent e) {
				
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
