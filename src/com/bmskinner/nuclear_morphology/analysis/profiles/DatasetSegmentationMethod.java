/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.analysis.profiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.DatasetValidator;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.ProgressEvent;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileSegmenter.UnsegmentableProfileException;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.DefaultProfileAggregate;
import com.bmskinner.nuclear_morphology.components.generic.DefaultProfileCollection;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.IProfileAggregate;
import com.bmskinner.nuclear_morphology.components.generic.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.SegmentedFloatProfile;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment.SegmentUpdateException;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * Run the segmentation of datasets. This allows for the median profile
 * of a collection to be segmented, and the segments to be assigned to 
 * each nucleus in the dataset.
 * 
 * The complete analysis pipeline operates via the following pseudocode:<br>
 * <br>
 * 	1) Profile the cell collection<br>
 * 		- identify the RP via dataset RuleSets<br>
 *      - generate a median profile <br>
 * 	2) Find the best fit of each nucleus to the median to refine RP and other Tags<br>
 * 	3) Segment the median profile<br>
 *  4) Fit the segments to the nuclei using best-fit alignments<br>
 *  5) Generate frankenprofiles of each nucleus to the median<br>
 *  6) Profile the frankencollection - generate a new frankenmedian<br>
 *  7) Segment the frankenmedian; new segments may emerge from the reduced variation<br>
 *  8) Fit the frankensegments to the nuclei<br>
 *  9) Measure the sum of profile differences between the nuclei and the frankenmedian<br>
 *  <br>
 *   While this value is decreasing, repeat steps 5-9<br>
 *   <br>
 *   This class contains the methods to carry out steps 3-9.
 * 
 * @author bms41
 *
 */
public class DatasetSegmentationMethod extends SingleDatasetAnalysisMethod {

	private ICellCollection sourceCollection = null;
	
	private final ICellCollection collection;

	private MorphologyAnalysisMode mode = MorphologyAnalysisMode.NEW;

	public enum MorphologyAnalysisMode {
		NEW, COPY, REFRESH
	}

	/**
	 * Segment a dataset with the given mode
	 * 
	 * @param dataset the dataset to be segmented
	 * @param mode the analysis mode to run
	 */
	public DatasetSegmentationMethod(@NonNull IAnalysisDataset dataset, @NonNull MorphologyAnalysisMode mode) {
		super(dataset);
		collection = dataset.getCollection();
		this.mode = mode;
	}

	/**
	 * Copy the segmentation pattern from the source collection onto the given
	 * dataset
	 * 
	 * @param dataset the dataset to be segmented
	 * @param source the collection to copy segment patterns from
	 */
	public DatasetSegmentationMethod(@NonNull IAnalysisDataset dataset, @NonNull ICellCollection source) {
		this(dataset, MorphologyAnalysisMode.COPY);
		this.sourceCollection = source;
	}

	@Override
	public IAnalysisResult call() throws Exception {
		fine("-----------------------------");
    	fine("Beginning segmentation method");
    	fine("-----------------------------");
		try {

			switch (mode) {
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
			for (Nucleus n : dataset.getCollection().getNuclei()) {
				n.updateVerticallyRotatedNucleus();
				n.updateDependentStats();
			}
			fine("Updated verticals and stats");

		} catch (Exception e) {
			result = null;
			System.out.println("Error in segmentation: "+e.getMessage());
			e.printStackTrace();
			stack("Error in segmentation analysis", e);
		}

		return result;

	}

	/**
	 * The main analysis method. Segment the median, apply the segments to
	 * nuclei, generate the franken-median, and adjust the nuclear segments based
	 * on comparison of franken-profiles.
	 * 
	 * @return an analysis result with the segmented dataset
	 * @throws Exception
	 */
	private IAnalysisResult runNewAnalysis() throws Exception {

		dataset.getCollection().setConsensus(null); // clear if present

		ISegmentedProfile median = createSegmentsInMedian(); // 3 - segment the median profile
		
		if(median.getSegmentCount()<=1) {
			warn("Unable to find segments in median profile");
			return new DefaultAnalysisResult(dataset);
		}
		
		dataset.getCollection().getProfileCollection().addSegments(median.getSegments());
		
		assignSegmentsToNuclei(median);// 4 - fit the segments to nuclei by best-fit
		
		
		
		// 5 - Generate frankenprofiles for each nucleus against the median
		// 6 - Profile the frankencollection
		// 7 - Create a new frankenmedian
		// 8 - Fit the frankensegments to the nuclei
		// 9 - Measure the sum of profile differences between the nuclei and the frankenmedian
//		iterateFrankenprofiles();
		
		// The best segmentation pattern has been found
		// Copy segmentation to child datasets and invalidate 
		// any consensus nuclei
		if(dataset.hasChildren()){
			for(IAnalysisDataset child: dataset.getAllChildDatasets()){
				child.getCollection().setConsensus(null);
				dataset.getCollection().getProfileManager().copyCollectionOffsets(child.getCollection());				
			}
		}
		
		fine("Validating dataset after segmentation");
		DatasetValidator v = new DatasetValidator();
		boolean ok = v.validate(dataset);
		if(!ok) {
			for(String s : v.getErrors()){
				fine(s);
			}
		}
		return new DefaultAnalysisResult(dataset);
	}

	private IAnalysisResult runCopyAnalysis() throws Exception {
		if (sourceCollection == null) {
			warn("Cannot copy: source collection is null");
			return null;
		}

		if(!sourceCollection.getProfileCollection().hasSegments()) {
			fine("Cannot copy segments: source collection has no segments");
			dataset.getCollection().createProfileCollection(); // ensure profiles are set
			return new DefaultAnalysisResult(dataset);
		}

		fine("Copying segmentation pattern");
		reapplyProfiles();
		fine("Copying complete");
		return new DefaultAnalysisResult(dataset);
	}

	
	/**
	 * Refresh the nucleus segmentation. Assumes that the segmentation in the
	 * median profile is correct, and updates all nuclei to match.
	 * 
	 * @param collection
	 * @return
	 */
	private IAnalysisResult runRefreshAnalysis() throws Exception {
		ISegmentedProfile median = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		assignSegmentsToNuclei(median);
		return new DefaultAnalysisResult(dataset);
	}


	/**
	 * When a population needs to be reanalysed do not offset nuclei or
	 * recalculate best fits; just get the new median profile
	 * 
	 * @param collection the collection of nuclei
	 * @param sourceCollection the collection with segments to copy
	 */
	private void reapplyProfiles() throws Exception {

		fine("Applying existing segmentation profile to population");

		sourceCollection.getProfileManager().copyCollectionOffsets(collection);

		// At this point the collection has only a regular profile collections.
		// No Frankenprofile has been copied.
		
		

//		reviseSegments();

		fine("Re-profiling complete");
	}
	
	/**
	 * Use a segmenter to segment the median profile of the
	 * collection starting from the reference point
	 * 
	 * @param collection
	 */
	private ISegmentedProfile createSegmentsInMedian() throws Exception {

		IProfileCollection pc = collection.getProfileCollection();

		// the reference point is always index 0, so the segments will match
		// the profile
//		IProfile median = pc.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		
		// choose the best subset of nuclei to make a median
		RepresentativeMedianFinder finder = new RepresentativeMedianFinder(collection);
		
		IProfile median = finder.findMedian();

		ProfileSegmenter segmenter = new ProfileSegmenter(median);
		List<IBorderSegment> segments = segmenter.segment();
		return new SegmentedFloatProfile(median, segments);
	}
	
	/**
	 * Assign the segments in the given profile to the nuclei within the
	 * collection. The template profile is assumed to be indexed at the reference
	 * point.
	 * @throws UnavailableComponentException 
	 * @throws SegmentUpdateException 
	 * @throws ProfileException 
	 * 
	 * @throws Exception
	 */
	private void assignSegmentsToNuclei(@NonNull ISegmentedProfile template) throws ProfileException, SegmentUpdateException, UnavailableComponentException {
		// Find the approximate segment boundary position using least-squares fitting
		for(Nucleus n : collection.getNuclei()) {
			bestFitAlignSegments(template, n);
			if(n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).getSegmentCount()!=template.getSegmentCount()){
				throw new ProfileException("Segments were not fitted to nucleus");
			}
		}
	}
	
	
	/**
	 * Create a frankenmedian based on the current segmentation patterns of the nuclei in the 
	 * collection.
	 * @return
	 * @throws UnavailableBorderTagException
	 * @throws UnavailableProfileTypeException
	 * @throws ProfileException
	 * @throws UnsegmentedProfileException
	 */
	private ISegmentedProfile createFrankenMedianFromNuclei(@NonNull ISegmentedProfile template) throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException, UnsegmentedProfileException {

		IProfileAggregate agg = new DefaultProfileAggregate(template.size(), collection.getNucleusCount());
		
		for(Nucleus n : collection.getNuclei()) {
			ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
			ISegmentedProfile franken = profile.frankenNormaliseToProfile(template);
			agg.addValues(franken);
		}
		return new SegmentedFloatProfile(agg.getMedian(), template.getSegments());
	}

	
	/**
	 * Calculate the sum of sum-of-squares differences between the give profile and each nucleus profile.
	 * The template profile is assumed to be indexed at the reference point.
	 * @param template the profile to be compared to each nucleus in the collection.
	 * @return
	 * @throws ProfileException
	 * @throws UnavailableBorderTagException
	 * @throws UnavailableProfileTypeException
	 */
	private double calculateDifferenceScoresToProfile(ISegmentedProfile template) throws ProfileException, UnavailableBorderTagException, UnavailableProfileTypeException {
		double score = 0;
		for(Nucleus n : collection.getNuclei()) {
			ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
			ISegmentedProfile franken = profile.frankenNormaliseToProfile(template);
			score += template.absoluteSquareDifference(franken);
		}
		return score;
	}
	
	
//	private void iterateFrankenprofiles() throws ProfileException, UnsegmentedProfileException, UnsegmentableProfileException, SegmentUpdateException, UnavailableComponentException {
//		
//		// Debugging charts only
//		List<IProfile> profiles = new ArrayList<>();
//		List<String> names = new ArrayList<>();
//
//		// Calculate new difference scores
//		ISegmentedProfile median = collection.getProfileCollection()
//				.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
//
//		
//		List<IBorderSegment> frankenSegments = median.getSegments(); // to restore from if frankenprofiling fails
//		double score = calculateDifferenceScoresToProfile(median);
//		profiles.add(median.copy());
//		names.add("Starting median: score "+score);
//		
//		fine(String.format("Starting median has %s segments", median.getSegmentCount()));
//		
//		fine("Starting score: "+score);
//		double prevScore = Double.MAX_VALUE;
//		while(score<prevScore) {
//			prevScore = score;
//			// 5 - Generate frankenprofiles for each nucleus against the median
//			// 6 - Profile the frankencollection
//			// 7 - Create a new frankenmedian
//			ISegmentedProfile frankenMedian = createFrankenMedianFromNuclei(median);
//
//			// 8 - Segment the frankenmedian
//			ProfileSegmenter segmenter = new ProfileSegmenter(frankenMedian);
//			List<IBorderSegment> testFrankenSegments = segmenter.segment();
//			if(testFrankenSegments.size()==1)
//				break; // that went wrong, we should get more segments out, not fewer/equal
//			
//			
//			frankenMedian.setSegments(testFrankenSegments);
//			median.setSegments(testFrankenSegments);
//
//			profiles.add(frankenMedian.copy());
//
//			fine(String.format("Frankenmedian has %s segments", testFrankenSegments.size()));
//
//			// 9 - Fit the frankensegments to each nucleus
//			assignSegmentsToNuclei(frankenMedian);
//			
//			// Calculate new difference scores
//			score = calculateDifferenceScoresToProfile(frankenMedian);
//			fine("Score after frankenprofiling: "+score);
//			
//			if(score>=prevScore) { // go back to the previous segments
//
//				frankenMedian.setSegments(frankenSegments);
//				median.setSegments(frankenSegments);
//				assignSegmentsToNuclei(frankenMedian);
//				profiles.add(frankenMedian.copy());
//				fine(String.format("Final frankenmedian has %s segments", frankenSegments.size()));
//				score = calculateDifferenceScoresToProfile(frankenMedian);
//			} else {
//				frankenSegments = testFrankenSegments;
//			}
//			
//			names.add("Frankenmedian iteration: score "+score);
//		}
//		fine("Final score: "+score);
//		// Add the new segments back to the profile collection
//		collection.getProfileCollection().addSegments(frankenSegments);
//		
//		
//		
////		try {
////			ChartFactoryTest.showProfiles(profiles, names, "Segmentation method");
////		} catch (InterruptedException e) {
////			throw new ProfileException("Charting error", e);
////		}
//	}
	
	/**
	 * Assign segments to a nucleus, finding the best match of the
	 * nucleus profile to the template profile. Segments are matched using a sliding
	 * window offset of the entire profile.
	 *  
	 * @param n the nucleus to assign segments to
	 * @param template the segmented template profile
	 * @throws SegmentUpdateException 
	 * @throws UnavailableComponentException 
	 */
	private void bestFitAlignSegments(@NonNull ISegmentedProfile template, @NonNull Nucleus n) throws ProfileException, SegmentUpdateException, UnavailableComponentException {

		if (n.isLocked())
			return;
		ISegmentedProfile nucleusProfile  = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
//		nucleusProfile = bestFitAlignSegments(template, nucleusProfile);
		IterativeSegmentFitter fitter = new IterativeSegmentFitter(template);
		nucleusProfile = fitter.fit(nucleusProfile);
		n.setProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, nucleusProfile);
	}
	
	/**
	 * Assign segments from a template to a target, finding the best match of the
	 * target profile to the template profile. Segments are matched using a sliding
	 * window offset of the entire profile.
	 * 
	 * Step 1 is to create segments in the target at the proportional indexes of the template.
	 * Step 2 is to adjust the segment boundaries based on the best-fit offsets 
	 * 
	 * Is is assumed that the template and target have the same reference points at index zero.
	 *  
	 * @param template the segmented template profile
	 * @param target the profile to be segmented
	 * @return the target profile with segments
	 * @throws SegmentUpdateException 
	 * @throws UnavailableComponentException 
	 */
	private ISegmentedProfile bestFitAlignSegments(@NonNull ISegmentedProfile template, @NonNull ISegmentedProfile target) throws ProfileException, SegmentUpdateException, UnavailableComponentException {

		// Debugging charts only
		List<IProfile> profiles = new ArrayList<>();
		List<String> names = new ArrayList<>();

		profiles.add(template.copy());
		names.add("Input template profile");
		target.clearSegments();
		/* TODO: What happens when an object is symmetric about at least one axis?
		   The offset could land in either of at least two places, causing segments 
		   to be placed on top
		   of each other. See the test method with varying border offsets.

		   Step 1 is designed to avoid this, by ensuring the segments are all present
		   and non-overlapping. Once in place, Step 2 involves updating the boundaries to a 
		   valid best fit index.
		 */

		List<IBorderSegment> targetSegments = new ArrayList<>();

		int prevEnd = 0; // Always start at the RP
		for (IBorderSegment segment : template.getSegments()) {

			int medianEnd   = segment.getEndIndex();
			double propEnd  = template.getFractionOfIndex(medianEnd);
			
			int targetStart = prevEnd;
			int targetEnd   = target.getIndexOfFraction(propEnd);
			
			IBorderSegment seg = IBorderSegment.newSegment(targetStart, targetEnd, target.size(),
					segment.getID());
			
			targetSegments.add(seg);
			prevEnd = targetEnd;
		}

		IBorderSegment.linkSegments(targetSegments);
		target.setSegments(targetSegments);
		fine("Assigned segments to target profile by proportional matching");

		/*
		 * Step 2:
		 * Find the best fit offset for the segment endpoints within
		 * the range of the current segment and previous segment.
		 * 
		 * The first segment must still start at index zero.
		 * 
		 * The end segment must still end at index zero.
		 * 
		 */

		for(int i=0; i<targetSegments.size(); i++) {
			if(i==0)
				continue;
			
			IBorderSegment templateSeg = template.getSegments().get(i);
			IBorderSegment targetSeg   = targetSegments.get(i);

			profiles.add(target.copy());
			names.add("Target input for segment fitting to "+templateSeg.getDetail());

			// Profile starting at the segment start
			IProfile startOffsetTemplate = template.offset(templateSeg.getStartIndex());
			
			// Test shows the template profile is correctly offset
//			System.out.println(Arrays.toString(startOffsetTemplate.toFloatArray()));
			
			// search within the previous and current segment range of the target
			
			int minOffset = target.wrap(targetSeg.prevSegment().getStartIndex()+IBorderSegment.MINIMUM_SEGMENT_LENGTH);
			int maxOffset = target.wrap(targetSeg.getEndIndex()-IBorderSegment.MINIMUM_SEGMENT_LENGTH);
			
//			System.out.println(String.format("Testing segment offsets %s-%s", minOffset, maxOffset));
			
			profiles.add(startOffsetTemplate.copy());
			names.add("Template offset to "+templateSeg.getStartIndex());

			int startIndex = target.findBestFitOffset(startOffsetTemplate, minOffset, maxOffset);
			fine(String.format("Optimum start index found at %s for %s", startIndex, targetSeg.getDetail()));

			
			targetSeg.update(startIndex, targetSeg.getEndIndex());			
		}
//		try {
//			ChartFactoryTest.showProfiles(profiles, names, "Segmentation method");
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		target.setSegments(targetSegments);
		if(target.getSegmentCount()!=template.getSegmentCount())
			throw new ProfileException("Nucleus does not have the correct segment count");

		fine(String.format("Completed segment assignment for target profile"));
		return target;
	}

	@Override
	public void progressEventReceived(ProgressEvent event) {
		fireProgressEvent();
	}

}
