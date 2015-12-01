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
	protected JProgressBar progressBar = null;
	protected String errorMessage = null;
	protected AnalysisWorker worker;
	protected Integer downFlag = 0; // store flags to tell the action what to do after finishing
	protected LogPanel logPanel;
	protected Logger programLogger;
	protected MainWindow mw;
	
	private List<Object> interfaceListeners = new ArrayList<Object>();
	private List<Object> datasetListeners = new ArrayList<Object>();
	
	public ProgressableAction(AnalysisDataset dataset, String barMessage, String errorMessage, MainWindow mw){
		
		this.errorMessage 	= errorMessage;
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

	}
	
	public ProgressableAction(AnalysisDataset dataset, String barMessage, String errorMessage, MainWindow mw, int flag){
		this(dataset, barMessage, errorMessage, mw);
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
		removeDatasetEventListener(mw);
		removeInterfaceEventListener(mw);
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
		fireInterfaceEvent(InterfaceMethod.SAVE_ROOT);
		
		
		List<AnalysisDataset> list = new ArrayList<AnalysisDataset>(0);
		list.add(dataset);

		fireInterfaceEvent(InterfaceMethod.UPDATE_PANELS);
		
		fireDatasetEvent(DatasetMethod.SELECT_DATASETS, list);
		fireInterfaceEvent(InterfaceMethod.RECACHE_CHARTS);
//		fireDatasetEvent(DatasetMethod.RECALCULATE_CACHE, list);
		
		this.removeInterfaceEventListener(mw);
		this.removeDatasetEventListener(mw);
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
