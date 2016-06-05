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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import logging.Loggable;
import components.generic.ProfileType;
import components.generic.XYPoint;
import components.nuclear.NucleusBorderSegment;
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
	
	private int segmentCount = 0; // the number of segments to divide on
	
	private int vertexSpacing = 10; // the default average number of border points between vertices
	
	// For each segment, list the proportions through the segment at which a vertex is found
	private Map<Integer, List<Double>> segmentVertexProportions = new HashMap<Integer, List<Double>>();
	
//	// Track the number of divisions for each segment to allow mapping between meshes
//	private Map<Integer, Integer> segmentDivisions = new HashMap<Integer, Integer>();
	
	// Store the vertices in the mesh. List, so we can index it.
	private List<NucleusMeshVertex> peripheralVertices = new ArrayList<NucleusMeshVertex>();
	
	// Store the skeleton vertices in the mesh
	private List<NucleusMeshVertex> internalVertices = new ArrayList<NucleusMeshVertex>();
	
	// Not all vertices need to be linked - store edges for comparisons
//	private List<NucleusMeshEdge> internalEdges     = new ArrayList<NucleusMeshEdge>();
	
	private Set<NucleusMeshEdge> edges     = new LinkedHashSet<NucleusMeshEdge>();
//	
//	// Track the edges linking border vertices separately from internal edges
//	private List<NucleusMeshEdge> peripheralEdges  = new ArrayList<NucleusMeshEdge>();
	
	// Track the faces of interest in the mesh
	private Set<NucleusMeshFace> faces  = new LinkedHashSet<NucleusMeshFace>();
	
	private Nucleus nucleus;
		
	/**
	 * Construct a mesh from the given nucleus
	 * @param n
	 */
	public NucleusMesh(Nucleus n){
		this.nucleus = n;
		
		this.createPeripheralVertices();
		this.createInternalVertices();
		
		this.createEdges();
	}
	
	
	/**
	 * Private constructor, to return a compared mesh
	 * @param mesh
	 * @param edges
	 */
//	private NucleusMesh(NucleusMesh mesh, List<NucleusMeshEdge> internal, List<NucleusMeshEdge> peripheral){
//		this.segmentDivisions = mesh.segmentDivisions;
//		this.peripheralVertices         = mesh.peripheralVertices;
//		this.internalEdges    = internal;
//		this.peripheralEdges  = peripheral;
//
//		this.nucleus          = mesh.nucleus;
//	}
	
	
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
	private int addVertex(XYPoint p, boolean peripheral){
		
		if(peripheral){
			int newIndex = peripheralVertices.size();
			peripheralVertices.add( new NucleusMeshVertex(newIndex, p, peripheral)  );
			return newIndex;
		} else {
			int newIndex = peripheralVertices.size();
			internalVertices.add( new NucleusMeshVertex(newIndex, p, peripheral)  );
			return newIndex;
		}
	}
	
	/**
	 * Fetch or create the edge between the given vertices
	 * @param v1
	 * @param v2
	 * @return
	 */
	public NucleusMeshEdge getEdge(NucleusMeshVertex v1, NucleusMeshVertex v2){
		
		if(this.contains(v1) && this.contains(v2)){
			for(NucleusMeshEdge e : edges){
				if(e.containsVertex(v1) && e.containsVertex(v2)){
					return e;
				}
			}
			NucleusMeshEdge e = new NucleusMeshEdge(v1, v2, 1);
			edges.add(e);
			return e;
		} else {
			throw new IllegalArgumentException("Mesh does not contain vertices");
		}
	}
	
	/**
	 * Fetch or create the face bounded by the given vertices
	 * @param v1
	 * @param v2
	 * @param v3
	 * @return
	 */
	public NucleusMeshFace getFace(NucleusMeshVertex v1, NucleusMeshVertex v2, NucleusMeshVertex v3){
		if(this.contains(v1) && this.contains(v2) && this.contains(v3)){
			
			NucleusMeshEdge e1 = this.getEdge(v1, v2);
			NucleusMeshEdge e2 = this.getEdge(v1, v3);
			NucleusMeshEdge e3 = this.getEdge(v2, v3);
			
			for(NucleusMeshFace f : faces){
				if(f.hasEdge(e1) && f.hasEdge(e2) && f.hasEdge(e3)){
					return f;
				}
			}
			NucleusMeshFace f = new NucleusMeshFace(e1, e2, e3);
			faces.add(f);
			return f;
			
			
		} else {
			return null;
		}
	}
	
	public boolean contains(NucleusMeshVertex v){
		return(peripheralVertices.contains(v) || internalVertices.contains(v));
	}
	

	public int getSegmentCount(){
		return segmentCount;
	}
	
	public int getVertexSpacing(){
		return this.vertexSpacing;
	}
	
	public int getVertexCount(){
		return peripheralVertices.size() + internalVertices.size();
	}
	
	public int getEdgeCount(){
		return edges.size();
	}
	
	public int getFaceCount(){
		return faces.size();
	}
		
	public List<NucleusMeshVertex> getPeripheralVertices(){
		return peripheralVertices;
	}
	
	public List<NucleusMeshVertex> getInternalVertices(){
		return internalVertices;
	}
		
	public Set<NucleusMeshEdge> getEdges(){
		return edges;
	}
	
	public Set<NucleusMeshFace> getFaces(){
		return this.faces;
	}
	
	/**
	 * Using the vertex spacing as a guide, determing the number and proportion
	 * of vertices to make for each segment. Then select the appropriate border
	 * points and create vertices.
	 */
	private void createPeripheralVertices(){
		List<NucleusBorderSegment> list;
		try {
			list = nucleus.getProfile(ProfileType.REGULAR).getOrderedSegments();
		} catch (Exception e) {
			error("Error getting segments from nucleus", e);
			return;
		}
		
		int segNumber = 0;
		
		
		for(NucleusBorderSegment seg : list){
			
			List<Double> proportions = new ArrayList<Double>();
			int divisions = seg.length() / vertexSpacing; // find the number of divisions to make
			
			
			finest("Dividing segment into "+divisions+" parts");
			
			double proportion = 1d / (double) divisions;
			
			for(double d=0; d<1; d+=proportion){
				int index = seg.getProportionalIndex(d);
				finest("Fetching point at index "+index);
				proportions.add(d);
				addVertex(nucleus.getBorderPoint(index), true);
			}
			segmentVertexProportions.put(segNumber++, proportions); // Store the proportion through the segment of each vertex
		}
	}

	
	/**
	 * Starting at the reference point, create vertices
	 * between the peripheral vertex pairs down to the tail
	 */
	private void createInternalVertices(){
		
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
	
	private void createEdges(){
		// Build the edges
		
		//TODO Add the algorithm from sketches
	}
			
}
