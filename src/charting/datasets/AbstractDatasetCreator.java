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

import gui.Labels;

import java.text.DecimalFormat;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import logging.Loggable;

public class AbstractDatasetCreator implements Loggable {	
		
	/**
	 * The standard formatter for datasets. At least one integer, and 2 decimals: 0.00
	 */
	public static final DecimalFormat DEFAULT_DECIMAL_FORMAT = new DecimalFormat("#0.00"); 
	
	static {
		
		DEFAULT_DECIMAL_FORMAT.setMinimumFractionDigits(2);
		DEFAULT_DECIMAL_FORMAT.setMinimumIntegerDigits(1);
	}
	
	/**
	 * Create an empty table declaring no data is loaded
	 * @return
	 */
	public static TableModel createBlankTable(){
		DefaultTableModel model = new DefaultTableModel();
		model.addColumn(Labels.NO_DATA_LOADED);
		return model;
	}
	
	/**
	 * Create an empty table declaring no data is loaded
	 * @return
	 */
	public static TableModel createLoadingTable(){
		DefaultTableModel model = new DefaultTableModel();
		model.addColumn(Labels.LOADING_DATA);
		return model;
	}

}
