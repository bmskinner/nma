/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.gui.actions;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.main.MainWindow;
import com.bmskinner.nuclear_morphology.main.DatasetListManager;
import com.bmskinner.nuclear_morphology.main.EventHandler;
import com.bmskinner.nuclear_morphology.main.ThreadManager;

/**
 * Run segmentation on the given datasets
 * @author ben
 *
 */
public class RunSegmentationAction extends SingleDatasetResultAction {

    private MorphologyAnalysisMode mode = MorphologyAnalysisMode.NEW;

    private static final String PROGRESS_LBL = "Segmentation analysis";
    private IAnalysisDataset    source       = null;
    private CountDownLatch      latch        = null;

    /**
     * Carry out a segmentation on a dataset
     * 
     * @param dataset
     *            the dataset to work on
     * @param mode
     *            the type of morphology analysis to carry out
     * @param downFlag
     *            the next analyses to perform
     */
    public RunSegmentationAction(IAnalysisDataset dataset, MorphologyAnalysisMode mode, int downFlag, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh,
            CountDownLatch latch) {
        super(dataset, PROGRESS_LBL, acceptor, eh, downFlag);
        this.mode = mode;
        this.latch = latch;
    }

    public RunSegmentationAction(List<IAnalysisDataset> list, MorphologyAnalysisMode mode, int downFlag,
    		@NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
        super(list, PROGRESS_LBL, acceptor, eh, downFlag);
        this.mode = mode;
    }

    public RunSegmentationAction(IAnalysisDataset dataset, IAnalysisDataset source, Integer downFlag, @NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh,
            CountDownLatch latch) {
        super(dataset, "Copying morphology to " + dataset.getName(), acceptor, eh);
        this.downFlag = downFlag;
        this.latch = latch;
        this.mode = MorphologyAnalysisMode.COPY;
        this.source = source;
    }

    /**
     * Copy the morphology information from the source dataset to each dataset
     * in a list
     * 
     * @param list
     * @param source
     */
    public RunSegmentationAction(List<IAnalysisDataset> list, IAnalysisDataset source, Integer downFlag,
    		@NonNull final ProgressBarAcceptor acceptor, @NonNull final EventHandler eh) {
        super(list, PROGRESS_LBL, acceptor, eh);
        this.downFlag = downFlag;
        this.mode = MorphologyAnalysisMode.COPY;
        this.source = source;
    }

    @Override
    public void run() {

        setProgressBarIndeterminate();

        switch (mode) {
        case NEW: {
            runNewAnalysis();
            break;
        }

        case COPY: {
            runCopyAnalysis();
            break;
        }

        case REFRESH: {
            runRefreshAnalysis();
            break;
        }

        default: {
            runNewAnalysis();
            break;
        }
        }
    }

    private void runCopyAnalysis() {

        setProgressMessage("Copying segmentation");
        IAnalysisMethod m = new DatasetSegmentationMethod(dataset, source.getCollection());
        worker = new DefaultAnalysisWorker(m);
        worker.addPropertyChangeListener(this);
        ThreadManager.getInstance().submit(worker);
    }

    private void runRefreshAnalysis() {
    	runAnalysis("Refreshing segmentation");
    }

    private void runNewAnalysis() {
    	runAnalysis("Segmenting: " + dataset.getName());
    }
    
    private void runAnalysis(String message){
    	setProgressMessage(message);
        IAnalysisMethod m = new DatasetSegmentationMethod(dataset, mode);
        worker = new DefaultAnalysisWorker(m);
        worker.addPropertyChangeListener(this);
        ThreadManager.getInstance().submit(worker);
    }

    @Override
    public void finished() {

        // ensure the progress bar gets hidden even if it is not removed
        setProgressBarVisible(false);

        // The analysis takes place in a new thread to accomodate refolding.
        // See specific comment below
        Thread thr = new Thread() {

            public void run() {

                /*
                 * When refreshing segmentation, the orientation point may have
                 * changed. Update the vertical orientation nuclei for the
                 * dataset. Also force the consensus nucleus to be refolded at
                 * the next step.
                 */
                if (mode.equals(MorphologyAnalysisMode.REFRESH)) {
                    dataset.getCollection().updateVerticalNuclei();

                    // if(dataset.getCollection().hasConsensusNucleus()){
                    // downFlag |= MainWindow.CURVE_REFOLD;
                    // }
                }

                /*
                 * The refold action is a progressable action, so must not block
                 * the EDT. Also, the current action must wait for refolding to
                 * complete, otherwise the next RunSegmentationAction in the
                 * chain will block the refold from firing a done signal.
                 * 
                 * Hence, put a latch on the refold to make this thread wait
                 * until the refolding is complete.
                 */
                if ((downFlag & CURVE_REFOLD) == CURVE_REFOLD) {
                    finest("Preparing to hold thread while refolding datast");
                    final CountDownLatch latch = new CountDownLatch(1);
                    Runnable r = new RefoldNucleusAction(dataset, progressAcceptors.get(0), eh, latch);
                    r.run();
                    try {
                        latch.await();
                    } catch (InterruptedException e) {
                        error("Interruption to thread", e);
                    }
                    finest("Resuming thread after refolding datast");
                }

                /*
                 * Recache charts if the dataset exists
                 */
                finer("Firing clear cache event");
                getDatasetEventHandler().fireDatasetEvent(DatasetEvent.CLEAR_CACHE, dataset);

                /*
                 * Ideally, wait for the charts to clear before firing the
                 * selection request
                 */

                /*
                 * Save the dataset, regardless of flags
                 */
                finer("Saving the dataset");
                getDatasetEventHandler().fireDatasetEvent(DatasetEvent.SAVE, dataset);

                if ((downFlag & ADD_POPULATION) == ADD_POPULATION) {
                    finest("Adding dataset to list manager");
                    DatasetListManager.getInstance().addDataset(dataset);
                    finest("Firing add dataset signal");
                    getDatasetEventHandler().fireDatasetEvent(DatasetEvent.ADD_DATASET, dataset);

                }

                // if no list was provided, or no more entries remain,
                // call the finish
                if (!hasRemainingDatasetsToProcess()) {
                    finest("No more datasets remain to process");
                    if (latch != null) {
                        latch.countDown();
                    }
                    // finer("Firing refresh cache event");
                    // fireDatasetEvent(DatasetMethod.REFRESH_CACHE, dataset);

                    finer("Firing select dataset event");
                    getDatasetEventHandler().fireDatasetEvent(DatasetEvent.SELECT_ONE_DATASET, dataset);

                    RunSegmentationAction.super.finished();

                } else {

                    // otherwise analyse the next item in the list
                    cancel(); // remove progress bar

                    Runnable task = mode.equals(MorphologyAnalysisMode.COPY)
                            ? new RunSegmentationAction(getRemainingDatasetsToProcess(), source, downFlag, progressAcceptors.get(0), eh)
                            : new RunSegmentationAction(getRemainingDatasetsToProcess(), mode, downFlag, progressAcceptors.get(0), eh);

                    task.run();
                }
            }
        };
        thr.start();
    }

}
