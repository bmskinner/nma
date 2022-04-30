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
package com.bmskinner.nma.gui.events;

import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * An event class to signal a UI event has been triggered
 * 
 * @author bms41
 *
 */
public class UserActionEvent extends EventObject {

	public static final String POST_FISH_MAPPING = "PostFISHRemappingAction";

	public static final String EXPORT_STATS = "ExportStatsAction";
	public static final String EXPORT_PROFILES = "ExportProfilesAction";
	public static final String EXPORT_OUTLINES = "ExportOutlinesAction";
	public static final String EXPORT_SIGNALS = "ExportSignalsAction";
	public static final String EXPORT_SHELLS = "ExportShellsAction";
	public static final String EXPORT_CELL_LOCS = "SaveCellLocations";
	public static final String EXPORT_OPTIONS = "ExportOptionsAction";
	public static final String EXPORT_RULESETS = "ExportRulesetsAction";
	public static final String EXPORT_SINGLE_CELL_IMAGES = "ExportSingleCellImagesAction";

	public static final String CHANGE_SCALE = "ChangeScaleAction";
	public static final String RELOCATE_CELLS = "RelocateCellsAction";

	public static final String NEW_ANALYSIS_PREFIX = "New|";
	public static final String IMPORT_DATASET_PREFIX = "Open|";
	public static final String IMPORT_WORKFLOW_PREFIX = "Flow|";

	public static final String IMPORT_WORKSPACE_PREFIX = "Wrk|";
	public static final String SAVE_WORKSPACE = "ExportWorkspace";
	public static final String NEW_WORKSPACE = "NewWorkspace";

	public static final String ADD_NUCLEAR_SIGNAL = "AddNuclearSignalAction";
	public static final String SIGNAL_WARPING = "SignalWarpingAction";

	public static final String CLUSTER_FROM_FILE = "AddClusterFromFileAction";

	public static final String DATASET_ARITHMETIC = "DatasetArithmeticAction";
	public static final String CHANGE_NUCLEUS_IMAGE_FOLDER = "ChangeNucleusFolderAction";
	public static final String EXTRACT_SUBSET = "Extract subset";

	public static final String SAVE_SELECTED_DATASETS = "SaveSelectedDatasetsAction";
	public static final String SAVE_ALL_DATASETS = "SaveAllDatasetsAction";
	public static final String SAVE_ALL_WORKSPACES = "SaveAllWorkspacesAction";

	public static final String SAVE_SELECTED_DATASET = "SaveCollectionAction";
	public static final String EXPORT_XML_DATASET = "ExportXMLDatasetAction";
	public static final String EXPORT_TPS_DATASET = "ExportTPSDatasetAction";
	public static final String MERGE_DATASETS_ACTION = "MergeCollectionAction";
	public static final String MERGE_SIGNALS_ACTION = "MergeSignalsAction";
	public static final String DELETE_DATASET = "DeleteCollectionAction";
	public static final String CURATE_DATASET = "CurateCollectionAction";

	public static final String MOVE_DATASET_UP_ACTION = "MoveDatasetUpAction";
	public static final String MOVE_DATASET_DOWN_ACTION = "MoveDatasetDownAction";

	public static final String NEW_WORKSPACE_PREFIX = "NewWorkspace|";
	public static final String ADD_TO_WORKSPACE = "AddToWorkspace";
	public static final String ADD_WORKSPACE = "Add workspace";
	public static final String REMOVE_FROM_WORKSPACE_PREFIX = "RemoveFromWorkspace|";

//	public static final String NEW_BIOSAMPLE_PREFIX = "NewBioSample|";
//	public static final String ADD_TO_BIOSAMPLE_PREFIX = "AddToBioSample|";
//	public static final String REMOVE_FROM_BIOSAMPLE_PREFIX = "RemoveFromBioSample|";

	/** Profile and segment */
	public static final String MORPHOLOGY_ANALYSIS_ACTION = "Morphology analysis action";

	public static final String PROFILING_ACTION = "Profiling action";

	/** Run segmentation on the datasets using existing tagged profiles */
	public static final String SEGMENTATION_ACTION = "Segment dataset";

	/**
	 * Signal that segments should be copied from the secondary dataset in the
	 * action to all primary datasets listed
	 */
	public static final String COPY_PROFILE_SEGMENTATION = "Copy profile segmentation";

	/** Keep the current median segmentation, and update nuclei to match */
	public static final String APPLY_MEDIAN_TO_NUCLEI = "Refresh morphology";

	/**
	 * Run new profiling and segmentation on the given datasets. Do not attempt to
	 * add as a new population.
	 */
	public static final String REFPAIR_SEGMENTATION = "Repair segmentation";

	public static final String REFOLD_CONSENSUS = "Refold consensus";
	public static final String SELECT_DATASETS = "Select multiple datasets";
	public static final String SELECT_ONE_DATASET = "Select single dataset";
	public static final String EXTRACT_SOURCE = "Extract source";
	public static final String CLUSTER = "Cluster";
	public static final String MANUAL_CLUSTER = "Manual cluster";
	public static final String BUILD_TREE = "Build tree";
	public static final String TRAIN_CLASSIFIER = "Train classifier";

	public static final String SAVE = "Save selected";
	public static final String SAVE_AS = "Save as new file";
	public static final String RESEGMENT = "Resegment dataset";

	/**
	 * Rerun the profiling action to generate new median profiles. Also recalculates
	 * the positions of border tags using built in rules. Does not recache charts.
	 */
	public static final String RECALCULATE_MEDIAN = "Recalculate median profiles";
	public static final String RUN_SHELL_ANALYSIS = "Run shell analysis";

	public static final String RUN_GLCM_ANALYSIS = "Run GLCM analysis";

	private static final long serialVersionUID = 1L;
	private String message;

	private final List<IAnalysisDataset> datasets = new ArrayList<>();

	/** for use in e.g. copying data from one dataset to others */
	private IAnalysisDataset secondaryDataset = null;

	/**
	 * Create an event from a source class, with the given message
	 * 
	 * @param source
	 * @param type
	 */
	public UserActionEvent(@NonNull Object source, @NonNull String message) {
		this(source, message, null, null);
	}

	/**
	 * Create an event from a source class, with the given message and datasets to
	 * process
	 * 
	 * @param source
	 * @param type
	 */
	public UserActionEvent(@NonNull Object source, @NonNull String message,
			List<IAnalysisDataset> datasets) {
		this(source, message, datasets, null);
	}

	/**
	 * Create an event from a source class, with the given message and datasets to
	 * process, including a secondary dataset slot
	 * 
	 * @param source
	 * @param type
	 */
	public UserActionEvent(@NonNull Object source, @NonNull String message,
			List<IAnalysisDataset> datasets,
			IAnalysisDataset second) {
		super(source);
		this.message = message;
		if (datasets != null)
			this.datasets.addAll(datasets);
		this.secondaryDataset = second;
	}

	public UserActionEvent(UserActionEvent event) {
		super(event.source);
		this.message = event.message;
		this.datasets.addAll(event.datasets);
		this.secondaryDataset = event.secondaryDataset;
	}

	/**
	 * The type of event, or other message to carry
	 * 
	 * @return
	 */
	public @NonNull String type() {
		return message;
	}

	public @NonNull List<IAnalysisDataset> getDatasets() {
		return datasets;
	}

	public @Nullable IAnalysisDataset getSecondaryDataset() {
		return secondaryDataset;
	}
}
