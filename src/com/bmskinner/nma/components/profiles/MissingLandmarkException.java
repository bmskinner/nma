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
 * Thrown when the requested border tag is not present in a Taggable object
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public class MissingLandmarkException extends MissingDataException {
    private static final long serialVersionUID = 1L;

    public MissingLandmarkException() {
        super();
    }

    public MissingLandmarkException(String message) {
        super(message);
    }

    public MissingLandmarkException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingLandmarkException(Throwable cause) {
        super(cause);
    }
}
