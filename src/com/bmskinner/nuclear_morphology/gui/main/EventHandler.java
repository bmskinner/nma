/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.gui.main;

import java.awt.Cursor;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.gui.CancellableRunnable;
import com.bmskinner.nuclear_morphology.gui.ConsensusNucleusPanel;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.DatasetEventListener;
import com.bmskinner.nuclear_morphology.gui.DatasetUpdateEvent;
import com.bmskinner.nuclear_morphology.gui.DatasetUpdateEventListener;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.InterfaceEventListener;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.SignalChangeListener;
import com.bmskinner.nuclear_morphology.gui.actions.AddNuclearSignalAction;
import com.bmskinner.nuclear_morphology.gui.actions.BuildHierarchicalTreeAction;
import com.bmskinner.nuclear_morphology.gui.actions.ClusterAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.ClusterFileAssignmentAction;
import com.bmskinner.nuclear_morphology.gui.actions.DatasetArithmeticAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportStatsAction.ExportNuclearStatsAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportStatsAction.ExportShellsAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportStatsAction.ExportSignalsAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExtractRandomCellsAction;
import com.bmskinner.nuclear_morphology.gui.actions.FishRemappingAction;
import com.bmskinner.nuclear_morphology.gui.actions.LobeDetectionAction;
import com.bmskinner.nuclear_morphology.gui.actions.MergeCollectionAction;
import com.bmskinner.nuclear_morphology.gui.actions.MergeSourceExtractionAction;
import com.bmskinner.nuclear_morphology.gui.actions.NewAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.PopulationImportAction;
import com.bmskinner.nuclear_morphology.gui.actions.RefoldNucleusAction;
import com.bmskinner.nuclear_morphology.gui.actions.RelocateFromFileAction;
import com.bmskinner.nuclear_morphology.gui.actions.ReplaceSourceImageDirectoryAction;
import com.bmskinner.nuclear_morphology.gui.actions.RunProfilingAction;
import com.bmskinner.nuclear_morphology.gui.actions.RunSegmentationAction;
import com.bmskinner.nuclear_morphology.gui.actions.SaveDatasetAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportWorkspaceAction;
import com.bmskinner.nuclear_morphology.gui.actions.ShellAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.SingleDatasetResultAction;
import com.bmskinner.nuclear_morphology.gui.dialogs.collections.CellCollectionOverviewDialog;
import com.bmskinner.nuclear_morphology.gui.tabs.TabPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.nuclear.NuclearStatisticsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.segments.SegmentsDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.signals.SignalsDetailPanel;
import com.bmskinner.nuclear_morphology.io.CellFileExporter;
import com.bmskinner.nuclear_morphology.io.WorkspaceImporter;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.main.DatasetListManager;
import com.bmskinner.nuclear_morphology.main.ThreadManager;

/**
 * Listens to messages from the UI and launches actions
 * 
 * @author bms41
 * @since 1.13.7
 *
 */
public class EventHandler implements Loggable, SignalChangeListener, DatasetEventListener, InterfaceEventListener {

    private final MainWindow mw;

    private List<Object> updateListeners = new ArrayList<Object>();

    public EventHandler(final MainWindow mw) {
        this.mw = mw;
    }

    /**
     * Map events to the actions they should trigger
     * 
     * @author bms41
     * @since 1.13.4
     *
     */
    private class ActionFactory {

        public ActionFactory() { }

        /**
         * Create a runnable action for the given event
         * 
         * @param event
         * @return
         */
        public synchronized Runnable create(final SignalChangeEvent event) {
        	
        	final List<IAnalysisDataset> selectedDatasets = DatasetListManager.getInstance().getSelectedDatasets();
        	final IAnalysisDataset selectedDataset = selectedDatasets.isEmpty() ? null
                    : selectedDatasets.get(0);
        	

            if (event.type().equals(SignalChangeEvent.EXPORT_WORKSPACE))
                return new ExportWorkspaceAction(DatasetListManager.getInstance().getWorkspaces(), mw);

            if (event.type().equals(SignalChangeEvent.DATASET_ARITHMETIC))
                return new DatasetArithmeticAction(selectedDatasets, mw);
            
            if (event.type().equals(SignalChangeEvent.EXTRACT_SUBSET))
                return new ExtractRandomCellsAction(selectedDataset, mw);

            if (event.type().equals(SignalChangeEvent.CHANGE_NUCLEUS_IMAGE_FOLDER))
                return new ReplaceSourceImageDirectoryAction(selectedDataset, mw);

            if (event.type().equals(SignalChangeEvent.ADD_NUCLEAR_SIGNAL))
                return new AddNuclearSignalAction(selectedDataset, mw);

            if (event.type().equals(SignalChangeEvent.POST_FISH_MAPPING))
                return new FishRemappingAction(selectedDatasets, mw);

            if (event.type().equals(SignalChangeEvent.EXPORT_STATS))
                return new ExportNuclearStatsAction(selectedDatasets, mw);
            
            if (event.type().equals(SignalChangeEvent.EXPORT_SIGNALS))
            	return new ExportSignalsAction(selectedDatasets, mw);
            
            if (event.type().equals(SignalChangeEvent.EXPORT_SHELLS))
                return new ExportShellsAction(selectedDatasets, mw);

            if (event.type().equals(SignalChangeEvent.LOBE_DETECTION))
                return new LobeDetectionAction(selectedDataset, mw);
            
            if (event.type().equals(SignalChangeEvent.MERGE_DATASETS_ACTION))
                return new MergeCollectionAction(selectedDatasets, mw);

            if (event.type().equals(SignalChangeEvent.CHANGE_SCALE))
                return () -> setScale(selectedDatasets);
                
            if (event.type().equals(SignalChangeEvent.SAVE_DATASET_ACTION))
            	return () -> saveDataset(selectedDataset, true);
            	
        	if (event.type().equals(SignalChangeEvent.UPDATE_PANELS_WITH_NULL) )
        		return () -> fireDatasetUpdateEvent(new ArrayList<IAnalysisDataset>());
        		
        	if (event.type().equals(SignalChangeEvent.UPDATE_PANELS))
                return () -> fireDatasetUpdateEvent(selectedDatasets);
                
                
            if (event.type().equals(SignalChangeEvent.CURATE_DATASET))
            	return () ->{
                CellCollectionOverviewDialog d = new CellCollectionOverviewDialog(selectedDataset);
                d.addDatasetEventListener(EventHandler.this);
            };
            
            if (event.type().equals(SignalChangeEvent.UPDATE_POPULATION_PANELS))
                return () -> mw.getPopulationsPanel().update(selectedDatasets);
                
            if (event.type().equals(SignalChangeEvent.EXPORT_CELL_LOCS))
            	return () ->{
                    log("Exporting cell locations...");
                    if (new CellFileExporter().exportCellLocations(selectedDataset)) {
                        log("Export complete");
                    } else {
                        log("Export failed");
                    }
                };
            
            if (event.type().startsWith("Open|")) {
                String s = event.type().replace("Open|", "");
                File f = new File(s);

                return new PopulationImportAction(mw, f);
            }
            
            if (event.type().startsWith("Wrk|"))
            	return () -> {
            		String s = event.type().replace("Wrk|", "");
            		File f = new File(s);

            		IWorkspace w = WorkspaceImporter.createImporter(f).importWorkspace();

            		DatasetListManager.getInstance().addWorkspace(w);

            		for (File dataFile : w.getFiles()) {
            			new PopulationImportAction(mw, dataFile).run();
            		}
            	};

            if (event.type().startsWith("New|")) {
                String s = event.type().replace("New|", "");
                File f = new File(s);

                return new NewAnalysisAction(mw, f);
            }
            
            if (event.type().equals(SignalChangeEvent.NEW_WORKSPACE))
            	return () ->{
            		log("New workspace created");
            	};
            	
                        
            if (event.type().startsWith(SignalChangeEvent.REMOVE_FROM_WORKSPACE_PREFIX))
            	return () ->{
            		String workspaceName = event.type().replace(SignalChangeEvent.REMOVE_FROM_WORKSPACE_PREFIX, "");
            		IWorkspace ws = DatasetListManager.getInstance().getWorkspaces().stream()
            				.filter(w->w.getName().equals(workspaceName)).findFirst().orElseThrow(IllegalArgumentException::new);
            		log("Removing dataset from workspace "+workspaceName);
            	};
            
        	if (event.type().startsWith(SignalChangeEvent.ADD_TO_WORKSPACE_PREFIX))
        		return () ->{
        			String workspaceName = event.type().replace(SignalChangeEvent.ADD_TO_WORKSPACE_PREFIX, "");
        			IWorkspace ws = DatasetListManager.getInstance().getWorkspaces().stream()
        					.filter(w->w.getName().equals(workspaceName)).findFirst().orElseThrow(IllegalArgumentException::new);
        			log("Adding dataset to workspace "+workspaceName);
        		};
            
            
            if (event.type().equals(SignalChangeEvent.RELOCATE_CELLS)) 
                return new RelocateFromFileAction(selectedDataset, mw, new CountDownLatch(1));

            return null;
        }

        /**
         * Create a runnable action for the given event
         * 
         * @param event
         * @return
         */
        public synchronized Runnable create(final DatasetEvent event) {

            if (event.getDatasets().isEmpty())
                return null;

            final List<IAnalysisDataset> selectedDatasets = event.getDatasets();

            if (event.method().equals(DatasetEvent.PROFILING_ACTION)) {

                int flag = 0; // set the downstream analyses to run
                flag |= SingleDatasetResultAction.ADD_POPULATION;
                flag |= SingleDatasetResultAction.STATS_EXPORT;
                flag |= SingleDatasetResultAction.NUCLEUS_ANNOTATE;
                flag |= SingleDatasetResultAction.ASSIGN_SEGMENTS;

                Optional<IAnalysisOptions> op = event.firstDataset().getAnalysisOptions();
                if(op.isPresent() && op.get().refoldNucleus())
                	flag |= SingleDatasetResultAction.CURVE_REFOLD;

                // begin a recursive morphology analysis
                return new RunProfilingAction(selectedDatasets, flag, mw);
            }

            if (event.method().equals(DatasetEvent.NEW_MORPHOLOGY)) {
                log("Running new morphology analysis");
                final int flag = SingleDatasetResultAction.ADD_POPULATION;
                return new RunSegmentationAction(selectedDatasets, MorphologyAnalysisMode.NEW, flag, mw);
            }

            if (event.method().equals(DatasetEvent.REFRESH_MORPHOLOGY)) {
                final int flag = 0;
                return new RunSegmentationAction(selectedDatasets, MorphologyAnalysisMode.REFRESH, flag, mw);
            }
            
            if (event.method().equals(DatasetEvent.REFPAIR_SEGMENTATION)) {
                final int flag = 0;
                return new RunSegmentationAction(selectedDatasets, MorphologyAnalysisMode.NEW, flag, mw);
            }

            if (event.method().equals(DatasetEvent.RUN_SHELL_ANALYSIS)) {
                return new ShellAnalysisAction(event.firstDataset(), mw);
            }

            if (event.method().equals(DatasetEvent.COPY_PROFILE_SEGMENTATION)) {

                final IAnalysisDataset source = event.secondaryDataset();
                if (source == null) {
                    return null;
                }
                return new RunSegmentationAction(selectedDatasets, source, SingleDatasetResultAction.ADD_POPULATION,
                        mw);
            }

            if (event.method().equals(DatasetEvent.CLUSTER)) {
                log("Clustering dataset");
                return new ClusterAnalysisAction(event.firstDataset(), mw);

            }
            
            if (event.method().equals(DatasetEvent.CLUSTER_FROM_FILE))
                return new ClusterFileAssignmentAction(event.firstDataset(), mw);

            if (event.method().equals(DatasetEvent.BUILD_TREE)) {
                log("Building a tree from dataset");
                return new BuildHierarchicalTreeAction(event.firstDataset(), mw);
            }

            if (event.method().equals(DatasetEvent.RECALCULATE_MEDIAN))
                return new RunProfilingAction(selectedDatasets, SingleDatasetResultAction.NO_FLAG, mw);
            
            
            if (event.method().equals(DatasetEvent.EXTRACT_SOURCE))
                return new MergeSourceExtractionAction(event.getDatasets(), mw);
            
            if (event.method().equals(DatasetEvent.REFOLD_CONSENSUS)) {
                Runnable r = () -> {
                    for(IAnalysisDataset d : selectedDatasets){
                        refoldConsensus(d);
                    }
                };
                return r;

            }
            return null;
        }

        /**
         * Create and run an action for the given event
         * 
         * @param event
         */
        public synchronized void run(final SignalChangeEvent event) {
            Runnable r = create(event);
            if (r != null) {
                r.run();
            }
        }

        /**
         * Create and run an action for the given event
         * 
         * @param event
         */
        public synchronized void run(final DatasetEvent event) {
            Runnable r = create(event);
            if (r != null) {
                r.run();
            }
        }
    }

    @Override
    public synchronized void signalChangeReceived(final SignalChangeEvent event) {
        new ActionFactory().run(event);
    }

    @Override
    public synchronized void datasetEventReceived(final DatasetEvent event) {

        // Try to launch via factory
        new ActionFactory().run(event);

        // Remaining methods
        final List<IAnalysisDataset> list = event.getDatasets();
        if (!list.isEmpty()) {

            if (event.method().equals(DatasetEvent.SELECT_DATASETS)) {
                mw.getPopulationsPanel().selectDatasets(event.getDatasets());
            }

            if (event.method().equals(DatasetEvent.SELECT_ONE_DATASET)) {
                mw.getPopulationsPanel().selectDataset(event.firstDataset());
            }

            if (event.method().equals(DatasetEvent.SAVE)) {
                saveDataset(event.firstDataset(), false);
            }

            if (event.method().equals(DatasetEvent.REFRESH_CACHE)) {
                recacheCharts(list);
            }

            if (event.method().equals(DatasetEvent.CLEAR_CACHE)) {

                clearChartCache(list);

            }

            if (event.method().equals(DatasetEvent.ADD_DATASET)) {
                addDataset(event.firstDataset());
            }

        }

    }

    @Override
    public synchronized void interfaceEventReceived(final InterfaceEvent event) {

        InterfaceMethod method = event.method();
        
        final List<IAnalysisDataset> selected = DatasetListManager.getInstance().getSelectedDatasets();

        switch (method) {

        case REFRESH_POPULATIONS:
            mw.getPopulationsPanel().update(selected); // ensure all child
                                                       // datasets are included
            break;

        case SAVE_ROOT:
            saveRootDatasets(); // DO NOT WRAP IN A SEPARATE THREAD, IT WILL
                                // LOCK THE PROGRESS BAR

            break;

        case UPDATE_PANELS: {
            finer("Updating tab panels with list of " + selected.size() + " datasets");
            fireDatasetUpdateEvent(selected);
            break;
        }

        case RECACHE_CHARTS:
            recacheCharts();
            break;
        case LIST_SELECTED_DATASETS:
            int count = 0;
            for (IAnalysisDataset d : selected) {
                log(count + "\t" + d.getName());
                count++;
            }
            break;

        case CLEAR_LOG_WINDOW:
            mw.getLogPanel().clear();
            break;

        case UPDATE_IN_PROGRESS:
            for (TabPanel panel : mw.getTabPanels()) {
                panel.setAnalysing(true);
            }
            mw.setCursor(new Cursor(Cursor.WAIT_CURSOR));
            break;

        case UPDATE_COMPLETE:
            for (TabPanel panel : mw.getTabPanels()) {
                panel.setAnalysing(false);
            }
            mw.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            break;

        case DUMP_LOG_INFO:
            for (IAnalysisDataset d : selected) {

                for (Nucleus n : d.getCollection().getNuclei()) {
                    log(n.toString());
                }
            }
            break;

        case INFO:
            for (IAnalysisDataset d : selected) {

                log(d.getCollection().toString());
            }
            break;

        default:
            break;

        }
    }

    
    private synchronized void setScale(final List<IAnalysisDataset> selectedDatasets) {
    	try {
			final String CHOOSE_NEW_SCALE_LBL      = "Pixels per micron";

			SpinnerNumberModel sModel = new SpinnerNumberModel(1d, 
					1d, 100000d, 1d);

			JSpinner spinner = new JSpinner(sModel);

			int option = JOptionPane.showOptionDialog(null, spinner, CHOOSE_NEW_SCALE_LBL, 
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, null, null);

			if (option == JOptionPane.OK_OPTION) {

				double scale = (double) spinner.getModel().getValue();

				if (scale > 0) { // don't allow a scale to cause divide by zero errors
					selectedDatasets.stream().forEach(d->d.getCollection().setScale(scale));
					log("Updated scale to "+scale+" pixels per micron");
					interfaceEventReceived(new InterfaceEvent(this, InterfaceMethod.RECACHE_CHARTS, "Scale change"));
				}
			}
		}catch(Exception e){
			warn("Error updating scale");
			stack("Error updating scale", e);
		}
    }
    
    /**
     * Add the given dataset and all its children to the populations panel
     * 
     * @param dataset
     */
    private synchronized void addDataset(final IAnalysisDataset dataset) {

        mw.getPopulationsPanel().addDataset(dataset);
        for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
            mw.getPopulationsPanel().addDataset(child);
        }

        // This will also trigger a dataset update event as the dataset
        // is selected, so don't trigger another update here.
        mw.getPopulationsPanel().update(dataset);
    }

    /**
     * Begin a refolding of the consensus nucleus for the given dataset
     * 
     * @param dataset
     */
    private synchronized void refoldConsensus(final IAnalysisDataset dataset) {

        Runnable r = () -> {
            /*
             * The refold action needs to be able to hold up a series of
             * following actions, when it is being used in a New Analysis. The
             * countdown latch does nothing here, but must be retained for
             * compatibility.
             */

            final List<IAnalysisDataset> list = new ArrayList<IAnalysisDataset>();
            list.add(dataset);

            for (TabPanel p : mw.getTabPanels()) {
                if (p instanceof SegmentsDetailPanel || p instanceof NuclearStatisticsPanel
                        || p instanceof SignalsDetailPanel || p instanceof ConsensusNucleusPanel) {
                    p.clearChartCache(list);
                }
            }

            final CountDownLatch latch = new CountDownLatch(1);

            Runnable task = new RefoldNucleusAction(dataset, mw, latch);
            task.run();

            try {

            	latch.await();
            	Optional<IAnalysisOptions> op = dataset.getAnalysisOptions();
            	if(op.isPresent())
            		op.get().setRefoldNucleus(true);

                mw.getPopulationsPanel().selectDataset(dataset);

            } catch (InterruptedException e) {
                error("Interruption to thread", e);
            }
        };

        ThreadManager.getInstance().execute(r);
    }

    /**
     * Save all the root datasets in the populations panel
     */
    public synchronized void saveRootDatasets() {

        Runnable r = () -> {
            for (IAnalysisDataset root : DatasetListManager.getInstance().getRootDatasets()) {
                final CountDownLatch latch = new CountDownLatch(1);

                Runnable task = new SaveDatasetAction(root, mw, latch, false);
                task.run();
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    error("Interruption to thread", e);
                }
            }
            fine("All root datasets attempted to be saved");
        };

        ThreadManager.getInstance().execute(r);
    }

    /**
     * Save the given dataset. If it is root, save directly. If it is not root,
     * find the root parent and save it.
     * 
     * @param d
     * @param saveAs
     *            should the action ask for a directory
     */
    public synchronized void saveDataset(final IAnalysisDataset d, boolean saveAs) {

        if (d.isRoot()) {
            final CountDownLatch latch = new CountDownLatch(1);

            Runnable r = new SaveDatasetAction(d, mw, latch, saveAs);
            r.run();
        } else {
            IAnalysisDataset target = null;
            for (IAnalysisDataset root : DatasetListManager.getInstance().getRootDatasets()) {
                for (IAnalysisDataset child : root.getAllChildDatasets()) {
                    if (child.getId().equals(d.getId())) {
                        target = root;
                        break;
                    }
                }
                if (target != null) {
                    break;
                }
            }
            if (target != null) {
                saveDataset(target, saveAs);
            }
        }
    }

    /*
     * Trigger a recache of all charts and datasets
     */
    private synchronized void recacheCharts() {

        Runnable task = () -> {
            for (TabPanel panel : mw.getTabPanels()) {
                panel.refreshChartCache();
                panel.refreshTableCache();
            }
        };
        ThreadManager.getInstance().execute(task);
    }

    private synchronized void clearChartCache() {
        for (TabPanel panel : mw.getTabPanels()) {
            panel.clearChartCache();
            panel.clearTableCache();
        }
    }

    private synchronized void clearChartCache(final List<IAnalysisDataset> list) {

        if (list == null || list.isEmpty()) {
            warn("A cache clear was requested for a specific list, which was null or empty");
            clearChartCache();
            return;
        }
        for (TabPanel panel : mw.getTabPanels()) {
            panel.clearChartCache(list);
            panel.clearTableCache(list);
        }
    }


    private synchronized void recacheCharts(final List<IAnalysisDataset> list) {

        Runnable task = () -> {
            for (TabPanel panel : mw.getTabPanels()) {
                panel.refreshChartCache(list);
                panel.refreshTableCache(list);
            }
            fireDatasetUpdateEvent(DatasetListManager.getInstance().getSelectedDatasets()); // ensure all selected datasets get redrawn
        };
        ThreadManager.getInstance().submit(task);

    }

    public synchronized void addDatasetUpdateEventListener(DatasetUpdateEventListener l) {
        updateListeners.add(l);
    }

    public synchronized void removeDatasetUpdateEventListener(DatasetUpdateEventListener l) {
        updateListeners.remove(l);
    }

    /**
     * Signal listeners that the given datasets should be displayed
     * 
     * @param list
     */
    public synchronized void fireDatasetUpdateEvent(final List<IAnalysisDataset> list) {
        PanelUpdater r = new PanelUpdater(list);
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
                for (TabPanel p : mw.getTabPanels()) {
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
