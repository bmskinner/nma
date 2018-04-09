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

import org.eclipse.jdt.annotation.NonNull;

@SuppressWarnings("serial")
public class Mean extends DescriptiveStatistic {

    public Mean(Number[] array) {
        if (array == null || array.length == 0) {
            throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);
        }
        List<? extends Number> list = Arrays.asList(array);
        compareList(list);
    }

    /**
     * Calculate the mean value in the given list
     * 
     * @param list
     */
    public Mean(List<? extends Number> list) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException(NULL_OR_EMPTY_LIST_ERROR);
        }

        compareList(list);
    }

    private void compareList(@NonNull List<? extends @NonNull Number> list) {

        if (list.size() == 0) {
            throw new IllegalArgumentException(NULL_OR_EMPTY_ARRAY_ERROR);
        }

        if (list.size() == 1) {
            value = list.get(0);
            return;
        }

        double sum = 0;
        for (Number d : list) {
            sum += d.doubleValue();
        }
        value = sum / list.size();

    }
}
