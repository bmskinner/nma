package com.bmskinner.nuclear_morphology.io.xml;

import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
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
		
		rootElement.addContent(createElement(SOFTWARE_CREATION_VERSION_KEY, template.getVersion()));
		rootElement.addContent(createElement(SOFTWARE_SERIALISE_VERSION_KEY, Version.currentVersion()));
		rootElement.addContent(createElement(DATASET_NAME_KEY, template.getName()));
		rootElement.addContent(createElement(DATASET_ID_KEY, template.getId()));
		rootElement.addContent(createElement(DATASET_ROOT_KEY, String.valueOf(template.isRoot())));
		if(template.hasDatasetColour())
			rootElement.addContent(createElement(COLOUR_KEY, toHex(template.getDatasetColour().get())));
		
		if(template.hasMergeSources())
			rootElement.addContent(createMergeSources());
		
		if(template.hasChildren())
			rootElement.addContent(createChildDatasets());
		
		if(template.hasClusters())
			rootElement.addContent(createClusterGroups(template));
		
		rootElement.addContent(create(template.getCollection()));
		rootElement.addContent(create(template.getAnalysisOptions().get()));
		
		return new Document(rootElement);
	}
		
	private Element createClusterGroups(IAnalysisDataset template) {
		Element clusters = new Element(CLUSTERS_SECTION_KEY);
		for(IClusterGroup g : template.getClusterGroups()) {
			if(g.getOptions().isPresent()) {
				Element cluster = new Element(CLUSTER_GROUP);
				cluster.addContent(createElement(CLUSTER_NAME, g.getName()));
				appendElement(cluster, g.getOptions().get().duplicate());
				if(g.hasTree())
					cluster.addContent(createElement(CLUSTER_TREE_KEY, g.getTree()));
				
				Element datasets = new Element(DATASET_IDS_KEY);
				for(UUID id : g.getUUIDs())
					datasets.addContent(createElement(ID_KEY, id));
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
		e.addContent(createElement(DATASET_ID_KEY, mge.getId()));
		
		Element cells = new Element(CELL_IDS_KEY);
		for(ICell cell : mge.getCollection())
			cells.addContent(createElement(ID_KEY, cell.getId()));
		e.addContent(cells);
		e.addContent(create(mge.getAnalysisOptions().get()));
		
		Element sources = new Element(MERGE_SOURCES_SECTION_KEY);
		
		for(IAnalysisDataset subMge : mge.getMergeSources())
			addMergeSource(sources, subMge);
		
		if(sources.getContentSize()>0)
			e.addContent(sources);
		
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
		e.addContent(createElement(DATASET_ID_KEY, child.getId()));
		
		if(child.hasDatasetColour())
			e.addContent(createElement(COLOUR_KEY, toHex(child.getDatasetColour().get())));
						
		if(child.hasClusters())
			e.addContent(createClusterGroups(child));
		
		e.addContent(create(child.getCollection()));
		
		Element children = new Element(CHILD_DATASETS_SECTION_KEY);
		for(IAnalysisDataset subChild : child.getChildDatasets())
			addChildDataset(children, subChild);
		if(children.getContentSize()>0)
			e.addContent(children);

		element.addContent(e);
	}
	

}
