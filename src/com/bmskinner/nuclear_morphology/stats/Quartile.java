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


package com.bmskinner.nuclear_morphology.stats;

import java.util.Arrays;

public class Quartile {

	public static final String NULL_OR_EMPTY_ARRAY_ERROR = "The data array either is null or does not contain any data.";
    public static final String NULL_OR_EMPTY_LIST_ERROR  = "The data list either is null or does not contain any data.";
	
    public static final int LOWER_QUARTILE = 25;
    public static final int UPPER_QUARTILE = 75;
    public static final int MEDIAN         = 50;

    /**
     * Get the quartile for a float array
     * 
     * @param values the values
     * @param quartile the quartile to find
     * @return the quartile value
     */
    public static float quartile(float[] values, int quartile) {

        if (values == null || values.length == 0)
            throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);

        if (values.length == 1)
            return values[0];

        if (values.length == 2)
            return quartile < MEDIAN ? values[0] : values[1];

        // Rank order the values
        float[] v = new float[values.length];
        System.arraycopy(values, 0, v, 0, values.length);
        Arrays.sort(v);

        int n = Math.round(((float) v.length * quartile) / 100);
        return v[n];
    }
    
    /**
     * Get the quartile for a float array
     * 
     * @param values the values
     * @param quartile the quartile to find
     * @return the quartile value
     */
    public static int quartile(int[] values, int quartile) {

        if (values == null || values.length == 0)
            throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);

        if (values.length == 1)
            return values[0];

        if (values.length == 2)
            return quartile < MEDIAN ? values[0] : values[1];

        // Rank order the values
        int[] v = new int[values.length];
        System.arraycopy(values, 0, v, 0, values.length);
        Arrays.sort(v);

        int n = Math.round(((float) v.length * quartile) / 100);

        return v[n];
    }

    /**
     * Get the quartile for a double array
     * 
     * @param values
     *            the values
     * @param quartile
     *            the quartile to find
     * @return the quartile value
     */
    public static double quartile(double[] values, int quartile) {

        if (values == null || values.length == 0)
            throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);

        if (values.length == 1)
            return values[0];

        if (values.length == 2)
            return quartile < MEDIAN ? values[0] : values[1];

        // Rank order the values
        double[] v = new double[values.length];
        System.arraycopy(values, 0, v, 0, values.length);
        Arrays.sort(v);

        int n = Math.round(((float) v.length * quartile) / 100);

        return v[n];
    }
}
