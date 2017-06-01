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

package com.bmskinner.nuclear_morphology.gui.tabs.cells_detail;

import javax.swing.ImageIcon;

import com.bmskinner.nuclear_morphology.components.ICell;

public class LabelInfo {
    private ImageIcon icon;
    private boolean   isSelected = false;
    private ICell     cell;

    public LabelInfo(ImageIcon icon, ICell cell) {
        this.icon = icon;
        this.cell = cell;
    }

    public ImageIcon getIcon() {
        return icon;
    }

    public ICell getCell() {
        return cell;
    }

    public String toString() {
        return cell == null ? "" : cell.getNucleus().getNameAndNumber();
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    public String state() {
        return this.toString() + ": " + isSelected;
    }

}