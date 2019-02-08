package com.bmskinner.nuclear_morphology.io.xml;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.ChildAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.Consensus;
import com.bmskinner.nuclear_morphology.components.DefaultAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.VirtualCellCollection;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.SegmentedFloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnprofilableObjectException;
import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.components.nuclear.DefaultNuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclei.DefaultConsensusNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusFactory;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.gui.PolygonRoi;
import ij.gui.Roi;

/**
 * Read serialised XML dataset files
 * @author bms41
 * @since 1.14.0
 *
 */
public class DatasetXMLReader extends XMLFileReader<IAnalysisDataset> {

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
			stack(e);
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
