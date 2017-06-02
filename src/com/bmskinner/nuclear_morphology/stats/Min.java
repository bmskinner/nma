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
import java.util.List;

@SuppressWarnings("serial")
public class Min extends DescriptiveStatistic {

    /**
     * Calculate the maximum value of the two integers
     * 
     * @param list
     */
    public Min(int a, int b) {
        value = a < b ? a : b;
    }

    /**
     * Calculate the maximum value of the two integers
     * 
     * @param list
     */
    public Min(double a, double b) {
        value = a < b ? a : b;
    }

    /**
     * Calculate the maximum value of the two integers
     * 
     * @param list
     */
    public Min(Number[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);
        }
        List<? extends Number> list = Arrays.asList(array);
        compareList(list);
    }

    /**
     * Calculate the maximum value in the given list
     * 
     * @param list
     */
    public Min(List<? extends Number> list) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException(NULL_OR_EMPTY_LIST_ERROR);
        }

        compareList(list);
    }

    private void compareList(List<? extends Number> list) {
        Object firstEntry = list.get(0);

        if (firstEntry instanceof Double) {
            value = compareDouble(list);
        }

        if (firstEntry instanceof Float) {
            value = compareFloat(list);
        }

        if (firstEntry instanceof Integer) {
            value = compareInt(list);
        }

        if (firstEntry instanceof Long) {
            value = compareLong(list);
        }
    }

    private Number compareDouble(List<? extends Number> list) {
        Number result = Double.MAX_VALUE;
        for (Number n : list) {
            if (n.doubleValue() < result.doubleValue()) {
                result = n;
            }
        }
        return result;
    }

    private Number compareFloat(List<? extends Number> list) {
        Number result = Float.MAX_VALUE;
        for (Number n : list) {
            if (n.floatValue() < result.floatValue()) {
                result = n;
            }
        }
        return result;
    }

    private Number compareInt(List<? extends Number> list) {
        Number result = Integer.MAX_VALUE;
        for (Number n : list) {
            if (n.intValue() < result.intValue()) {
                result = n;
            }
        }
        return result;
    }

    private Number compareLong(List<? extends Number> list) {
        Number result = Long.MAX_VALUE;
        for (Number n : list) {
            if (n.longValue() < result.longValue()) {
                result = n;
            }
        }
        return result;
    }

}
