package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;

/**
 * Holds other nucleus detection options. E.g. profile window
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class NucleusProfileSettingsPanel extends SettingsPanel implements ChangeListener {
	
	private static final double MIN_PROFILE_PROP = 0;
	private static final double MAX_PROFILE_PROP = 1;
	private static final double STEP_PROFILE_PROP = 0.01;
	
	private static final String TYPE_LBL           = "Nucleus type";
	private static final String PROFILE_WINDOW_LBL = "Profile window";
	
	private IMutableAnalysisOptions options;
	
	private JSpinner  profileWindow;

	
	private JComboBox<NucleusType> typeBox;
	
	public NucleusProfileSettingsPanel(final IMutableAnalysisOptions op){
		super();
		options = op;
		this.add(createPanel(), BorderLayout.CENTER);
	}
	
	/**
	 * Create the settings spinners based on the input options
	 */
	private void createSpinners(){
		
		

		typeBox = new JComboBox<NucleusType>(NucleusType.values());
		typeBox.setSelectedItem(NucleusType.RODENT_SPERM);
		
		typeBox.addActionListener( e ->{
			IMutableDetectionOptions nucleusOptions  = options.getDetectionOptions(IAnalysisOptions.NUCLEUS);
			
			if(e.getActionCommand().equals(TYPE_LBL)){
				NucleusType type = (NucleusType) typeBox.getSelectedItem();
				options.setNucleusType(type);

				if(type.equals(NucleusType.ROUND)){
					nucleusOptions.setMinCirc(  0.0 );
					nucleusOptions.setMaxCirc(  1.0 );				
				}
				
				if(type.equals(NucleusType.RODENT_SPERM)){
					nucleusOptions.setMinCirc(  0.2 );
					nucleusOptions.setMaxCirc(  0.8 );
				}
				
				if(type.equals(NucleusType.PIG_SPERM)){
					nucleusOptions.setMinCirc(  0.2 );
					nucleusOptions.setMaxCirc(  0.8 );
				}
			}
			fireOptionsChangeEvent();
		});
		
		
		profileWindow = new JSpinner(new SpinnerNumberModel(
				options.getProfileWindowProportion(),	
				MIN_PROFILE_PROP, 
				MAX_PROFILE_PROP, 
				STEP_PROFILE_PROP));
		
		Dimension dim = new Dimension(80, 20);
		profileWindow.setPreferredSize(dim);
		profileWindow.addChangeListener(this);
	}
	
	private JPanel createPanel(){
		
		this.createSpinners();
		
		JPanel panel = new JPanel();

		panel.setLayout(new GridBagLayout());
		
		List<JLabel> labels = new ArrayList<JLabel>();
		labels.add(new JLabel(TYPE_LBL));
		labels.add(new JLabel(PROFILE_WINDOW_LBL));
		

		List<Component> fields = new ArrayList<Component>();
		
		fields.add(typeBox);
		fields.add(profileWindow);
		
		addLabelTextRows(labels, fields, new GridBagLayout(), panel );

		return panel;
	}
	
	/**
	 * Update the spinners to current options values 
	 */
	@Override
	protected void update(){
		super.update();
		profileWindow.setValue( options.getProfileWindowProportion());
	}
		
	@Override
	public void stateChanged(ChangeEvent e) {

		try {
			if(e.getSource()==profileWindow){
				JSpinner j = (JSpinner) e.getSource();
				j.commitEdit();
				options.setAngleWindowProportion(  (Double) j.getValue());
			}	
			
			fireOptionsChangeEvent();
		} catch (ParseException e1) {
			stack("Parsing error in JSpinner", e1);
		}
		
	}

}
