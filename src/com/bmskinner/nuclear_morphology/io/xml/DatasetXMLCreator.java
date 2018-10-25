package com.bmskinner.nuclear_morphology.io.xml;

import java.awt.Color;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;
import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Serialise a dataset to XML
 * @author ben
 * @since 1.14.0
 *
 */
public class DatasetXMLCreator extends XMLCreator<IAnalysisDataset> implements Loggable {


	public DatasetXMLCreator(@NonNull final IAnalysisDataset dataset) {
		super(dataset);
	}
	
	
	@Override
	public Document create() {
		Element rootElement = new Element(ANALYSIS_DATASET_KEY);
		
		rootElement.addContent(createElement(SOFTWARE_CREATION_VERSION_KEY, template.getVersion().toString()));
		rootElement.addContent(createElement(SOFTWARE_SERIALISE_VERSION_KEY, Version.currentVersion().toString()));
		rootElement.addContent(createElement(DATASET_NAME_KEY, template.getName()));
		rootElement.addContent(createElement(DATASET_ID_KEY, template.getId().toString()));
		rootElement.addContent(createElement(DATASET_ROOT_KEY, String.valueOf(template.isRoot())));
		if(template.hasDatasetColour())
			rootElement.addContent(createElement(DATASET_COLOUR_KEY, toHex(template.getDatasetColour().get())));
		
		if(template.hasMergeSources())
			rootElement.addContent(createMergeSources());
		
		if(template.hasChildren())
			rootElement.addContent(createChildDatasets());
		
		if(template.hasClusters())
			rootElement.addContent(createClusterGroups());
		
		rootElement.addContent(create(template.getCollection()));
		rootElement.addContent(create(template.getAnalysisOptions().get()));
		
		return new Document(rootElement);
	}
	
	private String toHex(Color c) {
		return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());  
	}
	
	private Element createClusterGroups() {
		Element clusters = new Element(CLUSTERS);
		for(IClusterGroup g : template.getClusterGroups()) {
			if(g.getOptions().isPresent()) {
				Element cluster = new Element(CLUSTER_GROUP);
				cluster.addContent(createElement(CLUSTER_NAME, g.getName()));
				appendElement(cluster, g.getOptions().get().duplicate());
				if(g.hasTree())
					cluster.addContent(createElement(CLUSTER_TREE_KEY, g.getTree()));
				
				Element datasets = new Element(DATASET_IDS_KEY);
				for(UUID id : g.getUUIDs())
					datasets.addContent(createElement(ID_KEY, id.toString()));
				cluster.addContent(datasets);
				
				clusters.addContent(cluster);
			}
		}
		return clusters;
	}
	
 	private Element createMergeSources() {
		Element e = new Element(MERGE_SOURCES_SECTION_KEY);
		for(IAnalysisDataset src : template.getMergeSources())
			addMergeSource(e, src);
		return e;
	}
	
	private void addMergeSource(Element element, IAnalysisDataset mge) {
		Element e = new Element(MERGE_SOURCE_KEY);
		e.addContent(createElement(DATASET_NAME_KEY, mge.getName()));
		e.addContent(createElement(DATASET_ID_KEY, mge.getId().toString()));
		
		Element cells = new Element(CELL_IDS_KEY);
		for(ICell cell : mge.getCollection())
			cells.addContent(createElement(ID_KEY, cell.getId().toString()));
		e.addContent(cells);
		e.addContent(create(mge.getAnalysisOptions().get()));
		
		for(IAnalysisDataset subMge : mge.getMergeSources())
			addMergeSource(e, subMge);
		
		element.addContent(e);
	}
	
	private Element createChildDatasets() {
		Element e = new Element(CHILD_DATASETS_SECTION_KEY);
		for(IAnalysisDataset src : template.getChildDatasets())
			addChildDataset(e, src);
		return e;
	}
	
	private void addChildDataset(Element element, IAnalysisDataset child) {
		Element e = new Element(CHILD_DATASET_KEY);
		e.addContent(createElement(DATASET_NAME_KEY, child.getName()));
		e.addContent(createElement(DATASET_ID_KEY, child.getId().toString()));
		
		Element cells = new Element(CELL_IDS_KEY);
		for(ICell cell : child.getCollection())
			cells.addContent(createElement(ID_KEY, cell.getId().toString()));
		e.addContent(cells);
		
		for(IAnalysisDataset subChild : child.getChildDatasets())
			addChildDataset(e, subChild);

		element.addContent(e);
	}
	

}
