package com.bmskinner.nuclear_morphology.io.xml;

import java.util.List;
import java.util.UUID;

import org.jdom2.Document;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.ICytoplasm;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalCollection;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * Base class for creating XML representations of datasets 
 * @author ben
 *
 * @param <T> the type of object to create
 * @since 1.14.0
 */
public abstract class XMLCreator<T> {
	
	public static final String CELL_COLLECTION_KEY            = "CellCollection";
	public static final String NUCLEUS_TYPE_KEY               = "NucleusType";
	public static final String CELL_IDS_KEY                   = "CellIds";
	public static final String CELL_KEY                       = "Cell";
	public static final String ID_KEY                         = "Id";
	public static final String COM_KEY                        = "CentreOfMass";
	public static final String X                              = "X";
	public static final String Y                              = "Y";
	public static final String BORDER_POINTS_KEY              = "UninterpolatedBorderPoints";
	public static final String POINT_KEY                      = "Point";
	public static final String NAME_KEY                       = "Name";
	public static final String VALUE_KEY                      = "Value";
	public static final String INDEX_KEY                      = "Index";
	public static final String STATS_KEY                      = "MeasuredStatistics";
	public static final String STAT_KEY                       = "Statistic";
	
	
	public static final String SOURCE_FILE_KEY                = "SourceFile";
	public static final String SOURCE_CHANNEL_KEY             = "SourceChannel";
	public static final String SOURCE_SCALE_KEY               = "SourceScale";
	
	public static final String CYTOPLASM_KEY                  = CellularComponent.CYTOPLASM;
	public static final String NUCLEUS_KEY                    = CellularComponent.NUCLEUS;
	public static final String CONSENSUS_KEY                  = "ConsensusNucleus";
	
	public static final String BORDER_TAGS_KEY               = "BorderTags";
	public static final String BORDER_TAG_KEY                = "Tag";
	
	public static final String BORDER_SEGS_KEY               = "BorderSegments";
	public static final String BORDER_SEG_KEY                = "Segment";
	
	public static final String NUCLEAR_SIGNALS_KEY            = CellularComponent.NUCLEAR_SIGNAL+"s";
	public static final String NUCLEAR_SIGNAL_KEY             = CellularComponent.NUCLEAR_SIGNAL;
	public static final String NUCLEAR_SIGNAL_GROUP_KEY       = "SignalGroup";
	
	public static final String BORDER_LENGTH_KEY             = "InterpolatedBorderLength";
	public static final String PROFILE_LENGTH_KEY            = "ProfileLength";
	
	protected final T template;
	
	public XMLCreator(T template) {
		this.template = template;
	}
	
	/**
	 * Create an XML representation of the object
	 * @return
	 */
	public abstract Document create();
	
	protected Element createElement(String key, String value) {
		Element e = new Element(key);
		e.setText(value);
		return e;
	}
	
	/**
	 * Test if the given string could be a UUID 
	 * @param s
	 * @return
	 */
	public static boolean isUUID(String s) {
		if(s==null)
			return false;
		if(s.length()!=36)
			return false;
		if(s.matches("[\\w|\\d]{8}-[\\w|\\d]{4}-[\\w|\\d]{4}-[\\w|\\d]{4}-[\\w|\\d]{12}"))
			return true;
		return false;
	}
	
	
	/**
	 * Create XML for the given collection of cells
	 * @param collection
	 * @return
	 */
	protected Element create(ICellCollection collection) {
		Element e = new Element(CELL_COLLECTION_KEY);
		e.addContent(createElement(NUCLEUS_TYPE_KEY, collection.getNucleusType().toString()));
		
		for(ICell cell : collection)
			e.addContent(create(cell));
		
		if(collection.hasConsensus()) {
			Element consensus = new Element(CONSENSUS_KEY);
			consensus.addContent(create(collection.getConsensus()));
			e.addContent(consensus);
		}
				
		e.addContent(createElement(PROFILE_LENGTH_KEY, String.valueOf(collection.getProfileCollection().length())));	
		
		// Add border tags
		Element tags = new Element(BORDER_TAGS_KEY);
		for(Tag t : collection.getProfileCollection().getBorderTags()) {
			Element tag = new Element(BORDER_TAG_KEY);
			tag.addContent(createElement(NAME_KEY, t.getName()));
			try {
				int i = collection.getProfileCollection().getIndex(t);
				tag.addContent(createElement(INDEX_KEY, String.valueOf(i)));
			} catch (UnavailableBorderTagException e1) {
				// ignore missing tags
			}
			tags.addContent(tag);
		}
		e.addContent(tags);
		
		// Add segments
		Element segs = new Element(BORDER_SEGS_KEY);
		try {
			ISegmentedProfile profile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);

			for(IBorderSegment s : profile.getSegments()) {
				Element seg = new Element(BORDER_SEG_KEY);
				seg.addContent(createElement(ID_KEY, s.getID().toString()));
				seg.addContent(createElement(INDEX_KEY, String.valueOf(s.getStartIndex())));
				segs.addContent(seg);
			}
			e.addContent(segs);
		} catch (UnavailableProfileTypeException | UnavailableBorderTagException | ProfileException | UnsegmentedProfileException e1) {
			// ignore missing profiles
		}
		
		return e;
	}
	
	protected Element create(ICell cell) {
		Element e = new Element(CELL_KEY);
		e.addContent(createElement(ID_KEY, cell.getId().toString()));
		if(cell.hasCytoplasm())
			e.addContent(create(cell.getCytoplasm()));
		
		for(Nucleus n : cell.getNuclei())
			e.addContent(create(n));
			

		
		return e;
	}
	
	protected Element create(ICytoplasm cytoplasm) {
		Element e = new Element(CYTOPLASM_KEY);
		create(e, cytoplasm);
		return e;
	}
	
	protected Element create(Nucleus nucleus) {
		Element e = new Element(NUCLEUS_KEY);
		create(e, nucleus);
		
		// Add signals
		
		ISignalCollection sc = nucleus.getSignalCollection();
		if(sc.hasSignal()) {
			Element signals = new Element(NUCLEAR_SIGNALS_KEY);
			
			for(UUID signalGroupId : sc.getSignalGroupIds()) {
				
				Element group = new Element(NUCLEAR_SIGNAL_GROUP_KEY);
				group.addContent(createElement(ID_KEY, signalGroupId.toString()));
				
				List<INuclearSignal> sigList = sc.getSignals(signalGroupId);
				for(INuclearSignal s : sigList) {
					Element signal = new Element(NUCLEAR_SIGNAL_KEY);
					create(signal, s);
					group.addContent(signal);
				}
				signals.addContent(group);
			}
		}
		
		return e;
	}
	
	protected Element create(Element e, Taggable taggable) {
		create(e, (CellularComponent)taggable);
		
		// Add border tags
		Element tags = new Element(BORDER_TAGS_KEY);
		for(Tag t : taggable.getBorderTags().keySet()) {
			Element tag = new Element(BORDER_TAG_KEY);
			tag.addContent(createElement(NAME_KEY, t.getName()));
			try {
				int i = taggable.getBorderIndex(t);
				tag.addContent(createElement(INDEX_KEY, String.valueOf(i)));
			} catch (UnavailableBorderTagException e1) {
				// ignore missing tags
			}
			tags.addContent(tag);
		}
		e.addContent(tags);
		
		// Add segments
		Element segs = new Element(BORDER_SEGS_KEY);
		try {
			ISegmentedProfile profile = taggable.getProfile(ProfileType.ANGLE);
			
			for(IBorderSegment s : profile.getSegments()) {
				Element seg = new Element(BORDER_SEG_KEY);
				seg.addContent(createElement(ID_KEY, s.getID().toString()));
				seg.addContent(createElement(INDEX_KEY, String.valueOf(s.getStartIndex())));
				segs.addContent(seg);
			}
			e.addContent(segs);
		} catch (UnavailableProfileTypeException e1) {
			// ignore missing profiles
		}
	
		return e;
	}
	
	
	/**
	 * Cellular components are abstract; create the element to add to in an implementation create method.
	 * E.g. create(Nucleus)  
	 * @param e
	 * @param component
	 * @return
	 */
	private Element create(Element e, CellularComponent component) {
		e.addContent(createElement(ID_KEY, component.getID().toString()));
		
		e.addContent(create(COM_KEY, component.getOriginalCentreOfMass()));
		e.addContent(createElement(SOURCE_FILE_KEY, component.getSourceFile().getAbsolutePath()));
		e.addContent(createElement(SOURCE_CHANNEL_KEY, String.valueOf(component.getChannel())));
		e.addContent(createElement(SOURCE_SCALE_KEY, String.valueOf(component.getScale())));
		e.addContent(createElement(BORDER_LENGTH_KEY, String.valueOf(component.getBorderLength())));
		
		// add border points
		Element border = new Element(BORDER_POINTS_KEY);		
		int[][] points = component.getUnsmoothedBorderCoordinates();
		int[] xpoints = points[0];
		int[] ypoints = points[1];
		
		for(int i=0; i<xpoints.length; i++)
			border.addContent(create(POINT_KEY, xpoints[i], ypoints[i]));
		
		e.addContent(border);
		
		// add stats
		Element stats = new Element(STATS_KEY);
		for(PlottableStatistic s : component.getStatistics()) {
			Element stat = new Element(STAT_KEY);
			stat.addContent(createElement(NAME_KEY, s.toString()));
			stat.addContent(createElement(VALUE_KEY, String.valueOf(component.getStatistic(s))));
			stats.addContent(stat);
		}
		e.addContent(stats);
		return e;
	}
	
	private Element create(String key , IPoint point) {
		Element e = new Element(key);
		e.addContent(createElement(X, String.valueOf(point.getX())));
		e.addContent(createElement(Y, String.valueOf(point.getY())));
		return e;
	}
	
	private Element create(String key , int x, int y) {
		Element e = new Element(key);
		e.addContent(createElement(X, String.valueOf(x)));
		e.addContent(createElement(Y, String.valueOf(y)));
		return e;
	}

}
