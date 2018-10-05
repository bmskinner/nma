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
package com.bmskinner.nuclear_morphology.io;

/**
 * This exception gets thrown by components that try to access a source image
 * file that has become unavailable, and signals to other modules that they
 * should not try to process the source image
 * 
 * @author ben
 *
 */
public class UnloadableImageException extends Exception {
    private static final long serialVersionUID = 1L;

    public UnloadableImageException() {
        super();
    }

    public UnloadableImageException(String message) {
        super(message);
    }

    public UnloadableImageException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnloadableImageException(Throwable cause) {
        super(cause);
    }
}
