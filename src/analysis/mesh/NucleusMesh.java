/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package analysis.mesh;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import logging.Loggable;
import components.generic.XYPoint;
import components.nuclei.Nucleus;

/**
 * 
 * The mesh should allow comparisons of equivalent points between different nuclei.
 * 
 * The requirement is to:
 * 1) consistently identify points around the periphery of the nucleus
 * 2) Translate those points to another nucleus.
 * 
 * The points are identified based on proportion through segments. We can
 * be reasonably confident that segment boundaries are at equivalent biological
 * features. Each segment is divided into points, separated by about 10 pixels.
 * 
 * These points around the periphery of the nucleus are used to build a skeleton for
 * the object. The skeleton travels from the reference point through the centre of the 
 * nucleus.
 * 
 * Edges are constructed between the peripheral vertices and their corresponding
 * skeleton vertices, making a triangular mesh.
 * 
 * All vertices can be located in another nucleus using segment proportions.
 * @author bms41
 *
 */
public class NucleusMesh implements Loggable{
	
	// Track the number of divisions for each segment to allow mapping between meshes
	private Map<Integer, Integer> segmentDivisions = new HashMap<Integer, Integer>();
	
	// Store the vertices in the mesh
	private List<NucleusMeshVertex> peripheralVertices = new ArrayList<NucleusMeshVertex>();
	
	// Store the skeleton vertices in the mesh
	private List<NucleusMeshVertex> internalVertices = new ArrayList<NucleusMeshVertex>();
	
	// Not all vertices need to be linked - store edges for comparisons
	private List<NucleusMeshEdge> internalEdges     = new ArrayList<NucleusMeshEdge>();
	
	// Track the edges linking border vertices separately from internal edges
	private List<NucleusMeshEdge> peripheralEdges  = new ArrayList<NucleusMeshEdge>();
	
	private Nucleus nucleus;
		
	/**
	 * Construct a mesh from the given nucleus
	 * @param n
	 */
	public NucleusMesh(Nucleus n){
		this.nucleus = n;
	}
	
	
	/**
	 * Private constructor, to return a compared mesh
	 * @param mesh
	 * @param edges
	 */
	private NucleusMesh(NucleusMesh mesh, List<NucleusMeshEdge> internal, List<NucleusMeshEdge> peripheral){
		this.segmentDivisions = mesh.segmentDivisions;
		this.peripheralVertices         = mesh.peripheralVertices;
		this.internalEdges    = internal;
		this.peripheralEdges  = peripheral;

		this.nucleus          = mesh.nucleus;
	}
	
	
	public String getNucleusName() {
		return  nucleus.getNameAndNumber();
	}

	
	/**
	 * Add the given point as a vertex to the mesh. Returns the number
	 * of the added vertex
	 * @param p
	 * @param peripheral
	 * @return
	 */
	public int addVertex(XYPoint p, boolean peripheral){
		int newIndex = peripheralVertices.size();
		peripheralVertices.add( new NucleusMeshVertex(newIndex, p, peripheral)  );
		return newIndex;
	}
	
	public void addInternalEdge(NucleusMeshEdge e){
		internalEdges.add(e);
	}
	
	public void addPeripheralEdge(NucleusMeshEdge e){
		peripheralEdges.add(e);
	}
	
	public int getSegmentCount(){
		return segmentDivisions.keySet().size();
	}
	
	public int getVertexCount(){
		return peripheralVertices.size();
	}
	
	public int getEdgeCount(){
		return internalEdges.size() + peripheralEdges.size();
	}
	
	public int getInternalEdgeCount(){
		return internalEdges.size();
	}
	
	public int getPeripheralEdgeCount(){
		return peripheralEdges.size();
	}
	
	public NucleusMeshVertex getVertex(int i){
		return  peripheralVertices.get(i);
	}
	
	public NucleusMeshEdge getInternalEdge(int i){
		return internalEdges.get(i);
	}
	
	public NucleusMeshEdge getPeripheralEdge(int i){
		return peripheralEdges.get(i);
	}
	
	public List<NucleusMeshEdge> getEdges(){
		List<NucleusMeshEdge> result = new ArrayList<NucleusMeshEdge>();
		result.addAll(internalEdges);
		result.addAll(peripheralEdges);
		return result;
	}

	
	public List<NucleusMeshEdge> getInternalEdges(){
		return internalEdges;
	}
	
	public List<NucleusMeshEdge> getPeripheralEdges(){
		return peripheralEdges;
	}	
	
	
	public void setDivision(int segment, int divisions){
		segmentDivisions.put(segment, divisions);
	}
	
	public int getDivision(int segment){
		return segmentDivisions.get(segment);
	}
			
	public NucleusMesh compare(NucleusMesh mesh){
		
		if(mesh.getEdgeCount() != this.getEdgeCount()){
			throw new IllegalArgumentException("Meshes are not comparable: "+mesh.getEdgeCount()+" versus "+getEdgeCount());
		}
		
		log(Level.FINEST, "Comparing meshes");
		
		List<NucleusMeshEdge> internalEdges   = new ArrayList<NucleusMeshEdge>();
		List<NucleusMeshEdge> peripheralEdges = new ArrayList<NucleusMeshEdge>();
		
		for(int i=0; i<getInternalEdgeCount(); i++){
			internalEdges.add( getInternalEdge(i).compare(mesh.getInternalEdge(i)) );
		}
		
		for(int i=0; i<getPeripheralEdgeCount(); i++){
			peripheralEdges.add( getPeripheralEdge(i).compare(mesh.getPeripheralEdge(i)) );
		}
		
		return new NucleusMesh(this, internalEdges, peripheralEdges);
	}
	
	
	/**
	 * Starting at the reference point, create vertices
	 * between the peripheral vertex pairs down to the tail
	 */
	public void makeCentreVertices(){
		
		List<NucleusMeshVertex> list = getPeripheralVertices();
		/*
		 * The first index in the list is the reference point.
		 * Take the second and last, the third and next-to-last etc
		 */
		for(int i=1; i<list.size()/2; i++){
			
			NucleusMeshVertex v1 = list.get(i);
			NucleusMeshVertex v2 = list.get(list.size()-i);
			
			NucleusMeshEdge test = new NucleusMeshEdge(v1, v2, 1);
			this.addVertex (test.getMidpoint(), false);
		}
		
	}
	
	/**
	 * Create new vertices at the midpoints of all edges that have
	 * one or fewer peripheral vertices. Add new edges between them.
	 */
	public void subdivide(){
		List<NucleusMeshEdge> toAdd = new ArrayList<NucleusMeshEdge>();
		
		for(final Iterator<NucleusMeshEdge> it = internalEdges.iterator(); it.hasNext(); ){

			NucleusMeshEdge e = it.next();
			if( ! e.isPeripheral()){
				int index = addVertex (e.getMidpoint(), false);
				
				NucleusMeshEdge e1 = new NucleusMeshEdge(e.getV1(), peripheralVertices.get(index), 1);
				NucleusMeshEdge e2 = new NucleusMeshEdge(e.getV2(), peripheralVertices.get(index), 1);
				
				getVertex(index).addEdge(e1);
				e.getV1().addEdge(e1);
				e.getV2().addEdge(e2);
				getVertex(index).addEdge(e2);
				toAdd.add( e1 );
				toAdd.add( e2 );
				e.getV1().removeEdge(e);
				e.getV2().removeEdge(e);
				it.remove();

			}
		}
		
		for(NucleusMeshEdge e : toAdd){
			addInternalEdge(e); 
		}
	}
	
	public void makePairwiseEdges(){
		List<NucleusMeshEdge> toAdd = new ArrayList<NucleusMeshEdge>();
		
		
		
		for(NucleusMeshVertex v1 : peripheralVertices){
			
			for(NucleusMeshVertex v2 : peripheralVertices){
				if(v1.overlaps(v2)){
					continue;
				}
				NucleusMeshEdge e = new NucleusMeshEdge(v1, v2, 1);
				toAdd.add( e );
				
			}
			
		}
		
		for(NucleusMeshEdge e : toAdd){
			addInternalEdge(e); 
		}
		
		
		
	}
	
	/**
	 * Remove edges that are direct duplicates (completely overlapping endpoints)
	 */
	private void mergeDuplicateEdges(){
		Set<NucleusMeshEdge> toRemove = new HashSet<NucleusMeshEdge>();
		log(Level.FINEST, "Starting edges: "+internalEdges.size());
		
		for(NucleusMeshEdge e1 : internalEdges){
			
			if(toRemove.contains(e1)){
				continue; 
			}
			
			for(NucleusMeshEdge e2 : internalEdges){
				
				if(e1==e2){  // ignore self self
					continue;
				}
				
				if(toRemove.contains(e2)){
					continue; // ignore existing matches
				}
				
				if(e1.overlaps(e2)){
					// not the same object, but equivalent
					// ensure vertices track the edge being kept
											
					
					NucleusMeshVertex v1 = e1.getV1();
					NucleusMeshVertex v2 = e1.getV2();
					v1.addEdge(e2);
					v2.addEdge(e2);
					toRemove.add(e1);
					
				}

				
			}
			
		}
		log(Level.FINEST, "Merging edges: "+toRemove.size());
		removeEdges(toRemove);
		log(Level.FINEST, "Remaining edges: "+internalEdges.size());
		
		// Check that vertices don't have an edge twice following assignment
		mergeDuplicateEdgesByVertex();
	}
	
	
	/**
	 * Check if any vertices have a duplicated edge in inverted format.
	 * If so, remove the duplicates. 
	 */
	private void mergeDuplicateEdgesByVertex(){
		
		Set<NucleusMeshEdge> toRemove = new HashSet<NucleusMeshEdge>();
		
		for(NucleusMeshVertex v : peripheralVertices){
			
			Set<NucleusMeshEdge> list = v.getEdges();
			
			for(NucleusMeshEdge e1 : list){
				
				if(toRemove.contains(e1)){
					continue; 
				}
				
				for(NucleusMeshEdge e2 : list){
					
					if(e1==e2){
						continue;
					}
					
					if(toRemove.contains(e2)){
						continue; 
					}
					
					if(e1.overlaps(e2)){
						toRemove.add(e2);
					}
					
				}
				
			}
			
		}
		log(Level.FINEST, "Merging edges at vertices: "+toRemove.size());
		removeEdges(toRemove);
		log(Level.FINEST, "Remaining edges: "+internalEdges.size());
	}
	
			
	/**
	 * Remove edges from the list that span more than one third the bounding height of
	 * the mesh
	 * @return
	 */
	private List<NucleusMeshEdge> getLongestEdges(List<NucleusMeshEdge> input, double factor){
		
		double maxLength = input.parallelStream()
			.max( (e1, e2) -> Double.compare(e1.getLength(), e2.getLength()))
			.get()
			.getLength();
		
		
		final double max = maxLength / factor;
		
		List<NucleusMeshEdge> result = input.parallelStream()
			.filter(e -> e.getLength() > max)
			.collect(Collectors.toList());
		
		
		return result;
		
	}
	
	private NucleusMeshEdge getLongestEdge(List<NucleusMeshEdge> input){
		return input.parallelStream()
		.max( (e1, e2) -> Double.compare(e1.getLength(), e2.getLength()))
		.get();
	}
			
	private void removeEdges(Collection<NucleusMeshEdge> toRemove){
		for(NucleusMeshEdge e : toRemove){
			internalEdges.remove(e);
			peripheralEdges.remove(e);
			e.getV1().removeEdge(e);
			e.getV2().removeEdge(e);
		}
	}
	
	/**
	 * Remove internal edges whose midpoint lies outside the nucleus
	 */
	public void removeExternalEdges(){
		List<NucleusMeshEdge> toRemove = internalEdges.parallelStream()
				.filter( e -> ! nucleus.containsPoint(e.getMidpoint()))
				.collect(Collectors.toList());
		
		log(Level.FINEST, "Removing "+toRemove.size()+" external edges");
		removeEdges(toRemove);
	}
			
	
	private List<NucleusMeshVertex> getPeripheralVertices(){
		List<NucleusMeshVertex> result = peripheralVertices.parallelStream()
				.filter( e -> e.isPeripheral())
				.collect(Collectors.toList());
		return result;
	}
	
	private List<NucleusMeshVertex> getInternalVertices(){
		List<NucleusMeshVertex> result = peripheralVertices.parallelStream()
				.filter( e -> ! e.isPeripheral())
				.collect(Collectors.toList());
		return result;
	}
			
	private boolean testEdgeCrossesPeriphery(NucleusMeshEdge e){
		
//		List<NucleusMeshEdge> preipheralEdges = getPeripheralEdges();
		
		return getPeripheralEdges().parallelStream()
			.anyMatch( p -> e.crosses(p));
		
//		for(NucleusMeshEdge p : preipheralEdges){
//			if(e.crosses(p)){
//				return true;
//			}
//		}
//		return  false;
	}
	
	
	/**
	 * Get any edges that link central non-adjacent vertices
	 * @return
	 */
	private Set<NucleusMeshEdge> getNonAdjacentCentralVertexEdges(){
		Set<NucleusMeshEdge> result = new HashSet<NucleusMeshEdge>();
		
		List<NucleusMeshVertex> list = getInternalVertices();
		
		for(int i=0; i<list.size(); i++){
			
			int prev = i==0 ? list.size()-1 : i-1;
			int next = i==list.size()-1 ? 0 : i+1;
			
			NucleusMeshVertex p1 = list.get(prev);
			NucleusMeshVertex v1 = list.get(i);
			NucleusMeshVertex p2 = list.get(next);
			
			for(NucleusMeshEdge e : v1.getEdges()){
				
				// The edge is entirely within the central vertices
				if( list.contains(e.getV1()) && list.contains(e.getV2())){

					// the edge does not list to an adjacent vertex 
					if( !e.containsVertex(p1) && !e.containsVertex(p2) ){
						result.add(e);
					}
				}
			}
			
		}
		return result;
	}
	
	private List<NucleusMeshEdge> getOverlappingInternalEdges(NucleusMeshEdge e1){
			
		List<NucleusMeshEdge> result = internalEdges.parallelStream()
			.filter( e -> e1.crosses(e))
			.collect(Collectors.toList());
		
		return result;
	}
	/**
	 * Remove all edges that intersect another edge in the mesh.
	 * Discards the longer edge of the two
	 */
	public void pruneOverlaps(){

		this.removeExternalEdges();
		this.mergeDuplicateEdges();
		
		Set<NucleusMeshEdge> toRemove = new HashSet<NucleusMeshEdge>();

		
		log(Level.FINEST, "Pruning overlaps");
		log(Level.FINEST, "Starting with "+peripheralVertices.size()+" vertices");
		log(Level.FINEST, "Starting with "+internalEdges.size()+" edges");
		
		List<NucleusMeshEdge> longestEdges  = getLongestEdges(internalEdges, 3);
		log(Level.FINEST, "Found "+longestEdges.size()+" long edges using factor "+3);

		
		/*
		 * Add the longest edges
		 */
		toRemove.addAll(  
				
				internalEdges.parallelStream()
				.filter( e -> longestEdges.contains(e))
				.collect(Collectors.toList())
				
			);
		
		
		log(Level.FINEST, "Removing "+toRemove.size()+" edges combining length");
		
		/*
		 * Add the edges that cross the peripheral edges
		 */
		toRemove.addAll(  
				
				internalEdges.parallelStream()
					.filter( e -> testEdgeCrossesPeriphery(e))
					.collect(Collectors.toList())
				
			);
		log(Level.FINEST, "Removing "+toRemove.size()+" edges combining periphery crossing");
		
		
		/*
		 * Add connections between peripheral vertices that are not peripheral edges
		 */
		
		toRemove.addAll(  
				
				internalEdges.parallelStream()
					.filter( e -> e.getV1().isPeripheral() && e.getV2().isPeripheral())
					.collect(Collectors.toList())
				
			);
		log(Level.FINEST, "Removing "+toRemove.size()+" edges combining peripheral vertices");
		
		/*
		 * Add connections between internal vertices (the centre line) that are not adjacent
		 */
		
		log(Level.FINEST, "Removing "+toRemove.size()+" edges combining peripheral vertices");
		
		toRemove.addAll( getNonAdjacentCentralVertexEdges());
		
	
		/*
		 * Clear out the first set of removable edges
		 */
		removeEdges(toRemove);
		log(Level.FINEST, "Remaining internal edges: "+internalEdges.size());
		/*
		 * Remove the longer of internal edges that cross another internal edge
		 */
		
		log(Level.FINEST, "Removing crossing internal edges");
		boolean result = true;
		int totalRemoved = 0;
		int counter = 0;
		while(result && counter<50){
			
			/*
			 * Go over each vertex, trying to remove an edge
			 */
//			logIJ(" ");
//			logIJ("Loop iteration over "+vertices.size()+" vertices");
			int removed = removeLoop();
//			logIJ("Removed "+removed+" edges");
			if(removed==0){
				result=false;
			}
			totalRemoved+=removed;
			counter++;
		}
		log(Level.FINEST, "Removed "+totalRemoved+" internal edges");
		

		log(Level.FINEST, "Remaining internal edges: "+internalEdges.size());
	}
	
	private int removeLoop(){
		
		int result = 0;
		
		for(NucleusMeshVertex v : peripheralVertices){
//			logIJ(" ");
//			logIJ("Testing vertex "+v.toString()+" with "+v.getEdges().size()+" connected edges");
			if(removeEdgeLoop(v)){
				result++;
			} 
		}
		return result;
	}
	
	private boolean removeEdgeLoop(NucleusMeshVertex v){
		Set<NucleusMeshEdge> edges = v.getEdges();

		for(NucleusMeshEdge e1 : edges){
//			logIJ("   Edge "+e1.toString());
			if(removeLongestOverlappingEdge(e1)){
//				logIJ("Able to remove edge overlapping "+e1.toString());
				return true;
			}
		}
		return false;
	}
	
	private boolean removeLongestOverlappingEdge(NucleusMeshEdge e1){
		Set<NucleusMeshEdge> toRemove = getRemovableEdges(e1);
		if(toRemove.isEmpty()){
			return false;
		} else {
			removeEdges(toRemove);
			return true;
		}
	}
	
	private Set<NucleusMeshEdge> getRemovableEdges(NucleusMeshEdge e1){

			
//		logIJ("Testing edge "+e1.toString()+": "+e1.getLength());
			Set<NucleusMeshEdge> toRemove = new HashSet<NucleusMeshEdge>();
			
			List<NucleusMeshEdge> overlaps = getOverlappingInternalEdges(e1);
			if( ! overlaps.isEmpty()){
				overlaps.add(e1); // include self for possible deletion
				
//				for(NucleusMeshEdge e : overlaps){
//					logIJ(e.toString()+": "+e.getLength());
//				}

				NucleusMeshEdge longest = getLongestEdge(overlaps);
				toRemove.add(longest);
//				logIJ("Removing edge "+longest.toString()+": "+longest.getLength());
			}
			return toRemove;
		
	}
			
}
