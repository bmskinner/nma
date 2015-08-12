package no.analysis;

import ij.IJ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

	/**
	 * The multiplier to add to best-fit scores when shrinking a segment below the 
	 * minimum segment size specified in ProfileSegmenter
	 */
	static final double PENALTY_SHRINK = 1.5;
	
	/**
	 * The multiplier to add to best-fit scores when shrinking a segment above the 
	 * median segment size
	 */
	static final double PENALTY_GROW   = 1;
	
	private Profile medianProfile; // the profile to align against
	private Profile   testProfile; // the profile to adjust
	
	List<NucleusBorderSegment> medianSegments;
	List<NucleusBorderSegment>   testSegments;
	
	/**
	 * The number of points ahead and behind to test
	 * when creating new segment profiles
	 */
	private static int POINTS_TO_TEST = 50;
	
	/**
	 * The smallest number of points a segment can contain. 
	 */
//	private static int MIN_SEGMENT_SIZE = 5;
	
	/**
	 * Construct with a median profile and list of segments. The originals will not be modified
	 * @param medianProfile the profile
	 * @param medianSegments the list of segments within the profile
	 */
	public SegmentFitter(Profile medianProfile, List<NucleusBorderSegment> medianSegments){
		if(medianProfile==null || medianSegments==null){
			throw new IllegalArgumentException("Median profile or segment list is null");
		}
		this.medianProfile  = new Profile(medianProfile);
		this.medianSegments = new ArrayList<NucleusBorderSegment>(0);
		for(NucleusBorderSegment seg : medianSegments){
			this.medianSegments.add(new NucleusBorderSegment(seg));
		}
	}
	
	/**
	 * Run the segment fitter on the given nucleus
	 * @param n the nucleus to fit to the current median profile
	 */
	public void fit(Nucleus n){
		if(n==null){
			throw new IllegalArgumentException("Test nucleus is null");
		}
		if(n.getSegments()==null){
			throw new IllegalArgumentException("Nucleus has no segments");
		}
		
		this.testProfile = new Profile(n.getAngleProfile());
		this.testSegments = new ArrayList<NucleusBorderSegment>(0);
		
		for(NucleusBorderSegment seg : n.getSegments()){
			this.testSegments.add(new NucleusBorderSegment(seg));
		}
		
		try{
			List<NucleusBorderSegment> newList = this.runFitter(this.testSegments);
			
	//		Profile revisedProfile = this.recombineSegments(newList, testProfile);
//			IJ.log(n.getImageName()+"-"+n.getNucleusNumber());
			double score = testProfile.differenceToProfile(medianProfile);
//			IJ.log("Start score: "+score);
			double prevScore = score+1;
			while(score<prevScore){
				newList = runFitter(newList);
				Profile revisedProfile = this.recombineSegments(newList, testProfile);
				prevScore = score;
				score = revisedProfile.differenceToProfile(medianProfile);
//				IJ.log("Score: "+score);
			}
//			IJ.log("Final score: "+score);
			n.setSegments(newList);
			
			// modify tail point to nearest segment end
//			this.remapBorderPoints(n, newList);
			
		} catch(Exception e){
			IJ.log("    Error refitting segments: "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	 * Join the segmnets within the given nucleus into Frankenstein's Profile 
	 * @param n the nucleus to recombine
	 * @return a profile
	 */
	public Profile recombine(Nucleus n){
		if(n==null){
			throw new IllegalArgumentException("Test nucleus is null");
		}
		if(n.getSegments()==null){
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
	
	private Profile recombineSegments(List<NucleusBorderSegment> testSegs, Profile testMedian){
		if(testSegs==null || testSegs.isEmpty()){
			throw new IllegalArgumentException("Segment list is null or empty");
		}
		if(testMedian==null){
			throw new IllegalArgumentException("Test profile is null in recombiner");
		}
		List<Profile> finalSegmentProfiles = new ArrayList<Profile>(0);

		// go through each segment
		for(int i=0; i<testSegs.size();i++){
			NucleusBorderSegment targetSeg = this.medianSegments.get(i);

			// we may need to trim out the last element, because the segments share endpoints
			NucleusBorderSegment   testSeg = testSegs.get(i);

			// interpolate the test segments to the length of the median segments
			Profile testSegProfile = this.getSegmentProfile(testSeg, testMedian);
			Profile revisedProfile = testSegProfile.interpolate(targetSeg.length());
			finalSegmentProfiles.add(revisedProfile);
		}
		return this.merge(finalSegmentProfiles);
	}
	
	/**
	 * Given a list of ordered profiles, merge them into one 
	 * contiguous profile
	 * @param list the list of profiles to merge
	 * @return the merged profile
	 */
	private Profile merge(List<Profile> list){
		if(list==null || list.size()==0){
			throw new IllegalArgumentException("Profile list is null or empty");
		}
		Profile result = new Profile(new double[0]);
		List<Double> combinedList = new ArrayList<Double>(0);
		
		for(Profile p : list){
			double[] values = p.asArray();
			List<Double> valueList = Arrays.asList(Utils.getDoubleFromdouble(values));
			combinedList.addAll(valueList);
		}
		
		Double[] combinedArray = (Double[]) combinedList.toArray(new Double[0]);
		result = new Profile(Utils.getdoubleFromDouble(combinedArray));
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
	private List<NucleusBorderSegment> runFitter(List<NucleusBorderSegment> testList){

		List<NucleusBorderSegment> newList = new ArrayList<NucleusBorderSegment>(0);
		int profileLength = this.testProfile.size();
		int segmentCount  = testList.size();
		
//		IJ.log("");
		for(int i=0; i<segmentCount;i++){
//			IJ.log("    Segment "+i);			
			NucleusBorderSegment seg = testList.get(i);
			int oldLength = seg.length();
			
//			NucleusBorderSegment prevSeg = new NucleusBorderSegment(newList.get(  Utils.wrapIndex(i-1, segmentCount)  ));
			NucleusBorderSegment prevSeg = i==0 ? new NucleusBorderSegment(testList.get(segmentCount-1)) : new NucleusBorderSegment(newList.get( i-1 ));
			
//			seg.print();
			// basic checks - is the segment long enough, does it wrap, offset from the previous fitting
			seg = preprocessSegment(seg, prevSeg);
						
			double score =  compareSegments(this.medianSegments.get(i), seg);
			double minScore = score;
			NucleusBorderSegment bestSeg = seg;
			
			// TODO: allow rotation through the entire profile
//			for(int j=0;j<this.testProfile.size();j++){
			for(int j=0;j<SegmentFitter.POINTS_TO_TEST;j++){
				
				// make the new segment
				int newEndIndex = Utils.wrapIndex(seg.getEndIndex()+j, profileLength);
				NucleusBorderSegment newSeg = new NucleusBorderSegment(seg.getStartIndex(), newEndIndex, profileLength);
				newSeg.setSegmentType("Seg_"+i);
				
				int newLength = newSeg.length();
								
				// get the score for the new segment
				score = compareSegments(this.medianSegments.get(i), newSeg);
				
				// add a penalty for each point that makes the segment longer
				if(newLength>oldLength){
					score += (newLength-oldLength) * SegmentFitter.PENALTY_GROW ;
				}
				
				// add a penalty if the proposed new segment is shorter that the minimum segment length
				if(newLength<ProfileSegmenter.MIN_SEGMENT_SIZE){
					// penalty increases the smaller we go below the minimum
					score += (ProfileSegmenter.MIN_SEGMENT_SIZE - newLength) * SegmentFitter.PENALTY_SHRINK;
				}
				
				if(score<minScore){
					minScore=score;
					bestSeg = newSeg;
				}
				
//				IJ.log("      Endpoint offset "+j+": "+score);	
			}
			newList.add(bestSeg);
			if(i==segmentCount-1){ 
				// this is the last segment; 
				// set the start index of the first segment to be
				// the end index for this segment
				newList.set(0, new NucleusBorderSegment(bestSeg.getEndIndex(), newList.get(0).getEndIndex(), profileLength));
				newList.get(0).setSegmentType("Seg_"+0);
				
			}
//			bestSeg.print();	

		}
		return newList;
	}
	
	/**
	 * Basic preprocessing of a segment to be fitted
	 * @param seg the segment to process
	 * @param prev the previous segment in the list
	 * @return the process segment for fitting
	 */
	private NucleusBorderSegment preprocessSegment(NucleusBorderSegment seg, NucleusBorderSegment prev){
		if(seg==null || prev==null){
			throw new IllegalArgumentException("Current of previous segment is null");
		}
		
		NucleusBorderSegment unalteredSeg = new NucleusBorderSegment(seg);
		int segLength = seg.length();
		
		// carry over the offset from the previous segment
		seg = new NucleusBorderSegment(prev.getEndIndex(), seg.getEndIndex(), unalteredSeg.getTotalLength());

		// if the endpoint has ended up before the start point, the segment was not originally wrapping,
		// correct the posiiton
		if(seg.getEndIndex()<seg.getStartIndex() && !unalteredSeg.contains(0)){

			// the new end point is the start point, plus half the minimum segment size
			// The penalty for segments < MIN_SEGMENT_SIZE should bring this up again, but
			// if there really is a shrinkage, this will hit on it faster
			int newEndIndex = Utils.wrapIndex(seg.getStartIndex()+ProfileSegmenter.MIN_SEGMENT_SIZE/2, this.testProfile.size());
			seg = new NucleusBorderSegment(seg.getStartIndex(), newEndIndex, unalteredSeg.getTotalLength());
		}
		
		// if the segment is otherwise too short, update the end position
		if( segLength<ProfileSegmenter.MIN_SEGMENT_SIZE/2){
			// find the number of points needed to make the segment long enough
			int extension = ProfileSegmenter.MIN_SEGMENT_SIZE/2 - segLength;
			// get the index of the new end point
			int newEndIndex = Utils.wrapIndex(seg.getEndIndex()+extension, this.testProfile.size());
			// add the new end index position to the segment
			seg = new NucleusBorderSegment(seg.getStartIndex(), newEndIndex, unalteredSeg.getTotalLength());
		}
		seg.setSegmentType(unalteredSeg.getSegmentType());
		return seg;
	}
	
	/**
	 * Compare two segments
	 * @param reference the reference segment
	 * @param test the segment to test
	 * @return the sum of differences between the segments
	 */
	private double compareSegments(NucleusBorderSegment reference, NucleusBorderSegment test){
		
		Profile refProfile  = getSegmentProfile(reference, this.medianProfile); // make a profile from the segment
		Profile subjProfile = getSegmentProfile(test, this.testProfile);
		
		return refProfile.differenceToProfile(subjProfile);
	}
	
	/**
	 * Create a profile from a segment
	 * @param seg the segment to convert
	 * @param p the profile to get values from
	 * @return a profile containing the profile values covered by the segment 
	 */
	private Profile getSegmentProfile(NucleusBorderSegment seg, Profile p){
		if(seg==null || p==null){
			throw new IllegalArgumentException("Profile or segment is null");
		}
		
		double[] ref = p.asArray();
		Profile result = new Profile(new double[0]);
		if(seg.getStartIndex() < seg.getEndIndex()){ // most segments
			double[] segment = Arrays.copyOfRange(ref, seg.getStartIndex(), seg.getEndIndex());
			result = new Profile(segment);
		} else{ // a wrapping end segment
			double[] first = Arrays.copyOfRange(ref, 0, seg.getEndIndex());
			double[] last  = Arrays.copyOfRange(ref, seg.getStartIndex(), ref.length);
			
			List<Double> firstHalf = Arrays.asList(Utils.getDoubleFromdouble(first));
			List<Double> lastHalf  = Arrays.asList(Utils.getDoubleFromdouble(last));
			
			List<Double> combinedList = new ArrayList<Double>(0);
			combinedList.addAll(lastHalf); // they need to be in the correct order as the span the gap
			combinedList.addAll(firstHalf);
			
			Double[] combinedArray = (Double[]) combinedList.toArray(new Double[0]);
			result = new Profile(Utils.getdoubleFromDouble(combinedArray));
		}
		return result;
	}
}
