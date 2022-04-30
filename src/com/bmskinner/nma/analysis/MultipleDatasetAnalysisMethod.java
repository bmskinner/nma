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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * An abstract class of analysis method designed for handling multiple analysis datasets
 * e.g. merging datasets together.
 * @author bms41
 * @since 1.13.8
 *
 */
public abstract class MultipleDatasetAnalysisMethod extends AbstractAnalysisMethod {
    
    protected final List<IAnalysisDataset> datasets = new ArrayList<>();
    
    /**
     * Construct with a list of datasets to be analysed
     * @param datasets
     */
    public MultipleDatasetAnalysisMethod(@NonNull List<IAnalysisDataset> datasets) {
        super();
        this.datasets.addAll(datasets);
    }
    
    /**
     * Construct with a single dataset to be analysed
     * @param datasets
     */
    public MultipleDatasetAnalysisMethod(@NonNull IAnalysisDataset dataset) {
        super();
        datasets.add(dataset);
    }

}
