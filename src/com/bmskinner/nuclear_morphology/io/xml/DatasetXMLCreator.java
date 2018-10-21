package com.bmskinner.nuclear_morphology.io.xml;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Serialise a dataset to XML
 * @author ben
 * @since 1.14.0
 *
 */
public class DatasetXMLCreator extends XMLCreator<IAnalysisDataset> implements Loggable {
	
	public static final String ROOT_LBL                       = "AnalysisDataset";
	public static final String SOFTWARE_CREATION_VERSION_KEY  = "VersionCreated";
	public static final String SOFTWARE_SERIALISE_VERSION_KEY = "VersionSerialised";
	public static final String DATASET_NAME_KEY               = "DatasetName";
	public static final String DATASET_ID_KEY                 = "DatasetId";
	public static final String DATASET_COLOUR_KEY             = "DatasetColour";
	public static final String DATASET_ROOT_KEY               = "DatasetIsRoot";
	
	public static final String MERGE_SOURCES_SECTION_KEY      = "MergeSources";
	public static final String MERGE_SOURCE_KEY               = "MergeSource";

	
	public static final String CHILD_DATASETS_SECTION_KEY     = "ChildDatasets";
	public static final String CHILD_DATASET_KEY              = "ChildDataset";

	public static final String ANALYSIS_OPTIONS_KEY           = "AnalysisOptions";

	public DatasetXMLCreator(@NonNull final IAnalysisDataset dataset) {
		super(dataset);
	}
	
	
	@Override
	public Document create() {
		Element rootElement = new Element(ROOT_LBL);
		
		rootElement.addContent(createElement(SOFTWARE_CREATION_VERSION_KEY, template.getVersion().toString()));
		rootElement.addContent(createElement(SOFTWARE_SERIALISE_VERSION_KEY, Version.currentVersion().toString()));
		rootElement.addContent(createElement(DATASET_NAME_KEY, template.getName()));
		rootElement.addContent(createElement(DATASET_ID_KEY, template.getId().toString()));
		rootElement.addContent(createElement(DATASET_ROOT_KEY, String.valueOf(template.isRoot())));
		if(template.hasDatasetColour())
			rootElement.addContent(createElement(DATASET_COLOUR_KEY, template.getDatasetColour().get().toString()));
		
		if(template.hasMergeSources())
			rootElement.addContent(createMergeSources());
		
		if(template.hasChildren())
			rootElement.addContent(createChildDatasets());
		
		rootElement.addContent(create(template.getCollection()));
		rootElement.addContent(createAnalysisOptions());
		
		
		return new Document(rootElement);
	}
	
	private Element createAnalysisOptions() {
		Element e = new Element(ANALYSIS_OPTIONS_KEY);
		
		return e;
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
		
		element.addContent(e);
	}
	
	private Element createChildDatasets() {
		Element e = new Element(CHILD_DATASETS_SECTION_KEY);
		for(IAnalysisDataset src : template.getMergeSources())
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
		
		element.addContent(e);
	}
	

}
