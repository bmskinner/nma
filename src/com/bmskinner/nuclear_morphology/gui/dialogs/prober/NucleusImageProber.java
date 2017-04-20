package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.Finder;
import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.FluorescentNucleusFinder;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.ConstructableSettingsPanel;
//import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.NucleusDetectionSettingsPanel;

/**
 * An image prober for detecting nuclei
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class NucleusImageProber extends IntegratedImageProber {
	
	private static final String DIALOG_TITLE_BAR_LBL = "Nucleus detection settings";

	public NucleusImageProber(final File folder, final IMutableAnalysisOptions o){

		try {
			this.options = o;
			
//			options.setDetectionOptions(IAnalysisOptions.NUCLEUS, nucleusOptions);
			IMutableDetectionOptions nucleusOptions = options.getDetectionOptions(IAnalysisOptions.NUCLEUS);

			// make the panel
			
			optionsSettingsPanel = new ConstructableSettingsPanel(options)
				.addCopyFromOpenPanel(IAnalysisOptions.NUCLEUS)
				.addImageChannelPanel(IAnalysisOptions.NUCLEUS)
				.addImageProcessingPanel(IAnalysisOptions.NUCLEUS)
				.addSwitchPanel(IAnalysisOptions.NUCLEUS)
				.addSizePanel(IAnalysisOptions.NUCLEUS)
				.addMiscNucleusSettingsPanel(IAnalysisOptions.NUCLEUS)
				.addNucleusProfilePanel(IAnalysisOptions.NUCLEUS)
				.build();
			
			Finder finder = new FluorescentNucleusFinder(options);
			imageProberPanel = new GenericImageProberPanel(folder, finder, this);
//			optionsSettingsPanel = new NucleusDetectionSettingsPanel(options);
//			imageProberPanel     = new NucleusImageProberPanel(this, nucleusOptions, ImageSet.NUCLEUS_IMAGE_SET);
			JPanel footerPanel   = createFooter();
			
			this.add(optionsSettingsPanel, BorderLayout.WEST);
			this.add(imageProberPanel,     BorderLayout.CENTER);
			this.add(footerPanel,          BorderLayout.SOUTH);

			this.setTitle(DIALOG_TITLE_BAR_LBL);
			
			optionsSettingsPanel.addProberReloadEventListener(imageProberPanel); // inform update needed
			imageProberPanel.addPanelUpdatingEventListener(optionsSettingsPanel); // disable settings while working
			
			
		} catch (Exception e){
			warn("Error launching analysis window");
			stack(e.getMessage(), e);
			this.dispose();
		}	

		this.pack();
		this.setModal(true);
		this.setLocationRelativeTo(null); // centre on screen
		this.setVisible(true);
	}
	
	public IMutableAnalysisOptions getOptions(){
		return options;
	}

	@Override
	protected void okButtonClicked() {
		// no other action here
		
	}

}
