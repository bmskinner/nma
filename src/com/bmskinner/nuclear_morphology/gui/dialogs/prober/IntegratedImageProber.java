package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.io.File;

import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.components.options.DefaultAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableAnalysisOptions;
import com.bmskinner.nuclear_morphology.gui.LoadingIconDialog;

/**
 * Integrates the analysis setup dialog with the image prober.
 * Experimental.
 * @author ben
 *
 */
public class IntegratedImageProber extends LoadingIconDialog {
	
	private IMutableAnalysisOptions options; // the active options
	
	private NucleusDetectionSettingsPanel optionsSettingsPanel; // settings
	
	private ImageProberPanel proberPanel; // result
	
	private File folder;
	
	
	public IntegratedImageProber(final File folder){
		
		this.folder = folder;
		
		// make the panel
		
		// add a listener to the options settings panel - when they change, refresh the
		// panel and update the analysis options
		
		// Detect which stage of the process has been altered, so only the necessary 
		// table cells are updated
		
		// Need to be able to cancel updating panels when options change
	}

}
