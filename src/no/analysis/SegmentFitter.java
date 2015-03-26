package no.analysis;

import ij.IJ;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import no.components.NucleusBorderSegment;
import no.components.Profile;
import no.nuclei.INuclearFunctions;
import no.nuclei.Nucleus;
import no.utility.Utils;

/**
 * This takes a median profile plus segments, and a real profile plus segments from
 * a Nucleus, and tries to optimise the segment endpoints by moving the segment
 * boundaries. The segments are interpolated to match the corresponding median segment,
 * and a best fit score is calculated. This should help overcome the 'sensory homunculus'
 * problem with Yqdels in an otherwise WT population
 */
public class SegmentFitter {

	private Profile medianProfile; // the profile to align against
	private Profile   testProfile; // the profile to adjust
	
	List<NucleusBorderSegment> medianSegments;
	List<NucleusBorderSegment>   testSegments;
	
	private static int POINTS_TO_TEST = 8;
	
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
	public void fit(INuclearFunctions n){
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
		
		List<NucleusBorderSegment> newList = this.runFitter();
		
		n.setSegments(newList);
		
//		ProfileSegmenter segmenter = new ProfileSegmenter(this.testProfile, newList);
//		segmenter.draw(n.getNucleusFolder()+File.separator+Nucleus.IMAGE_PREFIX+n.getNucleusNumber()+".revised_segments.tiff");
	}
	
	/**
	 * for each test segment
				// compare with median segment
				// move the increase or decrease the test endpoint
				// score again
				// get the lowest score within ?10 border points either side
				// next segment
			
			// update the nucleus
	 */
	private List<NucleusBorderSegment> runFitter(){
//		IJ.log("Running fitter:");
		List<NucleusBorderSegment> newList = new ArrayList<NucleusBorderSegment>(0);
		
//		Profile testMinima = this.testProfile.smooth(2).getLocalMinima(5);
//		Profile testMaxima = this.testProfile.smooth(2).getLocalMaxima(5);
		
		for(int i=0; i<this.testSegments.size();i++){
//			IJ.log("    Segment "+i);			
			NucleusBorderSegment seg = this.testSegments.get(i);
			if(i>0){ // carry over the offset from the previous segment
				seg = new NucleusBorderSegment(newList.get(i-1).getEndIndex(), seg.getEndIndex());
			}
//			seg.print();
			
				
			double score =  compareSegments(this.medianSegments.get(i), seg);
			double minScore = score;
			NucleusBorderSegment bestSeg = seg;
			for(int j=-SegmentFitter.POINTS_TO_TEST;j<=SegmentFitter.POINTS_TO_TEST;j++){
				int newEndIndex = Utils.wrapIndex(seg.getEndIndex()+j, this.testProfile.size());
				NucleusBorderSegment newSeg = new NucleusBorderSegment(seg.getStartIndex(), newEndIndex);
				score = compareSegments(this.medianSegments.get(i), newSeg);
				if(score<minScore){
					minScore=score;
					bestSeg = newSeg;
				}
				// this is a tempting system, but it falls apart as soon as we get irregular nuclei
//				if((testMinima.get(newSeg.getEndIndex())==1 || testMaxima.get(newSeg.getEndIndex())==1) && j>-SegmentFitter.POINTS_TO_TEST){ // we found something interesting
//					bestSeg = newSeg;
//					IJ.log("      Endpoint offset "+j+": Mimum or maximum");	
//					break; // stop looking, we have our point of interest
//				}
//				IJ.log("      Endpoint offset "+j+": "+score);	
			}
			newList.add(bestSeg);
//			bestSeg.print();	

		}
		return newList;
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
