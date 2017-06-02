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

    public DefaultNuclearSignal(Roi roi, IPoint centreOfMass, File f, int channel, int[] position) {
        super(roi, centreOfMass, f, channel, position);
    }

    /**
     * Create a copy of the given signal
     * 
     * @param n
     */
    public DefaultNuclearSignal(INuclearSignal n) {
        super(n);

        this.closestNuclearBorderPoint = n.getClosestBorderPoint();

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

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.INuclearSignal#duplicate()
     */
    @Override
    public INuclearSignal duplicate() {
        return new DefaultNuclearSignal(this);
    }

    @Override
    public void alignVertically() {
        // TODO Auto-generated method stub
    }

    // @Override
    // public void setPositionWithin(CellularComponent c) {
    //
    //// If the signal was never within the component, ignore
    // if( ! c.containsOriginalPoint(this.getOriginalCentreOfMass())){
    // return;
    // }
    //
    //// // Check that the centre of mass is in sync with the border list
    //// if( ! this.containsPoint(getCentreOfMass())){
    ////
    //// IPoint origCoM = this.getOriginalCentreOfMass();
    //// IPoint thisCoM = this.getCentreOfMass();
    ////
    //// // Out of sync. Move the border list back over the centre of mass
    //// // Use the difference between the original and current centres of mass
    //// // as a template
    //// double diffX = thisCoM.getX() - origCoM.getX();
    //// double diffY = thisCoM.getY() - origCoM.getY();
    ////
    //// this.offset(diffX, diffY);
    //// this.setCentreOfMassDirectly(thisCoM);
    ////
    //// // Now the centre of mass is within the border list
    //// }
    //
    // // Get the difference between the original positions of the components
    // IPoint compOrigCoM = this.getOriginalCentreOfMass();
    // IPoint origCoM = c.getOriginalCentreOfMass();
    //
    // // This is the difference that should exist between the signal CoM and
    // // the nucleus CoM.
    // double targetDiffX = compOrigCoM.getX() - origCoM.getX();
    // double targetDiffY = compOrigCoM.getY() - origCoM.getY();
    //
    // IPoint thisCoM = this.getCentreOfMass();
    // IPoint compCoM = c.getCentreOfMass();
    //
    // // This is the difference that does exist between the signal CoM and
    // // the nucleus CoM.
    // double actualDiffX = compCoM.getX() - thisCoM.getX();
    // double actualDiffY = compCoM.getY() - thisCoM.getY();
    //
    // double offsetX = actualDiffX - targetDiffX;
    // double offsetY = actualDiffY - targetDiffY;
    //
    //
    //
    // // Move the border of this signal by the required amount from
    // // the original border position of the signal
    //
    // this.offset(offsetX, offsetY);
    //// this.setCentreOfMassDirectly(thisCoM);
    //
    // }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

        in.defaultReadObject();

    }

}
