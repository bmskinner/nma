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
package com.bmskinner.nuclear_morphology.analysis.classification;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.options.IClusteringOptions;

import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 * This provides an interface between subclassing methods and the
 * weka library being used
 * @author ben
 * @since 1.14.0
 *
 */
public abstract class CellClusteringMethod extends SingleDatasetAnalysisMethod {
	
	protected final Map<Instance, UUID> cellToInstanceMap = new HashMap<>();
	protected final ICellCollection    collection;
	protected final IClusteringOptions options;
	
	public CellClusteringMethod(@NonNull IAnalysisDataset dataset, @NonNull IClusteringOptions options) {
        super(dataset);
        this.collection = dataset.getCollection();
        this.options = options;
    }
	
	/**
	 * Create the rows of the matrix to be analysed
	 * @return
	 * @throws Exception
	 */
	protected abstract Instances makeInstances() throws ClusteringMethodException;
	
	/**
	 * Create the columns of the matrix to be analysed
	 * @return
	 */
	protected abstract FastVector makeAttributes() throws ClusteringMethodException;
	
}
