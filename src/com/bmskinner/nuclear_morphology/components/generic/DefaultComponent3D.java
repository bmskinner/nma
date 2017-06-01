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

package com.bmskinner.nuclear_morphology.components.generic;

import com.bmskinner.nuclear_morphology.components.CellularComponent;

/**
 * Create a 3D version of a cellular component by rotating about the x axis to
 * trace out a 3D shape. Used for shell simulations only at the moment.
 * 
 * @author bms41
 *
 */
public class DefaultComponent3D implements Component3D {

    private CellularComponent c;

    public DefaultComponent3D(CellularComponent c) {
        this.c = c;
    }

    @Override
    public boolean contains(Point3D p) {

        // /* The 2D component is rotated about x to generate
        // the shape.
        //
        // This means that a slice at every angle about x has the same shape.
        // We can rotate the point to z=0, and then use the 2D contains()
        // method.
        // //
        // */

        // Create a point on the x axis through the CoM adjacent to p

        Point3D origin = new Point3D((float) p.getX(), (float) c.getCentreOfMass().getY(), 0);

        // Get the length of the hypotenuse of the triangle
        double newY = origin.getLengthTo(IPoint.makeNew(p.getY(), p.getZ()));

        // rotated the point about x so z=0
        Point3D rotateX = new Point3D((float) p.getX(), (float) (origin.getY() + newY), 0);

        return c.containsPoint(rotateX);

    }

}
