package com.bmskinner.nuclear_morphology.gui.main;

import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFrame;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.core.EventHandler;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.CancellableRunnable;
import com.bmskinner.nuclear_morphology.gui.DatasetUpdateEvent;
import com.bmskinner.nuclear_morphology.gui.EventListener;
import com.bmskinner.nuclear_morphology.gui.PopulationListUpdateListener;
import com.bmskinner.nuclear_morphology.gui.PopulationListUpdateListener.PopulationListUpdateEvent;
import com.bmskinner.nuclear_morphology.gui.tabs.DatasetSelectionListener;
import com.bmskinner.nuclear_morphology.gui.tabs.TabPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.populations.PopulationsPanel;
import com.bmskinner.nuclear_morphology.logging.Loggable;

public abstract class AbstractMainWindow extends JFrame implements Loggable, MainView, EventListener, DatasetSelectionListener, PopulationListUpdateListener {
	
	private static final String PROGRAM_TITLE_BAR_LBL = "Nuclear Morphology Analysis v"
            + Version.currentVersion().toString();
	
	protected final List<EventListener> listeners   = new ArrayList<>();
	protected List<EventListener> updateListeners = new ArrayList<>();
	
	// store panels for iterating messages
	protected final List<TabPanel> detailPanels = new ArrayList<>();

	protected final EventHandler eh;

	protected boolean isStandalone = false;
	
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
                        // If the update is called immediately, then the chart
                        // size has
                        // not yet changed, and therefore will render at the
                        // wrong aspect
                        // ratio

                        Thread.sleep(100);
                    } catch (InterruptedException e1) {
                        return;
                    }

                    for (TabPanel d : detailPanels) {
                        d.updateSize();
                    }
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
    
    
    /*
     * Trigger a recache of all charts and datasets
     */
    protected synchronized void recacheCharts() {

        Runnable task = () -> {
            for (TabPanel panel : getTabPanels()) {
                panel.refreshChartCache();
                panel.refreshTableCache();
            }
        };
        ThreadManager.getInstance().execute(task);
    }

    protected synchronized void clearChartCache() {
        for (TabPanel panel : getTabPanels()) {
            panel.clearChartCache();
            panel.clearTableCache();
        }
    }

    protected synchronized void clearChartCache(final List<IAnalysisDataset> list) {

        if (list == null || list.isEmpty()) {
            warn("A cache clear was requested for a specific list, which was null or empty");
            clearChartCache();
            return;
        }
        for (TabPanel panel : getTabPanels()) {
            panel.clearChartCache(list);
            panel.clearTableCache(list);
        }
    }


    protected synchronized void recacheCharts(final List<IAnalysisDataset> list) {

        Runnable task = () -> {
            for (TabPanel panel : getTabPanels()) {
                panel.refreshChartCache(list);
                panel.refreshTableCache(list);
            }
            eventReceived(new DatasetUpdateEvent(this, list));//DatasetListManager.getInstance().getSelectedDatasets()); // ensure all selected datasets get redrawn
        };
        ThreadManager.getInstance().submit(task);

    }
    
    
    @Override
    public void dispose() {
        super.dispose();
    }
    
    public synchronized void addDatasetUpdateEventListener(EventListener l) {
        updateListeners.add(l);
    }

    public synchronized void removeDatasetUpdateEventListener(EventListener l) {
        updateListeners.remove(l);
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
    
    public class PanelUpdater implements CancellableRunnable {
        private final List<IAnalysisDataset> list;
        
        private final AtomicBoolean isCancelled = new AtomicBoolean(false);

        public PanelUpdater(final List<IAnalysisDataset> list) {
            this.list = list;
        }

        @Override
        public synchronized void run() {

        	for (TabPanel p : getTabPanels()) {
        		p.setChartsAndTablesLoading();
        	}

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
