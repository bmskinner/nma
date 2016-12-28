package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.bmskinner.nuclear_morphology.components.options.ICannyOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableCannyOptions;

import ij.IJ;

/**
 * A panel that allows changes to be made to a CannyOptions
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class CannySettingsPanel extends SettingsPanel implements ActionListener, ChangeListener {
	
	public static final double THRESHOLD_STEP_SIZE = 0.05;
	public static final double KERNEL_STEP_SIZE    = 1;
	public static final double KUWAHARA_STEP_SIZE  = 2;
	
	public static final double THRESHOLD_MIN      = 0;
	public static final double KERNEL_RADIUS_MIN  = 0;
	public static final double KERNEL_WIDTH_MIN   = 1;
	public static final double KUWAHARA_WIDTH_MIN = 1;
	public static final double CLOSING_RADIUS_MIN = 1;
	
	public static final double LOW_THRESHOLD_MAX  = 10;
	public static final double HIGH_THRESHOLD_MAX = 20;
	public static final double KERNEL_RADIUS_MAX  = 20;
	public static final double KERNEL_WIDTH_MAX   = 50;
	public static final double KUWAHARA_WIDTH_MAX = 11;
	public static final double CLOSING_RADIUS_MAX = 100;
	public static final double FLATTEN_THRESHOLD_MAX = 255;
	
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
		finest("Updated Canny spinners");
		
		cannyAutoThresholdCheckBox.setSelected(options.isCannyAutoThreshold());
		useKuwaharaCheckBox.setSelected(options.isUseKuwahara());
		flattenImageCheckBox.setSelected(options.isUseFlattenImage());
		finest("Updated Canny checkboxes");
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
				options.getKernelWidth(),	
				KERNEL_WIDTH_MIN, 
				KERNEL_WIDTH_MAX, 
				KERNEL_STEP_SIZE));
		
		closingObjectRadiusSpinner = new JSpinner(new SpinnerNumberModel(
				options.getClosingObjectRadius(), 
				CLOSING_RADIUS_MIN, 
				CLOSING_RADIUS_MAX , 
				KERNEL_STEP_SIZE));
					
		kuwaharaRadiusSpinner = new JSpinner(new SpinnerNumberModel(
				options.getKuwaharaKernel(), 
				KUWAHARA_WIDTH_MIN,
				KUWAHARA_WIDTH_MAX , 
				KUWAHARA_STEP_SIZE));
		
		flattenImageThresholdSpinner = new JSpinner(new SpinnerNumberModel(
				options.getFlattenThreshold(), 
				THRESHOLD_MIN,
				FLATTEN_THRESHOLD_MAX , 
				KERNEL_STEP_SIZE));
		
		
		// add the change listeners
		cannyLowThreshold.addChangeListener(this);
		cannyHighThreshold.addChangeListener(this);
		cannyKernelRadius.addChangeListener(this);
		cannyKernelWidth.addChangeListener(this);
		closingObjectRadiusSpinner.addChangeListener(this);
		
	}
		
	private void createPanel(){
		
		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		Dimension minSize = new Dimension(10, 5);
		Dimension prefSize = new Dimension(10, 5);
		Dimension maxSize = new Dimension(Short.MAX_VALUE, 5);
		c.gridwidth = GridBagConstraints.REMAINDER; //next-to-last
		c.fill = GridBagConstraints.NONE;      //reset to default
		c.weightx = 0.1;                       //reset to default
		this.add(new Box.Filler(minSize, prefSize, maxSize),c);
		
		cannyAutoThresholdCheckBox = new JCheckBox(AUTO_THRESHOLD_LBL);
		cannyAutoThresholdCheckBox.setSelected(false);
		cannyAutoThresholdCheckBox.setActionCommand(AUTO_THRESHOLD_ACTION);
		cannyAutoThresholdCheckBox.addActionListener(this);
		this.add(cannyAutoThresholdCheckBox);
		
		this.add(new Box.Filler(minSize, prefSize, maxSize),c);
		
		
		// add the canny settings
		List<JLabel> labelList	 = new ArrayList<JLabel>();
		List<JSpinner> fieldList = new ArrayList<JSpinner>();
		
		labelList.add(new JLabel(LOW_THRESHOLD_LBL));
		labelList.add(new JLabel(HIGH_THRESHOLD_LBL));
		labelList.add(new JLabel(KERNEL_RADIUS_LBL));
		labelList.add(new JLabel(KERNEL_WIDTH_LBL));
		labelList.add(new JLabel(CLOSING_RADIUS_LBL));
		
		JLabel[] labels = labelList.toArray(new JLabel[0]);
		
		fieldList.add(cannyLowThreshold);
		fieldList.add(cannyHighThreshold);
		fieldList.add(cannyKernelRadius);
		fieldList.add(cannyKernelWidth);
		fieldList.add(closingObjectRadiusSpinner);
		
		JSpinner[] fields = fieldList.toArray(new JSpinner[0]);

					
		addLabelTextRows(labels, fields, new GridBagLayout(), this );
		
		// add a space
		this.add(new Box.Filler(minSize, prefSize, maxSize),c);
		
		// add the Kuwahara checkbox
		useKuwaharaCheckBox = new JCheckBox(USE_KUWAHARA_LBL);
		useKuwaharaCheckBox.setSelected(options.isUseKuwahara());
		useKuwaharaCheckBox.setActionCommand(USE_KUWAHARA_ACTION);
		useKuwaharaCheckBox.addActionListener(this);
		this.add(useKuwaharaCheckBox);
		this.add(new Box.Filler(minSize, prefSize, maxSize),c);
		
		// add the Kuwahara radius spinner
		labels = new JLabel[1];
		fields = new JSpinner[1];
		
		labels[0] = new JLabel(KUWAHARA_KERNEL_LBL);
		fields[0] = kuwaharaRadiusSpinner;
		
		addLabelTextRows(labels, fields, new GridBagLayout(), this );
		
		// add a space
		this.add(new Box.Filler(minSize, prefSize, maxSize),c);
		
		
		// add the chromocentre flattening checkbox
		flattenImageCheckBox = new JCheckBox(FLATTEN_CHROMOCENTRES_LBL);
		flattenImageCheckBox.setSelected(options.isUseKuwahara());
		flattenImageCheckBox.setActionCommand(FLATTEN_CHROMOCENTRES_ACTION);
		flattenImageCheckBox.addActionListener(this);
		this.add(flattenImageCheckBox);
		this.add(new Box.Filler(minSize, prefSize, maxSize),c);
					
		// add the flattening threshold spinner
		labels = new JLabel[1];
		fields = new JSpinner[1];

		labels[0] = new JLabel(FLATTENING_THRESHOLD_LBL);
		fields[0] = flattenImageThresholdSpinner;
		
		flattenImageThresholdSpinner.addChangeListener(this);
		
		addLabelTextRows(labels, fields, new GridBagLayout(), this );
		
		
		// Add the border adding box
		addBorderCheckBox = new JCheckBox(ADD_BORDER_LBL);
		addBorderCheckBox.setSelected(options.isAddBorder());
		addBorderCheckBox.setActionCommand(ADD_BORDER_ACTION);
		addBorderCheckBox.addActionListener(this);
//		this.add(addBorderCheckBox); // Do not enable until signal detector gets IAnalysisOptions
//		this.add(new Box.Filler(minSize, prefSize, maxSize),c);

		
		
		
		
	}


	@Override
	public void stateChanged(ChangeEvent e) {
		try{
			if(e.getSource()==cannyLowThreshold){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();

				if( (Double) j.getValue() > (Double) cannyHighThreshold.getValue() ){
					j.setValue( cannyHighThreshold.getValue() );
				}
				Double doubleValue = (Double) j.getValue();
				options.setLowThreshold(    doubleValue.floatValue() );
			}

			if(e.getSource()==cannyHighThreshold){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();

				if( (Double) j.getValue() < (Double) cannyLowThreshold.getValue() ){
					j.setValue( cannyLowThreshold.getValue() );
				}
				Double doubleValue = (Double) j.getValue();
				options.setHighThreshold( doubleValue.floatValue() );
			}

			if(e.getSource()==cannyKernelRadius){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				Double doubleValue = (Double) j.getValue();
				options.setKernelRadius( doubleValue.floatValue());
			}

			if(e.getSource()==cannyKernelWidth){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				options.setKernelWidth( (Integer) j.getValue());
			}

			if(e.getSource()==closingObjectRadiusSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				options.setClosingObjectRadius( (Integer) j.getValue());
			}
			
			if(e.getSource()==kuwaharaRadiusSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				Integer value = (Integer) j.getValue();
				if(value % 2 == 0){ // even
					value--; // only odd values are allowed
					j.setValue(value);
					j.commitEdit();
					j.repaint();
				}
				options.setKuwaharaKernel(value);
			}
			
			if(e.getSource()==flattenImageThresholdSpinner){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				options.setFlattenThreshold( (Integer) j.getValue());
			}
			
			fireOptionsChangeEvent();
			
		} catch (ParseException e1) {
			stack("Parsing error in JSpinner", e1);
		}
		
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
