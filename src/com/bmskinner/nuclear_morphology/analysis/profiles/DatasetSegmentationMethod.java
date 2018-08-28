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
		
		
		createSegmentsInMedian(); // 3 - segment the median profile
		assignMedianSegmentsToNuclei(); // 4 - fit the segments to nuclei
		
		// Calculate new difference scores
		double score = calculateDifferenceScoresToProfile(collection.getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN));
		System.out.println("Starting score "+score);
		double prevScore = Double.MAX_VALUE;
		while(score<prevScore) {
			prevScore = score;
			// 5 - Generate frankenprofiles for each nucleus against the median
			// 6 - Profile the frankencollection
			// 7 - Create a new frankenmedian
			ISegmentedProfile frankenMedian = createFrankenMedianFromNuclei();

			// 8 - Segment the frankenmedian
			ProfileSegmenter segmenter = new ProfileSegmenter(frankenMedian);
			List<IBorderSegment> frankenSegments = segmenter.segment();
			
			fine(String.format("Frankenmedian has %s segments", frankenSegments.size()));
			
			// 9 - Fit the frankensegments to each nucleus
			collection.getNuclei().parallelStream().forEach(n -> {
				try {
					assignSegmentsToNucleus(frankenMedian, n);
				} catch (ProfileException | UnavailableProfileTypeException e) {
					warn("Unable to assign frankenmedian segments to nucleus " + n.getNameAndNumber());
//					stack(e.getMessage(), e);
				}
			});
			
			// Calculate new difference scores
			score = calculateDifferenceScoresToProfile(frankenMedian);
			System.out.println("Score "+score);
		}

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
		v.validate(dataset);
		for(String s : v.getErrors()){
			warn(s);
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
		assignMedianSegmentsToNuclei();
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

		reviseSegments();

		fine("Re-profiling complete");
	}
	
	/**
	 * Use a segmenter to segment the median profile of the
	 * collection starting from the reference point
	 * 
	 * @param collection
	 */
	private void createSegmentsInMedian() throws Exception {

		fine("Creating segments in median profile");
		IProfileCollection pc = collection.getProfileCollection();

		// the reference point is always index 0, so the segments will match
		// the profile
		IProfile median = pc.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);

		ProfileSegmenter segmenter = new ProfileSegmenter(median);
		List<IBorderSegment> segments = segmenter.segment();

		fine(String.format("Creating %s segments in median profile", segments.size()));
		pc.addSegments(Tag.REFERENCE_POINT, segments);
	}

	/**
	 * Assign the segments in the median profile to the nuclei within the
	 * collection
	 * 
	 * @param collection
	 * @throws Exception
	 */
	private void assignMedianSegmentsToNuclei() throws Exception {

		IProfileCollection pc = collection.getProfileCollection();

		// find the corresponding point in each nucleus
		ISegmentedProfile median = pc.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);

		collection.getNuclei().parallelStream().forEach(n -> {
			try {
				assignSegmentsToNucleus(median, n);
			} catch (ProfileException | UnavailableProfileTypeException e) {
				warn("Error setting profile offsets in " + n.getNameAndNumber());
				stack(e.getMessage(), e);
			}
		});

		// Update segments to best fit
		reviseSegments();
		
		// If any nuclei do not have a segment starting on the RP, correct this
		ensureRPatSegmentBoundary();
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
	private ISegmentedProfile createFrankenMedianFromNuclei() throws UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException, UnsegmentedProfileException {
				
		ISegmentedProfile median = collection.getProfileCollection()
				.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		
		IProfileAggregate agg = new DefaultProfileAggregate(median.size(), collection.getNucleusCount());
		
		for(Nucleus n : collection.getNuclei()) {
			ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
			ISegmentedProfile franken = profile.frankenNormaliseToProfile(median);
			agg.addValues(franken);
		}
		return new SegmentedFloatProfile(agg.getMedian(), median.getSegments());
	}

	
	private double calculateDifferenceScoresToProfile(ISegmentedProfile frankenMedian) throws ProfileException, UnavailableBorderTagException, UnavailableProfileTypeException {
		double score = 0;
		for(Nucleus n : collection.getNuclei()) {
			ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
			ISegmentedProfile franken = profile.frankenNormaliseToProfile(frankenMedian);
			score += frankenMedian.absoluteSquareDifference(franken);
		}
		return score;
	}
	
	/**
	 * Assign the median segments to the nucleus, finding the best match of the
	 * nucleus profile to the median profile
	 * 
	 * @param n the nucleus to segment
	 * @param median the segmented median profile
	 * @throws UnavailableProfileTypeException 
	 */
	private void assignSegmentsToNucleus(@NonNull ISegmentedProfile median, @NonNull Nucleus n) throws ProfileException, UnavailableProfileTypeException {

		if (n.isLocked())
			return;

		// remove any existing segments in the nucleus
		ISegmentedProfile nucleusProfile  = n.getProfile(ProfileType.ANGLE);

		nucleusProfile.clearSegments();

		List<IBorderSegment> nucleusSegments = new ArrayList<>();

		// go through each segment defined for the median curve
		IBorderSegment prevSeg = null;

		for (IBorderSegment segment : median.getSegments()) {

			// get the positions the segment begins and ends in the median
			// profile
			int startIndexInMedian = segment.getStartIndex();
			int endIndexInMedian = segment.getEndIndex();

			// find the positions these correspond to in the offset profiles

			// get the median profile, indexed to the start or end point
			IProfile startOffsetMedian = median.offset(startIndexInMedian);
			IProfile endOffsetMedian = median.offset(endIndexInMedian);

			try {

				// find the index at the point of the best fit
				int startIndex = n.getProfile(ProfileType.ANGLE).getSlidingWindowOffset(startOffsetMedian);
				int endIndex = n.getProfile(ProfileType.ANGLE).getSlidingWindowOffset(endOffsetMedian);

				IBorderSegment seg = IBorderSegment.newSegment(startIndex, endIndex, n.getBorderLength(),
						segment.getID());
				if (prevSeg != null) {
					seg.setPrevSegment(prevSeg);
					prevSeg.setNextSegment(seg);
				}

				nucleusSegments.add(seg);

				prevSeg = seg;

			} catch (IllegalArgumentException | UnavailableProfileTypeException e) {
				warn("Cannot make segment");
				stack("Error making segment for nucleus " + n.getNameAndNumber(), e);
				break;

			}

		}

		IBorderSegment.linkSegments(nucleusSegments);
		nucleusProfile.setSegments(nucleusSegments);

		if(nucleusProfile.getSegmentCount()!=median.getSegmentCount())
			throw new ProfileException("Nucleus does not have the correct segment count");

		n.setProfile(ProfileType.ANGLE, nucleusProfile);
	}

	/**
	 * Check if the reference point of the nuclear profiles is at a segment
	 * boundary for all nuclei. If the RP is not at a segment boundary, move
	 * the start segment boundary of the segment containing the RP.
	 * 
	 * @param collection
	 * @return
	 */
	private void ensureRPatSegmentBoundary() {
		finer("Checking RP is at a segment boundary in all nuclei");
		collection.getNuclei().stream().forEach(n -> {
			try {

				// Profile with RP at zero
				ISegmentedProfile profile = n.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT);
				boolean hit = false;
				for (IBorderSegment s : profile.getSegments()) {
					hit |= s.getStartIndex()==0;
				}

				if (!hit) {
					finer("Moving RP to segment boundary");
					// The RP is not at the start of a segment
					// Update the segment start to zero
					IBorderSegment seg = profile.getSegmentContaining(0);
					seg.update(0, seg.getEndIndex());
					finer("Applying profile with updated RP: "+profile.toString());
					n.setProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, profile);
				}

			} catch (UnavailableComponentException | ProfileException | SegmentUpdateException e) {
				warn("Error updating nucleus segment to RP ");
				stack(e);
			}
		});
	}

	/**
	 * Update segment assignments in individual nuclei by stretching each
	 * segment to the best possible fit along the median profile
	 * 
	 * @param collection
	 * @param pointType
	 * @throws ProfileException 
	 */
	private void reviseSegments() throws ProfileException {

		IProfileCollection pc = collection.getProfileCollection();

		try {
			List<IBorderSegment> segments = pc.getSegments(Tag.REFERENCE_POINT);

			// Get the median profile for the population
			ISegmentedProfile medianProfile = pc.getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);

			SegmentFitter fitter = new SegmentFitter(medianProfile);

			collection.getNuclei().parallelStream().forEach(n->{

				try {
					if (! n.isLocked())
						fitter.fit(n, pc);
				} catch (IndexOutOfBoundsException | ProfileException | UnavailableComponentException
						| UnsegmentedProfileException e) {
					stack("Could not fit segments for nucleus "+n.getNameAndNumber()+": "+e.getMessage(), e);
				} finally {
					fireProgressEvent();
				}

			});

		} catch (UnavailableBorderTagException e1) {
			error("Unavailable border tag in segment recombining task: " + e1.getMessage(), e1);
		} catch (UnavailableProfileTypeException e1) {
			error("Unavailable profile type in segment recombining task: " + e1.getMessage(), e1);
		} catch (UnsegmentedProfileException e1) {
			error("Unsegmented profile in segment recombining task: " + e1.getMessage(), e1);
		}

		pc.createProfileAggregate(collection, pc.length());

		ensureRPatSegmentBoundary();
	}

	@Override
	public void progressEventReceived(ProgressEvent event) {
		fireProgressEvent();
	}

}
