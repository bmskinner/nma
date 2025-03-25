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
package com.bmskinner.nma.components.options;

import com.bmskinner.nma.components.MissingDataException;

/**
 * Thrown when an expected option is not found
 * 
 * @author Ben Skinner
 *
 */
public class MissingOptionException extends MissingDataException {
	private static final long serialVersionUID = 1L;

	public MissingOptionException() {
		super();
	}

	public MissingOptionException(String message) {
		super(message);
	}

	public MissingOptionException(String message, Throwable cause) {
		super(message, cause);
	}

	public MissingOptionException(Throwable cause) {
		super(cause);
	}
}
