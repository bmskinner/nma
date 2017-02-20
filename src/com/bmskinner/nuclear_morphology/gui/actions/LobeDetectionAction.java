package com.bmskinner.nuclear_morphology.gui.actions;

import java.util.concurrent.ExecutionException;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.dialogs.LobeDetectionSetupDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.SubAnalysisSetupDialog;

public class LobeDetectionAction extends ProgressableAction {

	private static final String PROGRESS_BAR_LABEL = "Detecting lobes";
	
	public LobeDetectionAction(IAnalysisDataset dataset, MainWindow mw) {
		super(dataset, PROGRESS_BAR_LABEL, mw);
	}
	
	@Override
	public void run(){

		SubAnalysisSetupDialog setup = new LobeDetectionSetupDialog(mw, dataset);

		if(setup.isReadyToRun()){ // if dialog was cancelled, skip
			
			log("Running lobe detection");
			IAnalysisMethod m = setup.getMethod();
			
			int maxProgress = dataset.getCollection().size();
			worker = new DefaultAnalysisWorker(m, maxProgress);
			
			worker.addPropertyChangeListener(this);
			ThreadManager.getInstance().submit(worker);

		} else {
			this.cancel();
		}
		setup.dispose();
	}



	@Override
	public void finished() {

		this.setProgressBarVisible(false);
		
		
		try {
			IAnalysisResult r = worker.get();
	
		} catch (InterruptedException | ExecutionException e) {
			warn("Error in lobe detection");
			stack(e.getMessage(), e);
		}
		
//		fireDatasetEvent(DatasetEvent.SAVE, dataset);
		fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
		super.finished();
		

	}
}
