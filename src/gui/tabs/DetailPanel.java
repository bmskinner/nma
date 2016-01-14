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
package gui.tabs;

import gui.DatasetEvent;
import gui.DatasetEvent.DatasetMethod;
import gui.DatasetEventListener;
import gui.InterfaceEvent;
import gui.InterfaceEvent.InterfaceMethod;
import gui.InterfaceEventListener;
import gui.SignalChangeEvent;
import gui.SignalChangeListener;

import java.awt.Cursor;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JPanel;

import charting.ChartCache;
import charting.TableCache;
import analysis.AnalysisDataset;

/**
 * Add the listener and signal change settings to save each panel
 * reimplementing them
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public abstract class DetailPanel extends JPanel implements TabPanel {
	
	private final List<Object> listeners 			= new ArrayList<Object>();
	private final List<Object> datasetListeners 	= new ArrayList<Object>();
	private final List<Object> interfaceListeners 	= new ArrayList<Object>();
	
	private List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
	
	private final List<DetailPanel> subPanels = new  ArrayList<DetailPanel>();
	
	// The chart cache holds rendered charts for all selected options, until a change is made to a dataset
	// The table cache does the same for table models
	protected final ChartCache chartCache = new ChartCache();
	protected final TableCache tableCache = new TableCache();

	
	volatile private boolean isUpdating = false;
	
	protected final Logger programLogger;
	
	public DetailPanel( final Logger programLogger){
		this.programLogger = programLogger;
	}
	
	
	/**
	 * Add another detail panel as a sub panel to this.
	 * This will pass on refreshes and UI updates
	 * @param panel
	 */
	public void addSubPanel(DetailPanel panel){
		subPanels.add(panel);
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
	
	/**
	 * Make a new list holding only the active dataset. This is used
	 * to pass the active dataset back to update()
	 * @return
	 */
	public List<AnalysisDataset> activeDatasetToList(){
		List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
		list.add(activeDataset());
		return list;
	}
	
	/**
	 * Test if only a single dataset is selected
	 * @return
	 */
	public boolean isSingleDataset(){
		if(this.list.size()==1){
			return true;
		} else {
			return false;
		}
	}
	
	public boolean hasDatasets(){
		if(this.list.size()>0){
			return true;
		} else {
			return false;
		}
	}
	
	protected List<AnalysisDataset> getDatasets(){
		return this.list;
	}
	
	public ChartCache getChartCache(){
		return this.chartCache;
	}
	
	public boolean isUpdating(){
		boolean result = false;
		for(DetailPanel panel : this.subPanels){
			if(panel.isUpdating()){
				result = true;
			}
		}
		if(this.isUpdating){
			result = true;
		}
		return result;
	}
	
	protected void setUpdating(boolean b){
		this.isUpdating = b;
	}
	
	
	/**
	 * Toggle wait cursor on element
	 * @param b
	 */
	public void setAnalysing(boolean b){
		if(b){
			this.setCursor(new Cursor(Cursor.WAIT_CURSOR));
		} else {
			this.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		}
		for(DetailPanel panel : this.subPanels){
			panel.setAnalysing(b);;
		}
	}
	
	public void setEnabled(boolean b){
		for(DetailPanel panel : this.subPanels){
			panel.setEnabled(b);;
		}
	}
	
	public void update(List<AnalysisDataset> list){
		
		if(this.isUpdating()){
			programLogger.log(Level.FINEST, "Panel is already updating");
		} else {
			if(list!=null){
				this.list = list;
			} else {
				this.list = new ArrayList<AnalysisDataset>();
			}
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
		for(DetailPanel panel : this.subPanels){
			panel.refreshChartCache();
		}
	}
	
	/**
	 * Remove all charts from the cache containing datasets in
	 * the given list, so they will be recalculated
	 * @param list
	 */
	public void refreshChartCache(List<AnalysisDataset> list){
		programLogger.log(Level.FINEST, "Refreshing chart cache");
		this.getChartCache().refresh(list);
		for(DetailPanel panel : this.subPanels){
			panel.refreshChartCache(list);
		}
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
		for(DetailPanel panel : this.subPanels){
			panel.refreshTableCache();
		}
	}
	
	/**
	 * Remove all tables from the cache containing datasets in
	 * the given list, so they will be recalculated
	 * @param list
	 */
	public void refreshTableCache(List<AnalysisDataset> list){
		programLogger.log(Level.FINEST, "Refreshing chart cache");
		this.getTableCache().refresh(list);
		for(DetailPanel panel : this.subPanels){
			panel.refreshTableCache(list);
		}
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
    
    protected synchronized void fireSignalChangeEvent(SignalChangeEvent event) {
    	Iterator<Object> iterator = datasetListeners.iterator();
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
    
    protected synchronized void fireDatasetEvent(DatasetEvent event) {
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
    
    protected synchronized void fireInterfaceEvent(InterfaceEvent event) {

        Iterator<Object> iterator = interfaceListeners.iterator();
        while( iterator.hasNext() ) {
            ( (InterfaceEventListener) iterator.next() ).interfaceEventReceived( event );
        }
    }

}
