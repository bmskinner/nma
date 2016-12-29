package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.components.options.IMutableNuclearSignalOptions;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.ComponentSizeSettingsPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.EdgeThresholdSwitchPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.ImageChannelSettingsPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.SettingsPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.SignalMethodSettingsPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.SignalSizeSettingsPanel;

public class SignalDetectionSettingsPanel extends SettingsPanel {
		
		private IMutableNuclearSignalOptions options;
		
		private static final String OBJECT_FINDING_LBL = "Object finding";
		private static final String SIZE_SETTINGS_LBL  = "Filtering";
//		private static final String PROFILING_LBL      = "Profiling";
		private static final String MISC_LBL           = "Other";
		private static final String CHANNEL_LBL        = "Image";
		
		public SignalDetectionSettingsPanel(IMutableNuclearSignalOptions options){
			
			try {
			this.options = options;
			
			this.add(createPanel(), BorderLayout.CENTER);
			} catch (Exception e){
				error(e.getMessage(), e);
			}
		}
		
		private JPanel createPanel(){
			
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
			
//			SettingsPanel switchPanel  = new EdgeThresholdSwitchPanel(nucleusOptions);
			SettingsPanel sizePanel    = new SignalSizeSettingsPanel(options);
			SettingsPanel methodPanel  = new SignalMethodSettingsPanel(options);
//			SettingsPanel miscPanel    = new MiscNucleusSettingsPanel(options);
			SettingsPanel channelPanel = new ImageChannelSettingsPanel(options);
			
			

			methodPanel.setBorder( BorderFactory.createTitledBorder(OBJECT_FINDING_LBL));
			sizePanel.setBorder(   BorderFactory.createTitledBorder(SIZE_SETTINGS_LBL ));
//			profilePanel.setBorder(BorderFactory.createTitledBorder(PROFILING_LBL     ));
//			miscPanel.setBorder(   BorderFactory.createTitledBorder(MISC_LBL          ));
			channelPanel.setBorder(BorderFactory.createTitledBorder(CHANNEL_LBL       ));

			this.addSubPanel(methodPanel);
			this.addSubPanel(sizePanel);
			this.addSubPanel(channelPanel);
					
			panel.add(channelPanel);
			panel.add(methodPanel);
			panel.add(sizePanel);
			
			
			return panel;
		}
		
		@Override
		public void optionsChangeEventReceived(OptionsChangeEvent e) {
			
			if(this.hasSubPanel((SettingsPanel) e.getSource())){
				update();
				
				if(e.getSource() instanceof EdgeThresholdSwitchPanel 
						|| e.getSource() instanceof ComponentSizeSettingsPanel 
						|| e.getSource() instanceof ImageChannelSettingsPanel){
					fireProberReloadEvent(); // don't fire an update for values that have no effect on a prober
				}
			}

			
			

		}
}
