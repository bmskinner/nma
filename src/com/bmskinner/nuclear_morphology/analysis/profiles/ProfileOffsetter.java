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

import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableComponentException;
import com.bmskinner.nuclear_morphology.components.generic.UnsegmentedProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * Offset the profiles of individual nuclei within a CellCollection based on the
 * similarities of the profile to the collection median
 * 
 * @author bms41
 *
 */
public class ProfileOffsetter {
	
	private static final Logger LOGGER = Logger.getLogger(ProfileOffsetter.class.getName());

    private final ICellCollection collection;

    public ProfileOffsetter(@NonNull final ICellCollection collection) {

        if (collection == null)
            throw new IllegalArgumentException("Collection cannot be null");

        this.collection = collection;
    }

    /**
     * This method requires the frankenprofiling to be completed
     * 
     * @throws ProfileOffsetException
     */
    public void assignBorderTagToNucleiViaFrankenProfile(Tag tag) throws ProfileOffsetException {

        int index;
        try {
            index = collection.getProfileCollection().getIndex(tag);
        } catch (UnavailableBorderTagException e2) {
            throw new ProfileOffsetException("Cannot find " + tag + " index in median", e2);
        }

        UUID segID;
        ISegmentedProfile profile;
        IBorderSegment segFromRef;
        try {
            segID = collection.getProfileCollection().getSegmentContaining(tag).getID();

            profile = collection.getProfileCollection().getSegmentedProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT,
            		Stats.MEDIAN);

            segFromRef = profile.getSegment(segID);
        } catch (ProfileException | UnsegmentedProfileException | UnavailableComponentException e1) {
            LOGGER.log(Loggable.STACK, "Error getting median profile and segment", e1);
            throw new ProfileOffsetException("Cannot get median profile or segment", e1);
        }

        /*
         * Get the proportion of the index through the segment
         */
        double proportion = segFromRef.getIndexProportion(index);

        /* Go through each nucleus and apply the position  */
        for (Nucleus nucleus : collection.getNuclei()) {
        	try {

                IBorderSegment nucleusSegment = nucleus.getProfile(ProfileType.ANGLE).getSegment(segID);

                // find the index in the segment closest to the proportion 
                int newIndex = nucleusSegment.getProportionalIndex(proportion);

                if (newIndex == -1) {
                    LOGGER.warning("Cannot find " + tag + " index in nucleus profile at proportion " + proportion);
                    continue;
                }

                nucleus.setBorderTag(tag, newIndex);
            } catch (IndexOutOfBoundsException | UnavailableComponentException e) {
                LOGGER.log(Loggable.STACK, "Cannot set " + tag + " index in nucleus profile", e);
            }

        }

    }

    /**
     * Use the proportional segment method to update top and bottom vertical
     * positions within the dataset
     * 
     * @throws Exception
     */
    public void reCalculateVerticals() throws ProfileOffsetException {
        assignTopAndBottomVerticalsViaFrankenProfile();
    }

    /**
     * This method requires the frankenprofiling to be completed
     * 
     * @throws Exception
     */
    private void assignTopAndBottomVerticalsViaFrankenProfile() throws ProfileOffsetException {

        /*
         * Franken profile method: segment proportionality
         */

        assignBorderTagToNucleiViaFrankenProfile(Tag.TOP_VERTICAL);
        assignBorderTagToNucleiViaFrankenProfile(Tag.BOTTOM_VERTICAL);

        for (Nucleus nucleus : collection.getNuclei()) {
            nucleus.updateVerticallyRotatedNucleus();
        }

    }

    public class ProfileOffsetException extends Exception {
        private static final long serialVersionUID = 1L;

        public ProfileOffsetException() {
            super();
        }

        public ProfileOffsetException(String message) {
            super(message);
        }

        public ProfileOffsetException(String message, Throwable cause) {
            super(message, cause);
        }

        public ProfileOffsetException(Throwable cause) {
            super(cause);
        }
    }

}
