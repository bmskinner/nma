package com.bmskinner.nuclear_morphology.analysis;

import java.util.List;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;

/**
 * Describes the results that can be obtained from an IAnalysisMethod
 * @author ben
 * @since 1.13.4
 *
 */
public interface IAnalysisResult {
	
	/**
	 * Get the datasets within this result
	 * @return
	 */
	List<IAnalysisDataset> getDatasets();
	
	/**
	 * Get the first dataset in the list of result datasets.
	 * Useful if there is only a single dataset in the list.
	 * @return the dataset
	 */
	IAnalysisDataset getFirstDataset();
	
	/**
	 * Get the boolean value stored at the given index.
	 * The index keys are found in each IAnalysisMethod
	 * @param i the  index to fetch
	 * @return the boolean at that index
	 */
	boolean getBoolean(int i);

}
