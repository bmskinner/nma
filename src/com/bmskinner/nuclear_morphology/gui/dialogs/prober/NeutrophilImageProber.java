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

import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IMutableDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.NeutrophilDetectionSettingsPanel;

@SuppressWarnings("serial")
public class NeutrophilImageProber  extends IntegratedImageProber {
	
	private static final String DIALOG_TITLE_BAR_LBL = "Neutrophil detection settings";

	public NeutrophilImageProber(final File folder){
		
		try {

			options = OptionsFactory.makeAnalysisOptions();

			IMutableDetectionOptions nucleusOptions = OptionsFactory.makeNucleusDetectionOptions(folder);
			IMutableDetectionOptions cytoOptions    = OptionsFactory.makeNucleusDetectionOptions(folder);
			
			options.setNucleusType(NucleusType.NEUTROPHIL);
			
			cytoOptions.setMinCirc(0);
			cytoOptions.setMaxCirc(1);
			cytoOptions.setMinSize(500);
			cytoOptions.setMaxSize(5000);
			cytoOptions.getCannyOptions().setFlattenThreshold(130);
			
			nucleusOptions.setMinCirc(0);
			nucleusOptions.setMaxCirc(1);
			nucleusOptions.setMinSize(500);
			nucleusOptions.setMaxSize(1000);
			
			options.setDetectionOptions(IAnalysisOptions.NUCLEUS, nucleusOptions);
			options.setDetectionOptions(IAnalysisOptions.CYTOPLASM, cytoOptions);

			// make the panel
			optionsSettingsPanel = new NeutrophilDetectionSettingsPanel(options);
			imageProberPanel     = new NeutrophilImageProberPanel(this, cytoOptions, nucleusOptions, ImageSet.NEUTROPHIL_IMAGE_SET);
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
