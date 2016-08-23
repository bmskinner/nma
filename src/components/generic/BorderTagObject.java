/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package components.generic;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import components.generic.BorderTag.BorderTagType;

/**
 * This is a wrapper for the BorderTag, to allow custom points of interest
 * to be added to a nucleus. For these, use the BorderTag.CUSTOM tag, and
 * a unique name
 * @author bms41
 *
 */
public class BorderTagObject implements Serializable, Comparable<BorderTagObject> {
	
	private static final long serialVersionUID = 1L;
	
	private final String name;
	private final BorderTag tag;
	
	public static final BorderTagObject REFERENCE_POINT    = new BorderTagObject(BorderTag.REFERENCE_POINT);
	public static final BorderTagObject ORIENTATION_POINT  = new BorderTagObject(BorderTag.ORIENTATION_POINT);
	public static final BorderTagObject TOP_VERTICAL       = new BorderTagObject(BorderTag.TOP_VERTICAL);
	public static final BorderTagObject BOTTOM_VERTICAL    = new BorderTagObject(BorderTag.BOTTOM_VERTICAL);
	public static final BorderTagObject INTERSECTION_POINT = new BorderTagObject(BorderTag.INTERSECTION_POINT);
	public static final BorderTagObject CUSTOM_POINT        = new BorderTagObject(BorderTag.CUSTOM);
	
	
	public BorderTagObject(final BorderTag tag){
		this.tag = tag;
		this.name = tag.toString();
	}
	
	public BorderTagObject(final String name, final BorderTag tag){
		this.tag = tag;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public BorderTag getTag() {
		return tag;
	}
	
	public BorderTagType type(){
		return tag.type();
	}
	
	public String toString(){
		return name;
	}
	
	public static BorderTagObject[] values(){
		BorderTagObject[] result = {
				REFERENCE_POINT,
				ORIENTATION_POINT,
				TOP_VERTICAL,
				BOTTOM_VERTICAL,
				INTERSECTION_POINT
		};
		return result;
	}
	
	public static BorderTagObject[] values(BorderTagType type){
		
		List<BorderTagObject> list = new ArrayList<BorderTagObject>();
		for(BorderTagObject o : values()){
			if(o.tag.type().equals(type)){
				list.add(o);
			}
		}
		
		return list.toArray( new BorderTagObject[0]);
		
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((tag == null) ? 0 : tag.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BorderTagObject other = (BorderTagObject) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (tag != other.tag)
			return false;
		return true;
	}


	@Override
	public int compareTo(BorderTagObject arg0) {
		return name.compareTo(arg0.toString());
	}
	
	
	

}
