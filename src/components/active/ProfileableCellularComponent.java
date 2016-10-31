package components.active;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import analysis.profiles.ProfileCreator;
import analysis.profiles.Profileable;
import analysis.profiles.Taggable;
import components.CellularComponent;
import components.active.generic.DefaultBorderPoint;
import components.active.generic.FloatPoint;
import components.active.generic.SegmentedFloatProfile;
import components.active.generic.UnprofilableObjectException;
import components.generic.IPoint;
import components.generic.IProfile;
import components.generic.ISegmentedProfile;
import components.generic.ProfileType;
import components.generic.Tag;
import components.nuclear.IBorderPoint;
import components.nuclei.Nucleus;
import ij.gui.Roi;
import stats.NucleusStatistic;

/**
 * This is the class of objects that can have angle profiles applied to them.
 * @author ben
 *
 */
public abstract class ProfileableCellularComponent 
	extends DefaultCellularComponent 
	implements Taggable {
	
	private static final long serialVersionUID = 1L;
	public static final double ERROR_CALCULATING_STAT   = -1d;
	public static final double BORDER_POINT_NOT_PRESENT = -2d;
	
	protected double angleWindowProportion; // The proportion of the perimeter to use for profiling
	
	protected Map<ProfileType, ISegmentedProfile> profileMap = new HashMap<ProfileType, ISegmentedProfile>();

	protected Map<Tag, Integer>    borderTags  = new HashMap<Tag, Integer>(0); // the indexes of tags in the profiles and border list
			
	private boolean segsLocked = false; // allow locking of segments and tags if manually assigned
	
	/*
	 * TRANSIENT FIELDS
	 */

	protected transient int    angleProfileWindowSize; // the chosen window size for the nucleus based on proportion
	
	
	
	public ProfileableCellularComponent(Roi roi, File f, int channel, int[] position){
		super(roi, f, channel, position);
		
	}
	
	/**
	 * Construct with an ROI, a source image and channel, and the original position in the source image
	 * @param roi
	 * @param f
	 * @param channel
	 * @param position
	 * @param centreOfMass
	 */
	public ProfileableCellularComponent(Roi roi, File f, int channel, int[] position, IPoint centreOfMass){
		super(roi, f, channel, position, centreOfMass );
	}
		
	public ProfileableCellularComponent(CellularComponent c) throws UnprofilableObjectException {
		super(c);
		
		if( c instanceof Taggable ){


			Taggable comp = (Taggable) c;

			finest("Created cellular component");		

			this.angleWindowProportion  = comp.getWindowProportion(ProfileType.ANGLE);
			this.angleProfileWindowSize = comp.getWindowSize(ProfileType.ANGLE);


			for(ProfileType type : ProfileType.values()){
				if(comp.hasProfile(type)){
					this.profileMap.put(type, ISegmentedProfile.makeNew(comp.getProfile(type)));
				}
			}


			this.setBorderTags(comp.getBorderTags());

			this.segsLocked = comp.isLocked();
		} else {
			throw new UnprofilableObjectException("Object is not a profileable object");
		}

	}
		
	/*
	* Finds the key points of interest around the border
	* of the Nucleus. Can use several different methods, and 
	* take a best-fit, or just use one. The default in a round 
	* nucleus is to get the longest diameter and set this as
	*  the head/tail axis.
	*/
	public abstract void findPointsAroundBorder();
	

	public void initialise(double proportion) {

		this.angleWindowProportion = proportion;
		
		double perimeter = this.getStatistic(NucleusStatistic.PERIMETER);
		double angleWindow = perimeter * proportion;
		
		
		// calculate profiles
		this.angleProfileWindowSize = (int) Math.round(angleWindow);


		calculateProfiles();
    
	}
	
	public IBorderPoint getPoint(Tag tag){	
		int index = this.getBorderIndex(tag);
		return this.getBorderPoint(index);
	}
	
	/**
	 * Checks if the smoothed array nuclear shape profile has the appropriate
	 * orientation.Counts the number of points above 180 degrees
	 * in each half of the array.
	 * @return 
	 * @throws Exception
	 */
	public abstract boolean isProfileOrientationOK();
	
	/*
	 * #############################################
	 * Methods implementing the Taggable interface
	 * #############################################
	 */
	
	
	
	/**
	 * 
	 * @param p
	 * @param pointType
	 * @throws Exception
	 */
	public void setProfile(ProfileType type, Tag tag, ISegmentedProfile p) throws Exception{
		
		if(segsLocked){
			return;
		}
		
		// fetch the index of the pointType (the zero of the input profile)
		int pointIndex = this.borderTags.get(tag);
		
		// remove the offset from the profile, by setting the profile to start from the pointIndex
		this.setProfile(type, new SegmentedFloatProfile(p).offset(-pointIndex));
//		this.updateVerticallyRotatedNucleus();
	}

	
	public IBorderPoint getBorderTag(Tag tag){
		IBorderPoint result = new DefaultBorderPoint(0,0);
		if(this.getBorderIndex(tag)>-1){
			result = this.getBorderPoint((this.getBorderIndex(tag)));
		} else {
			return null;
		}
		return result;
	}
	
	public IBorderPoint getBorderPoint(Tag tag){
		return getBorderTag(tag) ;
	}
		
	public Map<Tag, Integer> getBorderTags(){
		Map<Tag, Integer> result = new HashMap<Tag, Integer>();
		for(Tag b : borderTags.keySet()){
			result.put(b,  borderTags.get(b));
		}
		return result;
	}
	
	public void setBorderTags(Map<Tag, Integer> m){
		if(segsLocked){
			return;
		}
		this.borderTags = m;
	}
	
	public int getBorderIndex(Tag tag){
		int result = -1;
		if(this.borderTags.containsKey(tag)){
			result = this.borderTags.get(tag);
		}
		return result;
	}
	
	
	public void setBorderTag(Tag tag, int i){
		if(segsLocked){
			return;
		}
		// When moving the RP, move all segments to match
		if(tag.equals(Tag.REFERENCE_POINT)){
			ISegmentedProfile p = getProfile(ProfileType.ANGLE);
			int oldRP = getBorderIndex(tag);
			int diff  = i-oldRP;
			p.nudgeSegments(diff);
			finest("Old RP at "+oldRP);
			finest("New RP at "+i);
			finest("Moving segments by"+diff);
			
			setProfile(ProfileType.ANGLE, p);

			
		}

		this.borderTags.put( tag, i);

		// The intersection point should always be opposite the orientation point
		if(tag.equals(Tag.ORIENTATION_POINT)){
			int intersectionIndex = this.getBorderIndex(this.findOppositeBorder( this.getBorderPoint(i) ));
			this.setBorderTag(Tag.INTERSECTION_POINT, intersectionIndex);
//			updateVerticallyRotatedNucleus(); // force an update
		}
		
		if(tag.equals(Tag.TOP_VERTICAL) || tag.equals(Tag.BOTTOM_VERTICAL)){
//			updateVerticallyRotatedNucleus();
		}
	}
	
	public void setBorderTag(Tag reference, Tag tag, int i){
		if(segsLocked){
			return;
		}
		int newIndex = getOffsetBorderIndex(reference, i);
		this.setBorderTag(tag, newIndex);
	}
	
	
	public void replaceBorderTags(Map<Tag, Integer> tagMap){
		
		int oldRP = getBorderIndex(Tag.REFERENCE_POINT);
		ISegmentedProfile p = getProfile(ProfileType.ANGLE);
		
		this.borderTags = tagMap;
		
		
		int newRP = getBorderIndex(Tag.REFERENCE_POINT);
		int diff  = newRP-oldRP;
		p.nudgeSegments(diff);
		finest("Old RP at "+oldRP);
		finest("New RP at "+newRP);
		finest("Moving segments by"+diff);

		setProfile(ProfileType.ANGLE, p);


		
		int newOP = getBorderIndex(Tag.ORIENTATION_POINT);
		int intersectionIndex = this.getBorderIndex(this.findOppositeBorder( this.getBorderPoint(newOP) ));
		this.borderTags.put(Tag.INTERSECTION_POINT, intersectionIndex);
		
		
//		updateVerticallyRotatedNucleus();		
		
	}
	
		
	public boolean hasBorderTag(Tag tag){
		return this.borderTags.containsKey(tag);
	}
	
	public boolean hasBorderTag( int index){
		return this.borderTags.containsValue(index);
	}
	
	public boolean hasBorderTag(Tag tag, int index){
				
		// remove offset
		int newIndex = getOffsetBorderIndex(tag, index);
		return this.hasBorderTag(newIndex);
	}
	
	public int getOffsetBorderIndex(Tag reference, int index){
		if(this.getBorderIndex(reference)>-1){
			int newIndex =  wrapIndex( index+this.getBorderIndex(reference) );
			
//			int newIndex =  AbstractCellularComponent.wrapIndex( index+this.getBorderIndex(reference) , this.getBorderLength() );
			return newIndex;
		}
		return -1;
	}
	
	public Tag getBorderTag(Tag tag, int index){
		int newIndex = getOffsetBorderIndex(tag, index);
		return this.getBorderTag(newIndex);
	}
	
	public Tag getBorderTag(int index){

		for(Tag b : this.borderTags.keySet()){
			if(this.borderTags.get(b)==index){
				return b;
			}
		}
		return null;
	}
	
	

	/*
	 * #############################################
	 * Methods implementing the Profileable interface
	 * #############################################
	 */
	
	
	public boolean isLocked(){
		return segsLocked;
	}
	

	public void setLocked(boolean b){
		segsLocked = b;
	}
	
	
	@Override
	public int getWindowSize(ProfileType type){
		switch(type){
			case ANGLE: { 
				return angleProfileWindowSize;
			}
			
			default:{
				return Profileable.DEFAULT_PROFILE_WINDOW; // Not needed for DIAMETER and RADIUS
			}
		}
	}
	
	public double getWindowProportion(ProfileType type){
		
		switch(type){
			case ANGLE: { 
				return angleWindowProportion;
			}
			
			default:{
				return Profileable.DEFAULT_PROFILE_WINDOW_PROPORTION; // Not needed for DIAMETER and RADIUS
			}
		}
	}
	

	public void setWindowProportion(ProfileType type, double d){
		if(d<0 || d> 1){
			throw new IllegalArgumentException("Angle window proportion must be 0-1");
		}
		
		if(segsLocked){
			return;
		}
		
		if(type.equals(ProfileType.ANGLE)){
			
			this.angleWindowProportion = d;
			
			double perimeter = this.getStatistic(NucleusStatistic.PERIMETER);
			double angleWindow = perimeter * d;
			
			
			// calculate profiles
			this.angleProfileWindowSize = (int) Math.round(angleWindow);
			finest("Recalculating angle profile");
			ProfileCreator creator = new ProfileCreator(this);
			ISegmentedProfile profile = creator.createProfile(ProfileType.ANGLE);
			
			this.profileMap.put(ProfileType.ANGLE, profile);		
			
		}
	}
	
	
	public ISegmentedProfile getProfile(ProfileType type) {
		if(this.hasProfile(type)){
			return new SegmentedFloatProfile(this.profileMap.get(type));
		} else {
			throw new IllegalArgumentException("Profile type "+type+" is not found in this nucleus");
		}
	}
	
	public boolean hasProfile(ProfileType type){
		return this.profileMap.containsKey(type);
	}


	public ISegmentedProfile getProfile(ProfileType type, Tag tag){
		
		// fetch the index of the pointType (the new zero)
		int pointIndex = this.borderTags.get(tag);
		
		ISegmentedProfile profile = null;
		if(this.hasProfile(type)){
			
			// offset the angle profile to start at the pointIndex
			profile =  new SegmentedFloatProfile(this.getProfile(type).offset(pointIndex));
			
		}

		return profile;
	}
	

	public void setProfile(ProfileType type, ISegmentedProfile profile) {
		if(profile==null){
			throw new IllegalArgumentException("Error setting nucleus profile: type "+type+" is null");
		}
		
		if(segsLocked){
			return;
		}
		
		// Replace frankenprofiles completely
		if(type.equals(ProfileType.FRANKEN)){
			this.profileMap.put(type, profile);
		} else { // Otherwise update the segment lists for all other profile types

			for(ProfileType t : profileMap.keySet()){
				if( ! t.equals(ProfileType.FRANKEN)){
					this.profileMap.get(type).setSegments(profile.getSegments());
				}
			}
		}
	}
	
	public void calculateProfiles() {
		
		/*
		 * All these calculations operate on the same border point order
		 */
		
		ProfileCreator creator = new ProfileCreator(this);
		
		for(ProfileType type : ProfileType.values()){
			
			ISegmentedProfile profile = creator.createProfile(type);
			profileMap.put(type, profile);
		}
	}
	
	public void setSegmentStartLock(boolean lock, UUID segID){
		if(segID==null){
			throw new IllegalArgumentException("Requested seg id is null");
		}
		for(ISegmentedProfile p : this.profileMap.values()){
			
			if(p.hasSegment(segID)){
				p.getSegment(segID).setLocked(lock);
			}
		}
	}


	public double getPathLength(ProfileType type) {
		double pathLength = 0;

		IProfile profile = this.getProfile(type);
		
		// First previous point is the last point of the profile
		IPoint prevPoint = new FloatPoint(0,profile.get(this.getBorderLength()-1));
		 
		for (int i=0; i<this.getBorderLength();i++ ) {
				double normalisedX = ((double)i/(double)this.getBorderLength())*100; // normalise to 100 length
				
				// We are measuring along the chart of angle vs position
				// Each median angle value is treated as an XYPoint
				IPoint thisPoint = new FloatPoint(normalisedX, profile.get(i));
				pathLength += thisPoint.getLengthTo(prevPoint);
				prevPoint = thisPoint;
		}
		return pathLength;
	}

	public void reverse(){
		
		super.reverse();
		
		if(segsLocked){
			return;
		}
		for(ProfileType type : profileMap.keySet()){

			ISegmentedProfile profile = profileMap.get(type);
			profile.reverse();
			profileMap.put(type, profile);
		}
		
//		List<IBorderPoint> reversed = new ArrayList<IBorderPoint>(0);
//		for(int i=this.getBorderLength()-1; i>=0;i--){
//			reversed.add(this.getBorderPoint(i));
//		}
//		this.setBorderList(reversed);

		// replace the tag positions also
		Set<Tag> keys = borderTags.keySet();
		for( Tag s : keys){
			int index = borderTags.get(s);
			int newIndex = this.getBorderLength() - index - 1; // if was 0, will now be <length-1>; if was length-1, will be 0
//			 update the bordertag map directly to avoid segmentation changes due to RP shift
			borderTags.put(s, newIndex);
		}
	}
	

	public IBorderPoint getNarrowestDiameterPoint() {

		int index = this.getProfile(ProfileType.DIAMETER).getIndexOfMin();

		return IBorderPoint.makeNew(this.getBorderPoint(index));
	}
	
	public double getNarrowestDiameter() {
		return Arrays.stream(this.getProfile(ProfileType.DIAMETER).asArray()).min().orElse(0);
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		
		in.defaultReadObject();
		
		// set transient fields
		double perimeter = this.getStatistic(NucleusStatistic.PERIMETER);
		double angleWindow = perimeter * angleWindowProportion;
		this.angleProfileWindowSize = (int) Math.round(angleWindow);

	}

}
