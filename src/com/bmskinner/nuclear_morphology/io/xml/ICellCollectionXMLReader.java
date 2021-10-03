package com.bmskinner.nuclear_morphology.io.xml;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.datasets.ICellCollection;
import com.bmskinner.nuclear_morphology.components.nuclei.Consensus;
import com.bmskinner.nuclear_morphology.components.nuclei.DefaultConsensusNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusFactory;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.UnprofilableObjectException;
import com.bmskinner.nuclear_morphology.components.signals.DefaultSignalGroup;
import com.bmskinner.nuclear_morphology.components.signals.ISignalGroup;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Reader for cell collections
 * @author bms41
 * @since 1.15.1
 *
 */
public class ICellCollectionXMLReader extends XMLReader<ICellCollection> {
	
	private static final Logger LOGGER = Logger.getLogger(ICellCollectionXMLReader.class.getName());
	
	private final NucleusFactory fact;
	private final double windowProportion;
	private final String collectionName;
	private final UUID collectionId;

	/**
	 * Create a reader
	 * @param e the cell element to read
	 * @param windowProportion the window proportion
	 * @param 
	 */
	public ICellCollectionXMLReader(@NonNull final Element e, final double windowProportion, @NonNull String name, @NonNull UUID id) {
		super(e);
		this.fact = new NucleusFactory();
		this.windowProportion = windowProportion;
		this.collectionName = name;
		this.collectionId = id;
	}
	
	@Override
	public ICellCollection read() throws XMLReadingException {
//		return readRealCollection(rootElement);
		return null;
	}
	
	private void readCollectionSegments(Element segs, ICellCollection collection) {
		int profileLength = collection.getProfileCollection().length();
		List<IProfileSegment> newSegs = new ArrayList<>();
		int prevStart = -1;
		UUID prevId = null;
		for(Element seg : segs.getChildren()) {			
			UUID id = readUUID(seg);
			int startIndex = readInt(seg, XMLCreator.INDEX_KEY);
			if(prevId!=null) {
				IProfileSegment newSeg = IProfileSegment.newSegment(prevStart, startIndex, profileLength, prevId);
				newSegs.add(newSeg);
			}
			prevStart = startIndex;
			prevId = id;
		}
		if(prevId!=null) {
			IProfileSegment lastSeg = IProfileSegment.newSegment(prevStart, newSegs.get(0).getStartIndex(), profileLength, prevId);
			newSegs.add(lastSeg);
		}
		
		collection.getProfileCollection().addSegments(newSegs);

	}
		
	private void readSignalGroups(Element e, ICellCollection collection) {
		if(e==null)
			return;
		
		for(Element groupElement : e.getChildren(XMLCreator.SIGNAL_GROUP_KEY)) {
			LOGGER.fine("Adding signal group to "+collection.getName());
			String name = groupElement.getChildText(XMLCreator.NAME_KEY);
			UUID id = readUUID(groupElement);
			ISignalGroup sg = new DefaultSignalGroup(name);
			Element colourElement = groupElement.getChild(XMLCreator.COLOUR_KEY);
			if(colourElement!=null)
				sg.setGroupColour(Color.decode(colourElement.getText()));
			LOGGER.fine("Adding signal group "+sg.toString());
			collection.addSignalGroup(id, sg);
		}
	}
	
	private Consensus<Nucleus> readConsensus(Element e) throws XMLReadingException {
		XMLReader<Nucleus> nuclReader = new NucleusXMLReader(e.getChild(XMLCreator.NUCLEUS_KEY), fact, windowProportion);
		Nucleus template = nuclReader.read();
		try {
			return new DefaultConsensusNucleus(template);
		} catch (UnprofilableObjectException e1) {
			LOGGER.log(Loggable.STACK, "Error reading consensus", e1);
			throw new XMLReadingException(e1);
		}
	}
}
