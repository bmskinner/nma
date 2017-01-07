package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.OptionsChangeEvent;

/**
 * A settings panel to detect cytoplasm
 * @author ben
 * @since 1.13.4
 *
 */
public class CytoplasmDetectionSettingsPanel extends SettingsPanel {
	
	private IMutableDetectionOptions options;
	
	private static final String OBJECT_FINDING_LBL = "Object finding";
	private static final String SIZE_SETTINGS_LBL  = "Filtering";
	private static final String PROFILING_LBL      = "Profiling";
	private static final String MISC_LBL           = "Other";
	private static final String CHANNEL_LBL        = "Image";
	private static final String PREPROCESSING_LBL  = "Preprocessing";
	
	public CytoplasmDetectionSettingsPanel(IMutableDetectionOptions options){
		this.options = options;
		
		this.add(createPanel(), BorderLayout.CENTER);
		
	}
	
	private JPanel createPanel(){
				
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		SettingsPanel switchPanel  = new EdgeThresholdSwitchPanel(options);
		SettingsPanel prePanel     = new ImagePreprocessingSettingsPanel(options);
		SettingsPanel sizePanel    = new ComponentSizeSettingsPanel(options);
		SettingsPanel channelPanel = new ImageChannelSettingsPanel(options);
		

		prePanel.setBorder(    BorderFactory.createTitledBorder(PREPROCESSING_LBL));
		switchPanel.setBorder( BorderFactory.createTitledBorder(OBJECT_FINDING_LBL));
		sizePanel.setBorder(   BorderFactory.createTitledBorder(SIZE_SETTINGS_LBL ));
		channelPanel.setBorder(BorderFactory.createTitledBorder(CHANNEL_LBL       ));
		
		this.addSubPanel(prePanel);
		this.addSubPanel(switchPanel);
		this.addSubPanel(sizePanel);
		this.addSubPanel(channelPanel);
			
		panel.add(channelPanel);
		panel.add(prePanel);
		panel.add(switchPanel);
		panel.add(sizePanel);
				
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
