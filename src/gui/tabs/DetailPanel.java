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
package gui.tabs;

import gui.DatasetEvent;
import gui.DatasetEvent.DatasetMethod;
import gui.DatasetEventListener;
import gui.SignalChangeEvent;
import gui.SignalChangeListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JPanel;

import analysis.AnalysisDataset;

/**
 * Add the listener and signal change settings to save each panel
 * reimplementing them
 * @author bms41
 *
 */
public abstract class DetailPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	private List<Object> listeners = new ArrayList<Object>();
	private List<Object> datasetListeners = new ArrayList<Object>();
	
	public DetailPanel(){
		
	}
	
	public synchronized void addSignalChangeListener( SignalChangeListener l ) {
        listeners.add( l );
    }
    
    public synchronized void removeSignalChangeListener( SignalChangeListener l ) {
        listeners.remove( l );
    }
    
    public synchronized void addDatasetEventListener( DatasetEventListener l ) {
    	datasetListeners.add( l );
    }
    
    public synchronized void removeDatasetEventListener( DatasetEventListener l ) {
    	datasetListeners.remove( l );
    }
    
    /**
     * A message to log in the main window log panel
     * @param message
     */
    public void log(String message){
    	fireSignalChangeEvent("Log_"+message);
    }
    
    
    /**
     * A message to write in the main window status line
     * @param message
     */
    public void status(String message){
    	fireSignalChangeEvent("Status_"+message);
    }
    
    /**
     * Log an error to the main window
     * @param message
     * @param e
     */
    public void error(String message, Exception e){
    	log(message+": "+e.getMessage());
		for(StackTraceElement e1 : e.getStackTrace()){
			log(e1.toString());
		}
    }
	
    protected synchronized void fireSignalChangeEvent(String message) {
    	
        SignalChangeEvent event = new SignalChangeEvent( this, message, this.getClass().getSimpleName());
        Iterator<Object> iterator = listeners.iterator();
        while( iterator.hasNext() ) {
            ( (SignalChangeListener) iterator.next() ).signalChangeReceived( event );
        }
    }
    
    protected synchronized void fireDatasetEvent(DatasetMethod method, List<AnalysisDataset> list) {
    	
        DatasetEvent event = new DatasetEvent( this, method, this.getClass().getSimpleName(), list);
        Iterator<Object> iterator = datasetListeners.iterator();
        while( iterator.hasNext() ) {
            ( (DatasetEventListener) iterator.next() ).datasetEventReceived( event );
        }
    }

}
