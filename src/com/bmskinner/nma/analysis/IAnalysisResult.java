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
package com.bmskinner.nma.analysis;

import java.util.List;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * Describes the results that can be obtained from an IAnalysisMethod
 * 
 * @author Ben Skinner
 * @since 1.13.4
 *
 */
public interface IAnalysisResult {

    /**
     * Get the datasets within this result
     * 
     * @return
     */
    List<IAnalysisDataset> getDatasets();

    /**
     * Get the first dataset in the list of result datasets. Useful if there is
     * only a single dataset in the list.
     * 
     * @return the dataset
     */
    IAnalysisDataset getFirstDataset();

    /**
     * Get the boolean value stored at the given index. The index keys are found
     * in each IAnalysisMethod
     * 
     * @param i the index to fetch
     * @return the boolean at that index
     */
    boolean getBoolean(int i);

}
