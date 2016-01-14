package gui.actions;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import analysis.AnalysisDataset;
import analysis.nucleus.DatasetProfiler;
import analysis.nucleus.DatasetSegmenter.MorphologyAnalysisMode;
import gui.MainWindow;

public class RunProfilingAction extends ProgressableAction {
	
	public RunProfilingAction(AnalysisDataset dataset, int downFlag, MainWindow mw){
		super(dataset, "Segmentation analysis", mw, downFlag);

		programLogger.log(Level.FINE, "Creating profiling analysis");
		runNewAnalysis();
	}
	
	public RunProfilingAction(List<AnalysisDataset> list, int downFlag, MainWindow mw){
		super(list, "Segmentation analysis", mw, downFlag);
		programLogger.log(Level.FINE, "Creating profiling analysis");
		runNewAnalysis();
	}
	
	public RunProfilingAction(AnalysisDataset dataset, int downFlag, MainWindow mw, CountDownLatch latch){
		super(dataset, "Segmentation analysis", mw, downFlag);
		this.setLatch(latch);
		programLogger.log(Level.FINE, "Creating profiling analysis");
		runNewAnalysis();
		
	}
	
	public RunProfilingAction(List<AnalysisDataset> list, int downFlag, MainWindow mw, CountDownLatch latch){
		super(list, "Segmentation analysis", mw, downFlag);
		this.setLatch(latch);
		programLogger.log(Level.FINE, "Creating profiling analysis");
		runNewAnalysis();
		
	}
	
	private void runNewAnalysis(){
		try{
			String message = "Profiling analysis: "+dataset.getName();
		

			this.setProgressMessage(message);
			this.cooldown();

			worker = new DatasetProfiler(this.dataset, programLogger);
			worker.addPropertyChangeListener(this);
			programLogger.log(Level.FINE, "Running morphology analysis");
			worker.execute();
		} catch(Exception e){
			this.cancel();
			programLogger.log(Level.SEVERE, "Error in morphology analysis", e);
		}
	}
	
	@Override
	public void finished() {

		// ensure the progress bar gets hidden even if it is not removed
		this.setProgressBarVisible(false);

		// The analysis takes place in a new thread to accomodate refolding.
		// See specific comment in RunSegmentationAction
		Thread thr = new Thread(){

			public void run(){

				if(  (downFlag & MainWindow.ASSIGN_SEGMENTS) == MainWindow.ASSIGN_SEGMENTS){
					
					new RunSegmentationAction(dataset, MorphologyAnalysisMode.NEW, downFlag, mw);
					
				}


				// if no list was provided, or no more entries remain,
				// call the finish
				if( ! hasRemainingDatasetsToProcess()){

					cancel();		
					RunProfilingAction.this.removeInterfaceEventListener(mw);
					RunProfilingAction.this.removeDatasetEventListener(mw);					
//					
					RunProfilingAction.this.countdownLatch();
					
				} else {
					// otherwise analyse the next item in the list
					cancel(); // remove progress bar

					SwingUtilities.invokeLater(new Runnable(){
						public void run(){

								new RunProfilingAction(getRemainingDatasetsToProcess(), downFlag, mw);
							
						}});

				}			
			}
		};
		thr.start();

	}

}
