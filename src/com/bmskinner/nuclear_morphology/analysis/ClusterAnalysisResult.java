package com.bmskinner.nuclear_morphology.analysis;

import java.util.List;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.IClusterGroup;

/**
 * An extension to the default analysis result to hold cluster groups
 * @author ben
 *
 */
public class ClusterAnalysisResult extends DefaultAnalysisResult{
	
	IClusterGroup group;
	
	public ClusterAnalysisResult(IAnalysisDataset d, IClusterGroup g){
		super(d);
		group = g;
	}
	
	public ClusterAnalysisResult(List<IAnalysisDataset> list, IClusterGroup g){
		super(list);
		group = g;
	}
	
	public IClusterGroup getGroup(){
		return group;
	}
}
