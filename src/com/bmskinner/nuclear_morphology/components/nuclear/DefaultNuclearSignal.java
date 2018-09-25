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

import java.io.File;
import java.io.IOException;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.DefaultCellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;

/**
 * An implementation of {@link INuclearSignal}.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultNuclearSignal extends DefaultCellularComponent implements INuclearSignal {

    private static final long serialVersionUID = 1L;

    private int closestNuclearBorderPoint;

    public DefaultNuclearSignal(@NonNull Roi roi, @NonNull IPoint centreOfMass, @NonNull File f, int channel, int[] position) {
        super(roi, centreOfMass, f, channel, position);
    }

    /**
     * Create a copy of the given signal
     * 
     * @param n
     */
    public DefaultNuclearSignal(@NonNull INuclearSignal n) {
        super(n);
        this.closestNuclearBorderPoint = n.getClosestBorderPoint();
    }
    
    @Override
    public INuclearSignal duplicate() {
    	return new DefaultNuclearSignal(this);
    }

    @Override
    public int getClosestBorderPoint() {
        return this.closestNuclearBorderPoint;
    }

    @Override
    public void setClosestBorderPoint(int p) {
        this.closestNuclearBorderPoint = p;
    }

    @Override
    public void alignVertically() {
        // TODO Auto-generated method stub
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + closestNuclearBorderPoint;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultNuclearSignal other = (DefaultNuclearSignal) obj;
		if (closestNuclearBorderPoint != other.closestNuclearBorderPoint)
			return false;
		return true;
	}
    
    
}
