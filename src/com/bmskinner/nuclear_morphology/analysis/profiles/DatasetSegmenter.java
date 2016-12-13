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
package com.bmskinner.nuclear_morphology.analysis.profiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Level;

import com.bmskinner.nuclear_morphology.analysis.AnalysisWorker;
import com.bmskinner.nuclear_morphology.analysis.ProgressEvent;
import com.bmskinner.nuclear_morphology.analysis.ProgressListener;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.BorderTagObject;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.SegmentedFloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.stats.Quartile;

/**
 * This is the core of the morphology analysis pipeline.
 * 1) Segments median profiles
 * 2) Apply the segments to nuclei
 * 3) Use frankenprofiles to generate best fist of segments in each nucleus
 */
public class DatasetSegmenter extends AnalysisWorker implements ProgressListener {

    private ICellCollection sourceCollection = null; // a collection to take segments from

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
    public DatasetSegmenter(IAnalysisDataset dataset, MorphologyAnalysisMode mode){
    	super(dataset);
    	this.mode = mode;
    }
    
    /**
     * Copy the segmentation pattern from the source collection onto the given dataset
     * @param dataset
     * @param source
     * @param programLogger
     */
    public DatasetSegmenter(IAnalysisDataset dataset, ICellCollection source){
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
			

			// Ensure segments are copied appropriately to verticals
			// Ensure hook statistics are generated appropriately
//			log("Updating verticals");
			for(Nucleus n : this.getDataset().getCollection().getNuclei()){
//				log("Updating "+n.getNameAndNumber());
				n.updateVerticallyRotatedNucleus();
				n.updateDependentStats();
				
			}
			fine("Updated verticals and stats");
			
		} catch(Exception e){
			result = false;
			stack("Error in segmentation analysis", e);
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

		this.setProgressTotal(getDataset().getCollection().size()*2);


		Tag pointType = Tag.REFERENCE_POINT;

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
			this.setProgressTotal(getDataset().getCollection().size());
			fine( "Copying segmentation pattern");
			reapplyProfiles(getDataset().getCollection(), sourceCollection);
			fine("Copying complete");
			return true;
		}
    }
    
    private boolean runRefreshAnalysis() throws Exception{
    	fine("Refreshing segmentation");
    	this.setProgressTotal(getDataset().getCollection().size()*3);
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

			// generate segments in the median profile
			fine("Creating segments...");
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
		if( ! collection.getNucleusType().equals(NucleusType.ROUND)){
			
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
				mainPool.invoke(task);
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
				log(Level.SEVERE, "Error creating franken profile aggregate");
				log(Level.SEVERE, "Attempting to continue without franken profiling");
				return;
			}
			
			/*
			 * At this point, the frankencollection is indexed on the reference point.
			 * This is because the reference point is used to generate the profile collection.
			 * Copy the segments from the profile collection, starting from the reference point
			 */
//			frankenCollection.addSegments(pointType, segments);
//
//			double firstPoint = frankenCollection.getSegmentedProfile(Tag.REFERENCE_POINT).get(0);
//			finer("FrankenProfile generated: angle at index 0 for "+Tag.REFERENCE_POINT+" is "+firstPoint);
//			
//			// attach the frankencollection to the cellcollection
//			collection.setProfileCollection(ProfileType.FRANKEN, frankenCollection);
//			finer("Segment assignments refined");
			
//			// copy the segments from the profile collection to other profiles
//			finer("Copying profile collection segments to distance profile");
//			collection.getProfileCollection(ProfileType.DIAMETER).addSegments(pointType, segments);
//			
//			finer("Copying profile collection segments to single distance profile");
//			collection.getProfileCollection(ProfileType.RADIUS).addSegments(pointType, segments);
	}
	
	
	@Override
	public void progressEventReceived(ProgressEvent event) {
		publish(++progressCount);
		
	}
}
