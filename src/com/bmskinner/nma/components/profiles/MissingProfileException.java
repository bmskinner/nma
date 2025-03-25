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
package com.bmskinner.nma.components.profiles;

import com.bmskinner.nma.components.MissingDataException;

/**
 * Thrown when a profile type is not available within a profile collection.
 * 
 * @author Ben Skinner
 * @since 1.13.3
 *
 */
public class MissingProfileException extends MissingDataException {
    private static final long serialVersionUID = 1L;

    public MissingProfileException() {
        super();
    }

    public MissingProfileException(String message) {
        super(message);
    }

    public MissingProfileException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingProfileException(Throwable cause) {
        super(cause);
    }
}
