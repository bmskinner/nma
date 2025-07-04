package com.bmskinner.nma.gui.events;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import com.bmskinner.nma.analysis.DatasetDeleter;
import com.bmskinner.nma.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.XMLNames;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.workspaces.IWorkspace;
import com.bmskinner.nma.components.workspaces.WorkspaceFactory;
import com.bmskinner.nma.core.DatasetListManager;
import com.bmskinner.nma.core.InputSupplier;
import com.bmskinner.nma.core.InputSupplier.RequestCancelledException;
import com.bmskinner.nma.core.ThreadManager;
import com.bmskinner.nma.gui.DefaultInputSupplier;
import com.bmskinner.nma.gui.ProgressBarAcceptor;
import com.bmskinner.nma.gui.actions.AddNuclearSignalAction;
import com.bmskinner.nma.gui.actions.BooleanOperationAction;
import com.bmskinner.nma.gui.actions.CalculateCellHistogramAction;
import com.bmskinner.nma.gui.actions.ClusterAutomaticAction;
import com.bmskinner.nma.gui.actions.ClusterFileAssignmentAction;
import com.bmskinner.nma.gui.actions.ClusterManualAction;
import com.bmskinner.nma.gui.actions.CreateConsensusAction;
import com.bmskinner.nma.gui.actions.DatasetScaleChangeAction;
import com.bmskinner.nma.gui.actions.ExportCellLocationsAction;
import com.bmskinner.nma.gui.actions.ExportDatasetAction;
import com.bmskinner.nma.gui.actions.ExportKeypointsAction;
import com.bmskinner.nma.gui.actions.ExportMeasurementsAction.ExportNuclearOutlinesAction;
import com.bmskinner.nma.gui.actions.ExportMeasurementsAction.ExportNuclearProfilesAction;
import com.bmskinner.nma.gui.actions.ExportMeasurementsAction.ExportNuclearStatsAction;
import com.bmskinner.nma.gui.actions.ExportMeasurementsAction.ExportShellsAction;
import com.bmskinner.nma.gui.actions.ExportMeasurementsAction.ExportSignalsAction;
import com.bmskinner.nma.gui.actions.ExportOptionsAction;
import com.bmskinner.nma.gui.actions.ExportRuleSetsAction;
import com.bmskinner.nma.gui.actions.ExportSingleCellImagesAction;
import com.bmskinner.nma.gui.actions.ExportTPSAction;
import com.bmskinner.nma.gui.actions.ExportWorkspaceAction;
import com.bmskinner.nma.gui.actions.ExtractRandomCellsAction;
import com.bmskinner.nma.gui.actions.FilterPoorEdgeDetectionCellsAction;
import com.bmskinner.nma.gui.actions.FishRemappingAction;
import com.bmskinner.nma.gui.actions.ImportDatasetAction;
import com.bmskinner.nma.gui.actions.ImportWorkflowAction;
import com.bmskinner.nma.gui.actions.ImportWorkspaceAction;
import com.bmskinner.nma.gui.actions.LandmarkRemappingAction;
import com.bmskinner.nma.gui.actions.MergeCollectionAction;
import com.bmskinner.nma.gui.actions.MergeSignalsAction;
import com.bmskinner.nma.gui.actions.MergeSourceExtractionAction;
import com.bmskinner.nma.gui.actions.NewAnalysisAction;
import com.bmskinner.nma.gui.actions.RelocateFromFileAction;
import com.bmskinner.nma.gui.actions.ReplaceSourceImageDirectoryAction;
import com.bmskinner.nma.gui.actions.RunGLCMAction;
import com.bmskinner.nma.gui.actions.RunProfilingAction;
import com.bmskinner.nma.gui.actions.RunSegmentationAction;
import com.bmskinner.nma.gui.actions.SegmentMergeAction;
import com.bmskinner.nma.gui.actions.SegmentSplitAction;
import com.bmskinner.nma.gui.actions.SegmentUnmergeAction;
import com.bmskinner.nma.gui.actions.ShellAnalysisAction;
import com.bmskinner.nma.gui.actions.SignalWarpingAction;
import com.bmskinner.nma.gui.actions.SingleDatasetResultAction;
import com.bmskinner.nma.gui.actions.TextFileAnalysisAction;
import com.bmskinner.nma.gui.actions.UpdateLandmarkAction;
import com.bmskinner.nma.gui.actions.UpdateSegmentIndexAction;
import com.bmskinner.nma.gui.dialogs.collections.AbstractCellCollectionDialog;
import com.bmskinner.nma.gui.dialogs.collections.ManualCurationDialog;
import com.bmskinner.nma.gui.runnables.MorphologyAnalysis;
import com.bmskinner.nma.gui.runnables.SaveAllDatasets;
import com.bmskinner.nma.io.GenericFileImporter;
import com.bmskinner.nma.io.Io;
import com.bmskinner.nma.io.SVGWriter;
import com.bmskinner.nma.utility.FileUtils;

/**
 * Controller for all user actions
 * 
 * @author ben
 * @since 2.0.0
 *
 */
public class UserActionController implements UserActionEventListener, ConsensusUpdateEventListener,
		LandmarkUpdateEventListener, SegmentEventListener, FileImportEventListener {

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

		final IAnalysisDataset selectedDataset = DatasetListManager.getInstance()
				.getActiveDataset();

		// Does not require selected dataset
		if (event.type().equals(UserActionEvent.REMAP_LANDMARKS))
			return new LandmarkRemappingAction(acceptor);

		// The full pipeline for a new analysis
		if (UserActionEvent.MORPHOLOGY_ANALYSIS_ACTION.equals(event.type()))
			return new MorphologyAnalysis(event.getDatasets(), acceptor);

		// When DnD is parsed
		if (event.type().startsWith(UserActionEvent.NEW_ANALYSIS_PREFIX)) {
			return () -> {
				String s = event.type().replace(UserActionEvent.NEW_ANALYSIS_PREFIX, "");
				if (s.equals(""))
					return;
				File f = new File(s);

				new NewAnalysisAction(acceptor, f).run();
			};
		}

		if (event.type().equals(UserActionEvent.NEW_TEXT_FILE_ANALYSIS)) {
			new TextFileAnalysisAction(acceptor).run();
		}

		if (event.type().equals(UserActionEvent.PRINT_DATASET_HASH_CMD)) {
			// Print the component hashes of the selected datasets
//			for (IAnalysisDataset d : event.getDatasets()) {
			for (IAnalysisDataset d : DatasetListManager.getInstance().getRootDatasets()) {
				LOGGER.info(() -> "Dataset '%s': hash '%s'".formatted(d.getName(), d.hashCode()));

				// TODO: remove once finished with the GLCM measurements being auto-errors
//				LOGGER.info(
//						() -> "  Collection: hash '%s'".formatted(d.getCollection().hashCode()));
////				LOGGER.info(
////						() -> "  Signals: hash '%s'"
////								.formatted(d.getCollection().getSignalGroups().hashCode()));
//				LOGGER.info(
//						() -> "  Consensus: hash '%s'"
//								.formatted(d.getCollection().getRawConsensus().hashCode()));
//				for (ICell c : d.getCollection()) {
//					LOGGER.info(
//							() -> "    Cell '%s': hash '%s'"
//									.formatted(c.getId(), c.hashCode()));
//					for (Nucleus n : c.getNuclei()) {
//						LOGGER.info(
//								() -> "      Nucleus '%s': hash '%s'"
//										.formatted(n.getID(), n.hashCode()));
//
////						LOGGER.info(
////								() -> "      Signals '%s': hash '%s'"
////										.formatted(n.getSignalCollection(),
////												n.getSignalCollection().hashCode()));
//						LOGGER.info(
//								() -> "      profileLandmarks: hash '%s'"
//										.formatted(n.getLandmarks().hashCode()));
//						LOGGER.info(
//								() -> {
//									try {
//										return "      segments: hash '%s'"
//												.formatted(
//														n.getProfile(ProfileType.ANGLE).hashCode());
//									} catch (MissingProfileException | MissingLandmarkException
//											| ProfileException e) {
//										// TODO Auto-generated catch block
//										e.printStackTrace();
//										return "Error";
//									}
//								});
//						LOGGER.info(
//								() -> "      border: hash '%s'"
//										.formatted(n.getBorderList().hashCode()));
//						LOGGER.info(
//								() -> "      measurements '%s'"
//										.formatted(n.getMeasurements().hashCode()));
//						for (Measurement m : n.getMeasurements()) {
//							LOGGER.info(
//									() -> "      '%s': %s : '%s'"
//											.formatted(m, n.getMeasurement(m), m.hashCode()));
//						}
//
//					}
//				}

			}
		}

		if (event.type().equals(UserActionEvent.NEW_WORKSPACE)) {
			return () -> {
				try {
					String workspaceName = is.requestString("New workspace name");
					IWorkspace w = WorkspaceFactory.createWorkspace(workspaceName);
					DatasetListManager.getInstance().addWorkspace(w);
					LOGGER.fine("New workspace created: " + workspaceName);
					UIController.getInstance().fireWorkspaceAdded(w);

				} catch (RequestCancelledException e) {
					// no action needed
				}
			};
		}

		if (event.type().equals(UserActionEvent.SAVE_WORKSPACE))
			return new ExportWorkspaceAction(DatasetListManager.getInstance().getWorkspaces(),
					acceptor);

		if (event.type().equals(UserActionEvent.DATASET_ARITHMETIC))
			return new BooleanOperationAction(event.getDatasets(), acceptor);

		if (event.type().equals(UserActionEvent.EXTRACT_SUBSET))
			return new ExtractRandomCellsAction(selectedDataset, acceptor);

		if (event.type().equals(UserActionEvent.FILTER_POOR_EDGE_DETECTION))
			return new FilterPoorEdgeDetectionCellsAction(selectedDataset, acceptor);

		if (event.type().equals(UserActionEvent.POST_FISH_MAPPING))
			return new FishRemappingAction(event.getDatasets(), acceptor);

		if (event.type().equals(UserActionEvent.EXPORT_STATS))
			return new ExportNuclearStatsAction(event.getDatasets(), acceptor);

		if (event.type().equals(UserActionEvent.EXPORT_PROFILES))
			return new ExportNuclearProfilesAction(event.getDatasets(), acceptor);

		if (event.type().equals(UserActionEvent.EXPORT_OUTLINES))
			return new ExportNuclearOutlinesAction(event.getDatasets(), acceptor);

		if (event.type().equals(UserActionEvent.EXPORT_SIGNALS))
			return new ExportSignalsAction(event.getDatasets(),
					acceptor);

		if (event.type().equals(UserActionEvent.EXPORT_SHELLS))
			return new ExportShellsAction(event.getDatasets(),
					acceptor);

		if (event.type().equals(UserActionEvent.EXPORT_OPTIONS))
			return new ExportOptionsAction(event.getDatasets(),
					acceptor);

		if (event.type().equals(UserActionEvent.EXPORT_RULESETS))
			return new ExportRuleSetsAction(event.getDatasets(),
					acceptor);

		if (event.type().equals(UserActionEvent.EXPORT_SINGLE_CELL_IMAGES))
			return new ExportSingleCellImagesAction(event.getDatasets(), acceptor);

		if (event.type().equals(UserActionEvent.EXPORT_KEYPOINTS))
			return new ExportKeypointsAction(event.getDatasets(), acceptor);

		if (event.type().equals(UserActionEvent.MERGE_DATASETS_ACTION))
			return new MergeCollectionAction(event.getDatasets(),
					acceptor);

		if (event.type().equals(UserActionEvent.MERGE_SIGNALS_ACTION)) {
			return new MergeSignalsAction(selectedDataset, acceptor);
		}

		if (event.type().equals(UserActionEvent.CHANGE_SCALE))
			return new DatasetScaleChangeAction(event.getDatasets(), acceptor);

		if (event.type().equals(UserActionEvent.EXPORT_TPS_DATASET))
			return new ExportTPSAction(selectedDataset, acceptor);

		if (event.type().equals(UserActionEvent.SAVE_ALL_DATASETS))
			return new SaveAllDatasets(DatasetListManager.getInstance().getRootDatasets(),
					acceptor);

		if (event.type().equals(UserActionEvent.SAVE_SELECTED_DATASETS))
			return new SaveAllDatasets(event.getDatasets(),
					acceptor);

		if (event.type().equals(UserActionEvent.CURATE_DATASET))
			return () -> {
				Runnable r = () -> {
					AbstractCellCollectionDialog d = new ManualCurationDialog(selectedDataset);
				};
				new Thread(r).start();// separate from the UI and method threads - we must not block
										// them
			};

		if (event.type().equals(UserActionEvent.EXPORT_CELL_LOCS))
			return new ExportCellLocationsAction(event.getDatasets(), acceptor);

		if (event.type().startsWith(UserActionEvent.ADD_TO_WORKSPACE))
			return () -> {
				addToWorkspace(event.getDatasets());
			};

		if (event.type().startsWith(UserActionEvent.REMOVE_FROM_WORKSPACE))
			return () -> {
				removeFromWorkspace(event.getDatasets());
			};

		if (event.type().equals(UserActionEvent.SEGMENTATION_ACTION))
			return new RunSegmentationAction(event.getDatasets(),
					MorphologyAnalysisMode.SEGMENT_FROM_SCRATCH,
					SingleDatasetResultAction.NO_FLAG, acceptor);

		if (event.type().equals(UserActionEvent.APPLY_MEDIAN_TO_NUCLEI))
			return new RunSegmentationAction(event.getDatasets(),
					MorphologyAnalysisMode.APPLY_MEDIAN_TO_NUCLEI,
					SingleDatasetResultAction.NO_FLAG, acceptor);

		if (event.type().equals(UserActionEvent.SAVE)) {
			return () -> {
				final CountDownLatch latch = new CountDownLatch(1);
				new ExportDatasetAction(event.getDatasets(), acceptor, latch).run();
			};
		}

		if (event.type().equals(UserActionEvent.COPY_PROFILE_SEGMENTATION)) {
			// copy the profile segmentation from one dataset to another
			return () -> {
				final IAnalysisDataset source = event.getSecondaryDataset();
				if (source == null)
					return;

				final CountDownLatch segmentLatch = new CountDownLatch(1);
				new Thread(() -> { // wait for profiling and run segmentation
					LOGGER.fine("Starting segmentation action");
					new RunSegmentationAction(event.getDatasets(), source,
							SingleDatasetResultAction.NO_FLAG, acceptor,
							segmentLatch).run();
				}).start();

				new Thread(() -> { // wait for save and recache charts
					try {
						segmentLatch.await();
						LOGGER.fine("Adding datasets");
						UIController.getInstance().fireDatasetAdded(event.getDatasets());
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}).start();
			};
		}

		if (UserActionEvent.CLUSTER_AUTOMATICALLY.equals(event.type()))
			return new ClusterAutomaticAction(event.getDatasets().get(0), acceptor);

		if (UserActionEvent.CLUSTER_MANUALLY.equals(event.type()))
			return new ClusterManualAction(event.getDatasets().get(0), acceptor);

		if (UserActionEvent.CLUSTER_FROM_FILE.equals(event.type()))
			return new ClusterFileAssignmentAction(event.getDatasets().get(0), acceptor);

		if (event.type().equals(UserActionEvent.RECALCULATE_MEDIAN)) {
			return () -> {
				final CountDownLatch latch = new CountDownLatch(1);
				new Thread(() -> {
					new RunProfilingAction(event.getDatasets(), SingleDatasetResultAction.NO_FLAG,
							acceptor, latch).run();
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
				new RunGLCMAction(event.getDatasets(), latch, acceptor).run();
			};
		}

		if (event.type().equals(UserActionEvent.RUN_HISTOGRAM_CALC)) {
			return () -> {
				final CountDownLatch latch = new CountDownLatch(1);
				new CalculateCellHistogramAction(event.getDatasets(), latch, acceptor).run();
			};
		}

		if (event.type().equals(UserActionEvent.EXTRACT_MERGE_SOURCE))
			return new MergeSourceExtractionAction(event.getDatasets(), acceptor);

		if (event.type().equals(UserActionEvent.REFOLD_CONSENSUS)) {
			return () -> {
				final CountDownLatch refoldLatch = new CountDownLatch(1);
				new Thread(() -> { // run refolding
					Runnable task = new CreateConsensusAction(event.getDatasets(), acceptor,
							refoldLatch);
					task.run();
				}).start();

				new Thread(() -> { // wait for refolding and recache charts
					try {
						refoldLatch.await();
						UIController.getInstance().fireConsensusNucleusChanged(event.getDatasets());
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}).start();
			};

		}

		if (UserActionEvent.DELETE_DATASET.equals(event.type())) {

			DatasetDeleter deleter = new DatasetDeleter(event.getDatasets());
			ThreadManager.getInstance().submit(deleter);
		}

		if (selectedDataset == null)
			return null;

		if (event.type().equals(UserActionEvent.RELOCATE_CELLS))
			return new RelocateFromFileAction(selectedDataset, acceptor, new CountDownLatch(1));

		if (event.type().equals(UserActionEvent.RUN_SHELL_ANALYSIS)) {
			return new ShellAnalysisAction(selectedDataset, acceptor);
		}

		if (event.type().equals(UserActionEvent.SIGNAL_WARPING))
			return new SignalWarpingAction(selectedDataset, acceptor);

		if (event.type().equals(UserActionEvent.SAVE_SELECTED_DATASET))
			return new ExportDatasetAction(selectedDataset, acceptor, null, true);

		if (event.type().equals(UserActionEvent.EXPORT_XML_DATASET))
			return new ExportDatasetAction(selectedDataset, acceptor, null, true);

		if (event.type().equals(UserActionEvent.CHANGE_NUCLEUS_IMAGE_FOLDER))
			return new ReplaceSourceImageDirectoryAction(selectedDataset, acceptor);

		if (event.type().equals(UserActionEvent.ADD_NUCLEAR_SIGNAL))
			return new AddNuclearSignalAction(selectedDataset, acceptor);

		return null;
	}

	private void addToWorkspace(final List<IAnalysisDataset> selectedDatasets) {
		try {
			// Make a list of open workspaces and choose one
			IWorkspace[] wks = DatasetListManager.getInstance().getWorkspaces()
					.toArray(new IWorkspace[0]);

			if (wks.length == 0) {
				JOptionPane.showMessageDialog(null,
						"No workspaces are open. Create or open a workspace first.",
						"Cannot add to workspace",
						JOptionPane.WARNING_MESSAGE, null);
				return;
			}

			int i = is.requestOption(wks, "Choose workspace to add dataset to",
					"Choose workspace");

			for (IAnalysisDataset d : DatasetListManager.getInstance()
					.getRootParents(selectedDatasets)) {
				wks[i].add(d);
				UIController.getInstance().fireDatasetAdded(wks[i], d);
			}

			// Automatically save workspaces
			userActionEventReceived(new UserActionEvent(this, UserActionEvent.SAVE_WORKSPACE));

		} catch (RequestCancelledException e) {
			// No action, user cancelled
		}
	}

	private void removeFromWorkspace(final List<IAnalysisDataset> selectedDatasets) {
		try {

			List<IWorkspace> ws = new ArrayList<>();
			for (IAnalysisDataset d : selectedDatasets)
				ws.addAll(DatasetListManager.getInstance().getWorkspaces(d));

			IWorkspace[] wks = ws.toArray(new IWorkspace[0]);

			if (wks.length == 0) {
				JOptionPane.showMessageDialog(null, "Dataset does not belong to an open workspace",
						"Cannot remove from workspace",
						JOptionPane.WARNING_MESSAGE, null);
				return;
			}

			int i = is.requestOption(wks, "Choose workspace to remove dataset from",
					"Choose workspace");

			for (IAnalysisDataset d : DatasetListManager.getInstance()
					.getRootParents(selectedDatasets)) {
				wks[i].remove(d);
				UIController.getInstance().fireDatasetRemoved(wks[i], d);
			}

			// Automatically save workspaces
			userActionEventReceived(new UserActionEvent(this, UserActionEvent.SAVE_WORKSPACE));

		} catch (RequestCancelledException e) {
			// No action, user cancelled
		}
	}

	@Override
	public void consensusRotationUpdateReceived(List<IAnalysisDataset> datasets, double rotation) {
		for (IAnalysisDataset d : datasets) {
			if (d.getCollection().hasConsensus()) {
				d.getCollection()
						.rotateConsensus(d.getCollection().currentConsensusRotation() - rotation);
			}
		}
		UIController.getInstance().fireConsensusNucleusChanged(datasets);
	}

	@Override
	public void consensusRotationUpdateReceived(IAnalysisDataset dataset, double rotation) {
		if (dataset.getCollection().hasConsensus()) {
			dataset.getCollection()
					.rotateConsensus(dataset.getCollection().currentConsensusRotation() - rotation);
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
	public void consensusTranslationUpdateReceived(List<IAnalysisDataset> datasets, double x,
			double y) {
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
			dataset.getCollection().offsetConsensus(x, y);
			UIController.getInstance().fireConsensusNucleusChanged(dataset);
		}
	}

	@Override
	public void consensusTranslationResetReceived(List<IAnalysisDataset> datasets) {
		for (IAnalysisDataset d : datasets) {
			if (d.getCollection().hasConsensus()) {
				d.getCollection().offsetConsensus(0, 0);
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
	public void consensusSVGExportRequestReceived(List<IAnalysisDataset> datasets) {
		exportConsensusNuclei(datasets);
	}

	@Override
	public void landmarkUpdateEventReceived(LandmarkUpdateEvent event) {
		if (event.dataset != null) {
			ThreadManager.getInstance().execute(
					new UpdateLandmarkAction(event.dataset, event.lm, event.newIndex, acceptor));
		}
	}

	@Override
	public void segmentStartIndexUpdateEventReceived(SegmentStartIndexUpdateEvent event) {

		if (event.isDataset()) {
			ThreadManager.getInstance().execute(
					new UpdateSegmentIndexAction(event.dataset, event.id, event.index,
							acceptor));

		}

		if (event.isCell()) {
			Runnable r = () -> {
				try {
					event.dataset.getCollection().getProfileManager()
							.updateCellSegmentStartIndex(event.cell, event.id, event.index);

				} catch (MissingDataException | SegmentUpdateException e) {
					LOGGER.warning("Cannot update this segment start index");
				} finally {
					UIController.getInstance().fireCellUpdatedEvent(event.dataset, event.cell);
				}
			};
			ThreadManager.getInstance().execute(r);
		}
	}

	@Override
	public void segmentMergeEventReceived(SegmentMergeEvent event) {
		if (event.dataset != null) {
			ThreadManager.getInstance().execute(
					new SegmentMergeAction(event.dataset, event.id1, event.id2, acceptor));
		}
	}

	@Override
	public void segmentUnmergeEventReceived(SegmentUnmergeEvent event) {
		if (event.dataset != null) {
			ThreadManager.getInstance().execute(
					new SegmentUnmergeAction(event.dataset, event.id, acceptor));
		}
	}

	@Override
	public void segmentSplitEventReceived(SegmentSplitEvent event) {
		if (event.dataset != null) {
			ThreadManager.getInstance().execute(
					new SegmentSplitAction(event.dataset, event.id, acceptor));
		}
	}

	@Override
	public void profileWindowProportionUpdateEventReceived(
			ProfileWindowProportionUpdateEvent event) {
		Runnable r = () -> {
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
			} catch (MissingDataException | SegmentUpdateException e) {
				LOGGER.warning("Unable to update profile window proportion: " + e.getMessage());
			}
		};
		ThreadManager.getInstance().execute(r);
	}

	@Override
	public void fileImportRequested(FileImportEvent f) {
		ThreadManager.getInstance()
				.execute(new GenericFileImporter(f.file(), acceptor, null, f.type()));

	}

	@Override
	public void fileImported(FileImportEvent f) {

		if (XMLNames.XML_ANALYSIS_DATASET.equals(f.type())) {
			ThreadManager.getInstance()
					.execute(new ImportDatasetAction(acceptor, f.document(), f.file(), null));
		}

		if (XMLNames.XML_WORKSPACE.equals(f.type())) {
			ThreadManager.getInstance()
					.execute(new ImportWorkspaceAction(acceptor, f.document(), f.file()));
		}

		if (XMLNames.XML_ANALYSIS_OPTIONS.equals(f.type())) {
			ThreadManager.getInstance()
					.execute(new ImportWorkflowAction(acceptor, f.file()));
		}

	}

	private void exportConsensusNuclei(List<IAnalysisDataset> datasets) {

		if (datasets.isEmpty())
			return;

		if (datasets.stream().map(IAnalysisDataset::getCollection)
				.noneMatch(ICellCollection::hasConsensus))
			return;

		String defaultFileName = datasets.size() > 1
				? "Outlines"
				: datasets.get(0).getName();
		File defaultFolder = FileUtils.commonPathOfDatasets(datasets);

		try {
			File exportFile = new DefaultInputSupplier().requestFileSave(defaultFolder,
					defaultFileName,
					Io.SVG_FILE_EXTENSION_NODOT);

			// If the file exists, confirm before overwriting
			if (exportFile.exists()) {
				if (!new DefaultInputSupplier().requestApproval("Overwrite existing file?",
						"Confirm overwrite"))
					return;
			}

			SVGWriter wr = new SVGWriter(exportFile);

			String[] scaleChoices = new String[] { MeasurementScale.MICRONS.toString(),
					MeasurementScale.PIXELS.toString() };

			int scaleChoice = new DefaultInputSupplier().requestOption(scaleChoices,
					"Choose scale");

			MeasurementScale scale = scaleChoice == 0 ? MeasurementScale.MICRONS
					: MeasurementScale.PIXELS;
			wr.exportConsensusOutlines(datasets, scale);
		} catch (RequestCancelledException e) {
			// User cancelled
		}
	}

}
