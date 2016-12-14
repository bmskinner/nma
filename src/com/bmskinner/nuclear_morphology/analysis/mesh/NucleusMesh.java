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
package com.bmskinner.nuclear_morphology.analysis.mesh;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.DefaultCellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.FloatPoint;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderSegment;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * An implementation of the Mesh for Nuclei
 * @author bms41
 *
 */
public class NucleusMesh implements Loggable, Mesh<Nucleus> {
	
	private int segmentCount = 0; // the number of segments to divide on
	
	private int vertexSpacing = 10; // the default average number of border points between vertices
	
	// For each segment, list the proportions through the segment at which a vertex is found
	private Map<Integer, List<Double>> segmentVertexProportions = new HashMap<Integer, List<Double>>();
		
	// Store the vertices in the mesh. List, so we can index it.
	private List<MeshVertex> peripheralVertices = new ArrayList<MeshVertex>();
	
	// Store the skeleton vertices in the mesh
	private List<MeshVertex> internalVertices = new ArrayList<MeshVertex>();
	
	
	private Set<MeshEdge> edges     = new LinkedHashSet<MeshEdge>();
	
	// Track the faces of interest in the mesh
	private Set<MeshFace> faces  = new LinkedHashSet<MeshFace>();
	
	Nucleus nucleus;
		
	/**
	 * Construct a mesh from the given nucleus with default vertex
	 * spacing
	 * @param n
	 * @throws MeshCreationException 
	 */
	public NucleusMesh(Nucleus n) throws MeshCreationException{
		this(n, DEFAULT_VERTEX_SPACING);
	}
	
	/**
	 * Construct a mesh from the given nucleus
	 * @param n
	 * @throws MeshCreationException 
	 */
	public NucleusMesh(Nucleus n, int vertexSpacing) throws MeshCreationException{
		this.nucleus = n;
		this.vertexSpacing = vertexSpacing;

		try {
			this.determineVertexProportions();

			this.createPeripheralVertices();

			this.createInternalVertices();

			this.createEdgesAndFaces();
		} catch(IllegalArgumentException e){
			throw new MeshCreationException("Unable to create mesh for nucleus "+n.getNameAndNumber(), e);
		}
	}
	
	
	/**
	 * Create a mesh from a nucleus, using another mesh as a template for proportions
	 * @param mesh
	 * @param edges
	 * @throws MeshCreationException 
	 */
	public NucleusMesh(Nucleus n, Mesh<Nucleus> template) throws MeshCreationException {
		this.nucleus                  = n;
		this.segmentVertexProportions = template.getVertexProportions();
		this.vertexSpacing            = template.getVertexSpacing();
		this.segmentCount             = template.getSegmentCount();



		try {
			this.createPeripheralVertices();
			this.createInternalVertices();
			this.createEdgesAndFaces();

		} catch(IllegalArgumentException e){
			throw new MeshCreationException("Unable to create mesh for nucleus "+n.getNameAndNumber(), e);
		}
	}
	
	/**
	 * Duplicate the mesh. Does not yet keep consistency of vertices and edges 
	 * @param template
	 */
	public NucleusMesh(Mesh<Nucleus> template){
		this.nucleus                  = template.getComponent();
		this.segmentVertexProportions = template.getVertexProportions();
		this.vertexSpacing            = template.getVertexSpacing();
		this.segmentCount             = template.getSegmentCount();
		
		for(MeshEdge e : template.getEdges()){
			this.edges.add(new NucleusMeshEdge(e));
		}
		
		for(MeshFace e : template.getFaces()){
			this.faces.add(new NucleusMeshFace(e));
		}
		
		this.peripheralVertices = template.getPeripheralVertices();
		this.internalVertices   = template.getInternalVertices();
		
		

	}
	
	@Override
	public Nucleus getComponent(){
		return nucleus;
	}
	
	@Override
	public Map<Integer, List<Double>> getVertexProportions(){
		return segmentVertexProportions;
	}
	
			
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.Mesh#getNucleusName()
	 */
	@Override
	public String getComponentName() {
		return  nucleus.getNameAndNumber();
	}

	
	/**
	 * Add the given point as a vertex to the mesh. Returns the number
	 * of the added vertex
	 * @param p
	 * @param peripheral
	 * @return
	 */
	private int addVertex(IPoint p, boolean peripheral){
		
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
	private MeshEdge getEdge(MeshVertex v1, MeshVertex v2){
		
		if(this.contains(v1) && this.contains(v2)){
			for(MeshEdge e : edges){
				if(e.containsVertex(v1) && e.containsVertex(v2)){
					return e;
				}
			}
			MeshEdge e = new NucleusMeshEdge(v1, v2, 1);
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
	private MeshFace getFace(MeshVertex v1, MeshVertex v2, MeshVertex v3){
		
		if(this.contains(v1) && this.contains(v2) && this.contains(v3)){
						
			for(MeshFace f : faces){
				if(f.contains(v1) && f.contains(v2) && f.contains(v3)){
					return f;
				}
			}
			MeshFace f = new NucleusMeshFace(v1, v2, v3);
			faces.add(f);
			return f;
			
			
		} else {
			return null;
		}
	}
		
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.Mesh#contains(com.bmskinner.nuclear_morphology.analysis.mesh.NucleusMeshVertex)
	 */
	@Override
	public boolean contains(MeshVertex v){
		return( v!=null && (peripheralVertices.contains(v) || internalVertices.contains(v)));
	}
	
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.Mesh#contains(com.bmskinner.nuclear_morphology.analysis.mesh.NucleusMeshFace)
	 */
	@Override
	public boolean contains(MeshFace test){
		return test !=null && faces.contains(test);
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.Mesh#contains(com.bmskinner.nuclear_morphology.analysis.mesh.NucleusMeshFace)
	 */
	@Override
	public boolean contains(IPoint test){
		return hasFaceContaining(test);
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.Mesh#contains(com.bmskinner.nuclear_morphology.analysis.mesh.NucleusMeshEdge)
	 */
	@Override
	public boolean contains(MeshEdge e){
		return e != null && edges.contains(e);
	}
	

	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.Mesh#getSegmentCount()
	 */
	@Override
	public int getSegmentCount(){
		return segmentCount;
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.Mesh#getVertexSpacing()
	 */
	@Override
	public int getVertexSpacing(){
		return this.vertexSpacing;
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.Mesh#getVertexCount()
	 */
	@Override
	public int getVertexCount(){
		return peripheralVertices.size() + internalVertices.size();
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.Mesh#getInternalVertexCount()
	 */
	@Override
	public int getInternalVertexCount(){
		return internalVertices.size();
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.Mesh#getPeripheralVertexCount()
	 */
	@Override
	public int getPeripheralVertexCount(){
		return peripheralVertices.size();
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.Mesh#getEdgeCount()
	 */
	@Override
	public int getEdgeCount(){
		return edges.size();
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.Mesh#getFaceCount()
	 */
	@Override
	public int getFaceCount(){
		return faces.size();
	}
		
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.Mesh#getPeripheralVertices()
	 */
	@Override
	public List<MeshVertex> getPeripheralVertices(){
		return peripheralVertices;
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.Mesh#getInternalVertices()
	 */
	@Override
	public List<MeshVertex> getInternalVertices(){
		return internalVertices;
	}
		
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.Mesh#getEdges()
	 */
	@Override
	public Set<MeshEdge> getEdges(){
		return edges;
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.Mesh#getFaces()
	 */
	@Override
	public Set<MeshFace> getFaces(){
		return this.faces;
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.Mesh#isComparableTo(com.bmskinner.nuclear_morphology.analysis.mesh.NucleusMesh)
	 */
	@Override
	public boolean isComparableTo(Mesh<Nucleus> mesh){
		
		if(this.peripheralVertices.size()!= mesh.getPeripheralVertexCount()){
			return false;
		}
		
		if(this.internalVertices.size()!= mesh.getInternalVertexCount()){
			return false;
		}
		
		if(this.edges.size()!= mesh.getEdgeCount()){
			return false;
		}
		
		if(this.faces.size()!= mesh.getFaceCount()){
			return false;
		}
		return true;
	}
	
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.Mesh#compareTo(com.bmskinner.nuclear_morphology.analysis.mesh.NucleusMesh)
	 */
	@Override
	public Mesh<Nucleus> comparison(Mesh<Nucleus> mesh){
		
		if( ! this.isComparableTo(mesh) ){
			throw new IllegalArgumentException("Cannot compare meshes");
		}
		
		finer("Comparing this mesh "+this.getComponentName()+" to "+mesh.getComponentName());
		finer("Mesh has "+mesh.getFaceCount()+" faces");
		
		NucleusMesh result = new NucleusMesh(this);
		
		List<MeshEdge> ourEdges   = new ArrayList<MeshEdge>(edges);
		List<MeshEdge> theirEdges = new ArrayList<MeshEdge>(mesh.getEdges());
		

		for(int i = 0; i<ourEdges.size(); i++){
			MeshEdge our   = ourEdges.get(i);
			MeshEdge their = theirEdges.get(i);
			
			double ratio = our.getLength() / their.getLength();
			
			// Store the value
			result.getEdge(our).setValue(ratio);
		}
		
		List<MeshFace> ourFaces    = new ArrayList<MeshFace>(faces);
		List<MeshFace> theirFaces  = new ArrayList<MeshFace>(mesh.getFaces());
		List<MeshFace> resultFaces = new ArrayList<MeshFace>(result.faces);
		
		for(int i = 0; i<ourFaces.size(); i++){
			MeshFace our   = ourFaces.get(i);
			MeshFace their = theirFaces.get(i);
			
			double ratio = our.getArea() / their.getArea();
			
			resultFaces.get(i).setValue(ratio);
			
		}
		
		return result;
		
	}
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.Mesh#straighten()
	 */
	@Override
	public Mesh<Nucleus> straighten(){
		fine("Straightening mesh");
		NucleusMesh result = new NucleusMesh(this);
		
		result.clearEdges();
		result.clearFaces();
		
		float nucleusHeight = (float) result.nucleus.getBounds().getHeight();
		float nucleusWidth  = (float) result.nucleus.getBounds().getWidth();
		float vertices      = result.getInternalVertexCount();
		
		float xStep  = nucleusWidth/2;
		float xStart = 0;
		float yStart = 0;
		float yStep  = nucleusHeight / vertices;
		
		IPoint pos = IPoint.makeNew(xStart, yStart);
		
		
		// Straighten internal skeleton
		
		for(int i=0; i<result.internalVertices.size(); i++){
//		for(MeshVertex v : result.internalVertices){
			MeshVertex v = result.internalVertices.get(i);
			v = new NucleusMeshVertex(pos, v.getName(), v.isPeripheral());
			result.peripheralVertices.set(i, v);
//			v.setPosition(pos);
			// update for the next point
			pos = IPoint.makeNew( xStart,  pos.getY()+yStep);
		}
		finer("Set skeleton");
		
		// Position peripheral vertices around skeleton
		
		// Positon the first vertex under the skeleton
		MeshVertex v = result.peripheralVertices.get(0);
		v = new NucleusMeshVertex(IPoint.makeNew(xStart, yStart-yStep), v.getName(), v.isPeripheral());
		result.peripheralVertices.set(0, v);
//		v.setPosition( IPoint.makeNew(xStart, yStart-yStep)  );
		
		int halfArray = (int) Math.floor(( (double) result.peripheralVertices.size() / 2));
		
		for(int i=1, j=result.getPeripheralVertexCount()-1; i<halfArray; i++, j--){
			
			// Get the vertices either side of the skeleton
			MeshVertex v1 = result.peripheralVertices.get(i);
			MeshVertex v2 = result.peripheralVertices.get(j);
			
			v1 = new NucleusMeshVertex( IPoint.makeNew(  xStart-xStep ,   (i*yStep)-(yStep/2) ), v1.getName(), v1.isPeripheral());
			v2 = new NucleusMeshVertex( IPoint.makeNew(  xStart+xStep ,   (i*yStep)-(yStep/2) ), v2.getName(), v2.isPeripheral());
			
			result.peripheralVertices.set(i, v1);
			result.peripheralVertices.set(j, v2);
		}
		
		finer("Peripheral vertex count = "+result.getPeripheralVertexCount());
				
		// if needed, adjust the final vertex above the skeleton
		if(result.peripheralVertices.size()%2==0){
			finer("Setting final vertex");
			MeshVertex p1 = result.peripheralVertices.get(halfArray);
			p1 = new NucleusMeshVertex(IPoint.makeNew(  xStart ,   (halfArray*yStep)-(yStep/2) ), p1.getName(), p1.isPeripheral());
			result.peripheralVertices.set(halfArray, p1);
		}
		
		finer("Set periphery");
		
		result.createEdgesAndFaces();
		
		finer("Straightened mesh");
		finer(result.toString());
		
	
		return result;
	}
	
	
	/* (non-Javadoc)
	 * @see com.bmskinner.nuclear_morphology.analysis.mesh.Mesh#toPath()
	 */
	@Override
	public Path2D toPath(){
		Path2D path = new Path2D.Double();

		int i=0;
		for(MeshVertex v : peripheralVertices){
			
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
	@Override
	public MeshFace getFace(MeshFace test){
		
		for(MeshFace f : faces){
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
	@Override
	public MeshEdge getEdge(MeshEdge test){
		for(MeshEdge e : edges){
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
	private void determineVertexProportions() throws MeshCreationException{
		
		finer("Determining vertex proportions");
		List<IBorderSegment> list;

			try {
				list = nucleus.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).getOrderedSegments();
			} catch (UnavailableBorderTagException
					| UnavailableProfileTypeException | ProfileException e) {
				throw new MeshCreationException("Unable to get segments from template nucleus", e);
			}

		
		int segNumber = 0;
		
		for(IBorderSegment seg : list){
			
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
	 * @throws MeshCreationException  
	 */
	private void createPeripheralVertices() throws MeshCreationException {
		finer("Creating peripheral vertices");

		List<IBorderSegment> list;

			try {
				list = nucleus.getProfile(ProfileType.ANGLE, Tag.REFERENCE_POINT).getOrderedSegments();
			} catch (UnavailableBorderTagException
					| UnavailableProfileTypeException | ProfileException e) {
				throw new MeshCreationException("Unable to get segments from template nucleus", e);
			}
				
		Set<Integer> segs = segmentVertexProportions.keySet();
		for(int segIndex : segs){
			
			IBorderSegment segment = list.get(segIndex);
			finer("Segment "+segIndex+": "+segment.length());
			
			
			
			List<Double> proportions = segmentVertexProportions.get(segIndex);
			
			
			if(segment.length()<=proportions.size()){
				// The segment is too small for each vertex to have a separate XYPoint
				// Usually caused when mapping a poorly segmented nucleus onto a template mesh.
				throw new IllegalArgumentException("Segment "+segIndex+" is too small to fit mesh");
			}
			
			for(Double d : proportions){
				int index = segment.getProportionalIndex(d);
				
				// Since the segments have been offset to the RP, correct back
				// to the actual nucleus index
				int correctedIndex = DefaultCellularComponent
						.wrapIndex(index+nucleus.getBorderIndex(Tag.REFERENCE_POINT), segment.getTotalLength());
				
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
	@Override
	public MeshFace getFace(IPoint p){
		if(nucleus.containsOriginalPoint(p)){
			for(MeshFace f : faces){
				if(f.contains(p)){
					return f;
				}
			}
			
		} 
		return null;
	}
	
	protected boolean hasFaceContaining(IPoint p){
		
		if(nucleus.containsOriginalPoint(p)){


			for(MeshFace f : faces){
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
			
			MeshVertex v1 = peripheralVertices.get(i);
			MeshVertex v2 = peripheralVertices.get(peripheralVertices.size()-i);
			
			MeshEdge e = new NucleusMeshEdge(v1, v2, 1);
			
			this.addVertex (e.getMidpoint(), false);
		}
		
	}
	
	private void createEdgesAndFaces(){
		// Build the edges
		
		// Link peripheral vertices
		finer("Creating peripheral edges");
		for(int i=0, j=1; j<peripheralVertices.size(); i++, j++){
			
			MeshVertex v1 = peripheralVertices.get(i);
			MeshVertex v2 = peripheralVertices.get(j);
			
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
			
			MeshVertex v1 = internalVertices.get(i);
			MeshVertex v2 = internalVertices.get(j);
			
			// Getting adds the edge to the internal list
			this.getEdge(v1, v2);
		}
		
		
		// Link between peripheral and internal vertices
		int halfArray = (int) Math.floor(( (double) peripheralVertices.size() / 2));
		
		finer("Linking peripheral edges and internal edges");
		finer("Peripheral vertices: "+peripheralVertices.size());
		finer("Internal vertices: "+internalVertices.size());
		finer("Half array: "+halfArray);
		
		try {

			// Link the RP point (peripheral index 0) to the first internal vertex

			// Starting at each end of the periperal array, make edges to the internal vertices
			for(int i=1, j=peripheralVertices.size()-1; i<halfArray; i++, j--){

				// Points A are ascending from the RP
				// Points X are decending from the RP

				MeshVertex p1_a = peripheralVertices.get(i);
				MeshVertex p1_x = peripheralVertices.get(j);

				MeshVertex p2_a = peripheralVertices.get(i+1);
				MeshVertex p2_x = peripheralVertices.get(j-1);

				// Each peripheral vertex links to two internal vertices
				MeshVertex i1 = internalVertices.get(i-1);

				// handle the end of the internal skeleton
				MeshVertex i2;
				if(i==internalVertices.size()){
					i2 = i1; // when there is no point here, use the same vertex as i1
				} else {
					i2 = internalVertices.get(i);
				}

				this.getEdge(p1_a, i1);
				this.getEdge(p2_a, i1);
				this.getEdge(p2_a, i2);

		
				this.getEdge(p1_x, i1);
				this.getEdge(p2_x, i1);
				this.getEdge(p2_x, i2);

				
				// Make the faces
				this.getFace(p1_a, i1, p2_a);
				this.getFace(p2_a, i1, i2);

				this.getFace(p1_x, i1, p2_x);
				this.getFace(p2_x, i1, i2);
			}
			
			finer("Created first face set");

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

			finer("Created top face set");

			// if needed, create the bottom face (final intenal vertex to central peripheral vertices)
			if(peripheralVertices.size()%2!=0){

				finer("Need bottom face set");
				
				MeshVertex p1 = peripheralVertices.get(halfArray);
				MeshVertex p2 = peripheralVertices.get(halfArray+1);
				MeshVertex i1 = internalVertices.get(internalVertices.size()-1);

				// Ensure the edges are created
				getEdge(p1, p2);
				getEdge(p1, i1);
				getEdge(p2, i1);

				this.getFace(p1, p2, i1);
				
				finer("Created bottom face set");
			}

		} catch(Exception e){
			warn("Error linking edges and vertices in mesh");
			log(Level.FINE, "Error linking edges and vertices in mesh", e);
			fine(this.toString());
		}
		
		
	}
	
	public String toString(){
		
		StringBuilder b = new StringBuilder();
		b.append("Nucleus mesh based on "+this.getComponentName()+"\n");
		b.append("Peripheral vertices:\n");
		
		for(MeshVertex v : this.peripheralVertices){
			b.append(v.toString()+"\n");
		}
		
		b.append("Internal vertices:\n");
		for(MeshVertex v : this.internalVertices){
			b.append(v.toString()+"\n");
		}
		
		b.append("Edges:\n");
		for(MeshEdge e : edges){
			b.append(e.toString()+"\n");
		}
		
		b.append("Faces:\n");
		for(MeshFace f : faces){
			b.append(f.toString()+"\n");
		}
		return b.toString();
	}

	@Override
	public int compareTo(Mesh<Nucleus> o) {
		
		if(this.isComparableTo(o)){
			return 0;
		}
		
		if(this.getVertexCount()>o.getVertexCount()){
			return 1;
		}
		return -1;
	}
			
}
