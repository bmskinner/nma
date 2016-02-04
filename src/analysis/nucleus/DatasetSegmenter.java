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
package analysis.nucleus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import analysis.ProfileManager;
import components.CellCollection;
import components.generic.BooleanProfile;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileCollection;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.generic.BorderTag.BorderTagType;
import components.nuclear.NucleusBorderSegment;
import components.nuclear.NucleusType;
import components.nuclei.Nucleus;
import components.nuclei.sperm.RodentSpermNucleus;
import utility.Constants;

/**
 * This is the core of the morphology analysis pipeline.
 * This segments median profiles, and applies the segments to
 * nuclei.
 */
public class DatasetSegmenter extends AnalysisWorker {

    private CellCollection sourceCollection = null; // a collection to take segments from

    private MorphologyAnalysisMode mode = MorphologyAnalysisMode.NEW;

    public enum MorphologyAnalysisMode {
    	NEW, COPY, REFRESH
    }

    
    /*
      //////////////////////////////////////////////////
      Constructors
      //////////////////////////////////////////////////
    */
    
    /**
     * Segment a dataset with the given mode
     * @param dataset
     * @param mode
     * @param programLogger
     */
    public DatasetSegmenter(AnalysisDataset dataset, MorphologyAnalysisMode mode, Logger programLogger){
    	super(dataset, programLogger);
    	this.mode = mode;
    }
    
    /**
     * Copy the segmentation pattern from the source collection onto the given dataset
     * @param dataset
     * @param source
     * @param programLogger
     */
    public DatasetSegmenter(AnalysisDataset dataset, CellCollection source, Logger programLogger){
    	this(dataset, MorphologyAnalysisMode.COPY, programLogger);
    	this.sourceCollection = source;
    }
    
    /*
      //////////////////////////////////////////////////
      SwingWorker methods
      //////////////////////////////////////////////////
     */
        
    @Override
    protected Boolean doInBackground() throws Exception {
    	
    	boolean result = true;
		try{
			
			switch(mode){
				case COPY:
					result = runCopyAnalysis();
					break;
				case NEW:
					result = runNewAnalysis();
					break;
				case REFRESH:
					result = runRefreshAnalysis();
					break;
				default:
					result = false;
					break;
			}
			
			// Ensure that all hook-hump assignments are correct, and
			// top and bottom border points are set for rodent sperm
			giveAFinalPolish();
			
		} catch(Exception e){
			result = false;
			logError("Error in segmentation analysis", e);
		}

		return result;

	}
    
    private void giveAFinalPolish() throws Exception{
    	
    	if(this.getDataset().getCollection().getNucleusType().equals(NucleusType.RODENT_SPERM)){
    		if(! getDataset().getCollection()
    				.getProfileCollection(ProfileType.REGULAR)
    				.hasBorderTag(BorderTag.TOP_VERTICAL)  ){

    			log(Level.FINE, "TOP_ and BOTTOM_VERTICAL not assigned; calculating");
    			calculateTopAndBottomVerticals(getDataset());
    			log(Level.FINE, "Calculating TOP and BOTTOM for child datasets");
    			for(AnalysisDataset child : getDataset().getAllChildDatasets()){
    				calculateTopAndBottomVerticals(child);
    			}

    		}

    		log(Level.FINE, "Updating dataset hook-hump split and signals");
    		updateRodentSpermHookHumpSplits(this.getDataset());
    		for(AnalysisDataset child : this.getDataset().getAllChildDatasets()){
    			updateRodentSpermHookHumpSplits(child);
    		}
    	}
    }
    
	private void calculateTopAndBottomVerticals(AnalysisDataset dataset) throws Exception {
		
		log(Level.FINE, "Detecting flat region");
		DatasetProfiler.TailFinder.assignTopAndBottomVerticalInMouse(dataset.getCollection());
		
		log(Level.FINE, "Assigning flat region to nuclei");
		DatasetProfiler.Offsetter.assignFlatRegionToMouseNuclei(dataset.getCollection());
	}
    
	/**
	 * Recalculate the hook-hunp split, and signal angle measurements for the 
	 * given dataset of rodent sperm nuclei
	 * @param d
	 * @throws Exception
	 */
	private void updateRodentSpermHookHumpSplits(AnalysisDataset d) throws Exception{
		
		if(d.getCollection().getNucleusType().equals(NucleusType.RODENT_SPERM)){
			for(Nucleus n : d.getCollection().getNuclei()){

				RodentSpermNucleus nucleus = (RodentSpermNucleus) n;
				// recalculate - old datasets have problems
				nucleus.splitNucleusToHeadAndHump();

				// recalculate signal angles - old datasets have problems
				nucleus.calculateSignalAnglesFromPoint(nucleus.getPoint(BorderTag.ORIENTATION_POINT));
			}
		}
		
	}
    
    private boolean runNewAnalysis() throws Exception {
    	log(Level.FINE, "Beginning core morphology analysis");

		this.setProgressTotal(getDataset().getCollection().getNucleusCount()*2);


		BorderTag pointType = BorderTag.REFERENCE_POINT;

		// segment the profiles from head
		runSegmentation(getDataset().getCollection(), pointType);

		log(Level.FINE, "Core morphology analysis complete");
		return true;
    }
    
    private boolean runCopyAnalysis() throws Exception{
    	if(sourceCollection==null){
			log(Level.WARNING,  "Cannot copy: source collection is null");
			return false;
		} else {
			this.setProgressTotal(getDataset().getCollection().getNucleusCount());
			log(Level.FINE,  "Copying segmentation pattern");
			reapplyProfiles(getDataset().getCollection(), sourceCollection);
			log(Level.FINE, "Copying complete");
			return true;
		}
    }
    
    private boolean runRefreshAnalysis() throws Exception{
    	log(Level.FINE, "Refreshing segmentation");
    	this.setProgressTotal(getDataset().getCollection().getNucleusCount()*3);
		refresh(getDataset().getCollection());
		log(Level.FINE, "Refresh complete");
		return true;
    }
    
    
    /*
    //////////////////////////////////////////////////
    Analysis methods
    //////////////////////////////////////////////////
   */
		
	/**
	 * When a population needs to be reanalysed do not offset nuclei or recalculate best fits;
	 * just get the new median profile 
	 * @param collection the collection of nuclei
	 * @param sourceCollection the collection with segments to copy
	 */
	public boolean reapplyProfiles(CellCollection collection, CellCollection sourceCollection) throws Exception {
		
		log(Level.FINE, "Applying existing segmentation profile to population...");
		
		
		sourceCollection.getProfileManager().copyCollectionOffsets(collection);
		
		// At this point the collection has only a regular profile collections.
		// No Frankenprofile has been copied.

		reviseSegments(collection, BorderTag.REFERENCE_POINT);	


		log(Level.FINE, "Re-profiling complete");
		return true;
	}
	
	/**
	 * Refresh the given collection. Create a new profile aggregate, and recalculate
	 * the FrankenProfiles. Do not recalculate segments for the median profile. Adjust
	 * individual nucleus segments to the median.
	 * @param collection
	 * @return
	 */
	public boolean refresh(CellCollection collection) throws Exception {

		log(Level.FINE, "Refreshing mophology");

		BorderTag pointType = BorderTag.REFERENCE_POINT;

		// get the empty profile collection from the new CellCollection
		ProfileCollection pc = collection.getProfileCollection(ProfileType.REGULAR);
		int previousLength = pc.length();

		// make an aggregate from the nuclei. A new median profile must necessarily result.
		// By default, the aggregates are created from the reference point
		// Ensure that the aggregate created has the same length as the original, otherwise
		// segments will not fit
		pc.createProfileAggregate(collection, ProfileType.REGULAR,  previousLength);

		List<NucleusBorderSegment> segments = pc.getSegments(BorderTag.REFERENCE_POINT);

		// make a new profile collection to hold the frankendata
		ProfileCollection frankenCollection = new ProfileCollection();

		// add the correct offset keys
		// These are the same as the profile collection keys, and have
		// the same positions (since a franken profile is based on the median)
		// The reference point is at index 0
		for(BorderTag key : pc.getOffsetKeys()){
			frankenCollection.addOffset(key, pc.getOffset(key));
		}

		// At this point, the FrankenCollection is identical to the ProfileCollection
		// We need to add the individual recombined frankenProfiles


		// make a segment fitter to do the recombination of profiles
		SegmentFitter fitter = new SegmentFitter(pc.getSegmentedProfile(pointType));


		for(Nucleus n : collection.getNuclei()){ 
			// recombine the segments at the lengths of the median profile segments
			Profile recombinedProfile = fitter.recombine(n, BorderTag.REFERENCE_POINT);
			n.setProfile(ProfileType.FRANKEN, new SegmentedProfile(recombinedProfile));

			publish(progressCount++);

		}
		
		fitter = null;

		// add all the nucleus frankenprofiles to the frankencollection
		frankenCollection.createProfileAggregate(collection, ProfileType.FRANKEN, (int)pc.getAggregate().length());

		// update the profile aggregate
//		frankenCollection.createProfileAggregateFromInternalProfiles((int)pc.getAggregate().length());
		log(Level.FINER, "FrankenProfile generated");

		// copy the segments from the profile collection
		log(Level.FINER, "Copying profile collection segments to frankenCollection");
		frankenCollection.addSegments(pointType, segments);
		
		// copy the segments from the profile collection to other profiles
		log(Level.FINER, "Copying profile collection segments to distance profile");
		collection.getProfileCollection(ProfileType.DISTANCE).addSegments(pointType, segments);
		
		log(Level.FINER, "Copying profile collection segments to single distance profile");
		collection.getProfileCollection(ProfileType.SINGLE_DISTANCE).addSegments(pointType, segments);
		
		// attach the frankencollection to the cellcollection
		collection.setProfileCollection(ProfileType.FRANKEN, frankenCollection);



		// Ensure each nucleus gets the new profile pattern
		assignSegments(collection);
		reviseSegments(collection, pointType);
		
		// Unlock all the segments
		collection.getProfileManager().setLockOnAllNucleusSegments(false);
			
		return true;
	}
		
	/**
	 * Run the segmentation part of the analysis. 
	 * @param collection
	 * @param pointType
	 */
	private void runSegmentation(CellCollection collection, BorderTag pointType) throws Exception {

		log(Level.FINE, "Beginning segmentation...");
	
			
			// generate segments in the median profile
			log(Level.FINE, "Creating segments...");
			createSegments(collection);
			
			// map the segments from the median directly onto the nuclei
			log(Level.FINE, "Assigning segments...");
			assignSegments(collection);
			
			// adjust the segments to better fit each nucleus
			log(Level.FINE, "Revising segments...");
			reviseSegments(collection, pointType);		
	
			// update the aggregate in case any borders have changed
			collection.getProfileCollection(ProfileType.REGULAR).createProfileAggregate(collection, ProfileType.REGULAR);
						
			// At this point, the franken collection still contains tip/head values only
			
		
		log(Level.FINE, "Segmentation complete");
	}
	
	/**
	 * Run the segmenter on the median profile for the given point type
	 * @param collection
	 */
	private void createSegments(CellCollection collection) throws Exception {

		ProfileCollection pc = collection.getProfileCollection(ProfileType.REGULAR);

		// the reference point is always index 0, so the segments will match
		// the profile
		Profile median = pc.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);

		ProfileSegmenter segmenter = new ProfileSegmenter(median);		

		List<NucleusBorderSegment> segments = segmenter.segment();

		log(Level.FINER, "Found "+segments.size()+" segments in "+collection.getPoint(BorderTag.REFERENCE_POINT)+" profile");

		segmenter = null; // clean up
				
		
		/* If there are round nuclei in the collection, and only two segments
		 * were called, the orientation point may not have been
		 * associated with the segment breakpoint. 
		 * Update the segment position to the OP
		 */
		
		if(collection.getNucleusType().equals(NucleusType.ROUND) && segments.size()==2){

			log(Level.FINER, "Updating segment pattern to match orientation point in round nuclei");

			int medianOrientationIndex = pc.getOffset(BorderTag.ORIENTATION_POINT);
			
			log(Level.FINER, "Orientation point is at index "+medianOrientationIndex);
						
			NucleusBorderSegment firstSegment = segments.get(0);
			firstSegment.update(firstSegment.getStartIndex(), medianOrientationIndex);
		}
		
		pc.addSegments(BorderTag.REFERENCE_POINT, segments);
		
	}

	/**
	 * From the calculated median profile segments, assign segments to each nucleus
	 * based on the best offset fit of the start and end indexes 
	 * @param collection
	 * @param pointType
	 */
	private void assignSegments(CellCollection collection){

		try{
			log(Level.FINER, "Assigning segments to nuclei...");

			ProfileCollection pc = collection.getProfileCollection(ProfileType.REGULAR);

			// find the corresponding point in each Nucleus
			SegmentedProfile median = pc.getSegmentedProfile(BorderTag.REFERENCE_POINT);

			for(Nucleus n : collection.getNuclei()){
				assignSegmentsToNucleus(n, median);
				publish(progressCount++);
			}
			log(Level.FINER, "Segments assigned to nuclei");
		} catch(Exception e){
			logError("Error assigning segments", e);
		}
	}
	
	/**
	 * Assign the given segments to the nucleus, finding the best match of the nucleus
	 * profile to the median profile
	 * @param n the nucleus to segment
	 * @param median the segmented median profile
	 */
	private void assignSegmentsToNucleus(Nucleus n, SegmentedProfile median) throws Exception {
			
		// remove any existing segments in the nucleus
		SegmentedProfile nucleusProfile = n.getProfile(ProfileType.REGULAR);
		nucleusProfile.clearSegments();

		List<NucleusBorderSegment> nucleusSegments = new ArrayList<NucleusBorderSegment>();

		// go through each segment defined for the median curve
		NucleusBorderSegment prevSeg = null;

		for(NucleusBorderSegment segment : median.getSegments()){

			// get the positions the segment begins and ends in the median profile
			int startIndexInMedian 	= segment.getStartIndex();
			int endIndexInMedian 	= segment.getEndIndex();

			// find the positions these correspond to in the offset profiles

			// get the median profile, indexed to the start or end point
			Profile startOffsetMedian 	= median.offset(startIndexInMedian);
			Profile endOffsetMedian 	= median.offset(endIndexInMedian);

			// find the index at the point of the best fit
			int startIndex 	= n.getProfile(ProfileType.REGULAR).getSlidingWindowOffset(startOffsetMedian);
			int endIndex 	= n.getProfile(ProfileType.REGULAR).getSlidingWindowOffset(endOffsetMedian);

			// create a segment at these points
			// ensure that the segment meets length requirements
			NucleusBorderSegment seg = new NucleusBorderSegment(startIndex, endIndex, n.getBorderLength(), segment.getID());
			if(prevSeg != null){
				seg.setPrevSegment(prevSeg);
				prevSeg.setNextSegment(seg);
			}

//			seg.setName(segment.getName());
			nucleusSegments.add(seg);

			prevSeg = seg;
		}

		NucleusBorderSegment.linkSegments(nucleusSegments);
		nucleusProfile.setSegments(nucleusSegments);
		n.setProfile(ProfileType.REGULAR, nucleusProfile);

		
	}

	/**
	 * Update initial segment assignments by stretching each segment to the best possible fit along
	 * the median profile 
	 * @param collection
	 * @param pointType
	 */
	private void reviseSegments(CellCollection collection, BorderTag pointType) throws Exception {
		log(Level.FINER, "Refining segment assignments...");

			ProfileCollection pc = collection.getProfileCollection(ProfileType.REGULAR);
//			fileLogger.log(Level.FINE, "Median profile");
//			fileLogger.log(Level.FINE, pc.toString());
			List<NucleusBorderSegment> segments = pc.getSegments(pointType);
//			fileLogger.log(Level.FINE, "Fetched segments from profile collection");
//			fileLogger.log(Level.FINE, NucleusBorderSegment.toString(segments));

			// make a new profile collection to hold the frankendata
			ProfileCollection frankenCollection = new ProfileCollection();

			/*
			   The border tags for the frankenCollection are the same as the profile 
			   collection keys, and have the same positions (since a franken profile
			   is based on the median). The reference point is at index 0. 
			 */
			for(BorderTag key : pc.getOffsetKeys()){
				
				int offset = pc.getOffset(key);
				log(Level.FINEST, "Adding franken collection offset "+offset+" for "+ key);
				frankenCollection.addOffset(key, pc.getOffset(key));
			}

			
			/*
				At this point, the FrankenCollection is identical to the ProfileCollection, 
				but has no ProfileAggregate.
				We need to add the individual recombined frankenProfiles to the internal profile list,
				and build a ProfileAggregate
			 */

			// Get the median profile for the population
			SegmentedProfile medianProfile = pc.getSegmentedProfile(pointType);
			log(Level.FINER, "Median profile: angle at index 0 for "+BorderTag.REFERENCE_POINT+" is "+medianProfile.get(0));

			
			/*
			 * At this point, the median profile has the reference point at index 0
			 */
			
			SegmentFitter fitter = new SegmentFitter(medianProfile);
//			List<Profile> frankenProfiles = new ArrayList<Profile>(0);

			int count = 1;
			for(Nucleus n : collection.getNuclei()){ 
				log(Level.FINER, "Fitting nucleus "+n.getPathAndNumber()+" ("+count+" of "+collection.cellCount()+")");
				fitter.fit(n, pc);

				// recombine the segments at the lengths of the median profile segments
				Profile recombinedProfile = fitter.recombine(n, BorderTag.REFERENCE_POINT);
				try{
					n.setProfile(ProfileType.FRANKEN, new SegmentedProfile(recombinedProfile));
				} catch(Exception e){
					log(Level.SEVERE, recombinedProfile.toString());
					logError("Error setting nucleus profile", e);
					throw new Exception("Error setting nucleus profile");
				}
				
//				frankenProfiles.add(recombinedProfile);
				count++;
				publish(progressCount++); // publish the progress to gui
			}

			fitter = null; // clean up
			/*
			 * Build a profile aggregate in the new frankencollection by taking the
			 * stored frankenprofiles from each nucleus in the collection
			 */
			try {
			frankenCollection.createProfileAggregate(collection, ProfileType.FRANKEN, (int)pc.getAggregate().length());
			} catch(Exception e){
				logError("Error creating profile aggregate", e);
				throw new Exception("Error creating profile aggregate");
			}
			// add all the nucleus frankenprofiles to the frankencollection
//			frankenCollection.addNucleusProfiles(frankenProfiles);

			// update the profile aggregate
//			frankenCollection.createProfileAggregateFromInternalProfiles((int)pc.getAggregate().length());
			
			/*
			 * At this point, the frankencollection is indexed on the reference point.
			 * This is because the reference point is used to generate the profile collection.
			 * Copy the segments from the profile collection, starting from the reference point
			 */
			frankenCollection.addSegments(pointType, segments);

			double firstPoint = frankenCollection.getSegmentedProfile(BorderTag.REFERENCE_POINT).get(0);
			log(Level.FINER, "FrankenProfile generated: angle at index 0 for "+BorderTag.REFERENCE_POINT+" is "+firstPoint);
			
			// attach the frankencollection to the cellcollection
			collection.setProfileCollection(ProfileType.FRANKEN, frankenCollection);
			log(Level.FINER, "Segment assignments refined");
			
			// copy the segments from the profile collection to other profiles
			log(Level.FINER, "Copying profile collection segments to distance profile");
			collection.getProfileCollection(ProfileType.DISTANCE).addSegments(pointType, segments);
			
			log(Level.FINER, "Copying profile collection segments to single distance profile");
			collection.getProfileCollection(ProfileType.SINGLE_DISTANCE).addSegments(pointType, segments);
	}
	
	public class SegmentFitter {

		/**
		 * The multiplier to add to best-fit scores when shrinking a segment below the 
		 * minimum segment size specified in ProfileSegmenter
		 */
		static final double PENALTY_SHRINK = 1.5;
		
		/**
		 * The multiplier to add to best-fit scores when shrinking a segment above the 
		 * median segment size
		 */
		static final double PENALTY_GROW   = 20;
		
		private final SegmentedProfile medianProfile; // the profile to align against
		
		private boolean optimise = false; // a flag to enable test optimisations.
		
		// This holds tested profiles so that their scores do not have to be recalculated
		private List<SegmentedProfile> testedProfiles = new ArrayList<SegmentedProfile>();
		
		/**
		 * The number of points ahead and behind to test
		 * when creating new segment profiles
		 */
//		private static final int POINTS_TO_TEST = 20;

		/**
		 * Construct with a median profile containing segments. The originals will not be modified
		 * @param medianProfile the profile
		 * @param logFile the file for logging status
		 * @throws Exception 
		 */
		public SegmentFitter(final SegmentedProfile medianProfile) throws Exception{
			if(medianProfile==null){
				log(Level.SEVERE, "Segmented profile is null");
				throw new IllegalArgumentException("Median profile is null");
			}

			this.medianProfile  = new SegmentedProfile(medianProfile);
		}
		
		/**
		 * Run the segment fitter on the given nucleus. It will take the segments
		 * loaded into the fitter upon cosntruction, and apply them to the nucleus
		 * angle profile.
		 * @param n the nucleus to fit to the current median profile
		 * @param pc the ProfileCollection from the CellCollection the nucleus belongs to
		 */
		public void fit(final Nucleus n, final ProfileCollection pc){
			
			// Input checks
			if(n==null){
				log(Level.SEVERE, "Test nucleus is null");
				throw new IllegalArgumentException("Test nucleus is null");
			}
			
			try {
				if(n.getProfile(ProfileType.REGULAR).getSegments()==null || n.getProfile(ProfileType.REGULAR).getSegments().isEmpty()){
					log(Level.SEVERE, "Nucleus has no segments");
					throw new IllegalArgumentException("Nucleus has no segments");
				}
			} catch (Exception e1) {
				logError("Error getting segments", e1);
			}
			
			long startTime = System.currentTimeMillis();
//			 Begin fitting the segments to the median

			try{
				
				// get the best fit of segments to the median
				SegmentedProfile newProfile = this.runFitter(n.getProfile(ProfileType.REGULAR));

				n.setProfile(ProfileType.REGULAR, newProfile);
				
				// modify tail and head/tip point to nearest segment end
				remapBorderPoints(n, pc);
				
				log(Level.FINE, "Fitted nucleus "+n.getPathAndNumber());
				

				long endTime = System.currentTimeMillis();
				long time = endTime - startTime;
				log(Level.FINEST, "Fitting took "+time+" milliseconds");

				
			} catch(Exception e){
				logError("Error refitting segments", e);
			}
		}
			
		/**
		 * Join the segments within the given nucleus into Frankenstein's Profile. 
		 * @param n the nucleus to recombine
		 * @param tag the BorderTag to start from
		 * @return a profile
		 */
		public Profile recombine(Nucleus n, BorderTag tag){
			if(n==null){
				log(Level.WARNING, "Recombined nucleus is null");
				throw new IllegalArgumentException("Test nucleus is null");
			}

			SegmentedProfile frankenProfile = null;
			try {
				if(n.getProfile(ProfileType.REGULAR).hasSegments()){

					/*
					 * Generate a segmented profile from the angle profile of the point type.
					 * The zero index of the profile is the border tag. The segment list for the profile
					 * begins with seg 0 at the border tag.
					 */
					
					SegmentedProfile nucleusProfile = new SegmentedProfile(n.getProfile(ProfileType.REGULAR, tag));
					
					
					log(Level.FINEST, "    Segmentation beginning from "+tag);
					log(Level.FINEST, "    The border tag "+tag+" in this nucleus is at raw index "+n.getBorderIndex(tag));
					log(Level.FINEST, "    Angle at incoming segmented profile index 0 ("+tag+") is "+nucleusProfile.get(0));

					// stretch or squeeze the segments to match the length of the median profile of the collection
					//			frankenProfile = recombineSegments(n, nucleusProfile, tag);
					frankenProfile = nucleusProfile.frankenNormaliseToProfile(medianProfile);
					
					log(Level.FINEST, "Angle at median profile index 0 ("+tag+") is "+medianProfile.get(0));
					
					log(Level.FINEST, "Angle at franken profile index 0 ("+tag+") is "+frankenProfile.get(0));
					
				} else {
					log(Level.WARNING, "Nucleus has no segments");
					throw new IllegalArgumentException("Nucleus has no segments");
				}
			} catch(Exception e){
				logError("Error recombining segments", e);
			}

			
			
			return frankenProfile;
		}
		
		/**
		 * Move core border points within a nucleus to the end of their appropriate segment
		 * based on the median profile segmentation pattern
		 * @param n the nucleus to fit
		 * @param pc the profile collection from the CellCollection
		 */
		private void remapBorderPoints(Nucleus n, ProfileCollection pc) throws Exception {
			
			if(pc==null){
				log(Level.WARNING, "No profile collection found, skipping remapping");
				return; // this allows the unit tests to skip this section if a profile collection has not been created
			}
			
			/*
			 * Not all the tags will be associated with endpoints;
			 * e.g. the intersection point. The orientation and 
			 * reference points should be updated though - members of
			 * the core border tag population
			 */
			
			for(BorderTag tag : BorderTag.values(BorderTagType.CORE)){
				
				// get the segments the point should lie between
				// from the median profile
				
				/*
				 * The goal is to move the index of the border tag to the start index
				 * of the relevant segment.
				 * 
				 * Select the segments from the median profile, offset to begin from the tag.
				 * The relevant segment has a start index of 0
				 * Find the name of this segment, and adjust it's start position in the
				 * individual nucleus profile.
				 */		
				NucleusBorderSegment seg = pc.getSegmentStartingWith(tag);
				List<NucleusBorderSegment> segments = pc.getSegments(tag);
							
				if(seg!=null){
					// Get the same segment in the nucleus, and move the tag to the segment start point
					NucleusBorderSegment nSeg = n.getProfile(ProfileType.REGULAR).getSegment(seg.getName());
					n.setBorderTag(tag, nSeg.getStartIndex());
					log(Level.FINE, "Remapped border point '"+tag+"' to "+nSeg.getStartIndex());
				} else {
									
					// A segment was not found with a start index at zero; segName is null
					log(Level.WARNING, "Border tag '"+tag+"' not found in median profile");
					log(Level.WARNING, "No segment with start index zero in median profile");
					log(Level.WARNING, "Median profile:");
					log(Level.WARNING, pc.toString());
					log(Level.WARNING, "Median segment list:");
					log(Level.WARNING, NucleusBorderSegment.toString(segments));
					
					// Check to see if the segments are reversed
					seg = pc.getSegmentEndingWith(tag);
					if(seg!=null){
						log(Level.WARNING, "Found segment "+seg.getName()+" ending with tag "+tag);
					} else {
						log(Level.WARNING, "No segments end with tag "+tag);
					}
					
				}
			}
		}
			
		/**
		 * 
		 * @param profile the profile to fit against the median profile
		 * @return a profile with best-fit segmentation to the median
		 * @throws Exception 
		 */
		private SegmentedProfile runFitter(SegmentedProfile profile) throws Exception {
			// Input check
			if(profile==null){
				log(Level.SEVERE, "Profile is null");
				throw new IllegalArgumentException("Profile is null in runFitter()");
			}
			
			testedProfiles = new ArrayList<SegmentedProfile>();

			log(Level.FINE, "Fitting segments");
			
			// By default, return the input profile
			SegmentedProfile result 	 = new SegmentedProfile(profile);

			// A new list to hold the fitted segments
			SegmentedProfile tempProfile = new SegmentedProfile(profile);
			
			// fit each segment independently
			List<UUID> idList = medianProfile.getSegmentIDs();
			
			for(UUID id : idList){
			
//			for(String name : tempProfile.getSegmentNames()){
				
				// get the current segment
				NucleusBorderSegment segment = tempProfile.getSegment(id);
//				NucleusBorderSegment segment = tempProfile.getSegment(name);
				
				if( ! segment.isStartPositionLocked()){ //only run the test if this segment is unlocked
				
					// get the initial score for the segment and log it
					double score = compareSegmentationPatterns(medianProfile, tempProfile);
					log(Level.FINE, segment.toString());
					log(Level.FINE, "\tInitial score: "+score);

					// find the best length and offset change
					// apply them to the profile
					tempProfile = testLength(tempProfile, id);

					// copy the best fit profile to the result
					result 	 = new SegmentedProfile(tempProfile);		
				}
			}
		
			
			return result;
		}
		
		/**
		 * Test the effect of changing length on the given segment from the list
		 * @param list
		 * @param segmnetNumber the segment to test
		 * @return
		 */
		private SegmentedProfile testLength(SegmentedProfile profile, UUID id) throws Exception {
			
			// by default, return the same profile that came in
			SegmentedProfile result = new SegmentedProfile(profile);
					
			
			// the segment in the input profile to work on
			NucleusBorderSegment segment = profile.getSegment(id);
			
			
			// Get the initial score to beat
			double bestScore = compareSegmentationPatterns(medianProfile, profile);

			
			// the most extreme negative offset to apply to the end of this segment
			// without making the length invalid
			int minimumChange = 0 - (segment.length() - NucleusBorderSegment.MINIMUM_SEGMENT_LENGTH);
			
			// the maximum length offset to apply
			// we can't go beyond the end of the next segment anyway, so use that as the cutoff
			// how far from current end to next segment end?
			int maximumChange = segment.testLength(segment.getEndIndex(), segment.nextSegment().getEndIndex());	

			
			/* Trying all possible lengths takes a long time. Try adjusting lengths in a window of 
			 * <changeWindowSize>, and finding the window with the best fit. Then drop down to individual
			 * index changes to refine the match 
			 */
			int bestChangeWindow = 0;
			int changeWindowSize = 10;
			for(int changeWindow = minimumChange; changeWindow < maximumChange; changeWindow+=changeWindowSize){

				// find the changeWindow with the best fit, 
				// apply all changes to a fresh copy of the profile
				SegmentedProfile testProfile = new SegmentedProfile(profile);

				testProfile = testChange(profile, id, changeWindow);
				double score = compareSegmentationPatterns(medianProfile, testProfile);
				if(score < bestScore){
					bestChangeWindow = changeWindow;
				}
			}
			

			
			int halfWindow = changeWindowSize / 2;
			// now we have the best window,  drop down to a changeValue
			for(int changeValue = bestChangeWindow - halfWindow; changeValue < bestChangeWindow+halfWindow; changeValue++){
				SegmentedProfile testProfile = new SegmentedProfile(profile);

				testProfile = testChange(profile, id, changeValue);
				double score = compareSegmentationPatterns(medianProfile, testProfile);
				if(score < bestScore){
					result = testProfile;
				}
			}
			
			return result;
		}
			
		private SegmentedProfile testChange(SegmentedProfile profile, UUID id, int changeValue) throws Exception {
			
			double bestScore = compareSegmentationPatterns(medianProfile, profile);
			
			// apply all changes to a fresh copy of the profile
			SegmentedProfile result = new SegmentedProfile(profile);
			SegmentedProfile testProfile = new SegmentedProfile(profile);
			NucleusBorderSegment segment = profile.getSegment(id);
//			if(debug){
//				fileLogger.log(Level.FINE, "\tTesting length change "+changeValue);
//			}
			
			// not permitted if it violates length constraint
			if(testProfile.adjustSegmentStart(id, changeValue)){
				
				// testProfile should now contain updated segment endpoints
				SegmentedProfile compareProfile = new SegmentedProfile(testProfile);
				
////					 if this pattern has been seen, skip the rest of the test
//				if(optimise){
//					if(hasBeenTested(compareProfile)){
//						if(debug){
//							fileLogger.log("\tProfile has been tested");
//						}
//						continue;
//					}
//				}
					
				// anything that gets in here should be valid
				try{
					double score = compareSegmentationPatterns(medianProfile, testProfile);
//					if(debug){
//						fileLogger.log(Level.FINE, "\tLengthen "+changeValue+":\tScore:\t"+score);
//					}
					
					if(score < bestScore){
						bestScore 	= score;
						result = new SegmentedProfile(testProfile);
//						if(debug){
//							fileLogger.log(Level.FINE, "\tNew best score:\t"+score+"\tLengthen:\t"+changeValue);
//						}
					}
				}catch(IllegalArgumentException e){
					// throw a new edxception rather than trying a nudge a problem profile
					fileLogger.log(Level.SEVERE, e.getMessage());
					throw new Exception("Error getting segmentation pattern: "+e.getMessage());
				}
				
				
				// test if nudging the lengthened segment with will help
				int nudge = testNudge(testProfile, segment.length());
				testProfile.nudgeSegments(nudge);

				double score = compareSegmentationPatterns(medianProfile, testProfile);
				if(score < bestScore){
					bestScore = score;
					result = new SegmentedProfile(testProfile);
//					if(debug){
//						fileLogger.log(Level.FINE, "\tNew best score:\t"+score+"\tNudge:\t"+nudge);
//					}
				}
				if(optimise){
					testedProfiles.add(compareProfile);
				}
				
										
			} else {
//				if(debug){
//					fileLogger.log(Level.FINE, "\tLengthen "+changeValue
//						+":\tInvalid length change:\t"
//						+testProfile.getSegment(name).getLastFailReason()
//						+"\t"+segment.toString());
//				}
			}
			return result;
		}
		
		/**
		 * Check if the given profile has already been tested
		 * @param test
		 * @return
		 */
		private boolean hasBeenTested(SegmentedProfile test){
			if(testedProfiles.contains(test)){
				return true;
			} else {
				return false;
			}
		}
		
		/**
		 * Find the nudge to the given list of segments that gives the best
		 * fit to the median profile
		 * @param list the segment list
		 * @param length the length to cycle through. Use the segment length for simple measure
		 * @return the best nudge value to use
		 * @throws Exception when the segmentation comparison fails
		 */
		private int testNudge(SegmentedProfile profile, int length) throws Exception {
			
//			int totalLength = list.get(0).getTotalLength();
			double score 		= 0;
			double bestScore 	= 0;
			int bestNudge 		= 0;
					
			for( int nudge = -length; nudge<length; nudge++){
				SegmentedProfile newProfile = new SegmentedProfile(profile);
				newProfile.nudgeSegments(nudge);
				
				if(optimise){
					if(hasBeenTested(newProfile)){
						continue;
					}
				}
				
				try{
					score = compareSegmentationPatterns(medianProfile, newProfile);
//					fileLogger.log("\tNudge "+nudge+":\tScore:\t"+score, fileLogger.DEBUG);
					
				}catch(IllegalArgumentException e){
					logError("Nudge error getting segmentation pattern: ", e);
					throw new Exception("Nudge error getting segmentation pattern");
				}
				
				if(score < bestScore){
					bestScore = score;
					bestNudge = nudge;
				}
				if(optimise){
					testedProfiles.add(newProfile);
				}	
			}
			return bestNudge;
		}
					
		/**
		 * Get the sum-of-squares difference betweene two segments in the given profile
		 * @param name the name of the segment
		 * @param referenceProfile the profile to measure against
		 * @param testProfile the profile to measure
		 * @return the sum of square differences between the segments
		 */
		private double compareSegments(String name, SegmentedProfile referenceProfile, SegmentedProfile testProfile){
			if(name == null){
				throw new IllegalArgumentException("Segment name is null");
			}
			
			NucleusBorderSegment reference  = referenceProfile.getSegment(name);
			NucleusBorderSegment test		=      testProfile.getSegment(name);

			Profile refProfile  = referenceProfile.getSubregion(reference);
			Profile subjProfile =      testProfile.getSubregion(test);
			
			return refProfile.absoluteSquareDifference(subjProfile);
		}
		
		/**
		 * Get the score for an entire segment list of a profile. Tests the effect of  changing one segment
		 * on the entire set
		 * @param reference
		 * @param test
		 * @return the score
		 */
		private double  compareSegmentationPatterns(SegmentedProfile referenceProfile, SegmentedProfile testProfile){
			
			if(referenceProfile.getSegmentCount()!=testProfile.getSegmentCount()){
				throw new IllegalArgumentException("Lists are of different lengths");
			}
			
			double result = 0;
			for(String name : referenceProfile.getSegmentNames()){
				result += compareSegments(name, referenceProfile, testProfile);
			}
			return result;
		}
	}
	
	/**
	 * Divide a profile into segments of interest based on
	 * minima and maxima.
	 */
	public class ProfileSegmenter {
		
		/**
		 * The smallest number of points a segment can contain. 
		 * Increasing this value will make the segment fitting more robust, 
		 * but reduces resolution
		 */
		public static final int MIN_SEGMENT_SIZE = 10;
		
		private static final int SMOOTH_WINDOW	 = 2; // the window size for smoothing profiles
		private static final int MAXIMA_WINDOW	 = 5; // the window size for calculating minima and maxima
		private static final int DELTA_WINDOW	 = 2; // the window size for calculating deltas
		private static final int ANGLE_THRESHOLD = 180; // a maximum must be above this, a minimum below it
		
		private static final double MIN_RATE_OF_CHANGE = 0.02; // a potential inflection cannot vary by more than this
		
		
		private final Profile profile; // the profile to segment
		private final List<NucleusBorderSegment> segments = new ArrayList<NucleusBorderSegment>(0);
		
		private BooleanProfile inflectionPoints = null;
		private Profile        deltaProfile     = null;
		private double         minRateOfChange  = 1;
			
		/**
		 * Constructed from a profile
		 * @param p
		 */
		public ProfileSegmenter(final Profile p){
			if(p==null){
				throw new IllegalArgumentException("Profile is null");
			}
			this.profile = p;

			this.initialise();
			
			log(Level.FINE, "Created profile segmenter");
		}
		
		private void initialise(){

			/*
			 * Find minima and maxima, to set the inflection points in the profile
			 */
			BooleanProfile maxima = this.profile.smooth(SMOOTH_WINDOW).getLocalMaxima(MAXIMA_WINDOW, ANGLE_THRESHOLD);
			BooleanProfile minima = this.profile.smooth(SMOOTH_WINDOW).getLocalMinima(MAXIMA_WINDOW, ANGLE_THRESHOLD);
			inflectionPoints = minima.or(maxima);
			
			/*
			 * Find second derivative rates of change
			 */
			Profile deltas  = this.profile.smooth(SMOOTH_WINDOW).calculateDeltas(DELTA_WINDOW); // minima and maxima should be near 0 
			deltaProfile =       deltas.smooth(SMOOTH_WINDOW).calculateDeltas(DELTA_WINDOW); // second differential
			
			/*
			 * Find levels of variation within the complete profile
			 */
			double dMax = deltaProfile.getMax();
			double dMin = deltaProfile.getMin();
			double variationRange = Math.abs(dMax - dMin);
			
			minRateOfChange = variationRange * MIN_RATE_OF_CHANGE;
			
		}
		
		/**
		 * Get the deltas and find minima and maxima. These switch between segments
		 * @param splitIndex an index point that must be segmented on
		 * @return a list of segments
		 */
		public List<NucleusBorderSegment> segment(){

			/*
			 * Prepare segment start index
			 */
			int segmentStart = 0;

					
			try{
				
				/*
				 * Iterate through the profile, looking for breakpoints
				 * The reference point is at index 0, as defined by the 
				 * calling method.
				 * 
				 *  Therefore, a segment should always be started at index 0
				 */
				for(int index=0; index<profile.size(); index++){
					
					if(testSegmentEndFound(index, segmentStart)){
						
						// we've hit a new segment
						NucleusBorderSegment seg = new NucleusBorderSegment(segmentStart, index, profile.size());

						segments.add(seg);
						
						log(Level.FINE, "New segment found in profile: "+seg.toString());
						
						segmentStart = index; // Prepare for the next segment
					}
					
				}
				
				/*
				 * Now, there is a list of segments which covers most but not all of 
				 * the profile. The final segment has not been defined: is is the current segment
				 * start, but has not had an endpoint added. Since segment ends cannot be
				 * called within MIN_SIZE of the profile end, there is enough space to make a segment
				 * running from the current segment start back to index 0
				 */
				NucleusBorderSegment seg = new NucleusBorderSegment(segmentStart, 0, profile.size());
				segments.add(seg);
				
				
//				/*
//				 * End of the profile; call a new segment boundary to avoid
//				 * linking across the reference point
//				 * 
//				 * If a boundary is already called at index 0 in the first segment,
//				 * do not create a new segment, as it would have 1 length
//				 * 
//				 * If a boundary is already called at the last index in the profile,
//				 *  do not add a terminal segment, and just allow merging
//				 */
//				int firstSegmentEndIndex = segments.get(0).getEndIndex();
//				int lastSegmentEndIndex =  segments.get(segments.size()-1).getEndIndex();
//				int lastProfileIndex = profile.size()-1;
//				
//				log(Level.FINE, "First segment end: " + firstSegmentEndIndex + " of "+lastProfileIndex);
//				log(Level.FINE, "Final segment end: " + lastSegmentEndIndex  + " of "+lastProfileIndex);
//				
//                if(  (segments.get(0).getEndIndex()!=0) && segments.get(segments.size()-1).getEndIndex() != profile.size()-1 ) {
//
//				
//                	/*
//                	 * What happens in round nuclei, when the segmentation detects a boundary in the index
//                	 * dirctly before 0?
//                	 * 
//                	 * A new segment is attempted to be created with length 1, and of course fails.
//                	 * Catch the exception, and attempty to extend the final segment up to index 0
//                	 */
//
//                	try {
//					
//                		NucleusBorderSegment seg = new NucleusBorderSegment(segmentStart, segmentEnd, profile.size());
//                		seg.setName("Seg_"+segCount);
//    					segments.add(seg);
//    					log(Level.FINE, "Terminal segment found: "+seg.toString());
//    					
//                	} catch(IllegalArgumentException e){
//                		
//                		log(Level.WARNING, "Error creating segment, likely it was too short");
//                		log(Level.WARNING, "Attempting to extend the final segment into a terminal segment");
//                		NucleusBorderSegment finalSegment = segments.get(segments.size()-1);
//                		finalSegment.update(finalSegment.getStartIndex(), 0);;
//                	}
//                	
//					
//					
//				} else {
//					// the first segment is not larger than the minimum size
//					// We need to merge the first and last segments
//					
//					log(Level.FINE, "Terminal segment not needed: first segment has index 0 or last has full index");
//					
////					log(Level.FINE, " Updating the final segment to end at index zero");
////					segments.get(segments.size()-1).update(segments.get(segments.size()-1).getStartIndex(), 0);
//				}
				
				NucleusBorderSegment.linkSegments(segments);
				
				log(Level.FINE, "Segments linked");
				for(NucleusBorderSegment s : segments){
					log(Level.FINE, s.toString());
				}
				

			} catch (Exception e){
				logError( "Error in segmentation", e);
			}
			
			return segments;
		}
		

		/**
		 * @param index the current index being tested
		 * @param segmentStart the start of the current segment being built
		 * @return
		 * @throws Exception 
		 */
		private boolean testSegmentEndFound(int index, int segmentStart) throws Exception{

			/*
			 * The first segment must meet the length limit
			 */
			if(index < MIN_SEGMENT_SIZE ){
				return false;
			}
			

			/*
			 * Segment must be long enough
			 */
			int potentialSegLength = index-segmentStart;
			
			if(potentialSegLength < MIN_SEGMENT_SIZE ){
				return false;
			}
			
			/*
			 * Once the index has got close to the end of the profile, a new segmnet cannot be called,
			 * even at a really nice infection point, because it would be too close to the 
			 * reference point
			 */
			if( index >  profile.size() - MIN_SEGMENT_SIZE ){
				return false;
			}
			
			/*
			 * All length and position checks are passed.
			 * Now test for a good inflection point.
			 * 
			 * We want a minima or maxima, and the value
			 * must be distinct from its surroundings.	
			 */

			if( (   inflectionPoints.get(index)==true 
					&& Math.abs(deltaProfile.get(index)) > minRateOfChange
					&& potentialSegLength >= MIN_SEGMENT_SIZE)){
				
				return true;

			}
			return false;
		}

		
		
		
		/**
		 * For debugging. Print the details of each segment found 
		 */
		public void print(){
			for(NucleusBorderSegment s : segments){
				s.print();
			}
		}
	}
}
