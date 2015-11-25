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
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import analysis.AnalysisDataset;
import analysis.AnalysisWorker;
import utility.Constants;
import utility.Utils;
import components.CellCollection;
import components.generic.BooleanProfile;
import components.generic.BorderTag;
import components.generic.Profile;
import components.generic.ProfileCollection;
import components.generic.ProfileCollectionType;
import components.generic.SegmentedProfile;
import components.nuclear.NucleusBorderSegment;
import components.nuclear.NucleusType;
import components.nuclei.Nucleus;
import components.nuclei.sperm.PigSpermNucleus;
import components.nuclei.sperm.RodentSpermNucleus;

/**
 * This is the core of the morphology analysis pipeline.
 * It is the only module that is essential. This offsets nucleus profiles,
 * generates the median profiles, segments them, and applies the segments to
 * nuclei.
 */
public class MorphologyAnalysis extends AnalysisWorker {
	
//	private static Logger logger;
    public static final int MODE_NEW     = 0;
    public static final int MODE_COPY    = 1;
    public static final int MODE_REFRESH = 2;
        
    private CellCollection collection; // the collection to work on
    private CellCollection sourceCollection = null; // a collection to take segments from
    private int mode = MODE_NEW; 				// the analysis mode

    
    /*
      //////////////////////////////////////////////////
      Constructors
      //////////////////////////////////////////////////
    */
    
    public MorphologyAnalysis(AnalysisDataset dataset, int mode, Logger programLogger){
    	super(dataset, programLogger);
//    	MorphologyAnalysis.programLogger = programLogger;
    	try{
    		this.collection = dataset.getCollection();
    		this.mode = mode;
    		
    		AnalysisWorker.fileLogger = Logger.getLogger(MorphologyAnalysis.class.getName());
    		fileLogger.setLevel(Level.FINE);
    		fileLogger.addHandler(dataset.getLogHandler());
    		fileLogger.log(Level.FINE, "Created MorphologyAnalysis");
    	} catch (Exception e){
    		programLogger.log(Level.SEVERE, "Error creating analysis", e);
    	}
    }
    
    public MorphologyAnalysis(AnalysisDataset dataset, CellCollection source, Logger programLogger){
    	super(dataset, programLogger);
    	try{
    		this.collection = dataset.getCollection();
    		this.mode = MODE_COPY;
    		this.sourceCollection = source;

    		MorphologyAnalysis.fileLogger = Logger.getLogger(MorphologyAnalysis.class.getName());
    		fileLogger.setLevel(Level.FINE);
    		fileLogger.addHandler(dataset.getLogHandler());
    		fileLogger.log(Level.FINE, "Created MorphologyAnalysis");
    	} catch (Exception e){
    		programLogger.log(Level.SEVERE, "Error creating analysis", e);
    	}
    }
    
    /*
      //////////////////////////////////////////////////
      SwingWorker methods
      //////////////////////////////////////////////////
     */
    
//    @Override
//    protected void process( List<Integer> integers ) {
//        int amount = integers.get( integers.size() - 1 );
//        int percent = (int) ( (double) amount / (double) totalNuclei * 100);
//        if(percent >= 0 && percent <=100){
//        	setProgress(percent); // the integer representation of the percent
//        }
//    }
    
    @Override
    protected Boolean doInBackground() throws Exception {
//    	logger = new Logger(collection.getDebugFile(), "MorphologyAnalysis");
//    	DebugFileHandler handler = null;
//		try {
//			handler = new DebugFileHandler(collection.getDebugFile());
//			handler.setFormatter(new DebugFileFormatter());
//			fileLogger.addHandler(handler);
//		} catch (SecurityException e1) {
//			programLogger.log(Level.SEVERE, "Could not create the log file handler", e1);
//		} catch (IOException e1) {
//			programLogger.log(Level.SEVERE, "Could not create the log file handler", e1);
//		}
    	
    	boolean result = true;
		try{

			// mode selection
			if(mode == MODE_NEW){
				programLogger.log(Level.FINE, "Beginning core morphology analysis");
				fileLogger.log(Level.INFO, "Beginning core morphology analysis");

				// we run the profiler and segmenter on each nucleus - may need to double
				this.setProgressTotal(collection.getNucleusCount());
//				totalNuclei = collection.getNucleusCount(); 

				BorderTag pointType = BorderTag.REFERENCE_POINT;

				// profile the collection from head/tip, then apply to tail
				runProfiler(collection, pointType);

				// segment the profiles from head
				runSegmentation(collection, pointType);

				fileLogger.log(Level.INFO, "Core morphology analysis complete");
			}
			
			if(mode == MODE_REFRESH){

				fileLogger.log(Level.INFO, "Refreshing morphology");
				refresh(collection);
				fileLogger.log(Level.INFO, "Refresh complete");
//				return true;
			}
			
			if(mode == MODE_COPY){
//				IJ.log("Copying");
				if(sourceCollection==null){
					programLogger.log(Level.WARNING,  "Cannot copy: source collection is null");
					fileLogger.log(Level.INFO, "Cannot copy: source collection is null");
					result = false;
				} else {
					this.setProgressTotal(collection.getNucleusCount());
//					totalNuclei = collection.getNucleusCount(); 
					fileLogger.log(Level.INFO, "Copying segmentation pattern");
					programLogger.log(Level.FINE,  "Copying segmentation pattern");
					reapplyProfiles(collection, sourceCollection);
					fileLogger.log(Level.INFO, "Copying complete");
					programLogger.log(Level.FINE, "Copying complete");
				}
			}
			
		} catch(Exception e){
			
			fileLogger.log(Level.SEVERE, "Error in morphology analysis", e);
			programLogger.log(Level.SEVERE, "Error in morphology analysis");
			
			fileLogger.log(Level.SEVERE, "Collection keys:");
			fileLogger.log(Level.SEVERE, collection.getProfileCollection(ProfileCollectionType.REGULAR).printKeys());
			
			fileLogger.log(Level.SEVERE, "FrankenCollection keys:");
			fileLogger.log(Level.SEVERE, collection.getProfileCollection(ProfileCollectionType.FRANKEN).printKeys());
			result = false;
		} 

		return result;
	}
    
//    @Override
//    public void done() {
//    	
//    	programLogger.log(Level.FINEST, "Completed morphology worker task; firing trigger");
//
//        try {
//            if(this.get()){
//            	programLogger.log(Level.FINEST, "Firing trigger for sucessful task");
//                firePropertyChange("Finished", getProgress(), Constants.Progress.FINISHED.code());            
//
//            } else {
//            	programLogger.log(Level.FINEST, "Firing trigger for error in task");
//                firePropertyChange("Error", getProgress(), Constants.Progress.ERROR.code());
//            }
//        } catch (InterruptedException e) {
//        	fileLogger.log(Level.SEVERE, "Error in morphology application", e);
//        } catch (ExecutionException e) {
//        	fileLogger.log(Level.SEVERE, "Error in morphology application", e);
//            
//        	fileLogger.log(Level.SEVERE, "Collection keys:");
//        	fileLogger.log(Level.SEVERE, collection.getProfileCollection(ProfileCollectionType.REGULAR).printKeys());
//            
//        	fileLogger.log(Level.SEVERE, "FrankenCollection keys:");
//        	fileLogger.log(Level.SEVERE, collection.getProfileCollection(ProfileCollectionType.FRANKEN).printKeys());
//       }
////        	} finally{
////			for(Handler h : fileLogger.getHandlers()){
////				h.close();
////			}
////		}
//
//    } 

    /*
    //////////////////////////////////////////////////
    Analysis methods
    //////////////////////////////////////////////////
   */
    	
	/**
	 * Calculaate the median profile of the colleciton, and generate the
	 * best fit offsets of each nucleus to match
	 * @param collection
	 * @param pointType
	 */
	private static void runProfiler(CellCollection collection, BorderTag pointType){
		
		try{
			programLogger.log(Level.FINE,  "Profiling collection");
			// A cell collection starts with an empty Regular ProfileCollection
			ProfileCollection pc = collection.getProfileCollection(ProfileCollectionType.REGULAR);

			// default is to make profile aggregate from reference point
			pc.createProfileAggregate(collection);

			// use the median profile of this aggregate to find the orientation point ("tail")
			TailFinder.findTailIndexInMedianCurve(collection);

			// carry out iterative offsetting to refine the orientation point estimate
			double score = compareProfilesToMedian(collection, pointType);
			double prevScore = score*2;
			while(score < prevScore){

				// rebuild the aggregate - needed if the orientation point index has changed in any nuclei
				pc.createProfileAggregate(collection);

				// carry out the orientation point detection in the median again
				TailFinder.findTailIndexInMedianCurve(collection);

				// apply offsets to each nucleus in the collection
				Offsetter.calculateOffsets(collection); 

				prevScore = score;

				// get the difference between aligned profiles and the median
				score = compareProfilesToMedian(collection, pointType);
				fileLogger.log(Level.INFO, "Reticulating splines: score: "+(int)score);
				programLogger.log(Level.FINE, "Reticulating splines: score: "+(int)score);
			}
			programLogger.log(Level.FINE,  "Finished profiling collection");
			fileLogger.log(Level.INFO, "Finished profiling collection: "+(int)score);
			fileLogger.log(Level.FINE, collection.getProfileCollection(ProfileCollectionType.REGULAR).toString());
		} catch(Exception e){
			fileLogger.log(Level.SEVERE, "Error in morphology profiling", e);
		}
	}
		
	/**
	 * When a population needs to be reanalysed do not offset nuclei or recalculate best fits;
	 * just get the new median profile 
	 * @param collection the collection of nuclei
	 * @param sourceCollection the collection with segments to copy
	 */
	public boolean reapplyProfiles(CellCollection collection, CellCollection sourceCollection){
		
//		logger = new Logger(collection.getDebugFile(), "MorphologyAnalysis");
		fileLogger.log(Level.INFO, "Applying existing segmentation profile to population...");
		
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
				fileLogger.log(Level.FINE, "Setting "+offsetKey+" to "+offset);
			}
			
			
			// What happens when the array length is greater in the source collection? 
			// Segments are added that no longer have an index
			// We need to scale the segments to the array length of the new collection
			pc.addSegments(referencePoint, sc.getSegments(referencePoint));

			
			// At this point the collection has only a regular profile collection.
			// No Frankenprofile has been copied.

			reviseSegments(collection, referencePoint);	



		} catch (Exception e) {
			fileLogger.log(Level.SEVERE, "Error reapplying profiles", e);
			return false;
		}
		fileLogger.log(Level.INFO, "Re-profiling complete");
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
		fileLogger.log(Level.INFO, "Refreshing mophology");
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
			fileLogger.log(Level.INFO, "FrankenProfile generated");

			// attach the frankencollection to the cellcollection
			collection.setProfileCollection(ProfileCollectionType.FRANKEN, frankenCollection);

		} catch (Exception e) {
			fileLogger.log(Level.SEVERE, "Error reapplying profiles", e);
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
		fileLogger.log(Level.INFO, "Beginning segmentation...");
		programLogger.log(Level.FINE, "Beginning segmentation...");
		try{	
			
			// generate segments in the median profile
			programLogger.log(Level.FINE, "Creating segments...");
			createSegments(collection);
			
			// map the segments from the median directly onto the nuclei
			programLogger.log(Level.FINE, "Assigning segments...");
			assignSegments(collection);
			
			// adjust the segments to better fit each nucleus
			programLogger.log(Level.FINE, "Revising segments...");
			reviseSegments(collection, pointType);		
	
			// update the aggregate in case any borders have changed
			collection.getProfileCollection(ProfileCollectionType.REGULAR).createProfileAggregate(collection);
						
			// At this point, the franken collection still contains tip/head values only
			
		} catch(Exception e){
			fileLogger.log(Level.SEVERE, "Error segmenting",e);
			programLogger.log(Level.SEVERE, "Error segmenting",e);
			collection.getProfileCollection(ProfileCollectionType.REGULAR).printKeys();
		}
		programLogger.log(Level.FINE, "Segmentation complete");
		fileLogger.log(Level.INFO, "Segmentation complete");
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

			fileLogger.log(Level.INFO, "Found "+segments.size()+" segments in "+collection.getPoint(BorderTag.REFERENCE_POINT)+" profile");

			// Add the segments to the collection
//			fileLogger.log(Level.FINE, "Adding segments to profile collection");
//			fileLogger.log(Level.FINE, NucleusBorderSegment.toString(segments));
			pc.addSegments(BorderTag.REFERENCE_POINT, segments);
		} catch(Exception e){
			fileLogger.log(Level.SEVERE, "Error creating segments", e);
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
			fileLogger.log(Level.INFO, "Assigning segments to nuclei...");

			ProfileCollection pc = collection.getProfileCollection(ProfileCollectionType.REGULAR);
			
//			fileLogger.log(Level.FINE, "Using regular profile collection for assignment");
//			fileLogger.log(Level.FINE, pc.toString());

			// find the corresponding point in each Nucleus
			SegmentedProfile median = pc.getSegmentedProfile(BorderTag.REFERENCE_POINT);

			for(Nucleus n : collection.getNuclei()){
				assignSegmentsToNucleus(n, median);
			}
			fileLogger.log(Level.INFO, "Segments assigned to nuclei");
		} catch(Exception e){
			fileLogger.log(Level.SEVERE, "Error assigning segments", e);
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
			fileLogger.log(Level.SEVERE, "Error assigning segments to nucleus",e);
		}

		
	}

	/**
	 * Update initial segment assignments by stretching each segment to the best possible fit along
	 * the median profile 
	 * @param collection
	 * @param pointType
	 */
	private void reviseSegments(CellCollection collection, BorderTag pointType){
		fileLogger.log(Level.INFO, "Refining segment assignments...");
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

			TODO: An error occurs in here somewhere. A frankenMedian for Testing
			 has put the frankenMedian reference point at the segment 5-0 boundary.
			 This is to the right of the orientation point.
			 
			 Analysis of the error: test the incoming profiles.


			 The profile aggregate is being given values that have the wrong offset;
			 these values will come from individual frankenProfiles
			 Therefore, the frankenProfiles have had their reference point wrongly assigned
 
			 */
			for(BorderTag key : pc.getOffsetKeys()){
				
				int offset = pc.getOffset(key);
				fileLogger.log(Level.FINE, "Adding franken collection offset "+offset+" for "+ key);
				frankenCollection.addOffset(key, pc.getOffset(key));
			}


			// copy the segments from the profile collection
			frankenCollection.addSegments(pointType, segments);
			
//			fileLogger.log(Level.FINE, "Franken profile collection");
//			fileLogger.log(Level.FINE, frankenCollection.toString());

			// At this point, the FrankenCollection is identical to the ProfileCollection
			// We need to add the individual recombined frankenProfiles


			// run the segment fitter on each nucleus
			SegmentFitter fitter = new SegmentFitter(pc.getSegmentedProfile(pointType), fileLogger);
			List<Profile> frankenProfiles = new ArrayList<Profile>(0);

			int count = 1;
			for(Nucleus n : collection.getNuclei()){ 
				fileLogger.log(Level.INFO, "Fitting nucleus "+n.getPathAndNumber()+" ("+count+" of "+collection.size()+")");
				fitter.fit(n, pc);

				// recombine the segments at the lengths of the median profile segments
				//TODO: When the frankenProfile is created, the reference point may be wrong
				Profile recombinedProfile = fitter.recombine(n, BorderTag.REFERENCE_POINT);
				frankenProfiles.add(recombinedProfile);
				count++;
				publish(count); // publish the progress to gui
			}

			// add all the nucleus frankenprofiles to the frankencollection
			frankenCollection.addNucleusProfiles(frankenProfiles);

			// update the profile aggregate
			frankenCollection.createProfileAggregateFromInternalProfiles((int)pc.getAggregate().length());
//			logger.log("FrankenProfile generated");
			double firstPoint = frankenCollection.getSegmentedProfile(BorderTag.REFERENCE_POINT).get(0);
			fileLogger.log(Level.INFO, "FrankenProfile generated: angle at index 0 for "+BorderTag.REFERENCE_POINT+" is "+firstPoint);
			// attach the frankencollection to the cellcollection
			collection.setProfileCollection(ProfileCollectionType.FRANKEN, frankenCollection);
			fileLogger.log(Level.INFO, "Segment assignments refined");
		} catch(Exception e){
			fileLogger.log(Level.SEVERE, "Error revising segments", e);
		}
	}
	
	
	public static class TailFinder {

		private static void findTailInRodentSpermMedian(CellCollection collection){
			try{
				// can't use regular tail detector, because it's based on NucleusBorderPoints
				// get minima in curve, then find the lowest minima / minima furthest from both ends
				collection.getProfileCollection(ProfileCollectionType.REGULAR).addOffset(BorderTag.REFERENCE_POINT, 0);

				Profile medianProfile = collection.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(BorderTag.REFERENCE_POINT, 50);

				BooleanProfile minima = medianProfile.smooth(2).getLocalMinima(5); // window size 5

				//		double minDiff = medianProfile.size();
				double minAngle = 180;
				int tailIndex = 0;

				int tipExclusionIndex1 = (int) (medianProfile.size() * 0.2);
				int tipExclusionIndex2 = (int) (medianProfile.size() * 0.6);

				for(int i = 0; i<minima.size();i++){
					if( minima.get(i)==true){
						int index = i;

						double angle = medianProfile.get(index);
						if(angle<minAngle && index > tipExclusionIndex1 && index < tipExclusionIndex2){ // get the lowest point that is not near the tip
							minAngle = angle;
							tailIndex = index;
						}
					}
				}

				collection.getProfileCollection(ProfileCollectionType.REGULAR).addOffset(BorderTag.ORIENTATION_POINT, tailIndex);

			} catch(Exception e){
				fileLogger.log(Level.SEVERE, "Error finding tail", e);
			}
		}

		private static void findTailInPigSpermMedian(CellCollection collection) throws Exception {
			
			// define the current zero offset at the reference point
			// It does not matter, it just gives an offset key for the ProfileCollection
			collection.getProfileCollection(ProfileCollectionType.REGULAR).addOffset(BorderTag.REFERENCE_POINT, 0);
			
			// get the profile
			// This is starting from an arbitrary point?
			// Starting from the head in test data, so the reference point is correct
			Profile medianProfile = collection.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(BorderTag.REFERENCE_POINT, 50);
//			medianProfile.print();

			// find local maxima in the median profile over 180
			BooleanProfile maxima = medianProfile.smooth(2).getLocalMaxima(5, 180); // window size 5, only values over 180


			double minAngle = 180;
			int tailIndex = 0;

			// do not consider maxima that are too close to the head of the sperm
			/*
			 * ERROR when head is defined near to tail by chance - we exclude the true tail
			 */
//			int tipExclusionIndex1 = (int) (medianProfile.size() * 0.2);
//			int tipExclusionIndex2 = (int) (medianProfile.size() * 0.6);

			if(maxima.size()==0){
				fileLogger.log(Level.SEVERE, "Error: no maxima found in median line");
				tailIndex = 100; // set to roughly the middle of the array for the moment

			} else{
				
				for(int i = 0; i<maxima.size();i++){
					
					if(maxima.get(i)==true){ // look at local maxima
						int index = i;

						double angle = medianProfile.get(index); // get the angle at this maximum
						
						// look for the highest local maximum outside the exclusion range
//						if(angle>minAngle && index > tipExclusionIndex1 && index < tipExclusionIndex2){
						if(angle>minAngle){
							minAngle = angle;
							tailIndex = index;
						}
					}
				}
			}

			// add this index to be the orientation point
			fileLogger.log(Level.FINE, "Setting tail to index: "+tailIndex);
			collection.getProfileCollection(ProfileCollectionType.REGULAR).addOffset(BorderTag.ORIENTATION_POINT, tailIndex);
			collection.getProfileCollection(ProfileCollectionType.REGULAR).addOffset(BorderTag.REFERENCE_POINT, tailIndex);
			/*
			 * Looks like reference point needs to be 0. Check the process aligning the profiles - they must be settling on 
			 * the RP 
			 */
			
			
			
			// set the reference point half way around from the tail
			double length = (double) collection.getProfileCollection(ProfileCollectionType.REGULAR).getAggregate().length();		
			int offset =  (int) Math.ceil(length / 2d); // ceil to ensure offsets are correct
			
			// now we have the tail point located, update the reference point to be opposite
//			fileLogger.log(Level.FINE, "Profile collection before intersection point re-index: ");
//			fileLogger.log(Level.FINE, collection.getProfileCollection(ProfileCollectionType.REGULAR).toString());
			
//			 adjust the index to the offset
			int headIndex  = Utils.wrapIndex( tailIndex - offset, collection.getProfileCollection(ProfileCollectionType.REGULAR).getAggregate().length());
//			fileLogger.log(Level.FINE, "Setting head to index: "+headIndex);
			collection.getProfileCollection(ProfileCollectionType.REGULAR).addOffset(BorderTag.INTERSECTION_POINT, headIndex);
		}
		

		private static void findTailInRoundMedian(CellCollection collection) throws Exception {
			
			collection.getProfileCollection(ProfileCollectionType.REGULAR).addOffset(BorderTag.REFERENCE_POINT, 0);
			ProfileCollection pc = collection.getProfileCollection(ProfileCollectionType.REGULAR);

			Profile medianProfile = pc.getProfile(BorderTag.REFERENCE_POINT, 50);

			int tailIndex = (int) Math.floor(medianProfile.size()/2);
			
			
			
			collection.getProfileCollection(ProfileCollectionType.REGULAR).addOffset(BorderTag.ORIENTATION_POINT, tailIndex);
		}

		/**
		 * Identify tail in median profile and offset nuclei profiles. For a 
		 * regular round nucleus, the tail is one of the points of longest
		 *  diameter, and lowest angle
		 * @param collection the nucleus collection
		 * @param nucleusClass the class of nucleus
		 */
		public static void findTailIndexInMedianCurve(CellCollection collection){

			try{

				if(collection.getNucleusType().equals(NucleusType.ROUND)){
					findTailInRoundMedian(collection);
				}

				if(collection.getNucleusType().equals(NucleusType.PIG_SPERM)){
					findTailInPigSpermMedian(collection);
				}

				if(collection.getNucleusType().equals(NucleusType.RODENT_SPERM)){
					findTailInRodentSpermMedian(collection);
				}

			} catch(Exception e){
				fileLogger.log(Level.SEVERE, "Error finding tail", e);
			}

		}
	}
	
	public static class Offsetter {
		
		private static void calculateOffsetsInRoundNuclei(CellCollection collection){
			
			try{
				Profile medianToCompare = collection.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(BorderTag.REFERENCE_POINT, 50); // returns a median profile with head at 0

				for(Nucleus n : collection.getNuclei()){

					// returns the positive offset index of this profile which best matches the median profile
					int newHeadIndex = n.getAngleProfile().getSlidingWindowOffset(medianToCompare);
					n.setBorderTag(BorderTag.REFERENCE_POINT, newHeadIndex);

					// check if flipping the profile will help

					double differenceToMedian1 = n.getAngleProfile(BorderTag.REFERENCE_POINT).absoluteSquareDifference(medianToCompare);
					n.reverse();
					double differenceToMedian2 = n.getAngleProfile(BorderTag.REFERENCE_POINT).absoluteSquareDifference(medianToCompare);

					if(differenceToMedian1<differenceToMedian2){
						n.reverse(); // put it back if no better
					}

					// also update the tail position
					int tailIndex = n.getIndex(n.findOppositeBorder( n.getPoint(newHeadIndex) ));
					n.setBorderTag(BorderTag.ORIENTATION_POINT, tailIndex);
				}
			}catch(Exception e){
				fileLogger.log(Level.SEVERE, "Error calculating offsets", e);
			}
		}

		
		private static void calculateOffsetsInRodentSpermNuclei(CellCollection collection){
			
			try{
				// Get the median profile starting from the orientation point
				Profile median = collection.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(BorderTag.ORIENTATION_POINT, 50); // returns a median profile

				// go through each nucleus
				for(Nucleus n : collection.getNuclei()){

					// ensure the correct class is chosen
					RodentSpermNucleus nucleus = (RodentSpermNucleus) n;

					// get the offset for the best fit to the median profile
					int newTailIndex = nucleus.getAngleProfile().getSlidingWindowOffset(median);

					// add the offset of the tail to the nucleus
					nucleus.setBorderTag(BorderTag.ORIENTATION_POINT, newTailIndex);
					

					// also update the head position (same as round reference point)
					// - the point opposite the tail through the CoM
					int headIndex = nucleus.getIndex(nucleus.findOppositeBorder( nucleus.getPoint(newTailIndex) ));
					nucleus.setBorderTag(BorderTag.REFERENCE_POINT, headIndex);
					nucleus.splitNucleusToHeadAndHump();
				}
			}catch(Exception e){
				fileLogger.log(Level.SEVERE, "Error calculating offsets", e);
			}
		}
		
		private static void calculateOffsetsInPigSpermNuclei(CellCollection collection) throws Exception {

			// get the median profile zeroed on the orientation point
			Profile medianToCompare = collection.getProfileCollection(ProfileCollectionType.REGULAR).getProfile(BorderTag.ORIENTATION_POINT, 50); 

			for(Nucleus nucleus : collection.getNuclei()){
				PigSpermNucleus n = (PigSpermNucleus) nucleus;

				// returns the positive offset index of this profile which best matches the median profile
				int tailIndex = n.getAngleProfile().getSlidingWindowOffset(medianToCompare);
				
				n.setBorderTag(BorderTag.ORIENTATION_POINT, tailIndex);

				
				// also update the head position
				int headIndex = n.getIndex(n.findOppositeBorder( n.getPoint(tailIndex) ));
				n.setBorderTag(BorderTag.REFERENCE_POINT, headIndex);
			}

		}
		
		/**
		 * Offset the position of the tail in each nucleus based on the difference to the median
		 * @param collection the nuclei
		 * @param nucleusClass the class of nucleus
		 */
		public static void calculateOffsets(CellCollection collection){

			try{

				if(collection.getNucleusType().equals(NucleusType.ROUND)){
					calculateOffsetsInRoundNuclei(collection);
				}

				if(collection.getNucleusType().equals(NucleusType.RODENT_SPERM)){
					calculateOffsetsInRodentSpermNuclei(collection);
				}

				if(collection.getNucleusType().equals(NucleusType.PIG_SPERM)){
					calculateOffsetsInPigSpermNuclei(collection);
				}
			}catch(Exception e){
				fileLogger.log(Level.SEVERE, "Error calculating offsets", e);
			}
		}
	}
}
