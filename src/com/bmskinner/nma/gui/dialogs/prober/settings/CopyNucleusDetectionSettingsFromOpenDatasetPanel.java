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
package com.bmskinner.nma.gui.dialogs.prober.settings;

import java.awt.event.ActionListener;
import java.io.File;
import java.util.Optional;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.gui.components.FileSelector;
import com.bmskinner.nma.io.XMLReader;
import com.bmskinner.nma.io.XMLReader.XMLReadingException;
import com.bmskinner.nma.logging.Loggable;

/**
 * A copy button that allows nuclear detection options to be copied from an open
 * dataset
 * 
 * @author Ben Skinner
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public class CopyNucleusDetectionSettingsFromOpenDatasetPanel extends CopyFromOpenDatasetPanel {

	private static final Logger LOGGER = Logger
			.getLogger(CopyNucleusDetectionSettingsFromOpenDatasetPanel.class.getName());

	/**
	 * Create with an analysis options and the detection options to copy to
	 * 
	 * @param parent the analysis options to copy to
	 * @param op     the analysis options to copy from
	 */
	public CopyNucleusDetectionSettingsFromOpenDatasetPanel(IAnalysisOptions parent,
			HashOptions op) {
		super(parent, op);
	}

	@Override
	protected ActionListener createCopyActionListener() {
		return (e) -> {
			IAnalysisDataset[] nameArray = DatasetListManager.getInstance()
					.getRootDatasets()
					.toArray(new IAnalysisDataset[0]);

			// Choose the dataset the options should come from
			IAnalysisDataset sourceDataset = (IAnalysisDataset) JOptionPane.showInputDialog(null,
					CHOOSE_DATASET_MSG_LBL, CHOOSE_DATASET_TTL_LBL, JOptionPane.QUESTION_MESSAGE,
					null, nameArray,
					nameArray[0]);

			if (sourceDataset != null) {

				LOGGER.fine("Copying options from dataset: " + sourceDataset.getName());

				// Ensure the folder is not overwritten by the new options
				File folder = parent.getNucleusDetectionFolder().get();

				Optional<IAnalysisOptions> sourceOptions = sourceDataset.getAnalysisOptions();
				if (sourceOptions.isPresent()) {
					Optional<HashOptions> srcOptions = sourceOptions.get()
							.getNucleusDetectionOptions();
					options.set(srcOptions.get());
					parent.setRuleSetCollection(sourceOptions.get().getRuleSetCollection());
					parent.setNucleusDetectionFolder(folder);
				}

				fireOptionsChangeEvent();
			}
		};
	}

	@Override
	protected ActionListener createOpenActionListener() {
		return (e) -> {

			File f = FileSelector.chooseOptionsImportFile(null);
			if (f == null)
				return;

			try {
				IAnalysisOptions o = XMLReader.readAnalysisOptions(f);
				options.set(o.getNucleusDetectionOptions().get());
				parent.setRuleSetCollection(o.getRuleSetCollection());
				parent.setAngleWindowProportion(o.getProfileWindowProportion());

				fireOptionsChangeEvent();

			} catch (XMLReadingException | ComponentCreationException e1) {
				LOGGER.log(Loggable.STACK, e1.getMessage(), e1);
			}
		};
	}

}
