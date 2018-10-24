package com.bmskinner.nuclear_morphology.io.xml;

import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Serialise analysis options to be used for new analyses.
 * @author ben
 * @since 1.14.0
 *
 */
public class OptionsXMLCreator extends XMLCreator<IAnalysisDataset> implements Loggable {
	
	public static final String DETECTION_LBL    = "Detection_settings";
	public static final String DETECTION_METHOD = "Detection_method";
	public static final String DETECTED_OBJECT  = "Detected_object";
	public static final String NUCLEUS_TYPE     = "Nucleus_type";
	public static final String PROFILE_WINDOW   = "Profile_window";
	public static final String SOFTWARE_VERSION = "SoftwareVersion";
	
	public static final String BOOLEAN_KEY = "Booleans";
	public static final String FLOAT_KEY   = "Floats";
	public static final String DOUBLE_KEY  = "Doubles";
	public static final String INT_KEY     = "Ints";
	public static final String STRING_KEY  = "Strings";
	public static final String PAIR_KEY    = "Option";
	public static final String KEY_KEY     = "Key";
	public static final String VALUE_KEY   = "Value";
	
	public static final String SUB_OPTION_KEY = "Sub_option";
	public static final String SUB_TYPE_KEY   = "Sub_option_type";
	
	public static final String SIGNAL_DETECTION_MODE_KEY   = "Detection_mode";
	
	public static final String UUID_PREFIX = "UUID_";
	
	public static final String SIGNAL_GROUP_PREFIX = "SignalGroup_";
	public static final String SIGNAL_ID         = "SignalGroupId";
	public static final String SIGNAL_NAME       = "SignalGroupName";
	public static final String CLUSTERS = "Clusters";
	public static final String CLUSTER_GROUP = "ClusterGroup";
	public static final String CLUSTER_NAME  = "ClusterGroupName";
	
	public OptionsXMLCreator(@NonNull IAnalysisDataset dataset) {
		super(dataset);
	}
	
	
	@Override
	public Document create() {
		Element rootElement = new Element(DETECTION_LBL);
		if(template.hasAnalysisOptions())
			appendElement(template, template.getAnalysisOptions().get(), rootElement);
		
		Element clusters = new Element(CLUSTERS);
		for(IClusterGroup g : template.getClusterGroups()) {
			if(g.getOptions().isPresent()) {
				Element cluster = new Element(CLUSTER_GROUP);
				cluster.setAttribute(CLUSTER_NAME, g.getName());
				appendElement(g.getOptions().get().duplicate(), cluster);
				clusters.addContent(cluster);
			}
		}
		rootElement.addContent(clusters);
		return new Document(rootElement);
	}

	private static Document createDocument(@NonNull IAnalysisDataset dataset, @NonNull IAnalysisOptions options) {
		Element rootElement = new Element(DETECTION_LBL);
		appendElement(dataset, options, rootElement);
		return new Document(rootElement);
	}

	static Document createDocument(@NonNull HashOptions options) {
		Element rootElement = new Element(DETECTION_LBL);
		appendElement(options, rootElement);
		return new Document(rootElement);
	}
	
	private static void appendElement(@NonNull IAnalysisDataset dataset, @NonNull IAnalysisOptions options, Element rootElement) {
		
		Element versionElement = new Element(SOFTWARE_VERSION);
		versionElement.setText(dataset.getVersion().toString());
		rootElement.addContent(versionElement);
		
		for(String key : options.getDetectionOptionTypes()){
			Element element = new Element(DETECTION_METHOD);
			if(isUUID(key) || key.startsWith(IAnalysisOptions.SIGNAL_GROUP)){ // signal group without prefix
				element.setAttribute(DETECTED_OBJECT, IAnalysisOptions.NUCLEAR_SIGNAL);
			} else {
				element.setAttribute(DETECTED_OBJECT, key);
			}
			
			// add signal group names
			if(element.getAttribute(DETECTED_OBJECT).getValue().equals(IAnalysisOptions.NUCLEAR_SIGNAL)) {
				UUID signalGroup = UUID.fromString(key.replaceAll(IAnalysisOptions.SIGNAL_GROUP, ""));
				String groupName = dataset.getCollection().getSignalGroup(signalGroup).get().getGroupName();
				
				Element signalId = new Element(OptionsXMLCreator.SIGNAL_ID);
				signalId.setText(signalGroup.toString());
				element.addContent(signalId);
				
				Element signalName = new Element(OptionsXMLCreator.SIGNAL_NAME);
				signalName.setText(groupName);
				element.addContent(signalName);
			}
			
			appendElement(options.getDetectionOptions(key).get(), element);
			rootElement.addContent(element);
		}
		Element ntElement = new Element(NUCLEUS_TYPE);
		ntElement.setText(options.getNucleusType().name());
		rootElement.addContent(ntElement);
		
		Element anElement = new Element(PROFILE_WINDOW);
		anElement.setText(String.valueOf(options.getProfileWindowProportion()));
		rootElement.addContent(anElement);
	}

	private static void appendElement(@NonNull IDetectionOptions options, Element rootElement) {
		appendElement( (HashOptions)options, rootElement);
		for(String key : options.getSubOptionKeys()){
			Element element = new Element(SUB_OPTION_KEY);
			element.setAttribute(SUB_TYPE_KEY, key);
			try {
				appendElement(options.getSubOptions(key), element);
				rootElement.addContent(element);
			} catch (MissingOptionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
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
		
	private static void appendElement(@NonNull HashOptions options, Element rootElement) {
		
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
