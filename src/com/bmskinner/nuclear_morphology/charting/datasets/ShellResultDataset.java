/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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

package com.bmskinner.nuclear_morphology.charting.datasets;

import java.util.UUID;

import org.jfree.data.KeyedObjects2D;
import org.jfree.data.Range;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;

@SuppressWarnings("serial")
public class ShellResultDataset extends DefaultStatisticalCategoryDataset {
	
	KeyedObjects2D signalGroups;
	
	
	public ShellResultDataset(){
		super();
		signalGroups = new KeyedObjects2D();
	}
		
	
	/**
	 * Add a series to this dataset, with a signal group id
	 * @param signalGroup
	 * @param mean
	 * @param stdev
	 * @param rowKey
	 * @param columnKey
	 */
	public void add(UUID signalGroup, double mean, double stdev, Comparable<?> rowKey, Comparable<?> columnKey){
		super.add(mean, stdev, rowKey, columnKey);
		signalGroups.addObject(signalGroup, rowKey, columnKey);
	}
	
	/**
	 * Get the signal group for the given row and column
	 * @param rowKey
	 * @param columnKey
	 * @return
	 */
	public UUID getSignalGroup(Comparable<?> rowKey, Comparable<?> columnKey){
		return (UUID) signalGroups.getObject(rowKey, columnKey);
	}
	
	/**
	 * Get the range of the data within this dataset
	 * @return
	 */
	public Range getVisibleRange(){
		
		double max = 0;
		double min = 0;
		
		for( int i=0; i<getColumnCount(); i++){
			Comparable columnKey = getColumnKey(i);
			
			for (int j = 0; j < getRowCount(); j++) {

				Comparable rowKey = getRowKey(j);
				
				double mean = this.getMeanValue(rowKey, columnKey).doubleValue();
				
				double stdev = this.getStdDevValue(rowKey, columnKey).doubleValue();
				
				if(mean+stdev > max){
					max = mean+stdev;
				}
				
				if(mean-stdev<min){
					min = mean-stdev;
				}
				
				
			}
		}
		
		return new Range(min*1.1, max*1.1);
		
	}

}
