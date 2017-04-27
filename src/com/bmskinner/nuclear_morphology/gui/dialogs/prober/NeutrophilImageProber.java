/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.Finder;
import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.NeutrophilFinder;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.ConstructableSettingsPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.NeutrophilDetectionSettingsPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.SettingsPanel;

@SuppressWarnings("serial")
public class NeutrophilImageProber  extends IntegratedImageProber {
	
	private static final String DIALOG_TITLE_BAR_LBL = "Neutrophil detection settings";

	public NeutrophilImageProber(final File folder){
		
		try {

			// Create the options
			options = OptionsFactory.makeDefaultNeutrophilDetectionOptions(folder);

			Finder<?> finder = new NeutrophilFinder(options);
			
			optionsSettingsPanel = new ConstructableSettingsPanel(options)
					.addColourThresholdWatershedSwitchPanel(IAnalysisOptions.CYTOPLASM, "Cytoplasm detection")
					.addSizePanel(IAnalysisOptions.CYTOPLASM, "Cytoplasm filtering")
					.addTopHatPanel(IAnalysisOptions.NUCLEUS, "Nucleus detection")
					.addThresholdPanel(IAnalysisOptions.NUCLEUS, "Nucleus threshold")
					.addSizePanel(IAnalysisOptions.NUCLEUS, "Nucleus filtering")
					.addNucleusProfilePanel(IAnalysisOptions.NUCLEUS, ConstructableSettingsPanel.PROFILING_LBL)
					.build();
			// make the panel
//			optionsSettingsPanel = new NeutrophilDetectionSettingsPanel(options);
			imageProberPanel     = new GenericImageProberPanel(folder, finder, this);

			JPanel footerPanel   = createFooter();
			
			this.add(optionsSettingsPanel, BorderLayout.WEST);
			this.add(imageProberPanel,     BorderLayout.CENTER);
			this.add(footerPanel,          BorderLayout.SOUTH);

			this.setTitle(DIALOG_TITLE_BAR_LBL);
			
			optionsSettingsPanel.addProberReloadEventListener(imageProberPanel); // inform update needed
			imageProberPanel.addPanelUpdatingEventListener(optionsSettingsPanel); // disable settings while working
						
			
		} catch (Exception e){
			error("Error launching analysis window", e);
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
