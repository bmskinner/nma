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

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import analysis.ProfileSegmenter;
import analysis.ProgressEvent;
import analysis.ProgressListener;
import components.CellCollection;
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
 * 1) Segments median profiles
 * 2) Apply the segments to nuclei
 * 3) Use frankenprofiles to generate best fist of segments in each nucleus
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
    public DatasetSegmenter(AnalysisDataset dataset, MorphologyAnalysisMode mode){
    	super(dataset);
    	this.mode = mode;
    }
    
    /**
     * Copy the segmentation pattern from the source collection onto the given dataset
     * @param dataset
     * @param source
     * @param programLogger
     */
    public DatasetSegmenter(AnalysisDataset dataset, CellCollection source){
    	this(dataset, MorphologyAnalysisMode.COPY);
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
			error("Error in segmentation analysis", e);
			return false;
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
    private boolean runNewAnalysis() throws Exception {
    	fine("Beginning core morphology analysis");

		this.setProgressTotal(getDataset().getCollection().getNucleusCount()*2);


		BorderTag pointType = BorderTag.REFERENCE_POINT;

		// segment the profiles from head
		runSegmentation(getDataset().getCollection(), pointType);

		fine("Core morphology analysis complete");
		return true;
    }
    
    private boolean runCopyAnalysis() throws Exception{
    	if(sourceCollection==null){
			warn("Cannot copy: source collection is null");
			return false;
		} else {
			this.setProgressTotal(getDataset().getCollection().getNucleusCount());
			fine( "Copying segmentation pattern");
			reapplyProfiles(getDataset().getCollection(), sourceCollection);
			fine("Copying complete");
			return true;
		}
    }
    
    private boolean runRefreshAnalysis() throws Exception{
    	fine("Refreshing segmentation");
    	this.setProgressTotal(getDataset().getCollection().getNucleusCount()*3);
		refresh(getDataset().getCollection());
		fine("Refresh complete");
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
		
		fine("Applying existing segmentation profile to population");
		
		
		sourceCollection.getProfileManager().copyCollectionOffsets(collection);
		
		// At this point the collection has only a regular profile collections.
		// No Frankenprofile has been copied.

		reviseSegments(collection, BorderTag.REFERENCE_POINT);	


		fine("Re-profiling complete");
		return true;
	}
	
	/**
	 * Copy the profile offsets from the median profile to a
	 * frankenprofile ProfileCollection. Also copies the 
	 * segments from the median profile.
	 * @param collection
	 * @throws Exception 
	 */
	private ProfileCollection createFrankenMedian(CellCollection collection) throws Exception{
		
		ProfileCollection pc = collection.getProfileCollection(ProfileType.REGULAR);
		
		// make a new profile collection to hold the frankendata
		ProfileCollection frankenCollection = new ProfileCollection();

		// add the correct offset keys
		// These are the same as the profile collection keys, and have
		// the same positions (since a franken profile is based on the median)
		// The reference point is at index 0
		for(BorderTag key : pc.getOffsetKeys()){
			frankenCollection.addOffset(key, pc.getOffset(key));
		}
		finest("Added frankenCollection index offsets for border tags");
		
		finest("Creating profile aggregate for FrankenCollection");
		// add all the nucleus frankenprofiles to the frankencollection
		frankenCollection.createProfileAggregate(collection, ProfileType.FRANKEN, (int)pc.getAggregate().length());

		finer("FrankenProfile generated");

		// copy the segments from the profile collection
		finer("Copying profile collection segments to frankenCollection");
		
		List<NucleusBorderSegment> segments = pc.getSegments(BorderTag.REFERENCE_POINT);
		frankenCollection.addSegments( BorderTag.REFERENCE_POINT, segments);
		finer("Added segments to frankenmedian");
		return frankenCollection;
	}
	
	/**
	 * Refresh the given collection. Create a new profile aggregate, and recalculate
	 * the FrankenProfiles. Do not recalculate segments for the median profile. Adjust
	 * individual nucleus segments to the median.
	 * @param collection
	 * @return
	 */
	public boolean refresh(CellCollection collection) throws Exception {

		fine("Refreshing mophology");

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

//		// make a new profile collection to hold the frankendata
		ProfileCollection frankenCollection = createFrankenMedian(collection);

		// At this point, the FrankenCollection is identical to the ProfileCollection
		// We need to add the individual recombined frankenProfiles

		// Ensure that the median profile segment IDs are propogated to the nuclei
		assignMedianSegmentsToNuclei(collection);

		// make a segment fitter to do the recombination of profiles
		finest("Creating segment fitter");
		SegmentFitter fitter = new SegmentFitter(pc.getSegmentedProfile(pointType));


		for(Nucleus n : collection.getNuclei()){ 
			// recombine the segments at the lengths of the median profile segments
			Profile recombinedProfile = fitter.recombine(n, BorderTag.REFERENCE_POINT);
			n.setProfile(ProfileType.FRANKEN, new SegmentedProfile(recombinedProfile));

			publish(progressCount++);

		}
		
		fitter = null;
		finest("Segment fitting complete");
		
		// copy the segments from the profile collection to other profiles
		finer("Copying profile collection segments to distance profile");
		collection.getProfileCollection(ProfileType.DISTANCE).addSegments(pointType, segments);
		
		finer("Copying profile collection segments to single distance profile");
		collection.getProfileCollection(ProfileType.SINGLE_DISTANCE).addSegments(pointType, segments);
		
		// attach the frankencollection to the cellcollection
		collection.setProfileCollection(ProfileType.FRANKEN, frankenCollection);



		// Ensure each nucleus gets the new profile pattern
//		assignMedianSegmentsToNuclei(collection);

		// find the corresponding point in each Nucleus
		SegmentedProfile median = pc.getSegmentedProfile(BorderTag.REFERENCE_POINT);
		
		finer("Creating segmnent assignment task");
		SegmentAssignmentTask task = new SegmentAssignmentTask(median, collection.getNuclei().toArray(new Nucleus[0]));
		task.addProgressListener(this);
//		task.invoke();
		mainPool.invoke(task);
		reviseSegments(collection, pointType);
		
		finer("Segments revised for nuclei");
		
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

		fine("Beginning segmentation...");
	
			
		// generate segments in the median profile
		fine("Creating segments...");
		createSegmentsInMedian(collection);

		// map the segments from the median directly onto the nuclei
		assignMedianSegmentsToNuclei(collection);

		// adjust the segments to better fit each nucleus
		fine("Revising segments by frankenprofile...");
		
		try{
			reviseSegments(collection, pointType);
		} catch (Exception e){
			logError("Error revising segments", e);
		}
		
		// update the aggregate in case any borders have changed
		collection.getProfileCollection(ProfileType.REGULAR).createProfileAggregate(collection, ProfileType.REGULAR);


		fine("Segmentation complete");
	}
	
	/**
	 * Assign the segments in the median profile to the 
	 * nuclei within the collection
	 * @param collection
	 * @throws Exception
	 */
	private void assignMedianSegmentsToNuclei(CellCollection collection) throws Exception{
		fine("Assigning segments to nuclei");
		ProfileCollection pc = collection.getProfileCollection(ProfileType.REGULAR);

		// find the corresponding point in each Nucleus
		SegmentedProfile median = pc.getSegmentedProfile(BorderTag.REFERENCE_POINT);
		//			assignSegments(collection);
		SegmentAssignmentTask task = new SegmentAssignmentTask(median, collection.getNuclei().toArray(new Nucleus[0]));
		task.addProgressListener(this);
		task.invoke();
		fine("Assigned segments to nuclei");
	}
	
	
	/**
	 * Use a ProfileSegmenter to segment the regular median profile of the collection starting
	 * from the reference point 
	 * @param collection
	 * @return
	 * @throws Exception
	 */
	private List<NucleusBorderSegment> segmentMedianProfile(CellCollection collection) throws Exception{
		
		ProfileCollection pc = collection.getProfileCollection(ProfileType.REGULAR);

		// the reference point is always index 0, so the segments will match
		// the profile
		Profile median = pc.getProfile(BorderTag.REFERENCE_POINT, Constants.MEDIAN);

		ProfileSegmenter segmenter = new ProfileSegmenter(median);		

		List<NucleusBorderSegment> segments = segmenter.segment();

		finer("Found "+segments.size()+" segments in "+collection.getPoint(BorderTag.REFERENCE_POINT)+" profile");

		segmenter = null; // clean up
		return segments;
	}
	
	/**
	 * Run the segmenter on the median profile for the given point type
	 * @param collection
	 */
	private void createSegmentsInMedian(CellCollection collection) throws Exception {

		ProfileCollection pc = collection.getProfileCollection(ProfileType.REGULAR);

		List<NucleusBorderSegment> segments = segmentMedianProfile(collection);

		/*
		 * Handle edge cases where the segmenter has buggered up
		 * TODO: Check if this is still relevant
		 */
		if(collection.getNucleusType().equals(NucleusType.ROUND) && segments.size()==2){

			finer("Updating segment pattern to match orientation point in round nuclei");

			int medianOrientationIndex = pc.getOffset(BorderTag.ORIENTATION_POINT);
			
			finer("Orientation point is at index "+medianOrientationIndex);
						
			NucleusBorderSegment firstSegment = segments.get(0);
			firstSegment.update(firstSegment.getStartIndex(), medianOrientationIndex);
		}
		
		pc.addSegments(BorderTag.REFERENCE_POINT, segments);
		
		/*
		 * TODO: Find the root cause
		 */
		
		if(collection.getNucleusType().equals(NucleusType.PIG_SPERM) ){
			
			if( ! pc.hasSegmentStartingWith(BorderTag.ORIENTATION_POINT)){
				finer("The pig RP is at "+pc.getOffset(BorderTag.REFERENCE_POINT));
				finer("The pig OP is at "+pc.getOffset(BorderTag.ORIENTATION_POINT));
				
				

				int medianRPIndex = pc.getOffset(BorderTag.REFERENCE_POINT);
				pc.addOffset(BorderTag.ORIENTATION_POINT, medianRPIndex);
				finer("Updating OP to "+medianRPIndex);
			}
		}
		
		
		
		
		/* 
		 * If only two segments were created, the orientation point 
		 * may not be at a segment breakpoint. 
		 * 
		 * Split segments appropriately so that the OP is on a boundary.
		 * 
		 * There should not be a problem with segment lengths here, because
		 * a highly segmentatble profile will have a detectable tail in the 
		 * first place.
		 * 
		 * 2016-05-25 There is a problem in pig nuclei. The OP does not always 
		 * have segment boundary. We need to find the segment start closest to the
		 * opIndex and update the segment
		 */
		
		if( ! pc.hasSegmentStartingWith(BorderTag.ORIENTATION_POINT)){
			finer("The orientation point is not at a segment boundary");
			finer("The RP is at "+pc.getOffset(BorderTag.REFERENCE_POINT));
			finer("The OP is at "+pc.getOffset(BorderTag.ORIENTATION_POINT));
			int opIndex = pc.getOffset(BorderTag.ORIENTATION_POINT);
			NucleusBorderSegment seg = pc.getSegmentContaining(opIndex);
			
			finest("Trying to split the segment at index "+opIndex);
			
			UUID newID1 = java.util.UUID.randomUUID();
			UUID newID2 = java.util.UUID.randomUUID();

			/*
			 * This causes an error when only one segment is present in the profile.
			 * This is because the start and end points of the segment are the same.
			 * 
			 * An error also occurs if a segment boundary lies too close to the opIndex 
			 */
			SegmentedProfile p = pc.getSegmentedProfile(BorderTag.REFERENCE_POINT);
			
			try{
				p.splitSegment(seg, opIndex, newID1, newID2);

				segments = p.getOrderedSegments();
				pc.addSegments(BorderTag.REFERENCE_POINT, segments);
			} catch (Exception e){
				log(Level.SEVERE, "Profile:");
				log(Level.SEVERE, p.toString());
				log(Level.SEVERE, "Segment to split:");
				log(Level.SEVERE, seg.toString());
				throw e;
			}
			
		}
		
	}

	/**
	 * Update segment assignments in individual nuclei by stretching each segment to the best possible fit along
	 * the median profile 
	 * @param collection
	 * @param pointType
	 */
	private void reviseSegments(CellCollection collection, BorderTag pointType) throws Exception {
		finer("Refining segment assignments...");

			ProfileCollection pc = collection.getProfileCollection(ProfileType.REGULAR);
			fine("Median profile");
			fine(pc.toString());
			List<NucleusBorderSegment> segments = pc.getSegments(pointType);
			fine("Fetched segments from profile collection");
			fine(NucleusBorderSegment.toString(segments));

			// make a new profile collection to hold the frankendata
			ProfileCollection frankenCollection = new ProfileCollection();

			/*
			   The border tags for the frankenCollection are the same as the profile 
			   collection keys, and have the same positions (since a franken profile
			   is based on the median). The reference point is at index 0. 
			 */
			for(BorderTag key : pc.getOffsetKeys()){
				
				int offset = pc.getOffset(key);
				finest("Adding franken collection offset "+offset+" for "+ key);
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
			finer("Median profile: angle at index 0 for "+BorderTag.REFERENCE_POINT+" is "+medianProfile.get(0));

			
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
				finest("Creating franken profile aggregate of length "+pc.getAggregate().length());
				frankenCollection.createProfileAggregate(collection, ProfileType.FRANKEN, pc.getAggregate().length());
			} catch(Exception e){
				log(Level.SEVERE, "Error creating franken profile aggregate");
				log(Level.SEVERE, "Attempting to continue without franken profiling");
				return;
			}
			
			/*
			 * At this point, the frankencollection is indexed on the reference point.
			 * This is because the reference point is used to generate the profile collection.
			 * Copy the segments from the profile collection, starting from the reference point
			 */
			frankenCollection.addSegments(pointType, segments);

			double firstPoint = frankenCollection.getSegmentedProfile(BorderTag.REFERENCE_POINT).get(0);
			finer("FrankenProfile generated: angle at index 0 for "+BorderTag.REFERENCE_POINT+" is "+firstPoint);
			
			// attach the frankencollection to the cellcollection
			collection.setProfileCollection(ProfileType.FRANKEN, frankenCollection);
			finer("Segment assignments refined");
			
			// copy the segments from the profile collection to other profiles
			finer("Copying profile collection segments to distance profile");
			collection.getProfileCollection(ProfileType.DISTANCE).addSegments(pointType, segments);
			
			finer("Copying profile collection segments to single distance profile");
			collection.getProfileCollection(ProfileType.SINGLE_DISTANCE).addSegments(pointType, segments);
	}
	
	
	@Override
	public void progressEventReceived(ProgressEvent event) {
		publish(++progressCount);
		
	}
}
