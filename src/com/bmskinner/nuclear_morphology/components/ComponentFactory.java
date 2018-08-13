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


package com.bmskinner.nuclear_morphology.components;

import ij.gui.Roi;

import java.io.File;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * The interface for component factories
 * 
 * @author ben
 * @since 1.13.3
 *
 * @param <E>
 *            the type of CellularComponent to be created
 */
public interface ComponentFactory<E extends CellularComponent> extends Loggable {

    /**
     * Create a component of the appropriate class for the factory
     * 
     * @param roi the roi to create a nucleus from
     * @param file the image file the component is found in
     * @param channel the RGB image channel
     * @param originalPosition the position of the roi in the source image in the format of
     *            {@link CellularComponent#getPosition()}
     * @param centreOfMass the centre of mass of the roi
     * @return a component of the type for this factory
     * @throws ComponentCreationException if creation fails
     */
    E buildInstance(@NonNull Roi roi, File file, int channel, int[] originalPosition, @NonNull IPoint centreOfMass)
            throws ComponentCreationException;

    /**
     * Thrown when a component cannot be created or initialised (includes when
     * profile collection or segmented profile has no assigned segments)
     * 
     * @author bms41
     * @since 1.13.4
     *
     */
    static class ComponentCreationException extends Exception {
        private static final long serialVersionUID = 1L;

        public ComponentCreationException() {
            super();
        }

        public ComponentCreationException(String message) {
            super(message);
        }

        public ComponentCreationException(String message, Throwable cause) {
            super(message, cause);
        }

        public ComponentCreationException(Throwable cause) {
            super(cause);
        }

    }

}
