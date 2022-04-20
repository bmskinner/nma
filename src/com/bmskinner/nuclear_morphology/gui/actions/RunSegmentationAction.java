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

import com.bmskinner.nuclear_morphology.analysis.AnalysisMethodException;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.events.UserActionEvent;
import com.bmskinner.nuclear_morphology.gui.events.revamp.UIController;
import com.bmskinner.nuclear_morphology.gui.events.revamp.UserActionController;

/**
 * Run segmentation on the given datasets
 * 
 * @author ben
 *
 */
public class RunSegmentationAction extends SingleDatasetResultAction {

	private static final Logger LOGGER = Logger.getLogger(RunSegmentationAction.class.getName());

	private MorphologyAnalysisMode mode = MorphologyAnalysisMode.SEGMENT_FROM_SCRATCH;

	private static final String PROGRESS_LBL = "Segmentation analysis";
	private IAnalysisDataset source = null;

	/**
	 * Carry out a segmentation on a dataset
	 * 
	 * @param dataset  the dataset to work on
	 * @param mode     the type of morphology analysis to carry out
	 * @param downFlag the next analyses to perform
	 */
	public RunSegmentationAction(IAnalysisDataset dataset, MorphologyAnalysisMode mode, int downFlag,
			@NonNull final ProgressBarAcceptor acceptor, CountDownLatch latch) {
		super(dataset, PROGRESS_LBL, acceptor, downFlag);
		this.mode = mode;
		setLatch(latch);
	}

	public RunSegmentationAction(List<IAnalysisDataset> list, MorphologyAnalysisMode mode, int downFlag,
			@NonNull final ProgressBarAcceptor acceptor) {
		super(list, PROGRESS_LBL, acceptor, downFlag);
		this.mode = mode;
	}

	public RunSegmentationAction(List<IAnalysisDataset> list, MorphologyAnalysisMode mode, int downFlag,
			@NonNull final ProgressBarAcceptor acceptor, CountDownLatch latch) {
		super(list, PROGRESS_LBL, acceptor, downFlag);
		this.mode = mode;
		setLatch(latch);
	}

	public RunSegmentationAction(IAnalysisDataset dataset, IAnalysisDataset source, int downFlag,
			@NonNull final ProgressBarAcceptor acceptor, CountDownLatch latch) {
		super(dataset, "Copying morphology to " + dataset.getName(), acceptor);
		this.downFlag = downFlag;
		setLatch(latch);
		this.mode = MorphologyAnalysisMode.COPY_FROM_OTHER_DATASET;
		this.source = source;
	}

	/**
	 * Copy the morphology information from the source dataset to each dataset in a
	 * list
	 * 
	 * @param list
	 * @param source
	 */
	public RunSegmentationAction(List<IAnalysisDataset> list, IAnalysisDataset source, int downFlag,
			@NonNull final ProgressBarAcceptor acceptor, CountDownLatch latch) {
		super(list, PROGRESS_LBL, acceptor);
		this.downFlag = downFlag;
		this.mode = MorphologyAnalysisMode.COPY_FROM_OTHER_DATASET;
		this.source = source;
		setLatch(latch);
	}

	@Override
	public void run() {
		setProgressBarIndeterminate();

		switch (mode) {
		case COPY_FROM_OTHER_DATASET:
			runCopyAnalysis();
			return;
		case SEGMENT_FROM_SCRATCH:
			runNewAnalysis();
			return;
		case APPLY_MEDIAN_TO_NUCLEI:
			runApplyMedianToNuclei();
			return;
		default:
			runNewAnalysis();
			return;
		}
	}

	private void runCopyAnalysis() {

		setProgressMessage("Copying segmentation");
		try {
			IAnalysisMethod m = new DatasetSegmentationMethod(dataset, source.getCollection());
			worker = new DefaultAnalysisWorker(m);
			worker.addPropertyChangeListener(this);
			ThreadManager.getInstance().submit(worker);
		} catch (AnalysisMethodException e) {
			LOGGER.warning(e.getMessage());
			finished();
		}

	}

	private void runApplyMedianToNuclei() {
		runAnalysis("Updating segmentation: " + dataset.getName());
	}

	private void runNewAnalysis() {
		runAnalysis("Segmenting: " + dataset.getName());
	}

	private void runAnalysis(String message) {
		setProgressMessage(message);
		try {
			IAnalysisMethod m = new DatasetSegmentationMethod(dataset, mode);
			worker = new DefaultAnalysisWorker(m);
			worker.addPropertyChangeListener(this);
			ThreadManager.getInstance().submit(worker);
		} catch (AnalysisMethodException e) {
			LOGGER.warning(e.getMessage());
			finished();
		}
	}

	@Override
	public void finished() {

		// ensure the progress bar gets hidden even if it is not removed
		setProgressBarVisible(false);

		Thread thr = new Thread() {
			@Override
			public void run() {

				// if no list was provided, or no more entries remain,
				// call the finish
				if (!hasRemainingDatasetsToProcess()) {
					countdownLatch();
					UIController.getInstance().fireProfilesUpdated(dataset);
					UserActionController.getInstance().userActionEventReceived(
							new UserActionEvent(this, UserActionEvent.SELECT_ONE_DATASET, List.of(dataset)));
					RunSegmentationAction.super.finished();

				} else {

					// otherwise analyse the next item in the list
					cancel(); // remove progress bar

					Runnable task = mode.equals(MorphologyAnalysisMode.COPY_FROM_OTHER_DATASET)
							? new RunSegmentationAction(getRemainingDatasetsToProcess(), source, downFlag,
									progressAcceptors.get(0), getLatch().get())
							: new RunSegmentationAction(getRemainingDatasetsToProcess(), mode, downFlag,
									progressAcceptors.get(0), getLatch().get());

					task.run();
				}
			}
		};
		thr.start();
	}

}
