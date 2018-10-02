package com.bmskinner.nuclear_morphology.analysis;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Check datasets are equal
 * @author bms41
 * @since 1.14.0
 *
 */
public class DatasetComparator implements Loggable {
	
	
	
	/**
	 * Test if the datasets have the same values, except in ID
	 * @param d1 the first dataset
	 * @param d2 the second dataset
	 * @return
	 */
	public boolean compare(@NonNull IAnalysisDataset d1, @NonNull IAnalysisDataset d2) {
		fine("Comparing "+d1.getName()+" to "+d2.getName());
		if(d1==d2)
			return true;
		if(d1.isRoot() !=d2.isRoot()) {
			fine(String.format("Difference in root: %s versus %s", d1.isRoot(), d2.isRoot()));
			return false;
		}
			
		
		if(!d1.getAnalysisOptions().equals(d2.getAnalysisOptions())) {
			fine(String.format("Difference in options: %s versus %s", d1.getAnalysisOptions(), d2.getAnalysisOptions()));
			return false;
		}
		
		if(d1.getChildCount()!=d2.getChildCount()) {
			fine(String.format("Difference in child count: %s versus %s", d1.getChildCount(), d2.getChildCount()));
			return false;
		}
		
		if(!d1.getAllMergeSourceIDs().equals(d2.getMergeSourceIDs())) {
			fine(String.format("Difference in merge source ids: %s versus %s", d1.getMergeSourceIDs(), d2.getMergeSourceIDs()));
			return false;
		}
		
		if(!d1.getName().equals(d2.getName())) {
			fine(String.format("Difference in name: %s versus %s", d1.getName(), d2.getName()));
			return false;
		}
		
		if(!d1.getCollection().getCells().equals(d2.getCollection().getCells())) {
			fine(String.format("Difference in cells: %s versus %s", d1.getCollection().getCells(), d2.getCollection().getCells()));
			return false;
		}
		return true;
	}

}
