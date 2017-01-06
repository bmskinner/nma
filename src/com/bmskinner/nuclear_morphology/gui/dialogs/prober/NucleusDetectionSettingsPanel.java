package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.ComponentSizeSettingsPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.CopyFromOpenDatasetPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.EdgeThresholdSwitchPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.ImageChannelSettingsPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.ImagePreprocessingSettingsPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.MiscNucleusSettingsPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.NucleusProfileSettingsPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.SettingsPanel;

/**
 * The detection setttings for nuclei. Composed of subpanels that set each type
 * of options
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class NucleusDetectionSettingsPanel extends SettingsPanel {
	
	private IMutableAnalysisOptions options;
	
	private static final String OBJECT_FINDING_LBL = "Object finding";
	private static final String SIZE_SETTINGS_LBL  = "Filtering";
	private static final String PROFILING_LBL      = "Profiling";
	private static final String MISC_LBL           = "Other";
	private static final String CHANNEL_LBL        = "Image";
	private static final String PREPROCESSING_LBL  = "Preprocessing";
	
	public NucleusDetectionSettingsPanel(IMutableAnalysisOptions options){
		this.options = options;
		
		this.add(createPanel(), BorderLayout.CENTER);
		
	}
	
	private JPanel createPanel(){
		IMutableDetectionOptions nucleusOptions = options.getDetectionOptions(IAnalysisOptions.NUCLEUS);
				
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		SettingsPanel switchPanel  = new EdgeThresholdSwitchPanel(nucleusOptions);
		SettingsPanel prePanel     = new ImagePreprocessingSettingsPanel(nucleusOptions);
		SettingsPanel sizePanel    = new ComponentSizeSettingsPanel(nucleusOptions);
		SettingsPanel profilePanel = new NucleusProfileSettingsPanel(options);
		SettingsPanel miscPanel    = new MiscNucleusSettingsPanel(options);
		SettingsPanel channelPanel = new ImageChannelSettingsPanel(nucleusOptions);
		SettingsPanel copyPanel    = new CopyFromOpenDatasetPanel(nucleusOptions);
		

		prePanel.setBorder(    BorderFactory.createTitledBorder(PREPROCESSING_LBL));
		switchPanel.setBorder( BorderFactory.createTitledBorder(OBJECT_FINDING_LBL));
		sizePanel.setBorder(   BorderFactory.createTitledBorder(SIZE_SETTINGS_LBL ));
		profilePanel.setBorder(BorderFactory.createTitledBorder(PROFILING_LBL     ));
		miscPanel.setBorder(   BorderFactory.createTitledBorder(MISC_LBL          ));
		channelPanel.setBorder(BorderFactory.createTitledBorder(CHANNEL_LBL       ));
		
		this.addSubPanel(prePanel);
		this.addSubPanel(switchPanel);
		this.addSubPanel(sizePanel);
		this.addSubPanel(profilePanel);
		this.addSubPanel(miscPanel);
		this.addSubPanel(channelPanel);
		this.addSubPanel(copyPanel);
			
		panel.add(copyPanel);
		panel.add(channelPanel);
		panel.add(prePanel);
		panel.add(switchPanel);
		panel.add(sizePanel);
		panel.add(miscPanel);
		panel.add(profilePanel);
		
		
		return panel;
	}
	
	@Override
	public void optionsChangeEventReceived(OptionsChangeEvent e) {
		
		if(this.hasSubPanel((SettingsPanel) e.getSource())){
			update();
			
			if(e.getSource() instanceof EdgeThresholdSwitchPanel 
					|| e.getSource() instanceof ImagePreprocessingSettingsPanel 
					|| e.getSource() instanceof ComponentSizeSettingsPanel 
					|| e.getSource() instanceof ImageChannelSettingsPanel){
				fireProberReloadEvent(); // don't fire an update for values that have no effect on a prober
			}
		}

		
		

	}
}
