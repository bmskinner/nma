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
package com.bmskinner.nuclear_morphology.components;

import java.io.File;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;

import ij.gui.Roi;

/**
 * A default implementation of the IAcrosome interface
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public class DefaultAcrosome extends DefaultCellularComponent implements IAcrosome {

    private static final long serialVersionUID = 1L;

    /**
     * Construct with an ROI, a source image and channel, and the original
     * position in the source image
     * 
     * @param roi
     * @param f
     * @param channel
     * @param position
     * @param centreOfMass
     */
    public DefaultAcrosome(Roi roi, IPoint centreOfMass, File f, int channel, int[] position) {
        super(roi, centreOfMass, f, channel, position);
    }

    /**
     * Construct from an existing acrosome
     * 
     * @param n
     *            the template acrosome
     */
    protected DefaultAcrosome(IAcrosome n) {
        super(n);
    }

    @Override
    public int compareTo(IAcrosome arg0) {
        return 0;
    }

    @Override
    public IAcrosome duplicate() {
        return new DefaultAcrosome(this);
    }

    @Override
    public void alignVertically() {
        // TODO Auto-generated method stub

    }

}
