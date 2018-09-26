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


package com.bmskinner.nuclear_morphology.components.generic;

import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.logging.Loggable;
import com.bmskinner.nuclear_morphology.stats.Stats;

/**
 * This is for testing a replacement of the profile aggregate using arrays
 * instead of collections. Not serializable.
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public class DefaultProfileAggregate implements Loggable, IProfileAggregate {

	/** the values samples per profile */
    private final float[][] aggregate;  
    
    /** the length of the aggregate */
    private final int length; 
    
    /** the number of profiles in the aggregate */
    private final int profileCount;

    /** track the number of profiles added to the aggregate */
    private int counter = 0;

    /**
     * Create specifying the length of the profile, and the number of profiles expected
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
	public IProfileAggregate duplicate() {
		DefaultProfileAggregate result = new DefaultProfileAggregate(length,profileCount);
		for(int i=0; i<aggregate.length; i++)
			for(int j=0; j<aggregate[0].length; j++)
				result.aggregate[i][j] = aggregate[i][j];
		return result;
	}
    
    

    @Override
	public void addValues(@NonNull final IProfile profile) throws ProfileException {

        if (counter >= profileCount)
            throw new ProfileException("Aggregate is full");

        /*
         * Make the profile the desired length, sample each point and add it to
         * the aggregate
         */

        IProfile interpolated = profile.interpolate(length);
        for (int i = 0; i < length; i++) {
            float d = (float) interpolated.get(i);
            aggregate[i][counter] = d;

        }

        counter++;

    }

    @Override
	public int length() {
        return length;
    }

    @Override
	public IProfile getMedian() {
        return calculateQuartile(Stats.MEDIAN);
    }

    public IProfile getQuartile(float quartile) {

        return calculateQuartile((int) quartile);
    }

    /**
     * Get the angle values at the given position in the aggregate.
     * 
     * @param position
     *            the position to search. Must be between 0 and the length of
     *            the aggregate.
     * @return an unsorted array of the values at the given position
     */
    public float[] getValuesAtPosition(int position) {
        if (position < 0 || position > length) {
            throw new IllegalArgumentException("Desired position is out of range: " + position);
        }
        return getValuesAtIndex(position);
    }

    /**
     * Get the angle values at the given position in the aggregate. If the
     * requested position is not an integer, the closest integer index values
     * are returned
     * 
     * @param position
     *            the position to search. Must be between 0 and 1.
     * @return an unsorted array of the values at the given position
     */
    @Override
	public double[] getValuesAtPosition(double position) {
        if (position < 0 || position > 1)
            throw new IllegalArgumentException("Desired x-position is out of range: " + position);

        double indexPosition = (double) this.length * position;

        // Choose the best position to return
        int index = (int) Math.round(indexPosition);

        float[] result = getValuesAtIndex(index);
        
        double[] d = new double[result.length];
        for(int i=0; i<result.length; i++){
            d[i] = result[i];
        }
        return d;
    }

    /**
     * Get the x-axis positions of the centre of each bin.
     * 
     * @return the Profile of positions
     */
    @Override
	public IProfile getXPositions() {
        float[] result = new float[length];

        float profileIncrement = 100f / (float) length;
        // start counting half a bin below zero
        // this sets the value to the bin centre
        float x = -profileIncrement / 2;

        // add the bin size for each positions
        for (int i = 0; i < length; i++) {
            x += profileIncrement;
            result[i] = x;
        }
        return new FloatProfile(result);
    }

    /*
     * 
     * PRIVATE METHODS
     * 
     */

    /**
     * Get the values from each profile at the given position in the aggregate
     * 
     * @param i
     * @return
     */
    private float[] getValuesAtIndex(int i) {
        float[] values = new float[profileCount];
        for (int n = 0; n < profileCount; n++)
            values[n] = aggregate[i][n];
        return values;
    }
    
    /**
     * Get the values for the given nucleus in the aggregate
     * 
     * @param n
     * @return
     */
    private float[] getValuesForNucleus(int n) {
        float[] values = new float[profileCount];
        for (int i = 0; i < profileCount; i++)
            values[i] = aggregate[i][n];
        return values;
    }

    /**
     * Calculate the profile for the given quartile
     * 
     * @param quartile
     * @return
     */
    private IProfile calculateQuartile(int quartile) {

        float[] medians = new float[length];

        for (int i = 0; i < length; i++) {
            float[] values = getValuesAtIndex(i);
            medians[i] = Stats.quartile(values, quartile);
        }
        return new FloatProfile(medians);
    }

    @Override
    public IProfile getQuartile(double quartile) throws ProfileException {
        return getQuartile((float) quartile);
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
		return "DefaultProfileAggregate [aggregate=" + Arrays.toString(aggregate) + ", length=" + length
				+ ", profileCount=" + profileCount + ", counter=" + counter + "]";
	}
    
    

}
