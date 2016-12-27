package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;

/**
 * The top level settings class for detection options
 * @author ben
 * @since 1.13.4
 *
 */
public abstract class DetectionSettingsPanel extends SettingsPanel {
	
	protected  IMutableDetectionOptions options;
	
	public DetectionSettingsPanel(IMutableDetectionOptions op){
		
		options = op;
	}
			
	/**
	 * Set the options values and update the spinners
	 * to match
	 * @param options
	 */
	public abstract void set(IDetectionOptions options);
}
