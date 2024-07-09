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
package com.bmskinner.nma.components.profiles;

import java.util.Arrays;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.stats.Stats;

/**
 * Aggregate profiles from individual nuclei for calculation of median and
 * quartile profiles
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public class DefaultProfileAggregate implements IProfileAggregate {

	private static final Logger LOGGER = Logger.getLogger(DefaultProfileAggregate.class.getName());

	/** the values samples per profile */
	private float[][] aggregate;

	/** the length of the aggregate */
	private final int length;

	/** the number of profiles in the aggregate */
	private final int profileCount;

	/** track the number of profiles added to the aggregate */
	private int counter = 0;

	/**
	 * Create specifying the length of the profile, and the number of profiles
	 * expected
	 * 
	 * @param length
	 * @param profileCount
	 */
	public DefaultProfileAggregate(final int length, final int profileCount) {
		if (profileCount <= 0)
			throw new IllegalArgumentException("Cannot have zero profiles in aggregate");
		this.length = length;
		this.profileCount = profileCount;

		aggregate = new float[length][profileCount];
	}

	@Override
	public void addValues(@NonNull final IProfile profile) throws SegmentUpdateException {

		if (counter >= profileCount)
			throw new IllegalArgumentException("Aggregate is full");

		/*
		 * Make the profile the desired length, sample each point and add it to the
		 * aggregate
		 */
		IProfile interpolated = profile.interpolate(length);
		for (int i = 0; i < length; i++) {
			float d = (float) interpolated.get(i);
			aggregate[i][counter] = d;
		}

		counter++;

	}

	@Override
	public IProfile getMedian() throws SegmentUpdateException {
		return getQuartile(Stats.MEDIAN);
	}

	@Override
	public IProfile getQuartile(int quartile) throws SegmentUpdateException {
		float[] medians = new float[length];

		for (int i = 0; i < length; i++) {
			medians[i] = Stats.quartile(aggregate[i], quartile);
		}
		return new DefaultProfile(medians);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.deepHashCode(aggregate);
		result = prime * result + counter;
		result = prime * result + length;
		result = prime * result + profileCount;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultProfileAggregate other = (DefaultProfileAggregate) obj;
		if (!Arrays.deepEquals(aggregate, other.aggregate))
			return false;
		if (counter != other.counter)
			return false;
		if (length != other.length)
			return false;
		if (profileCount != other.profileCount)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DefaultProfileAggregate [aggregate="
				+ Arrays.toString(aggregate)
				+ ", length=" + length
				+ ", profileCount=" + profileCount
				+ ", counter=" + counter + "]";
	}

}
