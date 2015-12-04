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
import gui.InterfaceEvent;
import gui.InterfaceEvent.InterfaceMethod;
import gui.InterfaceEventListener;
import gui.SignalChangeEvent;
import gui.SignalChangeListener;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import charting.ChartCache;
import charting.TableCache;
import analysis.AnalysisDataset;

/**
 * Add the listener and signal change settings to save each panel
 * reimplementing them
 * @author bms41
 *
 */
public abstract class DetailPanel extends JPanel implements TabPanel {
	
	private static final long serialVersionUID = 1L;
	private List<Object> listeners = new ArrayList<Object>();
	private List<Object> datasetListeners = new ArrayList<Object>();
	private List<Object> interfaceListeners = new ArrayList<Object>();
	private List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
	
	// The chart cache holds rendered charts for all selected options, until a change is made to a dataset
	// The table cache does the same for table models
	protected ChartCache chartCache = new ChartCache();
	protected TableCache tableCache = new TableCache();
	
	private boolean isUpdating = false;
	
	protected Logger programLogger;
	
	public DetailPanel(Logger programLogger){
		this.programLogger = programLogger;
	}
	
	/**
	 * Fetch the currently active dataset for the panel.
	 * Use when only one dataset is expected to be visible;
	 * this simply accesses the first dataset in the list provided
	 * @return
	 */
	public AnalysisDataset activeDataset(){
		return list.get(0);
	}
	
	protected List<AnalysisDataset> getDatasets(){
		return this.list;
	}
	
	public ChartCache getChartCache(){
		return this.chartCache;
	}
	
	protected boolean isUpdating(){
		return this.isUpdating;
	}
	
	protected void setUpdating(boolean b){
		this.isUpdating = b;
	}
	
	public void update(List<AnalysisDataset> list){
		
		if(this.isUpdating()){
			programLogger.log(Level.FINEST, "Panel is already updating");
		} else {
			this.list = list;
			setUpdating(true);
			updateDetail();
		}

	}
	
	protected void updateDetail(){
		
	}
		
	/**
	 * Remove all charts from the cache so they will be recalculated
	 * @param list
	 */
	public void refreshChartCache(){
		programLogger.log(Level.FINEST, "Refreshing chart cache");
		this.getChartCache().refresh();
	}
	
	/**
	 * Remove all charts from the cache containing datasets in
	 * the given list, so they will be recalculated
	 * @param list
	 */
	public void refreshChartCache(List<AnalysisDataset> list){
		programLogger.log(Level.FINEST, "Refreshing chart cache");
		this.getChartCache().refresh(list);
	}
	
	public TableCache getTableCache(){
		return this.tableCache;
	}
	
	/**
	 * Remove all charts from the cache so they will be recalculated
	 * @param list
	 */
	public void refreshTableCache(){
		programLogger.log(Level.FINEST, "Refreshing table cache");
		this.getTableCache().refresh();
	}
	
	/**
	 * Remove all tables from the cache containing datasets in
	 * the given list, so they will be recalculated
	 * @param list
	 */
	public void refreshTableCache(List<AnalysisDataset> list){
		programLogger.log(Level.FINEST, "Refreshing chart cache");
		this.getTableCache().refresh(list);
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
    
    public synchronized void addInterfaceEventListener( InterfaceEventListener l ) {
    	interfaceListeners.add( l );
    }
    
    public synchronized void removeInterfaceEventListener( InterfaceEventListener l ) {
    	interfaceListeners.remove( l );
    }
        
    
    /**
     * A message to write in the main window status line
     * @param message
     */
    public void status(String message){
    	fireSignalChangeEvent("Status_"+message);
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
    
    protected synchronized void fireDatasetEvent(DatasetMethod method, List<AnalysisDataset> list, AnalysisDataset template) {

    	DatasetEvent event = new DatasetEvent( this, method, this.getClass().getSimpleName(), list, template);
    	Iterator<Object> iterator = datasetListeners.iterator();
    	while( iterator.hasNext() ) {
    		( (DatasetEventListener) iterator.next() ).datasetEventReceived( event );
    	}
    }
    
    protected synchronized void fireInterfaceEvent(InterfaceMethod method) {
    	
    	InterfaceEvent event = new InterfaceEvent( this, method, this.getClass().getSimpleName());
        Iterator<Object> iterator = interfaceListeners.iterator();
        while( iterator.hasNext() ) {
            ( (InterfaceEventListener) iterator.next() ).interfaceEventReceived( event );
        }
    }

}
