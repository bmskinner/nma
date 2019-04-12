package com.bmskinner.nuclear_morphology.io.xml;

import java.awt.Color;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.ICytoplasm;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalCollection;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.IWarpedSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.WarpedSignalKey;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.stats.Stats;

import ij.process.ByteProcessor;

/**
 * Base class for creating XML representations of datasets 
 * @author ben
 *
 * @param <T> the type of object to create
 * @since 1.14.0
 */
public abstract class XMLCreator<T> {
	
	public static final String ANALYSIS_DATASET_KEY           = "AnalysisDataset";
	public static final String CELL_COLLECTION_KEY            = "CellCollection";
	public static final String CELLS_SECTION_KEY              = "Cells";
	public static final String OUTPUT_FOLDER_KEY              = "OutputFolder";
	public static final String NUCLEUS_TYPE_KEY               = "NucleusType";
	public static final String NUCLEUS_NUMBER_KEY             = "NucleusNumber";
	public static final String CELL_IDS_KEY                   = "CellIds";
	public static final String DATASET_IDS_KEY                = "DatasetIds";
	public static final String CELL_KEY                       = "Cell";
	public static final String ID_KEY                         = "Id";
	public static final String COM_KEY                        = "CentreOfMass";
	public static final String X                              = "X";
	public static final String Y                              = "Y";
	public static final String BASE_KEY                       = "Base";
	public static final String X_BASE_KEY                     = "XBase";
	public static final String Y_BASE_KEY                     = "YBase";
	public static final String W_BASE_KEY                     = "WBase";
	public static final String H_BASE_KEY                     = "HBase";
	public static final String BORDER_POINTS_KEY              = "UninterpolatedBorderPoints";
	public static final String POINT_KEY                      = "Point";
	public static final String NAME_KEY                       = "Name";
	public static final String VALUE_KEY                      = "Value";
	public static final String INDEX_KEY                      = "Index";
	public static final String STATS_SECTION_KEY                      = "MeasuredStatistics";
	public static final String STAT_KEY                       = "Statistic";
	public static final String COLOUR_KEY                     = "Colour";

	public static final String DATASET_NAME_KEY               = "DatasetName";
	public static final String DATASET_ID_KEY                 = "DatasetId";
	
	public static final String DATASET_ROOT_KEY               = "DatasetIsRoot";
	
	public static final String MERGE_SOURCES_SECTION_KEY      = "MergeSources";
	public static final String MERGE_SOURCE_KEY               = "MergeSource";

	
	public static final String CHILD_DATASETS_SECTION_KEY     = "ChildDatasets";
	public static final String CHILD_DATASET_KEY              = "ChildDataset";
	
	public static final String SOFTWARE_CREATION_VERSION_KEY  = "VersionCreated";
	public static final String SOFTWARE_SERIALISE_VERSION_KEY = "VersionSerialised";
	
	public static final String BOOLEAN_KEY = "Booleans";
	public static final String FLOAT_KEY   = "Floats";
	public static final String DOUBLE_KEY  = "Doubles";
	public static final String INT_KEY     = "Ints";
	public static final String STRING_KEY  = "Strings";
	public static final String PAIR_KEY    = "Option";
	public static final String KEY_KEY     = "Key";
	
	public static final String SUB_OPTION_KEY = "Sub_option";
	public static final String SUB_TYPE_KEY   = "Sub_option_type";
	
	public static final String ANALYSIS_OPTIONS_KEY    = "AnalysisOptions";
	public static final String DETECTION_SETTINGS_KEY  = "DetectionSettings";
	public static final String DETECTION_METHOD_KEY    = "DetectionMethod";
	public static final String DETECTED_OBJECT_KEY     = "DetectedObject";
	public static final String PROFILE_WINDOW_KEY      = "ProfileWindow";

	public static final String SIGNAL_DETECTION_MODE_KEY   = "Detection_mode";
	
	public static final String UUID_PREFIX = "UUID_";
	
	public static final String SIGNAL_GROUPS_SECTION_KEY   = "SignalGroups";
	public static final String SIGNAL_GROUP_KEY    = "SignalGroup";
	public static final String SIGNAL_GROUP_PREFIX = "SignalGroup_";
	public static final String SHELL_RESULT_KEY    = "ShellResult";
	
	public static final String WARPED_SIGNALS_SECTION_KEY  = "WarpedSignals";
	public static final String WARPED_SIGNAL_KEY   = "WarpedSignal";
	public static final String WARPED_TARGET_KEY   = "TargetShape";
	public static final String WARPED_SIG_ONLY_KEY = "SignalsOnly";
	public static final String WARPED_BYTE_KEY     = "ByteArray";
	public static final String WARPED_BYTE_SEP_KEY = "|"; // separate width and col byte arrays

	public static final String CLUSTERS_SECTION_KEY = "Clusters";
	public static final String CLUSTER_GROUP = "ClusterGroup";
	public static final String CLUSTER_NAME  = "ClusterGroupName";
	public static final String CLUSTER_TREE_KEY  = "ClusterGroupTree";
	
	
	public static final String SOURCE_FILE_KEY                = "SourceFile";
	public static final String SOURCE_CHANNEL_KEY             = "SourceChannel";
	public static final String SOURCE_SCALE_X_KEY             = "SourceScaleX";
	public static final String SOURCE_SCALE_Y_KEY             = "SourceScaleY";
	
	public static final String CYTOPLASM_KEY                  = CellularComponent.CYTOPLASM;
	public static final String NUCLEUS_KEY                    = CellularComponent.NUCLEUS;
	public static final String CONSENSUS_KEY                  = "ConsensusNucleus";
	
	public static final String BORDER_TAGS_KEY               = "BorderTags";
	public static final String BORDER_TAG_KEY                = "Tag";
	
	public static final String BORDER_SEGS_KEY               = "BorderSegments";
	public static final String BORDER_SEG_KEY                = "Segment";
	
	public static final String NUCLEAR_SIGNALS_KEY            = CellularComponent.NUCLEAR_SIGNAL+"s";
	public static final String NUCLEAR_SIGNAL_KEY             = CellularComponent.NUCLEAR_SIGNAL;
	public static final String NUCLEAR_SIGNAL_GROUP_KEY       = "SignalGroup";
	
	public static final String BORDER_LENGTH_KEY             = "InterpolatedBorderLength";
	public static final String PROFILE_LENGTH_KEY            = "ProfileLength";
	
	protected final T template;
	
	public XMLCreator(T template) {
		this.template = template;
	}
	
	/**
	 * Create an XML representation of the object
	 * @return
	 */
	public abstract Document create();
	
	
	/**
	 * Create an XML key-value element from an arbitrary object
	 * @param key the key
	 * @param value the value. This will use the toString() method of the value
	 * @return
	 */
	protected static Element createElement(String key, Object value) {
		return createElement(key, value.toString());
	}
	
	/**
	 * Create an XML key-value element  
	 * @param key the key
	 * @param value the value
	 * @return
	 */
	protected static Element createElement(String key, String value) {
		Element e = new Element(key);
		e.setText(value);
		return e;
	}
	
	/**
	 * Test if the given string could be a UUID 
	 * @param s
	 * @return
	 */
	public static boolean isUUID(String s) {
		if(s==null)
			return false;
		if(s.length()!=36)
			return false;
		if(s.matches("[\\w|\\d]{8}-[\\w|\\d]{4}-[\\w|\\d]{4}-[\\w|\\d]{4}-[\\w|\\d]{12}"))
			return true;
		return false;
	}
	
	public String toHex(Color c) {
		return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());  
	}
	
	
	/**
	 * Create XML for the given analysis options
	 * @param collection
	 * @return
	 */
	protected Element create(IAnalysisOptions options) {
		Element e = new Element(ANALYSIS_OPTIONS_KEY);
		
		for(String key : options.getDetectionOptionTypes()){
			Element element = new Element(DETECTION_METHOD_KEY);
			if(isUUID(key) || key.startsWith(IAnalysisOptions.SIGNAL_GROUP)){ // signal group without prefix
				element.setAttribute(DETECTED_OBJECT_KEY, IAnalysisOptions.NUCLEAR_SIGNAL);
			} else {
				element.setAttribute(DETECTED_OBJECT_KEY, key);
			}
			
			// add signal group names
			if(element.getAttribute(DETECTED_OBJECT_KEY).getValue().equals(IAnalysisOptions.NUCLEAR_SIGNAL)) {
				UUID signalGroup = UUID.fromString(key.replaceAll(IAnalysisOptions.SIGNAL_GROUP, ""));
				element.addContent(createElement(ID_KEY, signalGroup));
			}
			
			appendElement(element, options.getDetectionOptions(key).get());
			e.addContent(element);
		}
		e.addContent(createElement(NUCLEUS_TYPE_KEY, options.getNucleusType().name()));
		e.addContent(createElement(PROFILE_WINDOW_KEY, String.valueOf(options.getProfileWindowProportion())));
		return e;
	}
	
	/**
	 * Create XML for the given collection of cells
	 * @param collection
	 * @return
	 */
	protected Element create(ICellCollection collection) {
		Element e = new Element(CELL_COLLECTION_KEY);
		
		if(collection.isReal()) {
			e.addContent(createElement(NUCLEUS_TYPE_KEY, collection.getNucleusType()));
			e.addContent(createElement(OUTPUT_FOLDER_KEY, collection.getOutputFolder().getAbsolutePath()));

			Element cellsElement = new Element(CELLS_SECTION_KEY);
			for(ICell cell : collection)
				cellsElement.addContent(create(cell));
			e.addContent(cellsElement);
		} else {
			Element cells = new Element(CELLS_SECTION_KEY);
			for(ICell cell : collection)
				cells.addContent(createElement(ID_KEY, cell.getId()));
			e.addContent(cells);
		}
		
		// Add consensus
		if(collection.hasConsensus()) {
			Element consensus = new Element(CONSENSUS_KEY);
			consensus.addContent(create(collection.getConsensus()));
			e.addContent(consensus);
		}
				
		e.addContent(createElement(PROFILE_LENGTH_KEY, String.valueOf(collection.getProfileCollection().length())));	
		
		// Add border tags
		Element tags = new Element(BORDER_TAGS_KEY);
		for(Tag t : collection.getProfileCollection().getBorderTags()) {
			Element tag = new Element(BORDER_TAG_KEY);
			tag.addContent(createElement(NAME_KEY, t.getName()));
			try {
				int i = collection.getProfileCollection().getIndex(t);
				tag.addContent(createElement(INDEX_KEY, String.valueOf(i)));
			} catch (UnavailableBorderTagException e1) {
				// ignore missing tags
			}
			tags.addContent(tag);
		}
		e.addContent(tags);
		
		// Add segments
		Element segs = new Element(BORDER_SEGS_KEY);
		try {
			ISegmentedProfile profile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);

			for(IBorderSegment s : profile.getSegments()) {
				Element seg = new Element(BORDER_SEG_KEY);
				seg.addContent(createElement(ID_KEY, s.getID()));
				seg.addContent(createElement(INDEX_KEY, String.valueOf(s.getStartIndex())));
				segs.addContent(seg);
			}
			e.addContent(segs);
		} catch (UnavailableProfileTypeException | UnavailableBorderTagException | ProfileException | UnsegmentedProfileException e1) {
			// ignore missing profiles
		}
		
		// Add signal groups
		if(collection.getSignalManager().hasSignals()) {
			Element signalGroups = new Element(SIGNAL_GROUPS_SECTION_KEY);
			for(UUID signalGroupId : collection.getSignalGroupIDs())
				signalGroups.addContent(create(collection.getSignalGroup(signalGroupId).get(), signalGroupId));
			e.addContent(signalGroups);
		}
		
		return e;
	}
	
	protected Element create(ISignalGroup signalGroup, UUID signalGroupId) {
		Element e = new Element(SIGNAL_GROUP_KEY);
		
		e.addContent(createElement(ID_KEY, signalGroupId));
		e.addContent(createElement(NAME_KEY, signalGroup.getGroupName()));
		if(signalGroup.hasColour())
			e.addContent(createElement(COLOUR_KEY, toHex(signalGroup.getGroupColour().get())));
		
		if(signalGroup.hasShellResult())
			e.addContent(create(signalGroup.getShellResult().get()));
		if(signalGroup.hasWarpedSignals())
			e.addContent(create(signalGroup.getWarpedSignals().get()));
		
		return e;
	}
	
	protected Element create(IShellResult shellResult) {
		Element e = new Element(SHELL_RESULT_KEY);		
		return e;
	}
	
	
	protected Element create(IWarpedSignal warpedSignal) {
		Element e = new Element(WARPED_SIGNALS_SECTION_KEY);		
		
		for(WarpedSignalKey key : warpedSignal.getWarpedSignalKeys()) {
			Element warp = new Element(WARPED_SIGNAL_KEY);	
			warp.addContent(createElement(ID_KEY, warpedSignal.getSignalGroupId()));
			warp.addContent(createElement(NAME_KEY, warpedSignal.getTargetName(key)));
			
			Element target = new Element(WARPED_TARGET_KEY);
			create(target, key.getTargetShape());
			warp.addContent(target);
			warp.addContent(createElement(WARPED_SIG_ONLY_KEY, String.valueOf(key.isCellWithSignalsOnly())));
			
			// Fetch the byte array corresponding to the warped image and stringify
			ByteProcessor bp = (ByteProcessor) warpedSignal.getWarpedImage(key).get();
			
			StringBuilder sb = new StringBuilder();
			sb.append(Base64.getEncoder().encodeToString(IWarpedSignal.toByteArray(bp)));

			warp.addContent(createElement(W_BASE_KEY, String.valueOf(bp.getWidth())));
			warp.addContent(createElement(H_BASE_KEY, String.valueOf(bp.getHeight())));
//			warp.addContent(createElement(WARPED_BYTE_KEY, sb.toString() ));
			e.addContent(warp);
		}		
		return e;
	}
	
	
	protected Element create(ICell cell) {
		Element e = new Element(CELL_KEY);
		e.addContent(createElement(ID_KEY, cell.getId()));
		if(cell.hasCytoplasm())
			e.addContent(create(cell.getCytoplasm()));
		
		for(Nucleus n : cell.getNuclei())
			e.addContent(create(n));
			

		
		return e;
	}
	
	protected Element create(ICytoplasm cytoplasm) {
		Element e = new Element(CYTOPLASM_KEY);
		create(e, cytoplasm);
		return e;
	}
	
	protected Element create(Nucleus nucleus) {
		Element e = new Element(NUCLEUS_KEY);
		create(e, nucleus);
		
		e.addContent(createElement(NUCLEUS_NUMBER_KEY, String.valueOf(nucleus.getNucleusNumber())));
		
		// Add signals
		
		ISignalCollection sc = nucleus.getSignalCollection();
		if(sc.hasSignal()) {
			Element signals = new Element(SIGNAL_GROUPS_SECTION_KEY);
			
			for(UUID signalGroupId : sc.getSignalGroupIds()) {
				
				Element group = new Element(SIGNAL_GROUP_KEY);
				group.addContent(createElement(ID_KEY, signalGroupId));
				
				List<INuclearSignal> sigList = sc.getSignals(signalGroupId);
				for(INuclearSignal s : sigList) {
					Element signal = new Element(NUCLEAR_SIGNAL_KEY);
					create(signal, s);
					group.addContent(signal);
				}
				signals.addContent(group);
			}
			e.addContent(signals);
		}
		
		return e;
	}
	
	protected Element create(Element e, Taggable taggable) {
		create(e, (CellularComponent)taggable);
		
		// Add border tags
		Element tags = new Element(BORDER_TAGS_KEY);
		for(Tag t : taggable.getBorderTags().keySet()) {
			Element tag = new Element(BORDER_TAG_KEY);
			tag.addContent(createElement(NAME_KEY, t.getName()));
			try {
				int i = taggable.getBorderIndex(t);
				tag.addContent(createElement(INDEX_KEY, String.valueOf(i)));
			} catch (UnavailableBorderTagException e1) {
				// ignore missing tags
			}
			tags.addContent(tag);
		}
		e.addContent(tags);
		
		// Add segments
		Element segs = new Element(BORDER_SEGS_KEY);
		try {
			ISegmentedProfile profile = taggable.getProfile(ProfileType.ANGLE);
			
			for(IBorderSegment s : profile.getSegments()) {
				Element seg = new Element(BORDER_SEG_KEY);
				seg.addContent(createElement(ID_KEY, s.getID()));
				seg.addContent(createElement(INDEX_KEY, String.valueOf(s.getStartIndex())));
				segs.addContent(seg);
			}
			e.addContent(segs);
		} catch (UnavailableProfileTypeException e1) {
			// ignore missing profiles
		}
	
		return e;
	}
	
	
	/**
	 * Cellular components are abstract; create the element to add to in an implementation create method.
	 * E.g. create(Nucleus)  
	 * @param e
	 * @param component
	 * @return
	 */
	private Element create(Element e, CellularComponent component) {
		e.addContent(createElement(ID_KEY, component.getID()));
		
		e.addContent(create(COM_KEY, component.getOriginalCentreOfMass()));
		e.addContent(createElement(SOURCE_FILE_KEY, component.getSourceFile().getAbsolutePath()));
		e.addContent(createElement(SOURCE_CHANNEL_KEY, String.valueOf(component.getChannel())));
		e.addContent(createElement(SOURCE_SCALE_X_KEY, String.valueOf(component.getScale())));
		e.addContent(createElement(SOURCE_SCALE_Y_KEY, String.valueOf(component.getScale())));
		e.addContent(createElement(BORDER_LENGTH_KEY, String.valueOf(component.getBorderLength())));
		
		// add base
		Element base = new Element(BASE_KEY);	
		base.addContent(createElement(X_BASE_KEY, String.valueOf(component.getPosition()[Imageable.X_BASE])));
		base.addContent(createElement(Y_BASE_KEY, String.valueOf(component.getPosition()[Imageable.Y_BASE])));
		base.addContent(createElement(W_BASE_KEY, String.valueOf(component.getPosition()[Imageable.WIDTH])));
		base.addContent(createElement(H_BASE_KEY, String.valueOf(component.getPosition()[Imageable.HEIGHT])));
		e.addContent(base);
		
		// add border points
		Element border = new Element(BORDER_POINTS_KEY);		
		int[][] points = component.getUnsmoothedBorderCoordinates();
		int[] xpoints = points[0];
		int[] ypoints = points[1];
		
		for(int i=0; i<xpoints.length; i++)
			border.addContent(create(POINT_KEY, xpoints[i], ypoints[i]));
		
		e.addContent(border);
		
		// add stats
		Element stats = new Element(STATS_SECTION_KEY);
		for(PlottableStatistic s : component.getStatistics()) {
			Element stat = new Element(STAT_KEY);
			stat.addContent(createElement(NAME_KEY, s));
			stat.addContent(createElement(VALUE_KEY, String.valueOf(component.getStatistic(s))));
			stats.addContent(stat);
		}
		e.addContent(stats);
		return e;
	}
	
	private Element create(String key , IPoint point) {
		Element e = new Element(key);
		e.addContent(createElement(X, String.valueOf(point.getX())));
		e.addContent(createElement(Y, String.valueOf(point.getY())));
		return e;
	}
	
	private Element create(String key , int x, int y) {
		Element e = new Element(key);
		e.addContent(createElement(X, String.valueOf(x)));
		e.addContent(createElement(Y, String.valueOf(y)));
		return e;
	}
	
	private static Element createKeyPairElement(String key, String value) {
		Element pair = new Element(PAIR_KEY);
		Element keyElement = new Element(KEY_KEY);
		keyElement.setText(key);
		Element valElement = new Element(VALUE_KEY);
		valElement.setText(value);
		
		pair.addContent(keyElement);
		pair.addContent(valElement);
		return pair;
	}
	
	private static Element createKeyPairElement(String key, boolean value) {
		return createKeyPairElement(key, String.valueOf(value));
	}
	
	private static Element createKeyPairElement(String key, int value) {
		return createKeyPairElement(key, String.valueOf(value));
	}
	
	private static Element createKeyPairElement(String key, float value) {
		return createKeyPairElement(key, String.valueOf(value));
	}
	
	private static Element createKeyPairElement(String key, double value) {
		return createKeyPairElement(key, String.valueOf(value));
	}
	
	protected static void appendElement(Element rootElement, @NonNull IDetectionOptions options) {
		appendElement(rootElement, (HashOptions)options);
		for(String key : options.getSubOptionKeys()){
			Element element = new Element(SUB_OPTION_KEY);
			element.setAttribute(SUB_TYPE_KEY, key);
			try {
				appendElement(element, options.getSubOptions(key));
				rootElement.addContent(element);
			} catch (MissingOptionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
		
	protected static void appendElement(Element rootElement, @NonNull HashOptions options) {
		
		if(!options.getBooleanKeys().isEmpty()) {
			Element boolElement = new Element(BOOLEAN_KEY);
			for(String key : options.getBooleanKeys())
				boolElement.addContent(createKeyPairElement(key, options.getBoolean(key)));
			rootElement.addContent(boolElement);
		}
		
		if(!options.getIntegerKeys().isEmpty()) {
			Element intElement = new Element(INT_KEY);
			for(String key : options.getIntegerKeys())
				intElement.addContent(createKeyPairElement(key, options.getInt(key)));
			rootElement.addContent(intElement);
		}

		if(!options.getFloatKeys().isEmpty()) {
			Element floatElement = new Element(FLOAT_KEY);
			for(String key : options.getFloatKeys())
				floatElement.addContent(createKeyPairElement(key, options.getFloat(key)));
			rootElement.addContent(floatElement);
		}
		
		if(!options.getDoubleKeys().isEmpty()) {
			Element doubleElement = new Element(DOUBLE_KEY);
			for(String key : options.getDoubleKeys())
				doubleElement.addContent(createKeyPairElement(key, options.getDouble(key)));
			rootElement.addContent(doubleElement);
		}
		
		if(!options.getStringKeys().isEmpty()) {
			Element stringElement = new Element(STRING_KEY);
			for(String key : options.getStringKeys())
				stringElement.addContent(createKeyPairElement(key, options.getString(key)));
			rootElement.addContent(stringElement);
		}
	}

}
