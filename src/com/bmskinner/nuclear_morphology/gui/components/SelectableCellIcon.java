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
package com.bmskinner.nuclear_morphology.gui.components;

import java.awt.Image;

import javax.swing.ImageIcon;

import com.bmskinner.nuclear_morphology.components.cells.ICell;

import ij.process.ImageProcessor;

/**
 * Map a cell to a display image for use in curation dialogs
 * and individual cell images
 * @author bms41
 *
 */
public class SelectableCellIcon extends ImageIcon {
    private boolean   isSelected = false;
    private ICell     cell;

    /**
     * Create with an image to display and the cell it came from
     * @param ip the image to be displayed
     * @param cell the cell the image has come from
     */
    public SelectableCellIcon(ImageProcessor ip, ICell cell) {
    	this(ip.getBufferedImage(), cell);
    }
    
    /**
     * Create with an image to display and the cell it came from
     * @param img the image to be displayed
     * @param cell the cell the image has come from
     */
    public SelectableCellIcon(Image img, ICell cell) {
    	super(img);
        this.cell = cell;
    }
    
    public SelectableCellIcon() {
    	super();
        this.cell = null;
    }
    
    public ICell getCell() {
        return cell;
    }

    @Override
    public String toString() {
        return cell == null ? "" : cell.getPrimaryNucleus().getNameAndNumber();
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
