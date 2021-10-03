package com.bmskinner.nuclear_morphology.io.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.measure.Measurement;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusFactory;
import com.bmskinner.nuclear_morphology.components.profiles.IProfile;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.SegmentedFloatProfile;
import com.bmskinner.nuclear_morphology.components.signals.DefaultNuclearSignal;
import com.bmskinner.nuclear_morphology.components.signals.INuclearSignal;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.gui.PolygonRoi;
import ij.gui.Roi;

/**
 * Reader for nuclei encoded in XML
 * @author bms41
 * @since 1.15.1
 *
 */
public class NucleusXMLReader extends XMLReader<Nucleus>{
	
	private static final Logger LOGGER = Logger.getLogger(NucleusXMLReader.class.getName());
	
	private final NucleusFactory fact;
	private final double windowProportion;

	public NucleusXMLReader(@NonNull final Element e, @NonNull final NucleusFactory factory, final double windowProportion) {
		super(e);
		fact = factory;
		this.windowProportion = windowProportion;
	}

	@Override
	public Nucleus read() throws XMLReadingException {
		try {
			return readNucleus(rootElement);
		} catch (ComponentCreationException e) {
			LOGGER.log(Loggable.STACK, e.getMessage(), e);
			throw new XMLReadingException(e);
		}
	}
	
	private Nucleus readNucleus(Element e) throws ComponentCreationException {

		Roi roi = readRoi(e);

		int[] originalPosition = readOriginalPosition(e);
		
		Element base = e.getChild(XMLCreator.BASE_KEY);
		int xbase = readInt(base, XMLCreator.X_BASE_KEY);
        int ybase = readInt(base, XMLCreator.Y_BASE_KEY);
		
		IPoint com = readPoint(e.getChild(XMLCreator.COM_KEY));
		
		File imageFile = readFile(e, XMLCreator.SOURCE_FILE_KEY);
		
		int channel = readInt(e, XMLCreator.SOURCE_CHANNEL_KEY);
		
		UUID id = readUUID(e);
		
		int nucleusNumber = readInt(e, XMLCreator.NUCLEUS_NUMBER_KEY);
		
		Nucleus n = fact.buildInstance(roi, imageFile, channel, originalPosition, com, id, nucleusNumber);
		// Move the nucleus xbase and ybase to 0,0 coordinates for charting
        IPoint offsetCoM = IPoint.makeNew(com.getX() - xbase, com.getY() - ybase);
        n.moveCentreOfMass(offsetCoM);
		
		
		// Add stats
		Element stats = e.getChild(XMLCreator.STATS_SECTION_KEY);
		for(Element stat : stats.getChildren(XMLCreator.STAT_KEY)) {
			Measurement s = readStat(stat);
			double d = readDouble(stat, XMLCreator.VALUE_KEY);
			n.setStatistic(s, d);
		}
		
		n.setScale(readDouble(e, XMLCreator.SOURCE_SCALE_X_KEY)); // fallback until X and Y are handled separately
		
		n.initialise(windowProportion);
		
		int actualLength = n.getBorderLength();
		int expLength    = readInt(e, XMLCreator.BORDER_LENGTH_KEY);
		if(actualLength!=expLength)
			LOGGER.warning(String.format("Border interpolation to %s does not match saved value %s", actualLength, expLength));
		
		// Apply tags
		readTags(e.getChild(XMLCreator.BORDER_TAGS_KEY), n);
		
		// Apply segments
		readSegments(e.getChild(XMLCreator.BORDER_SEGS_KEY), n);
		
		
		// Apply signals
		readSignals(e.getChild(XMLCreator.SIGNAL_GROUPS_SECTION_KEY), n);
		
		return n;
	}
	
	private Roi readRoi(Element e) {
		Element border = e.getChild(XMLCreator.BORDER_POINTS_KEY);
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
		return roi;
	}
	
	private int[] readOriginalPosition(Element e) {
		Element base = e.getChild(XMLCreator.BASE_KEY);
        int[] originalPosition = { readInt(base, XMLCreator.X_BASE_KEY), 
        		readInt(base, XMLCreator.Y_BASE_KEY), 
        		readInt(base, XMLCreator.W_BASE_KEY), 
        		readInt(base, XMLCreator.H_BASE_KEY) };
        return originalPosition;
	}
	
	private void readSignals(Element signals, Nucleus n) {
		if(signals==null)
			return;
		for(Element groupElement : signals.getChildren(XMLCreator.SIGNAL_GROUP_KEY)) {	
			UUID groupId = readUUID(groupElement);
			for(Element signalElement : groupElement.getChildren(XMLCreator.NUCLEAR_SIGNAL_KEY)) {
				Roi roi = readRoi(signalElement);

				int[] originalPosition = readOriginalPosition(signalElement);
				
				Element base = signalElement.getChild(XMLCreator.BASE_KEY);
				int xbase = readInt(base, XMLCreator.X_BASE_KEY);
		        int ybase = readInt(base, XMLCreator.Y_BASE_KEY);
				
				IPoint com = readPoint(signalElement.getChild(XMLCreator.COM_KEY));
				
				File imageFile = readFile(signalElement, XMLCreator.SOURCE_FILE_KEY);
				
				int channel = readInt(signalElement, XMLCreator.SOURCE_CHANNEL_KEY);
				
				UUID id = readUUID(signalElement);
				INuclearSignal s = new DefaultNuclearSignal(roi, com, imageFile, channel, originalPosition, id);
				
				// Add stats
				Element stats = signalElement.getChild(XMLCreator.STATS_SECTION_KEY);
				for(Element statElement : stats.getChildren(XMLCreator.STAT_KEY)) {
					Measurement stat = readStat(statElement);
					double d = readDouble(statElement, XMLCreator.VALUE_KEY);
					s.setStatistic(stat, d);
				}
				
				s.setScale(readDouble(signalElement, XMLCreator.SOURCE_SCALE_X_KEY));
				
				n.getSignalCollection().addSignal(s, groupId);
			}
			
		}
	}
	
	
	private void readTags(Element tags, Nucleus n) {
		for(Element tag : tags.getChildren()) {			
			Landmark t = readTag(tag);
			int index = readInt(tag, XMLCreator.INDEX_KEY);
			n.setBorderTag(t, index);
		}
	}
	
	
	private void readSegments(Element segs, Nucleus n) {
		
		List<IProfileSegment> newSegs = new ArrayList<>();
		int prevStart = -1;
		UUID prevId = null;
		for(Element seg : segs.getChildren()) {			
			UUID id = readUUID(seg);				
			int startIndex = readInt(seg, XMLCreator.INDEX_KEY);
			if(prevId!=null) {
				IProfileSegment newSeg = IProfileSegment.newSegment(prevStart, startIndex, n.getBorderLength(), prevId);
				newSegs.add(newSeg);
			}
			prevStart = startIndex;
			prevId = id;
		}
		
		if(prevId!=null) {
			IProfileSegment lastSeg = IProfileSegment.newSegment(prevStart, newSegs.get(0).getStartIndex(), n.getBorderLength(), prevId);
			newSegs.add(lastSeg);
		}
		try {			
			for(ProfileType type : ProfileType.values()) { 
				IProfile profile = n.getProfile(type);
				ISegmentedProfile segmented = new SegmentedFloatProfile(profile, newSegs);
				n.setProfile(type, segmented);
				
				ISegmentedProfile p = n.getProfile(type, Landmark.REFERENCE_POINT); // ensure all profiles are updated to RP
				n.setProfile(type, Landmark.REFERENCE_POINT, p);
			}

		} catch(Exception e1) {
			LOGGER.log(Loggable.STACK, e1.getMessage(), e1);
		}
	}

}
