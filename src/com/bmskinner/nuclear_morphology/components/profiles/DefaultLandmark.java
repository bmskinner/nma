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
package com.bmskinner.nuclear_morphology.components.profiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNull;

/**
 * Allow custom points of interest to be added to a nucleus.
 * 
 * @author bms41
 *
 */
public class DefaultLandmark implements Landmark {

	private static final long serialVersionUID = 1L;

	private final String name;
	private final LandmarkType type;

	/**
	 * Create a new landmark with the given name and type
	 * 
	 * @param name
	 * @param type
	 */
	public DefaultLandmark(@NonNull final String name, @NonNull final LandmarkType type) {
		this.type = type;
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public LandmarkType type() {
		return type;
	}

	@Override
	public String toString() {
		return name;
	}

	public static DefaultLandmark[] values(LandmarkType type) {

		List<Landmark> list = new ArrayList<>();
		for (Landmark o : Landmark.defaultValues()) {
			if (o.type().equals(type)) {
				list.add(o);
			}
		}

		return list.toArray(new DefaultLandmark[0]);

	}

	@Override
	public int hashCode() {
		return Objects.hash(name, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultLandmark other = (DefaultLandmark) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	@Override
	public int compareTo(Landmark tag) {
		return name.compareTo(tag.getName());
	}

}
