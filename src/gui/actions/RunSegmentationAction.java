package gui.actions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import javax.swing.SwingUtilities;

import analysis.AnalysisDataset;
import analysis.nucleus.DatasetSegmenter;
import analysis.nucleus.DatasetSegmenter.MorphologyAnalysisMode;
import gui.MainWindow;
import gui.DatasetEvent.DatasetMethod;
import io.CompositeExporter;
import io.NucleusAnnotator;
import io.StatsExporter;

public class RunSegmentationAction extends ProgressableAction {
	
	private MorphologyAnalysisMode mode = MorphologyAnalysisMode.NEW;
	
	private AnalysisDataset source 			= null;
	
	/**
	 * Carry out a segmentation on a dataset
	 * @param dataset the dataset to work on 
	 * @param mode the type of morphology analysis to carry out
	 * @param downFlag the next analyses to perform
	 */
	public RunSegmentationAction(AnalysisDataset dataset, MorphologyAnalysisMode mode, int downFlag, MainWindow mw){
		super(dataset, "Segmentation analysis", mw, downFlag);
		this.mode = mode;
		programLogger.log(Level.FINE, "Creating segmentation analysis");
		runNewAnalysis();
	}
	
	public RunSegmentationAction(List<AnalysisDataset> list, MorphologyAnalysisMode mode, int downFlag, MainWindow mw){
		super(list, "Segmentation analysis", mw, downFlag);
		this.mode = mode;
		programLogger.log(Level.FINE, "Creating segmentation analysis");
		runNewAnalysis();
	}
	
	
	public RunSegmentationAction(AnalysisDataset dataset, AnalysisDataset source, Integer downFlag, MainWindow mw){
		super(dataset, "Copying morphology to "+dataset.getName(), mw);

		this.mode = MorphologyAnalysisMode.COPY;
		this.source = source;
		
		runCopyAnalysis();
	}
	
	/**
	 * Copy the morphology information from the source dataset to each dataset in a list
	 * @param list
	 * @param source
	 */
	public RunSegmentationAction(List<AnalysisDataset> list, AnalysisDataset source, Integer downFlag, MainWindow mw){
		super(list, "Segmentation analysis", mw);
		this.mode = MorphologyAnalysisMode.COPY;
		this.source = source;

		runCopyAnalysis();
	}
	
	private void runCopyAnalysis(){
		worker = new DatasetSegmenter(dataset, source.getCollection(), programLogger);
		worker.addPropertyChangeListener(this);
		worker.execute();
	}
	
	private void runNewAnalysis(){
		try{
			String message = null;
			switch (this.mode) {
			case COPY:  message = "Copying morphology";
			break;

			case REFRESH: message = "Refreshing morphology";
			break;

			default: message = "Morphology analysis: "+dataset.getName();
			break;  
			}

			this.setProgressMessage(message);
			this.cooldown();

			worker = new DatasetSegmenter(this.dataset, mode, programLogger);
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
		// See specific comment below
		Thread thr = new Thread(){

			public void run(){

				if(  (downFlag & MainWindow.STATS_EXPORT) == MainWindow.STATS_EXPORT){

					programLogger.log(Level.INFO, "Exporting stats");
					boolean ok = StatsExporter.run(dataset);
					if(ok){
						programLogger.log(Level.INFO, "Exporting stats OK");
					} else {
						programLogger.log(Level.INFO, "Exporting stats error");
					}
				}

				// annotate the nuclei in the population
				if(  (downFlag & MainWindow.NUCLEUS_ANNOTATE) == MainWindow.NUCLEUS_ANNOTATE){

					programLogger.log(Level.INFO, "Annotating nuclei...");
					boolean ok = NucleusAnnotator.run(dataset);
					if(ok){
						programLogger.log(Level.INFO, "Annotating nuclei OK");
					} else {
						programLogger.log(Level.INFO, "Annotating nuclei error");
					}
				}

				// make a composite image of all nuclei in the collection
				if(  (downFlag & MainWindow.EXPORT_COMPOSITE) == MainWindow.EXPORT_COMPOSITE){
					if(dataset.getCollection().getNucleusCount()<CompositeExporter.MAX_COMPOSITABLE_NUCLEI){
						programLogger.log(Level.INFO, "Exporting composite...");
						boolean ok = CompositeExporter.run(dataset);
						if(ok){
							programLogger.log(Level.INFO, "Exporting composite OK");
						} else {
							programLogger.log(Level.INFO, "Exporting composite error");
						}
					}
				}

				// The new refold action is a progressable action, so must not block
				// the EDT. Also, the current action must wait for refolding to complete,
				// otherwise the next MorphologyAnalysisAction in the chain will block the
				// refold from firing a done signal. Hence, put a latch on the refold to 
				// make this thread wait until the refolding is complete.
				if(  (downFlag & MainWindow.CURVE_REFOLD) == MainWindow.CURVE_REFOLD){
					programLogger.log(Level.FINEST, "Preparing to hold thread while refolding datast");
					final CountDownLatch latch = new CountDownLatch(1);
					new RefoldNucleusAction(dataset, mw, latch);
					try {
						latch.await();
					} catch (InterruptedException e) {
						programLogger.log(Level.SEVERE, "Interruption to thread", e);
					}
					programLogger.log(Level.FINEST, "Resuming thread after refolding datast");
				}

				if(  (downFlag & MainWindow.SAVE_DATASET) == MainWindow.SAVE_DATASET){
					final CountDownLatch latch = new CountDownLatch(1);
					programLogger.log(Level.FINEST, "Preparing to hold thread while saving datast");
					new SaveDatasetAction(dataset, mw, latch, false);
					try {
						latch.await();
					} catch (InterruptedException e) {
						programLogger.log(Level.SEVERE, "Interruption to thread", e);
					}
					programLogger.log(Level.FINEST, "Resuming thread after saving datast");
				}

				if(  (downFlag & MainWindow.ADD_POPULATION) == MainWindow.ADD_POPULATION){
					List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
					list.add(dataset);
					fireDatasetEvent(DatasetMethod.ADD_DATASET, list);
				}

				// if no list was provided, or no more entries remain,
				// call the finish
				if( ! hasRemainingDatasetsToProcess()){

					RunSegmentationAction.super.finished();
					
				} else {
					// otherwise analyse the next item in the list
					cancel(); // remove progress bar

					SwingUtilities.invokeLater(new Runnable(){
						public void run(){

							if(mode.equals(MorphologyAnalysisMode.COPY)){

								new RunSegmentationAction(getRemainingDatasetsToProcess(), source, downFlag, mw);
								
							} else {
								
								new RunSegmentationAction(getRemainingDatasetsToProcess(), mode, downFlag, mw);
							}
						}});

				}			
			}
		};
		thr.start();

	}

}
