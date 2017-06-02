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

import com.bmskinner.nuclear_morphology.components.CellularComponent;

/**
 * This holds CellularComponents for the ComponentListPanel
 * 
 * @author bms41
 *
 */
public class ComponentListCell {

    private String            name;
    private CellularComponent component;

    public ComponentListCell(final String name, final CellularComponent c) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null in component list cell");
        }

        if (c == null) {
            throw new IllegalArgumentException("Component cannot be null in component list cell");
        }

        this.name = name;
        this.component = c;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return name;
    }

    public CellularComponent getComponent() {
        return component;
    }

}
