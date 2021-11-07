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
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.Finder;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalFinder;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.SignalDetectionSettingsPanel;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Show the results of a signal detection with given options
 * 
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class SignalImageProber extends IntegratedImageProber {
	
	private static final Logger LOGGER = Logger.getLogger(SignalImageProber.class.getName());

    private static final String DIALOG_TITLE_BAR_LBL = "Signal detection settings";
    private static final String NEW_NAME_LBL         = "Enter a signal group name";

    private final HashOptions options;
    private final IAnalysisDataset dataset;

    /**
     * Create with a dataset (from which nuclei will be drawn) and a folder of
     * images to be analysed
     * 
     * @param dataset the analysis dataset
     * @param folder the folder of images
     */
    public SignalImageProber(@NonNull final IAnalysisDataset dataset, @NonNull final File folder) {
        this.dataset = dataset;
        options = OptionsFactory.makeNuclearSignalOptions(folder).build();
        
        Optional<IAnalysisOptions> op = dataset.getAnalysisOptions();
        if(!op.isPresent())
        	throw new IllegalArgumentException("Dataset has no options");
        
        Optional<HashOptions> nOp = op.get().getDetectionOptions(CellularComponent.NUCLEUS);
        
        if(!nOp.isPresent())
        	throw new IllegalArgumentException("Dataset has no nucleus options");

        options.setDouble(HashOptions.SCALE, nOp.get().getDouble(HashOptions.SCALE));


        try {

            // make the panel
            optionsSettingsPanel = new SignalDetectionSettingsPanel(options);

            Finder<?> finder = new SignalFinder(op.get(), options, dataset.getCollection());
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

    public HashOptions getOptions() {
        return options;
    }

    @Override
    protected void okButtonClicked() {

        String name = getGroupName();
        UUID id = UUID.randomUUID();
        
        options.setString(HashOptions.SIGNAL_GROUP_NAME, name);
        options.setString(HashOptions.SIGNAL_GROUP_ID, id.toString());
    }

    /**
     * Get the name of a signal group If blank, requests a new name
     * 
     * @return a valid name
     */
    private String getGroupName() {
		try {
			return inputSupplier.requestString(NEW_NAME_LBL);
		} catch (RequestCancelledException e) {
			return getGroupName(); // no, you will provide a name
		}
    }

}
