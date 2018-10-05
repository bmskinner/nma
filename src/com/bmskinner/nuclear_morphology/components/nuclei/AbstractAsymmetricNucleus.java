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
package com.bmskinner.nuclear_morphology.components.nuclei;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.UnprofilableObjectException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;

import ij.gui.Roi;

/**
 * The class of non-round nuclei from which all other assymetric nuclei derive
 * 
 * @author ben
 * @since 1.13.3
 */
public abstract class AbstractAsymmetricNucleus extends DefaultNucleus {

    private static final long serialVersionUID = 1L;

    // points considered to be sperm tails before filtering
    private transient List<IBorderPoint> tailEstimatePoints = new ArrayList<>(3);
    
   // Does the orientation of the nucleus have RP clockwise to the CoM. Only applicable to hooked sperm
    protected transient boolean clockwiseRP = false;

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
    public AbstractAsymmetricNucleus(Roi roi, IPoint centreOfMass, File f, int channel, int[] position, int number) {
        super(roi, centreOfMass, f, channel, position, number);
    }

    protected AbstractAsymmetricNucleus(Nucleus n) throws UnprofilableObjectException {
        super(n);
    }

    public List<IBorderPoint> getEstimatedTailPoints() {
        return tailEstimatePoints;
    }

    protected void addTailEstimatePosition(IBorderPoint p) {
        tailEstimatePoints.add(p);
    }

    @Override
    public boolean isClockwiseRP() {
        return clockwiseRP;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        tailEstimatePoints = new ArrayList<>(0);
        clockwiseRP = false;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
}
