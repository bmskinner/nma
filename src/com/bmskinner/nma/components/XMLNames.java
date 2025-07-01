package com.bmskinner.nma.components;

/**
 * Contains the keys used to construct XML for nmd files
 * 
 * @author Ben Skinner
 *
 */
public class XMLNames {

	// From workspaces

	public static final String XML_WORKSPACE = "workspace";
	public static final String XML_WORKSPACE_NAME = "name";
	public static final String XML_WORKSPACE_DATASET_ELEMENT = "dataset";
	public static final String XML_DATASETS_ELEMENT = "datasets";
	public static final String XML_DATASET_PATH = "path";

	public static final String XML_ANALYSIS_DATASET = "AnalysisDataset";

	// From datasets
	public static final String XML_SAVE_FILE = "SaveFile";

	public static final String XML_CELL_COLLECTION = "CellCollection";

	public static final String XML_PARENT = "Parent";

	public static final String XML_MERGE_SOURCE = "MergeSource";

	public static final String XML_OTHER_DATASETS = "OtherDatasets";

	public static final String XML_CHILD_DATASETS = "ChildDatasets";

	public static final String XML_ANALYSIS_OPTIONS = "AnalysisOptions";

	public static final String XML_CLUSTER_GROUP = "ClusterGroup";

	public static final String XML_COLOUR = "Colour";

	public static final String XML_VERSION_LAST_SAVED = "VersionLastSaved";

	public static final String XML_VERSION_CREATED = "VersionCreated";

	public static final String XML_VIRTUAL_DATASET = "VirtualDataset";

	// From cell collections

	public static final String XML_UNNAMED = "unnamed";

	public static final String XML_ID = "id";

	public static final String XML_NAME = "name";

	public static final String XML_PROFILE_COLLECTION = "ProfileCollection";

	public static final String XML_CONSENSUS_NUCLEUS = "ConsensusNucleus";

	public static final String XML_CELL = "Cell";

	public static final String XML_SIGNAL_GROUP = "SignalGroup";

	// From profile collections

	/** XML identifier for mapping an orientation to a landmark */
	public static final String XML_ORIENT = "Orient";

	// From measurements

	/** XML identifier for a valid measurement */
	public static final String XML_MEASUREMENT = "Measurement";

	/** XML identifier for a valid array measurement */
	public static final String XML_ARRAY_MEASUREMENT = "ArrayMeasurement";


	public static final String XML_DIMENSION = "dim";

	// From cluster groups

	public static final String XML_NEWICK = "NewickTree";
	public static final String XML_OPTIONS = "Options";
	public static final String XML_DATASET_ID = "DatasetId";

	// From cellular components

	public static final String XML_REVERSE = "reverse";

	public static final String XML_YPOINTS = "ypoints";

	public static final String XML_XPOINTS = "xpoints";

	public static final String XML_SCALE = "Scale";

	public static final String XML_CHANNEL = "Channel";

	public static final String XML_SOURCE_FILE = "SourceFile";

	public static final String XML_COMPONENT = "Component";

	public static final String XML_ORIGINAL_CENTRE_OF_MASS = "OriginalCentreOfMass";

	public static final String XML_Y = "y";

	public static final String XML_X = "x";

	public static final String XML_COM = "CoM";
	public static final String XML_VALUE = "value";

	// From profilable components

	public static final String XML_WINDOW_PROPORTION = "window";
	public static final String XML_ORIENTATION = "Orientation";

	/**
	 * XML identifier for a priority axis (is horizontal or vertical more
	 * important?)
	 */
	public static final String XML_PRIORITY_AXIS = "axis";

	public static final String XML_LOCKED = "locked";

	/** XML identifier for landmark */
	public static final String XML_LANDMARK = "Landmark";
	public static final String XML_INDEX = "index";
	public static final String XML_SEGMENT = "Segment";

	// From cells
	public static final String XML_NUCLEUS = "Nucleus";

	// From nuclei

	public static final String XML_NUCLEUS_NUMBER = "number";

	public static final String XML_SIGNAL_COLLECTION = "SignalCollection";

	// From consensus nuclei
	public static final String XML_OFFSET = "Offset";
	public static final String XML_R = "r";

	// From analysis options

	public static final String XML_SECONDARY = "Secondary";

	/** XML identifier for a ruleset collection */
	public static final String XML_RULE_SET_COLLECTION = "RuleSetCollection";

	public static final String XML_PROFILE_WINDOW = "ProfileWindow";

	public static final String XML_DETECTION = "Detection";
	public static final String XML_FOLDER = "folder";

	public static final String XML_ANALYSIS_TIME = "runtime";

	public static final String XML_INTEGER_KEY = "Integer";
	public static final String XML_FLOAT_KEY = "Float";
	public static final String XML_DOUBLE_KEY = "Double";
	public static final String XML_STRING_KEY = "String";
	public static final String XML_BOOLEAN_KEY = "Boolean";
	public static final String XML_SUBOPTION_KEY = "Suboption";

	// From profile segments
	public static final String XML_SEGMENT_START = "start";
	public static final String XML_SEGMENT_END = "end";
	public static final String XML_SEGMENT_TOTAL = "total";
	public static final String XML_SEGMENT_LOCK = "lock";
	public static final String XML_SEGMENT_MERGE_SOURCE = "MergeSource";

	// From profiles
	public static final String XML_PROFILE = "Profile";

	// From rules
	public static final String XML_RULE = "Rule";
	public static final String XML_RULE_VALUE = "Value";
	public static final String XML_RULE_TYPE = "type";

	/** XML identifier for a ruleset */
	public static final String XML_RULESET = "Ruleset";

	/** XML identifier for a rule application type */
	public static final String XML_RULE_APPLICATION_TYPE = "application";

	/** XML identifier for a rule version */
	public static final String XML_RULE_VERSION = "version";

	public static final String XML_RULESET_COLLECTION = "RuleSetCollection";

	// From signals and shells

	public static final String XML_SIGNAL = "Signal";
	public static final String XML_SIGNAL_CLOSEST_BORDER = "ClosestBorder";

	public static final String XML_SIGNAL_N_SHELLS = "nShells";
	public static final String XML_SIGNAL_SHELL_SHRINK_TYPE = "ShrinkType";
	public static final String XML_SIGNAL_SHELL_COUNT = "ShellCount";
	public static final String XML_SIGNAL_SHELL_COUNT_TYPE = "CountType";
	public static final String XML_SIGNAL_SHELL_RESULT = "ShellResult";

	public static final String XML_SIGNAL_SHELL_VALUES = "ShellValues";

	public static final String XML_SIGNAL_SHELL_CELLID = "CellId";
	public static final String XML_SIGNAL_SHELL_COMPONENTID = "ComponentId";
	public static final String XML_SIGNAL_SHELL_SIGNALID = "SignalId";

	public static final String XML_SIGNALS = "Signals";
	public static final String XML_SIGNALGROUP_ID = "group";

	public static final String XML_SIGNAL_IS_VISIBLE = "isVisible";

	public static final String XML_SIGNAL_COLOUR = "colour";

	// From warped signals

	public static final String XML_WARPED_SIGNAL = "WarpedSignal";
	public static final String XML_WARPED_SIGNAL_TARGET_NAME = "targetName";
	public static final String XML_WARPED_SIGNAL_SOURCE_DATASET = "sourceDataset";
	public static final String XML_WARPED_SIGNAL_SOURCE_SIGNAL = "sourceSignal";
	public static final String XML_WARPED_SIGNAL_SOURCE_DATASET_ID = "source";
	public static final String XML_WARPED_SIGNAL_DETECTION_THRESHOLD = "threshold";
	public static final String XML_WARPED_SIGNAL_IS_SIGNALS_ONLY = "isSignalsOnly";
	public static final String XML_WARPED_SIGNAL_IS_BINARISED = "isBinarised";
	public static final String XML_WARPED_SIGNAL_IS_NORMALISED = "isNormalised";
	public static final String XML_WARPED_SIGNAL_IMAGE_WIDTH = "width";
	public static final String XML_WARPED_SIGNAL_DISPLAY_THRESHOLD = "displayThreshold";
	public static final String XML_WARPED_SIGNAL_TARGET_SHAPE = "TargetShape";
	public static final String XML_WARPED_SIGNAL_BYTES = "Bytes";
	public static final String XML_WARPED_SIGNAL_BASE64 = "Base64";

}
