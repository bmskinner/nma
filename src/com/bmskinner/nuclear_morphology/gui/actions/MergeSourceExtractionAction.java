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
package com.bmskinner.nuclear_morphology.gui.actions;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.MergeSourceExtractionMethod;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.events.revamp.UIController;
import com.bmskinner.nuclear_morphology.logging.Loggable;

public class MergeSourceExtractionAction extends MultiDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(MergeSourceExtractionAction.class.getName());

	private static final @NonNull String PROGRESS_BAR_LABEL = "Extracting merge source";

	/**
	 * Refold the given selected dataset
	 */
	public MergeSourceExtractionAction(List<IAnalysisDataset> datasets, @NonNull final ProgressBarAcceptor acceptor) {
		super(datasets, PROGRESS_BAR_LABEL, acceptor);
	}

	@Override
	public void run() {
		this.setProgressBarIndeterminate();

		IAnalysisMethod m = new MergeSourceExtractionMethod(datasets);

		worker = new DefaultAnalysisWorker(m);
		worker.addPropertyChangeListener(this);

		this.setProgressMessage(PROGRESS_BAR_LABEL);
		ThreadManager.getInstance().submit(worker);

	}

	@Override
	public void finished() {

		setProgressBarVisible(false);

		try {

			IAnalysisResult r = worker.get();
			UIController.getInstance().fireDatasetAdded(r.getDatasets());

		} catch (InterruptedException e) {
			LOGGER.warning("Unable to extract merge source" + e.getMessage());
			LOGGER.log(Loggable.STACK, "Unable to extract merge source", e);
			return;
		} catch (ExecutionException e) {
			LOGGER.warning("Unable to extract merge source" + e.getMessage());
			LOGGER.log(Loggable.STACK, "Unable to extract merge source", e);
			return;
		}

		super.finished();
	}

}
