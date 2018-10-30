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
 * A default implementation of the IMitochondrion interface
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public class DefaultMitochondrion extends DefaultCellularComponent implements IMitochondrion {

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
    public DefaultMitochondrion(Roi roi, IPoint centreOfMass, File f, int channel, int[] position) {
        super(roi, centreOfMass, f, channel, position);
    }

    /**
     * Construct from an existing mitochondrion
     * 
     * @param n
     *            the template mitochondrion
     */
    protected DefaultMitochondrion(IMitochondrion n) {
        super(n);
    }

    @Override
    public int compareTo(IMitochondrion arg0) {
        return 0;
    }

    @Override
    public IMitochondrion duplicate() {
        return new DefaultMitochondrion(this);
    }

    @Override
    public void alignVertically() {
        // TODO Auto-generated method stub

    }
}
