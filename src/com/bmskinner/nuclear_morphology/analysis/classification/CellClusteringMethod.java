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
	
	protected final Map<Instance, UUID> cellToInstanceMap = new HashMap<Instance, UUID>();
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
