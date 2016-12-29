package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.analysis.nucleus.DefaultNucleusDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.DefaultAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;

/**
 * An image prober for detecting nuclei
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class NucleusImageProber extends IntegratedImageProber {
	
	private static final String DIALOG_TITLE_BAR_LBL = "Nucleus detection settings";

	public NucleusImageProber(final File folder){

		try {
			options = new DefaultAnalysisOptions();

			IMutableDetectionOptions nucleusOptions = new DefaultNucleusDetectionOptions(folder);
			options.setDetectionOptions(IAnalysisOptions.NUCLEUS, nucleusOptions);

			// make the panel
			optionsSettingsPanel = new NucleusDetectionSettingsPanel(options);
			imageProberPanel     = new NucleusImageProberPanel(this, nucleusOptions, ImageSet.NUCLEUS_IMAGE_SET);
			JPanel footerPanel   = createFooter();
			
			this.add(optionsSettingsPanel, BorderLayout.WEST);
			this.add(imageProberPanel,     BorderLayout.CENTER);
			this.add(footerPanel,          BorderLayout.SOUTH);

			this.setTitle(DIALOG_TITLE_BAR_LBL);
			
			optionsSettingsPanel.addProberReloadEventListener(imageProberPanel);
			
			
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
