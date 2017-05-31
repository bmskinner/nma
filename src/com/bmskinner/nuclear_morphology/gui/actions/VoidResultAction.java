/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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

package com.bmskinner.nuclear_morphology.gui.actions;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.swing.JProgressBar;

import com.bmskinner.nuclear_morphology.analysis.IAnalysisWorker;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.DatasetEventListener;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent;
import com.bmskinner.nuclear_morphology.gui.InterfaceEventListener;
import com.bmskinner.nuclear_morphology.gui.LogPanel;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * The base of all progressible actions. Handles progress bars and workers
 * @author bms41
 * @since 1.13.6
 *
 */
public abstract class VoidResultAction 
	implements PropertyChangeListener, 
	Loggable, 
	MouseListener,
	Runnable {

	
	protected IAnalysisDataset dataset = null; // the dataset being worked on
	private JProgressBar progressBar = null;
	
	protected IAnalysisWorker worker = null;
	protected int downFlag = 0; // store flags to tell the action what to do after finishing
	
	private LogPanel logPanel;
	protected MainWindow mw;
	private CountDownLatch latch = null; // allow threads to wait for the analysis to complete
	
	private List<Object> interfaceListeners = new ArrayList<Object>();
	private List<Object> datasetListeners = new ArrayList<Object>();
	
	/**
	 * Constructor with no datasets - used for new analysis
	 * @param barMessage
	 * @param mw
	 */
	protected VoidResultAction(String barMessage, MainWindow mw){
		
		this.progressBar 	= new JProgressBar(0, 100);
		this.progressBar.setString(barMessage);
		this.progressBar.setStringPainted(true);
		this.progressBar.setIndeterminate(true);
		this.progressBar.addMouseListener(this);

		this.mw 			= mw;
		this.logPanel 		= mw.getLogPanel();
		
		logPanel.addProgressBar(this.progressBar);
		logPanel.revalidate();
		logPanel.repaint();

		this.addInterfaceEventListener(mw.getEventHandler());
		this.addDatasetEventListener(mw.getEventHandler());
		finest( "Created progressable action");

	}
	
		
	protected void setLatch(CountDownLatch latch){
		this.latch = latch;
	}
	
	protected void countdownLatch(){
		if(latch!=null){
			latch.countDown();
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
		finest( "Removing interface and dataset listeners");
		removeProgressBar();
		removeDatasetEventListener(mw.getEventHandler());
		removeInterfaceEventListener(mw.getEventHandler());
	}
	
	protected void setProgressBarVisible(boolean b){
		this.progressBar.setVisible(b);
	}
	
	/**
	 * Use to manually remove the progress bar after an action is complete
	 */
	public void cleanup(){
		if (this.worker.isDone() || this.worker.isCancelled()){
			this.worker.removePropertyChangeListener(this);
			finest("Removed property change listener from worker");
			this.removeProgressBar();
			finest("Removed progress bar");
			
		}
	}
	
	
	@Override
	public void propertyChange(PropertyChangeEvent evt) {

	    int value = 0;

	    Object newValue = evt.getNewValue();

	    if(newValue instanceof Integer){
	    	value = (int) newValue;
	    }

		finer("Property change event heard: "+value);
		
		if(value >=0 && value <=100){
			
			if(this.progressBar.isIndeterminate()){
				this.progressBar.setIndeterminate(false);
			}
			this.progressBar.setValue(value);
		}

		if(evt.getPropertyName().equals("Finished")){
			finer("Worker signaled finished");
			finished();
		}

		if(evt.getPropertyName().equals("Error")){
			warn("Cancelling action due to error");
			removeProgressBar();
		}
		
		if(evt.getPropertyName().equals("Cooldown")){
			finer("Worker signaled cooldown");
			setProgressBarIndeterminate();
		}
		
	}
	
	/**
	 * The method run when the analysis has completed
	 */
	public void finished(){
		this.worker.removePropertyChangeListener(this);
		finer("Removed property change listener from worker");
		removeProgressBar();		
		
		this.removeInterfaceEventListener(mw.getEventHandler());
		this.removeDatasetEventListener(mw.getEventHandler());
		finer("Removed event listeners from action");
	}
	
	
	/**
	 * Runs if a cooldown signal is received. Use to set progress bars
	 * to an indeterminate state when no reliable progress metric is 
	 * available
	 */
	public void setProgressBarIndeterminate(){
		progressBar.setIndeterminate(true);
	}
	
	public synchronized boolean isDone(){
		return worker.isDone();
	}
		
	protected synchronized void fireInterfaceEvent(InterfaceMethod method) {
    	
        InterfaceEvent event = new InterfaceEvent( this, method, this.getClass().getSimpleName());
        Iterator<Object> iterator = interfaceListeners.iterator();
        while( iterator.hasNext() ) {
            ( (InterfaceEventListener) iterator.next() ).interfaceEventReceived( event );
        }
    }	
	
	protected synchronized void fireDatasetEvent(String method, IAnalysisDataset dataset) {

		List<IAnalysisDataset> list = new ArrayList<IAnalysisDataset>();
		list.add(dataset);
		fireDatasetEvent(method, list);
	}

	protected synchronized void fireDatasetEvent(String method, List<IAnalysisDataset> list) {
    	
        DatasetEvent event = new DatasetEvent( this, method, this.getClass().getSimpleName(), list);
        Iterator<Object> iterator = datasetListeners.iterator();
        while( iterator.hasNext() ) {
            ( (DatasetEventListener) iterator.next() ).datasetEventReceived( event );
        }
    }
	
	protected synchronized void fireDatasetEvent(String method, List<IAnalysisDataset> list, IAnalysisDataset secondary) {
    	
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
	
	@Override
	public void mouseClicked(MouseEvent e) {
		if(e.getSource()==progressBar){
			if(e.getClickCount()==2){
				if(progressBar.isIndeterminate()){
					this.removeProgressBar();
				}
			}
		}
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}
}
