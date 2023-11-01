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
package com.bmskinner.nma.gui;

/**
 * Hold tool tip labels for the various options panels
 * 
 * @author bms41
 *
 */
public class Labels {

	public static final String EMPTY_STRING = "";
	public static final String DATASET = "Dataset";
	public static final String NA = "N/A";
	public static final String NA_MERGE = "N/A - dataset is a merge and sources have multiple values";
	public static final String NUCLEI = "nuclei";

	public static final String REQUIRES_CONSENSUS_LBL = "Requires consensus nucleus";

	/*
	 * Table headers
	 */
	public static final String INCONSISTENT_SEGMENT_NUMBER = "Segment number is not consistent across datasets";
	public static final String NO_DATA_LOADED = "No data loaded";
	public static final String LOADING_DATA = "Loading data...";
	public static final String SINGLE_DATASET = "Single dataset selected";
	public static final String MULTIPLE_DATASETS = "Multiple datasets selected";
	public static final String NULL_DATASETS = "No datasets selected";

	public static class Consensus {

		public static final String RESET_LBL = "!";
		public static final String RESET_ROTATION_TOOLTIP = "Reset rotation to orientation point";
		public static final String RESET_COM_TOOLTIP = "Reset centre of mass to 0,0";

		public static final String INCREASE_X_LBL = "+x";
		public static final String DECREASE_X_LBL = "-x";
		public static final String INCREASE_Y_LBL = "+y";
		public static final String DECREASE_Y_LBL = "-y";

		public static final String INCREASE_X_TOOLTIP = "Move centre of mass x+1";
		public static final String DECREASE_X_TOOLTIP = "Move centre of mass x-1";
		public static final String INCREASE_Y_TOOLTIP = "Move centre of mass y+1";
		public static final String DECREASE_Y_TOOLTIP = "Move centre of mass y-1";

		public static final String INCREASE_ROTATION_LBL = "+r";
		public static final String DECREASE_ROTATION_LBL = "-r";

		public static final String INCREASE_ROTATION_TOOLTIP = "Rotate anti-clockwise 1 degree";
		public static final String DECREASE_ROTATION_TOOLTIP = "Rotate clockwise 1 degree";

		public static final String REFOLD_BTN_LBL = "Make consensus";
		public static final String RE_REFOLD_LBL = "Remake consensus";
	}

	/**
	 * Labels from the populations panel and popup menus
	 * 
	 * @author ben
	 *
	 */
	public static class Populations {

		public static final String MOVE_UP_LBL = "Move up";
		public static final String MOVE_DOWN_LBL = "Move down";

		public static final String SAVE_AS_LBL = "Save nmd as...";

		public static final String MERGE_LBL = "Merge";

		public static final String ARITHMETIC_LBL = "Boolean";
		public static final String DELETE_LBL = "Delete (child)";
		public static final String DELETE_TOOLTIP = "Delete the selected child datasets";

		public static final String CLOSE_LBL = "Close (root)";
		public static final String CLOSE_TOOLTIP = "Close the selected root datasets";
		public static final String CURATE_LBL = "Curate";

		public static final String CHANGE_FOLDER_LBL = "Change folder";

		public static final String EXTRACT_CELLS_LBL = "Extract cells";

		public static final String ADD_TO_WORKSPACE_LBL = "Add to workspace";
		public static final String REMOVE_FROM_WORKSPACE_LBL = "Remove from workspace";

		public static final String ADD_TO_NEW_LBL = "Add to new";
		public static final String ADD_TO_LBL_PREFIX = "Add to ";
		public static final String REMOVE_FROM_LBL_PREFIX = "Remove from ";

		public static final String ADD = "Add...";
		public static final String ADD_NUCLEAR_SIGNAL_LBL = "Add nuclear signal";
		public static final String ADD_NUCLEAR_SIGNAL_TIP = "Run on root datasets only";

		public static final String ADD_SHELLS_SIGNAL_LBL = "Shell analysis of signals";
		public static final String ADD_SHELLS_SIGNAL_TIP = "Run on root datasets only";

		public static final String WARP_BTN_LBL = "Warping of signals";
		public static final String WARP_BTN_TOOLTIP = "Requires single dataset with consensus nucleus and signals";

		public static final String POST_FISH_MAPPING_LBL = "Post-FISH mapping";
		public static final String POST_FISH_MAPPING_TOOLTIP = "Match FISH images with pre-FISH nuclei";
		public static final String ADD_CHILD_CELLS_LBL = "Child dataset from cell location file";
		public static final String ADD_CHILD_CELLS_TOOLTIP = "Select cells based on image name and location of centre-of-mass";

		public static final String CLUSTER = "Cluster...";
		public static final String ADD_CLUSTER_MANUAL_LBL = "Manually";
		public static final String ADD_CLUSTER_MANUAL_TOOLTIP = "Choose which cells to put in each cluster";
		public static final String ADD_CLUSTER_AUTO_LBL = "Automatically";
		public static final String ADD_CLUSTER_AUTO_TOOLTIP = "Assign cells to clusters using measured values";
		public static final String ADD_CLUSTER_FILE_LBL = "From cluster file";
		public static final String ADD_CLUSTER_FILE_TOOLTIP = "Import a map of cells to clusters created externally";

		public static final String CHANGE_SCALE_LBL = "Set scale";

		public static final String EXPORT = "Export...";
		public static final String EXPORT_STATS = "Nuclear measurements";
		public static final String EXPORT_PROFILES = "Full nuclear profiles";
		public static final String EXPORT_OUTLINES = "Full nuclear outlines";
		public static final String EXPORT_SIGNALS = "Nuclear signal measurements";
		public static final String EXPORT_SHELLS = "Nuclear signal shells";
		public static final String EXPORT_CELL_LOCS = "Cell locations within images";
		public static final String EXPORT_OPTIONS = "Dataset analysis options";
		public static final String EXPORT_RULESETS = "Landmark rulesets";
		public static final String EXPORT_XML_DATASET = "XML format dataset";
		public static final String EXPORT_CELL_IMAGES = "Single cell images";
		public static final String EXPORT_CONSENSUS = "Consensus nuclei as SVG";
	}

	/**
	 * Labels relating to signals for tables and UI
	 * 
	 * @author ben
	 *
	 */
	public static class Signals {
		public static final String SIGNAL_COLOUR_LABEL = "  "; // whitespace that will not be
																// collapsed
		public static final String SIGNAL_GROUP_LABEL = "Signal group name";
		public static final String NUMBER_OF_SIGNAL_GROUPS = "Number of signal groups";
		public static final String SIGNALS_LABEL = "Signals";
		public static final String SIGNAL_LABEL_SINGULAR = "Signal";
		public static final String SIGNALS_PER_NUCLEUS = "Signals per nucleus";
		public static final String AVERAGE_POSITION = "Average shell";
		public static final String SIGNAL_ID_LABEL = "ID";
		public static final String NO_SIGNAL_GROUPS = "No signal groups in datasets";
		public static final String CHOOSE_SIGNAL_COLOUR = "Choose signal color";
		public static final String SIGNAL_SOURCE_LABEL = "Source (double click to change)";
		public static final String MIN_CIRC_LBL = "Min circularity";
		public static final String MAX_CIRC_LBL = "Max circularity";
		public static final String MIN_SIZE_LBL = "Min size (pixels)";
		public static final String MAX_SIZE_LBL = "Max fraction";
		public static final String DETECTION_MODE_LBL = "Detection mode";
		public static final String THRESHOLD_LBL = "Threshold";
		public static final String SIGNAL_CHANNEL_LABEL = "Channel";

		public static final String SHOW_SIGNAL_RADII_LBL = "Show signal radii";
		public static final String MERGE_BTN_LBL = "Merge signal groups";

		/*
		 * signal detection
		 */
		public static final String FORWARD_THRESHOLDING_RADIO_LABEL = "<html>Includes all pixels over the threshold. </html>";

		public static final String REVERSE_THRESHOLDING_RADIO_LABEL = "<html>Starts with the brightest pixels <br>"
				+ "(intensity 255), and tries to find objects meeting <br>"
				+ "size and shape criteria. If no objects are found, <br>"
				+ "pixels with intensity 254 or above are considered. <br>"
				+ "This recurses until either a signal is found, <br>"
				+ "or the signal threshold is reached.<br><br>"
				+ "This will detect <i>locations</i> of signals surrounded by <br>"
				+ "bright background, but will not detect signal <i>sizes</i> <br>"
				+ "accurately</html>";

		public static final String ADAPTIVE_THRESHOLDING_RADIO_LABEL = "<html>Experimental. Attempts to distinguish<br>"
				+ "signal fom background based on the pixel<br>" + "intensity histogram.</html>";

		// public static final String ADAPTIVE_THRESHOLDING_RADIO_LABEL =
		// "<html>The intensity histogram within the nuclear <br> "
		// + "bounding box is trimmed to the minimum signal <br> "
		// + "threshold then scanned for the position with <br>"
		// + " maximum dropoff. <br>"
		// + "Formally, in the delta profile, this is the <br>"
		// + "local minimum: <br>"
		// + "(a) below zero <br>"
		// + "(b) with an absolute value greater than 10% of the <br>"
		// + " total intensity range of the trimmed profile <br>"
		// + "(c) with the highest index. <br><br>"
		// + "Since this position lies in the middle of the dropoff, <br>"
		// + "a (currently) fixed offset is added to the index to <br>"
		// + "remove remaining background. <br>"
		// + "This index is used as the new threshold for the detector. <br>"
		// + "If a suitable position is not found, we fall back to the <br>"
		// + "minimum signal threshold defined in the options.</html>";

		public static final String MINIMUM_SIGNAL_AREA = "The smallest number of pixels a signal can contain";
		public static final String MAXIMUM_SIGNAL_FRACTION = "The largest size of a signal, as a fraction of the nuclear area (0-1)";

		/**
		 * Labels for the signal warping dialog
		 * 
		 * @author ben
		 *
		 */
		public class Warper {
			public static final String TABLE_HEADER_SOURCE_DATASET = "Dataset";
			public static final String TABLE_HEADER_SOURCE_SIGNALS = "Signals";
			public static final String TABLE_HEADER_SIGNALS_ONLY = "Only cells with signals?";
			public static final String TABLE_HEADER_BINARISED = "Binarised";
			public static final String TABLE_HEADER_NORMALISED = "Normalised";
			public static final String TABLE_HEADER_TARGET_SHAPE = "Target";
			public static final String TABLE_HEADER_KEY_COLUMN = "Keys";
			public static final String TABLE_HEADER_N_CELLS = "Cells";
			public static final String TABLE_HEADER_THRESHOLD = "Threshold";
			public static final String TABLE_HEADER_COLOUR_COLUMN = "Colour";
			public static final String TABLE_HEADER_DELETE_COLUMN = "Delete";

		}

	}

	/**
	 * Labels relating to stats for tables and UI
	 * 
	 * @author ben
	 *
	 */
	public static class Stats {
		public static final String SPEARMANS_RHO = "Spearman's Rho";
		public static final String PROBABILITY = "p";
	}

	/**
	 * Labels relating to clusters for tables and UI
	 * 
	 * @author ben
	 *
	 */
	public static class Clusters {

		/*
		 * Clustering and tree building
		 */
		public static final String HIERARCHICAL_CLUSTER_METHOD = "The distance measure to use";
		public static final String USE_MODALITY_REGIONS = "Should profile angles with the lowest dip-test p-values "
				+ "be used in the clustering";
		public static final String NUMBER_MODALITY_REGIONS = "The number of dip-test p-values to "
				+ "be used in the clustering";
		public static final String USE_SIMILARITY_MATRIX = "<html>If selected, use the difference between each nucleus profile<br>"
				+ "and every other nucleus for clustering.<br>Otherwise, use area, circularity and aspect ratio</html>";

		public static final String CLUSTER_GROUP = "Cluster group";
		public static final String CLUSTER_FOUND = "Number of clusters";
		public static final String CLUSTER_METHOD = "Clustering method";
		public static final String CLUSTER_PARAMS = "Parameters";
		public static final String CLUSTER_DIM_RED = "Dimensional reduction";
		public static final String CLUSTER_DIM_PLOT = "Dimensional reduction plot";
		public static final String CLUSTER_SHOW_TREE = "View tree";

		public static final String HC_ITERATIONS = "Iterations";
		public static final String HC_METHOD = "Hierarchical method";
		public static final String TARGET_CLUSTERS = "Target cluster number";
		public static final String INCLUDE_PROFILE = "Include profile";
		public static final String PROFILE_TYPE = "Profile type";
		public static final String INCLUDE_MESH = "Include mesh";
		public static final String INCLUDE_SEGMENTS = "Include segments";
		public static final String TREE = "Hierarchical tree";
		public static final String VIEW_PLOT = "View plot";

		public static final String TSNE = "Barnes-Hut t-SNE";
		public static final String TSNE_PERPLEXITY = "Perplexity";
		public static final String TSNE_MAX_ITER = "Max iterations";

		public static final String UMAP = "UMAP";
		public static final String UMAP_N_NEIGHTBOURS = "Nearest neighbours";
		public static final String UMAP_MIN_DIST = "Min distance";

		public static final String PCA = "PCA";
		public static final String PCA_VARIANCE = "Variance";
		public static final String PCA_NUM_PCS = "Number of PCs";

		public static final String USE_FOR_CLUSTERING = "Use for clustering";
	}

	public static class Cells {
		public static final String SOURCE_FILE_LABEL = "Source image file";
		public static final String SOURCE_FILE_NAME_LABEL = "Source image name";
		public static final String SOURCE_CHANNEL_LABEL = "Source channel";
		public static final String ANGLE_WINDOW_PROP_LABEL = "Profile window prop.";
		public static final String ANGLE_WINDOW_SIZE_LABEL = "Profile window size";
		public static final String SCALE_LABEL = "Scale (pixels/um)";

		public static final String CHOOSE_NEW_SCALE_LBL = "Choose the new scale: pixels per micron";

		// Profiles tab
		public static final String PROFILES_PANEL_TITLE_LBL = "Profiles";
		public static final String REVERSE_PROFILE_BTN_LBL = "Reverse profile";

	}

	public static class AnalysisParameters {
		public static final String PROFILE_WINDOW = "Profile window";
		public static final String IMAGE_PREPROCESSING = "Image filtering";
		public static final String NUCLEUS_DETECTION = "Nucleus detection";
		public static final String NUCLEUS_CIRCULARITY = "Nucleus circularity";
		public static final String NUCLEUS_SIZE = "Nucleus area";
		public static final String COLLECTION_SOURCE = "Original image source";
		public static final String PIXEL_SCALE = "Pixel scale";
		public static final String RUN_TIME = "Run time";
		public static final String RULESET_USED = "Landmark ruleset";
		public static final String SOFTWARE_VERSION = "Created in";
	}

	public static class AnalysisRulesets {
		public static final String RULESET_NAME = "Landmark ruleset";
		public static final String RULE_APPLICATON_TYPE = "Rule type";
		public static final String RULESET_PROFILE = "Profile type";
		public static final String RULE_NAME = "Rule";
		public static final String LANDMARK_NAME = "Landmark";
		public static final String ORIENTATION_NAME = "Orientation";
		public static final String MEASUREMENT_NAME = "Measureable value";
		public static final String RULE_VALUES = "Values";

	}

	public static class Merges {
		public static final String NO_MERGE_SOURCES = "No merge sources";
		public static final String MERGE_SOURCE = "Merge sources";
		public static final String RECOVER_SOURCE = "Recover merge source";
	}

	public static class EditingBorderTags {
		public static final String RULESET_DIALOG_TITLE = "RuleSets for ";
		public static final String RULESET_DIALOG_HEADER_0 = "This window displays the rules that are used to find points in the outlines of nuclei";
		public static final String RULESET_DIALOG_HEADER_1 = "Rules are combined via logical AND to determine final valid positions";
		public static final String RULESET_DIALOG_CUSTOM_BTN = "Custom";
		public static final String RULESET_DIALOG_IMPORT_BTN = "Import";
		public static final String RULESET_DIALOG_SELECT_FILE = "Select ruleset file";
		public static final String RULESET_DIALOG_SAVE_CHOICE = "Do you want to save some or all custom rulesets?";
		public static final String RULESET_DIALOG_SAVE_TITLE = "Save ruleset(s)?";
		public static final String RULESET_DIALOG_SAVE_OPTION = "Save rulesets";
		public static final String RULESET_DIALOG_DISCARD_OPTION = "Discard rulesets";
	}

}
