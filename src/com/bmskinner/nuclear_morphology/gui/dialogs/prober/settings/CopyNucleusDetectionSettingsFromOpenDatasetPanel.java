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
package com.bmskinner.nuclear_morphology.gui.dialogs.prober.settings;

import java.awt.event.ActionListener;
import java.io.File;
import java.util.Optional;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.gui.components.FileSelector;
import com.bmskinner.nuclear_morphology.io.xml.OptionsXMLReader;
import com.bmskinner.nuclear_morphology.io.xml.XMLReader.XMLReadingException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * A copy button that allows nuclear detection options to be copied from an open
 * dataset
 * 
 * @author ben
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class CopyNucleusDetectionSettingsFromOpenDatasetPanel extends CopyFromOpenDatasetPanel {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    /**
     * Create with an analysis options and the detection options to copy to
     * @param parent
     * @param op
     */
    public CopyNucleusDetectionSettingsFromOpenDatasetPanel(IAnalysisOptions parent, IDetectionOptions op) {
        super(parent, op);
    }
    
	@Override
	protected ActionListener createCopyActionListener() {
		return (e) -> {
			IAnalysisDataset[] nameArray = DatasetListManager.getInstance()
            		.getRootDatasets()
                    .toArray(new IAnalysisDataset[0]);

            IAnalysisDataset sourceDataset = (IAnalysisDataset) JOptionPane.showInputDialog(null,
                    CHOOSE_DATASET_MSG_LBL, CHOOSE_DATASET_TTL_LBL, JOptionPane.QUESTION_MESSAGE, null, nameArray,
                    nameArray[0]);

            if (sourceDataset != null) {

            	LOGGER.fine("Copying options from dataset: " + sourceDataset.getName());

            	// Ensure the folder is not overwritten by the new options
            	File folder = options.getFolder();
            	Optional<IAnalysisOptions> op = sourceDataset.getAnalysisOptions();
            	if(op.isPresent()){
            		Optional<IDetectionOptions> srcOptions = op.get().getDetectionOptions(CellularComponent.NUCLEUS);
            		options.set(srcOptions.get());
            		options.setFolder(folder);
                    parent.setNucleusType(op.get().getNucleusType());
            	}

                fireOptionsChangeEvent();
            }
		};
	}

	@Override
	protected ActionListener createOpenActionListener() {
		return (e) -> {
			File folder = options.getFolder();
			File f = FileSelector.chooseOptionsImportFile(folder);
			if(f==null)
				return;

			try {
				IAnalysisOptions o = new OptionsXMLReader(f).read();
				options.set(o.getDetectionOptions(CellularComponent.NUCLEUS).get());
				parent.setNucleusType(o.getNucleusType());
				parent.setAngleWindowProportion(o.getProfileWindowProportion());
				options.setFolder(folder);
				fireOptionsChangeEvent();

			} catch (XMLReadingException e1) {
				LOGGER.log(Loggable.STACK, e1.getMessage(), e1);
			}
		};
	}

}
