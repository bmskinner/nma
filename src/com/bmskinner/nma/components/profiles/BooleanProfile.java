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

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.cells.CellularComponent;

import ij.IJ;

/**
 * Use to hold boolean results from a Profile - for example, local minima or
 * maxima.
 *
 */
public class BooleanProfile implements Serializable, Iterable<Integer> {

	private static final Logger LOGGER = Logger.getLogger(BooleanProfile.class.getName());

    private static final long serialVersionUID = 1L;
    final protected boolean[] array;

    /**
     * Constructor based on an array of values.
     * 
     * @param values
     *            the array to use
     */
    public BooleanProfile(final boolean[] values) {

        this.array = new boolean[values.length];
        for (int i = 0; i < this.array.length; i++) {
            array[i] = values[i];
        }
    }

	/**
	 * Create a profile of the given length. Set all indexes to the given boolean
	 * value.
	 * 
	 * @param length
	 * @param b
	 */
    public BooleanProfile(final int length, final boolean b) {
        this.array = new boolean[length];
        for (int i = 0; i < this.array.length; i++) {
            array[i] = b;
        }
    }

    /**
     * Constructor based on an existing Profile. Makes a copy of the existing
     * Profile
     * 
     * @param p
     *            the profile to copy
     */
    public BooleanProfile(final BooleanProfile p) {

        this.array = new boolean[p.size()];
        for (int i = 0; i < this.array.length; i++) {
            array[i] = p.get(i);
        }
    }

    /**
     * Construct an empty (false) profile with the same length as the input
     * 
     * @param p
     */
    public BooleanProfile(final IProfile p) {
        this(p, false);
    }

    /**
     * Construct a profile with the same length as the input and the given
     * values
     * 
     * @param p
     *            the template profile (for length)
     * @param b
     *            the default value
     */
    public BooleanProfile(final IProfile p, boolean b) {
        this.array = new boolean[p.size()];
        for (int i = 0; i < this.array.length; i++) {
            array[i] = b;
        }
    }

    /**
     * Get the length of the array in the profile
     * 
     * @return the size of the profile
     */
    public int size() {
        return array.length;
    }

    /**
     * Set the value at the given index. Wraps out of bounds indexes
     * 
     * @param index
     * @param b
     */
    public void set(int index, boolean b) {

        if (index < 0 || index >= array.length) {
            index = CellularComponent.wrapIndex(index, array.length);
        }
        array[index] = b;
    }

    /**
     * Get the value at the given index
     * 
     * @param index
     *            the index
     * @return the value at the index
     */
    public boolean get(int index) {
        boolean result = false;

        try {
            if (index >= array.length)
				throw new Exception("Requested index %s is beyond profile bounds".formatted(index));
            result = this.array[index];
        } catch (final Exception e) {
            IJ.log("Cannot get value from profile: " + e.getMessage());
            for (final StackTraceElement el : e.getStackTrace()) {
                IJ.log(el.toString());
            }
        }
        return result;
    }

    /**
     * Get the array from the profile
     * 
     * @return an array of values
     */
    public boolean[] toArray() {
        return this.array;
    }

    /**
     * Copy the profile and offset it to start from the given index.
     * 
     * @param j
     *            the index to start from
     * @return a new offset BooleanProfile
     * @throws Exception on error
     */
    public BooleanProfile offset(int j) throws Exception {
        final boolean[] newArray = new boolean[this.size()];
        for (int i = 0; i < this.size(); i++) {
            newArray[i] = this.array[CellularComponent.wrapIndex(i + j, array.length)];
        }
        return new BooleanProfile(newArray);
    }

    /**
     * Returns true at each position if either profile is true at that position.
     * 
     * @param profile the profile to compare. Must be the same length as this
     *            profile
     * @return the new profile
     */
    public BooleanProfile or(@NonNull BooleanProfile profile) {
        if (this.size() != profile.size())
            throw new IllegalArgumentException("Profile sizes do not match");

        final boolean[] result = new boolean[this.size()];

        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] || profile.get(i);
        }
        return new BooleanProfile(result);
    }

    /**
     * Returns true at each position if both profiles are true at that position
     * 
     * @param profile the profile to compare. Must be the same length as this
     *            profile
     * @return the new profile
     */
    public BooleanProfile and(@NonNull BooleanProfile profile) {
        if (array.length != profile.size())
            throw new IllegalArgumentException("Profile sizes do not match");
        
        final boolean[] result = new boolean[array.length];

        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] && profile.get(i);
        }
        return new BooleanProfile(result);
    }

    /**
     * Inverts the profile.
     * 
     * @return a new profile
     */
    public BooleanProfile invert() {
        final boolean[] result = new boolean[this.size()];

        for (int i = 0; i < array.length; i++) {
            result[i] = !array[i];
        }
        return new BooleanProfile(result);
    }
    
    /**
     * Count the number of true values in the profile
     * @return
     */
    public int countTrue(){
        int i=0;
        for(final boolean b : array){
            if(b) {
				i++;
			}
        }
        return i;
    }
    
    /**
     * Count the number of false values in the profile
     * @return
     */
    public int countFalse(){
        return array.length - countTrue();
    }
    
	/**
	 * Dilate this profile in a sliding window.
	 * 
	 * @param windowSize the width of the dilation. Should be odd. e.g. Use 3 to
	 *                   dilate one index either side of a given index.
	 * @return
	 */
	public BooleanProfile dilate(int windowSize) {
		if (windowSize < 3)
			throw new IllegalArgumentException(
					"Window size for boolean profile dilation must be at least 3 (%s provided)".formatted(windowSize));

		// Convert window size to a range centred on 0
		final int distance = windowSize / 2;
		LOGGER.finest("Window size is %s, distance is %s".formatted(windowSize, distance));

		BooleanProfile result = new BooleanProfile(this);

		for (int i = 0; i < this.size(); i++) {
			
			final BooleanProfile temp = new BooleanProfile(this);

			for (int j = i - distance; j <= i + distance; j++) {
				final int testIndex = temp.wrap(j);
				temp.set(temp.wrap(testIndex), temp.get(i) | temp.get(temp.wrap(testIndex)));
			}

			result = result.or(temp);
		}

		return result;

	}

	public BooleanProfile erode(int windowSize) {
		if (windowSize < 3)
			throw new IllegalArgumentException(
					"Window size for boolean profile erosion must be at least 3 (%s provided)".formatted(windowSize));

		// Convert window size to a range centred on 0
		final int distance = windowSize / 2;
		LOGGER.finest("Window size is %s, distance is %s".formatted(windowSize, distance));

		BooleanProfile result = new BooleanProfile(this);

		for (int i = 0; i < this.size(); i++) {

			final BooleanProfile temp = new BooleanProfile(this);

			for (int j = i - distance; j <= i + distance; j++) {
				final int testIndex = temp.wrap(j);
				LOGGER.finest("i %s, j %s".formatted(i, testIndex));
				temp.set(temp.wrap(testIndex), temp.get(i) & temp.get(temp.wrap(testIndex)));
			}

			result = result.and(temp);
		}

		return result;

	}

	public int wrap(int index) {
		if (index < 0)
			return wrap(size() + index);
		if (index < size())
			return index;
		return index % size();
	}

	@Override
	public Iterator<Integer> iterator() {
		return IntStream.range(0, array.length).iterator();
	}
    
    @Override
    public String toString(){
    	return Arrays.toString(array);
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(array);
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
		final BooleanProfile other = (BooleanProfile) obj;
		return Arrays.equals(array, other.array);
	}

	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }

}
