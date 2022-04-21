package com.bmskinner.nuclear_morphology.gui.events.revamp;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.components.MissingComponentException;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.cells.Nucleus;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.LandmarkType;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.SegmentationHandler;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace;
import com.bmskinner.nuclear_morphology.components.workspaces.IWorkspace.BioSample;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.core.InputSupplier;
import com.bmskinner.nuclear_morphology.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nuclear_morphology.core.ThreadManager;
import com.bmskinner.nuclear_morphology.gui.DefaultInputSupplier;
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
import com.bmskinner.nuclear_morphology.gui.actions.SignalWarpingAction;
import com.bmskinner.nuclear_morphology.gui.actions.SingleDatasetResultAction;
import com.bmskinner.nuclear_morphology.gui.dialogs.collections.AbstractCellCollectionDialog;
import com.bmskinner.nuclear_morphology.gui.dialogs.collections.ManualCurationDialog;
import com.bmskinner.nuclear_morphology.gui.events.LandmarkUpdateEvent;
import com.bmskinner.nuclear_morphology.gui.events.LandmarkUpdateEventListener;
import com.bmskinner.nuclear_morphology.gui.events.ProfileWindowProportionUpdateEvent;
import com.bmskinner.nuclear_morphology.gui.events.SegmentEventListener;
import com.bmskinner.nuclear_morphology.gui.events.SegmentMergeEvent;
import com.bmskinner.nuclear_morphology.gui.events.SegmentSplitEvent;
import com.bmskinner.nuclear_morphology.gui.events.SegmentStartIndexUpdateEvent;
import com.bmskinner.nuclear_morphology.gui.events.SegmentUnmergeEvent;
import com.bmskinner.nuclear_morphology.gui.events.UserActionEvent;
import com.bmskinner.nuclear_morphology.gui.runnables.MorphologyAnalysis;
import com.bmskinner.nuclear_morphology.gui.runnables.SaveAllDatasets;

/**
 * Controller for all user actions
 * 
 * @author ben
 * @since 2.0.0
 *
 */
public class UserActionController implements UserActionEventListener, ConsensusUpdateEventListener,
		LandmarkUpdateEventListener, SegmentEventListener {

	private static final Logger LOGGER = Logger.getLogger(UserActionController.class.getName());

	private static final UserActionController instance = new UserActionController();

	/** A place for progress bars to be displayed */
	private ProgressBarAcceptor acceptor = null;

	/** A source to request user input */
	private final InputSupplier is = new DefaultInputSupplier();

	private UserActionController() {
	}

	public static UserActionController getInstance() {
		return instance;
	}

	public void setProgressBarAcceptor(ProgressBarAcceptor a) {
		this.acceptor = a;
	}

	public ProgressBarAcceptor getProgressBarAcceptor() {
		return acceptor;
	}

	@Override
	public void userActionEventReceived(UserActionEvent e) {
		Runnable r = create(e);
		if (r != null)
			ThreadManager.getInstance().execute(r);
	}

	/**
	 * Create a runnable action for the given event
	 * 
	 * @param event
	 * @return
	 */
	private synchronized Runnable create(final UserActionEvent event) {

		final List<IAnalysisDataset> selectedDatasets = DatasetListManager.getInstance().getSelectedDatasets();
		final IAnalysisDataset selectedDataset = selectedDatasets.isEmpty() ? null : selectedDatasets.get(0);

		// The full pipeline for a new analysis
		if (UserActionEvent.MORPHOLOGY_ANALYSIS_ACTION.equals(event.type()))
			return new MorphologyAnalysis(event.getDatasets(), acceptor);

		if (event.type().startsWith(UserActionEvent.IMPORT_WORKFLOW_PREFIX)) {
			String s = event.type().replace(UserActionEvent.IMPORT_WORKFLOW_PREFIX, "");

			// No image folder specified; will be requested in workflow
			if (s.equals(""))
				return new ImportWorkflowAction(acceptor);

			// Image folder specified
			File f = new File(s);
			return new ImportWorkflowAction(acceptor, f);
		}

		if (event.type().startsWith(UserActionEvent.IMPORT_DATASET_PREFIX)) {
			String s = event.type().replace(UserActionEvent.IMPORT_DATASET_PREFIX, "");
			if (s.equals(""))
				return new ImportDatasetAction(acceptor);
			File f = new File(s);
			return new ImportDatasetAction(acceptor, f);
		}

		if (event.type().startsWith(UserActionEvent.IMPORT_WORKSPACE_PREFIX))
			return () -> {
				String s = event.type().replace(UserActionEvent.IMPORT_WORKSPACE_PREFIX, "");
				if (s.equals("")) {
					new ImportWorkspaceAction(acceptor).run();
					return;
				}
				File f = new File(s);
				new ImportWorkspaceAction(acceptor, f).run();
			};

		if (event.type().startsWith(UserActionEvent.NEW_ANALYSIS_PREFIX)) {
			return () -> {
				String s = event.type().replace(UserActionEvent.NEW_ANALYSIS_PREFIX, "");
				if (s.equals(""))
					return;
				File f = new File(s);

				new NewAnalysisAction(acceptor, f).run();
			};
		}

//		if (event.type().equals(UserActionEvent.NEW_WORKSPACE))
//			return () -> createWorkspace();

		if (event.type().equals(UserActionEvent.EXPORT_WORKSPACE))
			return new ExportWorkspaceAction(DatasetListManager.getInstance().getWorkspaces(), acceptor);

		if (selectedDataset == null)
			return null;

		if (event.type().equals(UserActionEvent.DATASET_ARITHMETIC))
			return new DatasetArithmeticAction(selectedDatasets, acceptor);

		if (event.type().equals(UserActionEvent.EXTRACT_SUBSET))
			return new ExtractRandomCellsAction(selectedDataset, acceptor);

		if (event.type().equals(UserActionEvent.CHANGE_NUCLEUS_IMAGE_FOLDER))
			return new ReplaceSourceImageDirectoryAction(selectedDataset, acceptor);

		if (event.type().equals(UserActionEvent.ADD_NUCLEAR_SIGNAL))
			return new AddNuclearSignalAction(selectedDataset, acceptor);

		if (event.type().equals(UserActionEvent.CLUSTER_FROM_FILE))
			return new ClusterFileAssignmentAction(selectedDataset, acceptor);

		if (event.type().equals(UserActionEvent.POST_FISH_MAPPING))
			return new FishRemappingAction(selectedDatasets, acceptor);

		if (event.type().equals(UserActionEvent.EXPORT_STATS))
			return new ExportNuclearStatsAction(selectedDatasets, acceptor);

		if (event.type().equals(UserActionEvent.EXPORT_PROFILES))
			return new ExportNuclearProfilesAction(selectedDatasets, acceptor);

		if (event.type().equals(UserActionEvent.EXPORT_OUTLINES))
			return new ExportNuclearOutlinesAction(selectedDatasets, acceptor);

		if (event.type().equals(UserActionEvent.EXPORT_SIGNALS))
			return new ExportSignalsAction(selectedDatasets, acceptor);

		if (event.type().equals(UserActionEvent.EXPORT_SHELLS))
			return new ExportShellsAction(selectedDatasets, acceptor);

		if (event.type().equals(UserActionEvent.EXPORT_OPTIONS))
			return new ExportOptionsAction(selectedDatasets, acceptor);

		if (event.type().equals(UserActionEvent.EXPORT_RULESETS))
			return new ExportRuleSetsAction(selectedDatasets, acceptor);

		if (event.type().equals(UserActionEvent.EXPORT_SINGLE_CELL_IMAGES))
			return new ExportSingleCellImagesAction(selectedDatasets, acceptor);

		if (event.type().equals(UserActionEvent.MERGE_DATASETS_ACTION))
			return new MergeCollectionAction(selectedDatasets, acceptor);

		if (event.type().equals(UserActionEvent.MERGE_SIGNALS_ACTION)) {
			return new MergeSignalsAction(selectedDataset, acceptor);
		}

		if (event.type().equals(UserActionEvent.CHANGE_SCALE))
			return () -> setScale(selectedDatasets);

		if (event.type().equals(UserActionEvent.SAVE_SELECTED_DATASET))
			return new ExportDatasetAction(selectedDataset, acceptor, null, true);

		if (event.type().equals(UserActionEvent.EXPORT_XML_DATASET))
			return new ExportDatasetAction(selectedDataset, acceptor, null, true);

		if (event.type().equals(UserActionEvent.EXPORT_TPS_DATASET))
			return new ExportTPSAction(selectedDataset, acceptor);

		if (event.type().equals(UserActionEvent.SAVE_ALL_DATASETS))
			return new SaveAllDatasets(DatasetListManager.getInstance().getRootDatasets(), acceptor);

		if (event.type().equals(UserActionEvent.SAVE_SELECTED_DATASETS))
			return new SaveAllDatasets(selectedDatasets, acceptor);

		if (event.type().equals(UserActionEvent.CURATE_DATASET))
			return () -> {
				Runnable r = () -> {
					AbstractCellCollectionDialog d = new ManualCurationDialog(selectedDataset);
				};
				new Thread(r).start();// separate from the UI and method threads - we must not block them
			};

		if (event.type().equals(UserActionEvent.EXPORT_CELL_LOCS))
			return new ExportCellLocationsAction(selectedDatasets, acceptor);

		if (event.type().startsWith(UserActionEvent.REMOVE_FROM_WORKSPACE_PREFIX))
			return () -> {
				String workspaceName = event.type().replace(UserActionEvent.REMOVE_FROM_WORKSPACE_PREFIX, "");
				IWorkspace ws = DatasetListManager.getInstance().getWorkspaces().stream()
						.filter(w -> w.getName().equals(workspaceName)).findFirst()
						.orElseThrow(IllegalArgumentException::new);
				for (IAnalysisDataset d : DatasetListManager.getInstance().getRootParents(selectedDatasets))
					ws.remove(d);
//				fireDatasetEvent(new UserActionEvent(this, UserActionEvent.ADD_WORKSPACE,
//						UserActionController.class.getName(), new ArrayList<>()));
			};

		if (event.type().startsWith(UserActionEvent.ADD_TO_WORKSPACE_PREFIX))
			return () -> {
				String workspaceName = event.type().replace(UserActionEvent.ADD_TO_WORKSPACE_PREFIX, "");
				IWorkspace ws = DatasetListManager.getInstance().getWorkspaces().stream()
						.filter(w -> w.getName().equals(workspaceName)).findFirst()
						.orElseThrow(IllegalArgumentException::new);

				for (IAnalysisDataset d : DatasetListManager.getInstance().getRootParents(selectedDatasets))
					ws.add(d);
//				fireDatasetEvent(new UserActionEvent(this, UserActionEvent.ADD_WORKSPACE,
//						UserActionController.class.getName(), new ArrayList<>()));
			};

		if (event.type().startsWith(UserActionEvent.NEW_BIOSAMPLE_PREFIX))
			return () -> {
				LOGGER.fine("Creating new biosample");
				try {
					String bsName = is.requestString("New biosample name");
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
			return new RelocateFromFileAction(selectedDataset, acceptor, new CountDownLatch(1));

		if (event.type().equals(UserActionEvent.SEGMENTATION_ACTION))
			return new RunSegmentationAction(selectedDatasets, MorphologyAnalysisMode.SEGMENT_FROM_SCRATCH,
					SingleDatasetResultAction.NO_FLAG, acceptor);

		if (event.type().equals(UserActionEvent.APPLY_MEDIAN_TO_NUCLEI))
			return new RunSegmentationAction(selectedDatasets, MorphologyAnalysisMode.APPLY_MEDIAN_TO_NUCLEI,
					SingleDatasetResultAction.NO_FLAG, acceptor);

		if (event.type().equals(UserActionEvent.SAVE)) {
			return () -> {
				final CountDownLatch latch = new CountDownLatch(1);
				new ExportDatasetAction(selectedDatasets, acceptor, latch).run();
			};
		}

		// Run a completely new analysis on the dataset
		if (event.type().equals(UserActionEvent.REFPAIR_SEGMENTATION)) {

			// begin a new morphology analysis
			return () -> {
				final CountDownLatch profileLatch = new CountDownLatch(1);
				final CountDownLatch segmentLatch = new CountDownLatch(1);
				new Thread(() -> {
					new RunProfilingAction(selectedDatasets, SingleDatasetResultAction.NO_FLAG, acceptor, profileLatch)
							.run();
				}).start();

				new Thread(() -> {

					try {
						profileLatch.await();
						new RunSegmentationAction(selectedDatasets, MorphologyAnalysisMode.SEGMENT_FROM_SCRATCH, 0,
								acceptor, segmentLatch).run();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}

				}).start();

				new Thread(() -> { // wait for save and recache charts
					try {
						segmentLatch.await();
						LOGGER.fine("Adding datasets");
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}).start();

			};
		}

		if (event.type().equals(UserActionEvent.RUN_SHELL_ANALYSIS)) {
			return new ShellAnalysisAction(selectedDataset, acceptor);
		}

		if (event.type().equals(UserActionEvent.SIGNAL_WARPING))
			return new SignalWarpingAction(selectedDataset, acceptor);

		if (event.type().equals(UserActionEvent.COPY_PROFILE_SEGMENTATION)) {
			// copy the profile segmentation from one dataset to another
			return () -> {
				final IAnalysisDataset source = event.getSecondaryDataset();
				if (source == null)
					return;

				final CountDownLatch segmentLatch = new CountDownLatch(1);
				new Thread(() -> { // wait for profiling and run segmentation
					LOGGER.fine("Starting segmentation action");
					new RunSegmentationAction(selectedDatasets, source, SingleDatasetResultAction.NO_FLAG, acceptor,
							segmentLatch).run();
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

		if (event.type().equals(UserActionEvent.CLUSTER)) {
			LOGGER.fine("Clustering dataset");
			return new ClusterAnalysisAction(selectedDataset, acceptor);
		}

		if (event.type().equals(UserActionEvent.MANUAL_CLUSTER)) {
			LOGGER.fine("Manually clustering dataset");
			return new ManualClusterAction(selectedDataset, acceptor);
		}

		if (event.type().equals(UserActionEvent.CLUSTER_FROM_FILE))
			return new ClusterFileAssignmentAction(selectedDataset, acceptor);

		if (event.type().equals(UserActionEvent.BUILD_TREE)) {
			LOGGER.fine("Building a tree from dataset");
			return new BuildHierarchicalTreeAction(selectedDataset, acceptor);
		}

		if (event.type().equals(UserActionEvent.RECALCULATE_MEDIAN)) {
			return () -> {
				final CountDownLatch latch = new CountDownLatch(1);
				new Thread(() -> {
					new RunProfilingAction(selectedDatasets, SingleDatasetResultAction.NO_FLAG, acceptor, latch).run();
				}).start();

				new Thread(() -> { // wait for profiling to complete and recache charts
					try {
						latch.await();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}).start();
			};
		}

		if (event.type().equals(UserActionEvent.RUN_GLCM_ANALYSIS)) {
			return () -> {
				final CountDownLatch latch = new CountDownLatch(1);
				new RunGLCMAction(selectedDatasets, latch, acceptor).run();
			};
		}

		if (event.type().equals(UserActionEvent.EXTRACT_SOURCE))
			return new MergeSourceExtractionAction(selectedDatasets, acceptor);

		if (event.type().equals(UserActionEvent.REFOLD_CONSENSUS)) {
			return () -> {
				final CountDownLatch refoldLatch = new CountDownLatch(1);
				new Thread(() -> { // run refolding
					Runnable task = new RefoldNucleusAction(selectedDatasets, acceptor, refoldLatch);
					task.run();
				}).start();

				new Thread(() -> { // wait for refolding and recache charts
					try {
						refoldLatch.await();
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
			double scale = new DefaultInputSupplier().requestDouble("Pixels per micron", currentScale, 1d, 100000d, 1d);
			if (scale > 0) { // don't allow a scale to cause divide by zero errors
				selectedDatasets.stream().forEach(d -> d.setScale(scale));
				UIController.getInstance().fireScaleUpdated(selectedDatasets);
			}

		} catch (RequestCancelledException e) {
			// No action needed
		}
	}

	@Override
	public void consensusRotationUpdateReceived(List<IAnalysisDataset> datasets, double rotation) {
		for (IAnalysisDataset d : datasets) {
			if (d.getCollection().hasConsensus()) {
				d.getCollection().rotateConsensus(d.getCollection().currentConsensusRotation() - rotation);
			}
		}
		UIController.getInstance().fireConsensusNucleusChanged(datasets);
	}

	@Override
	public void consensusRotationUpdateReceived(IAnalysisDataset dataset, double rotation) {
		if (dataset.getCollection().hasConsensus()) {
			dataset.getCollection().rotateConsensus(dataset.getCollection().currentConsensusRotation() - rotation);
			UIController.getInstance().fireConsensusNucleusChanged(dataset);
		}
	}

	@Override
	public void consensusRotationResetReceived(List<IAnalysisDataset> datasets) {
		for (IAnalysisDataset d : datasets) {
			if (d.getCollection().hasConsensus()) {
				d.getCollection().rotateConsensus(-90);
			}
		}
		UIController.getInstance().fireConsensusNucleusChanged(datasets);
	}

	@Override
	public void consensusRotationResetReceived(IAnalysisDataset dataset) {
		if (dataset.getCollection().hasConsensus()) {
			dataset.getCollection().rotateConsensus(-90);
			UIController.getInstance().fireConsensusNucleusChanged(dataset);
		}
	}

	@Override
	public void consensusTranslationUpdateReceived(List<IAnalysisDataset> datasets, double x, double y) {
		for (IAnalysisDataset d : datasets) {
			if (d.getCollection().hasConsensus()) {
				IPoint com = d.getCollection().getRawConsensus().getCentreOfMass();
				d.getCollection().offsetConsensus(com.getX() + x, com.getY() + y);
			}
		}
		UIController.getInstance().fireConsensusNucleusChanged(datasets);
	}

	@Override
	public void consensusTranslationUpdateReceived(IAnalysisDataset dataset, double x, double y) {
		if (dataset.getCollection().hasConsensus()) {
			IPoint com = dataset.getCollection().getRawConsensus().getCentreOfMass();
			dataset.getCollection().offsetConsensus(com.getX() + x, com.getY() + y);
			UIController.getInstance().fireConsensusNucleusChanged(dataset);
		}
	}

	@Override
	public void consensusTranslationResetReceived(List<IAnalysisDataset> datasets) {
		for (IAnalysisDataset d : datasets) {
			if (d.getCollection().hasConsensus()) {
				IPoint com = d.getCollection().getRawConsensus().getCentreOfMass();
				d.getCollection().offsetConsensus(com.getX(), com.getY());
			}
		}
		UIController.getInstance().fireConsensusNucleusChanged(datasets);
	}

	@Override
	public void consensusTranslationResetReceived(IAnalysisDataset dataset) {
		if (dataset.getCollection().hasConsensus()) {
			IPoint com = dataset.getCollection().getRawConsensus().getCentreOfMass();
			dataset.getCollection().offsetConsensus(com.getX(), com.getY());
			UIController.getInstance().fireConsensusNucleusChanged(dataset);
		}
	}

	@Override
	public void landmarkUpdateEventReceived(LandmarkUpdateEvent event) {
		if (event.dataset == null)
			return;

		IAnalysisDataset d = event.dataset;

		if (d.getCollection().isVirtual() && Landmark.REFERENCE_POINT.equals(event.lm)) {
			LOGGER.warning("Cannot update core border tag for a child dataset");
			return;
		}

		SegmentationHandler sh = new SegmentationHandler(d);
		sh.setLandmark(event.lm, event.newIndex);

		if (LandmarkType.CORE.equals(event.lm.type())) {
			UserActionController.getInstance().userActionEventReceived(
					new UserActionEvent(this, UserActionEvent.SEGMENTATION_ACTION, List.of(event.dataset)));
		} else {
			UIController.getInstance().fireProfilesUpdated(d);
		}

	}

	@Override
	public void segmentStartIndexUpdateEventReceived(SegmentStartIndexUpdateEvent event) {

		if (event.isDataset()) {
			SegmentationHandler sh = new SegmentationHandler(event.dataset);
			sh.updateSegmentStartIndexAction(event.id, event.index);
			userActionEventReceived(new UserActionEvent(this, UserActionEvent.APPLY_MEDIAN_TO_NUCLEI));
		}

	}

	@Override
	public void segmentMergeEventReceived(SegmentMergeEvent event) {
		try {
			SegmentationHandler sh = new SegmentationHandler(event.dataset);
			sh.mergeSegments(event.id1, event.id2);
			UIController.getInstance().fireProfilesUpdated(event.dataset);
		} catch (ProfileException | MissingComponentException e) {
			LOGGER.warning("Could not merge segments: " + e.getMessage());
		}
	}

	@Override
	public void segmentUnmergeEventReceived(SegmentUnmergeEvent event) {
		SegmentationHandler sh = new SegmentationHandler(event.dataset);
		sh.unmergeSegments(event.id);
		UIController.getInstance().fireProfilesUpdated(event.dataset);
	}

	@Override
	public void segmentSplitEventReceived(SegmentSplitEvent event) {
		SegmentationHandler sh = new SegmentationHandler(event.dataset);
		sh.splitSegment(event.id);
		UIController.getInstance().fireProfilesUpdated(event.dataset);
	}

	@Override
	public void profileWindowProportionUpdateEventReceived(ProfileWindowProportionUpdateEvent event) {

		try {
			// Update cells
			for (Nucleus n : event.dataset.getCollection().getNuclei())
				n.setWindowProportion(event.window);

			// recalculate profiles
			event.dataset.getCollection().getProfileCollection().calculateProfiles();
			Optional<IAnalysisOptions> op = event.dataset.getAnalysisOptions();
			if (op.isPresent())
				op.get().setAngleWindowProportion(event.window);

			UIController.getInstance().fireProfilesUpdated(event.dataset);
		} catch (ProfileException | MissingLandmarkException | MissingProfileException e) {
			LOGGER.warning("Unable to update profile window proportion: " + e.getMessage());
		}
	}

}
