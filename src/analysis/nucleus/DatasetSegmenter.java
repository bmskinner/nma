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
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileCollection;
import components.generic.ProfileCollectionType;
import components.generic.SegmentedProfile;
import components.nuclear.NucleusBorderSegment;
import components.nuclei.Nucleus;

/**
 * This is the core of the morphology analysis pipeline.
 * It is the only module that is essential. This offsets nucleus profiles,
 * generates the median profiles, segments them, and applies the segments to
 * nuclei.
 */
public class DatasetSegmenter extends AnalysisWorker {
	
    public static final int MODE_NEW     = 0;
    public static final int MODE_COPY    = 1;
    public static final int MODE_REFRESH = 2;
        
//    private CellCollection collection; // the collection to work on
    private CellCollection sourceCollection = null; // a collection to take segments from
    private int mode = MODE_NEW; 				// the analysis mode

    
    /*
      //////////////////////////////////////////////////
      Constructors
      //////////////////////////////////////////////////
    */
    
    public DatasetSegmenter(AnalysisDataset dataset, int mode, Logger programLogger){
    	super(dataset, programLogger);

    	try{
    		this.mode = mode;

    	} catch (Exception e){
    		logError("Error creating analysis", e);
    	}
    }
    
    public DatasetSegmenter(AnalysisDataset dataset, CellCollection source, Logger programLogger){
    	super(dataset, programLogger);
    	try{
    		this.mode = MODE_COPY;
    		this.sourceCollection = source;

    	} catch (Exception e){
    		logError("Error creating analysis", e);
    	}
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

			// mode selection
			if(mode == MODE_NEW){
				log(Level.FINE, "Beginning core morphology analysis");

				// we run the profiler and segmenter on each nucleus - may need to double
				this.setProgressTotal(getDataset().getCollection().getNucleusCount());
//				totalNuclei = collection.getNucleusCount(); 

				BorderTag pointType = BorderTag.REFERENCE_POINT;

				// segment the profiles from head
				runSegmentation(getDataset().getCollection(), pointType);

				log(Level.FINE, "Core morphology analysis complete");
			}
			
			if(mode == MODE_REFRESH){

				log(Level.FINE, "Refreshing morphology");
				refresh(getDataset().getCollection());
				log(Level.FINE, "Refresh complete");
//				return true;
			}
			
			if(mode == MODE_COPY){
//				IJ.log("Copying");
				if(sourceCollection==null){
					log(Level.WARNING,  "Cannot copy: source collection is null");
					result = false;
				} else {
					this.setProgressTotal(getDataset().getCollection().getNucleusCount());
					log(Level.FINE,  "Copying segmentation pattern");
					reapplyProfiles(getDataset().getCollection(), sourceCollection);
					log(Level.FINE, "Copying complete");
				}
			}
			
		} catch(Exception e){
			
			logError("Error in morphology analysis", e);
			
			fileLogger.log(Level.SEVERE, "Collection keys:");
			fileLogger.log(Level.SEVERE, getDataset().getCollection().getProfileCollection(ProfileCollectionType.REGULAR).printKeys());
			
			fileLogger.log(Level.SEVERE, "FrankenCollection keys:");
			fileLogger.log(Level.SEVERE, getDataset().getCollection().getProfileCollection(ProfileCollectionType.FRANKEN).printKeys());
			result = false;
		} 

		return result;
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
	public boolean reapplyProfiles(CellCollection collection, CellCollection sourceCollection){
		
//		logger = new Logger(collection.getDebugFile(), "MorphologyAnalysis");
		log(Level.FINE, "Applying existing segmentation profile to population...");
		
		try {
			BorderTag referencePoint   = BorderTag.REFERENCE_POINT;
			
			// use the same array length as the source collection to avoid segment slippage
			int profileLength = sourceCollection.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(referencePoint, 50).size();
			
			// get the empty profile collection from the new CellCollection
			// TODO: if the target collection is not new, ie we are copying onto
			// an existing segmenation pattern, then this profile collection will have
			// offsets
			ProfileCollection pc = collection.getProfileCollection(ProfileCollectionType.REGULAR);
			
			// make an aggregate from the nuclei. A new median profile must necessarily result.
			// By default, the aggregates are created from the reference point
			pc.createProfileAggregate(collection, profileLength);
			
			// copy the offset keys from the source collection
			//TODO:
			// If this is applied to a collection from an entirely difference source,
			// rather than a child, then the offsets will be completely wrong. In that
			// instance, we should probably keep the existing offset positions for head
			// and tail
			ProfileCollection sc = sourceCollection.getProfileCollection(ProfileCollectionType.REGULAR);
			
			for(BorderTag offsetKey : sc.getOffsetKeys()){
				int offset = sc.getOffset(offsetKey);
				pc.addOffset(offsetKey, offset);
				log(Level.FINER, "Setting "+offsetKey+" to "+offset);
			}
			
			
			// What happens when the array length is greater in the source collection? 
			// Segments are added that no longer have an index
			// We need to scale the segments to the array length of the new collection
			pc.addSegments(referencePoint, sc.getSegments(referencePoint));

			
			// At this point the collection has only a regular profile collection.
			// No Frankenprofile has been copied.

			reviseSegments(collection, referencePoint);	



		} catch (Exception e) {
			logError("Error reapplying profiles", e);
			return false;
		}
		log(Level.FINE, "Re-profiling complete");
		return true;
	}
	
	/**
	 * Refresh the given collection. Create a new profile aggregate, and recalculate
	 * the FrankenProfiles. Do not refit segments.
	 * @param collection
	 * @return
	 */
	public boolean refresh(CellCollection collection){
//		logger = new Logger(collection.getDebugFile(), "MorphologyAnalysis");
		log(Level.FINE, "Refreshing mophology");
		try{
			
			BorderTag pointType = BorderTag.REFERENCE_POINT;
			
			// get the empty profile collection from the new CellCollection
			ProfileCollection pc = collection.getProfileCollection(ProfileCollectionType.REGULAR);

			// make an aggregate from the nuclei. A new median profile must necessarily result.
			// By default, the aggregates are created from the reference point
			pc.createProfileAggregate(collection);
			
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


			// copy the segments from the profile collection
			frankenCollection.addSegments(pointType, segments);

			// At this point, the FrankenCollection is identical to the ProfileCollection
			// We need to add the individual recombined frankenProfiles


			// make a segment fitter to do the recombination of profiles
			SegmentFitter fitter = new SegmentFitter(pc.getSegmentedProfile(pointType), fileLogger);
			List<Profile> frankenProfiles = new ArrayList<Profile>(0);

			int count = 0;
			for(Nucleus n : collection.getNuclei()){ 
				// recombine the segments at the lengths of the median profile segments
				Profile recombinedProfile = fitter.recombine(n, BorderTag.REFERENCE_POINT);
				frankenProfiles.add(recombinedProfile);
				count++;
				publish(count);
				
			}

			// add all the nucleus frankenprofiles to the frankencollection
			frankenCollection.addNucleusProfiles(frankenProfiles);

			// update the profile aggregate
			frankenCollection.createProfileAggregateFromInternalProfiles((int)pc.getAggregate().length());
			log(Level.FINER, "FrankenProfile generated");

			// attach the frankencollection to the cellcollection
			collection.setProfileCollection(ProfileCollectionType.FRANKEN, frankenCollection);

		} catch (Exception e) {
			logError("Error reapplying profiles", e);
			return false;
		}
		return true;
	}
	
	/**
	 * Get the total differences to the median for all the nuclei in
	 * the collection
	 * @param collection
	 * @param pointType
	 * @return
	 */
	private static double compareProfilesToMedian(CellCollection collection, BorderTag pointType) throws Exception {
		double[] scores = collection.getDifferencesToMedianFromPoint(pointType);
		double result = 0;
		for(double s : scores){
			result += s;
		}
		return result;
	}
	
	/**
	 * Run the segmentation part of the analysis. 
	 * @param collection
	 * @param pointType
	 */
	private void runSegmentation(CellCollection collection, BorderTag pointType){
//		fileLogger.log(Level.INFO, "Beginning segmentation...");
		log(Level.FINE, "Beginning segmentation...");
		try{	
			
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
			collection.getProfileCollection(ProfileCollectionType.REGULAR).createProfileAggregate(collection);
						
			// At this point, the franken collection still contains tip/head values only
			
		} catch(Exception e){
			logError("Error segmenting",e);
//			programLogger.log(Level.SEVERE, "Error segmenting",e);
			collection.getProfileCollection(ProfileCollectionType.REGULAR).printKeys();
		}
		log(Level.FINE, "Segmentation complete");
//		fileLogger.log(Level.INFO, "Segmentation complete");
	}
	
	/**
	 * Run the segmenter on the median profile for the given point type
	 * @param collection
	 */
	private static void createSegments(CellCollection collection){

		try{
			ProfileCollection pc = collection.getProfileCollection(ProfileCollectionType.REGULAR);

//			fileLogger.log(Level.FINE, "Using regular profile collection for segmentation");
//			fileLogger.log(Level.FINE, pc.toString());
			
			// the reference point is always index 0, so the segments will match
			// the profile
			Profile median = pc.getProfile(BorderTag.REFERENCE_POINT, 50);

			ProfileSegmenter segmenter = new ProfileSegmenter(median, fileLogger);		
			
			int orientationIndex = pc.getOffset(BorderTag.ORIENTATION_POINT);
			List<NucleusBorderSegment> segments = segmenter.segment();

			log(Level.FINER, "Found "+segments.size()+" segments in "+collection.getPoint(BorderTag.REFERENCE_POINT)+" profile");

			// Add the segments to the collection

			pc.addSegments(BorderTag.REFERENCE_POINT, segments);
		} catch(Exception e){
			logError("Error creating segments", e);
		}
	}

	/**
	 * From the calculated median profile segments, assign segments to each nucleus
	 * based on the best offset fit of the start and end indexes 
	 * @param collection
	 * @param pointType
	 */
	private static void assignSegments(CellCollection collection){

		try{
			log(Level.FINER, "Assigning segments to nuclei...");

			ProfileCollection pc = collection.getProfileCollection(ProfileCollectionType.REGULAR);

			// find the corresponding point in each Nucleus
			SegmentedProfile median = pc.getSegmentedProfile(BorderTag.REFERENCE_POINT);

			for(Nucleus n : collection.getNuclei()){
				assignSegmentsToNucleus(n, median);
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
	private static void assignSegmentsToNucleus(Nucleus n, SegmentedProfile median){

		try{
			
			// remove any existing segments in the nucleus
			SegmentedProfile nucleusProfile = n.getAngleProfile();
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
				int startIndex 	= n.getAngleProfile().getSlidingWindowOffset(startOffsetMedian);
				int endIndex 	= n.getAngleProfile().getSlidingWindowOffset(endOffsetMedian);

				// create a segment at these points
				// ensure that the segment meets length requirements
				NucleusBorderSegment seg = new NucleusBorderSegment(startIndex, endIndex, n.getLength());
				if(prevSeg != null){
					seg.setPrevSegment(prevSeg);
					prevSeg.setNextSegment(seg);
				}

				seg.setName(segment.getName());
				nucleusSegments.add(seg);

				prevSeg = seg;
			}

			NucleusBorderSegment.linkSegments(nucleusSegments);
			nucleusProfile.setSegments(nucleusSegments);
			n.setAngleProfile(nucleusProfile);
		} catch (Exception e) {
			logError("Error assigning segments to nucleus", e);
//			fileLogger.log(Level.SEVERE, "Error assigning segments to nucleus",e);
		}

		
	}

	/**
	 * Update initial segment assignments by stretching each segment to the best possible fit along
	 * the median profile 
	 * @param collection
	 * @param pointType
	 */
	private void reviseSegments(CellCollection collection, BorderTag pointType){
		log(Level.FINER, "Refining segment assignments...");
		try{

			ProfileCollection pc = collection.getProfileCollection(ProfileCollectionType.REGULAR);
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
			 * At this point, the frankencollection is indexed on the reference point.
			 * This is because the reference point is used to generate the profile collection.
			 * Copy the segments from the profile collection, starting from the reference point
			 */
			frankenCollection.addSegments(pointType, segments);
			
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
			
			SegmentFitter fitter = new SegmentFitter(medianProfile, fileLogger);
			List<Profile> frankenProfiles = new ArrayList<Profile>(0);

			int count = 1;
			for(Nucleus n : collection.getNuclei()){ 
				log(Level.FINER, "Fitting nucleus "+n.getPathAndNumber()+" ("+count+" of "+collection.size()+")");
				fitter.fit(n, pc);

				// recombine the segments at the lengths of the median profile segments
				Profile recombinedProfile = fitter.recombine(n, BorderTag.REFERENCE_POINT);
				frankenProfiles.add(recombinedProfile);

				publish(count++); // publish the progress to gui
			}

			// add all the nucleus frankenprofiles to the frankencollection
			frankenCollection.addNucleusProfiles(frankenProfiles);

			// update the profile aggregate
			frankenCollection.createProfileAggregateFromInternalProfiles((int)pc.getAggregate().length());

			double firstPoint = frankenCollection.getSegmentedProfile(BorderTag.REFERENCE_POINT).get(0);
			log(Level.FINER, "FrankenProfile generated: angle at index 0 for "+BorderTag.REFERENCE_POINT+" is "+firstPoint);
			// attach the frankencollection to the cellcollection
			collection.setProfileCollection(ProfileCollectionType.FRANKEN, frankenCollection);
			log(Level.FINER, "Segment assignments refined");
		} catch(Exception e){
			logError("Error revising segments", e);
		}
	}
}
