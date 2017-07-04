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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import com.bmskinner.nuclear_morphology.analysis.MergeSourceExtractor;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.IMutableAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.gui.CancellableRunnable;
import com.bmskinner.nuclear_morphology.gui.ConsensusNucleusPanel;
import com.bmskinner.nuclear_morphology.gui.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.DatasetEventListener;
import com.bmskinner.nuclear_morphology.gui.DatasetListManager;
import com.bmskinner.nuclear_morphology.gui.DatasetUpdateEvent;
import com.bmskinner.nuclear_morphology.gui.DatasetUpdateEventListener;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent;
import com.bmskinner.nuclear_morphology.gui.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.InterfaceEventListener;
import com.bmskinner.nuclear_morphology.gui.MainWindow;
import com.bmskinner.nuclear_morphology.gui.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.SignalChangeListener;
import com.bmskinner.nuclear_morphology.gui.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.actions.AddNuclearSignalAction;
import com.bmskinner.nuclear_morphology.gui.actions.BuildHierarchicalTreeAction;
import com.bmskinner.nuclear_morphology.gui.actions.ClusterAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.DatasetArithmeticAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportStatsAction;
import com.bmskinner.nuclear_morphology.gui.actions.FishRemappingAction;
import com.bmskinner.nuclear_morphology.gui.actions.LobeDetectionAction;
import com.bmskinner.nuclear_morphology.gui.actions.MergeCollectionAction;
import com.bmskinner.nuclear_morphology.gui.actions.NewAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.PopulationImportAction;
import com.bmskinner.nuclear_morphology.gui.actions.RefoldNucleusAction;
import com.bmskinner.nuclear_morphology.gui.actions.RelocateFromFileAction;
import com.bmskinner.nuclear_morphology.gui.actions.ReplaceSourceImageDirectoryAction;
import com.bmskinner.nuclear_morphology.gui.actions.RunProfilingAction;
import com.bmskinner.nuclear_morphology.gui.actions.RunSegmentationAction;
import com.bmskinner.nuclear_morphology.gui.actions.SaveDatasetAction;
import com.bmskinner.nuclear_morphology.gui.actions.SaveWorkspaceAction;
import com.bmskinner.nuclear_morphology.gui.actions.ShellAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.SingleDatasetResultAction;
import com.bmskinner.nuclear_morphology.gui.dialogs.collections.CellCollectionOverviewDialog;
import com.bmskinner.nuclear_morphology.gui.tabs.NuclearStatisticsPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.SegmentsDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.SignalsDetailPanel;
import com.bmskinner.nuclear_morphology.gui.tabs.TabPanel;
import com.bmskinner.nuclear_morphology.io.MappingFileExporter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

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
        final IAnalysisDataset selectedDataset;
        List<IAnalysisDataset> selectedDatasets;

        public ActionFactory() {

            selectedDatasets = mw.getPopulationsPanel().getSelectedDatasets();
            selectedDataset = mw.getPopulationsPanel().getSelectedDatasets().isEmpty() ? null
                    : mw.getPopulationsPanel().getSelectedDatasets().get(0);

        }

        /**
         * Create a runnable action for the given event
         * 
         * @param event
         * @return
         */
        public Runnable create(final SignalChangeEvent event) {

            if (event.type().equals(SignalChangeEvent.EXPORT_WORKSPACE)) {
                return new SaveWorkspaceAction(DatasetListManager.getInstance().getRootDatasets(), mw);
            }

            if (event.type().equals(SignalChangeEvent.DATASET_ARITHMETIC)) {
                return new DatasetArithmeticAction(selectedDatasets, mw);
            }

            if (event.type().equals(SignalChangeEvent.CHANGE_NUCLEUS_IMAGE_FOLDER)) {
                return new ReplaceSourceImageDirectoryAction(selectedDataset, mw);
            }

            if (event.type().equals(SignalChangeEvent.ADD_NUCLEAR_SIGNAL)) {
                return new AddNuclearSignalAction(selectedDataset, mw);
            }

            if (event.type().equals(SignalChangeEvent.POST_FISH_MAPPING)) {
                return new FishRemappingAction(selectedDatasets, mw);
            }

            if (event.type().equals(SignalChangeEvent.EXPORT_STATS)) {

                return new ExportStatsAction(selectedDatasets, mw);
            }

            if (event.type().equals(SignalChangeEvent.LOBE_DETECTION)) {
                return new LobeDetectionAction(selectedDataset, mw);
            }

            if (event.type().startsWith("Open|")) {
                String s = event.type().replace("Open|", "");
                File f = new File(s);

                return new PopulationImportAction(mw, f);
            }

            if (event.type().startsWith("New|")) {
                String s = event.type().replace("New|", "");
                File f = new File(s);

                return new NewAnalysisAction(mw, f);
            }

            return null;
        }

        /**
         * Create a runnable action for the given event
         * 
         * @param event
         * @return
         */
        public Runnable create(final DatasetEvent event) {

            if (event.getDatasets().isEmpty()) {
                return null;
            }

            selectedDatasets = event.getDatasets();

            if (event.method().equals(DatasetEvent.PROFILING_ACTION)) {
                fine("Creating new profiling and segmentation");

                int flag = 0; // set the downstream analyses to run
                flag |= SingleDatasetResultAction.ADD_POPULATION;
                flag |= SingleDatasetResultAction.STATS_EXPORT;
                flag |= SingleDatasetResultAction.NUCLEUS_ANNOTATE;
                flag |= SingleDatasetResultAction.ASSIGN_SEGMENTS;

                try {
                    if (event.firstDataset().getAnalysisOptions().refoldNucleus()) {
                        flag |= SingleDatasetResultAction.CURVE_REFOLD;
                    }
                } catch (MissingOptionException e) {
                    warn("Missing analysis options");
                    stack(e.getMessage(), e);
                    return null;
                }
                // begin a recursive morphology analysis
                return new RunProfilingAction(selectedDatasets, flag, mw);
            }

            if (event.method().equals(DatasetEvent.NEW_MORPHOLOGY)) {
                log("Running new morphology analysis");
                final int flag = SingleDatasetResultAction.ADD_POPULATION;

                return new RunSegmentationAction(selectedDatasets, MorphologyAnalysisMode.NEW, flag, mw);
            }

            if (event.method().equals(DatasetEvent.REFRESH_MORPHOLOGY)) {
                finer("Refreshing segmentation across nuclei using existing border tags");
                final int flag = 0;
                return new RunSegmentationAction(selectedDatasets, MorphologyAnalysisMode.REFRESH, flag, mw);
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

            if (event.method().equals(DatasetEvent.BUILD_TREE)) {
                log("Building a tree from dataset");
                return new BuildHierarchicalTreeAction(event.firstDataset(), mw);
            }

            if (event.method().equals(DatasetEvent.RECALCULATE_MEDIAN)) {
                fine("Recalculating the median for the given datasets");

                return new RunProfilingAction(selectedDatasets, SingleDatasetResultAction.NO_FLAG, mw);
            }

            return null;
        }

        /**
         * Create and run an action for the given event
         * 
         * @param event
         */
        public void run(final SignalChangeEvent event) {
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
        public void run(final DatasetEvent event) {
            Runnable r = create(event);
            if (r != null) {
                r.run();
            }
        }
    }

    @Override
    public void signalChangeReceived(final SignalChangeEvent event) {

        finer("Heard signal change event: " + event.type());

        final IAnalysisDataset selectedDataset = mw.getPopulationsPanel().getSelectedDatasets().isEmpty() ? null
                : mw.getPopulationsPanel().getSelectedDatasets().get(0);

        // Try to launch via factory
        new ActionFactory().run(event);

        if (event.type().equals("MergeCollectionAction")) {

            Runnable task = new MergeCollectionAction(mw.getPopulationsPanel().getSelectedDatasets(), mw);
            ThreadManager.getInstance().execute(task);
        }

        if (event.type().equals("CurateCollectionAction")) {

            CellCollectionOverviewDialog d = new CellCollectionOverviewDialog(
                    mw.getPopulationsPanel().getSelectedDatasets().get(0));
            d.addDatasetEventListener(this);

        }

        if (event.type().equals("SaveCellLocations")) {
            log("Exporting cell locations...");
            if (new MappingFileExporter().exportCellLocations(selectedDataset)) {
                log("Export complete");
            } else {
                log("Export failed");
            }

        }

        if (event.type().equals("RelocateCellsAction")) {

            CountDownLatch latch = new CountDownLatch(1);
            Runnable r = new RelocateFromFileAction(selectedDataset, mw, latch);
            r.run();

        }

        if (event.type().equals("UpdatePanels")) {
            fireDatasetUpdateEvent(mw.getPopulationsPanel().getSelectedDatasets());
        }

        if (event.type().equals("UpdatePanelsNull")) {
            fireDatasetUpdateEvent(new ArrayList<IAnalysisDataset>());
        }

        if (event.type().equals("UpdatePopulationPanel")) {
            this.mw.getPopulationsPanel().update(mw.getPopulationsPanel().getSelectedDatasets());
        }

        if (event.type().equals("SaveCollectionAction")) {

            this.saveDataset(selectedDataset, true);
        }

    }

    @Override
    public void datasetEventReceived(final DatasetEvent event) {

        // Try to launch via factory
        new ActionFactory().run(event);

        // Remaining methods
        final List<IAnalysisDataset> list = event.getDatasets();
        if (!list.isEmpty()) {

            if (event.method().equals(DatasetEvent.REFOLD_CONSENSUS)) {
                refoldConsensus(event.firstDataset());
            }

            if (event.method().equals(DatasetEvent.SELECT_DATASETS)) {
                mw.getPopulationsPanel().selectDatasets(event.getDatasets());
            }

            if (event.method().equals(DatasetEvent.SELECT_ONE_DATASET)) {
                mw.getPopulationsPanel().selectDataset(event.firstDataset());
            }

            if (event.method().equals(DatasetEvent.SAVE)) {
                saveDataset(event.firstDataset(), false);
            }

            if (event.method().equals(DatasetEvent.EXTRACT_SOURCE)) {
                MergeSourceExtractor ext = new MergeSourceExtractor(list);
                ext.addDatasetEventListener(this);
                ext.extractSourceDataset();
                ;

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
    public void interfaceEventReceived(final InterfaceEvent event) {

        InterfaceMethod method = event.method();
        finest("Heard interface event: " + event.method().toString());

        final List<IAnalysisDataset> selected = mw.getPopulationsPanel().getSelectedDatasets();

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
            // threadManager.executeAndCancelUpdate( new PanelUpdateTask(list)
            // );
            // this.updatePanels(list);
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

    /**
     * Add the given dataset and all its children to the populations panel
     * 
     * @param dataset
     */
    private void addDataset(final IAnalysisDataset dataset) {

        fine("Adding dataset");

        mw.getPopulationsPanel().addDataset(dataset);
        for (IAnalysisDataset child : dataset.getAllChildDatasets()) {
            mw.getPopulationsPanel().addDataset(child);
        }

        finer("Ordering update of populations panel");

        // This will also trigger a dataset update event as the dataset
        // is selected, so don't trigger another update here.
        mw.getPopulationsPanel().update(dataset);
    }

    /**
     * Begin a refolding of the consensus nucleus for the given dataset
     * 
     * @param dataset
     */
    private void refoldConsensus(final IAnalysisDataset dataset) {
        fine("Refolding consensus");
        finest("Refold consensus dataset method is EDT: " + SwingUtilities.isEventDispatchThread());

        Runnable r = () -> {
            /*
             * The refold action needs to be able to hold up a series of
             * following actions, when it is being used in a New Analysis. The
             * countdown latch does nothing here, but must be retained for
             * compatibility.
             */
            fine("Clearing chart cache for consensus charts");

            final List<IAnalysisDataset> list = new ArrayList<IAnalysisDataset>();
            list.add(dataset);

            for (TabPanel p : mw.getTabPanels()) {
                if (p instanceof SegmentsDetailPanel || p instanceof NuclearStatisticsPanel
                        || p instanceof SignalsDetailPanel || p instanceof ConsensusNucleusPanel) {
                    p.clearChartCache(list);
                }
            }

            final CountDownLatch latch = new CountDownLatch(1);
            finest("Created latch: " + latch.getCount());
            Runnable task = new RefoldNucleusAction(dataset, mw, latch);
            task.run();
            finest("Running refolding");
            try {
                fine("Awaiting latch");
                latch.await();
                fine("Latch has released from refolding");
                if (dataset.hasAnalysisOptions()) {

                    IMutableAnalysisOptions op;
                    try {
                        op = (IMutableAnalysisOptions) dataset.getAnalysisOptions();
                        op.setRefoldNucleus(true);
                        fine("Set refold status in options");
                    } catch (MissingOptionException e) {
                        warn("Missing analysis options");
                        stack(e.getMessage(), e);

                    }

                } else {
                    fine("Dataset has no analysis options, cannot set refold state");
                }

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
    public void saveRootDatasets() {

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
    public void saveDataset(final IAnalysisDataset d, boolean saveAs) {

        if (d.isRoot()) {
            finer("Dataset is root");
            finest("Creating latch");
            final CountDownLatch latch = new CountDownLatch(1);

            Runnable r = new SaveDatasetAction(d, mw, latch, saveAs);
            finest("Passing save action to executor service");
            r.run();

            fine("Root dataset saved");
        } else {
            finest("Not a root dataset, checking for parent");
            IAnalysisDataset target = null;
            for (IAnalysisDataset root : DatasetListManager.getInstance().getRootDatasets()) {
                // for(AnalysisDataset root :
                // populationsPanel.getRootDatasets()){

                for (IAnalysisDataset child : root.getAllChildDatasets()) {
                    if (child.getUUID().equals(d.getUUID())) {
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
    private void recacheCharts() {

        Runnable task = () -> {
            for (TabPanel panel : mw.getTabPanels()) {
                panel.refreshChartCache();
                panel.refreshTableCache();
            }
        };
        ThreadManager.getInstance().execute(task);
    }

    private void clearChartCache() {
        for (TabPanel panel : mw.getTabPanels()) {
            panel.clearChartCache();
            panel.clearTableCache();
        }
    }

    private void clearChartCache(final List<IAnalysisDataset> list) {

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

    // private void recacheCharts(final AnalysisDataset dataset){
    // final List<AnalysisDataset> list = new ArrayList<AnalysisDataset>();
    // list.add(dataset);
    // recacheCharts(list);
    // }

    private void recacheCharts(final List<IAnalysisDataset> list) {

        Runnable task = () -> {
            fine("Heard recache request for list of  " + list.size() + " datasets");
            for (TabPanel panel : mw.getTabPanels()) {
                panel.refreshChartCache(list);
                panel.refreshTableCache(list);
            }
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
    public void fireDatasetUpdateEvent(final List<IAnalysisDataset> list) {
        fine("Heard dataset update event fire");
        PanelUpdater r = new PanelUpdater(list);
        ThreadManager.getInstance().executeAndCancelUpdate(r);
    }

    public class PanelUpdater implements CancellableRunnable {
        private final List<IAnalysisDataset> list;

        private AtomicBoolean isCancelled = new AtomicBoolean(false);

        public PanelUpdater(final List<IAnalysisDataset> list) {
            this.list = list;
        }

        @Override
        public void run() {
            PanelLoadingUpdater loader = new PanelLoadingUpdater();

            try {

                Future<?> f = ThreadManager.getInstance().submit(loader);

                // Wait for loading state to be set
                while (!f.isDone() && !isCancelled.get()) {
                    fine("Waiting for chart loading set...");
                    Thread.sleep(1);
                }

                if (isCancelled.get()) {
                    return;
                }

                Boolean ok = (Boolean) f.get();
                fine("Chart loading is set: " + ok);

            } catch (InterruptedException e1) {
                warn("Interrupted update");
                error("Error setting loading state", e1);
                error("Cause of loading state error", e1.getCause());
                return;
            } catch (ExecutionException e1) {
                error("Error setting loading state", e1);
                error("Cause of loading state error", e1.getCause());
                return;
            }

            // Now fire the update
            fine("Firing general update for " + list.size() + " datasets");
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
            fine("Cancelling thread");
            isCancelled.set(true);
            // Thread.currentThread().interrupt();

        }

    }

    public class PanelLoadingUpdater implements Callable {
        public PanelLoadingUpdater() {

        }

        @Override
        public Boolean call() {

            // log("Setting charts and tables loading");
            // Update charts and panels to loading

            for (TabPanel p : mw.getTabPanels()) {
                p.setChartsAndTablesLoading();
            }
            return true;

        }

    }

}
