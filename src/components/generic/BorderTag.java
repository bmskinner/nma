/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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

import java.util.ArrayList;
import java.util.List;

// use in charting
public enum BorderTag {
	
	REFERENCE_POINT   ("Reference point",   BorderTagType.CORE ),
	ORIENTATION_POINT ("Orientation point", BorderTagType.CORE ),
	INTERSECTION_POINT ("Intersection point", BorderTagType.EXTENDED),
	TOP_VERTICAL		("Top vertical point", BorderTagType.EXTENDED),
	BOTTOM_VERTICAL		("Bottom vertical point", BorderTagType.EXTENDED);
	
	private final String name;
	private final BorderTag.BorderTagType type;
	
	BorderTag(final String name, final BorderTag.BorderTagType type){
		this.name = name;
		this.type = type;
	}
	
	public String toString(){
		return this.name;
	}
	
	public BorderTag.BorderTagType type(){
		return type;
	}
		
	
	public static BorderTag[] values(final BorderTag.BorderTagType type){
		
		List<BorderTag> list = new ArrayList<BorderTag>();
		for(BorderTag tag : BorderTag.values()){
			if(tag.type.equals(type)){
				list.add(tag);
			}
		}
		return list.toArray( new BorderTag[0]);
	}
	
	// core tags are used in gui; extended are for internal mappings
	public enum BorderTagType { CORE, EXTENDED};
}