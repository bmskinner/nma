package com.bmskinner.nuclear_morphology.io.xml;

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
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusFactory;
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
	public IAnalysisDataset read() {
		
		try {
			Document document = readDocument();	
			
			Element rootElement = document.getRootElement();
			Element analysisOptions = rootElement.getChild(XMLCreator.ANALYSIS_OPTIONS_KEY);
			NucleusType type = NucleusType.valueOf(analysisOptions.getChildText(XMLCreator.NUCLEUS_TYPE_KEY));
			fact = new NucleusFactory(type);
			windowProportion = Double.valueOf(analysisOptions.getChildText(XMLCreator.PROFILE_WINDOW_KEY));
			
			return readDataset(document.getRootElement(), type );
			
		} catch (JDOMException | IOException | ComponentCreationException e) {
			stack(e);
		}
		
		return null;
	}
		
	private IAnalysisDataset readDataset(Element e, NucleusType type) throws ComponentCreationException {
		String name = e.getChildText(XMLCreator.DATASET_NAME_KEY);
		UUID id = UUID.fromString(e.getChildText(XMLCreator.DATASET_ID_KEY));
		ICellCollection c = readCollection(e.getChild(XMLCreator.CELL_COLLECTION_KEY), type, name, id);
		IAnalysisDataset d = new DefaultAnalysisDataset(c, file);
		d.setName(name);
		return d;
	}
	
	private ICellCollection readCollection(Element e, NucleusType type, String name, UUID id) throws ComponentCreationException {

		String outFolder = e.getChildText(XMLCreator.OUTPUT_FOLDER_KEY);

		ICellCollection collection = new DefaultCellCollection(null,outFolder, name, type, id);
		
		for(Element cell : e.getChildren(XMLCreator.CELL_KEY))
			collection.add(readCell(cell));

		
		collection.updateVerticalNuclei();
		return collection;
	}
	
	private ICell readCell(Element e) throws ComponentCreationException {
		UUID id = UUID.fromString(e.getChildText(XMLCreator.ID_KEY));
		ICell cell = new DefaultCell(id);
		
		for(Element n : e.getChildren((XMLCreator.NUCLEUS_KEY)))
			cell.addNucleus(readNucleus(n));
		return cell;
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
			
			IProfile profile = n.getProfile(ProfileType.ANGLE);
			ISegmentedProfile segmented = new SegmentedFloatProfile(profile, newSegs);
			n.setProfile(ProfileType.ANGLE, segmented);
//			for(ProfileType type : ProfileType.exportValues()) {
//				IProfile profile = n.getProfile(type);
//				ISegmentedProfile segmented = new SegmentedFloatProfile(profile, newSegs);
//				n.setProfile(type, segmented);
//			}
		} catch(Exception e1) {
			stack(e1);
		}
	}

}
