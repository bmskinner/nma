/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.analysis.profiles;

import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.ISegmentedProfile;
import com.bmskinner.nma.components.profiles.ProfileException;

/**
 * This class handles fitting of segments between profiles
 * 
 * @author bms41
 *
 */
public class SegmentFitter {

	private static final Logger LOGGER = Logger.getLogger(SegmentFitter.class.getName());

	/**
	 * The multiplier to add to best-fit scores when shrinking a segment below the
	 * minimum segment size specified in ProfileSegmenter
	 */
	static final double PENALTY_SHRINK = 1.5;

	/**
	 * The multiplier to add to best-fit scores when shrinking a segment above the
	 * median segment size
	 */
	static final double PENALTY_GROW = 20;

	@NonNull
	private final ISegmentedProfile templateProfile;

	/**
	 * Construct with a median profile containing segments. The originals will not
	 * be modified
	 * 
	 * @param profile the median profile of the collection
	 * @throws ProfileException       if the profile to be segmented cannot be
	 *                                copied
	 * @throws SegmentUpdateException
	 */
	@SuppressWarnings("null")
	public SegmentFitter(@NonNull final ISegmentedProfile template) throws SegmentUpdateException {
		templateProfile = template.duplicate();
	}

	/**
	 * Run the segment fitter on the given profile. It will take the segments from
	 * the template profile, and apply them to the target profile.
	 * 
	 * @param template the profile with the segments to be fitted
	 * @param target   the profile to fit to the template profile
	 * @return the profile with fitted segments, or on error, the original profile
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 */
	public static ISegmentedProfile fit(@NonNull final ISegmentedProfile template,
			@NonNull final ISegmentedProfile target)
			throws MissingDataException, SegmentUpdateException {
		if (!target.hasSegments())
			return target;

		// Create segments if needed
		if (target.getSegmentCount() != template.getSegmentCount())
			return (remapSegmentEndpoints(template, createSegments(template, target)));

		return remapSegmentEndpoints(template, target);
	}

	/**
	 * Run the segment fitter on the given profile. It will take the segments from
	 * the template profile loaded into the fitter upon construction, and apply them
	 * to the profile.
	 * 
	 * @param target the profile to fit to the current template profile
	 * @return the profile with fitted segments, or on error, the original profile
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 */
	public ISegmentedProfile fit(@NonNull final ISegmentedProfile target)
			throws MissingDataException, SegmentUpdateException {
		return fit(templateProfile, target);
	}

	/**
	 * If the template has a different number of segments to the source, then the
	 * new segments must be created before fitting can proceed
	 * 
	 * @return
	 * @throws SegmentUpdateException
	 */
	private static ISegmentedProfile createSegments(@NonNull final ISegmentedProfile template,
			@NonNull ISegmentedProfile target) throws SegmentUpdateException {
		ISegmentedProfile inter = template.interpolate(target.size());
		ISegmentedProfile result = target.duplicate();
		result.setSegments(inter.getSegments());
		return result;
	}

	/**
	 * 
	 * @param profile the profile to fit against the template profile
	 * @return a profile with best-fit segmentation to the median
	 * @throws ProfileException
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 */
	private static ISegmentedProfile remapSegmentEndpoints(
			@NonNull final ISegmentedProfile template,
			@NonNull ISegmentedProfile profile)
			throws MissingDataException, SegmentUpdateException {

		// By default, return the input profile
		ISegmentedProfile result = profile.duplicate();

		ISegmentedProfile tempProfile = profile.duplicate();

		// fit each segment in turn
		for (IProfileSegment templateSegment : template.getSegments()) {

			IProfileSegment segment = tempProfile.getSegment(templateSegment.getID());

			if (!segment.isLocked()) {
				tempProfile = bestFitSegment(template, tempProfile, templateSegment.getID())
						.duplicate();
				result = tempProfile.duplicate();
			}
		}

		if (result.getSegmentCount() != template.getSegmentCount())
			throw new SegmentUpdateException(
					"Segment fitter could not create correct number of segments in target profile");
		return result;
	}

	/**
	 * Find the best fit offset for the given segment id
	 * 
	 * @param profile
	 * @param id      the segment to test
	 * @return
	 * @throws ProfileException
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 */
	private static ISegmentedProfile bestFitSegment(@NonNull final ISegmentedProfile template,
			@NonNull ISegmentedProfile profile, @NonNull UUID id)
			throws MissingDataException, SegmentUpdateException {

		// by default, return the same profile that came in
		ISegmentedProfile result = profile.duplicate();

		// the segment in the input profile to work on
		IProfileSegment segment = profile.getSegment(id);

		// Get the initial score to beat
		double bestScore = compareSegmentationPatterns(template, profile);

		// the most extreme negative offset to apply to the end of this segment
		// without making the length invalid
		int minimumChange = 0 - (segment.length() - IProfileSegment.MINIMUM_SEGMENT_LENGTH);

		// the maximum length offset to apply
		// we can't go beyond the end of the next segment anyway, so use that as
		// the cutoff
		// how far from current end to next segment end?
		int maximumChange = segment.testLength(segment.getEndIndex(),
				segment.nextSegment().getEndIndex());

		/*
		 * Trying all possible lengths takes a long time. Try adjusting lengths in a
		 * window of <changeWindowSize>, and finding the window with the best fit. Then
		 * drop down to individual index changes to refine the match
		 */
		int bestChangeWindow = 0;
		int changeWindowSize = 10;
		for (int changeWindow = minimumChange; changeWindow < maximumChange; changeWindow += changeWindowSize) {

			// find the changeWindow with the best fit,
			// apply all changes to a fresh copy of the profile
			ISegmentedProfile testProfile = profile.duplicate();
			testProfile = testChange(template, profile, id, changeWindow);
			double score = compareSegmentationPatterns(template, testProfile);
			if (score < bestScore) {
				bestChangeWindow = changeWindow;
			}
		}

		int halfWindow = changeWindowSize / 2;
		// now we have the best window, drop down to a changeValue
		for (int changeValue = bestChangeWindow - halfWindow; changeValue < bestChangeWindow
				+ halfWindow; changeValue++) {
			ISegmentedProfile testProfile = profile.duplicate();

			testProfile = testChange(template, profile, id, changeValue);
			double score = compareSegmentationPatterns(template, testProfile);
			if (score < bestScore) {
				result = testProfile;
			}
		}

		return result;
	}

	/**
	 * Test the effect of moving the segment start boundary of the profile by a
	 * certain amount. If the change is a better fit to the median profile than
	 * before, it is kept.
	 * 
	 * @param profile     the profile to test
	 * @param id          the segment to alter
	 * @param changeValue the amount to alter the segment by
	 * @return the original profile, or a better fit to the median
	 * @throws MissingDataException
	 * @throws ProfileException
	 * @throws SegmentUpdateException
	 * @throws Exception
	 */
	private static ISegmentedProfile testChange(@NonNull final ISegmentedProfile template,
			@NonNull ISegmentedProfile profile, @NonNull UUID id, int changeValue)
			throws MissingDataException, SegmentUpdateException {
		double bestScore = compareSegmentationPatterns(template, profile);

		// apply all changes to a fresh copy of the profile
		ISegmentedProfile testProfile = profile.duplicate();

		// not permitted if it violates length constraints
		IProfileSegment seg = testProfile.getSegment(id);
		int newStart = testProfile.wrap(seg.getStartIndex() + changeValue);

		try {
			testProfile.getSegment(id).update(newStart, seg.getEndIndex());
		} catch (SegmentUpdateException e) {
			return profile;
		}

		double score = compareSegmentationPatterns(template, testProfile);

		if (score < bestScore)
			return testProfile.duplicate();
		return profile;
	}

	/**
	 * Get the score for an entire segment list of a profile. Tests the effect of
	 * changing one segment on the entire set
	 * 
	 * @param referenceProfile
	 * @param testProfile
	 * @return the score
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 * @throws Exception
	 */
	private static double compareSegmentationPatterns(@NonNull ISegmentedProfile referenceProfile,
			@NonNull ISegmentedProfile testProfile)
			throws MissingDataException, SegmentUpdateException {
		if (referenceProfile.getSegmentCount() != testProfile.getSegmentCount())
			throw new IllegalArgumentException("Segment counts are different for profiles");

		double result = 0;
		for (UUID id : referenceProfile.getSegmentIDs()) {
			result += compareSegments(id, referenceProfile, testProfile);
		}
		return result;
	}

	/**
	 * Get the sum-of-squares difference between two segments in the given profile
	 * 
	 * @param name             the name of the segment
	 * @param referenceProfile the profile to measure against
	 * @param testProfile      the profile to measure
	 * @return the sum of square differences between the segments
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 * @throws Exception
	 */
	private static double compareSegments(@NonNull UUID id,
			@NonNull ISegmentedProfile referenceProfile, @NonNull ISegmentedProfile testProfile)
			throws MissingDataException, SegmentUpdateException {

		IProfileSegment reference = referenceProfile.getSegment(id);
		IProfileSegment test = testProfile.getSegment(id);

		double result = 0;

		IProfile refProfile = referenceProfile.getSubregion(reference);
		IProfile subjProfile = testProfile.getSubregion(test);

		result = refProfile.absoluteSquareDifference(subjProfile);
		return result;
	}
}
