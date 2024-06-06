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
import java.util.Set;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.AnalysisMethodException;
import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nma.components.ComponentMeasurer;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.DatasetValidator;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.datasets.ICellCollection;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.options.MissingOptionException;
import com.bmskinner.nma.components.profiles.IProfile;
import com.bmskinner.nma.components.profiles.Landmark;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.MissingProfileException;
import com.bmskinner.nma.components.profiles.ProfileException;
import com.bmskinner.nma.components.profiles.ProfileType;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.rules.RuleApplicationType;
import com.bmskinner.nma.components.rules.RuleSet;
import com.bmskinner.nma.logging.Loggable;
import com.bmskinner.nma.stats.Stats;

/**
 * The method for profiling nuclei within a dataset. This detects the optimal
 * indexes to assign landmarks within each nucleus
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class DatasetProfilingMethod extends SingleDatasetAnalysisMethod {

	private static final Logger LOGGER = Logger.getLogger(Loggable.PROJECT_LOGGER);

	public static final int RECALCULATE_MEDIAN = 0;

	public static final int MAX_COERCION_ATTEMPTS = 50;

	private DatasetValidator dv = new DatasetValidator();

	/**
	 * Create a profiler for the given dataset
	 * 
	 * @param dataset
	 */
	public DatasetProfilingMethod(@NonNull IAnalysisDataset dataset) {
		super(dataset);
	}

	@Override
	public IAnalysisResult call() throws Exception {
		run();

		if (!dv.validate(dataset))
			throw new AnalysisMethodException(
					"Unable to validate dataset after profiling: " + dv.getSummary() + "\n"
							+ dv.getErrors());
		return new DefaultAnalysisResult(dataset);
	}

	/**
	 * Calculate the median profile of the colleciton, and generate the best fit
	 * offsets of each nucleus to match.
	 * 
	 * The individual nuclei within the collection have had RP determined from their
	 * internal profiles. (This is part of Nucleus constructor)
	 * 
	 * Build the median based on the RP indexes. If moving RP index in a nucleus
	 * improves the median, move it.
	 * 
	 * Continue until the best-fit of RP has been obtained.
	 * 
	 * Find the OP and other BorderTags in the median
	 * 
	 * Apply to nuclei using offsets
	 */
	private void run() throws Exception {
		if (!dataset.hasAnalysisOptions()) {
			LOGGER.warning("Unable to run profiling method, no analysis options in dataset "
					+ dataset.getName());
			return;
		}

		this.fireUpdateProgressTotalLength(dataset.size() * 3); // Sized assuming VIA_MEDIAN
																// selected

		RuleApplicationType ruleType = dataset.getAnalysisOptions()
				.orElseThrow(MissingOptionException::new)
				.getRuleSetCollection()
				.getApplicationType();

		if (RuleApplicationType.VIA_MEDIAN.equals(ruleType))
			runViaMedian();

		if (RuleApplicationType.PER_NUCLEUS.equals(ruleType))
			runPerNucleus();

	}

	/**
	 * Detect border tags in nuclei using the dataset rulesets, and also apply rules
	 * to the median profile. The median is not used for back-propogation of tags.
	 * 
	 * @throws MissingOptionException
	 * 
	 * @throws Exception
	 */
	private void runPerNucleus()
			throws ProfileException, MissingProfileException, MissingLandmarkException,
			MissingOptionException {
		ICellCollection collection = dataset.getCollection();

		collection.getProfileCollection().calculateProfiles();

		// Reference points are assigned in each nucleus on creation
		// Create a median from the current reference points in the nuclei
		collection.getProfileCollection().getProfile(ProfileType.ANGLE, OrientationMark.REFERENCE,
				Stats.MEDIAN);

		// For each tag in the dataset ruleset collection, identify the tag in nuclei
		Set<Landmark> tags = collection.getRuleSetCollection().getLandmarks();

		Landmark rp = collection.getRuleSetCollection()
				.getLandmark(OrientationMark.REFERENCE)
				.orElseThrow(MissingLandmarkException::new);

		for (Landmark lm : tags) {
			if (rp.equals(lm)) // Already set
				continue;
			List<RuleSet> ruleSets = collection.getRuleSetCollection().getRuleSets(lm);
			for (Nucleus n : collection.getNuclei()) {
				int index = 0;
				try {
					index = ProfileIndexFinder.identifyIndex(n, ruleSets);
				} catch (NoDetectedIndexException e) {
					LOGGER.fine("Cannot identify " + lm + " in nucleus " + n.getNucleusNumber()
							+ ", using index 0");
					// Fall back to zero index, correct manually
				}
				if (!n.isLocked()) {
					n.setLandmark(lm, index);
				} else {
					LOGGER.fine(() -> "Nucleus %s is locked, not changing %s".formatted(
							n.getNameAndNumber(),
							lm));
				}

				for (Measurement m : dataset.getAnalysisOptions()
						.orElseThrow(MissingOptionException::new)
						.getRuleSetCollection()
						.getMeasurableValues()) {
					n.setMeasurement(m, ComponentMeasurer.calculate(m, n));
				}
			}

			// Add the index to the median profiles
			int medianIndex = 0;
			try {
				medianIndex = ProfileIndexFinder.identifyIndex(collection, ruleSets);
			} catch (NoDetectedIndexException e) {
				LOGGER.fine("Cannot identify " + lm + " in median, using index 0");
			}
			collection.getProfileCollection().setLandmark(lm,
					CellularComponent.wrapIndex(medianIndex, collection.getMedianArrayLength()));
		}
	}

	/**
	 * Detect border tags using the median profile
	 * 
	 * @throws Exception
	 */
	private void runViaMedian() throws Exception {
		ICellCollection collection = dataset.getCollection();

		if (!dv.validate(dataset))
			throw new ProfileException(
					"Dataset does not validate before finding RP: " + dv.getSummary()
							+ dv.getErrors());

		// Find and update the RP
		identifyRP(collection);

		// Find all other landmarks
		identifyOtherLandmarks(collection);

		// Clear all calculated measured values and force recalculation
		// in each nucleus since some measurements use the landmarks
		// for orientation
		for (Nucleus n : dataset.getCollection().getNuclei()) {
			for (Measurement m : dataset.getAnalysisOptions()
					.orElseThrow(MissingOptionException::new)
					.getRuleSetCollection()
					.getMeasurableValues()) {
				n.setMeasurement(m, ComponentMeasurer.calculate(m, n));
			}
			fireProgressEvent();
		}

		fireIndeterminateState();
		// Clear all calculated median values in the collection and
		// recalculate. This ensures any values dependent on landmarks
		// (e.g. bounding dimensions) are correct
		for (Measurement m : dataset.getAnalysisOptions()
				.orElseThrow(MissingOptionException::new)
				.getRuleSetCollection()
				.getMeasurableValues()) {
			collection.clear(m, CellularComponent.NUCLEUS);
			// Force recalculation
			collection.getMedian(m, CellularComponent.NUCLEUS, MeasurementScale.PIXELS);
		}
	}

	/**
	 * Identify the RP from the median profile, and use this to refine RP placement
	 * in each nucleus.
	 * 
	 * @param collection
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 * @throws ProfileException
	 * @throws NoDetectedIndexException
	 * @throws ComponentCreationException
	 * @throws IndexOutOfBoundsException
	 */
	private synchronized void identifyRP(@NonNull ICellCollection collection)
			throws MissingLandmarkException, MissingProfileException, ProfileException,
			NoDetectedIndexException, IndexOutOfBoundsException {
		// Build the profile collection based on the current RP
		// positions in each nucleus
		collection.getProfileCollection().calculateProfiles();

		// Create a median from the current reference points in the nuclei
		IProfile median = collection.getProfileCollection().getProfile(ProfileType.ANGLE,
				OrientationMark.REFERENCE,
				Stats.MEDIAN);

		Landmark rp = collection.getRuleSetCollection().getLandmark(OrientationMark.REFERENCE)
				.orElseThrow(MissingLandmarkException::new);

		// RP index *should be* zero in the median profile at this point
		// Check this before updating nuclei
		int rpIndex = ProfileIndexFinder.identifyIndex(collection, rp);

		// Offset the median profile to place the RP at zero
		// This does not affect the actual median profile
		IProfile templateProfile = median.startFrom(rpIndex);

		// Using the template we created, update the position of the RP
		// in each nucleus.
		updateLandmarkToMedianBestFit(collection, rp,
				ProfileType.ANGLE,
				templateProfile);

		// Regenerate the profile aggregates based on the new RP positions
		// This should create a new median profile with the RP at zero
		collection.getProfileCollection().calculateProfiles();

		// Test if the recalculated profile aggregate naturally puts the RP at zero
		rpIndex = ProfileIndexFinder.identifyIndex(collection, rp);

		int coercionCounter = 0;
		while (rpIndex != 0 && coercionCounter++ < MAX_COERCION_ATTEMPTS) {
			rpIndex = coerceRPToZero(collection);
		}

		if (coercionCounter == MAX_COERCION_ATTEMPTS && rpIndex != 0)
			LOGGER.fine("Unable to coerce RP to index zero");

		if (!dv.validate(dataset))
			throw new ProfileException(
					"Dataset does not validate after finding RP: " + dv.getSummary()
							+ dv.getErrors());
	}

	/**
	 * Identify tags that are not core tags (i.e not the RP) using the median
	 * profile, and propagate these to nuclei
	 * 
	 * @param collection the collection to work on
	 * @throws NoDetectedIndexException
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 * @throws ProfileException
	 * @throws ComponentCreationException
	 * @throws IndexOutOfBoundsException
	 * @throws MissingOptionException
	 */
	private synchronized void identifyOtherLandmarks(ICellCollection collection)
			throws MissingLandmarkException, MissingProfileException, ProfileException,
			NoDetectedIndexException, IndexOutOfBoundsException, MissingOptionException {
		// Identify the border tags in the median profile

		// Which landmarks do we care about? Those defined in the dataset options.
		Set<Landmark> lms = dataset.getAnalysisOptions()
				.orElseThrow(MissingOptionException::new)
				.getRuleSetCollection()
				.getLandmarks();

		Landmark rp = collection.getRuleSetCollection()
				.getLandmark(OrientationMark.REFERENCE)
				.orElseThrow(MissingLandmarkException::new);

		for (Landmark om : lms) {
			// Don't identify the RP again
			if (rp.equals(om))
				continue;

			int index = ProfileIndexFinder.identifyIndex(collection, om);

			// Add the index to the median profiles
			collection.getProfileCollection().setLandmark(om,
					CellularComponent.wrapIndex(index, collection.getMedianArrayLength()));

			// Create a median from the current landmark
			IProfile lmMedian = collection.getProfileCollection().getProfile(ProfileType.ANGLE, om,
					Stats.MEDIAN);

			// Find the best position for the landmark in each nucleus
			updateLandmarkToMedianBestFit(collection, om, ProfileType.ANGLE,
					lmMedian);
		}
	}

	/**
	 * Rebuild the median and offset the nuclei to set the RP at zero
	 * 
	 * @param collection
	 * @return
	 * @throws NoDetectedIndexException
	 * @throws MissingLandmarkException
	 * @throws MissingProfileException
	 * @throws ProfileException
	 * @throws ComponentCreationException
	 * @throws IndexOutOfBoundsException
	 */
	private int coerceRPToZero(ICellCollection collection)
			throws NoDetectedIndexException, MissingLandmarkException,
			MissingProfileException, ProfileException, IndexOutOfBoundsException {

		Landmark rp = collection.getRuleSetCollection().getLandmark(OrientationMark.REFERENCE)
				.orElseThrow(MissingLandmarkException::new);

		// check the RP index in the median
		int rpIndex = ProfileIndexFinder.identifyIndex(collection, rp);

		// If RP is not at zero, update
		if (rpIndex != 0) {
			IProfile median = collection.getProfileCollection().getProfile(ProfileType.ANGLE,
					OrientationMark.REFERENCE,
					Stats.MEDIAN);

			// Get the median offset to the better start index
			IProfile templateProfile = median.startFrom(rpIndex);

			// Update the offsets in the profile collection to the new RP
			updateLandmarkToMedianBestFit(collection, rp,
					ProfileType.ANGLE,
					templateProfile);

			// At this stage we don't need to preserve the profile collection
			// because there are no other saved landmarks
			collection.getProfileCollection().calculateProfiles();

			// Find the effects of the offsets on the RP
			// It should be found at zero
			rpIndex = ProfileIndexFinder.identifyIndex(collection, rp);
		}

		return rpIndex;
	}

	/**
	 * Update the given tag in each nucleus of the collection to the index with a
	 * best fit of the profile to the given median profile
	 * 
	 * @param lm     the landmark to fit
	 * @param type   the profile type to fit against
	 * @param median the template profile to offset against
	 * @throws ProfileException
	 * @throws MissingProfileException
	 * @throws MissingLandmarkException
	 * @throws ComponentCreationException
	 * @throws IndexOutOfBoundsException
	 * @throws
	 */
	private void updateLandmarkToMedianBestFit(@NonNull ICellCollection collection,
			@NonNull Landmark lm,
			@NonNull ProfileType type,
			@NonNull IProfile median)
			throws MissingProfileException, ProfileException, MissingLandmarkException,
			IndexOutOfBoundsException {

		for (Nucleus n : collection.getNuclei()) {
			if (n.isLocked())
				continue;

			// Get the nucleus profile starting at the landmark
			// Find the best offset needed to make it match the median profile
			int offset = n.getProfile(type, lm).findBestFitOffset(median);

			// Update the landmark position to the original index plus the offset
			n.setLandmark(lm, n.wrapIndex(n.getBorderIndex(lm) + offset));

			// Update measurements - many are based on orientation
			n.clearMeasurements();
			fireProgressEvent();
		}

		// Update the consensus nucleus
		if (collection.hasConsensus()) {
			// Get the nucleus profile starting at the landmark
			// Find the best offset needed to make it match the median profile
			int offset = collection.getRawConsensus().getProfile(type, lm)
					.findBestFitOffset(median);

			// Update the landmark position to the original index plus the offset
			collection.getRawConsensus().setLandmark(lm, collection.getRawConsensus()
					.wrapIndex(collection.getRawConsensus().getBorderIndex(lm) + offset));

		}
	}
}
