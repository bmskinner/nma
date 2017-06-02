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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * Calculate the standard deviation of a list of numbers
 * 
 * @author ben
 *
 */
@SuppressWarnings("serial")
public class StDev extends DescriptiveStatistic {

    public StDev(Number[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);
        }
        List<? extends Number> list = Arrays.asList(array);
        compareList(list);
    }

    public StDev(double[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);
        }
        List<Number> list = new ArrayList<Number>();
        for (double d : array) {
            list.add(d);
        }
        compareList(list);

    }

    /**
     * Calculate the mean value in the given list
     * 
     * @param list
     */
    public StDev(List<? extends Number> list) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException(NULL_OR_EMPTY_LIST_ERROR);
        }

        compareList(list);
    }

    private void compareList(List<? extends Number> list) {

        if (list.size() == 0) {
            throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);
        }

        if (list.size() < 2) {
            value = 0;
            return;
        }

        value = Math.sqrt(new Variance(list).doubleValue());

    }

}
