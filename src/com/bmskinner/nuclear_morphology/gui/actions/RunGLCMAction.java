package com.bmskinner.nuclear_morphology.gui.actions;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.image.GLCMCalculationMethod;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;

/**
 * Trigger a GLCM calculation
 * @author Ben Skinner
 *
 */
public class RunGLCMAction extends SingleDatasetResultAction {
	
	private static final Logger LOGGER = Logger.getLogger(RunGLCMAction.class.getName());
	
	private static final @NonNull String PROGRESS_BAR_LABEL = "Calculating GLCM";
	
	public RunGLCMAction(@NonNull List<IAnalysisDataset> datasets, @NonNull CountDownLatch latch, @NonNull ProgressBarAcceptor acceptor, @NonNull EventHandler eh) {
		super(datasets, PROGRESS_BAR_LABEL, acceptor, eh);
		this.setLatch(latch);
	}
	
	public RunGLCMAction(@NonNull IAnalysisDataset dataset, int noFlag, @NonNull ProgressBarAcceptor acceptor, @NonNull EventHandler eh) {
		super(dataset, dataset.getName()+": "+PROGRESS_BAR_LABEL, acceptor, eh);
	}
	
	

	@Override
	public void run() {
		IAnalysisMethod m = new GLCMCalculationMethod(dataset);
        worker = new DefaultAnalysisWorker(m, dataset.getCollection().size());
        worker.addPropertyChangeListener(this);
        ThreadManager.getInstance().submit(worker);
	}
	
	@Override
    public void finished() {
        getDatasetEventHandler().fireDatasetEvent(DatasetEvent.RECACHE_CHARTS, dataset);
        
        if (!hasRemainingDatasetsToProcess()) {
            super.finished();
            getInterfaceEventHandler().removeListener(eh);
            getDatasetEventHandler().removeListener(eh);
            countdownLatch();

        } else {
            // otherwise analyse the next item in the list
            cancel(); // remove progress bar
            new RunGLCMAction(getRemainingDatasetsToProcess(), getLatch().get(), progressAcceptors.get(0), eh).run();

        }

    }
}
