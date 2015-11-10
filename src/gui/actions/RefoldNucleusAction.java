package gui.actions;

import gui.LogPanel;
import gui.MainWindow;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import analysis.AnalysisDataset;
import analysis.nucleus.CurveRefolder;
import analysis.nucleus.CurveRefolder.CurveRefoldingMode;

/**
 * Refold the consensus nucleus for the selected dataset using default parameters
 */
public class RefoldNucleusAction extends ProgressableAction {

	/**
	 * Refold the given selected dataset
	 */
	
	public RefoldNucleusAction(AnalysisDataset dataset, MainWindow mw, CountDownLatch doneSignal) {
		super(dataset, "Refolding", "Error refolding nucleus", mw);
		programLogger.log(Level.FINEST, "Created RefoldNucleusAction");
		try{

			this.progressBar.setIndeterminate(true);
			worker = new CurveRefolder(dataset, 
					CurveRefoldingMode.FAST, 
					doneSignal, 
					programLogger);

			worker.addPropertyChangeListener(this);
			this.setProgressMessage("Refolding: "+dataset.getName());
			
			/*
			 * The SwingWorker doInBackground is off the EDT. At this point, the EDT should be free
			 * 
			 * 
			 * What thread is waiting for a signal from the worker?
			 */
			programLogger.log(Level.FINEST, "RefoldNucleusAction init is EDT: "+SwingUtilities.isEventDispatchThread());
			
			worker.execute();
			programLogger.log(Level.FINEST, "Executed CurveRefolder");

		} catch(Exception e1){
			this.cancel();
			programLogger.log(Level.SEVERE, "Error refolding nucleus", e1);
		}
	}
	
	@Override
	public void finished(){

		programLogger.log(Level.FINE, "Refolding finished, cleaning up");
		programLogger.log(Level.FINEST, "RefoldNucleusAction.finished() is EDT: "+SwingUtilities.isEventDispatchThread());
		
		
		// ensure the bar is gone, even if the cleanup fails
		this.progressBar.setVisible(false);
		dataset.getAnalysisOptions().setRefoldNucleus(true);
		dataset.getAnalysisOptions().setRefoldMode("Fast");
		super.finished();
		
	}

}
