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
import java.util.logging.Level;
import java.util.logging.Logger;

import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import analysis.ProgressEvent;
import analysis.ProgressListener;
import components.CellCollection;
import components.generic.BooleanProfile;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileCollection;
import components.generic.ProfileType;
import components.generic.SegmentedProfile;
import components.nuclear.NucleusBorderSegment;
import components.nuclear.NucleusType;
import components.nuclei.Nucleus;
import utility.Constants;

/**
 * This is the core of the morphology analysis pipeline.
 * This segments median profiles, and applies the segments to
 * nuclei.
 */
public class DatasetSegmenter extends AnalysisWorker implements ProgressListener {

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
//			giveAFinalPolish();
			
		} catch(Exception e){
			result = false;
			logError("Error in segmentation analysis", e);
		}

		return result;

	}
    
//    private void giveAFinalPolish() throws Exception{
//    	
//    	if(this.getDataset().getCollection().getNucleusType().equals(NucleusType.RODENT_SPERM)){
//    		if(! getDataset().getCollection()
//    				.getProfileCollection(ProfileType.REGULAR)
//    				.hasBorderTag(BorderTag.TOP_VERTICAL)  ){
//
//    			log(Level.FINE, "TOP_ and BOTTOM_VERTICAL not assigned; calculating");
//    			calculateTopAndBottomVerticals(getDataset());
//    			log(Level.FINE, "Calculating TOP and BOTTOM for child datasets");
//    			for(AnalysisDataset child : getDataset().getAllChildDatasets()){
//    				calculateTopAndBottomVerticals(child);
//    			}
//
//    		}
//
//    		log(Level.FINE, "Updating dataset hook-hump split and signals");
//    		updateRodentSpermHookHumpSplits(this.getDataset());
//    		for(AnalysisDataset child : this.getDataset().getAllChildDatasets()){
//    			updateRodentSpermHookHumpSplits(child);
//    		}
//    	}
//    }
    
//	private void calculateTopAndBottomVerticals(AnalysisDataset dataset) throws Exception {
//		
//		log(Level.FINE, "Detecting flat region");
//		DatasetProfiler.TailFinder.assignTopAndBottomVerticalInMouse(dataset.getCollection());
//		
//		log(Level.FINE, "Assigning flat region to nuclei");
//		DatasetProfiler.Offsetter.assignFlatRegionToMouseNuclei(dataset.getCollection());
//	}
    
	/**
	 * Recalculate the hook-hunp split, and signal angle measurements for the 
	 * given dataset of rodent sperm nuclei
	 * @param d
	 * @throws Exception
	 */
//	private void updateRodentSpermHookHumpSplits(AnalysisDataset d) throws Exception{
//		
//		if(d.getCollection().getNucleusType().equals(NucleusType.RODENT_SPERM)){
//			for(Nucleus n : d.getCollection().getNuclei()){
//
//				RodentSpermNucleus nucleus = (RodentSpermNucleus) n;
//				// recalculate - old datasets have problems
//				nucleus.splitNucleusToHeadAndHump();
//
//				// recalculate signal angles - old datasets have problems
//				nucleus.calculateSignalAnglesFromPoint(nucleus.getPoint(BorderTag.ORIENTATION_POINT));
//			}
//		}
//		
//	}
    
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
//		assignSegments(collection);

		// find the corresponding point in each Nucleus
		SegmentedProfile median = pc.getSegmentedProfile(BorderTag.REFERENCE_POINT);
		
		SegmentAssignmentTask task = new SegmentAssignmentTask(median, collection.getNuclei().toArray(new Nucleus[0]));
		task.addProgressListener(this);
//		task.invoke();
		mainPool.invoke(task);
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
			ProfileCollection pc = collection.getProfileCollection(ProfileType.REGULAR);

			// find the corresponding point in each Nucleus
			SegmentedProfile median = pc.getSegmentedProfile(BorderTag.REFERENCE_POINT);
//			assignSegments(collection);
			SegmentAssignmentTask task = new SegmentAssignmentTask(median, collection.getNuclei().toArray(new Nucleus[0]));
			task.addProgressListener(this);
			task.invoke();
			
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
			 * Split the recombining task into chunks for multithreading
			 */
			
			SegmentRecombiningTask task = new SegmentRecombiningTask(medianProfile, pc, collection.getNuclei().toArray(new Nucleus[0]));
			task.addProgressListener(this);
//			task.invoke();
			mainPool.invoke(task);

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

	@Override
	public void progressEventReceived(ProgressEvent event) {
		publish(++progressCount);
		
	}
}
