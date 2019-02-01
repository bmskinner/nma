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
		ICellCollection c = readCollection(e.getChild(XMLCreator.CELL_COLLECTION_KEY), type, name, id);
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
	
	private ICellCollection readCollection(Element e, NucleusType type, String name, UUID id) throws ComponentCreationException, XMLReadingException {

		String outFolder = e.getChildText(XMLCreator.OUTPUT_FOLDER_KEY);

		ICellCollection collection = new DefaultCellCollection(null,outFolder, name, type, id);
		
		Element cellsElement = e.getChild(XMLCreator.CELLS_SECTION_KEY);
		for(Element cell : cellsElement.getChildren(XMLCreator.CELL_KEY)) {
			XMLReader<ICell> cellReader = new ICellXMLReader(cell, fact, windowProportion);
			collection.add(cellReader.read());
//			collection.add(readCell(cell));
		}
		
		try {
			collection.createProfileCollection();
		} catch (ProfileException e1) {
			stack(e1);
		}
		
		Element tags = e.getChild(XMLCreator.BORDER_TAGS_KEY);
		for(Element tag : tags.getChildren()) {			
			Tag t = readTag(tag);
			int index = readInt(tag, XMLCreator.INDEX_KEY);
			collection.getProfileCollection().addIndex(t, index);
		}
		
		// Add stats
		Element segs = e.getChild(XMLCreator.BORDER_SEGS_KEY);
		readCollectionSegments(segs, collection);
		
		// Add signals
		Element signals = e.getChild(XMLCreator.SIGNAL_GROUPS_SECTION_KEY);
		readSignalGroups(signals, collection);		
		
		// Add consensus if present
		try {
			if(e.getChild(XMLCreator.CONSENSUS_KEY)!=null){
				Consensus<Nucleus> consensus = readConsensus(e.getChild(XMLCreator.CONSENSUS_KEY), type);
				collection.setConsensus(consensus);
			}
		} catch (UnprofilableObjectException e1) {
			stack(e1);
		}

		return collection;
	}
		
	private void readSignalGroups(Element e, ICellCollection collection) {
		if(e==null)
			return;
		
		for(Element groupElement : e.getChildren(XMLCreator.SIGNAL_GROUP_KEY)) {
			fine("Adding signal group to "+collection.getName());
			String name = groupElement.getChildText(XMLCreator.NAME_KEY);
			UUID id = readUUID(groupElement);
			ISignalGroup sg = new SignalGroup(name);
			Element colourElement = groupElement.getChild(XMLCreator.COLOUR_KEY);
			if(colourElement!=null)
				sg.setGroupColour(Color.decode(colourElement.getText()));
			fine("Adding signal group "+sg.toString());
			collection.addSignalGroup(id, sg);
		}
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
	
	private void readCollectionSegments(Element segs, ICellCollection collection) {
		int profileLength = collection.getProfileCollection().length();
		List<IBorderSegment> newSegs = new ArrayList<>();
		int prevStart = -1;
		UUID prevId = null;
		for(Element seg : segs.getChildren()) {			
			UUID id = readUUID(seg);
			int startIndex = readInt(seg, XMLCreator.INDEX_KEY);
			if(prevId!=null) {
				IBorderSegment newSeg = IBorderSegment.newSegment(prevStart, startIndex, profileLength, prevId);
				newSegs.add(newSeg);
			}
			prevStart = startIndex;
			prevId = id;
		}
		if(prevId!=null) {
			IBorderSegment lastSeg = IBorderSegment.newSegment(prevStart, newSegs.get(0).getStartIndex(), profileLength, prevId);
			newSegs.add(lastSeg);
		}
		
		collection.getProfileCollection().addSegments(newSegs);

	}
	
//	private ICell readCell(Element e) throws ComponentCreationException {
//		UUID id = UUID.fromString(e.getChildText(XMLCreator.ID_KEY));
//		ICell cell = new DefaultCell(id);
//		
//		for(Element n : e.getChildren((XMLCreator.NUCLEUS_KEY)))
//			cell.addNucleus(readNucleus(n));
//		return cell;
//	}
	
	private Consensus<Nucleus> readConsensus(Element e, NucleusType type) throws UnprofilableObjectException, ComponentCreationException, XMLReadingException {
		XMLReader<Nucleus> nuclReader = new NucleusXMLReader(e.getChild(XMLCreator.NUCLEUS_KEY), fact, windowProportion);
		Nucleus template = nuclReader.read();
		return new DefaultConsensusNucleus(template, type);
	}
	
	
	
	
	
	

}
