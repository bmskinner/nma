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
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.events.revamp.UIController;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Launch a profiling action that will create object profiles, and automatically
 * detect tags using the built-in rules
 * 
 * @author ben
 *
 */
public class RunProfilingAction extends SingleDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(RunProfilingAction.class.getName());

	private static final @NonNull String PROGRESS_BAR_LABEL = "Profiling";

	public RunProfilingAction(@NonNull final IAnalysisDataset dataset, int downFlag,
			@NonNull final ProgressBarAcceptor acceptor) {
		super(dataset, PROGRESS_BAR_LABEL, acceptor, downFlag);
	}

	public RunProfilingAction(@NonNull final List<IAnalysisDataset> list, int downFlag,
			@NonNull final ProgressBarAcceptor acceptor) {
		super(list, PROGRESS_BAR_LABEL, acceptor, downFlag);
	}

	public RunProfilingAction(@NonNull final IAnalysisDataset dataset, int downFlag,
			@NonNull final ProgressBarAcceptor acceptor, CountDownLatch latch) {
		super(dataset, PROGRESS_BAR_LABEL, acceptor, downFlag);
		this.setLatch(latch);

	}

	public RunProfilingAction(@NonNull final List<IAnalysisDataset> list, int downFlag,
			@NonNull final ProgressBarAcceptor acceptor, CountDownLatch latch) {
		super(list, PROGRESS_BAR_LABEL, acceptor, downFlag);
		this.setLatch(latch);

	}

	@Override
	public void run() {
		runNewAnalysis();
	}

	private void runNewAnalysis() {
		try {

			this.setProgressMessage("Profiling: " + dataset.getName());
			IAnalysisMethod method = new DatasetProfilingMethod(dataset);
			worker = new DefaultAnalysisWorker(method);

			worker.addPropertyChangeListener(this);

			ThreadManager.getInstance().submit(worker);
		} catch (Exception e) {
			this.cancel();
			LOGGER.log(Loggable.STACK, "Error in morphology analysis", e);
		}
	}

	@Override
	public void finished() {

		// ensure the progress bar gets hidden even if it is not removed
		this.setProgressBarVisible(false);

		Runnable task = () -> {

			if ((downFlag & ADD_POPULATION) == ADD_POPULATION) {
				DatasetListManager.getInstance().addDataset(dataset);
				UIController.getInstance().fireDatasetAdded(dataset);

			}

			// if no list was provided, or no more entries remain,
			// call the finish
			if (!hasRemainingDatasetsToProcess()) {

				cancel();
				countdownLatch();

			} else {
				// otherwise analyse the next item in the list
				cancel(); // remove progress bar

				Runnable p = new RunProfilingAction(getRemainingDatasetsToProcess(), downFlag, progressAcceptors.get(0),
						getLatch().get());
				p.run();

			}
		};

		ThreadManager.getInstance().execute(task);

	}

}
