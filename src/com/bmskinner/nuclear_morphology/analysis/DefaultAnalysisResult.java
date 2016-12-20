package com.bmskinner.nuclear_morphology.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;

/**
 * The default implementation of IAnalysisResult, which provides 
 * access to datasets produced or modified in an IAnalysisMethod
 * @author ben
 * @since 1.13.4
 *
 */
public class DefaultAnalysisResult implements IAnalysisResult {

	List<IAnalysisDataset> datasets = new ArrayList<IAnalysisDataset>();
	
	// Store boolean options as needed
	Map<Integer, Boolean> booleans = new HashMap<Integer, Boolean>();
	
	public DefaultAnalysisResult(IAnalysisDataset d){
		datasets.add(d);
	}
	
	public DefaultAnalysisResult(List<IAnalysisDataset> d){
		datasets.addAll(d);
	}
	
	@Override
	public List<IAnalysisDataset> getDatasets() {
		return datasets;
	}

	@Override
	public IAnalysisDataset getFirstDataset() {
		return datasets.get(0);
	}
	
	public void setBoolean(int i, boolean b){
		this.booleans.put(i, b);
	}

	@Override
	public boolean getBoolean(int i) {
		return booleans.get(i);
	}
	
	
	
	

}
