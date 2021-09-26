package com.bmskinner.nuclear_morphology.io.xml;

import java.awt.Color;
import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.datasets.ChildAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.DefaultAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.datasets.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.components.Version;
import com.bmskinner.nuclear_morphology.components.cells.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusFactory;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusType;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Read serialised XML dataset files
 * @author bms41
 * @since 1.14.0
 *
 */
public class DatasetXMLReader extends XMLFileReader<IAnalysisDataset> {
	
	private static final Logger LOGGER = Logger.getLogger(DatasetXMLReader.class.getName());

	private NucleusFactory fact = null;
	private double windowProportion = 0.05;
	
	/**
	 * Create with a file to be read
	 * @param f
	 * @throws XMLReadingException 
	 */
	public DatasetXMLReader(@NonNull final File f) throws XMLReadingException {
		super(f);
	}

	
	@Override
	public IAnalysisDataset read() throws XMLReadingException {
		
		try {
			Element analysisOptions = rootElement.getChild(XMLCreator.ANALYSIS_OPTIONS_KEY);
			NucleusType type = NucleusType.valueOf(analysisOptions.getChildText(XMLCreator.NUCLEUS_TYPE_KEY));
			fact = new NucleusFactory(type);
			windowProportion = readDouble(analysisOptions, XMLCreator.PROFILE_WINDOW_KEY);
			
			return readDataset(rootElement, type );
			
		} catch (ComponentCreationException e) {
			throw new XMLReadingException("Could not create component from XML", e);
		} catch(XMLReadingException e) { 
			// Rethrow quietly
			throw e;
		} catch(Exception e) {
			// log anything else
			LOGGER.log(Loggable.STACK, e.getMessage(), e);
			throw new XMLReadingException("Error reading XML: invalid XML format", e);
		}
	}
	
	private IAnalysisDataset readDataset(Element e, NucleusType type) throws ComponentCreationException, XMLReadingException {
		
		Version created = Version.parseString(e.getChildText(XMLCreator.SOFTWARE_CREATION_VERSION_KEY));
		Version saved   = Version.parseString(e.getChildText(XMLCreator.SOFTWARE_SERIALISE_VERSION_KEY));

		// Add to this to handle version changes
		if(Version.v_1_14_0.equals(created))
			return readDataset_1_14_0(e, type);
		
		return readDataset_1_14_0(e, type);
	}
		
	private IAnalysisDataset readDataset_1_14_0(Element e, NucleusType type) throws ComponentCreationException, XMLReadingException {
		String name = e.getChildText(XMLCreator.DATASET_NAME_KEY);
		UUID id = UUID.fromString(e.getChildText(XMLCreator.DATASET_ID_KEY));
		XMLReader<ICellCollection> collectionReader = new ICellCollectionXMLReader(e.getChild(XMLCreator.CELL_COLLECTION_KEY), type, windowProportion, name, id);
		ICellCollection c = collectionReader.read();
		IAnalysisDataset d = new DefaultAnalysisDataset(c, file);
		
		Element colour = e.getChild(XMLCreator.COLOUR_KEY);
		if(colour!=null)
			d.setDatasetColour(Color.decode(colour.getText()));
		d.setName(name);
		
		IAnalysisOptions options = readOptions(e.getChild(XMLCreator.ANALYSIS_OPTIONS_KEY));
		d.setAnalysisOptions(options);
		
		readChildDatasets(e.getChild(XMLCreator.CHILD_DATASETS_SECTION_KEY), d);
		
		return d;
	}
	
	private IAnalysisOptions readOptions(Element e) {
		IAnalysisOptions op = OptionsFactory.makeAnalysisOptions();
		NucleusType type = NucleusType.valueOf(e.getChild(XMLCreator.NUCLEUS_TYPE_KEY).getText());
		op.setNucleusType(type);
		double windowSize = readDouble(e, XMLCreator.PROFILE_WINDOW_KEY);
		op.setAngleWindowProportion(windowSize);

		// should be single elements with options class
		for(Element component : e.getChildren(XMLCreator.DETECTION_METHOD_KEY))
			addComponent(component, op);

		return op;
	}
	


	private void readChildDatasets(Element children, IAnalysisDataset parent) {
		if(children==null)
			return;
		if(children.getChild(XMLCreator.CHILD_DATASET_KEY)==null)
			return;

		for(Element childElement : children.getChildren(XMLCreator.CHILD_DATASET_KEY)) {			
			String name = childElement.getChildText(XMLCreator.DATASET_NAME_KEY);

			UUID id = UUID.fromString(childElement.getChildText(XMLCreator.DATASET_ID_KEY));
			ICellCollection childCollection = new VirtualCellCollection(parent, name, id);
			
			Element childCells = childElement.getChild(XMLCreator.CELLS_SECTION_KEY);

			for(Element cellElement : childCells.getChildren()) {
				UUID cellId = UUID.fromString(cellElement.getText());
				childCollection.add(parent.getCollection().getCell(cellId));
			}
			IAnalysisDataset child = new ChildAnalysisDataset(parent, childCollection);
			readClusterGroups(childElement.getChild(XMLCreator.CLUSTERS_SECTION_KEY), child);
			parent.addChildDataset(child);
			if(childElement.getChild(XMLCreator.CHILD_DATASETS_SECTION_KEY)!=null)
				readChildDatasets(childElement.getChild(XMLCreator.CHILD_DATASETS_SECTION_KEY), child);
		}
	}
	
	private void readClusterGroups(Element e, IAnalysisDataset dataset) {
		//TODO
	}

}
