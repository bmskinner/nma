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
package components.nuclear;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import components.generic.BorderTag;
import components.nuclei.AsymmetricNucleus;
import components.nuclei.Nucleus;
import components.nuclei.RoundNucleus;
import components.nuclei.sperm.PigSpermNucleus;
import components.nuclei.sperm.RodentSpermNucleus;

/**
 * The types of nuclei we are able to analyse,
 * with the reference and orientation points to be used.
 * The reference point is the best identifiable point on the
 * nucleus when aligning profiles. The orientation point is the point
 * placed at the bottom when rotating a consensus nucleus.
 *
 */
public enum NucleusType {
	ROUND 		 ("Round nucleus"		 , "Head", "Tail", RoundNucleus.class), 
	ASYMMETRIC 	 ("Asymmetric nucleus"	 , "Head", "Tail", AsymmetricNucleus.class),
	RODENT_SPERM ("Rodent sperm nucleus" , "Tip" , "Tail", RodentSpermNucleus.class), 
	PIG_SPERM 	 ("Pig sperm nucleus"	 , "Tail", "Tail", PigSpermNucleus.class);
	
    private final String name;   
    private final Class<?> nucleusClass;
    
    private final Map<BorderTag, String> map = new HashMap<BorderTag, String>();
    
    NucleusType(String name, String referencePoint, String orientationPoint, Class<?> nucleusClass) {
        this.name = name;
        this.nucleusClass = nucleusClass;
        this.map.put(BorderTag.REFERENCE_POINT, referencePoint);
        this.map.put(BorderTag.ORIENTATION_POINT, orientationPoint);
	}
    
    public String toString(){
    	return this.name;
    }
        
    
    /**
     * Get the name of the given border tag, if present.
     * For example, the name of the RP in mouse sperm is
     * the tip. The name of the RP in pig sperm is the head.
     * @param point
     * @return
     */
    public String getPoint(BorderTag point){
    	return this.map.get(point);
    }
    
    public Class<?> getNucleusClass(){
    	return this.nucleusClass;
    }
    
    /**
     * Get the simple names of the border tags in the nucleus
     * @return
     */
    public String[] pointNames(){
    	List<String> list = new ArrayList<String>();
    	for(BorderTag tag : map.keySet()){
    		list.add(map.get(tag));
    	}
    	return list.toArray(new String[0]);
    }
    
    /**
     * Get the border tag with the given name, 
     * or null if the name is not found
     * @param name
     * @return
     */
    public BorderTag getTagFromName(String name){
    	for(BorderTag tag : map.keySet()){
    		if(map.get(tag).equals(name)){
    			return tag;
    		}
    	}
    	return null;
    }
    
    /**
     * Given a nucleus, find the appropriate NucleusType
     * @param n
     * @return
     */
    public static NucleusType getNucleusType(Nucleus n){
    	Class<?> nucleusClass = n.getClass();
    	for(NucleusType type : NucleusType.values()){
    		if(type.getNucleusClass().equals(nucleusClass)){
    			return type;
    		}
    	}
    	return null;
    }
    
    /**
	 * Test if a given name is a tag name
	 * @param s
	 * @return
	 */
	public static boolean isBorderTag(String s){
		for(BorderTag tag : BorderTag.values()){
			if(tag.toString().equals(s)){
				return true;
			}
		}
		return false;
	}
}