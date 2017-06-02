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

import java.io.File;

import com.bmskinner.nuclear_morphology.components.ComponentFactory;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;

import ij.gui.Roi;

/**
 * Factory for lobe construction
 * 
 * @author ben
 * @since 1.13.5
 *
 */
public class LobeFactory implements ComponentFactory<Lobe> {

    @Override
    public Lobe buildInstance(Roi roi, File imageFile, int channel, int[] originalPosition, IPoint centreOfMass)
            throws ComponentCreationException {

        return new DefaultLobe(roi, centreOfMass, imageFile, channel, originalPosition);
    }

}
