/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.gui.dialogs.prober;

import java.awt.BorderLayout;
import java.io.File;
import java.util.Collection;
import java.util.logging.Logger;

import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.Finder;
import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.FluorescentNucleusFinder;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.ConstructableSettingsPanel;
//import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.NucleusDetectionSettingsPanel;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * An image prober for detecting nuclei
 * 
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class NucleusImageProber extends IntegratedImageProber {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private static final String DIALOG_TITLE_BAR_LBL = "Nucleus detection settings";

    /**
     * Construct with a folder of images to probe, and the initial options
     * 
     * @param folder
     * @param o
     */
    public NucleusImageProber(final File folder, final IAnalysisOptions o) {

        try {
            this.options = o;

            optionsSettingsPanel = new ConstructableSettingsPanel(options)
                    .addCopyFromOpenPanel(IAnalysisOptions.NUCLEUS)
                    .addImageChannelPanel(IAnalysisOptions.NUCLEUS)
                    .addImageProcessingPanel(IAnalysisOptions.NUCLEUS)
                    .addEdgeThresholdSwitchPanel(IAnalysisOptions.NUCLEUS)
                    .addSizePanel(IAnalysisOptions.NUCLEUS)
                    .addNucleusProfilePanel(IAnalysisOptions.NUCLEUS)
                    .build();
            optionsSettingsPanel.setEnabled(false);

            Finder<Collection<ICell>> finder = new FluorescentNucleusFinder(options);
            imageProberPanel = new GenericImageProberPanel(folder, finder, this);

            JPanel footerPanel = createFooter();

            this.add(optionsSettingsPanel, BorderLayout.WEST);
            this.add(imageProberPanel, BorderLayout.CENTER);
            this.add(footerPanel, BorderLayout.SOUTH);

            this.setTitle(DIALOG_TITLE_BAR_LBL);

            optionsSettingsPanel.addProberReloadEventListener(imageProberPanel);
            imageProberPanel.addPanelUpdatingEventListener(optionsSettingsPanel);

        } catch (Exception e) {
            LOGGER.warning("Error launching analysis window");
            LOGGER.log(Loggable.STACK, e.getMessage(), e);
            this.dispose();
        }

        this.pack();
        this.setModal(true);
        this.setLocationRelativeTo(null); // centre on screen
        this.setVisible(true);
    }

    public IAnalysisOptions getOptions() {
        return options;
    }

    @Override
    protected void okButtonClicked() {
        // no other action here

    }

}
