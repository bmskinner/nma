package no.analysis;

import ij.IJ;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import utility.Logger;
import utility.Utils;
import no.components.NucleusBorderSegment;
import no.components.Profile;
import no.nuclei.Nucleus;

/**
 * This takes a median profile plus segments, and a real profile plus segments from
 * a Nucleus, and tries to optimise the segment endpoints by moving the segment
 * boundaries. The segments are interpolated to match the corresponding median segment,
 * and a best fit score is calculated. This should help overcome the 'sensory homunculus'
 * problem with Yqdels in an otherwise WT population
 */
public class SegmentFitter {
	
	private static Logger logger;

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
	
	private final 	Profile medianProfile; // the profile to align against
	private 		Profile   testProfile; // the profile to adjust
	
	final 	List<NucleusBorderSegment> medianSegments;
	private List<NucleusBorderSegment>   testSegments;
	
	/**
	 * The number of points ahead and behind to test
	 * when creating new segment profiles
	 */
	private static int POINTS_TO_TEST = 20;
		
	/**
	 * Construct with a median profile and list of segments. The originals will not be modified
	 * @param medianProfile the profile
	 * @param medianSegments the list of segments within the profile
	 */
	public SegmentFitter(Profile medianProfile, List<NucleusBorderSegment> medianSegments, File logFile){
		if(medianProfile==null || medianSegments==null){
			logger.log("Segment list is null or empty", Logger.ERROR);
			throw new IllegalArgumentException("Median profile or segment list is null");
		}
		this.medianProfile  = new Profile(medianProfile);
		this.medianSegments = new ArrayList<NucleusBorderSegment>(0);
		for(NucleusBorderSegment seg : medianSegments){
			this.medianSegments.add(new NucleusBorderSegment(seg));
		}
		logger = new Logger(logFile, "SegmentFitter");
	}
	
	/**
	 * Run the segment fitter on the given nucleus. It will take the segments
	 * loaded into the fitter upon cosntruction, and apply them to the nucleus
	 * angle profile.
	 * @param n the nucleus to fit to the current median profile
	 */
	public void fit(Nucleus n){
		
		// Input checks
		if(n==null){
			logger.log("Test nucleus is null", Logger.ERROR);
			throw new IllegalArgumentException("Test nucleus is null");
		}
		if(n.getSegments()==null){
			logger.log("Nucleus has no segments", Logger.ERROR);
			throw new IllegalArgumentException("Nucleus has no segments");
		}
		
		this.testProfile = new Profile(n.getAngleProfile());
		this.testSegments = new ArrayList<NucleusBorderSegment>(0);
		
		
		// Make a copy of the segments in the nucleus
		copyExistingNucleusSegments(n);
		
		// Begin fitting the segments to the median
		logger.log("Fitting nucleus "+n.getPathAndNumber(), Logger.INFO);
		try{
			
			// test how well the nucleus profile matches the median
			double score = testProfile.differenceToProfile(medianProfile);
			
			// get the best fit of segments to the median
			List<NucleusBorderSegment> newList = this.runFitter(this.testSegments);
					
			logger.log("Start score: "+score, Logger.DEBUG);

			double prevScore = score+1; // so we don't immediately end the loop
			
			while(score<prevScore){
				newList = runFitter(newList);
				Profile revisedProfile = this.recombineSegments(newList, testProfile);
				prevScore = score;
				score = revisedProfile.differenceToProfile(medianProfile);
				logger.log("Score: "+score, Logger.DEBUG);

			}
			logger.log("Final score: "+score, Logger.DEBUG);
			n.setSegments(newList);
			
			// modify tail point to nearest segment end
//			this.remapBorderPoints(n, newList);
			logger.log("Fitted nucleus "+n.getPathAndNumber(), Logger.INFO);
			
		} catch(Exception e){
			logger.log("Error refitting segments: "+e.getMessage(), Logger.ERROR);
			for(StackTraceElement e1 : e.getStackTrace()){
				logger.log(e1.toString(), Logger.STACK);
			}
		}
	}
	
	/**
	 * Make a copy of the segments in the nucleus, and ensure
	 * they link up properly
	 * @param n the nucleus to copy
	 */
	private void copyExistingNucleusSegments(Nucleus n){
		logger.log("Copying nucleus segments");
		for(NucleusBorderSegment seg : n.getSegments()){
			this.testSegments.add(new NucleusBorderSegment(seg));
		}
		NucleusBorderSegment.linkSegments(testSegments);
		logger.log("Segments copied and linked");
	}
	
	/**
	 * Join the segments within the given nucleus into Frankenstein's Profile. 
	 * @param n the nucleus to recombine
	 * @return a profile
	 */
	public Profile recombine(Nucleus n){
		if(n==null){
			logger.log("Recombined nucleus is null", Logger.ERROR);
			throw new IllegalArgumentException("Test nucleus is null");
		}
		if(n.getSegments()==null){
			logger.log("Nucleus has no segments", Logger.ERROR);
			throw new IllegalArgumentException("Nucleus has no segments");
		}
		Profile testMedian = new Profile(n.getAngleProfile());
		List<NucleusBorderSegment> testSegments = n.getSegments();
		
		return new Profile(recombineSegments(testSegments, testMedian));
	}
	
	/**
	 * Move any border points to their closest segment end
	 * @param n the nucleus to fit
	 * @param list the segments in the nucleus
	 */
//	private void remapBorderPoints(INuclearFunctions n, List<NucleusBorderSegment> list){
//		
//		for(String pointTag : this.getFeatureKeys()){
//			
//		}
//				
//		for(String pointTag: n.getBorderTags().keySet()){
//			int index = n.getBorderTags().get(pointTag); 
//			
//			int nearestIndex = index;
//			int smallestDiff = n.getLength();
//			
//			for(NucleusBorderSegment seg : n.getSegments()){
//				int difference = Math.abs(index - seg.getStartIndex());
//				// find the best fitting segment start, so long as it is not too far
//				// needed to stop head being moved in rodent or pig sperm
//				if(difference < smallestDiff && difference <  (double)n.getLength()/4){
//					nearestIndex = seg.getStartIndex();
//					smallestDiff = difference;
//				}
//			}
//			n.addBorderTag(pointTag, nearestIndex);
//		}
//		
//	}
	
	/**
	 * Perform the recombination of segments from a nucleus. It takes each segment
	 * and interpolates it to the length of the corresponding median segment.
	 * @param testSegs the segments to adjust
	 * @param testMedian the median profile
	 * @return a profile constructed from the stretched segments
	 */
	private Profile recombineSegments(List<NucleusBorderSegment> segments, Profile median){
		
		// Input checking
		if(segments==null || segments.isEmpty()){
			throw new IllegalArgumentException("Segment list is null or empty");
		}
		if(median==null){
			throw new IllegalArgumentException("Test profile is null in recombiner");
		}
		logger.log("Recombining segments to FrankenProfile", Logger.DEBUG);
		
		
		// the profiles derived from each segment will be merged to a single 
		// profile at the end
		List<Profile> finalSegmentProfiles = new ArrayList<Profile>(0);

		// go through each segment
		for(int i=0; i<segments.size();i++){
			
			// The relevant segment from the median profile
			NucleusBorderSegment 	medianSegment = this.medianSegments.get(i);

			// Segments share endpoints
			// As a result, the last index should be removed from each segment for best fitting
			NucleusBorderSegment 	testSeg = segments.get(i);

			// get the region within the segment as a new profile
			Profile testSegProfile = median.getSubregion(testSeg);

			// interpolate the test segments to the length of the median segments
			Profile revisedProfile = testSegProfile.interpolate(medianSegment.length());
			
			// Put the new profile into the list
			finalSegmentProfiles.add(revisedProfile);
		}
		return Profile.merge(finalSegmentProfiles);
	}
	
	/**
	 * In progress version of fitter for 1.10.0
	 * @param testList
	 * @return
	 * @throws Exception 
	 */
	private List<NucleusBorderSegment> runFitter(List<NucleusBorderSegment> testList) throws Exception{
		// Input check
		if(testList==null || testList.isEmpty()){
			logger.log("Segment list is null or empty", Logger.ERROR);
			throw new IllegalArgumentException("Segment list is null or empty");
		}

		logger.log("Fitting list", Logger.DEBUG);
		
		// A new list to hold the fitted segments
		List<NucleusBorderSegment> result = new ArrayList<NucleusBorderSegment>(0);
		
		// we want to check every possible configuration of segmentation
		
		// it should be a whole-segmentation based comparison
		// Whenever a change is made, assess the impact on the entire profile comparison, segment to segment
		
		// see the initial state
		double bestScore = 0;
		try{
			bestScore = compareSegmentationPatterns(testList, medianSegments);
		}catch(IllegalArgumentException e){
			logger.log(e.getMessage());
			throw new Exception("Error getting segmentation pattern: "+e.getMessage());
		}
		
		for(int changeValue = -POINTS_TO_TEST; changeValue<POINTS_TO_TEST; changeValue++){
			
			//	change the length from +length to -length, testing the effect at each step
//		 	rotate the segment set through the nucleus, testing position with new lengths
			// keep only the effects which reduce score
			
			double score = compareSegmentationPatterns(testList, medianSegments);
			logger.log("Change: "+changeValue+"\tScore: "+score, Logger.DEBUG);
			// For each segment:
			for(int i=0; i<testList.size();i++){
				
				NucleusBorderSegment segment = testList.get(i);
				
				if(segment.lengthenEnd(changeValue)){ // not permitted if it violates length constraint

					
					try{
						score = compareSegmentationPatterns(testList, medianSegments);
					}catch(IllegalArgumentException e){
						logger.log(e.getMessage());
						throw new Exception("Error getting segmentation pattern: "+e.getMessage());
					}
					
					bestScore = score < bestScore ? score : bestScore;
					result = score < bestScore ? testList : result;
					
					List<NucleusBorderSegment> newList = NucleusBorderSegment.nudge(testList, changeValue);
					try{
						score = compareSegmentationPatterns(testList, medianSegments);
					}catch(IllegalArgumentException e){
						logger.log(e.getMessage());
						throw new Exception("Error getting segmentation pattern: "+e.getMessage());
					}
					bestScore = score < bestScore ? score : bestScore;
					result = score < bestScore ? newList : result;
				}
			}
		}
		logger.log("Best score: "+bestScore, Logger.DEBUG);
		logger.log("Fitted segments to nucleus", Logger.DEBUG);
		return result;
	}
		
	/**
	 * for each test segment: compare with median segment
	 *	increase or decrease the test endpoint
	 *  score again
	 *  get the lowest score within ?10 border points either side
	 *  next segment
	 *  update the nucleus
	 */
//	private List<NucleusBorderSegment> runFitter(List<NucleusBorderSegment> testList){
//		
//		// Input check
//		if(testList==null || testList.isEmpty()){
//			logger.log("Segment list is null or empty", Logger.ERROR);
//			throw new IllegalArgumentException("Segment list is null or empty");
//		}
//		
//		logger.log("Fitting list", Logger.DEBUG);
//
//		// A new list to hold the fitted segments
//		List<NucleusBorderSegment> result = new ArrayList<NucleusBorderSegment>(0);
//
//		// the number of segments input
//		int segmentCount  = testList.size();
//		
//		
//
//		for(int i=0; i<segmentCount;i++){
//			
//			// make a copy of the test list of segments, so we can interrogate each
//			// segment separately without distorting the entire set via linkage
//			List<NucleusBorderSegment> tempList = new ArrayList<NucleusBorderSegment>(0);
//			for(NucleusBorderSegment segment : testList){
//				tempList.add(new NucleusBorderSegment(segment));
//			}
//			NucleusBorderSegment.linkSegments(tempList);
//			
//			// now get the appropriate segment from teh temp list
//					
//			NucleusBorderSegment seg = tempList.get(i);
//			int oldLength = seg.length();
//			
////			logger.log("Fitting segment "+i+": "+seg.getSegmentType(), Logger.DEBUG);
//			
//						
//			double score =  compareSegments(this.medianSegments.get(i), seg);
//			
////			logger.log("Score "+score, Logger.DEBUG);
//			
//			
//			double minScore = score;
//			int valueChange = 0;
//			NucleusBorderSegment bestSeg = seg;
//			
//			// TODO: allow rotation through the entire profile
//			for(int j=-SegmentFitter.POINTS_TO_TEST;j<SegmentFitter.POINTS_TO_TEST;j++){
//				
//				// before we try lengthening or shortening segments, see if they fit better
//				// at the same size, but moved along a bit
//				List<NucleusBorderSegment> nudgeList = NucleusBorderSegment.nudge(tempList, j);
//
//				// only refit if the change does not shrink the segment too much
//				if(seg.length()+j > NucleusBorderSegment.MINIMUM_SEGMENT_LENGTH){
//					
////					logger.log("Testing segment length change of "+j, Logger.DEBUG);
//					
//					// make a copy of the segment to work with
//					NucleusBorderSegment tempSeg = new NucleusBorderSegment(seg);
//					
//					try{
//						// try to lengthen the new segment by the given amount
//						if(tempSeg.lengthenEnd(j)){
//							
//							// If the change suceeded, get the score
//							// logger.log("Adjusted segment length", Logger.DEBUG);
//
//							// get the score for the new segment
//							score = compareSegments(this.medianSegments.get(i), tempSeg);
//
//							// add a penalty for each point that makes the segment longer
//							if(tempSeg.length()>oldLength){
//								score += (tempSeg.length()-oldLength) * SegmentFitter.PENALTY_GROW ;
//							}
//
//							// add a penalty if the proposed new segment is shorter that the minimum segment length
//							//						if(tempSeg.length()<NucleusBorderSegment.MINIMUM_SEGMENT_LENGTH){
//							//							// penalty increases the smaller we go below the minimum
//							//							score += (NucleusBorderSegment.MINIMUM_SEGMENT_LENGTH - tempSeg.length()) * SegmentFitter.PENALTY_SHRINK;
//							//						}
//
//							//						logger.log("New score: "+score, Logger.DEBUG);
//
//							if(score<minScore){
//								minScore=score;
//								bestSeg = tempSeg;
//								valueChange = j;
//							}
//						}
//
//					} catch(IllegalArgumentException e){
//						logger.log("Error in arguments: "+e.getMessage(), Logger.ERROR);
//					}
//
//				}	
//			}
//			
//			logger.log("Min score found as "+minScore+" with offset "+valueChange, Logger.DEBUG);
//			logger.log(bestSeg.toString(), Logger.INFO);
//
////			testList.get(i).update(bestSeg.getStartIndex(), bestSeg.getEndIndex());
//			result.add(bestSeg);
//
//
//		}
//		return result;
//	}
		
	/**
	 * Compare two segments in the same profile
	 * @param reference the reference segment
	 * @param test the segment to test
	 * @return the sum of square differences between the segments
	 */
	private double compareSegments(NucleusBorderSegment reference, NucleusBorderSegment test){
		if(reference == null){
			throw new IllegalArgumentException("Reference segment is null or empty");
		}
		if(test == null){
			throw new IllegalArgumentException("Test segment is null or empty");
		}
		
		Profile refProfile  = this.medianProfile.getSubregion(reference);
		Profile subjProfile = this.medianProfile.getSubregion(test);
		
		return refProfile.differenceToProfile(subjProfile);
	}
	
	/**
	 * Get the score for an entire segment list of a profile. Tests the effect of  changing one segment
	 * on the entire set
	 * @param reference
	 * @param test
	 * @return the score
	 */
	private double  compareSegmentationPatterns(List<NucleusBorderSegment> reference, List<NucleusBorderSegment> test){
		if(reference == null || reference.isEmpty()){
			throw new IllegalArgumentException("Reference segment list is null or empty");
		}
		if(test == null || test.isEmpty()){
			throw new IllegalArgumentException("Test segment list is null or empty");
		}
		
		if(reference.size()!=test.size()){
			throw new IllegalArgumentException("Lists are of different lengths");
		}
		
		double result = 0;
		for( int i=0; i<reference.size(); i++){
			result += compareSegments(reference.get(i), test.get(i));
		}
		return result;
	}
}
