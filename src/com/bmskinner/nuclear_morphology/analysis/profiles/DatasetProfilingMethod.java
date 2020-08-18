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
package com.bmskinner.nuclear_morphology.analysis.profiles;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder.NoDetectedIndexException;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.BorderTag;
import com.bmskinner.nuclear_morphology.components.generic.BorderTagObject;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.rules.RuleApplicationType;
import com.bmskinner.nuclear_morphology.components.rules.RuleSet;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * The method for profiling nuclei within a dataset. This detects the optimal indexes
 * to assign border tags within each nucleus
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class DatasetProfilingMethod extends SingleDatasetAnalysisMethod {
	
	private static final Logger LOGGER = Logger.getLogger(DatasetProfilingMethod.class.getName());

	public static final int RECALCULATE_MEDIAN = 0;
	
	public static final int MAX_COERCION_ATTEMPTS = 50;
	
	private final ProfileIndexFinder finder = new ProfileIndexFinder();

	/**
	 * Create a profiler for the given dataset
	 * @param dataset
	 */
	public DatasetProfilingMethod(IAnalysisDataset dataset) {
		super(dataset);
	}

	@Override
	public IAnalysisResult call() throws Exception {
		run();
		return new DefaultAnalysisResult(dataset);
	}

	/**
	 * Calculate the median profile of the colleciton, and generate the best
	 * fit offsets of each nucleus to match. 
	 * 
	 * The individual nuclei within the collection have had RP
	 * determined from their internal profiles. (This is part of Nucleus
	 * constructor)
	 * 
	 * Build the median based on the RP indexes.
	 * If moving RP index in a nucleus improves the median, move it.
	 * 
	 * Continue until the best-fit of RP has been obtained.
	 * 
	 * Find the OP and other BorderTags in the median
	 * 
	 * Apply to nuclei using offsets
	 * 
	 * @param collection
	 * @param pointType
	 */
	private void run() throws Exception {
    	LOGGER.fine("Beginning profiling method");
    	
    	RuleApplicationType ruleType = dataset.getAnalysisOptions().get().getRuleApplicationType();
    	
    	switch(ruleType) {
	    	case VIA_MEDIAN: {
	    		runViaMedian();
	    		break;
	    	}
	    	case PER_NUCLEUS:{
	    		runPerNucleus();
	    		break;
	    	}
    	}
	}
	
	/**
	 * Detect border tags in nuclei using the dataset rulesets,
	 * and also apply rules to the median profile. The median is not 
	 * used for back-propogation of tags.
	 * @throws Exception
	 */
	private void runPerNucleus() throws ProfileException, 
	UnavailableProfileTypeException,
	UnavailableBorderTagException {
		LOGGER.fine("Detecting border tags per-nucleus");
		ICellCollection collection = dataset.getCollection();
		
		collection.createProfileCollection();
		
		// Reference points are assigned in each nucleus on creation
		// Create a median from the current reference points in the nuclei
		collection.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
		
		
		// For each tag in the dataset ruleset collection, identify the tag in nuclei
		Set<Tag> tags = collection.getRuleSetCollection().getTags();
		
		for(Tag t : tags) {
			if(Tag.REFERENCE_POINT.equals(t)) // Already set
				continue;
			List<RuleSet> ruleSets = collection.getRuleSetCollection().getRuleSets(t);
			for(Nucleus n :  collection.getNuclei()) {
				int index = 0;
				try {
					index = finder.identifyIndex(n, ruleSets);
				} catch (NoDetectedIndexException e) {
					LOGGER.fine("Cannot identify "+t+" in nucleus "+n.getNucleusNumber()+", using index 0");
					// Fall back to zero index, correct manually
				}
				if(!n.isLocked()) {
					n.setBorderTag(t, index);
				} else {
					LOGGER.fine("Nucleus "+n.getNameAndNumber()+" is locked, not changing "+t);
				}
			}
			
			// Add the index to the median profiles
			int medianIndex = 0;
			try {
				medianIndex = finder.identifyIndex(collection, ruleSets);
			} catch (NoDetectedIndexException e) {
				LOGGER.fine("Cannot identify "+t+" in median, using index 0");
			}
			collection.getProfileManager().updateProfileCollectionOffsets(t, medianIndex);
		}
	}
	
	/**
	 * Detect border tags using the median profile
	 * @throws Exception
	 */
	private void runViaMedian() throws Exception {
		LOGGER.fine("Detecting border tags via median");
		ICellCollection collection = dataset.getCollection();

		collection.createProfileCollection();
		// Create a median from the current reference points in the nuclei
		IProfile median = collection.getProfileCollection()
				.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);


		// RP index *should be* zero in the median profile at this point
		// Check this before updating nuclei
		int rpIndex = finder.identifyIndex(collection, Tag.REFERENCE_POINT);
		LOGGER.finer( "RP in default median is located at index " + rpIndex);

		IProfile templateProfile = median.offset(rpIndex);
		// Update the position of the RP in the nuclei to best fit the median
		collection.getProfileManager().updateTagToMedianBestFit(Tag.REFERENCE_POINT, ProfileType.ANGLE, templateProfile);

		// Regenerate the profile aggregates based on the new RP positions
		collection.getProfileManager().recalculateProfileAggregates();
		
		// Test if the recalculated profile aggregate naturally puts the RP at zero
		rpIndex = finder.identifyIndex(collection, Tag.REFERENCE_POINT);

		int coercionCounter = 0;
		while (rpIndex != 0 && coercionCounter++<MAX_COERCION_ATTEMPTS) {
			LOGGER.fine("Coercing RP to zero, round " + coercionCounter);
			rpIndex = coerceRPToZero(collection);
		}
		if(coercionCounter==MAX_COERCION_ATTEMPTS && rpIndex!=0)
			LOGGER.fine("Unable to coerce RP to index zero");
		
		identifyNonCoreTags(collection);
	}

	/**
	 * Identify tags that are not core tags (i.e not the RP) using
	 * the median profile, and propagate these to nuclei
	 * @param collection the collection to work on
	 * @throws NoDetectedIndexException
	 * @throws UnavailableBorderTagException
	 * @throws UnavailableProfileTypeException
	 * @throws ProfileException
	 */
	private synchronized void identifyNonCoreTags(ICellCollection collection) throws NoDetectedIndexException, UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException {
		// Identify the border tags in the median profile
		for(Tag tag : BorderTagObject.values()) {

			// Don't identify the RP again, it could cause off-by-one errors
			// We do need to assign the RP in other ProfileTypes though
			if (tag.equals(Tag.REFERENCE_POINT)) {

				LOGGER.fine("Checking location of RP in profile");
				int index = finder.identifyIndex(collection, tag);
				LOGGER.fine("RP in collection is at index " + index);
				continue;
			}

			int index = 0;

			try {
				index = finder.identifyIndex(collection, tag);
			} catch (NoDetectedIndexException e) {
				LOGGER.warning("Unable to detect " + tag + " using default ruleset");

				if (tag.type().equals(BorderTag.BorderTagType.CORE)) {
					LOGGER.warning("Falling back on reference point");
				}
				continue;
			} catch (IllegalArgumentException e) {
				LOGGER.fine("No ruleset for " + tag + "; skipping");
				continue;
			}

			// Add the index to the median profiles
			collection.getProfileManager().updateProfileCollectionOffsets(tag, index);

			LOGGER.fine(tag + " in median is located at index " + index);

			// Create a median from the current reference points in the
			// nuclei
			IProfile tagMedian = collection.getProfileCollection().getProfile(ProfileType.ANGLE, tag,
					Stats.MEDIAN);

			collection.getProfileManager().updateTagToMedianBestFit(tag, ProfileType.ANGLE, tagMedian);
			LOGGER.fine("Assigned offset in nucleus profiles for " + tag);
		}
	}

	/**
	 * Rebuild the median and offset the nuclei to set the RP at zero
	 * @param collection
	 * @return
	 * @throws NoDetectedIndexException
	 * @throws UnavailableBorderTagException
	 * @throws UnavailableProfileTypeException
	 * @throws ProfileException
	 */
	private int coerceRPToZero(ICellCollection collection) throws NoDetectedIndexException, UnavailableBorderTagException, UnavailableProfileTypeException, ProfileException {

		// check the RP index in the median
		int rpIndex = finder.identifyIndex(collection, Tag.REFERENCE_POINT);
		LOGGER.fine("RP in median is located at index " + rpIndex);

		// If RP is not at zero, update
		if (rpIndex != 0) {
			
			LOGGER.fine("RP in median is not yet at zero");
			IProfile median = collection.getProfileCollection()
					.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT, Stats.MEDIAN);
			IProfile templateProfile = median.offset(rpIndex);
			// Update the offsets in the profile collection to the new RP
			collection.getProfileManager().updateTagToMedianBestFit(Tag.REFERENCE_POINT, ProfileType.ANGLE, templateProfile);
			collection.getProfileManager().recalculateProfileAggregates();

			// Find the effects of the offsets on the RP
			// It should be found at zero
			LOGGER.finer( "Checking RP index again");
			rpIndex = finder.identifyIndex(collection, Tag.REFERENCE_POINT);
			LOGGER.fine("RP in median is now located at index " + rpIndex);
		}

		return rpIndex;
	}
}
