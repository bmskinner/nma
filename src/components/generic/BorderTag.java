package components.generic;

import java.util.ArrayList;
import java.util.List;

// use in charting
public enum BorderTag {
	ORIENTATION_POINT ("Orientation point", BorderTagType.CORE ),
	REFERENCE_POINT   ("Reference point",   BorderTagType.CORE ),
	INTERSECTION_POINT ("Intersection point", BorderTagType.EXTENDED);
	
	private final String name;
	private BorderTag.BorderTagType type;
	
	BorderTag(String name, BorderTag.BorderTagType type){
		this.name = name;
		this.type = type;
	}
	
	public String toString(){
		return this.name;
	}
	
	public BorderTag.BorderTagType type(){
		return type;
	}
		
	
	public static BorderTag[] values(BorderTag.BorderTagType type){
		
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