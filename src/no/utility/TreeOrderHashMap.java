package no.utility;

import ij.IJ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
