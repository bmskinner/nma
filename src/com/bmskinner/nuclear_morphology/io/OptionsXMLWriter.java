package com.bmskinner.nuclear_morphology.io;

import java.io.File;
import java.io.IOException;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Write the complete set of options for all analyses in a dataset
 * to an XML file. The file will be compatible with the options reader.
 * @author ben
 * @since 1.14.0
 *
 */
public class OptionsXMLWriter extends XMLWriter implements Loggable {
	
	public static final String DETECTION_LBL    = "Detection_settings";
	public static final String DETECTION_METHOD = "Detection_method";
	public static final String DETECTED_OBJECT  = "Detected_object";
	public static final String NUCLEUS_TYPE     = "Nucleus_type";
	public static final String PROFILE_WINDOW   = "Profile_window";
	
	public static final String BOOLEAN_KEY = "Booleans";
	public static final String FLOAT_KEY   = "Floats";
	public static final String DOUBLE_KEY  = "Doubles";
	public static final String INT_KEY     = "Ints";
	public static final String STRING_KEY  = "Strings";
	
	public static final String SUB_OPTION_KEY = "Sub_option";
	public static final String SUB_TYPE_KEY   = "Sub_option_type";
	
	public static final String SIGNAL_DETECTION_MODE_KEY   = "Detection_mode";
	
	public static final String SPACE_REPLACEMENT = "_sp_";
	
	public void write(@NonNull IAnalysisDataset dataset, @NonNull File outFile) {
		Document doc = OptionsXMLWriter.createDocument(dataset);
		try {
			writeXML(doc, outFile);
		} catch (IOException e) {
			 warn("Cannot export options file");
		}
	}
	
	public static Document createDocument(@NonNull IAnalysisDataset dataset) {
		Element rootElement = new Element(DETECTION_LBL);
		if(dataset.hasAnalysisOptions())
			appendElement(dataset.getAnalysisOptions().get(), rootElement);
		
		for(IClusterGroup g : dataset.getClusterGroups()) {
			if(g.getOptions().isPresent()) {
				Element cluster = new Element("Cluster");
				cluster.setAttribute("Cluster_group", g.getName());
				appendElement(g.getOptions().get(), cluster);
			}
		}
		return new Document(rootElement);
	}

	public static Document createDocument(@NonNull IAnalysisOptions options) {
		Element rootElement = new Element(DETECTION_LBL);
		appendElement(options, rootElement);
		return new Document(rootElement);
	}

	public static Document createDocument(@NonNull HashOptions options) {
		Element rootElement = new Element(DETECTION_LBL);
		appendElement(options, rootElement);
		return new Document(rootElement);
	}
	
	private static void appendElement(@NonNull IAnalysisOptions options, Element rootElement) {
		for(String key : options.getDetectionOptionTypes()){
			Element element = new Element(DETECTION_METHOD);
			element.setAttribute(DETECTED_OBJECT, key);
			appendElement(options.getDetectionOptions(key).get(), element);
			rootElement.addContent(element);
		}
		Element ntElement = new Element(NUCLEUS_TYPE);
		ntElement.setText(options.getNucleusType().name());
		rootElement.addContent(ntElement);
		
		Element anElement = new Element(PROFILE_WINDOW);
		anElement.setText(String.valueOf(options.getProfileWindowProportion()));
		rootElement.addContent(anElement);
		
//		for(UUID signalGroupId : options.getNuclearSignalGroups()) {
//			Element signalElement = new Element("Signal_group");
//			signalElement.setAttribute("ID", signalGroupId.toString());
//			signalElement.addContent(createElement(options.getNuclearSignalOptions(signalGroupId)));
//			rootElement.addContent(signalElement);
//		}
	}

	private static void appendElement(@NonNull IDetectionOptions options, Element rootElement) {
		appendElement( (HashOptions)options, rootElement);
//		Element detElement = new Element("Detection_mode");
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
//		rootElement.addContent(detElement);
	}
	
	private static void appendElement(@NonNull INuclearSignalOptions options, Element rootElement) {
		appendElement( (IDetectionOptions)options, rootElement);
		Element detElement = new Element(SIGNAL_DETECTION_MODE_KEY);
		detElement.setText(options.getDetectionMode().toString());
		rootElement.addContent(detElement);
	}
	
	private static void appendElement(@NonNull HashOptions options, Element rootElement) {
		
		if(!options.getBooleanKeys().isEmpty()) {
			Element boolElement = new Element(BOOLEAN_KEY);
			for(String key : options.getBooleanKeys()){
				Element keyElement = new Element(key.replaceAll(" ", SPACE_REPLACEMENT));
				String s = String.valueOf(options.getBoolean(key)).replaceAll(" ", SPACE_REPLACEMENT);
				keyElement.setText(s);
				boolElement.addContent(keyElement);
			}
			rootElement.addContent(boolElement);
		}
		
		if(!options.getIntegerKeys().isEmpty()) {
			Element intElement = new Element(INT_KEY);
			for(String key : options.getIntegerKeys()){
				Element keyElement = new Element(key.replaceAll(" ", SPACE_REPLACEMENT));
				keyElement.setText(String.valueOf(options.getInt(key)));
				intElement.addContent(keyElement);
			}
			rootElement.addContent(intElement);
		}

		if(!options.getFloatKeys().isEmpty()) {
			Element floatElement = new Element(FLOAT_KEY);
			for(String key : options.getFloatKeys()){
				Element keyElement = new Element(key.replaceAll(" ", SPACE_REPLACEMENT));
				keyElement.setText(String.valueOf(options.getFloat(key)));
				floatElement.addContent(keyElement);
			}
			rootElement.addContent(floatElement);
		}
		
		if(!options.getDoubleKeys().isEmpty()) {
			Element doubleElement = new Element(DOUBLE_KEY);
			for(String key : options.getDoubleKeys()){
				Element keyElement = new Element(key.replaceAll(" ", SPACE_REPLACEMENT));
				keyElement.setText(String.valueOf(options.getDouble(key)));
				doubleElement.addContent(keyElement);
			}
			rootElement.addContent(doubleElement);
		}
		
		if(!options.getStringKeys().isEmpty()) {
			Element stringElement = new Element(STRING_KEY);
			for(String key : options.getStringKeys()){
				Element keyElement = new Element(key.replaceAll(" ", SPACE_REPLACEMENT));
				keyElement.setText(options.getString(key));
				stringElement.addContent(keyElement);
			}
			rootElement.addContent(stringElement);
		}
	}

}
