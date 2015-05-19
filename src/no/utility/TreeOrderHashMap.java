package no.utility;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

public class TreeOrderHashMap {

	private HashMap<UUID, Integer> forward = new HashMap<UUID, Integer>();;
	private HashMap<Integer, UUID> reverse = new HashMap<Integer, UUID>();;
	
	public TreeOrderHashMap(){
	}
	
	public void put(UUID id, Integer i){
		
		if(!forward.containsKey(id) && !forward.containsValue(i)){
			forward.put(id, i);
			reverse.put(i, id);
		}
	}
	
	public UUID get(Integer i){
		return reverse.get(i);
	}
	
	public Integer get(UUID id){
		return forward.get(id);
	}
	
	public void remove(UUID id){
		Integer i = get(id);
		forward.remove(id);
		reverse.remove(i);
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
	
	public Set<Integer> getPositions(){
		return reverse.keySet();
	}
	
	public Set<UUID> getIDs(){
		return forward.keySet();
	}
}
