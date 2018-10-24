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

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.ComponentFactory;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;

import ij.gui.Roi;

/**
 * Factory for creating nuclear signals
 * 
 * @author bms41
 * @since 1.13.5
 *
 */
public class SignalFactory implements ComponentFactory<INuclearSignal> {

    @Override
    public INuclearSignal buildInstance(Roi roi, File file, int channel, int[] originalPosition, IPoint centreOfMass)
            throws ComponentCreationException {
        return new DefaultNuclearSignal(roi, centreOfMass, file, channel, originalPosition);
    }

	@Override
	public INuclearSignal buildInstance(@NonNull Roi roi, File file, int channel, int[] originalPosition,
			@NonNull IPoint centreOfMass, @NonNull UUID id) throws ComponentCreationException {
		// TODO Auto-generated method stub
		return new DefaultNuclearSignal(roi, centreOfMass, file, channel, originalPosition, id);
	}

}
