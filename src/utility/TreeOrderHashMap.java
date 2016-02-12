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
package utility;

import ij.IJ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


/**
 * This stores a bi-directional hash, for tracking the position
 * of datasets within the populations panel
 * @author bms41
 *
 */
public class TreeOrderHashMap {

	private HashMap<UUID, Integer> forward = new HashMap<UUID, Integer>();;
	private HashMap<Integer, UUID> reverse = new HashMap<Integer, UUID>();;
	
	public TreeOrderHashMap(){
	}
	
	public void put(UUID id, Integer i){
		forward.put(id, i);
		reverse.put(i, id);
	}
	
	public UUID get(Integer i){
		return reverse.get(i);
	}
	
	public Integer get(UUID id){
		return forward.get(id);
	}
	
	public void remove(UUID id){
		Integer i = get(id);
		this.remove(i);
	}
	
	public void remove(Integer i){
		UUID id = get(i);
		forward.remove(id);
		reverse.remove(i);
		
		// move the remainder down one
		for(int j = i; j<forward.size();j++){
			this.put(get(j+1), j);
		}
	}
	
	public boolean contains(UUID id){
		return forward.containsKey(id);
	}
	
	public boolean contains(Integer i){
		return reverse.containsKey(i);
	}
	
	public int size(){
		return forward.size();
	}
	
	public List<Integer> getPositions(){
		List<Integer> result = new ArrayList<Integer>(0);
		for(int i = 0; i<this.size();i++){
			result.add(i);
		}
		return result;
//		return reverse.keySet();
	}
	
	public List<UUID> getIDs(){
		List<UUID> result = new ArrayList<UUID>(0);
		for(int i = 0; i<this.size();i++){
			result.add(this.get(i));
		}
		return result;
	}
	
	public void print(){
		for(Integer i : reverse.keySet()){
			IJ.log(i+" : "+reverse.get(i).toString());
		}
	}
}
