package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;
import com.bmskinner.nuclear_morphology.io.ImageImporter;

/**
 * Panel for image channel settings
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class ImageChannelSettingsPanel extends DetectionSettingsPanel {
	private static final double  SCALE_STEP_SIZE = 1;
	private static final double  SCALE_MIN = 1;
	private static final double  SCALE_MAX = 100000;

	private static final String H_AND_E_LBL = "H&E";
	private static final String CHANNEL_LBL = "Channel";
	private static final String SCALE_LBL   = "Scale (pixels/micron)";
	
	private static final boolean DEFAULT_H_AND_E = false;

	private JComboBox<String> channelBox = new JComboBox<String>(channelOptionStrings);
	private JSpinner scaleSpinner;
	private JCheckBox hAndECheckBox = new JCheckBox("", DEFAULT_H_AND_E);


	public ImageChannelSettingsPanel(final IMutableDetectionOptions options){
		super(options);		
		this.add(createPanel(), BorderLayout.CENTER);

	}

	/**
	 * Create the settings spinners based on the input options
	 */
	private void createSpinners(){

		
		hAndECheckBox.addActionListener( e ->{
			options.setRGB(hAndECheckBox.isSelected());
			channelBox.setEnabled(!hAndECheckBox.isSelected());
			fireOptionsChangeEvent();
		});
		hAndECheckBox.setSelected(options.isRGB());


		channelBox.setSelectedItem(ImageImporter.channelIntToName(options.getChannel()));
		channelBox.addActionListener( e -> {
			options.setChannel(channelBox.getSelectedItem().equals("Red") 
					? ImageImporter.RGB_RED
							: channelBox.getSelectedItem().equals("Green") 
							? ImageImporter.RGB_GREEN
									: ImageImporter.RGB_BLUE);
			fireOptionsChangeEvent();
		});
		channelBox.setEnabled( ! options.isRGB());
		
		scaleSpinner    = new JSpinner(new SpinnerNumberModel(
				options.getScale(),	
				SCALE_MIN, 
				SCALE_MAX, 
				SCALE_STEP_SIZE));

		scaleSpinner.addChangeListener( e ->{

			try {


				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				options.setScale(  (Double) j.getValue());

			} catch (ParseException e1) {
				stack("Parsing error in JSpinner", e1);
			}
		});
	}

	/**
	 * Create the panel containing the settings spinners
	 * @return
	 */
	private JPanel createPanel(){

		this.createSpinners();

		JPanel panel = new JPanel();

		panel.setLayout(new GridBagLayout());

		List<JLabel> labels = new ArrayList<JLabel>();

		labels.add(new JLabel(H_AND_E_LBL));
		labels.add(new JLabel(CHANNEL_LBL));
		labels.add(new JLabel(SCALE_LBL));


		List<Component> fields = new ArrayList<Component>();
		
		fields.add(hAndECheckBox);
		fields.add(channelBox);
		fields.add(scaleSpinner);

		addLabelTextRows(labels, fields, panel );

		return panel;
	}

	/**
	 * Update the spinners to current options values 
	 */
	@Override
	protected void update(){
		super.update();
		hAndECheckBox.setSelected(options.isRGB());
		channelBox.setSelectedItem(ImageImporter.channelIntToName(options.getChannel()));
		channelBox.setEnabled( ! options.isRGB());
		scaleSpinner.setValue(options.getScale());
	}
	
	@Override
	public void setEnabled(boolean b){
		super.setEnabled(b);
		hAndECheckBox.setEnabled(b);
		if(hAndECheckBox.isSelected()){
			channelBox.setEnabled(false);
		} else{
			channelBox.setEnabled(b);
		}
		scaleSpinner.setEnabled(b);

	}

	/**
	 * Set the options values and update the spinners
	 * to match
	 * @param options
	 */
	public void set(IDetectionOptions options){
		this.options.set(options);
		update();
	}



}
