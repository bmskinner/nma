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
package com.bmskinner.nuclear_morphology.charting.options;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;

/*
 * Hold the drawing options for a table. 
 * The appropriate options
 * are retrieved on table generation.
 */
public class DefaultTableOptions extends AbstractOptions implements TableOptions {

    private ICell cell = null;

    private JTable target = null;

    private Map<Integer, TableCellRenderer> renderer = new HashMap<>(1);

    public DefaultTableOptions(List<IAnalysisDataset> list) {
        super(list);
    }

    @Override
    public ICell getCell() {
        return cell;
    }

    @Override
    public void setCell(ICell cell) {
        this.cell = cell;
    }

    public void setTarget(JTable target) {
        this.target = target;
    }

    @Override
    public JTable getTarget() {
        return this.target;
    }

    @Override
    public boolean hasTarget() {
        return this.target != null;
    }

    public void setRenderer(int column, TableCellRenderer r) {
        renderer.put(column, r);

    }

    @Override
    public TableCellRenderer getRenderer(int i) {
        return renderer.get(i);
    }

    @Override
    public Set<Integer> getRendererColumns() {
        return renderer.keySet();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((cell == null) ? 0 : cell.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        DefaultTableOptions other = (DefaultTableOptions) obj;
        if (cell != other.cell)
            return false;
        return true;
    }

}
