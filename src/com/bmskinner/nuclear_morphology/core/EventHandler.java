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
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
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
import com.bmskinner.nuclear_morphology.gui.actions.ExportDatasetAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportOptionsAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportRuleSetsAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportSingleCellImagesAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportStatsAction.ExportNuclearOutlinesAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportStatsAction.ExportNuclearProfilesAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportStatsAction.ExportNuclearStatsAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportStatsAction.ExportShellsAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportStatsAction.ExportSignalsAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportTPSAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExportWorkspaceAction;
import com.bmskinner.nuclear_morphology.gui.actions.ExtractRandomCellsAction;
import com.bmskinner.nuclear_morphology.gui.actions.FishRemappingAction;
import com.bmskinner.nuclear_morphology.gui.actions.ImportDatasetAction;
import com.bmskinner.nuclear_morphology.gui.actions.ImportWorkflowAction;
import com.bmskinner.nuclear_morphology.gui.actions.ImportWorkspaceAction;
import com.bmskinner.nuclear_morphology.gui.actions.ManualClusterAction;
import com.bmskinner.nuclear_morphology.gui.actions.MergeCollectionAction;
import com.bmskinner.nuclear_morphology.gui.actions.MergeSignalsAction;
import com.bmskinner.nuclear_morphology.gui.actions.MergeSourceExtractionAction;
import com.bmskinner.nuclear_morphology.gui.actions.NewAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.RefoldNucleusAction;
import com.bmskinner.nuclear_morphology.gui.actions.RelocateFromFileAction;
import com.bmskinner.nuclear_morphology.gui.actions.ReplaceSourceImageDirectoryAction;
import com.bmskinner.nuclear_morphology.gui.actions.RunGLCMAction;
import com.bmskinner.nuclear_morphology.gui.actions.RunProfilingAction;
import com.bmskinner.nuclear_morphology.gui.actions.RunSegmentationAction;
import com.bmskinner.nuclear_morphology.gui.actions.ShellAnalysisAction;
import com.bmskinner.nuclear_morphology.gui.actions.SingleDatasetResultAction;
import com.bmskinner.nuclear_morphology.gui.dialogs.collections.AbstractCellCollectionDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.collections.ManualCurationDialog;
import com.bmskinner.nuclear_morphology.gui.events.DatasetEvent;
import com.bmskinner.nuclear_morphology.gui.events.DatasetUpdateEvent;
import com.bmskinner.nuclear_morphology.gui.events.EventListener;
import com.bmskinner.nuclear_morphology.gui.events.UserActionEvent;
import com.bmskinner.nuclear_morphology.gui.events.revamp.UIController;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Listens to messages from the UI and launches actions. This is the hub of
 * messaging; messages are passed from the UI to here, and dispatched back to
 * the UI or to actions.
 * 
 * @author bms41
 * @since 1.13.7
 *
 */
public class EventHandler implements EventListener {

	private static final Logger LOGGER = Logger.getLogger(EventHandler.class.getName());

	private final InputSupplier ic;
	private ProgressBarAcceptor acceptor;

	private final List<EventListener> updateListeners = new ArrayList<>();
	private final List<EventListener> datasetListeners = new ArrayList<>();

	/**
	 * Constructor
	 */
	public EventHandler(@NonNull final InputSupplier context) {
		ic = context;
	}

	/**
	 * Constructor specifying a progress bar acceptor for displaying progress bars
	 * 
	 * @param acceptor
	 */
	public EventHandler(@NonNull final InputSupplier context, @NonNull final ProgressBarAcceptor acceptor) {
		this(context);
		this.acceptor = acceptor;
	}

	/**
	 * The input context determines how the system will ask for user input
	 * 
	 * @return the current context
	 */
	public InputSupplier getInputSupplier() {
		return ic;
	}

	/**
	 * Add the given progress bar acceptor to this handler
	 * 
	 * @param p
	 */
	public void addProgressBarAcceptor(ProgressBarAcceptor p) {
		acceptor = p;
	}

//	public void addDatasetSelectionListener(DatasetSelectionListener l) {
//		selectionListeners.add(l);
//	}

//	private void fireDatasetSelectionEvent(IAnalysisDataset d) {
//		List<IAnalysisDataset> list = new ArrayList<>();
//		list.add(d);
//		fireDatasetSelectionEvent(list);
//	}

//	private void fireDatasetSelectionEvent(List<IAnalysisDataset> list) {
//		DatasetSelectionEvent e = new DatasetSelectionEvent(this, list);
//		for (DatasetSelectionListener l : selectionListeners) {
//			l.datasetSelectionEventReceived(e);
//		}
//	}

	public void addDatasetEventListener(EventListener l) {
		datasetListeners.add(l);
	}

	private void fireDatasetEvent(DatasetEvent event) {
		for (EventListener l : datasetListeners) {
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

		public ActionFactory() {
			// No fields
		}

		/**
		 * Create a runnable action for the given event
		 * 
		 * @param event
		 * @return
		 */
		public synchronized Runnable create(final UserActionEvent event) {

			final List<IAnalysisDataset> selectedDatasets = DatasetListManager.getInstance().getSelectedDatasets();
			final IAnalysisDataset selectedDataset = selectedDatasets.isEmpty() ? null : selectedDatasets.get(0);

			if (event.type().startsWith(UserActionEvent.IMPORT_WORKFLOW_PREFIX)) {
				String s = event.type().replace(UserActionEvent.IMPORT_WORKFLOW_PREFIX, "");

				// No image folder specified; will be requested in workflow
				if (s.equals(""))
					return new ImportWorkflowAction(acceptor, EventHandler.this);

				// Image folder specified
				File f = new File(s);
				return new ImportWorkflowAction(acceptor, EventHandler.this, f);
			}

			if (event.type().startsWith(UserActionEvent.IMPORT_DATASET_PREFIX)) {
				String s = event.type().replace(UserActionEvent.IMPORT_DATASET_PREFIX, "");
				if (s.equals(""))
					return new ImportDatasetAction(acceptor, EventHandler.this);
				File f = new File(s);
				return new ImportDatasetAction(acceptor, EventHandler.this, f);
			}

			if (event.type().startsWith(UserActionEvent.IMPORT_WORKSPACE_PREFIX))
				return () -> {
					String s = event.type().replace(UserActionEvent.IMPORT_WORKSPACE_PREFIX, "");
					if (s.equals("")) {
						new ImportWorkspaceAction(acceptor, EventHandler.this).run();
						return;
					}
					File f = new File(s);
					new ImportWorkspaceAction(acceptor, EventHandler.this, f).run();
				};

			if (event.type().startsWith(UserActionEvent.NEW_ANALYSIS_PREFIX)) {
				return () -> {
					String s = event.type().replace(UserActionEvent.NEW_ANALYSIS_PREFIX, "");
					if (s.equals(""))
						return;
					File f = new File(s);

					new NewAnalysisAction(acceptor, EventHandler.this, f).run();
				};
			}

			if (event.type().equals(UserActionEvent.NEW_WORKSPACE))
				return () -> createWorkspace();

			if (event.type().equals(UserActionEvent.EXPORT_WORKSPACE))
				return new ExportWorkspaceAction(DatasetListManager.getInstance().getWorkspaces(), acceptor,
						EventHandler.this);

			if (selectedDataset == null)
				return null;

			if (event.type().equals(UserActionEvent.DATASET_ARITHMETIC))
				return new DatasetArithmeticAction(selectedDatasets, acceptor, EventHandler.this);

			if (event.type().equals(UserActionEvent.EXTRACT_SUBSET))
				return new ExtractRandomCellsAction(selectedDataset, acceptor, EventHandler.this);

			if (event.type().equals(UserActionEvent.CHANGE_NUCLEUS_IMAGE_FOLDER))
				return new ReplaceSourceImageDirectoryAction(selectedDataset, acceptor, EventHandler.this);

			if (event.type().equals(UserActionEvent.ADD_NUCLEAR_SIGNAL))
				return new AddNuclearSignalAction(selectedDataset, acceptor, EventHandler.this);

			if (event.type().equals(UserActionEvent.CLUSTER_FROM_FILE))
				return new ClusterFileAssignmentAction(selectedDataset, acceptor, EventHandler.this);

			if (event.type().equals(UserActionEvent.POST_FISH_MAPPING))
				return new FishRemappingAction(selectedDatasets, acceptor, EventHandler.this);

			if (event.type().equals(UserActionEvent.EXPORT_STATS))
				return new ExportNuclearStatsAction(selectedDatasets, acceptor, EventHandler.this);

			if (event.type().equals(UserActionEvent.EXPORT_PROFILES))
				return new ExportNuclearProfilesAction(selectedDatasets, acceptor, EventHandler.this);

			if (event.type().equals(UserActionEvent.EXPORT_OUTLINES))
				return new ExportNuclearOutlinesAction(selectedDatasets, acceptor, EventHandler.this);

			if (event.type().equals(UserActionEvent.EXPORT_SIGNALS))
				return new ExportSignalsAction(selectedDatasets, acceptor, EventHandler.this);

			if (event.type().equals(UserActionEvent.EXPORT_SHELLS))
				return new ExportShellsAction(selectedDatasets, acceptor, EventHandler.this);

			if (event.type().equals(UserActionEvent.EXPORT_OPTIONS))
				return new ExportOptionsAction(selectedDatasets, acceptor, EventHandler.this);

			if (event.type().equals(UserActionEvent.EXPORT_RULESETS))
				return new ExportRuleSetsAction(selectedDatasets, acceptor, EventHandler.this);

			if (event.type().equals(UserActionEvent.EXPORT_SINGLE_CELL_IMAGES))
				return new ExportSingleCellImagesAction(selectedDatasets, acceptor, EventHandler.this);

			if (event.type().equals(UserActionEvent.MERGE_DATASETS_ACTION))
				return new MergeCollectionAction(selectedDatasets, acceptor, EventHandler.this);

			if (event.type().equals(UserActionEvent.MERGE_SIGNALS_ACTION)) {
				LOGGER.finer(
						"Event handler heard new merge signals action request on dataset " + selectedDataset.getName());
				return new MergeSignalsAction(selectedDataset, acceptor, EventHandler.this);
			}

			if (event.type().equals(UserActionEvent.CHANGE_SCALE))
				return () -> setScale(selectedDatasets);

			if (event.type().equals(UserActionEvent.SAVE_SELECTED_DATASET))
				return new ExportDatasetAction(selectedDataset, acceptor, EventHandler.this, null, true);

			if (event.type().equals(UserActionEvent.EXPORT_XML_DATASET))
				return new ExportDatasetAction(selectedDataset, acceptor, EventHandler.this, null, true);

			if (event.type().equals(UserActionEvent.EXPORT_TPS_DATASET))
				return new ExportTPSAction(selectedDataset, acceptor, EventHandler.this);

			if (event.type().equals(UserActionEvent.SAVE_ALL_DATASETS))
				return () -> saveRootDatasets();

			if (event.type().equals(UserActionEvent.CURATE_DATASET))
				return () -> {
					Runnable r = () -> {
						AbstractCellCollectionDialog d = new ManualCurationDialog(selectedDataset);
						d.addDatasetEventListener(EventHandler.this);
					};
					new Thread(r).start();// separate from the UI and method threads - we must not block them
				};

			if (event.type().equals(UserActionEvent.EXPORT_CELL_LOCS))
				return new ExportCellLocationsAction(selectedDatasets, acceptor, EventHandler.this);

			if (event.type().startsWith(UserActionEvent.REMOVE_FROM_WORKSPACE_PREFIX))
				return () -> {
					String workspaceName = event.type().replace(UserActionEvent.REMOVE_FROM_WORKSPACE_PREFIX, "");
					IWorkspace ws = DatasetListManager.getInstance().getWorkspaces().stream()
							.filter(w -> w.getName().equals(workspaceName)).findFirst()
							.orElseThrow(IllegalArgumentException::new);
					for (IAnalysisDataset d : DatasetListManager.getInstance().getRootParents(selectedDatasets))
						ws.remove(d);
					fireDatasetEvent(new DatasetEvent(this, DatasetEvent.ADD_WORKSPACE, EventHandler.class.getName(),
							new ArrayList<IAnalysisDataset>()));
				};

			if (event.type().startsWith(UserActionEvent.ADD_TO_WORKSPACE_PREFIX))
				return () -> {
					String workspaceName = event.type().replace(UserActionEvent.ADD_TO_WORKSPACE_PREFIX, "");
					IWorkspace ws = DatasetListManager.getInstance().getWorkspaces().stream()
							.filter(w -> w.getName().equals(workspaceName)).findFirst()
							.orElseThrow(IllegalArgumentException::new);

					for (IAnalysisDataset d : DatasetListManager.getInstance().getRootParents(selectedDatasets))
						ws.add(d);
					fireDatasetEvent(new DatasetEvent(this, DatasetEvent.ADD_WORKSPACE, EventHandler.class.getName(),
							new ArrayList<IAnalysisDataset>()));
				};

			if (event.type().startsWith(UserActionEvent.NEW_BIOSAMPLE_PREFIX))
				return () -> {
					LOGGER.fine("Creating new biosample");
					try {
						String bsName = ic.requestString("New biosample name");
						List<IWorkspace> workspaces = DatasetListManager.getInstance().getWorkspaces(selectedDataset);
						for (IWorkspace w : workspaces) {
							w.addBioSample(bsName);
							BioSample bs = w.getBioSample(bsName);
							if (bs != null)
								bs.addDataset(selectedDataset);
						}
						DatasetListManager.getInstance().setSelectedDataset(selectedDataset);
					} catch (RequestCancelledException e) {
						LOGGER.fine("New biosample cancelled");
						return;
					}
				};

			if (event.type().startsWith(UserActionEvent.REMOVE_FROM_BIOSAMPLE_PREFIX))
				return () -> {
					String bsName = event.type().replace(UserActionEvent.REMOVE_FROM_BIOSAMPLE_PREFIX, "");
					LOGGER.fine("Removing dataset from biosample " + bsName);
					List<IWorkspace> workspaces = DatasetListManager.getInstance().getWorkspaces(selectedDataset);
					for (IWorkspace w : workspaces) {
						BioSample b = w.getBioSample(bsName);
						if (b != null)
							b.removeDataset(selectedDataset);
					}
					DatasetListManager.getInstance().setSelectedDataset(selectedDataset);
				};

			if (event.type().startsWith(UserActionEvent.ADD_TO_BIOSAMPLE_PREFIX))
				return () -> {
					String bsName = event.type().replace(UserActionEvent.ADD_TO_BIOSAMPLE_PREFIX, "");
					LOGGER.fine("Adding dataset to biosample " + bsName);
					List<IWorkspace> workspaces = DatasetListManager.getInstance().getWorkspaces(selectedDataset);
					for (IWorkspace w : workspaces) {
						BioSample b = w.getBioSample(bsName);
						if (b != null)
							b.addDataset(selectedDataset);
					}
					DatasetListManager.getInstance().setSelectedDataset(selectedDataset);
				};

			if (event.type().equals(UserActionEvent.RELOCATE_CELLS))
				return new RelocateFromFileAction(selectedDataset, acceptor, EventHandler.this, new CountDownLatch(1));

			return null;
		}

		/**
		 * Create a new workspace
		 */
		private void createWorkspace() {

			try {
				String workspaceName = ic.requestString("New workspace name");
				IWorkspace w = WorkspaceFactory.createWorkspace(workspaceName);
				DatasetListManager.getInstance().addWorkspace(w);
				LOGGER.info("New workspace created: " + workspaceName);
				fireDatasetEvent(new DatasetEvent(this, DatasetEvent.ADD_WORKSPACE, EventHandler.class.getName(),
						new ArrayList<>()));

			} catch (RequestCancelledException e) {
				// no action needed
			}

		}

		/**
		 * Set the scale of the given datasets
		 * 
		 * @param selectedDatasets
		 */
		private synchronized void setScale(final List<IAnalysisDataset> selectedDatasets) {
			if (selectedDatasets.isEmpty())
				return;

			try {
				double d0scale = 1;
				double currentScale = 1;

				// is there a common scale in the datasets already?
				// Get the first dataset scale
				Optional<IAnalysisOptions> d0Options = selectedDatasets.get(0).getAnalysisOptions();
				if (d0Options.isPresent()) {
					Optional<HashOptions> d0NucleusOptions = d0Options.get().getNucleusDetectionOptions();
					if (d0NucleusOptions.isPresent()) {
						d0scale = d0NucleusOptions.get().getDouble(HashOptions.SCALE);
					}
				}

				// check any other datasets match
				final double d0scaleFinal = d0scale;
				boolean allMatch = selectedDatasets.stream().allMatch(d -> {
					Optional<IAnalysisOptions> dOptions = d.getAnalysisOptions();
					if (dOptions.isPresent()) {
						Optional<HashOptions> dNucleusOptions = dOptions.get().getNucleusDetectionOptions();
						if (dNucleusOptions.isPresent()) {
							return dNucleusOptions.get().getDouble(HashOptions.SCALE) == d0scaleFinal;
						}
					}
					return false;
				});
				if (allMatch)
					currentScale = d0scale;

				// request the new scale
				double scale = ic.requestDouble("Pixels per micron", currentScale, 1d, 100000d, 1d);
				if (scale > 0) { // don't allow a scale to cause divide by zero errors
					selectedDatasets.stream().forEach(d -> d.setScale(scale));
					UIController.getInstance().fireScaleUpdated(selectedDatasets);
				}

			} catch (RequestCancelledException e) {
				// No action needed
			}
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

				return () -> {

					final CountDownLatch profileLatch = new CountDownLatch(1);
					final CountDownLatch segmentLatch = new CountDownLatch(1);
					final CountDownLatch refoldLatch = new CountDownLatch(1);
					final CountDownLatch saveLatch = new CountDownLatch(1);

					new Thread(() -> { // run profiling
						new RunProfilingAction(selectedDatasets, SingleDatasetResultAction.NO_FLAG, acceptor,
								EventHandler.this, profileLatch).run();

					}).start();

					new Thread(() -> { // wait for profiling and run segmentation
						try {
							profileLatch.await();
							LOGGER.fine("Starting segmentation action");
							new RunSegmentationAction(selectedDatasets, MorphologyAnalysisMode.NEW,
									SingleDatasetResultAction.NO_FLAG, acceptor, EventHandler.this, segmentLatch).run();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						}
					}).start();

					new Thread(() -> { // wait for segmentation and run refolding
						try {
							segmentLatch.await();
							LOGGER.fine("Starting refolding action");
							new RefoldNucleusAction(selectedDatasets, acceptor, EventHandler.this, refoldLatch).run();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						}
					}).start();

					new Thread(() -> { // wait for refolding and run save
						try {
							refoldLatch.await();
							LOGGER.fine("Starting save action");
							new ExportDatasetAction(selectedDatasets, acceptor, EventHandler.this, saveLatch).run();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						}
					}).start();

					new Thread(() -> { // wait for save and recache charts
						try {
							saveLatch.await();
							LOGGER.fine("Starting recache action");
							UIController.getInstance().fireDatasetAdded(selectedDatasets);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
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
				return () -> {
					final CountDownLatch profileLatch = new CountDownLatch(1);
					final CountDownLatch segmentLatch = new CountDownLatch(1);
					new Thread(() -> {
						new RunProfilingAction(selectedDatasets, SingleDatasetResultAction.NO_FLAG, acceptor,
								EventHandler.this, profileLatch).run();
					}).start();

					new Thread(() -> {

						try {
							profileLatch.await();
							new RunSegmentationAction(selectedDatasets, MorphologyAnalysisMode.NEW, 0, acceptor,
									EventHandler.this, segmentLatch).run();
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						}

					}).start();

					new Thread(() -> { // wait for save and recache charts
						try {
							segmentLatch.await();
							LOGGER.fine("Adding datasets");
							fireDatasetEvent(new DatasetEvent(this, DatasetEvent.RECACHE_CHARTS,
									EventHandler.class.getName(), selectedDatasets));
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
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
				return () -> {
					final IAnalysisDataset source = event.secondaryDataset();
					if (source == null)
						return;

					final CountDownLatch segmentLatch = new CountDownLatch(1);
					new Thread(() -> { // wait for profiling and run segmentation
						LOGGER.fine("Starting segmentation action");
						new RunSegmentationAction(selectedDatasets, source, SingleDatasetResultAction.NO_FLAG, acceptor,
								EventHandler.this, segmentLatch).run();
					}).start();

					new Thread(() -> { // wait for save and recache charts
						try {
							segmentLatch.await();
							LOGGER.fine("Adding datasets");
							UIController.getInstance().fireDatasetAdded(selectedDatasets);
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						}
					}).start();
				};
			}

			if (event.method().equals(DatasetEvent.CLUSTER)) {
				LOGGER.fine("Clustering dataset");
				return new ClusterAnalysisAction(event.firstDataset(), acceptor, EventHandler.this);
			}

			if (event.method().equals(DatasetEvent.MANUAL_CLUSTER)) {
				LOGGER.fine("Manually clustering dataset");
				return new ManualClusterAction(event.firstDataset(), acceptor, EventHandler.this);
			}

			if (event.method().equals(DatasetEvent.CLUSTER_FROM_FILE))
				return new ClusterFileAssignmentAction(event.firstDataset(), acceptor, EventHandler.this);

			if (event.method().equals(DatasetEvent.BUILD_TREE)) {
				LOGGER.fine("Building a tree from dataset");
				return new BuildHierarchicalTreeAction(event.firstDataset(), acceptor, EventHandler.this);
			}

			if (event.method().equals(DatasetEvent.RECALCULATE_MEDIAN)) {
				return () -> {
					final CountDownLatch latch = new CountDownLatch(1);
					new Thread(() -> {
						new RunProfilingAction(selectedDatasets, SingleDatasetResultAction.NO_FLAG, acceptor,
								EventHandler.this, latch).run();
					}).start();

					new Thread(() -> { // wait for profiling to complete and recache charts
						try {
							latch.await();
							fireDatasetEvent(new DatasetEvent(this, DatasetEvent.RECACHE_CHARTS, "EventHandler",
									selectedDatasets));
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
							return;
						}
					}).start();
				};
			}

			if (event.method().equals(DatasetEvent.RUN_GLCM_ANALYSIS)) {
				return () -> {
					final CountDownLatch latch = new CountDownLatch(1);
					new RunGLCMAction(selectedDatasets, latch, acceptor, EventHandler.this).run();
				};
			}

			if (event.method().equals(DatasetEvent.EXTRACT_SOURCE))
				return new MergeSourceExtractionAction(event.getDatasets(), acceptor, EventHandler.this);

			if (event.method().equals(DatasetEvent.REFOLD_CONSENSUS)) {
				return () -> {
					final CountDownLatch refoldLatch = new CountDownLatch(1);
					new Thread(() -> { // run refolding
						Runnable task = new RefoldNucleusAction(selectedDatasets, acceptor, EventHandler.this,
								refoldLatch);
						task.run();
					}).start();

					new Thread(() -> { // wait for refolding and recache charts
						try {
							refoldLatch.await();
							fireDatasetEvent(new DatasetEvent(this, DatasetEvent.RECACHE_CHARTS, "EventHandler",
									selectedDatasets));
						} catch (InterruptedException e) {
							Thread.currentThread().interrupt();
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
		public synchronized void run(final UserActionEvent event) {
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
	public synchronized void eventReceived(final UserActionEvent event) {
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
				DatasetListManager.getInstance().setSelectedDatasets(event.getDatasets());

			if (event.method().equals(DatasetEvent.SELECT_ONE_DATASET))
				DatasetListManager.getInstance().setSelectedDataset(event.firstDataset());

			if (event.method().equals(DatasetEvent.RECACHE_CHARTS) || event.method().equals(DatasetEvent.CLEAR_CACHE))
				fireDatasetEvent(event);
		}

	}

	/**
	 * Save all the root datasets in the populations panel
	 */
	public synchronized void saveRootDatasets() {

		Runnable r = () -> {
			for (IAnalysisDataset root : DatasetListManager.getInstance().getRootDatasets()) {
				final CountDownLatch latch = new CountDownLatch(1);

				new Thread(() -> {
					Runnable task = new ExportDatasetAction(root, acceptor, EventHandler.this, latch, false);
					task.run();
					try {
						latch.await();
					} catch (InterruptedException e) {
						LOGGER.log(Loggable.STACK, "Interruption to thread", e);
						Thread.currentThread().interrupt();
					}

					Runnable wrk = new ExportWorkspaceAction(DatasetListManager.getInstance().getWorkspaces(), acceptor,
							EventHandler.this);
					wrk.run();
				}).start();
			}
			LOGGER.fine("All root datasets attempted to be saved");
		};

		ThreadManager.getInstance().execute(r);
	}

	public synchronized void addDatasetUpdateEventListener(EventListener l) {
		updateListeners.add(l);
	}

	public synchronized void removeDatasetUpdateEventListener(EventListener l) {
		updateListeners.remove(l);
	}

	@Override
	public void eventReceived(DatasetUpdateEvent event) {
		// TODO Auto-generated method stub

	}

}
