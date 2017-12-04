/*******************************************************************************
 *      Copyright (C) 2016 Ben Skinner
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

package com.bmskinner.nuclear_morphology.analysis;

import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;

/**
 * An abstract class of analysis method designed for handling multiple analysis datasets
 * e.g. merging datasets together.
 * @author bms41
 * @since 1.13.8
 *
 */
public abstract class MultipleDatasetAnalysisMethod  extends AbstractAnalysisMethod {
    
    final protected List<IAnalysisDataset> datasets;
    
    /**
     * Construct with a list of datasets to be analysed
     * @param datasets
     */
    public MultipleDatasetAnalysisMethod(List<IAnalysisDataset> datasets) {
        super();
        this.datasets = datasets;
    }
    
    /**
     * Construct with a single dataset to be analysed
     * @param datasets
     */
    public MultipleDatasetAnalysisMethod(IAnalysisDataset dataset) {
        super();
        this.datasets = new ArrayList<>();
        datasets.add(dataset);
    }

}
