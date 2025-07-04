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

import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.Taggable;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.profiles.BooleanProfile;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.rules.Rule;
import com.bmskinner.nma.components.rules.Rule.RuleType;
import com.bmskinner.nma.components.rules.RuleSet;
import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.stats.Stats;

/**
 * Allows rule based identification of indexes in a profile.
 * 
 * @author bms41
 * @since 1.13.0
 *
 */
public class ProfileIndexFinder {

	private static final Logger LOGGER = Logger.getLogger(ProfileIndexFinder.class.getName());

	public static final int NO_INDEX_FOUND = -1;
	public static final String RULESET_EMPTY_ERROR = "Ruleset list is empty";
	public static final String NULL_RULE_ERROR = "Rule is null";
	public static final String NULL_PROFILE_ERROR = "Profile is null";

	private ProfileIndexFinder() {
	}

	/**
	 * Assign landmarks to the given nucleus using the given rulesets. This uses the
	 * nucleus' own profiles, and is independent of any dataset medians. For this
	 * reason, if detection fails, it falls back to assigning the landmark to the
	 * zero index of the profile
	 * 
	 * @param n
	 * @param rsc
	 */
	public static void assignLandmarks(@NonNull Nucleus n, @NonNull RuleSetCollection rsc) {

		for (Landmark lm : rsc.getLandmarks()) {
			try {
				List<RuleSet> rulesets = rsc.getRuleSets(lm);
				if (rulesets.isEmpty())
					LOGGER.finer(n.getNameAndNumber() + ": No ruleset found for " + lm);
				for (RuleSet rule : rulesets) {
					IProfile p = n.getProfile(rule.getType());
					int index = identifyIndex(p, rule);

					n.setLandmark(lm, index);
				}
			} catch (MissingProfileException | SegmentUpdateException e) {
				LOGGER.log(Loggable.STACK, "Error getting profile type", e);
			} catch (NoDetectedIndexException e) {
				LOGGER.finer(n.getNameAndNumber() + ": Unable to detect " + lm
						+ " in nucleus, setting to zero");
				try {
					n.setLandmark(lm, 0);
				} catch (IndexOutOfBoundsException
						| MissingDataException
						| SegmentUpdateException e1) {
					LOGGER.log(Loggable.STACK, "Error setting landmark to zero", e);

				} // default to the zero index
			} catch (IndexOutOfBoundsException e) {
				LOGGER.log(Loggable.STACK, "Index out of bounds", e);
			} catch (MissingDataException e) {
				LOGGER.log(Loggable.STACK, "Data not present", e);
			}
		}
	}

	/**
	 * Test if the profile orientation is correct; performs a simple test that the
	 * higher values are to the left
	 * 
	 * @param n
	 * @return
	 * @throws ProfileException
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 */
	public static boolean shouldReverseProfile(@NonNull Nucleus n)
			throws SegmentUpdateException, MissingDataException {
		int frontPoints = 0;
		int rearPoints = 0;

		IProfile profile = n.getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE);

		int midPoint = n.getBorderLength() >> 1;
		for (int i = 0; i < n.getBorderLength(); i++) { // integrate points
														// over 180
			if (i < midPoint)
				frontPoints += profile.get(i);
			if (i > midPoint)
				rearPoints += profile.get(i);
		}

		// if the maxIndex is closer to the end than the beginning
		return frontPoints < rearPoints;
	}

	/**
	 * Get the indexes in the profile that match the given RuleSet
	 * 
	 * @param p the profile
	 * @param r the ruleset to use for identification
	 * @return the indexes matching the ruleset
	 * @throws NoDetectedIndexException
	 */
	public static BooleanProfile getMatchingIndexes(@NonNull final IProfile p,
			@NonNull final RuleSet r) throws NoDetectedIndexException {
		return isApplicable(p, r);
	}

	/**
	 * Get the indexes in the profile that match the given Rule
	 * 
	 * @param p the profile
	 * @param r the rule to use for identification
	 * @return the indexes matching the rule
	 * @throws NoDetectedIndexException
	 */
	public static BooleanProfile getMatchingIndexes(@NonNull final IProfile p,
			@NonNull final Rule r) throws NoDetectedIndexException {
		BooleanProfile result = new BooleanProfile(p, true);
		return isApplicable(p, r, result);
	}

	/**
	 * Count the indexes in the profile that match the given RuleSet
	 * 
	 * @param p the profile
	 * @param r the ruleset to use for identification
	 * @return the number of indexes matching the ruleset
	 * @throws NoDetectedIndexException
	 */
	public static int countMatchingIndexes(@NonNull final IProfile p, @NonNull final RuleSet r)
			throws NoDetectedIndexException {

		BooleanProfile matchingIndexes = getMatchingIndexes(p, r);

		int count = 0;
		for (int i = 0; i < p.size(); i++) {

			if (matchingIndexes.get(i)) {
				count++;
			}
		}
		return count;

	}

	/**
	 * Use the provided RuleSet to identify an index within a profile. Returns the
	 * first matching index in the profile
	 * 
	 * @param p the profile
	 * @param r the ruleset to use for identification
	 * @return the first matching index in the profile
	 * @throws NoDetectedIndexException if no indexes match
	 */
	public static int identifyIndex(@NonNull final IProfile p, @NonNull final RuleSet r)
			throws NoDetectedIndexException {

		BooleanProfile matchingIndexes = getMatchingIndexes(p, r);

		for (int i = 0; i < p.size(); i++) {

			if (matchingIndexes.get(i)) {
				return i;
			}
		}

		throw new NoDetectedIndexException();
	}

	/**
	 * Use the provided RuleSets to identify an index within profiles of an object.
	 * Returns the first matching index in the profile. On error or no hit, throw an
	 * exception.Note that this ignores the RuleSet's ProfileType preference, and
	 * works directly on the given profile
	 * 
	 * @param t    the object to detect on
	 * @param list the rulesets to use
	 * @return the first index matching the rules
	 * @throws NoDetectedIndexException if no indexes were found
	 * @throws MissingDataException
	 * @throws SegmentUpdateException
	 */
	public static int identifyIndex(@NonNull final Taggable t, @NonNull final List<RuleSet> list)
			throws NoDetectedIndexException, MissingDataException, SegmentUpdateException {
		if (list.isEmpty())
			throw new IllegalArgumentException(RULESET_EMPTY_ERROR);

		BooleanProfile indexes = new BooleanProfile(t.getBorderLength(), true);
		for (RuleSet r : list) {
			IProfile p = t.getProfile(r.getType());
			BooleanProfile matchingIndexes = getMatchingIndexes(p, r);
			indexes = indexes.and(matchingIndexes);
		}

		for (int i = 0; i < indexes.size(); i++) {
			if (indexes.get(i)) {
				return i;
			}
		}
		throw new NoDetectedIndexException();
	}

	/**
	 * Use the provided RuleSets to identify an index within a profile. Returns the
	 * first matching index in the profile. On error or no hit, return -1. Note that
	 * this ignores the RuleSet's ProfileType preference, and works directly on the
	 * given profile
	 * 
	 * @param p    the profile
	 * @param list the rulesets to use for identification
	 * @return the first index matching the ruleset
	 * @throws NoDetectedIndexException if the index is not found
	 */
	public static int identifyIndex(@NonNull final IProfile p, @NonNull final List<RuleSet> list)
			throws NoDetectedIndexException {

		if (list.isEmpty())
			throw new IllegalArgumentException(RULESET_EMPTY_ERROR);

		BooleanProfile indexes = new BooleanProfile(p, true);

		for (RuleSet r : list) {
			BooleanProfile matchingIndexes = getMatchingIndexes(p, r);
			indexes = indexes.and(matchingIndexes);
		}

		for (int i = 0; i < p.size(); i++) {
			if (indexes.get(i)) {
				return i;
			}
		}
		throw new NoDetectedIndexException();
	}

	/**
	 * Identify the index in the median profile of the collection matching the
	 * internal RuleSets for the border tag
	 * 
	 * @param collection the cell collection
	 * @param lm         the border tag to find
	 * @return the index in the profile corresponding to the tag
	 * @throws NoDetectedIndexException if the index is not found
	 * @throws SegmentUpdateException
	 * @throws MissingDataException
	 */
	public static int identifyIndex(@NonNull final ICellCollection collection,
			@NonNull final Landmark lm)
			throws NoDetectedIndexException, MissingDataException, SegmentUpdateException {

		List<RuleSet> list = collection.getRuleSetCollection().getRuleSets(lm);

		if (list == null || list.isEmpty())
			throw new IllegalArgumentException(RULESET_EMPTY_ERROR);

		try {
			return identifyIndex(collection, list);
		} catch (NoDetectedIndexException | MissingDataException | SegmentUpdateException e) {
			// No index was found for the collection, fall back
			LOGGER.fine("Landmark " + lm
					+ ": no index found in median using default rules; falling back on longest diameter");
			return identifyIndex(collection, List.of(RuleSet.roundRPRuleSet()));
		}
	}

	/**
	 * Identify the index for the median profile of the collection based on the
	 * given RuleSets
	 * 
	 * @param collection
	 * @param list
	 * @return the index in the profile corresponding to the tag
	 * @throws NoDetectedIndexException if the index is not found
	 * @throws SegmentUpdateException
	 * @throws MissingDataException
	 */
	public static int identifyIndex(@NonNull final ICellCollection collection,
			@NonNull final List<RuleSet> list)
			throws NoDetectedIndexException, MissingDataException, SegmentUpdateException {

		if (list.isEmpty())
			throw new IllegalArgumentException(RULESET_EMPTY_ERROR);

		BooleanProfile indexes = getMatchingProfile(collection, list);

		// Find the first true in the result profile
		for (int i = 0; i < indexes.size(); i++) {
			if (indexes.get(i))
				return i;
		}
		throw new NoDetectedIndexException();
	}

	/**
	 * Get a boolean profile of indexes matching the given rulesets
	 * 
	 * @param collection the cell collection to test
	 * @param list       the rulesets to be tested against the collection
	 * @return
	 * @throws SegmentUpdateException
	 * @throws MissingDataException
	 * @throws NoDetectedIndexException
	 */
	public static BooleanProfile getMatchingProfile(@NonNull final ICellCollection collection,
			@NonNull final List<RuleSet> list)
			throws MissingDataException, SegmentUpdateException, NoDetectedIndexException {

		if (list.isEmpty())
			throw new IllegalArgumentException(RULESET_EMPTY_ERROR);

		// Make a 'true' profile
		BooleanProfile indexes;
		indexes = new BooleanProfile(
				collection.getProfileCollection().getProfile(ProfileType.ANGLE,
						OrientationMark.REFERENCE, Stats.MEDIAN),
				true);

		for (RuleSet r : list) {

			// Get the correct profile for the RuleSet
			IProfile p = collection.getProfileCollection().getProfile(r.getType(),
					OrientationMark.REFERENCE, Stats.MEDIAN);

			// Apply the rule, and update the result profile
			BooleanProfile matchingIndexes = getMatchingIndexes(p, r);
			indexes = indexes.and(matchingIndexes);

		}
		return indexes;
	}

	/**
	 * Test a profile for the applicability of a ruleset
	 * 
	 * @param p
	 * @param r
	 * @return
	 * @throws NoDetectedIndexException
	 */
	private static BooleanProfile isApplicable(final IProfile p, final RuleSet r)
			throws NoDetectedIndexException {

		BooleanProfile result = new BooleanProfile(p, true);
		for (Rule rule : r.getRules()) {
			result = isApplicable(p, rule, result);
		}
		return result;
	}

	/**
	 * Test a profile for the applicability of a rule in a ruleset
	 * 
	 * @param p        the profile to test
	 * @param r        the rule to test
	 * @param existing the existing profile of valid indexes on which the rule will
	 *                 be applied
	 * @return
	 * @throws NoDetectedIndexException
	 */
	private static BooleanProfile isApplicable(final IProfile p, final Rule r,
			BooleanProfile limits) throws NoDetectedIndexException {

		RuleType type = r.getType();

		switch (type) {

		case IS_ZERO_INDEX:
			return findZeroIndex(p);

		case IS_LOCAL_MINIMUM:
			return findLocalMinima(p, limits, r.getBooleanValue(), r.getValue(1));
		case IS_LOCAL_MAXIMUM:
			return findLocalMaxima(p, limits, r.getBooleanValue(), r.getValue(1));

		case IS_MINIMUM:
			return findMinimum(p, limits, r.getBooleanValue());
		case IS_MAXIMUM:
			return findMaximum(p, limits, r.getBooleanValue());

		case INDEX_IS_LESS_THAN:
			return findIndexLessThan(p, limits, r.getValue());
		case INDEX_IS_MORE_THAN:
			return findIndexMoreThan(p, limits, r.getValue());

		case VALUE_IS_LESS_THAN:
			return findValueLessThan(p, limits, r.getValue());
		case VALUE_IS_MORE_THAN:
			return findValueMoreThan(p, limits, r.getValue());

		case IS_CONSTANT_REGION:
			return findConstantRegion(p, limits, r.getValue(0), r.getValue(1), r.getValue(2));

		case FIRST_TRUE:
			return findFirstTrue(limits, r.getBooleanValue());
		case LAST_TRUE:
			return findLastTrue(limits, r.getBooleanValue());

		case INDEX_IS_WITHIN_FRACTION_OF:
			return findIndexWithinFractionOf(limits, r.getValue());

		case INDEX_IS_OUTSIDE_FRACTION_OF:
			return findIndexOutsideFractionOf(limits, r.getValue());

		case INVERT:
			return limits.invert();

		default:
			return new BooleanProfile(p);

		}

	}

	/**
	 * Get indexes that are within the given fraction of a profile from any true
	 * value in the input limits
	 * 
	 * @param b        the limits
	 * @param fraction the fraction of the profile from 0-1 either side of true
	 *                 values that will be true
	 * @return a boolean profile with each true widened by the fractional amount
	 */
	private static BooleanProfile findIndexWithinFractionOf(final BooleanProfile b,
			final double fraction) {
		BooleanProfile result = new BooleanProfile(b.size(), false);

		int range = (int) Math.round(b.size() * fraction);

		for (int i = 0; i < b.size(); i++) {
			if (b.get(i)) {

				for (int j = i - range; j < i + range; j++) {
					result.set(j, true);
				}

			}
		}
		return result;
	}

	/**
	 * Get indexes that are within the given fraction of a profile from any true
	 * value in the input limits
	 * 
	 * @param b        the limits
	 * @param fraction the fraction of the profile from 0-1 either side of true
	 *                 values that will be true
	 * @return a boolean profile with each true widened by the fractional amount
	 */
	private static BooleanProfile findIndexOutsideFractionOf(final BooleanProfile b,
			final double fraction) {
		BooleanProfile result = findIndexWithinFractionOf(b, fraction);

		return result.invert();
	}

	/**
	 * Find constant regions within the profile.
	 * 
	 * @param p       the profile to test
	 * @param value   the value to remain at
	 * @param window  the window size in indexes
	 * @param epsilon the maximum distance from value allowed
	 * @return
	 */
	private static BooleanProfile findConstantRegion(final IProfile p, final BooleanProfile limits,
			final double value,
			final double window, final double epsilon) {

		BooleanProfile result = new BooleanProfile(p); // hard code the
														// smoothing window size
														// for now

		int[] verticalPoints = findConsistentRegionBounds(p, value, epsilon, (int) window);
		if (verticalPoints[0] != -1 && verticalPoints[1] != -1) {

			for (int i = verticalPoints[0]; i <= verticalPoints[1]; i++) {
				result.set(i, true);
			}

		}
		result = result.and(limits);
		return result;
	}

	/**
	 * Detect regions with a consistent value in a profile
	 * 
	 * @param value     the profile value that is to be maintained
	 * @param tolerance the variation allow plus or minus
	 * @points the number of points the value must be sustained over
	 * @return the first and last index in the profile covering the detected region
	 */
	private static int[] findConsistentRegionBounds(IProfile p, double value, double tolerance,
			int points) {
		int counter = 0;
		int start = -1;
		int end = -1;
		int[] result = { start, end };

		double[] array = p.toDoubleArray();

		for (int i = 0; i < array.length; i++) {
			double d = array[i];
			if (d > value - tolerance && d < value + tolerance) { // if the
																	// point meets
																	// criteria

				if (start == -1) { // start a new region if needed
					counter = 0;
					start = i;
				}
				counter++; // start counting a new region or increase an
							// existing region

			} else { // does not meet criteria

				end = i;

				if (counter >= points) { // if the region is large enough
					// return points
					result[0] = start; // use the saved start and end indexes
					result[1] = end;
					return result;

				}
				// otherwise, reset the counter
				start = -1;
				end = -1;

			}
		}
		return result;
	}

	/**
	 * Find the first true value in a BooleanProfile
	 * 
	 * @param b the limits to test within
	 * @param v find the first true value [true], or true values that are not the
	 *          first true value [false]
	 * @return
	 */
	private static BooleanProfile findFirstTrue(final BooleanProfile b, boolean v) {

		BooleanProfile result = new BooleanProfile(b.size(), false);
		boolean foundFirst = false;

		for (int i = 0; i < b.size(); i++) {

			if (v) {
				if (b.get(i)) {
					result.set(i, true);
					return result;
				}
			} else {
				if (b.get(i)) {
					result.set(i, foundFirst);
					foundFirst = true;
				}

			}
		}
		return result;
	}

	/**
	 * Find the last true value in a BooleanProfile
	 * 
	 * @param b the limits to test within
	 * @param v find the last true value [true], or true values that are not the
	 *          last true value [false]
	 * @return
	 */
	private static BooleanProfile findLastTrue(final BooleanProfile b, boolean v) {

		BooleanProfile result = new BooleanProfile(b.size(), false);

		int maxTrueIndex = NO_INDEX_FOUND;
		for (int i = 0; i < b.size(); i++) {
			if (b.get(i)) {
				maxTrueIndex = i;

			}
		}

		if (v) {
			if (maxTrueIndex > -1) {
				result.set(maxTrueIndex, true);
			}

		} else {

			if (maxTrueIndex > -1) {

				for (int i = 0; i < b.size(); i++) {
					if (b.get(i) && i != maxTrueIndex) {
						result.set(i, true);

					}
				}

			}

		}

		return result;
	}

	/**
	 * Find local minima within the profile.
	 * 
	 * @param p       the profile to test
	 * @param include if false, will find indexes that are NOT local minima
	 * @param window  the size of the smoothing window
	 * @return
	 */
	private static BooleanProfile findLocalMinima(final IProfile p, BooleanProfile limits,
			boolean include,
			double window) {

		BooleanProfile result = p.getLocalMinima((int) window); // hard code the
																// smoothing
																// window size
																// for now
		result = result.and(limits);

		if (!include) {
			result = result.invert();
		}
		return result;
	}

	/**
	 * Find local maxima within the profile.
	 * 
	 * @param p       the profile to test
	 * @param include if false, will find indexes that are NOT local maxima
	 * @param window  the size of the smoothing window
	 * @return
	 */
	private static BooleanProfile findLocalMaxima(final IProfile p, BooleanProfile limits,
			boolean include,
			double window) {

		BooleanProfile result = p.getLocalMaxima((int) window); // hard code the
																// smoothing
																// window size
																// for now
		result = result.and(limits);

		if (!include) {
			result = result.invert();
		}
		return result;
	}

	/**
	 * Find the index of the minimum value in a profile
	 * 
	 * @param p      the profile to test
	 * @param limits the limits to apply to the profile
	 * @param b      should the test be for indexes that are minimum or are not
	 *               minumum
	 * @return
	 */
	private static BooleanProfile findMinimum(final IProfile p, BooleanProfile limits, boolean b)
			throws NoDetectedIndexException {

//		int index = NO_INDEX_FOUND;

		int index = p.getIndexOfMin(limits);

		BooleanProfile result = new BooleanProfile(p, !b);
//		if (index > NO_INDEX_FOUND) {
		result.set(index, b);
//		}

		return result;
	}

	/**
	 * Find the index of the maximum value in a profile
	 * 
	 * @param p
	 * @return
	 * @throws NoDetectedIndexException
	 */
	private static BooleanProfile findMaximum(final IProfile p, BooleanProfile limits, boolean b)
			throws NoDetectedIndexException {
//		int index = NO_INDEX_FOUND;

		int index = p.getIndexOfMax(limits);

		BooleanProfile result = new BooleanProfile(p, !b);
//		if (index > NO_INDEX_FOUND) {
		result.set(index, b);
//		}

		return result;
	}

	/**
	 * Make a boolean profile where the indexes are less than the given value
	 * 
	 * @param p
	 * @param fileIndex
	 * @return
	 */
	private static BooleanProfile findIndexLessThan(final IProfile p, BooleanProfile limits,
			double proportion) {

		int index = (int) Math.ceil(p.size() * proportion);

		BooleanProfile result = new BooleanProfile(p);

		for (int i = 0; i < index; i++) {
			result.set(i, true);
		}

		result = result.and(limits);
		return result;

	}

	/**
	 * Make a boolean profile where the indexes are more than the given value
	 * 
	 * @param p
	 * @param fileIndex
	 * @return
	 */
	private static BooleanProfile findIndexMoreThan(final IProfile p, BooleanProfile limits,
			double proportion) {

		int index = (int) Math.floor(p.size() * proportion);

		BooleanProfile result = new BooleanProfile(p);

		for (int i = index; i < result.size(); i++) {
			result.set(i, true);
		}
		result = result.and(limits);
		return result;

	}

	/**
	 * Make a boolean profile where the values are less than the given value
	 * 
	 * @param p
	 * @param fileIndex
	 * @return
	 */
	private static BooleanProfile findValueLessThan(final IProfile p, BooleanProfile limits,
			double value) {

		BooleanProfile result = new BooleanProfile(p);

		for (int i : result) {

			if (p.get(i) < value) {
				result.set(i, true);
			}
		}
		result = result.and(limits);
		return result;

	}

	/**
	 * Make a boolean profile where the values are more than the given value
	 * 
	 * @param p
	 * @param fileIndex
	 * @return
	 */
	private static BooleanProfile findValueMoreThan(final IProfile p, BooleanProfile limits,
			double value) {

		BooleanProfile result = new BooleanProfile(p);

		for (int i : result) {

			if (p.get(i) > value) {
				result.set(i, true);
			}
		}
		result = result.and(limits);
		return result;

	}

	/**
	 * Make a boolean profile where only the first index is true
	 * 
	 * @param p
	 * @return
	 */
	private static BooleanProfile findZeroIndex(final IProfile p) {

		BooleanProfile result = new BooleanProfile(p, false);
		result.set(0, true);
		return result;

	}

}
