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

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;

/**
 * Store aggregated profile data.
 * 
 * @author Ben Skinner
 *
 */
public interface IProfileAggregate {

	/**
	 * Add the values from the given profile to the aggregate. The profile length
	 * will be adjusted via interpolation
	 * 
	 * @param values
	 * @throws SegmentUpdateException
	 */
	void addValues(@NonNull IProfile values) throws SegmentUpdateException;

	/**
	 * Get the median profile of the aggregate. Shortcut for getQuartile(50)
	 * 
	 * @return
	 * @throws SegmentUpdateException
	 */
	IProfile getMedian() throws SegmentUpdateException;

	/**
	 * Get the profile corresponding to the given quartile of the values in the
	 * aggregate
	 * 
	 * @param quartile - the quartile (0-100)
	 * @return
	 * @throws SegmentUpdateException
	 */
	IProfile getQuartile(int quartile) throws SegmentUpdateException;
}
