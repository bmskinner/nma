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


package com.bmskinner.nuclear_morphology.components.nuclear;

import ij.gui.Roi;
import ij.process.ImageProcessor;

import java.awt.Shape;
import java.io.File;
import java.io.IOException;

import com.bmskinner.nuclear_morphology.components.AbstractCellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;

/**
 * A NuclearSignal is a region within a nucleus of interest, such as a
 * chromosome paint. It is detected from an image, and thus implements
 * CellularComponent
 * 
 * @author ben
 *
 */
@Deprecated
public class NuclearSignal extends AbstractCellularComponent implements INuclearSignal {

    private static final long serialVersionUID = 1L;

    private int closestNuclearBorderPoint;

    private NuclearSignal(Roi roi, File f, int channel, int[] position, IPoint centreOfMass) {
        super(roi, f, channel, position, centreOfMass);

    }

    /**
     * Create a copy of the given signal
     * 
     * @param n
     */
    private NuclearSignal(NuclearSignal n) {
        super(n);

        this.closestNuclearBorderPoint = n.closestNuclearBorderPoint;

    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.INuclearSignal#getClosestBorderPoint()
     */
    @Override
    public int getClosestBorderPoint() {
        return this.closestNuclearBorderPoint;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.INuclearSignal#setClosestBorderPoint(int)
     */
    @Override
    public void setClosestBorderPoint(int p) {
        this.closestNuclearBorderPoint = p;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        // finest("\tReading nuclear signal");
        in.defaultReadObject();
        // finest("\tRead nuclear signal");
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        // finest("\t\tWriting nuclear signal");
        out.defaultWriteObject();
        // finest("\t\tWrote nuclear signal");
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.INuclearSignal#duplicate()
     */
    @Override
    public INuclearSignal duplicate() {
        return new NuclearSignal(this);
    }

    @Override
    public void alignVertically() {
        // TODO Auto-generated method stub

    }

    @Override
    public String getSourceFileNameWithoutExtension() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isSmoothByDefault() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public ImageProcessor getRGBImage() throws UnloadableImageException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ImageProcessor getComponentRGBImage() throws UnloadableImageException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Shape toShape(MeasurementScale scale) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void updateSourceFolder(File newFolder) {
        // TODO Auto-generated method stub

    }

	@Override
	public IPoint getBase() {
		// TODO Auto-generated method stub
		return null;
	}

    // @Override
    // public void setPositionWithin(CellularComponent c) {
    // // TODO Auto-generated method stub
    //
    // }
}
