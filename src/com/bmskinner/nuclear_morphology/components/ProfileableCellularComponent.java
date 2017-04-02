/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.components;

import ij.gui.Roi;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileCreator;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.analysis.profiles.Profileable;
import com.bmskinner.nuclear_morphology.analysis.profiles.Taggable;
import com.bmskinner.nuclear_morphology.components.generic.DefaultBorderPoint;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.SegmentedFloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnprofilableObjectException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

/**
 * This is the class of objects that can have profiles applied to them.
 * Positions around the border of the component can be tagged; the profiles will
 * track the tags.
 * @author ben
 * @since 1.13.3
 *
 */
public abstract class ProfileableCellularComponent 
	extends DefaultCellularComponent 
	implements Taggable {
	
	private static final long serialVersionUID = 1L;
	
	protected double angleWindowProportion; // The proportion of the perimeter to use for profiling
	
	protected Map<ProfileType, ISegmentedProfile> profileMap = new HashMap<ProfileType, ISegmentedProfile>();

	protected Map<Tag, Integer>    borderTags  = new HashMap<Tag, Integer>(0); // the indexes of tags in the profiles and border list
			
	private boolean segsLocked = false; // allow locking of segments and tags if manually assigned
	
	/*
	 * TRANSIENT FIELDS
	 */

	protected transient int    angleProfileWindowSize; // the chosen window size for the nucleus based on proportion

	
//	public ProfileableCellularComponent(Roi roi, File f, int channel, int[] position){
//		super(roi, f, channel, position);
//		
//	}
	
	/**
	 * Construct with an ROI, a source image and channel, and the original position in the source image
	 * @param roi
	 * @param f
	 * @param channel
	 * @param position
	 * @param centreOfMass
	 */
	public ProfileableCellularComponent(Roi roi, IPoint centreOfMass, File f, int channel, int[] position){
		super(roi, centreOfMass, f, channel, position );
	}
		
	public ProfileableCellularComponent(CellularComponent c) throws UnprofilableObjectException {
		super(c);
		
		if( c instanceof Taggable ){

			Taggable comp = (Taggable) c;

			finest("Created cellular component");
//			log("Duplicating "+this.getClass().getSimpleName());

			this.angleWindowProportion  = comp.getWindowProportion(ProfileType.ANGLE);
			this.angleProfileWindowSize = comp.getWindowSize(ProfileType.ANGLE);


			for(ProfileType type : ProfileType.values()){

				try {
					
					ISegmentedProfile oldProfile = comp.getProfile(type);
					ISegmentedProfile newProfile = ISegmentedProfile.makeNew(oldProfile);
					
					this.profileMap.put(type, newProfile);
					
				} catch (UnavailableProfileTypeException e) {
					stack("Cannot get profile type "+type+" from template", e);
					warn("Error copying profile");
				} catch (ProfileException e) {
					stack("Cannot make new profile type "+type+" from template", e);
					warn("Error copying profile");
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

		finest("Initialising profilable object");
		this.angleWindowProportion = proportion;
		
		finest("Getting perimeter");
		double perimeter = this.getStatistic(PlottableStatistic.PERIMETER);
		double angleWindow = perimeter * proportion;
		
		
		// calculate profiles
		this.angleProfileWindowSize = (int) Math.round(angleWindow);
		finest("Set window size");

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
	
	
	
	@Override
	protected double calculateStatistic(PlottableStatistic stat) {
		
		double result = super.calculateStatistic(stat);
				
		if(PlottableStatistic.MIN_DIAMETER.equals(stat)){
			return this.getNarrowestDiameter();
		}
		
		if(PlottableStatistic.PATH_LENGTH.equals(stat)){
			return this.getPathLength(ProfileType.ANGLE);
		}
	
		return result;
	}
	
	
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
	public void setProfile(ProfileType type, Tag tag, ISegmentedProfile p) throws UnavailableBorderTagException {
		
		if(! this.hasBorderTag(tag)){
			throw new UnavailableBorderTagException("Tag "+tag+" is not present");
		}
		
		if(segsLocked){
			return;
		}
		
		if(this.getBorderLength()!= p.size()){
			throw new IllegalArgumentException("Input profile does not match border length of object");
		}
				
		// fetch the index of the pointType (the zero of the input profile)
		int pointIndex = this.borderTags.get(tag);
		
		fine("Setting profile "+type+" at "+tag+" offsetting given profile by -"+pointIndex);
		
		// remove the offset from the profile, by setting the profile to start from the pointIndex
		try {
			this.setProfile(type, new SegmentedFloatProfile(p).offset(-pointIndex));
		} catch (ProfileException e) {
			stack("Error setting profile "+type+" at "+tag, e);
			return;
		}

	}

	
	public IBorderPoint getBorderTag(Tag tag) throws UnavailableBorderTagException {
		
		IBorderPoint result = new DefaultBorderPoint(0,0);
		
		int borderIndex = this.getBorderIndex(tag);
			
		if(borderIndex <0 || borderIndex>=this.getBorderLength()){
			throw new UnavailableBorderTagException("Tag "+tag+" is registered as index "+borderIndex);
		}
			
		result = this.getBorderPoint((this.getBorderIndex(tag)));
		return result;
	}
	
	public IBorderPoint getBorderPoint(Tag tag) throws UnavailableBorderTagException{
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
		int result = BORDER_INDEX_NOT_FOUND;
		if(this.borderTags.containsKey(tag)){
			result = this.borderTags.get(tag);
		}
		return result;
	}
	
	
	public void setBorderTag(Tag tag, int i){
		if(segsLocked){
			return;
		}
		
		if( i < 0 || i>= this.getBorderLength()){
			throw new IllegalArgumentException("Index "+i+" is out of bounds : border length is "+this.getBorderLength());
		}
		
		try {
		
			// When moving the RP, move all segments to match
			if(tag.equals(Tag.REFERENCE_POINT)){
				ISegmentedProfile p = getProfile(ProfileType.ANGLE);
				int oldRP = getBorderIndex(tag);
				int diff  = i-oldRP;
				try {
					p.nudgeSegments(diff);
				} catch (ProfileException e) {
					warn("Cannot offset profile to "+tag);
					fine("Error moving segments", e);
					return;
				}
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

		} catch (UnavailableProfileTypeException e) {
			stack("Error getting angle profile ", e);
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
		
		try {

			int oldRP = getBorderIndex(Tag.REFERENCE_POINT);
			ISegmentedProfile p = getProfile(ProfileType.ANGLE);

			this.borderTags = tagMap;


			int newRP = getBorderIndex(Tag.REFERENCE_POINT);
			int diff  = newRP-oldRP;
			try {
				p.nudgeSegments(diff);
			} catch (ProfileException e) {
				warn("Cannot offset profile");
				fine("Error moving segments", e);
				return;
			}
			finest("Old RP at "+oldRP);
			finest("New RP at "+newRP);
			finest("Moving segments by"+diff);

			setProfile(ProfileType.ANGLE, p);



			int newOP = getBorderIndex(Tag.ORIENTATION_POINT);
			int intersectionIndex = this.getBorderIndex(this.findOppositeBorder( this.getBorderPoint(newOP) ));
			this.borderTags.put(Tag.INTERSECTION_POINT, intersectionIndex);

		} catch (UnavailableProfileTypeException e) {
			stack("Error getting angle profile ", e);
		}

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
		if(this.getBorderIndex(reference)>BORDER_INDEX_NOT_FOUND){
			int newIndex =  wrapIndex( index+this.getBorderIndex(reference) );
			
//			int newIndex =  AbstractCellularComponent.wrapIndex( index+this.getBorderIndex(reference) , this.getBorderLength() );
			return newIndex;
		}
		return BORDER_INDEX_NOT_FOUND;
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
	
	public double getWindowProportion(ProfileType type) {
		
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
			
			double perimeter = this.getStatistic(PlottableStatistic.PERIMETER);
			double angleWindow = perimeter * d;
			
			
			// calculate profiles
			this.angleProfileWindowSize = (int) Math.round(angleWindow);
			finest("Recalculating angle profile");
			ProfileCreator creator = new ProfileCreator(this);
			ISegmentedProfile profile = creator.createProfile(ProfileType.ANGLE);
			
			this.profileMap.put(ProfileType.ANGLE, profile);		
			
		}
	}
	
	
	public ISegmentedProfile getProfile(ProfileType type) throws UnavailableProfileTypeException {
		
		if( ! this.hasProfile(type)){
			throw new UnavailableProfileTypeException("Cannot get profile type "+type);
		}
		
		try {
			return new SegmentedFloatProfile(this.profileMap.get(type));
		} catch (java.lang.IndexOutOfBoundsException | ProfileException e) {
			stack("Error getting profile "+type, e);
			throw new UnavailableProfileTypeException("Cannot get profile type "+type, e);
		}

	}
	
	public boolean hasProfile(ProfileType type){
		return this.profileMap.containsKey(type);
	}


	public ISegmentedProfile getProfile(ProfileType type, Tag tag)
			throws ProfileException, UnavailableBorderTagException, UnavailableProfileTypeException{
		
		// fetch the index of the pointType (the new zero)
		if( ! this.hasBorderTag(tag)){
			throw new UnavailableBorderTagException("Tag "+tag+" not present");
		}
				
		int pointIndex = this.borderTags.get(tag);
					
		// offset the angle profile to start at the pointIndex
		return getProfile(type).offset(pointIndex);

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


	private double getPathLength(ProfileType type) {
		double pathLength = 0;

		try {

			IProfile profile = this.getProfile(type);

			// First previous point is the last point of the profile
			IPoint prevPoint = IPoint.makeNew(0,profile.get(this.getBorderLength()-1));

			for (int i=0; i<this.getBorderLength();i++ ) {
				double normalisedX = ((double)i/(double)this.getBorderLength())*100; // normalise to 100 length

				// We are measuring along the chart of angle vs position
				// Each median angle value is treated as an XYPoint
				IPoint thisPoint = IPoint.makeNew(normalisedX, profile.get(i));
				pathLength += thisPoint.getLengthTo(prevPoint);
				prevPoint = thisPoint;
			}
		}catch (UnavailableProfileTypeException e) {
			stack("Error getting angle profile ", e);
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
		

		// replace the tag positions also
		Set<Tag> keys = borderTags.keySet();
		for( Tag s : keys){
			int index = borderTags.get(s);
			int newIndex = this.getBorderLength() - index - 1; // if was 0, will now be <length-1>; if was length-1, will be 0
//			 update the bordertag map directly to avoid segmentation changes due to RP shift
			borderTags.put(s, newIndex);
		}
	}
	

	public IBorderPoint getNarrowestDiameterPoint() throws UnavailableBorderPointException {
				
		try {

			int index = this.getProfile(ProfileType.DIAMETER).getIndexOfMin();
			return IBorderPoint.makeNew(this.getBorderPoint(index));

		} catch (UnavailableProfileTypeException | ProfileException e) {
			stack("Error getting diameter profile minimum", e);
			throw new UnavailableBorderPointException("Error getting diameter profile minimum");
		}

	}
	
	public double getNarrowestDiameter() {
		try {
			return Arrays.stream(this.getProfile(ProfileType.DIAMETER).toDoubleArray()).min().orElse(0);
		} catch (UnavailableProfileTypeException e) {
			stack("Error getting diameter profile", e);
			return 0;
		}
	}
	
	/**
	 * Go around the border of the object, measuring the angle to the OP. 
	 * If the angle is closest to target angle, return the distance to the CoM.
	 * @param angle the target angle
	 * @return the distance from the closest border point at the requested angle to the CoM
	 */
	@Override
	public double getDistanceFromCoMToBorderAtAngle(double angle){

		double bestDiff = 180;
		double bestDistance = 180;

		for(int i=0;i<getBorderLength();i++){
			IPoint p = getBorderPoint(i);
			double distance = p.getLengthTo(getCentreOfMass());
			double pAngle = getCentreOfMass().findAngle( p, IPoint.makeNew(0,-10));
			if(p.getX()<0){
				pAngle = 360-pAngle;
			}

			if(Math.abs(angle-pAngle) < bestDiff){

				bestDiff = Math.abs(angle-pAngle);
				bestDistance = distance;
			}
		}
		return bestDistance;
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		
		in.defaultReadObject();
		
		// set transient fields
		double perimeter = this.getStatistic(PlottableStatistic.PERIMETER);
		double angleWindow = perimeter * angleWindowProportion;
		this.angleProfileWindowSize = (int) Math.round(angleWindow);

	}
	
//	public class IndexOutOfBoundsException extends Exception {
//		private static final long serialVersionUID = 1L;
//		public IndexOutOfBoundsException() { super(); }
//		public IndexOutOfBoundsException(String message) { super(message); }
//		public IndexOutOfBoundsException(String message, Throwable cause) { super(message, cause); }
//		public IndexOutOfBoundsException(Throwable cause) { super(cause); }
//	}
	
}
