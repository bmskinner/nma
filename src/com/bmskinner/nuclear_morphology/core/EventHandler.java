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
package com.bmskinner.nuclear_morphology.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace.BioSample;
import com.bmskinner.nuclear_morphology.components.workspaces.WorkspaceFactory;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.gui.ProgressBarAcceptor;
import com.bmskinner.nuclear_morphology.gui.actions.AddNuclearSignalAction;
import com.bmskinner.nuclear_morphology.gui.actions.BuildHierarchicalTreeAction;
import com.bmskinner.nuclear_morphology.gui.actions.ClusterAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.ClusterFileAssignmentAction;
import com.bmskinner.nuclear_morphology.gui.actions.DatasetArithmeticAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportCellLocationsAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportOptionsAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportStatsAction.ExportNuclearStatsAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportStatsAction.ExportShellsAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportStatsAction.ExportSignalsAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportWorkspaceAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExtractRandomCellsAction;
import com.bmskinner.nuclear_morphology.gui.actions.FishRemappingAction;
import com.bmskinner.nuclear_morphology.gui.actions.ImportDatasetAction;
import com.bmskinner.nuclear_morphology.gui.actions.ImportWorkflowAction;
import com.bmskinner.nuclear_morphology.gui.actions.ImportWorkspaceAction;
import com.bmskinner.nuclear_morphology.gui.actions.LobeDetectionAction;
import com.bmskinner.nuclear_morphology.gui.actions.MergeCollectionAction;
import com.bmskinner.nuclear_morphology.gui.actions.MergeSourceExtractionAction;
import com.bmskinner.nuclear_morphology.gui.actions.NewAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.RefoldNucleusAction;
import com.bmskinner.nuclear_morphology.gui.actions.RelocateFromFileAction;
import com.bmskinner.nuclear_morphology.gui.actions.ReplaceSourceImageDirectoryAction;
import com.bmskinner.nuclear_morphology.gui.actions.RunProfilingAction;
import com.bmskinner.nuclear_morphology.gui.actions.RunSegmentationAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportDatasetAction;
import com.bmskinner.nuclear_morphology.gui.actions.ShellAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.SingleDatasetResultAction;
import com.bmskinner.nuclear_morphology.gui.dialogs.collections.CellCollectionOverviewDialog;
import com.bmskinner.nuclear_morphology.gui.events.ChartOptionsRenderedEvent;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.events.DatasetUpdateEvent;
import com.bmskinner.nuclear_morphology.gui.events.EventListener;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent;
import com.bmskinner.nuclear_morphology.gui.events.InterfaceEvent.InterfaceMethod;
import com.bmskinner.nuclear_morphology.gui.events.PopulationListUpdateListener;
import com.bmskinner.nuclear_morphology.gui.events.PopulationListUpdateListener.PopulationListUpdateEvent;
import com.bmskinner.nuclear_morphology.gui.events.SignalChangeEvent;
import com.bmskinner.nuclear_morphology.gui.tabs.DatasetSelectionListener;
import com.bmskinner.nuclear_morphology.gui.tabs.DatasetSelectionListener.DatasetSelectionEvent;
import com.bmskinner.nuclear_morphology.io.CellFileExporter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Listens to messages from the UI and launches actions. This is the hub of messaging;
 * messages are passed from the UI to here, and dispatched back to the UI or to actions.
 * 
 * @author bms41
 * @since 1.13.7
 *
 */
public class EventHandler implements Loggable, EventListener {

	private final InputSupplier ic;
    private ProgressBarAcceptor acceptor;

    private final List<EventListener> updateListeners = new ArrayList<>();
    private final List<EventListener> interfaceListeners = new ArrayList<>();
    private final List<EventListener> datasetListeners = new ArrayList<>();
    private final List<DatasetSelectionListener> selectionListeners = new ArrayList<>();
    private final List<PopulationListUpdateListener> populationsListUpdateListeners = new ArrayList<>();

    
    /**
     * Constructor 
     */
    public EventHandler(@NonNull final InputSupplier context) {
    	ic = context;
    }


    /**
     * Constructor specifying a progress bar acceptor for displaying progress bars
     * @param acceptor
     */
    public EventHandler(@NonNull final InputSupplier context, @NonNull final ProgressBarAcceptor acceptor) {
    	this(context);
        this.acceptor = acceptor;
    }
    
    /**
     * The input context determines how the system will ask for user input
     * @return the current context
     */
    public InputSupplier getInputSupplier() {
    	return ic;
    }
    
    /**
     * Add the given progress bar acceptor to this handler
     * @param p
     */
    public void addProgressBarAcceptor(ProgressBarAcceptor p) {
    	acceptor = p;
    }
    
    public void addInterfaceEventListener(EventListener l) {
    	interfaceListeners.add(l);
    }
    
    private void fireInterfaceEvent(InterfaceEvent e) {
    	for(EventListener l : interfaceListeners) {
    		l.eventReceived(e);
    	}
    }
    
    public void addDatasetSelectionListener(DatasetSelectionListener l) {
    	selectionListeners.add(l);
    }
    
    private void fireDatasetSelectionEvent(IAnalysisDataset d) {
    	List<IAnalysisDataset> list = new ArrayList<>();
    	list.add(d);
    	fireDatasetSelectionEvent(list);
    }
    
    private void fireDatasetSelectionEvent(List<IAnalysisDataset> list) {
    	DatasetSelectionEvent e = new DatasetSelectionEvent(this, list);
    	for(DatasetSelectionListener l : selectionListeners) {
    		l.datasetSelectionEventReceived(e);
    	}
    }
    
    public void addDatasetEventListener(EventListener l) {
    	datasetListeners.add(l);
    }
        
    private void fireDatasetEvent(DatasetEvent event) {
    	for(EventListener l : datasetListeners) {
    		l.eventReceived(event);
    	}
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
        	
        	if (event.type().startsWith(SignalChangeEvent.IMPORT_WORKFLOW_PREFIX)) {
                String s = event.type().replace(SignalChangeEvent.IMPORT_WORKFLOW_PREFIX, "");
                if(s.equals(""))
                	return new ImportWorkflowAction(acceptor, EventHandler.this);
                File f = new File(s);
                return new ImportWorkflowAction(acceptor, EventHandler.this, f);
            }
        	
        	if (event.type().startsWith(SignalChangeEvent.IMPORT_DATASET_PREFIX)) {
                String s = event.type().replace(SignalChangeEvent.IMPORT_DATASET_PREFIX, "");
                if(s.equals(""))
                	return new ImportDatasetAction(acceptor, EventHandler.this);
                File f = new File(s);
                return new ImportDatasetAction(acceptor, EventHandler.this, f);
            }
            
            if (event.type().startsWith(SignalChangeEvent.IMPORT_WORKSPACE_PREFIX))
            	return () -> {
            		String s = event.type().replace(SignalChangeEvent.IMPORT_WORKSPACE_PREFIX, "");
            		if(s.equals("")) {
            			new ImportWorkspaceAction(acceptor, EventHandler.this).run();
            			return;
            		}
            		File f = new File(s);
            		new ImportWorkspaceAction(acceptor, EventHandler.this, f).run();
            	};

            	if (event.type().startsWith(SignalChangeEvent.NEW_ANALYSIS_PREFIX)) {
            		return () -> {
            			String s = event.type().replace(SignalChangeEvent.NEW_ANALYSIS_PREFIX, "");
            			if(s.equals(""))
            				return;
            			File f = new File(s);

            			new NewAnalysisAction(acceptor, EventHandler.this, f).run();;
            		};
            	}
            
            if (event.type().equals(SignalChangeEvent.NEW_WORKSPACE))
            	return () -> createWorkspace();
            	
        	if (event.type().equals(SignalChangeEvent.EXPORT_WORKSPACE))
                return new ExportWorkspaceAction(DatasetListManager.getInstance().getWorkspaces(), acceptor, EventHandler.this);
        	
        	if(selectedDataset==null)
        		return null;

            if (event.type().equals(SignalChangeEvent.DATASET_ARITHMETIC))
                return new DatasetArithmeticAction(selectedDatasets, acceptor, EventHandler.this);
            
            if (event.type().equals(SignalChangeEvent.EXTRACT_SUBSET))
                return new ExtractRandomCellsAction(selectedDataset, acceptor, EventHandler.this);

            if (event.type().equals(SignalChangeEvent.CHANGE_NUCLEUS_IMAGE_FOLDER))
                return new ReplaceSourceImageDirectoryAction(selectedDataset, acceptor, EventHandler.this);

            if (event.type().equals(SignalChangeEvent.ADD_NUCLEAR_SIGNAL))
                return new AddNuclearSignalAction(selectedDataset, acceptor, EventHandler.this);

            if (event.type().equals(SignalChangeEvent.POST_FISH_MAPPING))
                return new FishRemappingAction(selectedDatasets, acceptor, EventHandler.this);

            if (event.type().equals(SignalChangeEvent.EXPORT_STATS))
                return new ExportNuclearStatsAction(selectedDatasets, acceptor, EventHandler.this);
            
            if (event.type().equals(SignalChangeEvent.EXPORT_SIGNALS))
            	return new ExportSignalsAction(selectedDatasets, acceptor, EventHandler.this);
            
            if (event.type().equals(SignalChangeEvent.EXPORT_SHELLS))
                return new ExportShellsAction(selectedDatasets, acceptor, EventHandler.this);
            
            if (event.type().equals(SignalChangeEvent.EXPORT_OPTIONS))
                return new ExportOptionsAction(selectedDatasets, acceptor, EventHandler.this);

            if (event.type().equals(SignalChangeEvent.LOBE_DETECTION))
                return new LobeDetectionAction(selectedDataset, acceptor, EventHandler.this);
            
            if (event.type().equals(SignalChangeEvent.MERGE_DATASETS_ACTION))
                return new MergeCollectionAction(selectedDatasets, acceptor, EventHandler.this);

            if (event.type().equals(SignalChangeEvent.CHANGE_SCALE))
                return () -> setScale(selectedDatasets);
                
            if (event.type().equals(SignalChangeEvent.SAVE_SELECTED_DATASET))
            	return new ExportDatasetAction(selectedDataset, acceptor, EventHandler.this, null, true);
            	
            if (event.type().equals(SignalChangeEvent.SAVE_ALL_DATASETS))
                return () -> saveRootDatasets();
            	
        	if (event.type().equals(SignalChangeEvent.UPDATE_PANELS_WITH_NULL) )
        		return () -> fireDatasetUpdateEvent(new ArrayList<IAnalysisDataset>());
        		
        	if (event.type().equals(SignalChangeEvent.UPDATE_PANELS))
                return () -> fireDatasetUpdateEvent(selectedDatasets);
                
                
            if (event.type().equals(SignalChangeEvent.CURATE_DATASET))
            	return () ->{
	                CellCollectionOverviewDialog d = new CellCollectionOverviewDialog(selectedDataset);
	                d.addDatasetEventListener(EventHandler.this);
	            };
                            
            if (event.type().equals(SignalChangeEvent.EXPORT_CELL_LOCS))
            	return new ExportCellLocationsAction(selectedDatasets, acceptor, EventHandler.this);
                    
            if (event.type().startsWith(SignalChangeEvent.REMOVE_FROM_WORKSPACE_PREFIX))
            	return () ->{
            		String workspaceName = event.type().replace(SignalChangeEvent.REMOVE_FROM_WORKSPACE_PREFIX, "");
            		IWorkspace ws = DatasetListManager.getInstance().getWorkspaces().stream()
            				.filter(w->w.getName().equals(workspaceName)).findFirst().orElseThrow(IllegalArgumentException::new);
            		for(IAnalysisDataset d : DatasetListManager.getInstance().getRootParents(selectedDatasets))
            				ws.remove(d);
            		fireDatasetEvent(new DatasetEvent(this, DatasetEvent.ADD_WORKSPACE, "EventHandler", new ArrayList()));
            	};
            
        	if (event.type().startsWith(SignalChangeEvent.ADD_TO_WORKSPACE_PREFIX))
        		return () ->{
        			String workspaceName = event.type().replace(SignalChangeEvent.ADD_TO_WORKSPACE_PREFIX, "");
        			IWorkspace ws = DatasetListManager.getInstance().getWorkspaces().stream()
        					.filter(w->w.getName().equals(workspaceName)).findFirst().orElseThrow(IllegalArgumentException::new);
        			
        			for(IAnalysisDataset d : DatasetListManager.getInstance().getRootParents(selectedDatasets))
        				ws.add(d);
        			fireDatasetEvent(new DatasetEvent(this, DatasetEvent.ADD_WORKSPACE, "EventHandler", new ArrayList()));
        		};

    		if (event.type().startsWith(SignalChangeEvent.NEW_BIOSAMPLE_PREFIX))
    			return () ->{
    				fine("Creating new biosample");
    				try {
						String bsName = ic.requestString("New biosample name");
						List<IWorkspace> workspaces = DatasetListManager.getInstance().getWorkspaces(selectedDataset);
						for(IWorkspace w : workspaces) {
							w.addBioSample(bsName);
							BioSample bs = w.getBioSample(bsName);
							if(bs!=null)
								bs.addDataset(selectedDataset);
						}
						fireDatasetSelectionEvent(selectedDataset); // Using to trigger a refresh of the populations panel
					} catch (RequestCancelledException e) {
						fine("New biosample cancelled");
						return;
					}
    			};	
        		
    		
    		if (event.type().startsWith(SignalChangeEvent.REMOVE_FROM_BIOSAMPLE_PREFIX))
            	return () ->{
            		String bsName = event.type().replace(SignalChangeEvent.REMOVE_FROM_BIOSAMPLE_PREFIX, "");
            		fine("Removing dataset from biosample "+bsName);
            		List<IWorkspace> workspaces = DatasetListManager.getInstance().getWorkspaces(selectedDataset);
            		for(IWorkspace w : workspaces) {
            			BioSample b = w.getBioSample(bsName);
            			if(b!=null)
            				b.removeDataset(selectedDataset);
					}
					fireDatasetSelectionEvent(selectedDataset); // Using to trigger a refresh of the populations panel
            	};
            
        	if (event.type().startsWith(SignalChangeEvent.ADD_TO_BIOSAMPLE_PREFIX))
        		return () ->{
        			String bsName = event.type().replace(SignalChangeEvent.ADD_TO_BIOSAMPLE_PREFIX, "");
        			fine("Adding dataset to biosample "+bsName);
        			List<IWorkspace> workspaces = DatasetListManager.getInstance().getWorkspaces(selectedDataset);
					for(IWorkspace w : workspaces) {
						BioSample b = w.getBioSample(bsName);
            			if(b!=null)
            				b.addDataset(selectedDataset);
					}
					fireDatasetSelectionEvent(selectedDataset); // Using to trigger a refresh of the populations panel
        		};
            
            
            if (event.type().equals(SignalChangeEvent.RELOCATE_CELLS)) 
                return new RelocateFromFileAction(selectedDataset, acceptor, EventHandler.this, new CountDownLatch(1));

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

            // The full pipeline for a new analysis
            if (event.method().equals(DatasetEvent.MORPHOLOGY_ANALYSIS_ACTION)) {

            	return () ->{
            		
            		final CountDownLatch profileLatch = new CountDownLatch(1);
            		final CountDownLatch segmentLatch = new CountDownLatch(1);
            		final CountDownLatch refoldLatch  = new CountDownLatch(1);
            		final CountDownLatch saveLatch    = new CountDownLatch(1);
            		            		

            		new Thread( ()->{ // run profiling
            			new RunProfilingAction(selectedDatasets, SingleDatasetResultAction.NO_FLAG, acceptor, EventHandler.this, profileLatch).run();
            			
            		}).start();

            		new Thread( ()-> { // wait for profiling and run segmentation
            			try {
            				profileLatch.await();
            				fine("Starting segmentation action");
            				new RunSegmentationAction(selectedDatasets, MorphologyAnalysisMode.NEW, SingleDatasetResultAction.NO_FLAG,
            						acceptor, EventHandler.this, segmentLatch).run();
            			} catch(InterruptedException e) {
            				return;
            			}	
            		}).start();

            		new Thread( ()-> { // wait for segmentation and run refolding
            			try {
            				segmentLatch.await();
            				fine("Starting refolding action");
            				new RefoldNucleusAction(selectedDatasets, acceptor, EventHandler.this, refoldLatch).run();
            			} catch(InterruptedException e) {
            				return;
            			}
            		}).start();   
            		
            		new Thread( ()-> { // wait for refolding and run save
            			try {
            				refoldLatch.await();
            				fine("Starting save action");
            				new ExportDatasetAction(selectedDatasets, acceptor, EventHandler.this, saveLatch).run();
            			} catch(InterruptedException e) {
            				return;
            			}	
            		}).start();
            		

            		new Thread( ()-> { //  wait for save and recache charts
            			try {
            				saveLatch.await();
            				fine("Starting recache action");
            				fireDatasetEvent(new DatasetEvent(this, DatasetEvent.ADD_DATASET, "EventHandler", selectedDatasets));
            			} catch(InterruptedException e) {
            				return;
            			}
            		}).start();
            	};
            }

            if (event.method().equals(DatasetEvent.SEGMENTATION_ACTION))
                return new RunSegmentationAction(selectedDatasets, MorphologyAnalysisMode.NEW, 
                		SingleDatasetResultAction.NO_FLAG, acceptor, EventHandler.this);

            if (event.method().equals(DatasetEvent.REFRESH_MORPHOLOGY))
                return new RunSegmentationAction(selectedDatasets, MorphologyAnalysisMode.REFRESH, 
                		SingleDatasetResultAction.NO_FLAG, acceptor, EventHandler.this);
            
            if (event.method().equals(DatasetEvent.SAVE)) {

            	return () -> {
            		final CountDownLatch latch = new CountDownLatch(1);
            		new ExportDatasetAction(selectedDatasets, acceptor, EventHandler.this, latch).run();
            	};
            }
            
            // Run a completely new analysis on the dataset
            if (event.method().equals(DatasetEvent.REFPAIR_SEGMENTATION)) {

            	// begin a new morphology analysis
            	return () ->{
            		final CountDownLatch profileLatch = new CountDownLatch(1);
            		final CountDownLatch segmentLatch = new CountDownLatch(1);
            		new Thread( ()->{
            			new RunProfilingAction(selectedDatasets, SingleDatasetResultAction.NO_FLAG, acceptor, EventHandler.this, profileLatch).run();
            		}).start();

            		new Thread( ()-> {
            			
            			try {
            				profileLatch.await();
            				new RunSegmentationAction(selectedDatasets, MorphologyAnalysisMode.NEW, 0, acceptor, EventHandler.this, segmentLatch).run();
            			} catch(InterruptedException e) {
            				return;
            			}
            			
            		}).start();
            		
            		new Thread( ()-> { //  wait for save and recache charts
            			try {
            				segmentLatch.await();
            				fine("Adding datasets");
            				fireDatasetEvent(new DatasetEvent(this, DatasetEvent.RECACHE_CHARTS, "EventHandler", selectedDatasets));
            			} catch(InterruptedException e) {
            				return;
            			}
            		}).start();

            	};
            }

            if (event.method().equals(DatasetEvent.RUN_SHELL_ANALYSIS)) {
                return new ShellAnalysisAction(event.firstDataset(), acceptor, EventHandler.this);
            }

            if (event.method().equals(DatasetEvent.COPY_PROFILE_SEGMENTATION)) {
            	// copy the profile segmentation from one dataset to another
            	return () ->{
            		final IAnalysisDataset source = event.secondaryDataset();
            		if (source == null)
            			return;
            		
            		final CountDownLatch segmentLatch = new CountDownLatch(1);
            		new Thread( ()-> { // wait for profiling and run segmentation
            				fine("Starting segmentation action");
            				new RunSegmentationAction(selectedDatasets, source, SingleDatasetResultAction.NO_FLAG,
                    				acceptor, EventHandler.this, segmentLatch).run();
            		}).start();

            		new Thread( ()-> { //  wait for save and recache charts
            			try {
            				segmentLatch.await();
            				fine("Adding datasets");
            				fireDatasetEvent(new DatasetEvent(this, DatasetEvent.ADD_DATASET, "EventHandler", selectedDatasets));
            			} catch(InterruptedException e) {
            				return;
            			}
            		}).start();
            	};
            }

            if (event.method().equals(DatasetEvent.CLUSTER)) {
                log("Clustering dataset");
                return new ClusterAnalysisAction(event.firstDataset(), acceptor, EventHandler.this);

            }
            
            if (event.method().equals(DatasetEvent.CLUSTER_FROM_FILE))
                return new ClusterFileAssignmentAction(event.firstDataset(), acceptor, EventHandler.this);

            if (event.method().equals(DatasetEvent.BUILD_TREE)) {
                log("Building a tree from dataset");
                return new BuildHierarchicalTreeAction(event.firstDataset(), acceptor, EventHandler.this);
            }

            if (event.method().equals(DatasetEvent.RECALCULATE_MEDIAN))
                return new RunProfilingAction(selectedDatasets, SingleDatasetResultAction.NO_FLAG, acceptor, EventHandler.this);
            
            
            if (event.method().equals(DatasetEvent.EXTRACT_SOURCE))
                return new MergeSourceExtractionAction(event.getDatasets(), acceptor, EventHandler.this);
            
            if (event.method().equals(DatasetEvent.REFOLD_CONSENSUS)) {
            	return () ->{
            		final CountDownLatch refoldLatch  = new CountDownLatch(1);
            		new Thread( ()-> { // run refolding
            			Runnable task = new RefoldNucleusAction(selectedDatasets, acceptor, EventHandler.this, refoldLatch);
            			task.run();
            		}).start();   
            		
            		new Thread( ()-> { // wait for refolding and recache charts
            			try {
            				refoldLatch.await();
            				fireDatasetEvent(new DatasetEvent(this, DatasetEvent.RECACHE_CHARTS, "EventHandler", selectedDatasets));
            			} catch(InterruptedException e) {
            				return;
            			}	
            		}).start();
            	};

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
            if (r != null)
                r.run();
        }

        /**
         * Create and run an action for the given event
         * 
         * @param event
         */
        public synchronized void run(final DatasetEvent event) {
            Runnable r = create(event);
            if (r != null)
                r.run();
        }
    }

    @Override
    public synchronized void eventReceived(final SignalChangeEvent event) {
        new ActionFactory().run(event);
    }

    @Override
    public synchronized void eventReceived(final DatasetEvent event) {

        // Try to launch via factory
        new ActionFactory().run(event);

        // Remaining methods
        final List<IAnalysisDataset> list = event.getDatasets();
        if (!list.isEmpty()) {

            if (event.method().equals(DatasetEvent.SELECT_DATASETS))
            	fireDatasetSelectionEvent(event.getDatasets());

            if (event.method().equals(DatasetEvent.SELECT_ONE_DATASET))
            	fireDatasetSelectionEvent(event.firstDataset());

            if (event.method().equals(DatasetEvent.RECACHE_CHARTS))
            	fireDatasetEvent(event);

            if (event.method().equals(DatasetEvent.CLEAR_CACHE))
            	fireDatasetEvent(event);

            if (event.method().equals(DatasetEvent.ADD_DATASET)) {
            	fireDatasetEvent(event);
            }

        }

    }

    @Override
    public synchronized void eventReceived(final InterfaceEvent event) {

    	fireInterfaceEvent(event); //pass onwards to registered listeners - only MainWindow at present
        InterfaceMethod method = event.method();
        
        final List<IAnalysisDataset> selected = DatasetListManager.getInstance().getSelectedDatasets();

        switch (method) {

        case SAVE_ROOT:
            saveRootDatasets(); // DO NOT WRAP IN A SEPARATE THREAD, IT WILL
                                // LOCK THE PROGRESS BAR

            break;

        case UPDATE_PANELS: {
            fireDatasetUpdateEvent(selected);
            break;
        }

       
        case LIST_SELECTED_DATASETS:
            int count = 0;
            for (IAnalysisDataset d : selected) {
                log(count + "\t" + d.getName());
                count++;
            }
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
            
        case REFRESH_POPULATIONS: {
        	firePopulationListUpdateEvent();
        	break;
        }
        
        case RECACHE_CHARTS:{
        	fireInterfaceEvent(InterfaceEvent.of(this, event.method())); // pass to main window. Change source so it is not ignored
        	break;
        }
            
        default:
            break;

        }
    }

    
    private synchronized void setScale(final List<IAnalysisDataset> selectedDatasets) {
    	
    	try {
			double scale = ic.requestDouble("Pixels per micron", 1d, 1d, 100000d, 1d);
			
			if (scale > 0) { // don't allow a scale to cause divide by zero errors
				selectedDatasets.stream().forEach(d->d.getCollection().setScale(scale));
				log("Updated scale to "+scale+" pixels per micron");
				eventReceived(new InterfaceEvent(this, InterfaceMethod.RECACHE_CHARTS, "Scale change"));
			}
				
		} catch (RequestCancelledException e) {
			return;
		}
    }
        
    private void createWorkspace() {

		try {
			String workspaceName = ic.requestString("New workspace name");
			IWorkspace w = WorkspaceFactory.createWorkspace(workspaceName);
			DatasetListManager.getInstance().addWorkspace(w);
    		log("New workspace created: "+workspaceName);
    		fireDatasetEvent(new DatasetEvent(this, DatasetEvent.ADD_WORKSPACE, "EventHandler", new ArrayList()));
			
		} catch (RequestCancelledException e) {
			return;
		}
    	
    }
    
    
    /**
     * Save all the root datasets in the populations panel
     */
    public synchronized void saveRootDatasets() {

    	Runnable r = () -> {
    		for (IAnalysisDataset root : DatasetListManager.getInstance().getRootDatasets()) {
    			final CountDownLatch latch = new CountDownLatch(1);

    			new Thread( () ->{
    				Runnable task = new ExportDatasetAction(root, acceptor, EventHandler.this, latch, false);
    				task.run();
    				try {
    					latch.await();
    				} catch (InterruptedException e) {
    					error("Interruption to thread", e);
    				}

    				Runnable wrk = new ExportWorkspaceAction(DatasetListManager.getInstance().getWorkspaces(), acceptor, EventHandler.this);
    				wrk.run();
    			}).start();
    		}
    		fine("All root datasets attempted to be saved");
    	};

        ThreadManager.getInstance().execute(r);
    }

    public synchronized void addDatasetUpdateEventListener(EventListener l) {
        updateListeners.add(l);
    }

    public synchronized void removeDatasetUpdateEventListener(EventListener l) {
        updateListeners.remove(l);
    }
//
//    /**
//     * Signal listeners that the given datasets should be displayed
//     * 
//     * @param list
//     */
    public synchronized void fireDatasetUpdateEvent(final List<IAnalysisDataset> list) {
        for(EventListener l : updateListeners) {
        	l.eventReceived(new DatasetUpdateEvent(this, list));
        }
    }
    
    public synchronized void addPopulationListUpdateListener(PopulationListUpdateListener l) {
    	populationsListUpdateListeners.add(l);
    }

    public synchronized void removePopulationListUpdateListener(PopulationListUpdateListener l) {
    	populationsListUpdateListeners.remove(l);
    }
    
    public void firePopulationListUpdateEvent() {
    	PopulationListUpdateEvent e = new PopulationListUpdateEvent(this);
    	for(PopulationListUpdateListener l : populationsListUpdateListeners) {
    		l.populationListUpdateEventReceived(e);
    	}
    }


	@Override
	public void eventReceived(DatasetUpdateEvent event) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void eventReceived(ChartOptionsRenderedEvent event) {
		// TODO Auto-generated method stub
		
	}
    
    

}
