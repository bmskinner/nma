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
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.DefaultAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.DefaultCellCollection;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.SegmentedFloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnprofilableObjectException;
import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
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
public class DatasetXMLReader extends XMLReader<IAnalysisDataset> {

	private NucleusFactory fact = null;
	private double windowProportion = 0.05;
	
	/**
	 * Create with a file to be read
	 * @param f
	 */
	public DatasetXMLReader(@NonNull final File f) {
		super(f);
	}
	
	@Override
	public IAnalysisDataset read() throws XMLReadingException {
		
		try {
			Document document = readDocument();	
			
			Element rootElement = document.getRootElement();
			Element analysisOptions = rootElement.getChild(XMLCreator.ANALYSIS_OPTIONS_KEY);
			NucleusType type = NucleusType.valueOf(analysisOptions.getChildText(XMLCreator.NUCLEUS_TYPE_KEY));
			fact = new NucleusFactory(type);
			windowProportion = Double.valueOf(analysisOptions.getChildText(XMLCreator.PROFILE_WINDOW_KEY));
			
			return readDataset(document.getRootElement(), type );
			
		} catch (ComponentCreationException e) {
			throw new XMLReadingException("Could not create component from XML", e);
		}
	}
	
	private IAnalysisDataset readDataset(Element e, NucleusType type) throws ComponentCreationException {
		
		Version created = Version.parseString(e.getChildText(XMLCreator.SOFTWARE_CREATION_VERSION_KEY));
		Version saved   = Version.parseString(e.getChildText(XMLCreator.SOFTWARE_SERIALISE_VERSION_KEY));

		// Add to this to handle version changes
		if(Version.v_1_14_0.equals(created))
			return readDataset_1_14_0(e, type);
		
		return readDataset_1_14_0(e, type);
	}
		
	private IAnalysisDataset readDataset_1_14_0(Element e, NucleusType type) throws ComponentCreationException {
		String name = e.getChildText(XMLCreator.DATASET_NAME_KEY);
		UUID id = UUID.fromString(e.getChildText(XMLCreator.DATASET_ID_KEY));
		ICellCollection c = readCollection(e.getChild(XMLCreator.CELL_COLLECTION_KEY), type, name, id);
		IAnalysisDataset d = new DefaultAnalysisDataset(c, file);
		
		Element colour = e.getChild(XMLCreator.DATASET_COLOUR_KEY);
		if(colour!=null)
			d.setDatasetColour(Color.decode(colour.getText()));
		d.setName(name);
		
		IAnalysisOptions options = readOptions(e.getChild(XMLCreator.ANALYSIS_OPTIONS_KEY));
		d.setAnalysisOptions(options);
		return d;
	}
	
	private IAnalysisOptions readOptions(Element e) {
		IAnalysisOptions op = OptionsFactory.makeAnalysisOptions();
		NucleusType type = NucleusType.valueOf(e.getChild(XMLCreator.NUCLEUS_TYPE_KEY).getText());
		op.setNucleusType(type);
		double windowSize = Double.parseDouble(e.getChild(XMLCreator.PROFILE_WINDOW_KEY).getText());
		op.setAngleWindowProportion(windowSize);

		// should be single elements with options class
		for(Element component : e.getChildren(XMLCreator.DETECTION_METHOD_KEY))
			addComponent(component, op);

		return op;
	}
	
	private ICellCollection readCollection(Element e, NucleusType type, String name, UUID id) throws ComponentCreationException {

		String outFolder = e.getChildText(XMLCreator.OUTPUT_FOLDER_KEY);

		ICellCollection collection = new DefaultCellCollection(null,outFolder, name, type, id);
		
		for(Element cell : e.getChildren(XMLCreator.CELL_KEY))
			collection.add(readCell(cell));
		
		try {
			collection.createProfileCollection();
		} catch (ProfileException e1) {
			stack(e1);
		}
		
		Element tags = e.getChild(XMLCreator.BORDER_TAGS_KEY);
		for(Element tag : tags.getChildren()) {			
			Tag t = readTag(tag);
			int index = Integer.valueOf(tag.getChildText(XMLCreator.INDEX_KEY));
			collection.getProfileCollection().addIndex(t, index);
		}
		
		// Add stats
		Element segs = e.getChild(XMLCreator.BORDER_SEGS_KEY);
		readCollectionSegments(segs, collection);
		
		// Add consensus
		try {
			Nucleus consensus = readConsensus(e.getChild(XMLCreator.CONSENSUS_KEY), type);
			collection.setConsensus(consensus);
		} catch (UnprofilableObjectException e1) {
			stack(e1);
		}
		
		
		collection.updateVerticalNuclei();
		return collection;
	}
	
	private void readCollectionSegments(Element segs, ICellCollection collection) {
		int profileLength = collection.getProfileCollection().length();
		List<IBorderSegment> newSegs = new ArrayList<>();
		int prevStart = -1;
		UUID prevId = null;
		for(Element seg : segs.getChildren()) {			
			UUID id = readUUID(seg);
			int startIndex = readInt(seg, XMLCreator.INDEX_KEY);
			if(prevStart!=-1) {
				IBorderSegment newSeg = IBorderSegment.newSegment(prevStart, startIndex, profileLength, prevId);
				newSegs.add(newSeg);
			}
			prevStart = startIndex;
			prevId = id;
		}
		IBorderSegment lastSeg = IBorderSegment.newSegment(prevStart, newSegs.get(0).getStartIndex(), profileLength, prevId);
		newSegs.add(lastSeg);
		
		collection.getProfileCollection().addSegments(newSegs);

	}
	
	private ICell readCell(Element e) throws ComponentCreationException {
		UUID id = UUID.fromString(e.getChildText(XMLCreator.ID_KEY));
		ICell cell = new DefaultCell(id);
		
		for(Element n : e.getChildren((XMLCreator.NUCLEUS_KEY)))
			cell.addNucleus(readNucleus(n));
		return cell;
	}
	
	private Nucleus readConsensus(Element e, NucleusType type) throws UnprofilableObjectException, ComponentCreationException {
		Nucleus template = readNucleus(e.getChild(XMLCreator.NUCLEUS_KEY));
		return new DefaultConsensusNucleus(template, type);
	}
	
	private Nucleus readNucleus(Element e) throws ComponentCreationException {
		
		Element border = e.getChild(XMLCreator.BORDER_POINTS_KEY);
		
		Element base = e.getChild(XMLCreator.BASE_KEY);

		// Make the int array border list
		List<Element> points = border.getChildren();
		int[] xpoints = new int[points.size()];
		int[] ypoints = new int[points.size()];
		
		for(int i=0; i<xpoints.length; i++) {
			Element point = points.get(i);
			xpoints[i] = readX(point);
			ypoints[i] = readY(point);	
		}

		Roi roi = new PolygonRoi(xpoints, ypoints, xpoints.length, Roi.POLYGON);
		
		int xbase = readInt(base, XMLCreator.X_BASE_KEY);
        int ybase = readInt(base, XMLCreator.Y_BASE_KEY);
        int[] originalPosition = { xbase, ybase, readInt(base, XMLCreator.W_BASE_KEY), readInt(base, XMLCreator.H_BASE_KEY) };
		
		IPoint com = readPoint(e.getChild(XMLCreator.COM_KEY));
		
		File imageFile = new File(e.getChildText(XMLCreator.SOURCE_FILE_KEY));
		int channel = Integer.valueOf((e.getChildText(XMLCreator.SOURCE_CHANNEL_KEY)));
		
		UUID id = UUID.fromString(e.getChildText(XMLCreator.ID_KEY));
		
		int nucleusNumber = readInt(e, XMLCreator.NUCLEUS_NUMBER_KEY);
		
		Nucleus n = fact.buildInstance(roi, imageFile, channel, originalPosition, com, id, nucleusNumber);
		// Move the nucleus xbase and ybase to 0,0 coordinates for charting
        IPoint offsetCoM = IPoint.makeNew(com.getX() - xbase, com.getY() - ybase);
        n.moveCentreOfMass(offsetCoM);
		
		
		// Add stats
		Element stats = e.getChild(XMLCreator.STATS_KEY);
		for(Element stat : stats.getChildren(XMLCreator.STAT_KEY)) {
			PlottableStatistic s = readStat(stat);
			double d = Double.valueOf(stat.getChildText(XMLCreator.VALUE_KEY));
			n.setStatistic(s, d);
		}
		
		n.setScale(Double.valueOf(e.getChildText(XMLCreator.SOURCE_SCALE_KEY)));
		
		n.initialise(windowProportion);
		
		int actualLength = n.getBorderLength();
		int expLength    = Integer.valueOf(e.getChildText(XMLCreator.BORDER_LENGTH_KEY));
		if(actualLength!=expLength)
			warn(String.format("Border interpolation to %s does not match saved value %s", actualLength, expLength));
		
		// Apply tags
		readTags(e.getChild(XMLCreator.BORDER_TAGS_KEY), n);
		
		// Apply segments
		readSegments(e.getChild(XMLCreator.BORDER_SEGS_KEY), n);

		n.getVerticallyRotatedNucleus();
		
		return n;
	}
	
	
	private void readTags(Element tags, Nucleus n) {
		for(Element tag : tags.getChildren()) {			
			Tag t = readTag(tag);
			int index = Integer.valueOf(tag.getChildText(XMLCreator.INDEX_KEY));
			n.setBorderTag(t, index);
		}
	}
	
	
	private void readSegments(Element segs, Nucleus n) {
		
		List<IBorderSegment> newSegs = new ArrayList<>();
		int prevStart = -1;
		UUID prevId = null;
		for(Element seg : segs.getChildren()) {			
			UUID id = readUUID(seg);
			int startIndex = Integer.valueOf(seg.getChildText(XMLCreator.INDEX_KEY));
			if(prevStart!=-1) {
				IBorderSegment newSeg = IBorderSegment.newSegment(prevStart, startIndex, n.getBorderLength(), prevId);
				newSegs.add(newSeg);
			}
			prevStart = startIndex;
			prevId = id;
		}
		IBorderSegment lastSeg = IBorderSegment.newSegment(prevStart, newSegs.get(0).getStartIndex(), n.getBorderLength(), prevId);
		newSegs.add(lastSeg);

		try {			
			for(ProfileType type : ProfileType.values()) { 
				IProfile profile = n.getProfile(type);
				ISegmentedProfile segmented = new SegmentedFloatProfile(profile, newSegs);
				n.setProfile(type, segmented);
				
				ISegmentedProfile p = n.getProfile(type, Tag.REFERENCE_POINT); // ensure all profiles are updated to RP
				n.setProfile(type, Tag.REFERENCE_POINT, p);
			}

		} catch(Exception e1) {
			stack(e1);
		}
	}

}
