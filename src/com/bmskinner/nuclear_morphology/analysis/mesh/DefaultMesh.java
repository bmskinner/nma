/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.analysis.mesh;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderPointException;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * A default implementation of the mesh
 * 
 * @author bms41
 *
 */
public class DefaultMesh<E extends Taggable> implements Mesh<E> {
	
	private static final Logger LOGGER = Logger.getLogger(DefaultMesh.class.getName());
	
	private static final boolean IS_PERIPERHAL = true;
	private static final boolean IS_INTERNAL   = false;
	private static final double DEFAULT_EDGE_WEIGHT = 1;

    /**  The average number of border points between vertices */
    private int vertexSpacing = DEFAULT_VERTEX_SPACING;

    /** For each segment, list the proportions through the segment at which a vertex is found */
    private final Map<Integer, List<Double>> segmentVertexProportions = new HashMap<>();

    /** Store the vertices in the mesh. List, so we can index it. */
    private final List<MeshVertex> peripheralVertices = new ArrayList<>();

    /** Store the skeleton vertices in the mesh */
    private final List<MeshVertex> internalVertices = new ArrayList<>();

    /** Track the edges in the mesh */
    private final Set<MeshEdge> edges = new LinkedHashSet<>();

    /** Track the faces in the mesh */
    private final Set<MeshFace> faces = new LinkedHashSet<>();
    
    /** Track the faces associated with each segment */
    private final Map<Integer, Set<MeshVertex>> segmentFaces = new HashMap<>();

    /** The component the mesh is constructed from */
    private final E component;

    /**
     * Construct a mesh from the given component with default vertex spacing
     * 
     * @param c the component to construct from 
     * @throws MeshCreationException
     */
    public DefaultMesh(@NonNull E c) throws MeshCreationException {
        this(c, DEFAULT_VERTEX_SPACING);
    }

    /**
     * Construct a mesh from the given component
     * 
     * @param c the component to construct from 
     * @param vertexSpacing the spacing between peripheral vertices
     * @throws MeshCreationException
     */
    public DefaultMesh(@NonNull E c, int vertexSpacing) throws MeshCreationException {
        this.component = c;
        this.vertexSpacing = vertexSpacing;
        
        try {
            determineVertexProportions();
            createPeripheralVertices();
            createInternalVertices();
            createEdgesAndFaces();
            
        } catch (IllegalArgumentException e) {
        	LOGGER.log(Loggable.STACK,"Unable to create mesh for component", e);
            throw new MeshCreationException("Unable to create mesh for component", e);
        }
    }

    /**
     * Create a mesh from a nucleus, using another mesh as a template for
     * proportions
     * 
     * @param n the object to create a mesh from
     * @param template the mesh to use for proportions
     * @throws MeshCreationException
     */
    public DefaultMesh(@NonNull E n, @NonNull Mesh<E> template) throws MeshCreationException {
        this.component = n;
        
        for(Integer segIndex : template.getVertexProportions().keySet()) {
        	segmentVertexProportions.put(segIndex, template.getVertexProportions().get(segIndex));
    		segmentFaces.put(segIndex, new HashSet<>());
        }
        
        this.vertexSpacing = template.getVertexSpacing();

        try {
            createPeripheralVertices();
            createInternalVertices();
            createEdgesAndFaces();

        } catch (IllegalArgumentException e) {
            LOGGER.fine("Error creating mesh: "+e.getMessage());
            throw new MeshCreationException("Unable to create mesh for component", e);
        }
    }

    /**
     * Duplicate the mesh.
     * 
     * @param template
     */
    public DefaultMesh(Mesh<E> template) {
        component = template.getComponent();
        vertexSpacing = template.getVertexSpacing();
        
        for(Integer segIndex : template.getVertexProportions().keySet()) {
        	segmentVertexProportions.put(segIndex, template.getVertexProportions().get(segIndex));
    		segmentFaces.put(segIndex, new HashSet<>());
        }

        for (MeshEdge e : template.getEdges())
            edges.add(new DefaultMeshEdge(e));

        for (MeshFace e : template.getFaces())
            faces.add(new DefaultMeshFace(e));

        peripheralVertices.addAll(template.getPeripheralVertices());
        internalVertices.addAll(template.getInternalVertices());
    }

    @Override
    public E getComponent() {
        return component;
    }

    @Override
    public Map<Integer, List<Double>> getVertexProportions() {
        return segmentVertexProportions;
    }

    @Override
    public String getComponentName() {
        return component==null ? "" : component.toString();
    }

    /**
     * Add the given point as a vertex to the mesh. Returns the number of the
     * added vertex
     * 
     * @param p
     * @param peripheral
     * @return
     */
    private int addVertex(@NonNull IPoint p, boolean peripheral) {

        if (peripheral) {
            int newIndex = peripheralVertices.size();
            String name = "P" + newIndex;
            peripheralVertices.add(new DefaultMeshVertex(p, name, peripheral));
            return newIndex;
        }
		int newIndex = internalVertices.size();
		String name = "I" + newIndex;
		internalVertices.add(new DefaultMeshVertex(p, name, peripheral));
		return newIndex;
    }

    /**
     * Fetch or create the edge between the given vertices
     * 
     * @param v1
     * @param v2
     * @return
     */
    private MeshEdge getEdge(MeshVertex v1, MeshVertex v2) {

        if (this.contains(v1) && this.contains(v2)) {
            for (MeshEdge e : edges) {
                if (e.containsVertex(v1) && e.containsVertex(v2))
                    return e;
            }
            MeshEdge e = new DefaultMeshEdge(v1, v2, DEFAULT_EDGE_WEIGHT);
            edges.add(e);
            return e;
        }
		throw new IllegalArgumentException("Mesh does not contain vertices");
    }

    /**
     * Fetch or create the face bounded by the given vertices
     * 
     * @param v1
     * @param v2
     * @param v3
     * @return
     */
    private MeshFace getFace(MeshVertex v1, MeshVertex v2, MeshVertex v3) {

        if (this.contains(v1) && this.contains(v2) && this.contains(v3)) {

            for (MeshFace f : faces) {
                if (f.contains(v1) && f.contains(v2) && f.contains(v3)) {
                    return f;
                }
            }
            MeshFace f = new DefaultMeshFace(v1, v2, v3);
            faces.add(f);
            return f;

        }
		return null;
    }


    @Override
    public boolean contains(@NonNull MeshVertex v) {
        return (v != null && (peripheralVertices.contains(v) || internalVertices.contains(v)));
    }

    @Override
    public boolean contains(@NonNull MeshFace test) {
        return test != null && faces.contains(test);
    }

    @Override
    public boolean contains(@NonNull IPoint test) {
        return hasFaceContaining(test);
    }

    @Override
    public boolean contains(@NonNull MeshEdge e) {
        return e != null && edges.contains(e);
    }


    @Override
    public int getVertexSpacing() {
        return this.vertexSpacing;
    }

    @Override
    public int getVertexCount() {
        return peripheralVertices.size() + internalVertices.size();
    }

    @Override
    public int getInternalVertexCount() {
        return internalVertices.size();
    }

    @Override
    public int getPeripheralVertexCount() {
        return peripheralVertices.size();
    }

    @Override
    public int getEdgeCount() {
        return edges.size();
    }

    @Override
    public int getFaceCount() {
        return faces.size();
    }

    @Override
    public List<MeshVertex> getPeripheralVertices() {
        return peripheralVertices;
    }

    @Override
    public List<MeshVertex> getInternalVertices() {
        return internalVertices;
    }

    @Override
    public Set<MeshEdge> getEdges() {
        return edges;
    }

    @Override
    public Set<MeshFace> getFaces() {
        return this.faces;
    }

    @Override
    public boolean isComparableTo(@NonNull Mesh<E> mesh) { 	
        if (this.peripheralVertices.size() != mesh.getPeripheralVertexCount())
            return false;
        if (this.internalVertices.size() != mesh.getInternalVertexCount())
            return false;
        if (this.edges.size() != mesh.getEdgeCount())
            return false;
        if (this.faces.size() != mesh.getFaceCount())
            return false;
        return true;
    }
    
    @Override
    public Mesh<E> comparison(@NonNull E target) throws MeshCreationException {
    	return comparison(new DefaultMesh<E>(target, this));
    }
    

    @Override
    public Mesh<E> comparison(@NonNull Mesh<E> mesh) {

        if (!this.isComparableTo(mesh))
            throw new IllegalArgumentException("Cannot compare meshes");

        LOGGER.finest( "Comparing this mesh " + this.getComponentName() + " to " + mesh.getComponentName());
        LOGGER.finest( "Mesh has " + mesh.getFaceCount() + " faces");

        DefaultMesh<E> result = new DefaultMesh<E>(this);

        List<MeshEdge> ourEdges = new ArrayList<MeshEdge>(edges);
        List<MeshEdge> theirEdges = new ArrayList<MeshEdge>(mesh.getEdges());

        for (int i = 0; i < ourEdges.size(); i++) {
            MeshEdge our = ourEdges.get(i);
            MeshEdge their = theirEdges.get(i);

            double ratio = our.getLength() / their.getLength();

            // Store the value
            result.getEdge(our).setValue(ratio);
        }

        List<MeshFace> ourFaces    = new ArrayList<>(faces);
        List<MeshFace> theirFaces  = new ArrayList<>(mesh.getFaces());
        List<MeshFace> resultFaces = new ArrayList<>(result.faces);

        for (int i = 0; i < ourFaces.size(); i++) {
            MeshFace our = ourFaces.get(i);
            MeshFace their = theirFaces.get(i);

            double ratio = our.getArea() / their.getArea();

            resultFaces.get(i).setValue(ratio);

        }

        return result;

    }

    @Override
    public Path2D toPath() {
        Path2D path = new Path2D.Double();

        int i = 0;
        for (MeshVertex v : peripheralVertices) {

            if (i++ == 0) {
                path.moveTo(v.getPosition().getX(), v.getPosition().getY());
            } else {
                path.lineTo(v.getPosition().getX(), v.getPosition().getY());
            }

        }
        path.closePath();
        return path;
    }

    @Override
    public MeshFace getFace(@NonNull MeshFace test) {

        for (MeshFace f : faces) {
            if (f.equals(test)) {
                return f;
            }
        }
        LOGGER.finer( "Cannot find face in mesh: " + test.toString());
        return null;
    }

    @Override
    public MeshEdge getEdge(@NonNull MeshEdge test) {
        for (MeshEdge e : edges) {
            if (e.equals(test)) {
                return e;
            }
        }
        LOGGER.finer( "Cannot find edge in mesh: " + test.toString());
        return null;
    }

    private void clearEdges() {
        this.edges.clear();
    }

    private void clearFaces() {
        this.faces.clear();
    }

    /**
     * Find the index proportions for each peripheral vertex
     * 
     */
    private void determineVertexProportions() throws MeshCreationException {

    	LOGGER.finest( "Determining vertex proportions");

    	try {
    		List<IProfileSegment> segments = component.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT).getOrderedSegments();

    		for(int segNumber=0; segNumber<segments.size(); segNumber++) {
    			IProfileSegment seg = segments.get(segNumber);
    			List<Double> proportions = new ArrayList<>();

    			double div = (double) seg.length() / (double) vertexSpacing;

    			// the closest number of divisions to a spacing of vertexSpacing
    			long divisions = Math.round(div); 

    			LOGGER.finest( "Dividing segment into " + divisions + " parts");

    			for (int i = 0; i < divisions; i++) {

    				double proportion = (double) i / (double) divisions;
    				LOGGER.finest( "Fetching point at proportion " + proportion);
    				proportions.add(proportion);
    			}

    			// Store the  proportion through the segment of each vertex
    			segmentVertexProportions.put(segNumber, proportions);
    			segmentFaces.put(segNumber, new HashSet<>());
    		}
    	} catch (UnavailableBorderTagException | UnavailableProfileTypeException | ProfileException e) {
    		throw new MeshCreationException("Unable to get segments from template nucleus", e);
    	}

    }

    /**
     * Using the vertex spacing as a guide, determing the number and proportion
     * of vertices to make for each segment. Then select the appropriate border
     * points and create vertices.
     * 
     * @throws MeshCreationException
     */
    private void createPeripheralVertices() throws MeshCreationException {
        LOGGER.finest( "Creating peripheral vertices");
        try {
            List<IProfileSegment> list = component.getProfile(ProfileType.ANGLE, Landmark.REFERENCE_POINT).getOrderedSegments();

            Set<Integer> segs = segmentVertexProportions.keySet();
            for (int segIndex : segs) {

                IProfileSegment segment = list.get(segIndex);
                Set<MeshVertex> segVertices = segmentFaces.get(segIndex);
                LOGGER.finest( "Segment " + segIndex + ": " + segment.length());

                List<Double> proportions = segmentVertexProportions.get(segIndex);

                if (segment.length() <= proportions.size()) {
                    /* The segment is too small for each vertex to have a
                    separate point. Usually caused when mapping a poorly segmented 
                    nucleus onto a template mesh. */
                    throw new IllegalArgumentException("Segment " + segIndex + " is too small to fit mesh");
                }
                
                // Now go through the segment and take the point at each determined proportion as a vertex

                for (Double d : proportions) {
                    int index = segment.getProportionalIndex(d);

                    // Since the segments have been offset to the RP, correct back to the actual nucleus index
                    int correctedIndex = CellularComponent
                            .wrapIndex(index + component.getBorderIndex(Landmark.REFERENCE_POINT), segment.getProfileLength());

                    LOGGER.finest( "Fetching point at index " + correctedIndex);

                    int vertIndex = addVertex(component.getOriginalBorderPoint(correctedIndex), IS_PERIPERHAL);
                    
                    // Add the vertex to the map with the segment.
                    MeshVertex v = peripheralVertices.get(vertIndex);
                    segVertices.add(v);
                }

            }
        } catch (UnavailableBorderTagException | UnavailableProfileTypeException | ProfileException
                | UnavailableBorderPointException e) {
            throw new MeshCreationException("Unable to get segments from template nucleus", e);
        }
    }


    @Override
    public MeshFace getFace(@NonNull IPoint p) {
        if (component.containsOriginalPoint(p)) {
            for (MeshFace f : faces)
                if (f.contains(p))
                    return f;
        }
        return null;
    }

    protected boolean hasFaceContaining(IPoint p) {

        if (component.containsOriginalPoint(p)) {

            for (MeshFace f : faces) {
                if (f.contains(p)) {
                    return true;
                }
            }

        }
        return false;
    }

    /**
     * Starting at the reference point, create vertices between the peripheral
     * vertex pairs down to the tail
     */
    private void createInternalVertices() {

        LOGGER.finest( "Creating internal vertices");
        /*
         * The first index in the list is the reference point. Take the second
         * and last, the third and next-to-last etc
         */
        int halfArray = peripheralVertices.size() >> 1;

        for (int i = 1; i < halfArray; i++) {

            MeshVertex v1 = peripheralVertices.get(i);
            MeshVertex v2 = peripheralVertices.get(peripheralVertices.size() - i);

            MeshEdge e = new DefaultMeshEdge(v1, v2, DEFAULT_EDGE_WEIGHT);
            addVertex(e.getMidpoint(), IS_INTERNAL);
        }

    }

    private void createEdgesAndFaces() {
        // Build the edges

        // Link peripheral vertices
        LOGGER.finest( "Creating peripheral edges");
        for (int i = 0, j = 1; j < peripheralVertices.size(); i++, j++) {

            MeshVertex v1 = peripheralVertices.get(i);
            MeshVertex v2 = peripheralVertices.get(j);

            // Getting adds the edge to the internal list
            this.getEdge(v1, v2);

            if (j == peripheralVertices.size() - 1) {

                // final link
                v1 = peripheralVertices.get(peripheralVertices.size() - 1);
                v2 = peripheralVertices.get(0);
                this.getEdge(v1, v2);
            }
        }

        LOGGER.finest( "Creating internal edges");
        // Link the internal vertices, from peripheral vertex 0
        for (int i = 0, j = 1; j < internalVertices.size(); i++, j++) {

            MeshVertex v1 = internalVertices.get(i);
            MeshVertex v2 = internalVertices.get(j);

            // Getting adds the edge to the internal list
            this.getEdge(v1, v2);
        }

        // Link between peripheral and internal vertices
        int halfArray = (int) Math.floor(((double) peripheralVertices.size() / 2));

        LOGGER.finest( "Linking peripheral edges and internal edges");
        LOGGER.finest( "Peripheral vertices: " + peripheralVertices.size());
        LOGGER.finest( "Internal vertices: " + internalVertices.size());
//        LOGGER.finest( "Half array: " + halfArray);

        try {

            // Link the RP point (peripheral index 0) to the first internal
            // vertex

            // Starting at each end of the periperal array, make edges to the
            // internal vertices
            for (int i = 1, j = peripheralVertices.size() - 1; i < halfArray; i++, j--) {

                // Points A are ascending from the RP
                // Points X are decending from the RP

                MeshVertex p1_a = peripheralVertices.get(i);
                MeshVertex p1_x = peripheralVertices.get(j);

                MeshVertex p2_a = peripheralVertices.get(i + 1);
                MeshVertex p2_x = peripheralVertices.get(j - 1);

                // Each peripheral vertex links to two internal vertices
                MeshVertex i1 = internalVertices.get(i - 1);

                // handle the end of the internal skeleton
                MeshVertex i2;
                if (i == internalVertices.size()) {
                    i2 = i1; // when there is no point here, use the same vertex
                             // as i1
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

            LOGGER.finest( "Created first face set");

            // create the top faces - RP to nearest peripheral indexes to I0
            getEdge(peripheralVertices.get(0), internalVertices.get(0));
            getEdge(peripheralVertices.get(1), internalVertices.get(0));
            getEdge(peripheralVertices.get(peripheralVertices.size() - 1), internalVertices.get(0));

            this.getFace(peripheralVertices.get(0), peripheralVertices.get(1), internalVertices.get(0));

            this.getFace(peripheralVertices.get(0), peripheralVertices.get(peripheralVertices.size() - 1),
                    internalVertices.get(0));

            LOGGER.finest( "Created top face set");

            // if needed, create the bottom face (final intenal vertex to
            // central peripheral vertices)
            if (peripheralVertices.size() % 2 != 0) {

                LOGGER.finest( "Creating bottom face set");

                MeshVertex p1 = peripheralVertices.get(halfArray);
                MeshVertex p2 = peripheralVertices.get(halfArray + 1);
                MeshVertex i1 = internalVertices.get(internalVertices.size() - 1);

                // Ensure the edges are created
                getEdge(p1, p2);
                getEdge(p1, i1);
                getEdge(p2, i1);

                this.getFace(p1, p2, i1);

                LOGGER.finest( "Created bottom face set");
            }

        } catch (Exception e) {
            LOGGER.log(Loggable.STACK, "Error linking edges and vertices in mesh", e);
        }

    }

    @Override
	public Set<MeshFace> getFaces(IProfileSegment seg){
    	Set<MeshVertex> vertices = this.segmentFaces.get(seg.getPosition());
    	
//    	faces.stream().filter(f->f.isPeripheral()&&f.getPeripheralVertices().stream().allMatch(v->vertices.contains(v)));
    	
    	return edges.stream().filter(e->{
    		return (vertices.contains(e.getV1()))
    				&&(vertices.contains(e.getV2())||e.getV2().isInternal());
    			}).flatMap(e->faces.stream().filter(f->f.contains(e)))
    			.collect(Collectors.toSet());
    }
    
    @Override
    public String toString() {

        StringBuilder b = new StringBuilder();
        b.append("Nucleus mesh based on " + this.getComponentName() + "\n");
        b.append("Peripheral vertices:\n");

        for (MeshVertex v : this.peripheralVertices) {
            b.append(v.toString() + "\n");
        }

        b.append("Internal vertices:\n");
        for (MeshVertex v : this.internalVertices) {
            b.append(v.toString() + "\n");
        }

        b.append("Edges:\n");
        for (MeshEdge e : edges) {
            b.append(e.toString() + "\n");
        }

        b.append("Faces:\n");
        for (MeshFace f : faces) {
            b.append(f.toString() + "\n");
        }
        return b.toString();
    }

    @Override
    public int compareTo(Mesh<E> o) {

        if (this.isComparableTo(o)) {
            return 0;
        }

        if (this.getVertexCount() > o.getVertexCount()) {
            return 1;
        }
        return -1;
    }

	@Override
	public double getMaxEdgeRatio() {
		return edges.stream().mapToDouble(e->e.getLog2Ratio()).max().orElse(0);
	}

}
