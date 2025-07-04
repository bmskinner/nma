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
package com.bmskinner.nma.components.cells;

import com.bmskinner.nma.components.MissingDataException;

/**
 * Thrown when the requested border point is not present in a cellular component
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
public class UnavailableBorderPointException extends MissingDataException {
    private static final long serialVersionUID = 1L;

    public UnavailableBorderPointException() {
        super();
    }

    public UnavailableBorderPointException(String message) {
        super(message);
    }

    public UnavailableBorderPointException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnavailableBorderPointException(Throwable cause) {
        super(cause);
    }
}
