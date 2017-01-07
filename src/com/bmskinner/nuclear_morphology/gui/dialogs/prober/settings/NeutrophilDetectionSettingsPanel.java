package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.OptionsChangeEvent;

/**
 * A combined settings panel allowing setup for neutrophil detection using cytoplasm 
 * and nucleus settings.
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class NeutrophilDetectionSettingsPanel extends SettingsPanel {
	
private IMutableAnalysisOptions options;
	
	private static final String CYTO_SETTINGS_LBL = "Cytoplasm";
	private static final String NUCL_SETTINGS_LBL = "Nucleus";

	
	public NeutrophilDetectionSettingsPanel(IMutableAnalysisOptions options){
		this.options = options;
		
		this.add(createPanel(), BorderLayout.CENTER);
		
	}
	
	private JPanel createPanel(){
		IMutableDetectionOptions cytoOptions = options.getDetectionOptions(IAnalysisOptions.CYTOPLASM);
		
				
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		
		SettingsPanel cytoPanel = new CytoplasmDetectionSettingsPanel(cytoOptions);
		SettingsPanel nuclPanel = new NucleusDetectionSettingsPanel(options);

		

		cytoPanel.setBorder( BorderFactory.createTitledBorder(CYTO_SETTINGS_LBL));
		nuclPanel.setBorder( BorderFactory.createTitledBorder(NUCL_SETTINGS_LBL));

		
		this.addSubPanel(cytoPanel);
		this.addSubPanel(nuclPanel);

			
		panel.add(cytoPanel);
		panel.add(nuclPanel);

		
		return panel;
	}
	
	@Override
	public void optionsChangeEventReceived(OptionsChangeEvent e) {
		
		if(this.hasSubPanel((SettingsPanel) e.getSource())){
			update();

			fireProberReloadEvent(); 
			
		}

		
		

	}
}
