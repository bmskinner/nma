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

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import logging.Loggable;
import components.AbstractCellularComponent;
import components.generic.BorderTagObject;
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
/**
 * @author bms41
 *
 */
/**
 * @author bms41
 *
 */
public class NucleusMesh implements Loggable {
	
	public static final int DEFAULT_VERTEX_SPACING = 10;
	
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
	
	Nucleus nucleus;
		
	/**
	 * Construct a mesh from the given nucleus with default vertex
	 * spacing
	 * @param n
	 */
	public NucleusMesh(Nucleus n){
		this(n, DEFAULT_VERTEX_SPACING);
	}
	
	/**
	 * Construct a mesh from the given nucleus
	 * @param n
	 */
	public NucleusMesh(Nucleus n, int vertexSpacing){
		this.nucleus = n;
		this.vertexSpacing = vertexSpacing;
		
		this.determineVertexProportions();
		
		this.createPeripheralVertices();
		this.createInternalVertices();
		
		this.createEdgesAndFaces();
	}
	
	
	/**
	 * Create a mesh from a nucleus, using another mesh as a template for proportions
	 * @param mesh
	 * @param edges
	 */
	public NucleusMesh(Nucleus n, NucleusMesh template){
		this.nucleus = n;
		this.segmentVertexProportions = template.segmentVertexProportions;
		this.vertexSpacing = template.vertexSpacing;
		this.segmentCount = template.segmentCount;
		
		this.createPeripheralVertices();
		this.createInternalVertices();
		
		this.createEdgesAndFaces();
	}
	
	/**
	 * Duplicate the mesh. Does not yet keep consistency of vertices and edges 
	 * @param template
	 */
	public NucleusMesh(NucleusMesh template){
		this.nucleus                  = template.nucleus;
		this.segmentVertexProportions = template.segmentVertexProportions;
		this.vertexSpacing            = template.vertexSpacing;
		this.segmentCount             = template.segmentCount;
		
		for(NucleusMeshEdge e : template.edges){
			this.edges.add(new NucleusMeshEdge(e));
		}
		
		for(NucleusMeshFace e : template.faces){
			this.faces.add(new NucleusMeshFace(e));
		}
		
		this.peripheralVertices = template.peripheralVertices;
		this.internalVertices   = template.internalVertices;
		
		

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
	private int addVertex(XYPoint p, boolean peripheral){
		
		if(peripheral){
			int newIndex = peripheralVertices.size();
			String name = "P"+newIndex;
			peripheralVertices.add( new NucleusMeshVertex(p, name, peripheral)  );
			return newIndex;
		} else {
			int newIndex = internalVertices.size();
			String name = "I"+newIndex;
			internalVertices.add(   new NucleusMeshVertex(p, name, peripheral)  );
			return newIndex;
		}
	}
	
	/**
	 * Fetch or create the edge between the given vertices
	 * @param v1
	 * @param v2
	 * @return
	 */
	private NucleusMeshEdge getEdge(NucleusMeshVertex v1, NucleusMeshVertex v2){
		
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
	private NucleusMeshFace getFace(NucleusMeshVertex v1, NucleusMeshVertex v2, NucleusMeshVertex v3){
		
		if(this.contains(v1) && this.contains(v2) && this.contains(v3)){
						
			for(NucleusMeshFace f : faces){
				if(f.contains(v1) && f.contains(v2) && f.contains(v3)){
					return f;
				}
			}
			NucleusMeshFace f = new NucleusMeshFace(v1, v2, v3);
			faces.add(f);
			return f;
			
			
		} else {
			return null;
		}
	}
		
	public boolean contains(NucleusMeshVertex v){
		return(peripheralVertices.contains(v) || internalVertices.contains(v));
	}
	
	
	/**
	 * Test if this mesh contains a face with the same vertex positions
	 * @param test
	 * @return
	 */
	public boolean contains(NucleusMeshFace test){
		return faces.contains(test);
	}
	
	public boolean contains(NucleusMeshEdge e){
		return edges.contains(e);
	}
	

	public int getSegmentCount(){
		return segmentCount;
	}
	
	public int getVertexSpacing(){
		return this.vertexSpacing;
	}
	
	/**
	 * The total number of vertices, internal and peripheral
	 * @return
	 */
	public int getVertexCount(){
		return peripheralVertices.size() + internalVertices.size();
	}
	
	public int getInternalVertexCount(){
		return internalVertices.size();
	}
	
	public int getPeripheralVertexCount(){
		return peripheralVertices.size();
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
	
	public boolean isComparableTo(NucleusMesh mesh){
		
		if(this.peripheralVertices.size()!= mesh.peripheralVertices.size()){
			return false;
		}
		
		if(this.internalVertices.size()!= mesh.internalVertices.size()){
			return false;
		}
		
		if(this.edges.size()!= mesh.edges.size()){
			return false;
		}
		
		if(this.faces.size()!= mesh.faces.size()){
			return false;
		}
		return true;
	}
	
	
	/**
	 * Find the edge and face ratios of this mesh versus the given mesh.
	 * Meshes must have the same number of vertices,  edges and faces. 
	 * @param mesh
	 * @return
	 */
	public NucleusMesh compareTo(NucleusMesh mesh){
		
		if( ! this.isComparableTo(mesh) ){
			throw new IllegalArgumentException("Cannot compare meshes");
		}
		
		finer("Comparing this mesh "+this.getNucleusName()+" to "+mesh.getNucleusName());
		finer("Mesh has "+mesh.getFaceCount()+" faces");
		
		NucleusMesh result = new NucleusMesh(this);
		
		List<NucleusMeshEdge> ourEdges   = new ArrayList<NucleusMeshEdge>(edges);
		List<NucleusMeshEdge> theirEdges = new ArrayList<NucleusMeshEdge>(mesh.edges);
		

		for(int i = 0; i<ourEdges.size(); i++){
			NucleusMeshEdge our   = ourEdges.get(i);
			NucleusMeshEdge their = theirEdges.get(i);
			
			double ratio = our.getLength() / their.getLength();
			
			// Store the value
			result.getEdge(our).setValue(ratio);
		}
		
		List<NucleusMeshFace> ourFaces   = new ArrayList<NucleusMeshFace>(faces);
		List<NucleusMeshFace> theirFaces = new ArrayList<NucleusMeshFace>(mesh.faces);
		List<NucleusMeshFace> resultFaces = new ArrayList<NucleusMeshFace>(result.faces);
		
		for(int i = 0; i<ourFaces.size(); i++){
			NucleusMeshFace our   = ourFaces.get(i);
			NucleusMeshFace their = theirFaces.get(i);
			
			double ratio = our.getArea() / their.getArea();
			
			resultFaces.get(i).setValue(ratio);
			
		}
		
		return result;
		
	}
	
	/**
	 * Reposition the vertices such that the internal
	 * skeleton vertices form a vertical line, equally
	 * spaced.
	 * @return
	 */
	public NucleusMesh straighten(){
		fine("Straightening mesh");
		NucleusMesh result = new NucleusMesh(this);
		
		result.clearEdges();
		result.clearFaces();
		
		double nucleusHeight = result.nucleus.getBounds().getHeight();
		double nucleusWidth  = result.nucleus.getBounds().getWidth();
		double vertices      = result.getInternalVertexCount();
		
		double xStep  = nucleusWidth/2;
		double xStart = 0;
		double yStart = 0;
		double yStep  = nucleusHeight / vertices;
		
		XYPoint pos = new XYPoint(xStart, yStart);
		
		
		// Straighten internal skeleton
		for(NucleusMeshVertex v : result.internalVertices){
			v.setPosition(pos);
			pos = new XYPoint( xStart,  pos.getY()+yStep);
		}
		finer("Set skeleton");
		
		// Position peripheral vertices around skeleton
		
		// Positon the first vertex under the skeleton
		NucleusMeshVertex v = result.peripheralVertices.get(0);
		v.setPosition( new XYPoint(xStart, yStart-yStep)  );
		
		int halfArray = (int) Math.floor(( (double) result.peripheralVertices.size() / 2));
		
		for(int i=1, j=result.getPeripheralVertexCount()-1; i<halfArray; i++, j--){
			
			// Get the vertices either side of the skeleton
			NucleusMeshVertex v1 = result.peripheralVertices.get(i);
			NucleusMeshVertex v2 = result.peripheralVertices.get(j);
			
			v1.setPosition( new XYPoint(  xStart-xStep ,   (i*yStep)-(yStep/2) ));
			v2.setPosition( new XYPoint(  xStart+xStep ,   (i*yStep)-(yStep/2) ));
		}
		
		finer("Peripheral vertex count = "+result.getPeripheralVertexCount());
				
		// if needed, adjust the final vertex above the skeleton
		if(result.peripheralVertices.size()%2==0){
			finer("Setting final vertex");
			NucleusMeshVertex p1 = result.peripheralVertices.get(halfArray);
			p1.setPosition(new XYPoint(  xStart ,   (halfArray*yStep)-(yStep/2) ));
		}
		
		finer("Set periphery");
		
		result.createEdgesAndFaces();
		
		finer("Straightened mesh");
		
	
		return result;
	}
	
	
	/**
	 * Get a closed path comprising the peripheral points of the mesh 
	 * @return
	 */
	public Path2D toPath(){
		Path2D path = new Path2D.Double();

		int i=0;
		for(NucleusMeshVertex v : peripheralVertices){
			
			if( i++ == 0){
				path.moveTo(v.getPosition().getX(), v.getPosition().getY());
			} else {
				path.lineTo(v.getPosition().getX(), v.getPosition().getY());
			}
			
			
		}
		path.closePath();
		return path;
	}
	
	/**
	 * Get the face in this mesh with the same vertices as the given face,
	 * or null if not present
	 * @param f
	 * @return
	 */
	protected NucleusMeshFace getFace(NucleusMeshFace test){
		
		for(NucleusMeshFace f : faces){
			if(f.equals(test)){
				return f;
			}
		}
		finer("Cannot find face in mesh: "+test.toString());
		return null;
	}
	
	/**
	 * Get the edge in this mesh with the same vertices as the given edge,
	 * or null if not present
	 * @param f
	 * @return
	 */	
	private NucleusMeshEdge getEdge(NucleusMeshEdge test){
		for(NucleusMeshEdge e : edges){
			if(e.equals(test)){
				return e;
			}
		}
		finer("Cannot find edge in mesh: "+test.toString());
		return null;
	}
	
	private void clearEdges(){
		this.edges.clear();
	}
	
	private void clearFaces(){
		this.faces.clear();
	}

	
	/**
	 * Find the index proportions for each peripheral vertex
	 * 
	 */
	private void determineVertexProportions(){
		
		finer("Determining vertex proportions");
		List<NucleusBorderSegment> list;
		try {
			list = nucleus.getProfile(ProfileType.ANGLE, BorderTagObject.REFERENCE_POINT).getOrderedSegments();
		} catch (Exception e) {
			error("Error getting segments from nucleus", e);
			return;
		}
		
		int segNumber = 0;
		
		for(NucleusBorderSegment seg : list){
			
			List<Double> proportions = new ArrayList<Double>();
			
			
			double div = (double) seg.length() / (double) vertexSpacing;
			
			long divisions = Math.round(div); // the closest number of divisions to a spacing of vertexSpacing
//			int divisions = seg.length() / vertexSpacing; // find the number of divisions to make
			
			finest("Dividing segment into "+divisions+" parts");
			
			for(int i=0; i<divisions;i++){
				
				double proportion = (double) i  / (double) divisions;
				finest("Fetching point at proportion "+proportion);
				proportions.add(proportion);
			}
			
//			
//			finest("Dividing segment into "+divisions+" parts");
//			
//			double proportion = 1d / (double) divisions;
//			
//			for(double d=0; d<1; d+=proportion){
//				finest("Fetching point at proportion "+d);
//				proportions.add(d);
//			}
			segmentVertexProportions.put(segNumber++, proportions); // Store the proportion through the segment of each vertex
		}
	}
	
	/**
	 * Using the vertex spacing as a guide, determing the number and proportion
	 * of vertices to make for each segment. Then select the appropriate border
	 * points and create vertices.
	 */
	private void createPeripheralVertices(){
		finer("Creating peripheral vertices");
		List<NucleusBorderSegment> list;
		try {
			list = nucleus.getProfile(ProfileType.ANGLE, BorderTagObject.REFERENCE_POINT).getOrderedSegments();
		} catch (Exception e) {
			error("Error getting segments from nucleus", e);
			return;
		}
				
		Set<Integer> segs = segmentVertexProportions.keySet();
		for(int segIndex : segs){
			
			NucleusBorderSegment segment = list.get(segIndex);
			finer("Segment "+segIndex+": "+segment.length());
			
			List<Double> proportions = segmentVertexProportions.get(segIndex);
			for(Double d : proportions){
				int index = segment.getProportionalIndex(d);
				
				// Since the segments have been offset to the RP, correct back
				// to the actual nucleus index
				int correctedIndex = AbstractCellularComponent
						.wrapIndex(index+nucleus.getBorderIndex(BorderTagObject.REFERENCE_POINT), segment.getTotalLength());
				
				finest("Fetching point at index "+correctedIndex);
				addVertex(nucleus.getOriginalBorderPoint(correctedIndex), true);
			}
			
		}
	}
	
	/**
	 * Get the face containing the given point within the nucleus
	 * @param p
	 * @return
	 */
	protected NucleusMeshFace getFaceContaining(XYPoint p){
		if(nucleus.containsOriginalPoint(p)){
			for(NucleusMeshFace f : faces){
				if(f.contains(p)){
					return f;
				}
			}
			
		} 
		return null;
	}
	
	protected boolean hasFaceContaining(XYPoint p){
		
		if(nucleus.containsOriginalPoint(p)){


			for(NucleusMeshFace f : faces){
				if(f.contains(p)){
					return true;
				}
			}

		} 
		return false;
	}
	
	/**
	 * Starting at the reference point, create vertices
	 * between the peripheral vertex pairs down to the tail
	 */
	private void createInternalVertices(){
		
		finer("Creating internal vertices");
		/*
		 * The first index in the list is the reference point.
		 * Take the second and last, the third and next-to-last etc
		 */
		int halfArray = peripheralVertices.size() >> 1;
		
		for(int i=1; i<halfArray; i++){
			
			NucleusMeshVertex v1 = peripheralVertices.get(i);
			NucleusMeshVertex v2 = peripheralVertices.get(peripheralVertices.size()-i);
			
			NucleusMeshEdge e = new NucleusMeshEdge(v1, v2, 1);
			
			this.addVertex (e.getMidpoint(), false);
		}
		
	}
	
	private void createEdgesAndFaces(){
		// Build the edges
		
		// Link peripheral vertices
		finer("Creating peripheral edges");
		for(int i=0, j=1; j<peripheralVertices.size(); i++, j++){
			
			NucleusMeshVertex v1 = peripheralVertices.get(i);
			NucleusMeshVertex v2 = peripheralVertices.get(j);
			
			// Getting adds the edge to the internal list
			this.getEdge(v1, v2);
			
			if(j==peripheralVertices.size()-1){
				
				// final link
				v1 = peripheralVertices.get(peripheralVertices.size()-1);
				v2 = peripheralVertices.get(0);
				this.getEdge(v1, v2);
			}
		}
		
		
		finer("Creating internal edges");
		// Link the internal vertices, from peripheral vertex 0
		for(int i=0, j=1; j<internalVertices.size(); i++, j++){
			
			NucleusMeshVertex v1 = internalVertices.get(i);
			NucleusMeshVertex v2 = internalVertices.get(j);
			
			// Getting adds the edge to the internal list
			this.getEdge(v1, v2);
		}
		
		
		// Link between peripheral and internal vertices
		int halfArray = (int) Math.floor(( (double) peripheralVertices.size() / 2));
		
		finer("Linking peripheral edges and internal edges");
		finest("Peripheral vertices: "+peripheralVertices.size());
		finest("Internal vertices: "+internalVertices.size());
		finest("Half array: "+halfArray);
		
		// Link the RP point (peripheral index 0) to the first internal vertex
		
		// Starting at each end of the periperal array, make edges to the internal vertices
		for(int i=1, j=peripheralVertices.size()-1; i<halfArray; i++, j--){
			
			// Points A are ascending from the RP
			// Points X are decending from the RP
			
			NucleusMeshVertex p1_a = peripheralVertices.get(i);
			NucleusMeshVertex p1_x = peripheralVertices.get(j);
			
			NucleusMeshVertex p2_a = peripheralVertices.get(i+1);
			NucleusMeshVertex p2_x = peripheralVertices.get(j-1);
			
			// Each peripheral vertex links to two internal vertices
			NucleusMeshVertex i1 = internalVertices.get(i-1);
			
			// handle the end of the internal skeleton
			NucleusMeshVertex i2;
			if(i==internalVertices.size()){
				i2 = i1; // when there is no point here, use the same vertex as i1
			} else {
				i2 = internalVertices.get(i);
			}
			
			this.getEdge(p1_a, i1);
			this.getEdge(p2_a, i1);
			this.getEdge(p2_a, i2);
//			
			this.getEdge(p1_x, i1);
			this.getEdge(p2_x, i1);
			this.getEdge(p2_x, i2);
//			
//			
			// Make the faces
			this.getFace(p1_a, i1, p2_a);
			this.getFace(p2_a, i1, i2);
			
			this.getFace(p1_x, i1, p2_x);
			this.getFace(p2_x, i1, i2);
		}
		
		// create the top faces - RP to nearest peripheral indexes to I0
		getEdge(peripheralVertices.get(0), internalVertices.get(0));
		getEdge(peripheralVertices.get(1), internalVertices.get(0));
		getEdge(peripheralVertices.get(peripheralVertices.size()-1), internalVertices.get(0));
		
		
		this.getFace(peripheralVertices.get(0), 
				peripheralVertices.get(1), 
				internalVertices.get(0));
		
		this.getFace(peripheralVertices.get(0), 
				peripheralVertices.get(peripheralVertices.size()-1), 
				internalVertices.get(0));
		
		
		
		// if needed, create the bottom face (final intenal vertex to central peripheral vertices)
		if(peripheralVertices.size()%2!=0){
			
			NucleusMeshVertex p1 = peripheralVertices.get(halfArray);
			NucleusMeshVertex p2 = peripheralVertices.get(halfArray+1);
			NucleusMeshVertex i1 = internalVertices.get(internalVertices.size()-1);
			
			// Ensure the edges are created
			getEdge(p1, p2);
			getEdge(p1, i1);
			getEdge(p2, i1);
			
			this.getFace(p1, p2, i1);
		}
		

		
		
	}
	
	public String toString(){
		
		StringBuilder b = new StringBuilder();
		b.append("Nucleus mesh based on "+this.getNucleusName()+"\n");
		b.append("Peripheral vertices:\n");
		
		for(NucleusMeshVertex v : this.peripheralVertices){
			b.append(v.toString()+"\n");
		}
		
		b.append("Internal vertices:\n");
		for(NucleusMeshVertex v : this.internalVertices){
			b.append(v.toString()+"\n");
		}
		
		b.append("Edges:\n");
		for(NucleusMeshEdge e : edges){
			b.append(e.toString()+"\n");
		}
		
		b.append("Faces:\n");
		for(NucleusMeshFace f : faces){
			b.append(f.toString()+"\n");
		}
		return b.toString();
	}
			
}
