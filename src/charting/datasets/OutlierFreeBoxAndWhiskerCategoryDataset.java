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
package charting.datasets;

import java.util.ArrayList;
import java.util.List;

import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;


/**
 * Use this to draw boxplots with no outlier circles
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class OutlierFreeBoxAndWhiskerCategoryDataset extends DefaultBoxAndWhiskerCategoryDataset {
	
	public OutlierFreeBoxAndWhiskerCategoryDataset(){
		super();
	}
	
	@Override
	public List<?> getOutliers(int row, int column){
		return new ArrayList();
		
	}

}
