/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.gui.main;

import java.awt.Cursor;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.core.InterfaceUpdater;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.CancellableRunnable;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.events.DatasetUpdateEvent;
import com.bmskinner.nuclear_morphology.gui.events.EventListener;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.events.PopulationListUpdateListener;
import com.bmskinner.nuclear_morphology.gui.tabs.DatasetSelectionListener;
import com.bmskinner.nuclear_morphology.gui.tabs.TabPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.populations.PopulationsPanel;


/**
 * Base class for main windows
 * @author Ben Skinner
 *
 */
@SuppressWarnings("serial")
public abstract class AbstractMainWindow extends JFrame implements MainView, EventListener, DatasetSelectionListener, PopulationListUpdateListener {
	
	private static final String PROGRAM_TITLE_BAR_LBL = "Nuclear Morphology Analysis v"
            + Version.currentVersion().toString();
	
	protected final List<EventListener> listeners   = new ArrayList<>();
	
	/** Listeners for datase update events */
	protected final List<EventListener> updateListeners   = new ArrayList<>();
	
	/** Panels displaying dataset information */
	protected final List<TabPanel> detailPanels = new ArrayList<>();

	protected final EventHandler eh;

	protected boolean isStandalone = false;
	
	private static final Logger LOGGER = Logger.getLogger(AbstractMainWindow.class.getName());
	
	/**
     * Create the frame.
     * 
     * @param standalone is the frame a standalone app, or launched within ImageJ?
     */
    public AbstractMainWindow(boolean standalone, EventHandler eh) {
    	 isStandalone = standalone;
         this.eh = eh;         
         setTitle(PROGRAM_TITLE_BAR_LBL);
         eh.addInterfaceEventListener(this);
         eh.addDatasetSelectionListener(this);
         eh.addDatasetEventListener(this);
         eh.addDatasetUpdateEventListener(this);
         eh.addPopulationListUpdateListener(this);
    }
    
    /**
     * Create the listeners that handle dataset saving when the main window is
     * closed
     * 
     */
    protected void createWindowListeners() {

        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        this.addWindowListener(new MainWindowCloseAdapter(this));

        // Add a listener for panel size changes. This will cause
        // charts to redraw at the new aspect ratio rather than stretch.
        this.addWindowStateListener(new WindowStateListener() {
            @Override
			public void windowStateChanged(WindowEvent e) {

                Runnable r = () -> {
                    try {
                        // If the update is called immediately, the chart  size has
                        // not yet changed, and therefore will render at the wrong aspect
                        // ratio
                        Thread.sleep(100);
                    } catch (InterruptedException e1) {
                        return;
                    }

                    for (TabPanel d : detailPanels)
                        d.updateSize();
                };
                ThreadManager.getInstance().submit(r);

            }
        });

        this.setDropTarget(new MainDragAndDropTarget(eh));
    }

    protected abstract void createUI();

    protected abstract void createEventHandling();
    
    public List<TabPanel> getTabPanels() {
        return this.detailPanels;
    }
    
    /**
     * Check if the program has been started as a plugin to ImageJ or as
     * standalone
     * 
     * @return
     */
    @Override
	public boolean isStandalone() {
        return isStandalone;
    }
      
    /**
     * Get the event handler that dispatches messages and analyses
     * 
     * @return
     */
    @Override
	public EventHandler getEventHandler() {
        return eh;
    }
    
    @Override
	public InputSupplier getInputSupplier() {
        return eh.getInputSupplier();
    }
    
    
    /**
     * Remove all charts and tables from caches, 
     * but do not redraw them 
     */
    protected synchronized void clearChartCache() {
        for (TabPanel panel : getTabPanels()) {
            panel.clearChartCache();
            panel.clearTableCache();
        }
    }

    /**
     * Remove charts and tables from caches which contain the
     * given datasets 
     * @param list
     */
    protected synchronized void clearChartCache(final List<IAnalysisDataset> list) {

        if (list == null || list.isEmpty()) {
            LOGGER.log(Level.WARNING, "A cache clear was requested for a specific list, which was null or empty");
            clearChartCache();
            return;
        }
        for (TabPanel panel : getTabPanels()) {
            panel.clearChartCache(list);
            panel.clearTableCache(list);
        }
    }
    
    /*
     * Trigger a recache of all charts and tables
     */
    protected synchronized void recacheCharts() {
        InterfaceUpdater task = () -> {
            for (TabPanel panel : getTabPanels()) {
                panel.refreshChartCache();
                panel.refreshTableCache();                
            }
        };
        ThreadManager.getInstance().execute(task);
    }

    /*
     * Trigger a recache of all charts and tables which contain the given datasets
     */
    protected synchronized void recacheCharts(final List<IAnalysisDataset> list) {

    	InterfaceUpdater task = () -> {
    		for (TabPanel panel : getTabPanels()) {
    			panel.refreshChartCache(list);
    			panel.refreshTableCache(list);
    		}
    	};
    	ThreadManager.getInstance().execute(task);

    }
    
    
    @Override
    public void dispose() {
        super.dispose();
    }
    
    public synchronized void addDatasetUpdateEventListener(EventListener l) {
        updateListeners.add(l);
    }

    protected abstract PopulationsPanel getPopulationsPanel();
    
    @Override
    public void populationListUpdateEventReceived(PopulationListUpdateEvent event) {
    	this.getPopulationsPanel().update();
    }
    
    @Override
	public void eventReceived(DatasetUpdateEvent event) {
		PanelUpdater r = new PanelUpdater(event.getDatasets());
        ThreadManager.getInstance().execute(r);
		
	}
    
    @Override
	public void eventReceived(DatasetEvent event) {

		if (event.method().equals(DatasetEvent.RECACHE_CHARTS))
            recacheCharts(event.getDatasets());

        if (event.method().equals(DatasetEvent.CLEAR_CACHE))
            clearChartCache(event.getDatasets());
        
        if (event.method().equals(DatasetEvent.ADD_WORKSPACE))
        	getPopulationsPanel().update();
	}
	
	public synchronized void removeDatasetUpdateEventListener(EventListener l) {
	    updateListeners.remove(l);
	}

	@Override
	public void eventReceived(InterfaceEvent event) {

		if(event.getSource().equals(eh)){
			InterfaceMethod method = event.method();
	        
	        final List<IAnalysisDataset> selected = DatasetListManager.getInstance().getSelectedDatasets();

	        switch (method) {

	        case REFRESH_POPULATIONS: getPopulationsPanel().update(selected); // ensure all child datasets are included
	            break;

	        case UPDATE_IN_PROGRESS:
	            for (TabPanel panel : getTabPanels()) {
	                panel.setAnalysing(true);
	            }
	            setCursor(new Cursor(Cursor.WAIT_CURSOR));
	            break;

	        case UPDATE_COMPLETE:
	            for (TabPanel panel : getTabPanels()) {
	                panel.setAnalysing(false);
	            }
	            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	            break;

	        case RECACHE_CHARTS:{
	        	recacheCharts();
	        	break;
	        }
			default:
				break;
	        }
		}
	}
    
    /**
     * Send panel update requests to all panels
     * @author ben
     *
     */
    public class PanelUpdater implements CancellableRunnable, InterfaceUpdater {
        private final List<IAnalysisDataset> list = new ArrayList<>();
        
        private final AtomicBoolean isCancelled = new AtomicBoolean(false);

        public PanelUpdater(final @NonNull List<IAnalysisDataset> datasets) {
            this.list.addAll(datasets);
//            log("Ping");
//        	for(StackTraceElement e :Thread.currentThread().getStackTrace())
//        		log(e.toString());
        }

        @Override
        public synchronized void run() {
        	
        	// Set the loading state
        	for (TabPanel p : getTabPanels()) {
        		p.setChartsAndTablesLoading();
        	}

        	// Fire the update to each listener
            DatasetUpdateEvent e = new DatasetUpdateEvent(this, list);
            Iterator<EventListener> iterator = updateListeners.iterator();
            while (iterator.hasNext()) {
                if (isCancelled.get())
                    return;
                iterator.next().eventReceived(e);
            }

        }

        @Override
        public void cancel() {
            isCancelled.set(true);
        }

    }
}
