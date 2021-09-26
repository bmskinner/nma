package com.bmskinner.nuclear_morphology.io.xml;

import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;

/**
 * Serialise analysis options to be used for new analyses.
 * @author ben
 * @since 1.14.0
 *
 */
public class OptionsXMLCreator extends XMLCreator<IAnalysisDataset> {
	
	private static final Logger LOGGER = Logger.getLogger(OptionsXMLCreator.class.getName());

	public OptionsXMLCreator(@NonNull IAnalysisDataset dataset) {
		super(dataset);
	}
	
	
	@Override
	public Document create() {
		Element rootElement = new Element(DETECTION_SETTINGS_KEY);
		if(template.hasAnalysisOptions())
			appendElement(template, template.getAnalysisOptions().get(), rootElement);
		
		Element clusters = new Element(CLUSTERS_SECTION_KEY);
		for(IClusterGroup g : template.getClusterGroups()) {
			if(g.getOptions().isPresent()) {
				Element cluster = new Element(CLUSTER_GROUP);
				cluster.setAttribute(CLUSTER_NAME, g.getName());
				appendElement(cluster, g.getOptions().get().duplicate());
				clusters.addContent(cluster);
			}
		}
		rootElement.addContent(clusters);
		return new Document(rootElement);
	}

	private static Document createDocument(@NonNull IAnalysisDataset dataset, @NonNull IAnalysisOptions options) {
		Element rootElement = new Element(DETECTION_SETTINGS_KEY);
		appendElement(dataset, options, rootElement);
		return new Document(rootElement);
	}

	static Document createDocument(@NonNull HashOptions options) {
		Element rootElement = new Element(DETECTION_SETTINGS_KEY);
		appendElement(rootElement, options );
		return new Document(rootElement);
	}
	
	private static void appendElement(@NonNull IAnalysisDataset dataset, @NonNull IAnalysisOptions options, Element rootElement) {
		
		rootElement.addContent(createElement(SOFTWARE_CREATION_VERSION_KEY, dataset.getVersionCreated())); 
		rootElement.addContent(createElement(SOFTWARE_SERIALISE_VERSION_KEY, Version.currentVersion()));
		
		for(String key : options.getDetectionOptionTypes()){
			Element element = new Element(DETECTION_METHOD_KEY);
			if(isUUID(key) || key.startsWith(IAnalysisOptions.SIGNAL_GROUP)){ // signal group without prefix
				element.setAttribute(DETECTED_OBJECT_KEY, CellularComponent.NUCLEAR_SIGNAL);
			} else {
				element.setAttribute(DETECTED_OBJECT_KEY, key);
			}
			
			// add signal group names
			if(element.getAttribute(DETECTED_OBJECT_KEY).getValue().equals(CellularComponent.NUCLEAR_SIGNAL)) {
				UUID signalGroup = UUID.fromString(key.replace(IAnalysisOptions.SIGNAL_GROUP, ""));
				String groupName = dataset.getCollection().getSignalGroup(signalGroup).get().getGroupName();
				
				element.addContent(createElement(ID_KEY, signalGroup.toString()));
				element.addContent(createElement(NAME_KEY, groupName));
			}
			
			appendElement(element, options.getDetectionOptions(key).get());
			rootElement.addContent(element);
		}
		
		rootElement.addContent(createElement(NUCLEUS_TYPE_KEY, options.getNucleusType().name()));
		rootElement.addContent(createElement(PROFILE_WINDOW_KEY, String.valueOf(options.getProfileWindowProportion())));
		rootElement.addContent(createElement(RULE_APPLICATION_KEY, options.getRuleApplicationType().name()));
	}

		
}
