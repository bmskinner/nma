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

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.datasets.IAnalysisDataset;

/**
 * An abstract class of analysis method designed for handling single analysis datasets
 * @author Ben Skinner
 * @since 1.13.8
 *
 */
public abstract class SingleDatasetAnalysisMethod extends AbstractAnalysisMethod {
    
    protected final @NonNull IAnalysisDataset dataset;
    
    /**
     * Create with a dataset for analysis
     * @param dataset
     */
    protected SingleDatasetAnalysisMethod(@NonNull final IAnalysisDataset dataset) {
        super();
        this.dataset = dataset;
    }
}
