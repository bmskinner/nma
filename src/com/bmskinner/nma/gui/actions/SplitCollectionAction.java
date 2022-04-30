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
package com.bmskinner.nma.gui.actions;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.cells.ICell;
import com.bmskinner.nma.components.datasets.DefaultCellCollection;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.logging.Loggable;

public class SplitCollectionAction extends SingleDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(SplitCollectionAction.class.getName());

	private static final @NonNull String PROGRESS_BAR_LABEL = "Splitting collection";

	public SplitCollectionAction(IAnalysisDataset dataset, @NonNull final ProgressBarAcceptor acceptor) {
		super(dataset, PROGRESS_BAR_LABEL, acceptor);
	}

	@Override
	public void run() {
		try {

			if (dataset.hasChildren()) {
				LOGGER.info("Splitting collection...");

				IAnalysisDataset[] names = dataset.getAllChildDatasets().toArray(new IAnalysisDataset[0]);

				IAnalysisDataset negative = (IAnalysisDataset) JOptionPane.showInputDialog(null,
						"Give me nuclei that are NOT present within the following population", "Split population",
						JOptionPane.PLAIN_MESSAGE, null, names, names[0]);

				if (negative != null) {

					// prepare a new collection
					ICellCollection collection = dataset.getCollection();

					ICellCollection newCollection = new DefaultCellCollection(dataset, "Subtraction");

					for (ICell c : collection)
						if (!negative.getCollection().contains(c))
							newCollection.add(c.duplicate());

					newCollection.setName("Not_in_" + negative.getName());

					dataset.addChildCollection(newCollection);

					if (newCollection.size() > 0) {

						LOGGER.info("Reapplying morphology...");

						int flag = 0;
						IAnalysisDataset newDataset = dataset.getChildDataset(newCollection.getId());
						final CountDownLatch latch = new CountDownLatch(1);
						new RunSegmentationAction(newDataset, dataset, flag, progressAcceptors.get(0), latch);
					}
				} else {
					LOGGER.fine("User cancelled split");
				}

			} else {
				LOGGER.info("Cannot split; no children in dataset");
			}

		} catch (Exception e1) {
			LOGGER.log(Loggable.STACK, "Error splitting collection", e1);
		} finally {
			cancel();
		}
	}

}
