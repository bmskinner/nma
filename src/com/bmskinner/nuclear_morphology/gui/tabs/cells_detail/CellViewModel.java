/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.gui.tabs.cells_detail;

import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.ICell;

public class CellViewModel {

    private volatile ICell             cell      = null;
    private volatile CellularComponent component = null;

    List<CellEditingTabPanel> views = new ArrayList<CellEditingTabPanel>();

    public CellViewModel(ICell cell, CellularComponent component) {
        this.cell = cell;
        this.component = component;
    }

    public void setCell(ICell c) {

        if (c == null || c != cell) {
            synchronized (this) {
                this.cell = c;
                component = null; // component cannot be carried over
            }
            updateViews();
        }
    }

    /**
     * Swap a cell with a new version of the same cell. Used in the
     * resegmentation dialog to update the active cell without triggering a view
     * update before the dialog closes.
     * 
     * @param c
     *            the cell. Must have the same ID as the existing cell.
     */
    public void swapCell(ICell c) {
        if (c == null || c.getId().equals(cell.getId())) {
            synchronized (this) {
                this.cell = c;
                component = null; // component cannot be carried over
            }
            clearChartCache();
        }
    }

    public ICell getCell() {
        return cell;
    }

    public boolean hasCell() {
        return cell != null;
    }

    /**
     * Cause all charts with the current active cell to be redrawn
     */
    public void clearChartCache() {
        for (CellEditingTabPanel d : views) {
            d.clearCellCharts();
        }
        updateViews();

    }

    public void updateComponent() {

    }

    public void setComponent(CellularComponent component) {
        if (this.component != component) {
            this.component = component;
            updateViews();
        }
    }

    public CellularComponent getComponent() {
        return this.component;
    }

    public void updateViews() {
        for (CellEditingTabPanel d : views) {
            d.update();
            d.setEnabled(cell != null);
        }
    }

    public void addView(CellEditingTabPanel d) {
        this.views.add(d);
    }

}
