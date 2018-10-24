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
package com.bmskinner.nuclear_morphology.components.nuclear;

import java.io.File;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.DefaultCellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;

import ij.gui.Roi;

/**
 * The default implementation of the lobe interface.
 * 
 * @author ben
 * @since 1.13.5
 *
 */
public class DefaultLobe extends DefaultCellularComponent implements Lobe {

    private static final long serialVersionUID = 1L;

    /**
     * Construct with an ROI, a source image and channel, and the original
     * position in the source image. It sets the immutable original centre of
     * mass, and the mutable current centre of mass. It also assigns a random ID
     * to the component.
     * 
     * @param roi the roi of the object
     * @param centerOfMass the original centre of mass of the component
     * @param source the image file the component was found in
     * @param channel the RGB channel the component was found in
     * @param position the bounding position of the component in the original image
     */
    public DefaultLobe(Roi roi, IPoint centreOfMass, File source, int channel, int[] position) {
        super(roi, centreOfMass, source, channel, position);
    }
    
    public DefaultLobe(Roi roi, IPoint centreOfMass, File source, int channel, int[] position, UUID id) {
        super(roi, centreOfMass, source, channel, position, id);
    }

    protected DefaultLobe(Lobe l) {
        super(l);

    }

    @Override
    public CellularComponent duplicate() {
        return new DefaultLobe(this);
    }

    @Override
    public void alignVertically() {
        // TODO Auto-generated method stub

    }

}
