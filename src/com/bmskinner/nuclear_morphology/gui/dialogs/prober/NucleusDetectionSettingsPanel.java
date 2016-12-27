package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * The detection setttings for nuclei. Composed of subpanels that set each type
 * of options
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class NucleusDetectionSettingsPanel extends SettingsPanel implements Loggable, OptionsChangeListener {
	
	private IMutableAnalysisOptions options;
	
	private static final String OBJECT_FINDING_LBL = "Object finding";
	private static final String SIZE_SETTINGS_LBL  = "Filtering";
	private static final String PROFILING_LBL      = "Profiling";
	private static final String MISC_LBL           = "Other";
	private static final String CHANNEL_LBL        = "Image";
	
	public NucleusDetectionSettingsPanel(IMutableAnalysisOptions options){
		this.options = options;
		
		this.add(createPanel(), BorderLayout.CENTER);
		
	}
	
	private JPanel createPanel(){
		IMutableDetectionOptions nucleusOptions = options.getDetectionOptions(IAnalysisOptions.NUCLEUS);
				
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		SettingsPanel switchPanel  = new EdgeThresholdSwitchPanel(nucleusOptions);
		SettingsPanel sizePanel    = new ComponentSizeSettingsPanel(nucleusOptions);
		SettingsPanel profilePanel = new NucleusProfileSettingsPanel(options);
		SettingsPanel miscPanel    = new MiscNucleusSettingsPanel(options);
		SettingsPanel channelPanel = new ImageChannelSettingsPanel(nucleusOptions);
		

		switchPanel.setBorder( BorderFactory.createTitledBorder(OBJECT_FINDING_LBL));
		sizePanel.setBorder(   BorderFactory.createTitledBorder(SIZE_SETTINGS_LBL ));
		profilePanel.setBorder(BorderFactory.createTitledBorder(PROFILING_LBL     ));
		miscPanel.setBorder(   BorderFactory.createTitledBorder(MISC_LBL          ));
		channelPanel.setBorder(BorderFactory.createTitledBorder(CHANNEL_LBL       ));

		this.addSubPanel(switchPanel);
		this.addSubPanel(sizePanel);
		this.addSubPanel(profilePanel);
		this.addSubPanel(miscPanel);
		this.addSubPanel(channelPanel);
		
		switchPanel.addOptionsChangeListener(this);
		sizePanel.addOptionsChangeListener(this);
		profilePanel.addOptionsChangeListener(this);
		miscPanel.addOptionsChangeListener(this);
		channelPanel.addOptionsChangeListener(this);
		
		panel.add(channelPanel);
		panel.add(switchPanel);
		panel.add(profilePanel);
		panel.add(sizePanel);
		panel.add(miscPanel);
		
		
		return panel;
	}	

}
