package gui.actions;

import gui.DatasetEvent;
import gui.DatasetEventListener;
import gui.InterfaceEvent;
import gui.InterfaceEvent.InterfaceMethod;
import gui.InterfaceEventListener;
import gui.LogPanel;
import gui.DatasetEvent.DatasetMethod;
import io.PopulationExporter;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import analysis.AnalysisDataset;

/**
 * Contains a progress bar and handling methods for when an action
 * is triggered as a SwingWorker. Subclassed for each action type.
 *
 */
abstract class ProgressableAction implements PropertyChangeListener {

	protected AnalysisDataset dataset = null; // the dataset being worked on
	protected JProgressBar progressBar = null;
	protected String errorMessage = null;
	protected SwingWorker<Boolean, Integer> worker;
	protected Integer downFlag = 0; // store flags to tell the action what to do after finishing
	protected LogPanel logPanel;
	protected Logger programLogger;
	
	private List<Object> interfaceListeners = new ArrayList<Object>();
	private List<Object> datasetListeners = new ArrayList<Object>();
	
	public ProgressableAction(AnalysisDataset dataset, String barMessage, String errorMessage, LogPanel logPanel, Logger programLogger){
		
		this.errorMessage 	= errorMessage;
		this.dataset 		= dataset;
		this.progressBar 	= new JProgressBar(0, 100);
		this.progressBar.setString(barMessage);
		this.progressBar.setStringPainted(true);
		this.logPanel = logPanel;
		this.programLogger = programLogger;
		
		logPanel.addProgressBar(this.progressBar);
		logPanel.revalidate();
		logPanel.repaint();
//		contentPane.revalidate();
//		contentPane.repaint();

	}
	
	public ProgressableAction(AnalysisDataset dataset, String barMessage, String errorMessage, LogPanel logPanel, Logger programLogger, int flag){
		this(dataset, barMessage, errorMessage, logPanel, programLogger);
		this.downFlag = flag;
	}
	
	/**
	 * Change the progress message from the default in the constructor
	 * @param messsage the string to display
	 */
	public void setProgressMessage(String messsage){
		this.progressBar.setString(messsage);
	}
	
	private void removeProgressBar(){
		logPanel.removeProgressBar(this.progressBar);
		logPanel.revalidate();
		logPanel.repaint();
	}
	
	public void cancel(){
		removeProgressBar();
	}
	
	/**
	 * Use to manually remove the progress bar after an action is complete
	 */
	public void cleanup(){
		if (this.worker.isDone() || this.worker.isCancelled()){
			this.removeProgressBar();
		}
	}
	
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) {

		int value = (Integer) evt.getNewValue(); // should be percent
//		IJ.log("Property change: "+value);
		
		if(value >=0 && value <=100){
			
			if(this.progressBar.isIndeterminate()){
				this.progressBar.setIndeterminate(false);
			}
			this.progressBar.setValue(value);
		}

		if(evt.getPropertyName().equals("Finished")){
			programLogger.log(Level.FINEST,"Worker signaled finished");
			finished();
		}

		if(evt.getPropertyName().equals("Error")){
			programLogger.log(Level.FINEST,"Worker signaled error");
			error();
		}
		
		if(evt.getPropertyName().equals("Cooldown")){
			programLogger.log(Level.FINEST,"Worker signaled cooldown");
			cooldown();
		}
		
	}
	
	/**
	 * The method run when the analysis has completed
	 */
	public void finished(){
		removeProgressBar();

		fireInterfaceEvent(InterfaceMethod.UPDATE_POPULATIONS);
//		populationsPanel.update(); // get any new populations
		
//		for(AnalysisDataset root : populationsPanel.getRootDatasets()){
//			PopulationExporter.saveAnalysisDataset(root);
//		}
		fireInterfaceEvent(InterfaceMethod.SAVE_ROOT);
		
		
//		List<AnalysisDataset> list = new ArrayList<AnalysisDataset>(0);
//		list.add(dataset);
//		
//		
//		populationsPanel.selectDataset(dataset);
//		updatePanels(list); // update with the current population
		fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
	}
	
	/**
	 * Runs when an error was encountered in the analysis
	 */
	public void error(){
		programLogger.log(Level.SEVERE, this.errorMessage);
//		log(this.errorMessage);
		removeProgressBar();
	}
	
	/**
	 * Runs if a cooldown signal is received. Use to set progress bars
	 * to an indeterminate state when no reliable progress metric is 
	 * available
	 */
	public void cooldown(){
		this.progressBar.setIndeterminate(true);
		logPanel.revalidate();
		logPanel.repaint();
//		contentPane.revalidate();
//		contentPane.repaint();
	}
	
	protected synchronized void fireInterfaceEvent(InterfaceMethod method) {
    	
        InterfaceEvent event = new InterfaceEvent( this, method, this.getClass().getSimpleName());
        Iterator<Object> iterator = interfaceListeners.iterator();
        while( iterator.hasNext() ) {
            ( (InterfaceEventListener) iterator.next() ).interfaceEventReceived( event );
        }
    }	
}
