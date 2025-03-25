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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisWorker;
import com.bmskinner.nma.analysis.IAnalysisMethod;
import com.bmskinner.nma.analysis.nucleus.ConsensusAveragingMethod;
import com.bmskinner.nma.analysis.nucleus.ConsensusSimilarityMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.core.GlobalOptions;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.ProgressBarAcceptor;

/**
 * Refold the consensus nucleus for the selected dataset using default
 * parameters
 */
public class CreateConsensusAction extends SingleDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(CreateConsensusAction.class.getName());

	private static final @NonNull String PROGRESS_LBL = "Creating consensus";
	private static final int PROGRESS_BAR_LENGTH = 100;

	/**
	 * Refold the given selected dataset
	 */
	public CreateConsensusAction(@NonNull IAnalysisDataset dataset,
			@NonNull final ProgressBarAcceptor acceptor,
			CountDownLatch doneSignal) {
		super(dataset, PROGRESS_LBL, acceptor);
		this.setLatch(doneSignal);
	}

	public CreateConsensusAction(@NonNull List<IAnalysisDataset> list,
			@NonNull final ProgressBarAcceptor acceptor,
			CountDownLatch doneSignal) {
		super(list, PROGRESS_LBL, acceptor);
		this.setLatch(doneSignal);
	}

	@Override
	public void run() {
		this.setProgressBarIndeterminate();

		try {

			IAnalysisMethod m = new ConsensusAveragingMethod(dataset);
			int progressLength = PROGRESS_BAR_LENGTH;
			// The averaging method does not work for nuclei that are round, or have extreme
			// variability.
			// In these cases, or if the program config file has been set to override, use
			// the most similar nucleus to the median

			boolean override = dataset.getAnalysisOptions().get().getRuleSetCollection().getName()
					.equals("Round")
					|| GlobalOptions.getInstance().getBoolean(GlobalOptions.REFOLD_OVERRIDE_KEY);
			if (override) {
				m = new ConsensusSimilarityMethod(dataset);
				progressLength = 2;
			}

			worker = new DefaultAnalysisWorker(m, progressLength);
			worker.addPropertyChangeListener(this);

			this.setProgressMessage(PROGRESS_LBL + ": " + dataset.getName());
			ThreadManager.getInstance().submit(worker);

		} catch (Exception e1) {
			this.cancel();
			LOGGER.warning("Error refolding nucleus");
			LOGGER.log(Level.SEVERE, "Error refolding nucleus", e1);
		}
	}

	@Override
	public void finished() {

		Runnable r = () -> {

			// if no list was provided, or no more entries remain,
			// call the finish
			if (!hasRemainingDatasetsToProcess()) {
				countdownLatch();
				CreateConsensusAction.super.finished();

			} else {
				// otherwise analyse the next item in the list
				cancel(); // remove progress bar
				Runnable task = new CreateConsensusAction(getRemainingDatasetsToProcess(),
						progressAcceptors.get(0),
						getLatch().get());
				task.run();
			}
		};
		new Thread(r).start();
	}

}
