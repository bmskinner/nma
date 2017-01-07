/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
/*
	-----------------------
	NUCLEUS CLASS
	-----------------------
	Contains the variables for storing a nucleus,
	plus the functions for calculating aggregate stats
	within a nucleus
*/  
package com.bmskinner.nuclear_morphology.components.nuclei;


import ij.gui.Roi;
import ij.process.FloatPolygon;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileCreator;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder;
import com.bmskinner.nuclear_morphology.analysis.profiles.Profileable;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalAnalyser;
import com.bmskinner.nuclear_morphology.components.AbstractCellularComponent;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.BorderTag;
import com.bmskinner.nuclear_morphology.components.generic.BorderTagObject;
import com.bmskinner.nuclear_morphology.components.generic.DefaultBorderPoint;
import com.bmskinner.nuclear_morphology.components.generic.DoubleEquation;
import com.bmskinner.nuclear_morphology.components.generic.LineEquation;
import com.bmskinner.nuclear_morphology.components.generic.FloatPoint;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.SegmentedFloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclear.SignalCollection;
import com.bmskinner.nuclear_morphology.components.rules.RuleSet;
import com.bmskinner.nuclear_morphology.components.stats.NucleusStatistic;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.components.stats.SignalStatistic;


/**
 * @author bms41
 *
 */
@Deprecated
public class RoundNucleus extends AbstractCellularComponent
	implements Nucleus {

	private static final long serialVersionUID = 1L;
		
	protected int nucleusNumber; // the number of the nucleus in the current image

	protected double angleWindowProportion; // The proportion of the perimeter to use for profiling
	protected int    angleProfileWindowSize; // the chosen window size for the nucleus based on proportion

	protected double pathLength;  // the angle profile path length - measures wibbliness in border
	
	protected Map<ProfileType, ISegmentedProfile> profileMap = new HashMap<ProfileType, ISegmentedProfile>();
	
	protected List<NucleusBorderSegment> segmentList = new ArrayList<NucleusBorderSegment>(0); // expansion for e.g acrosome

	protected Map<Tag, Integer>    borderTags  = new HashMap<Tag, Integer>(0); // to replace borderPointsOfInterest; <tag, index>
	
	protected Map<String, Integer>       segmentTags = new HashMap<String, Integer>(0);

	protected String outputFolder;  // the top-level path in which to store outputs; has analysis date e.g. /Testing/2015-11-24_10:00:00
	
	
	protected SignalCollection signalCollection = new SignalCollection();
	
	protected transient Nucleus verticalNucleus = null; // cache the vertically rotated nucleus
	
	
	private boolean segsLocked = false; // allow locking of segments and tags if manually assigned
	
	public RoundNucleus(Roi roi, File f, int channel, int number, int[] position){
		
		super(roi, f, channel, position);
		
		this.nucleusNumber   = number;
		
	}
	
	public RoundNucleus (Roi roi, File file, int number, int[] position) { // construct from an roi
		super(roi);
		if(file==null || Integer.valueOf(number)==null || position==null){
			throw new IllegalArgumentException("Nucleus constructor argument is null");
		}
		
		this.setSourceFile(file);
//		this.setPosition(position);
		this.nucleusNumber   = number;
	}
	
	/**
	 * Construct with an ROI, a source image and channel, and the original position in the source image
	 * @param roi
	 * @param f
	 * @param channel
	 * @param position
	 * @param centreOfMass
	 */
	public RoundNucleus(Roi roi, File f, int number, int[] position, IPoint centreOfMass){
		super(roi, f, 0, position, centreOfMass );
		this.nucleusNumber   = number;

	}

	public RoundNucleus(){
		super();
	}

	public RoundNucleus(Nucleus n) {
		super(n);

		if(n instanceof RoundNucleus){
			this.setOutputFolder( ((RoundNucleus)n).getOutputFolderName());
		}
		
		
				
		this.setNucleusNumber(n.getNucleusNumber());
				
		this.setSignals( new SignalCollection(n.getSignalCollection()));
		
		this.angleWindowProportion = n.getWindowProportion(ProfileType.ANGLE);
		this.angleProfileWindowSize = n.getWindowSize(ProfileType.ANGLE);
		
		
		for(ProfileType type : ProfileType.values()){

			try {
				this.profileMap.put(type, n.getProfile(type));
			} catch (UnavailableProfileTypeException e) {
				fine("Profile type "+type+" not present");
			}
			
		}


		this.setBorderTags(n.getBorderTags());
		
		this.segsLocked = n.isLocked();

	}
		
	public Nucleus duplicate(){
		try {

			RoundNucleus duplicate = new RoundNucleus(this);
			return duplicate;			
		} catch (Exception e) {
			return null;
		}
	}
	
	/*
	* Finds the key points of interest around the border
	* of the Nucleus. Can use several different methods, and 
	* take a best-fit, or just use one. The default in a round 
	* nucleus is to get the longest diameter and set this as
	*  the head/tail axis.
	*/
	public void findPointsAroundBorder(){
		
		try {
			RuleSet rpSet = RuleSet.roundRPRuleSet();
			IProfile p = this.getProfile(rpSet.getType());
			ProfileIndexFinder f = new ProfileIndexFinder();
			int rpIndex = f.identifyIndex(p, rpSet);



			setBorderTag(Tag.REFERENCE_POINT, rpIndex);		
			setBorderTag(Tag.ORIENTATION_POINT, rpIndex);

			if(!this.isProfileOrientationOK()){
				this.reverse();
			}  

		} catch(UnavailableProfileTypeException e){
			stack("Error getting profile type", e);
		}
		
	}
	

	public void initialise(double proportion) {

		this.angleWindowProportion = proportion;
		
		double perimeter = this.getStatistic(NucleusStatistic.PERIMETER);
		double angleWindow = perimeter * proportion;
		
		
		// calculate profiles
		this.angleProfileWindowSize = (int) Math.round(angleWindow);

		calculateProfiles();
		
		SignalAnalyser s = new SignalAnalyser();
		s.calculateSignalDistancesFromCoM(this);
		s.calculateFractionalSignalDistancesFromCoM(this);
		
	    
	}
	

	/*
		-----------------------
		Getters for basic values within nucleus
		-----------------------
	*/
	
	@Override
	public String getSourceFileNameWithoutExtension(){

		String trimmed = "";

		int i = this.getSourceFileName().lastIndexOf('.');
		if (i > 0) {
				trimmed   = this.getSourceFileName().substring(0,i);
		}
		return trimmed;
	}

	public String getOutputFolderName(){
		return this.outputFolder;
	}
			
	
	public File getOutputFolder(){
		return new File(this.getSourceFolder()+File.separator+this.outputFolder);
	}


	public String getPathWithoutExtension(){
		
//		String extension = "";
		String trimmed = "";

		int i = this.getSourceFileName().lastIndexOf('.');
		if (i > 0) {
//				extension = this.getPath().substring(i+1);
				trimmed = this.getSourceFileName().substring(0,i);
		}
		return trimmed;
	}  

	public int getNucleusNumber(){
		return this.nucleusNumber;
	}
	
	public String getNameAndNumber(){
		return this.getSourceFileName()+"-"+this.getNucleusNumber();
	}

	public String getPathAndNumber(){
		return this.getSourceFile()+File.separator+this.nucleusNumber;
	}


	public IBorderPoint getPoint(Tag tag){	
		int index = this.getBorderIndex(tag);
		return this.getBorderPoint(index);
	}
	
	@Override
	public void moveCentreOfMass(IPoint centreOfMass) {
		
		double xOffset = centreOfMass.getX() - getCentreOfMass().getX();
		double yOffset = centreOfMass.getY() - getCentreOfMass().getY();
		
		for(UUID id : signalCollection.getSignalGroupIDs()){
			
			signalCollection.getSignals(id).parallelStream().forEach( s -> {
				
//				log(this.getNameAndNumber()+": Offsetting signal - "+xOffset+", "+yOffset);
				
				s.offset(xOffset, yOffset);
			});

		}
		super.moveCentreOfMass(centreOfMass);
	}
	
	@Override
	protected double calculateStatistic(PlottableStatistic stat) {
		
		if(stat instanceof NucleusStatistic){
			return calculateStatistic( (NucleusStatistic) stat);
		} else {
			throw new IllegalArgumentException("Statistic type inappropriate for nucleus: "+stat.getClass().getName());
		}
		
	}

	protected double calculateStatistic(NucleusStatistic stat) {
		
//		finest("Calculating stat in round nucleus: "+stat);
		double result = 0;
		switch(stat){
		
		case AREA:
			result = this.getStatistic(stat);
			break;
		case ASPECT:
			result = this.getAspectRatio();
			break;
		case CIRCULARITY:
			result = this.getCircularity();
			break;
		case MAX_FERET:
			result = this.getStatistic(stat);
			break;
		case MIN_DIAMETER:
			result = this.getNarrowestDiameter();
			break;
		case PERIMETER:
			result = this.getStatistic(stat);
			break;
		case VARIABILITY:
			break;
		case BOUNDING_HEIGHT:
			result = this.getVerticallyRotatedNucleus().getBounds().getHeight();
			break;
		case BOUNDING_WIDTH:
			result = this.getVerticallyRotatedNucleus().getBounds().getWidth();
			break;
		case OP_RP_ANGLE:
			result = this.getCentreOfMass().findAngle(this.getBorderTag(Tag.REFERENCE_POINT), this.getBorderTag(Tag.ORIENTATION_POINT));
			break;
		default:
			break;
	
		}
//		finest("Calculated stat in round nucleus: "+stat);
		return result;
	}
	
			
	/**
	 * Find the bounding rectangle of the Nucleus. If the TopVertical and
	 * BottomVertical points have been set, these will be used. Otherwise,
	 * the given point is moved to directly below the CoM
	 * @param point the point to put at the bottom. Overridden if TOP_  and BOTTOM_ are set
	 * @return
	 * @throws Exception
	 */
	protected Rectangle calculateBoundingRectangle(Tag point) {
		ConsensusNucleus testw = new ConsensusNucleus( this, NucleusType.ROUND);

		if(this.hasBorderTag(Tag.TOP_VERTICAL) && this.hasBorderTag(Tag.BOTTOM_VERTICAL)){
			
			IBorderPoint[] points;
			try {
				points = getBorderPointsForVerticalAlignment();
				testw.alignPointsOnVertical(points[0], points[1] );
				
			} catch (UnavailableProfileTypeException e) {
				stack("Error getting vertical points", e);
				testw.rotatePointToBottom(testw.getBorderTag(point));
			}

			
			
		} else {
			testw.rotatePointToBottom(testw.getBorderTag(point));
		}

		
		FloatPolygon pw = testw.createPolygon();
		return pw.getBounds();
	}
	


			
	public double getCircularity() {
		double perim2 = Math.pow(this.getStatistic(NucleusStatistic.PERIMETER, MeasurementScale.PIXELS), 2);
		return (4 * Math.PI) * (this.getStatistic(NucleusStatistic.AREA, MeasurementScale.PIXELS) / perim2);
	}
	
	public double getAspectRatio() {
		try {
			double h = this.getVerticallyRotatedNucleus().getBounds().getHeight();
			double w = this.getVerticallyRotatedNucleus().getBounds().getWidth();
			
			return h / w;
			
//			return this.getBoundingRectangle(BorderTag.ORIENTATION_POINT).getHeight() / this.getBoundingRectangle(BorderTag.ORIENTATION_POINT).getWidth();
		} catch(Exception e){
			return 0;
		}
	}


	public void setOutputFolder(String f){
		this.outputFolder = f;
	}
	
	protected void setSignals(SignalCollection collection){
		this.signalCollection = collection;
	}


	protected void setNucleusNumber(int d){
		this.nucleusNumber = d;
	}


	public void setSegmentMap(Map<String, Integer> map){
		if(segsLocked){
			return;
		}
		this.segmentTags = map;
	}
		
	@Override
	public boolean isClockwiseRP(){
		return false;
	}

	
	public SignalCollection getSignalCollection(){
		return this.signalCollection;
	}


	public void updateSignalAngle(UUID channel, int signal, double angle){
		signalCollection.getSignals(channel).get(signal).setStatistic(SignalStatistic.ANGLE, angle);
	}

	// do not move this into SignalCollection - it is overridden in RodentSpermNucleus
	public void calculateSignalAnglesFromPoint(IBorderPoint p) {

		for( UUID signalGroup : signalCollection.getSignalGroupIDs()){
			
			if(signalCollection.hasSignal(signalGroup)){
				
			
				List<INuclearSignal> signals = signalCollection.getSignals(signalGroup);

				for(INuclearSignal s : signals){

					double angle = this.getCentreOfMass().findAngle(p, s.getCentreOfMass());
					s.setStatistic(SignalStatistic.ANGLE, angle);

				}
			}
		}
	}


	
	 /*
		Get a readout of the state of the nucleus
		Used only for debugging
	*/
	public String dumpInfo(int type){
		String result = "";
		result += "Dumping nucleus info: "+this.getNameAndNumber()+"\n";
		result += "    Border length: "+this.getBorderLength()+"\n";
		result += "    CoM: "+this.getCentreOfMass().toString()+"\n";
		if(type==ALL_POINTS || type==BORDER_POINTS){
			result += "    Border:\n";
			for(int i=0; i<this.getBorderLength(); i++){
				IBorderPoint p = this.getBorderPoint(i);
				result += "      Index "+i+": "+p.getX()+"\t"+p.getY()+"\t"+this.getBorderTag(i)+"\n";
			}
		}
		if(type==ALL_POINTS || type==BORDER_TAGS){
			result += "    Points of interest:\n";
			Map<Tag, Integer> pointHash = this.getBorderTags();

			for(Tag s : pointHash.keySet()){
			 IBorderPoint p = getBorderPoint(pointHash.get(s));
			 result += "    "+s+": "+p.getX()+"    "+p.getY()+" at index "+pointHash.get(s)+"\n";
			}
		}
		return result;
	}


	
	/**
	 * Checks if the smoothed array nuclear shape profile has the appropriate
	 * orientation.Counts the number of points above 180 degrees
	 * in each half of the array.
	 * @return 
	 * @throws Exception
	 */
	public boolean isProfileOrientationOK(){
		int frontPoints = 0;
		int rearPoints = 0;

		IProfile profile;
		try {
			profile = this.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
		} catch (UnavailableProfileTypeException e) {
			return false;
		}

		int midPoint = (int) (this.getBorderLength()/2) ;
		for(int i=0; i<this.getBorderLength();i++){ // integrate points over 180

			if(i<midPoint){
				frontPoints += profile.get(i);
			}
			if(i>midPoint){
				rearPoints  += profile.get(i);
			}
		}

		if(frontPoints > rearPoints){ // if the maxIndex is closer to the end than the beginning
			return true;
		} else{ 
			return false;
		}
	}

		
	public void updateSourceFolder(File newFolder) {
		File oldFile = this.getSourceFile();
		String oldName = oldFile.getName();
		File newFile = new File(newFolder+File.separator+oldName);
		if(newFile.exists()){
			this.setSourceFile(newFile);
//			this.setNucleusFolder(new File(this.getOutputFolder().getAbsolutePath()+File.separator+this.getImageNameWithoutExtension()));
		} else {
			throw new IllegalArgumentException("Cannot find file "+oldName+" in folder "+newFolder.getAbsolutePath());
		}
		
	}
		
	
	public void updateVerticallyRotatedNucleus(){
		this.verticalNucleus = null;
		this.getVerticallyRotatedNucleus();
	}
	
	public Nucleus getVerticallyRotatedNucleus(){
		if(verticalNucleus==null){
			verticalNucleus = this.duplicate();

			verticalNucleus.alignVertically();	
			
			// Ensure all vertical nuclei will share a common CoM
			verticalNucleus.moveCentreOfMass(IPoint.makeNew(0,0));
			this.setStatistic(NucleusStatistic.BOUNDING_HEIGHT, verticalNucleus.getBounds().getHeight());
			this.setStatistic(NucleusStatistic.BOUNDING_WIDTH,  verticalNucleus.getBounds().getWidth());
			
			double aspect = verticalNucleus.getBounds().getHeight() / verticalNucleus.getBounds().getWidth();
			this.setStatistic(NucleusStatistic.ASPECT,  aspect);			
			
		}
		return verticalNucleus;
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
	 * @throws UnavailableBorderTagException
	 */
	public void setProfile(ProfileType type, Tag tag, ISegmentedProfile p) throws UnavailableBorderTagException {
		
//		if(! this.hasBorderTag(tag)){
//			throw new UnavailableBorderTagException("Tag "+tag+" is not present");
//		}
		if(segsLocked){
			return;
		}
		
		// fetch the index of the pointType (the zero of the input profile)
		int pointIndex = this.borderTags.get(tag);
		
		// remove the offset from the profile, by setting the profile to start from the pointIndex
		try {
			this.setProfile(type, new SegmentedFloatProfile(p).offset(-pointIndex));
		} catch (ProfileException e) {
			warn("Cannot set profile");
			fine("Error setting profile", e);
		}
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
			
			try {
				ISegmentedProfile p = getProfile(ProfileType.ANGLE);
				int oldRP = getBorderIndex(tag);
				int diff  = i-oldRP;

				p.nudgeSegments(diff);
				finest("Old RP at "+oldRP);
				finest("New RP at "+i);
				finest("Moving segments by"+diff);

				setProfile(ProfileType.ANGLE, p);
			} catch (ProfileException | UnavailableProfileTypeException e) {
				warn("Cannot adjust segments");
				stack("Error moving segments", e);
			}
			
			
		}

		this.borderTags.put((BorderTagObject) tag, i);

		// The intersection point should always be opposite the orientation point
		if(tag.equals(Tag.ORIENTATION_POINT)){
			int intersectionIndex = this.getBorderIndex(this.findOppositeBorder( this.getBorderPoint(i) ));
			this.setBorderTag(Tag.INTERSECTION_POINT, intersectionIndex);
			updateVerticallyRotatedNucleus(); // force an update
		}
		
		if(tag.equals(Tag.TOP_VERTICAL) || tag.equals(Tag.BOTTOM_VERTICAL)){
			updateVerticallyRotatedNucleus();
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
		ISegmentedProfile p;
		try {
			p = getProfile(ProfileType.ANGLE);
		} catch (UnavailableProfileTypeException e1) {
			stack("Error getting angle profile", e1);
			return;
		}
		
		this.borderTags = tagMap;
		
		
		int newRP = getBorderIndex(Tag.REFERENCE_POINT);
		int diff  = newRP-oldRP;
		try {
			p.nudgeSegments(diff);
		} catch (ProfileException e) {
			warn("Cannot adjust segments");
			fine("Error moving segments", e);
		}
		finest("Old RP at "+oldRP);
		finest("New RP at "+newRP);
		finest("Moving segments by"+diff);
		setProfile(ProfileType.ANGLE, p);

		
		int newOP = getBorderIndex(Tag.ORIENTATION_POINT);
		int intersectionIndex = this.getBorderIndex(this.findOppositeBorder( this.getBorderPoint(newOP) ));
		this.borderTags.put(Tag.INTERSECTION_POINT, intersectionIndex);
		
		
		updateVerticallyRotatedNucleus();		
		
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
	
	
	public ISegmentedProfile getProfile(ProfileType type) throws UnavailableProfileTypeException {
		if(this.hasProfile(type)){
			try {
				return new SegmentedFloatProfile(this.profileMap.get(type));
			} catch (IndexOutOfBoundsException | ProfileException e) {
				stack("Error getting profile "+type, e);
				throw new UnavailableProfileTypeException("Error getting profile "+type);
			}
		} else {
			throw new IllegalArgumentException("Profile type "+type+" is not found in this nucleus");
		}
	}
	
	public boolean hasProfile(ProfileType type){
		return this.profileMap.containsKey(type);
	}


	public ISegmentedProfile getProfile(ProfileType type, Tag tag) throws UnavailableProfileTypeException{
		
		
		// fetch the index of the pointType (the new zero)
		int pointIndex = this.borderTags.get(tag);
		
		ISegmentedProfile profile = null;
		if(this.hasProfile(type)){
			
			// offset the angle profile to start at the pointIndex
			try {
				profile =  new SegmentedFloatProfile(this.getProfile(type).offset(pointIndex));
			} catch (ProfileException e) {
				warn("Error making offset profile");
				fine("Error making profile", e);
			}
			
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

		IProfile profile;
		try {
			profile = this.getProfile(type);
		} catch (UnavailableProfileTypeException e) {
			return 0;
		}
		
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
		return pathLength;
	}

	public void reverse(){
		if(segsLocked){
			return;
		}
		for(ProfileType type : profileMap.keySet()){

			ISegmentedProfile profile = profileMap.get(type);
			profile.reverse();
			profileMap.put(type, profile);
		}
		
		List<IBorderPoint> reversed = new ArrayList<IBorderPoint>(0);
		for(int i=this.getBorderLength()-1; i>=0;i--){
			reversed.add(this.getBorderPoint(i));
		}
		this.setBorderList(reversed);

		// replace the tag positions also
		Set<Tag> keys = borderTags.keySet();
		for( Tag s : keys){
			int index = borderTags.get(s);
			int newIndex = this.getBorderLength() - index - 1; // if was 0, will now be <length-1>; if was length-1, will be 0
//			 update the bordertag map directly to avoid segmentation changes due to RP shift
			borderTags.put(s, newIndex);
//			setBorderTag(s, newIndex);
		}
	}
	

	public IBorderPoint getNarrowestDiameterPoint() {

		int index = 0;
		try {
			index = this.getProfile(ProfileType.DIAMETER).getIndexOfMin();
		} catch (UnavailableProfileTypeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new DefaultBorderPoint(this.getBorderPoint(index));
	}
	
	public double getNarrowestDiameter() {
		try {
			return Arrays.stream(this.getProfile(ProfileType.DIAMETER).asArray()).min().orElse(0);
		} catch (UnavailableProfileTypeException e) {
			return 0;
		}

	}

	

	
	/*
	 * #############################################
	 * Methods implementing the Rotatable interface
	 * #############################################
	 */
	
	
	@Override
	public void alignVertically(){
		
		boolean useTVandBV = true;
		
		if(this.hasBorderTag(Tag.TOP_VERTICAL) && this.hasBorderTag(Tag.BOTTOM_VERTICAL)){
			
			int topPoint    = getBorderIndex(Tag.TOP_VERTICAL);
			int bottomPoint = getBorderIndex(Tag.BOTTOM_VERTICAL);
			
			if( topPoint == -1){ // check if the point was set but not found
				useTVandBV = false;
			}
			
			if( bottomPoint == -1){
				useTVandBV = false;
			}
			
			if(topPoint==bottomPoint){ // Situation when something went very wrong
				useTVandBV = false;
			}

		} else {
			
			useTVandBV = false;

		}
		
		
		
		
		if(useTVandBV){
			IBorderPoint[] points;
			try {
				points = getBorderPointsForVerticalAlignment();
				alignPointsOnVertical(points[0], points[1] );
			} catch (UnavailableProfileTypeException e) {
				rotatePointToBottom(getBorderPoint(Tag.ORIENTATION_POINT));
			}
			
		} else {
			
			// Default if top and bottom vertical points have not been specified
			rotatePointToBottom(getBorderPoint(Tag.ORIENTATION_POINT));
		}
		
	}
	
	/**
	 * Detect the points that can be used for vertical alignment.These are based on the
	 * BorderTags TOP_VERTICAL and BOTTOM_VETICAL. The actual points returned are not
	 * necessarily on the border of the nucleus; a bibble correction is performed on the
	 * line drawn between the two border points, minimising the sum-of-squares to each border
	 * point within the region covered by the line. 
	 * @return
	 * @throws UnavailableProfileTypeException 
	 */	
	private IBorderPoint[] getBorderPointsForVerticalAlignment() throws UnavailableProfileTypeException{
		
		
		IBorderPoint topPoint    = this.getBorderTag(Tag.TOP_VERTICAL);
		IBorderPoint bottomPoint = this.getBorderTag(Tag.BOTTOM_VERTICAL);
		
		
		// Find the border points between the top and bottom verticals
		List<IBorderPoint> pointsInRegion = new ArrayList<IBorderPoint>();
		
		int topIndex  = this.getBorderIndex(Tag.TOP_VERTICAL);
		int btmIndex  = this.getBorderIndex(Tag.BOTTOM_VERTICAL);
		int totalSize = this.getProfile(ProfileType.ANGLE).size();
		
		// A segment has built in methods for iterating through just the points it contains
		IBorderSegment region = IBorderSegment.newSegment(topIndex, btmIndex, totalSize );

		int index = topIndex;
		
		Iterator<Integer> it = region.iterator();
		
		while(it.hasNext()){
			index = it.next();
			pointsInRegion.add(this.getBorderPoint(index));
		}
		
		// As an anti-bibble defence, get a best fit line acrosss the region
		// Use the line of best fit to find appropriate top and bottom vertical points
		LineEquation eq = DoubleEquation.calculateBestFitLine(pointsInRegion);
		
		
		// Take values along the best fit line that are close to the original TV and BV
		
		// What about when the TV or BV are in the bibble? TODO
		
		IBorderPoint top = new DefaultBorderPoint(topPoint.getX(), eq.getY(topPoint.getX()));
		IBorderPoint btm = new DefaultBorderPoint(eq.getX(bottomPoint.getY()), bottomPoint.getY());
		
		return new IBorderPoint[] {top, btm};
		
	}
	
	
		
	@Override
	public void rotate(double angle){
		
		super.rotate(angle);
		
		if(angle!=0){
			
//			log(this.getNameAndNumber()+": Rotating signals");
			
			for(UUID id : signalCollection.getSignalGroupIDs()){
				
				signalCollection.getSignals(id).parallelStream().forEach( s -> {
					
					s.rotate(angle);
										
					// get the new signal centre of mass based on the nucleus rotation
					IPoint p = getPositionAfterRotation(s.getCentreOfMass(), angle);
//					
					s.moveCentreOfMass(p);					
				});
								
			}
		}
	}
	
	
	
	/*
	 * #############################################
	 * Object methods
	 * #############################################
	 */
	
	
		
	/**
	 * Describes the nucleus state
	 * @return
	 */
	public String toString(){
		String newLine = System.getProperty("line.separator");
		StringBuilder b = new StringBuilder();
		
		b.append(this.getNameAndNumber()+newLine);
		
		b.append(this.getSignalCollection().toString()+newLine);
		  
		return b.toString();
	}
	

	@Override
	public boolean equals(CellularComponent c) {
		
		if(this==c){
			return true;
		}
		
		if(c==null){
			return false;
		}
		
		if(this.getClass()!=c.getClass()){
			return false;
		}
		
		if( ! this.getID().equals(c.getID())){
			return false;
		}

		return true;
			
		
	}
	
	@Override
	public int compareTo(Nucleus n) {

		int number  = this.getNucleusNumber();
		String name = this.getSourceFileNameWithoutExtension();
		
		// Compare on image name.
		// If that is equal, compare on nucleus number

		int byName = name.compareTo(n.getSourceFileNameWithoutExtension());
		
		if(byName==0){
			
			if(number < n.getNucleusNumber()){
				return -1;
			} else if(number > n.getNucleusNumber()){
				return 1;
			} else {
				return 0;
			}

		} else {
			return byName;
		}

	}
		

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + angleProfileWindowSize;
		long temp;
		temp = Double.doubleToLongBits(angleWindowProportion);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result
				+ ((borderTags == null) ? 0 : borderTags.hashCode());
		result = prime * result + nucleusNumber;
		result = prime * result
				+ ((outputFolder == null) ? 0 : outputFolder.hashCode());
		temp = Double.doubleToLongBits(pathLength);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		
		
		result = prime * result
				+ ((profileMap == null) ? 0 : profileMap.hashCode());
		
		
		// Check the segmented profiles
		for(Map.Entry<ProfileType, ISegmentedProfile> entry : profileMap.entrySet()){

			result = prime * result
					+ ((entry == null) ? 0 : entry.getValue().hashCode());
		}
		
		
		result = prime * result
				+ ((segmentList == null) ? 0 : segmentList.hashCode());
		result = prime * result
				+ ((segmentTags == null) ? 0 : segmentTags.hashCode());
		result = prime
				* result
				+ ((signalCollection == null) ? 0 : signalCollection.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RoundNucleus other = (RoundNucleus) obj;
		if (angleProfileWindowSize != other.angleProfileWindowSize)
			return false;
		if (Double.doubleToLongBits(angleWindowProportion) != Double
				.doubleToLongBits(other.angleWindowProportion))
			return false;
		if (borderTags == null) {
			if (other.borderTags != null)
				return false;
		} else if (!borderTags.equals(other.borderTags))
			return false;
		if (nucleusNumber != other.nucleusNumber)
			return false;
		if (outputFolder == null) {
			if (other.outputFolder != null)
				return false;
		} else if (!outputFolder.equals(other.outputFolder))
			return false;
		if (Double.doubleToLongBits(pathLength) != Double
				.doubleToLongBits(other.pathLength))
			return false;
		if (profileMap == null) {
			if (other.profileMap != null)
				return false;
		} else if (!profileMap.equals(other.profileMap))
			return false;
		if (segmentList == null) {
			if (other.segmentList != null)
				return false;
		} else if (!segmentList.equals(other.segmentList))
			return false;
		if (segmentTags == null) {
			if (other.segmentTags != null)
				return false;
		} else if (!segmentTags.equals(other.segmentTags))
			return false;
		if (signalCollection == null) {
			if (other.signalCollection != null)
				return false;
		} else if (!signalCollection.equals(other.signalCollection))
			return false;
		return true;
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
//		finest("\tWriting nucleus");
		out.defaultWriteObject();
//		finest("\tWrote nucleus");
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
//		finest("\tReading nucleus");
		
		in.defaultReadObject();

		Map<Tag, Integer> newCache = new HashMap<Tag, Integer>(0);

		Iterator<?> it = borderTags.keySet().iterator();

		while(it.hasNext()){
			Object tag = it.next();
			if(tag instanceof BorderTag){
				fine("Deserialization has no BorderTagObject for "+tag.toString()+", creating");

				newCache.put(new BorderTagObject( (BorderTag) tag), borderTags.get(tag));						
			}

		}


		if( ! newCache.isEmpty()){
			borderTags = newCache;
		}
		
				
	    this.verticalNucleus    = null;
	}

	@Override
	public boolean isSmoothByDefault() {
		return true;
	}
	
}