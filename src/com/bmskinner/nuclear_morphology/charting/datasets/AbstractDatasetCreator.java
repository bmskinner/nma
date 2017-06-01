/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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

package com.bmskinner.nuclear_morphology.charting.datasets;

import java.text.DecimalFormat;

import com.bmskinner.nuclear_morphology.charting.options.DisplayOptions;
import com.bmskinner.nuclear_morphology.logging.Loggable;

public abstract class AbstractDatasetCreator<E extends DisplayOptions> implements Loggable {

    protected final E options;

    protected static final String EMPTY_STRING = "";

    public AbstractDatasetCreator(final E options) {
        if (options == null) {
            throw new IllegalArgumentException("Options cannot be null");
        }
        this.options = options;
    }

    /**
     * The standard formatter for datasets. At least one integer, and 2
     * decimals: 0.00
     */
    public static final DecimalFormat DEFAULT_DECIMAL_FORMAT = new DecimalFormat("#0.00");

    static {

        DEFAULT_DECIMAL_FORMAT.setMinimumFractionDigits(2);
        DEFAULT_DECIMAL_FORMAT.setMinimumIntegerDigits(1);
    }
}
