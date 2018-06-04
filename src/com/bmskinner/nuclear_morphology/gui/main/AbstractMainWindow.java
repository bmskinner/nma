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
import com.bmskinner.nuclear_morphology.gui.CancellableRunnable;
import com.bmskinner.nuclear_morphology.gui.DatasetEventListener;
import com.bmskinner.nuclear_morphology.gui.DatasetUpdateEvent;
import com.bmskinner.nuclear_morphology.gui.DatasetUpdateEventListener;
import com.bmskinner.nuclear_morphology.gui.InterfaceEventListener;
import com.bmskinner.nuclear_morphology.gui.tabs.DatasetSelectionListener;
import com.bmskinner.nuclear_morphology.gui.tabs.TabPanel;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.main.EventHandler;
import com.bmskinner.nuclear_morphology.main.ThreadManager;

public abstract class AbstractMainWindow extends JFrame implements Loggable, MainView, InterfaceEventListener, DatasetSelectionListener, DatasetEventListener, DatasetUpdateEventListener {
	
	private static final String PROGRAM_TITLE_BAR_LBL = "Nuclear Morphology Analysis v"
            + Version.currentVersion().toString();
	
	protected final List<InterfaceEventListener> listeners   = new ArrayList<>();
	protected List<Object> updateListeners = new ArrayList<Object>();
	
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

        this.setDropTarget(new MainDragAndDropTarget(this));
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
            datasetUpdateEventReceived(new DatasetUpdateEvent(this, list));//DatasetListManager.getInstance().getSelectedDatasets()); // ensure all selected datasets get redrawn
        };
        ThreadManager.getInstance().submit(task);

    }
    
    
    @Override
    public void dispose() {
        super.dispose();
    }
    
    public synchronized void addDatasetUpdateEventListener(DatasetUpdateEventListener l) {
        updateListeners.add(l);
    }

    public synchronized void removeDatasetUpdateEventListener(DatasetUpdateEventListener l) {
        updateListeners.remove(l);
    }
    
    @Override
	public void datasetUpdateEventReceived(DatasetUpdateEvent event) {
		PanelUpdater r = new PanelUpdater(event.getDatasets());
        ThreadManager.getInstance().submit(r);
		
	}
    
    private static final class Lock { }

    public class PanelUpdater implements CancellableRunnable {
        private final List<IAnalysisDataset> list;
        
        private final AtomicBoolean isCancelled = new AtomicBoolean(false);

        public PanelUpdater(final List<IAnalysisDataset> list) {
            this.list = list;
        }
        private final Lock lock = new Lock();

        @Override
        public synchronized void run() {
            final PanelLoadingUpdater loader = new PanelLoadingUpdater(lock);
            try {

                final Future<?> f = ThreadManager.getInstance().submit(loader);

                synchronized (lock) {
                // Wait for loading state to be set
                	while (!f.isDone() && !isCancelled.get()) {
                		lock.wait();
                	}
                }
                
                // Again stop if a cancel signal was heard 
                if (isCancelled.get()) {
                    return;
                }
                
                Boolean ok = (Boolean) f.get();

                if(ok!=null && !ok){
                    warn("Error updating the UI panels");
                }

            } catch (InterruptedException e1) {
                warn("Interrupted update");
                error("Error setting loading state", e1);
                error("Cause of loading state error", e1.getCause());
                return;
            } catch (ExecutionException e1) {
                error("Error setting loading state", e1);
                error("Cause of loading state error", e1.getCause());
                return;
            } catch (Exception e1){
                error("Undefined error setting panel loading state", e1);
            }
            
            // Now fire the update
            DatasetUpdateEvent e = new DatasetUpdateEvent(this, list);
            Iterator<Object> iterator = updateListeners.iterator();
            while (iterator.hasNext()) {
                if (isCancelled.get()) {
                    return;
                }
                ((DatasetUpdateEventListener) iterator.next()).datasetUpdateEventReceived(e);
            }

        }

        @Override
        public void cancel() {
            isCancelled.set(true);
        }

    }

    public class PanelLoadingUpdater implements Callable {
    	private final Object lock;
    	
    	public PanelLoadingUpdater(Lock l) {
    		lock = l;
    	}
    	
        @Override
        public synchronized Boolean call() {
            // Update charts and panels to loading
            try {
                for (TabPanel p : getTabPanels()) {
                    p.setChartsAndTablesLoading();
                }
            } catch(Exception e){
                error("Error setting loading state", e);
                return false;
            }
            
            synchronized (lock) {
                lock.notifyAll();
            }
            return true;

        }

    }
}
