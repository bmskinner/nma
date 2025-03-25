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
package com.bmskinner.nma.gui.dialogs.prober;

import java.awt.BorderLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.swing.JPanel;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.detection.Finder;
import com.bmskinner.nma.analysis.detection.FishRemappingFinder;
import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;

@SuppressWarnings("serial")
public class FishRemappingProber extends IntegratedImageProber {

	private static final Logger LOGGER = Logger.getLogger(FishRemappingProber.class.getName());

	private static final String DIALOG_TITLE_BAR_LBL = "Post-FISH mapping";
	private static final String PROCEED_LBL = "Finished selection";

	final IAnalysisDataset dataset;
	final List<IAnalysisDataset> newList = new ArrayList<>();

	/**
	 * Create with a dataset (from which nuclei will be drawn) and a folder of
	 * images to be analysed
	 * 
	 * @param dataset the analysis dataset
	 * @param folder  the folder of images
	 */
	public FishRemappingProber(@NonNull final IAnalysisDataset dataset,
			@NonNull final File fishImageDir) {
		this.dataset = dataset;

		Optional<IAnalysisOptions> analysisOptions = dataset.getAnalysisOptions();
		if (analysisOptions.isPresent()) {
			// make the panel
			Finder<?> finder = new FishRemappingFinder(dataset.getAnalysisOptions().get(),
					fishImageDir);

			try {
				imageProberPanel = new FishRemappingProberPanel(dataset, finder, this);
			} catch (MissingOptionException e) {
				LOGGER.warning("No options in dataset");
				this.dispose();
			}

			imageProberPanel.setSize(imageProberPanel.getPreferredSize());

			JPanel footerPanel = createFooter();
			this.setOkButtonText(PROCEED_LBL);

			this.add(imageProberPanel, BorderLayout.CENTER);
			this.add(footerPanel, BorderLayout.SOUTH);

			this.setTitle(DIALOG_TITLE_BAR_LBL);

			this.pack();
			this.setModal(true);
			this.setLocationRelativeTo(null); // centre on screen
			this.setVisible(true);
		} else {
			this.dispose();
		}
	}

	@Override
	protected void okButtonClicked() {

		List<ICellCollection> subs = ((FishRemappingProberPanel) imageProberPanel)
				.getSubCollections();

		if (subs.isEmpty()) {

			return;
		}

		for (ICellCollection sub : subs) {

			if (sub.hasCells()) {
				try {
					IAnalysisDataset subDataset = dataset.addChildCollection(sub);
					newList.add(subDataset);
				} catch (MissingDataException | SegmentUpdateException e) {
					LOGGER.log(Level.SEVERE, "Error addig new child dataset", e);
				}
			}
		}
	}

	public List<IAnalysisDataset> getNewDatasets() {
		return newList;
	}

}
