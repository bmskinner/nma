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

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder.NoDetectedIndexException;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.BorderTagObject;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.stats.Quartile;

/**
 * The method for profiling nuclei within a dataset
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class DatasetProfilingMethod extends SingleDatasetAnalysisMethod {

    private static final Tag DEFAULT_BORDER_TAG = Tag.REFERENCE_POINT;

    public static final int RECALCULATE_MEDIAN = 0;

    public DatasetProfilingMethod(IAnalysisDataset dataset) {
        super(dataset);
    }

    @Override
    public IAnalysisResult call() throws Exception {

        run();
        IAnalysisResult r = new DefaultAnalysisResult(dataset);

        return r;
    }

    private void run() {
        runProfiler(DEFAULT_BORDER_TAG);
    }

    /**
     * Calculaate the median profile of the colleciton, and generate the best
     * fit offsets of each nucleus to match
     * 
     * @param collection
     * @param pointType
     */
    private void runProfiler(Tag pointType) {

        try {
            fine("Profiling collection");

            /*
             * The individual nuclei within the collection have had RP
             * determined from their internal profiles. (This is part of Nucleus
             * constructor)
             * 
             * Build the median based on the RP indexes.
             * 
             * 
             * If moving RP index in a nucleus improves the median, move it.
             * 
             * Continue until the best-fit of RP has been obtained.
             * 
             * Find the OP and other BorderTags in the median
             * 
             * Apply to nuclei using offsets
             * 
             * 
             */

            ICellCollection collection = dataset.getCollection();

            // Build the ProfileCollections for each ProfileType
            collection.createProfileCollection();
            // collection.getProfileManager().createProfileCollections(false);
            finest("Created profile collections");

            // Create a median from the current reference points in the nuclei
            IProfile median = collection.getProfileCollection().getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT,
                    Quartile.MEDIAN);
            finest("Fetched median from initial RP");

            // RP index *should be* zero in the median profile at this point
            // Check this before updating nuclei
            ProfileIndexFinder finder = new ProfileIndexFinder();
            int rpIndex = finder.identifyIndex(collection, Tag.REFERENCE_POINT);
            fine("RP in default median is located at index " + rpIndex);

            // Update the nucleus profiles to best fit the median
            collection.getProfileManager().offsetNucleusProfiles(Tag.REFERENCE_POINT, ProfileType.ANGLE, median);

            // fine("Current median profile:");
            // fine(collection.getProfileCollection(ProfileType.ANGLE).getProfile(DEFAULT_BORDER_TAG,
            // 50).toString());
            // Now each nucleus should be at the best fit to the median profile
            // from the RP
            // Rebuilding the median may cause the RP index to change in the
            // median

            int coercionCounter = 0;
            while (rpIndex != 0) {

                // Rebuild the median and offset the nuclei until the RP settles
                // at zero
                fine("Coercing RP to zero, round " + coercionCounter);
                coercionCounter++;
                rpIndex = coerceRPToZero(collection);

                if (coercionCounter > 50) {
                    warn("Unable to cleanly assign RP");
                    break;
                }
            }

            fine("Identified best RP in nuclei and constructed median profiles");

            fine("Current state of profile collection:" + collection.getProfileCollection().tagString());

            fine("Identifying OP and other BorderTags");

            // Identify the border tags in the median profile

            for (Tag tag : BorderTagObject.values()) {

                // Don't identify the RP again, it could cause off-by-one errors
                // We do need to assign the RP in other ProfileTypes though
                if (tag.equals(Tag.REFERENCE_POINT)) {

                    fine("Checking location of RP in profile");
                    int index = finder.identifyIndex(collection, tag);
                    fine("RP is found at index " + index);
                    continue;
                }

                int index = 0;

                try {

                    index = finder.identifyIndex(collection, tag);

                } catch (NoDetectedIndexException e) {
                    warn("Unable to detect " + tag + " using default ruleset");

                    if (tag.type()
                            .equals(com.bmskinner.nuclear_morphology.components.generic.BorderTag.BorderTagType.CORE)) {
                        warn("Falling back on reference point");
                    }
                    continue;
                } catch (IllegalArgumentException e) {
                    fine("No ruleset for " + tag + "; skipping");
                    continue;
                }

                // Add the index to the median profiles
                collection.getProfileManager().updateProfileCollectionOffsets(tag, index);

                fine(tag + " in median is located at index " + index);

                // Create a median from the current reference points in the
                // nuclei
                IProfile tagMedian = collection.getProfileCollection().getProfile(ProfileType.ANGLE, tag,
                        Quartile.MEDIAN);

                collection.getProfileManager().offsetNucleusProfiles(tag, ProfileType.ANGLE, tagMedian);
                fine("Assigned offset in nucleus profiles for " + tag);
            }

            fine("Finished profiling collection");

        } catch (Exception e) {
            error("Error in dataset profiling", e);
        }
    }

    private int coerceRPToZero(ICellCollection collection) throws NoDetectedIndexException {

        ProfileIndexFinder finder = new ProfileIndexFinder();

        // check the RP index in the median
        int rpIndex = finder.identifyIndex(collection, Tag.REFERENCE_POINT);
        fine("RP in median is located at index " + rpIndex);

        // If RP is not at zero, update
        if (rpIndex != 0) {

            fine("RP in median is not yet at zero");

            // Update the offsets in the profile collection to the new RP
            collection.getProfileManager().updateRP(rpIndex);

            // Find the effects of the offsets on the RP
            // It should be found at zero
            finer("Checking RP index again");
            rpIndex = finder.identifyIndex(collection, Tag.REFERENCE_POINT);
            fine("RP in median is now located at index " + rpIndex);
            // fine("Current median profile:");
            // fine(collection.getProfileCollection(ProfileType.ANGLE).getProfile(DEFAULT_BORDER_TAG,
            // Constants.MEDIAN).toString());

            fine("Current state of profile collection:");
            fine(collection.getProfileCollection().tagString());

        }

        return rpIndex;
    }
}
