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

import java.io.IOException;
import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;

import ij.process.ImageProcessor;

@Deprecated
public class Mitochondrion extends AbstractCellularComponent implements IMitochondrion {
	
	private static final Logger LOGGER = Logger.getLogger(Mitochondrion.class.getName());

    private static final long serialVersionUID = 1L;

    public Mitochondrion() {
        super();
    }

    public Mitochondrion(final IMitochondrion m) {
        super(m);
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        LOGGER.finest( "\tWriting mitochondrion");
        out.defaultWriteObject();
        LOGGER.finest( "\tWrote mitochondrion");
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        LOGGER.finest( "Reading mitochondrion");
        in.defaultReadObject();
        LOGGER.finest( "Read mitochondrion");
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.I#alignVertically()
     */
    @Override
    public void alignVertically() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see components.I#duplicate()
     */
    @Override
    public IMitochondrion duplicate() {
        return new Mitochondrion(this);
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
    public int compareTo(IMitochondrion o) {
        // TODO Auto-generated method stub
        return 0;
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
	public IPoint getBase() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double wrapIndex(double d) {
		return CellularComponent.wrapIndex(d, getBorderLength());
	}

}
