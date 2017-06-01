/*******************************************************************************
 *      Copyright (C) 2016 Ben Skinner
 *
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.components.generic;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;

/**
 * Abstract base class for profiles. Not yet in use.
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
public abstract class AbstractProfile implements IProfile {

    private static final long serialVersionUID = 1L;

    @Override
    public int getIndexOfMax() throws ProfileException {

        BooleanProfile b = new BooleanProfile(this, true);
        return getIndexOfMax(b);
    }

    @Override
    public int getIndexOfMin() throws ProfileException {

        BooleanProfile b = new BooleanProfile(this, true);
        return getIndexOfMin(b);
    }

    /*
     * Interpolate another profile to match this, and move this profile along it
     * one index at a time. Find the point of least difference, and return this
     * offset. Returns the positive offset to this profile
     */
    /*
     * (non-Javadoc)
     * 
     * @see
     * components.generic.IProfile#getSlidingWindowOffset(components.generic.
     * IProfile)
     */
    @Override
    public int getSlidingWindowOffset(IProfile testProfile) throws ProfileException {

        double lowestScore = this.absoluteSquareDifference(testProfile);
        int index = 0;
        for (int i = 0; i < this.size(); i++) {

            IProfile offsetProfile = this.offset(i);

            double score = offsetProfile.absoluteSquareDifference(testProfile);
            if (score < lowestScore) {
                lowestScore = score;
                index = i;
            }

        }
        return index;
    }

    /**
     * Check the lengths of the two profiles. Return the first profile
     * interpolated to the length of the longer.
     * 
     * @param profile1
     *            the profile to return interpolated
     * @param profile2
     *            the profile to compare
     * @return a new profile with the length of the longest input profile
     * @throws ProfileException
     *             if the interpolation fails
     */
    protected IProfile equaliseLengths(IProfile profile1, IProfile profile2) throws ProfileException {
        if (profile1 == null || profile2 == null) {
            throw new IllegalArgumentException("Input profile is null when equilising lengths");
        }
        // profile 2 is smaller
        // return profile 1 unchanged
        if (profile2.size() < profile1.size()) {
            return profile1;
        } else {
            // profile 1 is smaller; interpolate to profile 2 length
            profile1 = profile1.interpolate(profile2.size());
        }

        return profile1;
    }

    @Override
    public IProfile getSubregion(IBorderSegment segment) throws ProfileException {

        if (segment == null) {
            throw new IllegalArgumentException("Segment is null");
        }

        if (segment.getTotalLength() != this.size()) {
            throw new ProfileException("Segment comes from a different length profile");
        }
        return getSubregion(segment.getStartIndex(), segment.getEndIndex());
    }

}
