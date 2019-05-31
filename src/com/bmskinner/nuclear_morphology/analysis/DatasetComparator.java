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
package com.bmskinner.nuclear_morphology.analysis;

import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Check datasets are equal
 * @author bms41
 * @since 1.14.0
 *
 */
public class DatasetComparator {
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.ROOT_LOGGER);
	
	/**
	 * Test if the datasets have the same values, except in ID
	 * @param d1 the first dataset
	 * @param d2 the second dataset
	 * @return
	 */
	public boolean compare(@NonNull IAnalysisDataset d1, @NonNull IAnalysisDataset d2) {
		LOGGER.fine("Comparing "+d1.getName()+" to "+d2.getName());
		if(d1==d2)
			return true;
		if(d1.isRoot() !=d2.isRoot()) {
			LOGGER.fine(String.format("Difference in root: %s versus %s", d1.isRoot(), d2.isRoot()));
			return false;
		}
			
		
		if(!d1.getAnalysisOptions().equals(d2.getAnalysisOptions())) {
			LOGGER.fine(String.format("Difference in options: %s versus %s", d1.getAnalysisOptions(), d2.getAnalysisOptions()));
			return false;
		}
		
		if(d1.getChildCount()!=d2.getChildCount()) {
			LOGGER.fine(String.format("Difference in child count: %s versus %s", d1.getChildCount(), d2.getChildCount()));
			return false;
		}
		
		if(!d1.getAllMergeSourceIDs().equals(d2.getMergeSourceIDs())) {
			LOGGER.fine(String.format("Difference in merge source ids: %s versus %s", d1.getMergeSourceIDs(), d2.getMergeSourceIDs()));
			return false;
		}
		
		if(!d1.getName().equals(d2.getName())) {
			LOGGER.fine(String.format("Difference in name: %s versus %s", d1.getName(), d2.getName()));
			return false;
		}
		
		if(!d1.getCollection().getCells().equals(d2.getCollection().getCells())) {
			LOGGER.fine(String.format("Difference in cells: %s versus %s", d1.getCollection().getCells(), d2.getCollection().getCells()));
			return false;
		}
		return true;
	}

}
