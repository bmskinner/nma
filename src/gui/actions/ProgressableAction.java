/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package gui.actions;

import gui.DatasetEvent;
import gui.DatasetEventListener;
import gui.InterfaceEvent;
import gui.InterfaceEvent.InterfaceMethod;
import gui.InterfaceEventListener;
import gui.LogPanel;
import gui.DatasetEvent.DatasetMethod;
import gui.MainWindow;
import io.PopulationExporter;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import analysis.AnalysisDataset;
import analysis.AnalysisWorker;

/**
 * Contains a progress bar and handling methods for when an action
 * is triggered as a SwingWorker. Subclassed for each action type.
 *
 */
abstract class ProgressableAction implements PropertyChangeListener {

	protected AnalysisDataset dataset = null; // the dataset being worked on
	private JProgressBar progressBar = null;
	
	protected AnalysisWorker worker = null;
	protected Integer downFlag = 0; // store flags to tell the action what to do after finishing
	private LogPanel logPanel;
	protected Logger programLogger;
	protected MainWindow mw;
	private CountDownLatch latch = null; // allow threads to wait for the analysis to complete
	
	private List<AnalysisDataset> processList = new ArrayList<AnalysisDataset>(0); // list of datasets that need processing after this
	
	private List<Object> interfaceListeners = new ArrayList<Object>();
	private List<Object> datasetListeners = new ArrayList<Object>();
	
	/**
	 * Constructor with no datasets - used for new analysis
	 * @param barMessage
	 * @param mw
	 */
	protected ProgressableAction(String barMessage, MainWindow mw){
		this.dataset 		= null;
		this.progressBar 	= new JProgressBar(0, 100);
		this.progressBar.setString(barMessage);
		this.progressBar.setStringPainted(true);

		this.mw 			= mw;
		this.logPanel 		= mw.getLogPanel();
		this.programLogger 	= mw.getProgramLogger();
		
		logPanel.addProgressBar(this.progressBar);
		logPanel.revalidate();
		logPanel.repaint();

		this.addInterfaceEventListener(mw);
		this.addDatasetEventListener(mw);
		log(Level.FINEST, "Created progressable action");

	}
	
	public ProgressableAction(AnalysisDataset dataset, String barMessage, MainWindow mw){
		
		this.dataset 		= dataset;
		this.progressBar 	= new JProgressBar(0, 100);
		this.progressBar.setString(barMessage);
		this.progressBar.setStringPainted(true);

		this.mw 			= mw;
		this.logPanel 		= mw.getLogPanel();
		this.programLogger 	= mw.getProgramLogger();
		
		logPanel.addProgressBar(this.progressBar);
		logPanel.revalidate();
		logPanel.repaint();

		this.addInterfaceEventListener(mw);
		this.addDatasetEventListener(mw);
		log(Level.FINEST, "Created progressable action");

	}
	
	/**
	 * Construct using a list of datasets to be processed. The first is analysed, and the rest stored.
	 * @param list
	 * @param barMessage
	 * @param mw
	 */
	public ProgressableAction(List<AnalysisDataset> list, String barMessage, MainWindow mw){
		this(list.get(0), barMessage, mw);
		processList = list;
		processList.remove(0); // remove the first entry
	}
	
	/**
	 * Construct using a list of datasets to be processed. The first is analysed, and the rest stored.
	 * @param list
	 * @param barMessage
	 * @param mw
	 * @param flag
	 */
	public ProgressableAction(List<AnalysisDataset> list, String barMessage, MainWindow mw, int flag){
		this(list, barMessage, mw);
		this.downFlag = flag;
	}
	
	/**
	 * Constructor including a flag for downstream analyses to be carried out
	 * @param dataset
	 * @param barMessage
	 * @param mw
	 * @param flag
	 */
	public ProgressableAction(AnalysisDataset dataset, String barMessage, MainWindow mw, int flag){
		this(dataset, barMessage, mw);
		this.downFlag = flag;
	}
	
	protected void log(Level level, String message){
		programLogger.log(level, message);
	}
	
	protected void setLatch(CountDownLatch latch){
		this.latch = latch;
	}
	
	protected void countdownLatch(){
		if(latch!=null){
			latch.countDown();
		}
	}
	
	protected List<AnalysisDataset> getRemainingDatasetsToProcess(){
		return this.processList;
	}
	
	protected boolean hasRemainingDatasetsToProcess(){
		if(this.processList.size()>0){
			return true;
		} else {
			return false;
		}
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
	
	/**
	 * Remove the progress bar and dataset and interface listeners 
	 */
	public void cancel(){
		removeProgressBar();
		removeDatasetEventListener(mw);
		removeInterfaceEventListener(mw);
	}
	
	protected void setProgressBarVisible(boolean b){
		this.progressBar.setVisible(b);
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
			log(Level.WARNING, "Error in worker");
			removeProgressBar();
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

		log(Level.FINEST, "Firing update populations");
		fireInterfaceEvent(InterfaceMethod.UPDATE_POPULATIONS);
//		log(Level.FINEST, "Firing save root");
//		fireInterfaceEvent(InterfaceMethod.SAVE_ROOT);
		
		
		List<AnalysisDataset> list = new ArrayList<AnalysisDataset>(0);
		list.add(dataset);

		log(Level.FINEST, "Firing update panels");
		fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
		
		log(Level.FINEST, "Firing select datasets");
		fireDatasetEvent(DatasetMethod.SELECT_DATASETS, list);
		
		log(Level.FINEST, "Firing recache charts");
		fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
		
		log(Level.FINEST, "Removing event listeners from action");
		this.removeInterfaceEventListener(mw);
		this.removeDatasetEventListener(mw);
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
	}
	
	protected synchronized void fireInterfaceEvent(InterfaceMethod method) {
    	
        InterfaceEvent event = new InterfaceEvent( this, method, this.getClass().getSimpleName());
        Iterator<Object> iterator = interfaceListeners.iterator();
        while( iterator.hasNext() ) {
            ( (InterfaceEventListener) iterator.next() ).interfaceEventReceived( event );
        }
    }	
	
	protected synchronized void fireDatasetEvent(DatasetMethod method, List<AnalysisDataset> list) {
    	
        DatasetEvent event = new DatasetEvent( this, method, this.getClass().getSimpleName(), list);
        Iterator<Object> iterator = datasetListeners.iterator();
        while( iterator.hasNext() ) {
            ( (DatasetEventListener) iterator.next() ).datasetEventReceived( event );
        }
    }
	
	protected synchronized void fireDatasetEvent(DatasetMethod method, List<AnalysisDataset> list, AnalysisDataset secondary) {
    	
        DatasetEvent event = new DatasetEvent( this, method, this.getClass().getSimpleName(), list, secondary);
        Iterator<Object> iterator = datasetListeners.iterator();
        while( iterator.hasNext() ) {
            ( (DatasetEventListener) iterator.next() ).datasetEventReceived( event );
        }
    }
	
	public synchronized void addDatasetEventListener( DatasetEventListener l ) {
		datasetListeners.add( l );
	}

	public synchronized void removeDatasetEventListener( DatasetEventListener l ) {
		datasetListeners.remove( l );
	}
	
	public synchronized void addInterfaceEventListener( InterfaceEventListener l ) {
		interfaceListeners.add( l );
	}

	public synchronized void removeInterfaceEventListener( InterfaceEventListener l ) {
		interfaceListeners.remove( l );
	}
}
