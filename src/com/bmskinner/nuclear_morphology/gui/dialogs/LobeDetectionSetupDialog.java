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
package com.bmskinner.nuclear_morphology.gui.dialogs;

import java.awt.BorderLayout;
import java.util.Optional;
import java.util.logging.Logger;

import javax.swing.JPanel;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.nucleus.LobeDetectionMethod;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions;
import com.bmskinner.nuclear_morphology.components.options.IHoughDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.HoughSettingsPanel;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings.SettingsPanel;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * The setup for lobe detection in neutrophils
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class LobeDetectionSetupDialog extends SubAnalysisSetupDialog {
	
	private static final Logger LOGGER = Logger.getLogger(LobeDetectionSetupDialog.class.getName());

    private static final String DIALOG_TITLE = "Lobe detection options";

    private IHoughDetectionOptions options;

    /**
     * Construct with a main program window to listen for actions, and a dataset
     * to operate on
     * 
     * @param mw
     * @param dataset
     */
    public LobeDetectionSetupDialog(final IAnalysisDataset dataset) {
        super(dataset, DIALOG_TITLE);
        createUI();
        packAndDisplay();

    }

    @Override
    public IAnalysisMethod getMethod() {

    	Optional<IAnalysisOptions> op = getFirstDataset().getAnalysisOptions();
    	if(op.isPresent()){
    		Optional<IDetectionOptions> nOp = op.get().getDetectionOptions(CellularComponent.NUCLEUS);
    		
    		if(nOp.isPresent())
    			nOp.get().setSubOptions(IDetectionSubOptions.HOUGH_OPTIONS, options);
    	}
        return new LobeDetectionMethod(getFirstDataset(), options);
    }
    
    @Override
	public HashOptions getOptions() {
		return options;
	}

    @Override
    protected void createUI() {

        try {
            options = OptionsFactory.makeHoughOptions();
            JPanel contentPanel = new JPanel(new BorderLayout());
            SettingsPanel panel = new HoughSettingsPanel(options);

            contentPanel.add(panel, BorderLayout.CENTER);
            contentPanel.add(createFooter(), BorderLayout.SOUTH);

            this.add(contentPanel, BorderLayout.CENTER);
        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, e.getMessage(), e);
        }
    }

    @Override
    protected void setDefaults() {
        // TODO Auto-generated method stub

    }

}
