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
package com.bmskinner.nuclear_morphology.components.profiles;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;

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
    
    @Override
	public int findBestFitOffset(@NonNull IProfile testProfile) throws ProfileException {
		return findBestFitOffset(testProfile, 0, size());
	}
	
	@Override
	public int findBestFitOffset(@NonNull IProfile testProfile, int minOffset, int maxOffset) throws ProfileException {
		double lowestScore = Double.MAX_VALUE;
      int index = 0;
      for (int i=minOffset; i <maxOffset; i++) {

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
     * @param profile1 the profile to be interpolated
     * @param profile2 the second profile
     * @return a new profile with the length of the longest input profile
     * @throws ProfileException
     *             if the interpolation fails
     */
    protected IProfile equaliseLengths(IProfile profile1, IProfile profile2) throws ProfileException {
        if (profile1 == null || profile2 == null)
            throw new IllegalArgumentException("Input profile is null when equilising lengths");

        if (profile2.size() < profile1.size())
            return profile1;
        
		// profile 1 is smaller; interpolate to profile 2 length
		profile1 = profile1.interpolate(profile2.size());

        return profile1;
    }

    @Override
    public IProfile getSubregion(IProfileSegment segment) throws ProfileException {

        if (segment == null) {
            throw new IllegalArgumentException("Segment is null");
        }

        if (segment.getProfileLength() != this.size()) {
            throw new IllegalArgumentException("Segment comes from a different length profile");
        }
        return getSubregion(segment.getStartIndex(), segment.getEndIndex());
    }

}
