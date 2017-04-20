package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.AbstractImageProberPanel.PanelUpdatingEvent;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.AbstractImageProberPanel.PanelUpdatingEventListener;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.OptionsChangeEvent;

/**
 * A combined settings panel allowing setup for neutrophil detection using cytoplasm 
 * and nucleus settings.
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class NeutrophilDetectionSettingsPanel extends SettingsPanel implements PanelUpdatingEventListener {
	
	private IMutableAnalysisOptions options;
	
	private static final String CYTO_SETTINGS_LBL = "Cytoplasm";
	private static final String NUCL_SETTINGS_LBL = "Nucleus";
	private static final String RELOAD_LBL        = "Reload";
	
	public NeutrophilDetectionSettingsPanel(IMutableAnalysisOptions options){
		this.options = options;
		
		this.setLayout(new BorderLayout());
		this.add(createPanel(), BorderLayout.CENTER);
		this.add(createFooter(), BorderLayout.SOUTH);
	}
	
	private JPanel createPanel(){
				
				
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
				

		try {
			SettingsPanel cytoPanel = new ConstructableSettingsPanel(options)
				.addImageChannelPanel(IAnalysisOptions.CYTOPLASM, ConstructableSettingsPanel.CHANNEL_LBL)
				.addColorThresholdPanel(IAnalysisOptions.CYTOPLASM, ConstructableSettingsPanel.THRESHOLDING_LBL)
				.addSizePanel(IAnalysisOptions.CYTOPLASM, ConstructableSettingsPanel.SIZE_SETTINGS_LBL)
				.build();



			SettingsPanel nuclPanel = new ConstructableSettingsPanel(options)
				.addImageChannelPanel(IAnalysisOptions.NUCLEUS, ConstructableSettingsPanel.CHANNEL_LBL)
				.addColorThresholdPanel(IAnalysisOptions.NUCLEUS, ConstructableSettingsPanel.THRESHOLDING_LBL)
				.addSizePanel(IAnalysisOptions.NUCLEUS, ConstructableSettingsPanel.SIZE_SETTINGS_LBL)
				.addNucleusProfilePanel(IAnalysisOptions.NUCLEUS, ConstructableSettingsPanel.PROFILING_LBL)
				.build();
			
			cytoPanel.setBorder( BorderFactory.createTitledBorder(CYTO_SETTINGS_LBL));
			nuclPanel.setBorder( BorderFactory.createTitledBorder(NUCL_SETTINGS_LBL));

			
			this.addSubPanel(cytoPanel);
			this.addSubPanel(nuclPanel);

				
			panel.add(cytoPanel);
			panel.add(nuclPanel);

				
		} catch (MissingOptionException e) {
			warn("Cannot make panels; missing options");
			stack(e.getMessage(), e);
		}

		
		return panel;
	}
	
	private JPanel createFooter(){
		JPanel panel = new JPanel();
		JButton reloadBtn = new JButton(RELOAD_LBL);
		reloadBtn.addActionListener( e->{
			fireProberReloadEvent(); 
		});
		
		panel.add(reloadBtn);
		return panel;
		
	}
	
	@Override
	public void optionsChangeEventReceived(OptionsChangeEvent e) {
		
		fireProberReloadEvent(); 
//		if(this.hasSubPanel((SettingsPanel) e.getSource())){
//			update();
//
//			
//			
//		}

		
		

	}

	@Override
	public void panelUpdatingEventReceived(PanelUpdatingEvent e) {
		// TODO Auto-generated method stub
		
	}
}
