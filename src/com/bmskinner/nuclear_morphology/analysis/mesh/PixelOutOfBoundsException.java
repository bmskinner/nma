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


package com.bmskinner.nuclear_morphology.analysis.mesh;

/**
 * Throw when a pixel cannot be mapped to a mesh face coordinate
 * 
 * @author bms41
 *
 */
public class PixelOutOfBoundsException extends Exception {
    private static final long serialVersionUID = 1L;

    public PixelOutOfBoundsException() {
        super();
    }

    public PixelOutOfBoundsException(String message) {
        super(message);
    }

    public PixelOutOfBoundsException(String message, Throwable cause) {
        super(message, cause);
    }

    public PixelOutOfBoundsException(Throwable cause) {
        super(cause);
    }
}
