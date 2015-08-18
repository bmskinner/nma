package no.analysis;

import ij.IJ;

import java.util.ArrayList;
import java.util.List;

import utility.Constants;
import utility.Logger;
import no.collections.CellCollection;
import no.components.NucleusBorderSegment;
import no.components.Profile;
import no.components.ProfileCollection;
import no.components.SegmentedProfile;
import no.nuclei.Nucleus;
import no.nuclei.RoundNucleus;
import no.nuclei.sperm.PigSpermNucleus;
import no.nuclei.sperm.RodentSpermNucleus;

/**
 * This is the core of the morphology analysis pipeline.
 * It is the only module that is essential. This offsets nucleus profiles,
 * generates the median profiles, segments them, and applies the segments to
 * nuclei.
 */
public class MorphologyAnalysis {
	
	private static Logger logger;
	
	public static boolean run(CellCollection collection){

		logger = new Logger(collection.getDebugFile(), "MorphologyAnalysis");
		try{

			logger.log("Beginning core morphology analysis");

			String pointType = collection.getReferencePoint();

			// profile the collection from head/tip, then apply to tail
			runProfiler(collection, pointType);

			// segment the profiles from head
			runSegmentation(collection, pointType);

			logger.log("Core morphology analysis complete");
			return true;
			
		} catch(Exception e){
			
			logger.log("Error in morphology analysis: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
			
			logger.log("Collection keys:", Logger.ERROR);
			logger.log(collection.getProfileCollection().printKeys(), Logger.ERROR);
			
			logger.log("FrankenCollection keys:", Logger.ERROR);
			logger.log(collection.getFrankenCollection().printKeys(), Logger.ERROR);
			return false;
		}

	}

	/**
	 * Calculaate the median profile of the colleciton, and generate the
	 * best fit offsets of each nucleus to match
	 * @param collection
	 * @param pointType
	 */
	private static void runProfiler(CellCollection collection, String pointType){
		
		ProfileCollection pc = collection.getProfileCollection();
		
		// default is to make profile aggregate from reference point
		pc.createProfileAggregate(collection);
		
		// use the median profile of this aggregate to find the orientation point ("tail")
		TailFinder.findTailIndexInMedianCurve(collection);
		
		// carry out iterative offsetting to refine the orientation point estimate
		double score = compareProfilesToMedian(collection, pointType);
		double prevScore = score+1;
		while(score < prevScore){
			
			// rebuild the aggregate - needed if the orientaion point index has changed in any nuclei
			pc.createProfileAggregate(collection);
			
			// carry out the orientation point detection in the median again
			TailFinder.findTailIndexInMedianCurve(collection);
			
			// apply offsets to each nucleus in the collection
			Offsetter.calculateOffsets(collection); 

			prevScore = score;
			
			// get the difference between aligned profiles and the median
			score = compareProfilesToMedian(collection, pointType);
			logger.log("Reticulating splines: score: "+(int)score);
		}
	}
		
	/**
	 * When a population needs to be reanalysed do not offset nuclei or recalculate best fits;
	 * just get the new median profile 
	 * @param collection the collection of nuclei
	 * @param sourceCollection the collection with segments to copy
	 */
	public static boolean reapplyProfiles(CellCollection collection, CellCollection sourceCollection){
		
		logger = new Logger(collection.getDebugFile(), "MorphologyAnalysis");
		logger.log("Applying existing segmentation profile to population...");
		
		try {
			String referencePoint   = collection.getReferencePoint();
			String orientationPoint = collection.getOrientationPoint();
			
			// use the same array length as the source collection to avoid segment slippage
			int profileLength = sourceCollection.getProfileCollection().getProfile(referencePoint).size();
			
			// get the empty profile collection from the new CellCollection
			ProfileCollection pc = collection.getProfileCollection();
			
			// make an aggregate from the nuclei. A new median profile must necessarily result.
			// By default, the aggregates are created from the reference point
			pc.createProfileAggregate(collection, profileLength);
			
			// copy the offset keys from the source collection
			ProfileCollection sc = sourceCollection.getProfileCollection();
			
			for(String offsetKey : sc.getOffsetKeys()){
				int offset = sc.getOffset(offsetKey);
				pc.addOffset(offsetKey, offset);
				logger.log("Setting "+offsetKey+" to "+offset);
			}
			
			
			// What happens when the array length is greater in the source collection? 
			// Segments are added that no longer have an index
			// We need to scale the segments to the array length of the new collection
			pc.addSegments(sc.getSegments(referencePoint));

			
			// At this point the collection has only a regular profile collection.
			// No Frankenprofile has been copied.

			reviseSegments(collection, referencePoint);	



		} catch (Exception e) {
			logger.log("Error reapplying profiles: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement el : e.getStackTrace()){
				logger.log(el.toString(), Logger.STACK);
			}
			return false;
		}
		logger.log("Re-profiling complete");
		return true;
	}
	
	/**
	 * Get the total differences to the median for all the nuclei in
	 * the collection
	 * @param collection
	 * @param pointType
	 * @return
	 */
	private static double compareProfilesToMedian(CellCollection collection, String pointType){
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
	private static void runSegmentation(CellCollection collection, String pointType){
		logger.log("Beginning segmentation...");
		try{	
			
			// generate segments in the median profile
			createSegments(collection);
			
			// map the segments from the median directly onto the nuclei
			assignSegments(collection);
			
			// adjust the segments to better fit each nucleus
			reviseSegments(collection, pointType);		
	
			// update the aggregate in case any borders have changed
			collection.getProfileCollection().createProfileAggregate(collection);
						
			// At this point, the franken collection still contains tip/head values only
			
		} catch(Exception e){
			logger.log("Error segmenting: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement e1 : e.getStackTrace()){
				logger.log(e1.toString(), Logger.STACK);
			}
			collection.getProfileCollection().printKeys();
		}
		logger.log("Segmentation complete");
	}
	
	/**
	 * Run the segmenter on the median profile for the given point type
	 * @param collection
	 */
	private static void createSegments(CellCollection collection){
		
		ProfileCollection pc = collection.getProfileCollection();

		// the reference point is always index 0, so the segments will match
		// the profile
		Profile median = pc.getProfile(collection.getReferencePoint());

		ProfileSegmenter segmenter = new ProfileSegmenter(median);		  
		List<NucleusBorderSegment> segments = segmenter.segment();

		logger.log("Found "+segments.size()+" segments in "+collection.getReferencePoint()+" profile");
		
		// Add the segments to the collection
		pc.addSegments(segments);
	}

	/**
	 * From the calculated median profile segments, assign segments to each nucleus
	 * based on the best offset fit of the start and end indexes 
	 * @param collection
	 * @param pointType
	 */
	private static void assignSegments(CellCollection collection){

		logger.log("Assigning segments to nuclei...");
		
		ProfileCollection pc = collection.getProfileCollection();

		// find the corresponding point in each Nucleus
		SegmentedProfile median = pc.getSegmentedProfile(collection.getReferencePoint());
		
		for(Nucleus n : collection.getNuclei()){
			assignSegmentsToNucleus(n, median);
		}
		logger.log("Segments assigned to nuclei");
	}
	
	/**
	 * Assign the given segments to the nucleus, finding the best match of the nucleus
	 * profile to the median profile
	 * @param n the nucleus to segment
	 * @param median the segmented median profile
	 */
	private static void assignSegmentsToNucleus(Nucleus n, SegmentedProfile median){
				
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
	}

	/**
	 * Update initial segment assignments by stretching each segment to the best possible fit along
	 * the median profile 
	 * @param collection
	 * @param pointType
	 */
	private static void reviseSegments(CellCollection collection, String pointType){
		logger.log("Refining segment assignments...");
		
		ProfileCollection pc = collection.getProfileCollection();
		List<NucleusBorderSegment> segments = pc.getSegments(pointType);
		
		// make a new profile collection to hold the frankendata
		ProfileCollection frankenCollection = new ProfileCollection();

		// create a new profile aggregate for the collection
		frankenCollection.createProfileAggregate(collection);
		
		
		// add the correct offset keys
		// These are the same as the profile collection keys, and have
		// the same positions (since a franken profile is based on the median)
		for(String key : pc.getOffsetKeys()){
			frankenCollection.addOffset(key, pc.getOffset(key));
		}

		// copy the segments from the profile collection
		frankenCollection.addSegments(segments);

		
		// run the segment fitter on each nucleus
		SegmentFitter fitter = new SegmentFitter(pc.getSegmentedProfile(pointType), logger.getLogfile());
		List<Profile> frankenProfiles = new ArrayList<Profile>(0);

		for(Nucleus n : collection.getNuclei()){ 
			fitter.fit(n);

			// recombine the segments at the lengths of the median profile segments
			// what does it look like?
			Profile recombinedProfile = fitter.recombine(n);
			frankenCollection.getAggregate().addValues(recombinedProfile);
			frankenProfiles.add(recombinedProfile);
		}
		
		// add all the nucleus frankenprofiles to the frankencollection
		frankenCollection.addNucleusProfiles(frankenProfiles);
		
		// update the profile aggregate
		frankenCollection.createProfileAggregate(  collection   );
		
		// attach the frankencollection to the cellcollection
		collection.setFrankenCollection(frankenCollection);
		logger.log("Segment assignments refined");
	}
	
	
	public static class TailFinder {

		private static void findTailInRodentSpermMedian(CellCollection collection){
			// can't use regular tail detector, because it's based on NucleusBorderPoints
			// get minima in curve, then find the lowest minima / minima furthest from both ends
			collection.getProfileCollection().addOffset(collection.getReferencePoint(), 0);

			Profile medianProfile = collection.getProfileCollection().getProfile(collection.getReferencePoint());

			Profile minima = medianProfile.smooth(2).getLocalMinima(5); // window size 5

			//		double minDiff = medianProfile.size();
			double minAngle = 180;
			int tailIndex = 0;

			int tipExclusionIndex1 = (int) (medianProfile.size() * 0.2);
			int tipExclusionIndex2 = (int) (medianProfile.size() * 0.6);

			for(int i = 0; i<minima.size();i++){
				if( (int)minima.get(i)==1){
					int index = i;

					double angle = medianProfile.get(index);
					if(angle<minAngle && index > tipExclusionIndex1 && index < tipExclusionIndex2){ // get the lowest point that is not near the tip
						minAngle = angle;
						tailIndex = index;
					}
				}
			}
//			Profile tailProfile = medianProfile.offset(tailIndex);
			collection.getProfileCollection().addOffset(collection.getOrientationPoint(), tailIndex);
			
//			collection.getProfileCollection().addProfile(collection.getOrientationPoint(), tailProfile);
//			collection.getProfileCollection()
//			.addFeature(collection.getReferencePoint(), 
//					new ProfileFeature(collection.getOrientationPoint(), tailIndex)
//					); // set the tail-index in the tip normalised profile
		}

		private static void findTailInPigSpermMedian(CellCollection collection){
			
			collection.getProfileCollection().addOffset(collection.getReferencePoint(), 0);
			Profile medianProfile = collection.getProfileCollection().getProfile(collection.getReferencePoint());

			Profile minima = medianProfile.getLocalMaxima(5); // window size 5

			//    double minDiff = medianProfile.size();
			double minAngle = 180;
			int tailIndex = 0;

			int tipExclusionIndex1 = (int) (medianProfile.size() * 0.2);
			int tipExclusionIndex2 = (int) (medianProfile.size() * 0.6);

			if(minima.size()==0){
				IJ.log("    Error: no minima found in median line");
				tailIndex = 100; // set to roughly the middle of the array for the moment

			} else{

				for(int i = 0; i<minima.size();i++){
					if(minima.get(i)==1){
						int index = (int)minima.get(i);

						//          int toEnd = medianProfile.size() - index;
						//          int diff = Math.abs(index - toEnd);

						double angle = medianProfile.get(index);
						if(angle>minAngle && index > tipExclusionIndex1 && index < tipExclusionIndex2){ // get the lowest point that is not near the tip
							minAngle = angle;
							tailIndex = index;
						}
					}
				}
			}

			
			collection.getProfileCollection().addOffset(collection.getOrientationPoint(), tailIndex);
		}

		private static void findTailInRoundMedian(CellCollection collection){
			
			collection.getProfileCollection().addOffset(collection.getReferencePoint(), 0);
			ProfileCollection pc = collection.getProfileCollection();

			Profile medianProfile = pc.getProfile(collection.getReferencePoint());

			int tailIndex = (int) Math.floor(medianProfile.size()/2);
			
			
			
			collection.getProfileCollection().addOffset(collection.getOrientationPoint(), tailIndex);

//			Profile tailProfile = medianProfile.offset(tailIndex);
//			pc.addProfile(collection.getOrientationPoint(), tailProfile);
//			pc.addFeature(collection.getReferencePoint(), new ProfileFeature(collection.getOrientationPoint(), tailIndex));

		}

		/**
		 * Identify tail in median profile and offset nuclei profiles. For a 
		 * regular round nucleus, the tail is one of the points of longest
		 *  diameter, and lowest angle
		 * @param collection the nucleus collection
		 * @param nucleusClass the class of nucleus
		 */
		public static void findTailIndexInMedianCurve(CellCollection collection){

			if(collection.getNucleusClass() == RoundNucleus.class){
				findTailInRoundMedian(collection);
			}

			if(collection.getNucleusClass() == PigSpermNucleus.class){
				findTailInPigSpermMedian(collection);
			}

			if(collection.getNucleusClass() == RodentSpermNucleus.class){
				findTailInRodentSpermMedian(collection);
			}
		}

	}
	
	public static class Offsetter {
		
		private static void calculateOffsetsInRoundNuclei(CellCollection collection){
			Profile medianToCompare = collection.getProfileCollection().getProfile(collection.getReferencePoint()); // returns a median profile with head at 0

			for(int i= 0; i<collection.getNucleusCount();i++){ // for each roi
				Nucleus n = collection.getCell(i).getNucleus();

				// returns the positive offset index of this profile which best matches the median profile
				int newHeadIndex = n.getAngleProfile().getSlidingWindowOffset(medianToCompare);
				n.addBorderTag(collection.getReferencePoint(), newHeadIndex);

				// check if flipping the profile will help

				double differenceToMedian1 = n.getAngleProfile(collection.getReferencePoint()).absoluteSquareDifference(medianToCompare);
				n.reverse();
				double differenceToMedian2 = n.getAngleProfile(collection.getReferencePoint()).absoluteSquareDifference(medianToCompare);

				if(differenceToMedian1<differenceToMedian2){
					n.reverse(); // put it back if no better
				}

				// also update the tail position
				int tailIndex = n.getIndex(n.findOppositeBorder( n.getPoint(newHeadIndex) ));
				n.addBorderTag(Constants.Nucleus.ROUND.orientationPoint(), tailIndex);
			}
		}
		
		
		private static void calculateOffsetsInRodentSpermNuclei(CellCollection collection){
			
			// Get the median profile starting from the orientation point
			Profile median = collection.getProfileCollection().getProfile(collection.getOrientationPoint()); // returns a median profile

			// go through each nucleus
			for(Nucleus n : collection.getNuclei()){
				
				// ensure the correct class is chosen
				RodentSpermNucleus nucleus = (RodentSpermNucleus) n;

				// get the offset for the best fit to the median profile
				int newTailIndex = nucleus.getAngleProfile().getSlidingWindowOffset(median);

				// add the offset of the tail to the nucleus
				nucleus.addBorderTag(collection.getOrientationPoint(), newTailIndex);

				// also update the head position - the point opposite the tail through the CoM
				int headIndex = nucleus.getIndex(nucleus.findOppositeBorder( nucleus.getPoint(newTailIndex) ));
				nucleus.addBorderTag("head", headIndex);
				nucleus.splitNucleusToHeadAndHump();
			}
		}
		
		private static void calculateOffsetsInPigSpermNuclei(CellCollection collection){
			Profile medianToCompare = collection.getProfileCollection().getProfile(collection.getReferencePoint()); // returns a median profile with head at 0

			for(int i= 0; i<collection.getNucleusCount();i++){ // for each roi
				PigSpermNucleus n = (PigSpermNucleus)collection.getCell(i).getNucleus();

				// returns the positive offset index of this profile which best matches the median profile
				int newHeadIndex = n.getAngleProfile().getSlidingWindowOffset(medianToCompare);

				n.addBorderTag(Constants.Nucleus.PIG_SPERM.referencePoint(), newHeadIndex);

				// also update the head position
				int tailIndex = n.getIndex(n.findOppositeBorder( n.getPoint(newHeadIndex) ));
				n.addBorderTag(Constants.Nucleus.PIG_SPERM.orientationPoint(), tailIndex);
			}
		}
		
		/**
		 * Offset the position of the tail in each nucleus based on the difference to the median
		 * @param collection the nuclei
		 * @param nucleusClass the class of nucleus
		 */
		public static void calculateOffsets(CellCollection collection){

			if(collection.getNucleusClass() == RoundNucleus.class){
				calculateOffsetsInRoundNuclei(collection);
			}
			
			if(collection.getNucleusClass() == RodentSpermNucleus.class){
				calculateOffsetsInRodentSpermNuclei(collection);
			}
			
			if(collection.getNucleusClass() == PigSpermNucleus.class){
				calculateOffsetsInPigSpermNuclei(collection);
			}
		}
	}
}
