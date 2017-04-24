package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.components.options.IMutableNuclearSignalOptions;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.OptionsChangeEvent;

/**
 * The settings panel for detection nuclear signals. This is designed
 * to be included in an image prober, and will fire a prober reload event
 * when settings are changed.
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class SignalDetectionSettingsPanel extends SettingsPanel {
		
		private IMutableNuclearSignalOptions options;
		
		private static final String OBJECT_FINDING_LBL = "Object finding";
		private static final String SIZE_SETTINGS_LBL  = "Filtering";
		private static final String THRESHOLD_LBL      = "Thresholding";
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
			
			SettingsPanel sizePanel    = new SignalSizeSettingsPanel(options);
			SettingsPanel threshPanel  = new ThresholdSettingsPanel(options);
			SettingsPanel methodPanel  = new SignalMethodSettingsPanel(options);
			SettingsPanel channelPanel = new ImageChannelSettingsPanel(options);
			
			methodPanel.setBorder( BorderFactory.createTitledBorder(OBJECT_FINDING_LBL));
			sizePanel.setBorder(   BorderFactory.createTitledBorder(SIZE_SETTINGS_LBL ));
			threshPanel.setBorder(BorderFactory.createTitledBorder(THRESHOLD_LBL     ));
			channelPanel.setBorder(BorderFactory.createTitledBorder(CHANNEL_LBL       ));

			this.addSubPanel(methodPanel);
			this.addSubPanel(threshPanel);
			this.addSubPanel(sizePanel);
			this.addSubPanel(channelPanel);
					
			panel.add(channelPanel);
			panel.add(threshPanel);
			panel.add(methodPanel);
			panel.add(sizePanel);
			
			
			return panel;
		}
		
		@Override
		public void optionsChangeEventReceived(OptionsChangeEvent e) {
			
			if(this.hasSubPanel((SettingsPanel) e.getSource())){
				update();
				
//				if(e.getSource() instanceof ThresholdSettingsPanel 
//						|| e.getSource() instanceof SignalSizeSettingsPanel 
//						|| e.getSource() instanceof ImageChannelSettingsPanel){
					fireProberReloadEvent(); // don't fire an update for values that have no effect on a prober
//				}
			}
		}
}
