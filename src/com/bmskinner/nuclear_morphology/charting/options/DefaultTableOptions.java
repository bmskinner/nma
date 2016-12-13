/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
package com.bmskinner.nuclear_morphology.charting.options;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;

/*
 * Hold the drawing options for a table. 
 * The appropriate options
 * are retrieved on table generation.
 */
public class DefaultTableOptions extends AbstractOptions implements TableOptions {
	
	private TableType type = null;
	
	private ICell cell      = null;
	
	private JTable target   = null;
	
	private Map<Integer, TableCellRenderer> renderer = new HashMap<Integer, TableCellRenderer>(1);
	
	public DefaultTableOptions(List<IAnalysisDataset> list) {
		super(list);

	}
	
	/* (non-Javadoc)
	 * @see charting.options.TableOptions#setType(charting.options.DefaultTableOptions.TableType)
	 */
	public void setType(TableType type){
		this.type = type;
	}

	
	/* (non-Javadoc)
	 * @see charting.options.TableOptions#getType()
	 */
	@Override
	public TableType getType(){
		return this.type;
	}
	
	

	/* (non-Javadoc)
	 * @see charting.options.TableOptions#getCell()
	 */
	@Override
	public ICell getCell() {
		return cell;
	}

	/* (non-Javadoc)
	 * @see charting.options.TableOptions#setCell(components.ICell)
	 */
	@Override
	public void setCell(ICell cell) {
		this.cell = cell;
	}
	
	
	public void setTarget(JTable target){
		this.target = target;
	}
	
	@Override
	public JTable getTarget(){
		return this.target;
	}
	
	@Override
	public boolean hasTarget(){
		return this.target!=null;
	}


	public void setRenderer(int column, TableCellRenderer r) {
		renderer.put(column,  r);
		
	}

	@Override
	public TableCellRenderer getRenderer(int i) {
		return renderer.get(i);
	}
	
	@Override
	public Set<Integer> getRendererColumns(){
		return renderer.keySet();
	}

	public enum TableType {
		ANALYSIS_PARAMETERS,
		ANALYSIS_STATS,
		VENN, 
		PAIRWISE_VENN,
		WILCOXON,
		SIGNAL_STATS_TABLE,
		MERGE_SOURCES
	}

	/* (non-Javadoc)
	 * @see charting.options.TableOptions#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		
		result = prime * result
				+ ((cell == null) ? 0 : cell.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see charting.options.TableOptions#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultTableOptions other = (DefaultTableOptions) obj;
		if (type != other.type)
			return false;
		if (cell != other.cell)
			return false;
		return true;
	}


	
	
}
