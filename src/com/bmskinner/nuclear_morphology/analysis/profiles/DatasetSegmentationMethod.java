package com.bmskinner.nuclear_morphology.analysis.profiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;

import com.bmskinner.nuclear_morphology.analysis.AbstractAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.ProgressEvent;
import com.bmskinner.nuclear_morphology.analysis.ProgressListener;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.stats.Quartile;

public class DatasetSegmentationMethod extends AbstractAnalysisMethod implements ProgressListener {
	
	private ICellCollection sourceCollection = null; // a collection to take segments from

    private MorphologyAnalysisMode mode = MorphologyAnalysisMode.NEW;

    public enum MorphologyAnalysisMode {
    	NEW, COPY, REFRESH
    }
    
    /**
     * Segment a dataset with the given mode
     * @param dataset
     * @param mode
     * @param programLogger
     */
    public DatasetSegmentationMethod(IAnalysisDataset dataset, MorphologyAnalysisMode mode){
    	super(dataset);
    	this.mode = mode;
    }
    
    /**
     * Copy the segmentation pattern from the source collection onto the given dataset
     * @param dataset
     * @param source
     * @param programLogger
     */
    public DatasetSegmentationMethod(IAnalysisDataset dataset, ICellCollection source){
    	this(dataset, MorphologyAnalysisMode.COPY);
    	this.sourceCollection = source;
    }
    
    @Override
	public IAnalysisResult call() throws Exception {
    	
    	
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
					result = null;
					break;
			}
			

			// Ensure segments are copied appropriately to verticals
			// Ensure hook statistics are generated appropriately
//			log("Updating verticals");
			for(Nucleus n : dataset.getCollection().getNuclei()){
//				log("Updating "+n.getNameAndNumber());
				n.updateVerticallyRotatedNucleus();
				n.updateDependentStats();
				
			}
			fine("Updated verticals and stats");
			
		} catch(Exception e){
			result = null;
			stack("Error in segmentation analysis", e);
		}

		return result;

	}
        
    /**
     * This has to do everything: segment the median, appy the segments
     * to nuclei, generate the frankenmedian, and adjust the nuclear segments
     * based on the frankenprofiles.
     * @return
     * @throws Exception
     */
    private IAnalysisResult runNewAnalysis() throws Exception {
    	fine("Beginning core morphology analysis");

		Tag pointType = Tag.REFERENCE_POINT;

		// segment the profiles from head
		runSegmentation(dataset.getCollection(), pointType);

		fine("Core morphology analysis complete");
		
		return new DefaultAnalysisResult(dataset);
    }
    
    private IAnalysisResult runCopyAnalysis() throws Exception{
    	if(sourceCollection==null){
			warn("Cannot copy: source collection is null");
			return null;
		} else {

			fine( "Copying segmentation pattern");
			reapplyProfiles(dataset.getCollection(), sourceCollection);
			fine("Copying complete");
			return new DefaultAnalysisResult(dataset);
		}
    }
    
    private IAnalysisResult runRefreshAnalysis() throws Exception{
    	fine("Refreshing segmentation");

		refresh(dataset.getCollection());
		fine("Refresh complete");
		return new DefaultAnalysisResult(dataset);
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
	public boolean reapplyProfiles(ICellCollection collection, ICellCollection sourceCollection) throws Exception {
		
		fine("Applying existing segmentation profile to population");
		
		
		sourceCollection.getProfileManager().copyCollectionOffsets(collection);
		
		// At this point the collection has only a regular profile collections.
		// No Frankenprofile has been copied.

		reviseSegments(collection, Tag.REFERENCE_POINT);	


		fine("Re-profiling complete");
		return true;
	}
	
	
	/**
	 * Refresh the given collection. Assumes that the segmentation in the 
	 * profile collection is correct, and updates all nuclei to match
	 * @param collection
	 * @return
	 */
	public boolean refresh(ICellCollection collection) throws Exception {

		fine("Refreshing mophology");

		Tag pointType = Tag.REFERENCE_POINT;
		
		assignMedianSegmentsToNuclei(collection, pointType);
		
		return true;

//		// get the empty profile collection from the new CellCollection
//		IProfileCollection pc = collection.getProfileCollection();
//		int previousLength = pc.length();
//
//		// make an aggregate from the nuclei. A new median profile must necessarily result.
//		// By default, the aggregates are created from the reference point
//		// Ensure that the aggregate created has the same length as the original, otherwise
//		// segments will not fit
//		pc.createProfileAggregate(collection, previousLength);
//
////		List<IBorderSegment> segments = pc.getSegments(Tag.REFERENCE_POINT);
//
////		// make a new profile collection to hold the frankendata
////		IProfileCollection frankenCollection = createFrankenMedian(collection);
//
//		// At this point, the FrankenCollection is identical to the ProfileCollection
//		// We need to add the individual recombined frankenProfiles
//
//		// Ensure that the median profile segment IDs are propogated to the nuclei
//		assignMedianSegmentsToNuclei(collection, pointType);
//
//		// make a segment fitter to do the recombination of profiles
//		finest("Creating segment fitter");
//		SegmentFitter fitter = new SegmentFitter(pc.getSegmentedProfile(ProfileType.ANGLE, pointType, Quartile.MEDIAN));
//
//
//		for(Nucleus n : collection.getNuclei()){ 
//			// recombine the segments at the lengths of the median profile segments
//			IProfile recombinedProfile = fitter.recombine(n, Tag.REFERENCE_POINT);
//			n.setProfile(ProfileType.FRANKEN, new SegmentedFloatProfile(recombinedProfile));
//
//			publish(progressCount++);
//
//		}
//		
//		fitter = null;
//		finest("Segment fitting complete");
//		
//		// copy the segments from the profile collection to other profiles
//		finer("Copying profile collection segments to distance profile");
////		collection.getProfileCollection(ProfileType.DIAMETER).addSegments(pointType, segments);
//		
//		finer("Copying profile collection segments to single distance profile");
////		collection.getProfileCollection(ProfileType.RADIUS).addSegments(pointType, segments);
//		
//		// attach the frankencollection to the cellcollection
////		collection.setProfileCollection(ProfileType.FRANKEN, frankenCollection);
//
//
//
//		// Ensure each nucleus gets the new profile pattern
////		assignMedianSegmentsToNuclei(collection);
//
//		// find the corresponding point in each Nucleus
//		ISegmentedProfile median = pc.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN);
//		
//		finer("Creating segmnent assignment task");
//		SegmentAssignmentTask task = new SegmentAssignmentTask(median, collection.getNuclei().toArray(new Nucleus[0]));
//		task.addProgressListener(this);
////		task.invoke();
//		mainPool.invoke(task);
//		reviseSegments(collection, pointType);
//		
//		finer("Segments revised for nuclei");
//		
//		// Unlock all the segments
//		collection.getProfileManager().setLockOnAllNucleusSegments(false);
			
//		return true;
	}
		
	/**
	 * Run the segmentation part of the analysis. 
	 * @param collection
	 * @param pointType
	 */
	private void runSegmentation(ICellCollection collection, Tag pointType) throws Exception {

		fine("Beginning segmentation...");

		createSegmentsInMedian(collection);

		assignMedianSegmentsToNuclei(collection, pointType);


		fine("Segmentation complete");
	}
	
	/**
	 * Given a cell collection with a segmented median profile, apply the segmentation
	 * pattern to the nuclei within the colleciton.
	 * @param collection the collection
	 * @param pointType the tag to begin from
	 * @throws Exception
	 */
	private void assignMedianSegmentsToNuclei(ICellCollection collection, Tag pointType) throws Exception{

		// map the segments from the median directly onto the nuclei
		assignMedianSegmentsToNuclei(collection);

		// adjust the segments to better fit each nucleus
		fine("Revising segments by frankenprofile...");

		try{
			reviseSegments(collection, pointType);
		} catch (Exception e){
			warn("Error revising segments");
			stack("Error revising segments", e);
		}

		// update the aggregate in case any borders have changed
		collection.getProfileCollection()
		.createProfileAggregate(collection, collection.getProfileCollection().length());


	}
	
	/**
	 * Assign the segments in the median profile to the 
	 * nuclei within the collection
	 * @param collection
	 * @throws Exception
	 */
	private void assignMedianSegmentsToNuclei(ICellCollection collection) throws Exception{
		fine("Assigning segments to nuclei");
		IProfileCollection pc = collection.getProfileCollection();

		// find the corresponding point in each Nucleus
		ISegmentedProfile median = pc.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN);

		/*
		 * OLD CODE
		 */
		
//		SegmentAssignmentTask task = new SegmentAssignmentTask(median, collection.getNuclei().toArray(new Nucleus[0]));
//		task.addProgressListener(this);
//		task.invoke();
//		fine("Assigned segments to nuclei");
		
		/*
		 * NEW CODE
		 */
		
		collection.getNuclei().parallelStream().forEach( n -> {
			try {
				assignSegmentsToNucleus(median, n);
			} catch (ProfileException e) {
				warn("Error setting profile offsets in "+n.getNameAndNumber());
				stack(e.getMessage(), e);
			}
		});
				
		if(!checkRPmatchesSegments(collection)){
			warn("Segments do not all start on reference point after offsetting");
		}
		
	}
	
	/**
	 * Assign the median segments to the nucleus, finding the best match of the nucleus
	 * profile to the median profile
	 * @param n the nucleus to segment
	 * @param median the segmented median profile
	 */
	private void assignSegmentsToNucleus(ISegmentedProfile median, Nucleus n) throws ProfileException {

		if(n.isLocked()){
			return;
		}
		
		// remove any existing segments in the nucleus
		ISegmentedProfile nucleusProfile;
		try {
			nucleusProfile = n.getProfile(ProfileType.ANGLE);
		} catch (UnavailableProfileTypeException e1) {
			warn("Cannot get angle profile for nucleus");
			stack("Profile type angle is not available", e1);
			return;
		}
		nucleusProfile.clearSegments();

		List<IBorderSegment> nucleusSegments = new ArrayList<IBorderSegment>();

		// go through each segment defined for the median curve
		IBorderSegment prevSeg = null;

		for(IBorderSegment segment : median.getSegments()){

			// get the positions the segment begins and ends in the median profile
			int startIndexInMedian 	= segment.getStartIndex();
			int endIndexInMedian 	= segment.getEndIndex();

			// find the positions these correspond to in the offset profiles

			// get the median profile, indexed to the start or end point
			IProfile startOffsetMedian 	= median.offset(startIndexInMedian);
			IProfile endOffsetMedian 	= median.offset(endIndexInMedian);
			
			try {
				
				// find the index at the point of the best fit
				int startIndex 	= n.getProfile(ProfileType.ANGLE).getSlidingWindowOffset(startOffsetMedian);
				int endIndex 	= n.getProfile(ProfileType.ANGLE).getSlidingWindowOffset(endOffsetMedian);



				IBorderSegment seg = IBorderSegment.newSegment(startIndex, endIndex, n.getBorderLength(), segment.getID());
				if(prevSeg != null){
					seg.setPrevSegment(prevSeg);
					prevSeg.setNextSegment(seg);
				}

				nucleusSegments.add(seg);

				prevSeg = seg;
			
			} catch(IllegalArgumentException | UnavailableProfileTypeException e){
				warn("Cannot make segment");
				stack("Error making segment for nucleus "+n.getNameAndNumber(), e);
				break;
				
			}

		}

		IBorderSegment.linkSegments(nucleusSegments);
		
		nucleusProfile.setSegments(nucleusSegments);

		n.setProfile(ProfileType.ANGLE, nucleusProfile);
		finest("Assigned segments to nucleus "+n.getNameAndNumber()+":");
		finest(nucleusProfile.toString());
		
		finest("Assigned segments to "+n.getNameAndNumber());
		
	}
	
	/**
	 * Check if the reference point of the nuclear profiles is at a segment boundary
	 * for all nuclei
	 * @param collection
	 * @return
	 */
	private boolean checkRPmatchesSegments(ICellCollection collection){
		return collection.getNuclei().stream().allMatch( n -> {
			try {

				boolean hit = false;
				for(IBorderSegment s : n.getProfile(ProfileType.ANGLE).getSegments()){
					if(s.getStartIndex()==n.getBorderIndex(Tag.REFERENCE_POINT)){
						hit=true;
					}
				}
				if(!hit){
					// Update the segment boundary closest to the RP
					fine("RP not at segment start for "+n.getNameAndNumber());
					int end = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT)
						 	.getSegmentAt(0).getEndIndex();
					
					 ISegmentedProfile p = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
							 
					 p.getSegmentAt(0).update(0, end);
					 
					 n.setProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, p);
				}
				return n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).getSegmentAt(0).getStartIndex()==0;
			} catch (UnavailableComponentException | ProfileException e) {
				warn("Error checking profile offsets");
				stack(e.getMessage(), e);
				return false;
			}
		});
	}
	
		
	/**
	 * Use a ProfileSegmenter to segment the regular median profile of the collection starting
	 * from the reference point 
	 * @param collection
	 */
	private void createSegmentsInMedian(ICellCollection collection) throws Exception {

		IProfileCollection pc = collection.getProfileCollection();
	

		// the reference point is always index 0, so the segments will match
		// the profile
		IProfile median = pc.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Quartile.MEDIAN);
		
		Map<Tag, Integer> map = new HashMap<Tag, Integer>();
		int opIndex = pc.getIndex(Tag.ORIENTATION_POINT);
		map.put(Tag.ORIENTATION_POINT, opIndex);
		
		List<IBorderSegment> segments;
		if( ! collection.getNucleusType().equals(NucleusType.ROUND) && ! collection.getNucleusType().equals(NucleusType.NEUTROPHIL)){
			
			ProfileSegmenter segmenter = new ProfileSegmenter(median, map);		
			segments = segmenter.segment();
			
		} else {
			
			segments = new ArrayList<IBorderSegment>(1);
			IBorderSegment seg1 = IBorderSegment.newSegment(0, opIndex, median.size());
			IBorderSegment seg2 = IBorderSegment.newSegment(opIndex, median.size()-1, median.size());
			segments.add(seg1);
			segments.add(seg2);
		}

		finer("Found "+segments.size()+" segments in regular profile");
		
		pc.addSegments(Tag.REFERENCE_POINT, segments);
				
	}

	/**
	 * Update segment assignments in individual nuclei by stretching each segment to the best possible fit along
	 * the median profile 
	 * @param collection
	 * @param pointType
	 */
	private void reviseSegments(ICellCollection collection, Tag pointType) throws Exception {
		finer("Refining segment assignments...");

			IProfileCollection pc = collection.getProfileCollection();

			fine(pc.tagString());
			List<IBorderSegment> segments = pc.getSegments(pointType);
			fine("Fetched segments from profile collection");
			fine(IBorderSegment.toString(segments));

			// make a new profile collection to hold the frankendata
//			IProfileCollection frankenCollection = new DefaultProfileCollection();

			/*
			   The border tags for the frankenCollection are the same as the profile 
			   collection keys, and have the same positions (since a franken profile
			   is based on the median). The reference point is at index 0. 
			 */
//			for(Tag key : pc.getBorderTags()){
//				
//				int offset = pc.getIndex(key);
//				finest("Adding franken collection offset "+offset+" for "+ key);
//				frankenCollection.addIndex(key, pc.getIndex(key));
//			}

			
			/*
				At this point, the FrankenCollection is identical to the ProfileCollection, 
				but has no ProfileAggregate.
				We need to add the individual recombined frankenProfiles to the internal profile list,
				and build a ProfileAggregate
			 */

			// Get the median profile for the population
			ISegmentedProfile medianProfile = pc.getSegmentedProfile(ProfileType.ANGLE, pointType, Quartile.MEDIAN);
			finer("Median profile: angle at index 0 for "+Tag.REFERENCE_POINT+" is "+medianProfile.get(0));

			
			/*
			 * Split the recombining task into chunks for multithreading
			 */
			
			SegmentRecombiningTask task = new SegmentRecombiningTask(medianProfile, pc, collection.getNuclei().toArray(new Nucleus[0]));
			task.addProgressListener(this);

			try {
				task.invoke();
			} catch(RejectedExecutionException e){
				error("Fork task rejected: "+e.getMessage(), e);
			}

			/*
			 * Build a profile aggregate in the new frankencollection by taking the
			 * stored frankenprofiles from each nucleus in the collection
			 */
			try {
				pc.createProfileAggregate(collection, pc.length());
				finest("Creating franken profile aggregate of length "+pc.length());
//				frankenCollection.createProfileAggregate(collection, ProfileType.FRANKEN, pc.getAggregate().length());
			} catch(Exception e){
				warn("Error creating franken profile aggregate");
				stack(e.getMessage(), e);
				return;
			}
			
			
			
			if(!checkRPmatchesSegments(collection)){
				warn("Segments do not all start on reference point after recombining");
			}
			
	}
	
	
	@Override
	public void progressEventReceived(ProgressEvent event) {
		fireProgressEvent();
	}

}
