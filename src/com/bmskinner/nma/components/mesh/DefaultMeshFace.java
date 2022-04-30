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
package com.bmskinner.nma.components.mesh;

import java.awt.geom.Path2D;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.FloatEquation;
import com.bmskinner.nma.components.measure.LineEquation;
import com.bmskinner.nma.gui.dialogs.VersionHelpDialog;
import com.bmskinner.nma.stats.Stats;

public class DefaultMeshFace implements MeshFace {
	
	private static final Logger LOGGER = Logger.getLogger(VersionHelpDialog.class.getName());

    final Set<MeshEdge> edges = new HashSet<>();

    private final Set<MeshVertex> vertices = new HashSet<>();

    private double value = 1;

    /**
     * Construct from edges.
     * @param e1 the first edge
     * @param e2 the second edge
     * @param e3 the third edge
     */
    public DefaultMeshFace(final MeshEdge e1, final MeshEdge e2, final MeshEdge e3) {

        // Check that the edges make an enclosed space - there are only 3 unique
        // vertices
        this.edges.add(e1);
        this.edges.add(e2);
        this.edges.add(e3);

        for (MeshEdge e : edges) {
            vertices.add(e.getV1());
            vertices.add(e.getV2());
        }

        if (vertices.size() != 3) {
            throw new IllegalArgumentException("Edges must enclose a triangle");
        }

    }

    /**
     * Construct from vertices.
     * @param e1 the first vertex
     * @param e2 the second vertex
     * @param e3 the third vertex
     */
    public DefaultMeshFace(final MeshVertex v1, final MeshVertex v2, final MeshVertex v3) {

        if (!v1.hasEdgeTo(v2)) {
            throw new IllegalArgumentException("Vertices v1 and v2 are not linked in face constructor: " + v1.toString()
                    + " and " + v2.toString());
        }

        if (!v1.hasEdgeTo(v3)) {
            throw new IllegalArgumentException("Vertices v1 and v3 are not linked in face constructor: " + v1.toString()
                    + " and " + v3.toString());
        }

        if (!v2.hasEdgeTo(v3)) {
            throw new IllegalArgumentException("Vertices v2 and v3 are not linked in face constructor: " + v2.toString()
                    + " and " + v3.toString());
        }

        vertices.add(v1);
        vertices.add(v2);
        vertices.add(v3);

        edges.add(v1.getEdgeTo(v2));
        edges.add(v2.getEdgeTo(v3));
        edges.add(v3.getEdgeTo(v1));

    }

    /**
     * Construct from an existing face.
     * 
     * @param f the face to duplicate
     */
    public DefaultMeshFace(final MeshFace f) {
        for (MeshEdge e : f.getEdges()) {
            edges.add(new DefaultMeshEdge(e));

            vertices.add(e.getV1());
            vertices.add(e.getV2());
        }
        this.value = f.getValue();
    }

    @Override
    public double getValue() {
        return value;
    }

    @Override
    public double getLog2Ratio() {
        return Stats.calculateLog2Ratio(value);
    }

    @Override
    public void setValue(double value) {
        this.value = value;
    }
    
	@Override
	public boolean isPeripheral() {
		return edges.stream().anyMatch(e->e.isPeripheral());
	}

    @Override
    public Set<MeshEdge> getEdges() {
        return edges;
    }

    @Override
    public Set<MeshVertex> getVertices() {
        return vertices;
    }
    
    @Override
    public Set<MeshVertex> getPeripheralVertices() {
        return vertices.stream().filter(v->v.isPeripheral()).collect(Collectors.toSet());
    }
    
    @Override
    public Set<MeshVertex> getInternalVertices() {
    	return vertices.stream().filter(v->v.isInternal()).collect(Collectors.toSet());
    }

    @Override
    public boolean contains(MeshEdge e) {
        return edges.contains(e);
    }

    @Override
    public boolean contains(MeshVertex v) {
        return vertices.contains(v);
    }

    @Override
    public boolean contains(IPoint p) {

        return this.toPath().contains(p.toPoint2D());

    }

    @Override
    public double getArea() {
        // Use Heron's formula:
        // s = (a+b+c) /2
        // a = sqrt( s(s-a)(s-b)(s-c) )

        double s = 0;
        for (MeshEdge e : edges) {
            s += e.getLength();
        }
        s /= 2;

        double a2 = s;

        for (MeshEdge e : edges) {
            double t = s - e.getLength();
            a2 *= t;
        }
        double a = Math.sqrt(a2);
        return a;

    }

    /**
     * Get the edge opposite the given vertex. This is the edge that does not
     * contain the vertex.
     * 
     * @param e
     * @return
     */
    private MeshEdge getOppositeEdge(MeshVertex v) {

        for (MeshEdge e : edges) {
            if (!e.containsVertex(v)) {
                return e;
            }
        }
        return null;

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;

        for (MeshEdge e : edges) {
            result = prime * result + e.hashCode();
        }

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;

        DefaultMeshFace other = (DefaultMeshFace) obj;

        // vertex tests for name and peripheral only
        for (MeshVertex v : vertices) {
            if (!other.vertices.contains(v)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String getName() {
        StringBuilder b = new StringBuilder();

        for (MeshVertex v : getVertices()) {
            b.append(v.getName() + " ");
        }
        return b.toString();
    }

    @Override
    public IPoint getMidpoint() {

        double avgX = 0;
        double avgY = 0;

        for (MeshVertex v : getVertices()) {
            avgX += v.getPosition().getX();
            avgY += v.getPosition().getY();
        }
        avgX /= 3;
        avgY /= 3;

        return new FloatPoint(avgX, avgY);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Face: " + this.getPeripheralVertexCount() + " peripheral vertices | Area: " + this.getArea()
                + " | Value: " + this.getValue() + "\n");

        for (MeshVertex v : vertices) {
            b.append(v.toString() + "\n");
        }
        return b.toString();
    }

    @Override
    public Path2D toPath() {
        Path2D path = new Path2D.Double();

        int i = 0;
        for (MeshVertex v : vertices) {

            if (i++ == 0) {
                path.moveTo(v.getPosition().getX(), v.getPosition().getY());
            } else {
                path.lineTo(v.getPosition().getX(), v.getPosition().getY());
            }

        }
        path.closePath();
        return path;
    }

    /**
     * Count the number of vertices in the face that are peripheral or internal
     * 
     * @param peripheral
     * @return
     */
    @Override
    public int getPeripheralVertexCount() {
        int count = 0;
        for (MeshVertex v : vertices) {
            if (v.isPeripheral()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Count the number of vertices in the face that are peripheral or internal
     * 
     * @param peripheral
     * @return
     */
    @Override
    public int getInternalVertexCount() {
        int count = 0;
        for (MeshVertex v : vertices) {
            if (!v.isPeripheral()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get the internal vertex of the face if it has two peripheral vertices, or
     * the peripheral vertex if there are two internal vertices
     * 
     * @return
     */
    @Override
    public MeshVertex getLowerInternalVertex() {

        int index = Integer.MAX_VALUE;
        MeshVertex result = null;

        for (MeshVertex v : vertices) {
            if (!v.isPeripheral()) {

                int number = v.getNumber();
                if (number < index) {
                    index = number;
                    result = v;
                }
            }
        }
        return result;
    }

    @Override
    public MeshVertex getHigherInternalVertex() {

        int index = -1;
        MeshVertex result = null;

        for (MeshVertex v : vertices) {
            if (!v.isPeripheral()) {
                int number = v.getNumber();
                if (number > index) {
                    index = number;
                    result = v;
                }
            }
        }
        return result;
    }

    /**
     * Get the lower peripheral vertex of the face
     * 
     * @return
     */
    @Override
    public MeshVertex getLowerPeripheralVertex() {

        int index = Integer.MAX_VALUE;
        MeshVertex result = null;

        for (MeshVertex v : vertices) {
            if (v.isPeripheral()) {

                int number = v.getNumber();
                if (number < index) {
                    index = number;
                    result = v;
                }
            }
        }
        return result;
    }

    /**
     * Get the upper peripheral vertex of the face
     * 
     * @return
     */
    @Override
    public MeshVertex getHigherPeripheralVertex() {

        int index = -1;
        MeshVertex result = null;

        for (MeshVertex v : vertices) {
            if (v.isPeripheral()) {
                int number = v.getNumber();
                if (number > index) {
                    index = number;
                    result = v;
                }
            }
        }
        return result;
    }

    /**
     * Get the proportional distance of the given point along the edge opposite
     * the given vertex
     * 
     * @param v
     *            the vertex opposite the edge
     * @param p
     *            the point within the face
     * @return
     * @throws PixelOutOfBoundsException
     */
    private double getEdgeProportion(MeshVertex v, IPoint p) throws PixelOutOfBoundsException {

        // Line from vertex to point
        LineEquation eq1 = new FloatEquation(v.getPosition(), p);

        // Edge opposite the vertex
        MeshEdge oppEdge = this.getOppositeEdge(v);
        oppEdge = correctEdgeOrientation(oppEdge);

        MeshVertex o1 = oppEdge.getV1();
        MeshVertex o2 = oppEdge.getV2();

        // Line marking opposite edge
        LineEquation eq2 = new FloatEquation(o1.getPosition(), o2.getPosition());

        // Position on edge intercepting line from vertex through point p
        IPoint intercept = eq2.getIntercept(eq1);

        // Proportion through edge
        try {
            double proportion = oppEdge.getPositionProportion(intercept);
            return proportion;
        } catch (IllegalArgumentException e) {
            throw new PixelOutOfBoundsException("Cannot get the edge proportion for edge " + oppEdge);
        }

    }

    /**
     * Check that v1 is the internal vertex, or the lower perpheral vertex. If
     * not, return new new edge with reversed orientation
     * 
     * @param e
     * @return
     */
    @Override
    public MeshEdge correctEdgeOrientation(MeshEdge e) {
        // Identify and correct the orientation of the edges
        boolean usePeripheral = this.getPeripheralVertexCount() == 2;

        MeshVertex p1 = usePeripheral ? getLowerPeripheralVertex() : getLowerInternalVertex();
        MeshVertex p2 = usePeripheral ? getHigherPeripheralVertex() : getHigherInternalVertex();
        MeshVertex i1 = usePeripheral ? getLowerInternalVertex() : getLowerPeripheralVertex();

        MeshVertex o1 = e.getV1();
        MeshVertex o2 = e.getV2();

        if (o1.equals(p1) && o2.equals(i1)) {
            return e.reverse();
        }

        if (o1.equals(p2) && o2.equals(i1)) {
            return e.reverse();
        }

        if (o1.equals(p2) && o2.equals(p1)) {
            return e.reverse();
        }

        return e;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bmskinner.nma.analysis.mesh.MeshFace#getFaceCoordinate
     * (com.bmskinner.nma.components.generic.IPoint)
     */
    @Override
    public MeshFaceCoordinate getFaceCoordinate(IPoint p) throws PixelOutOfBoundsException {

        if (!contains(p)) {
            throw new PixelOutOfBoundsException("Point is not within face: " + p.toString());
        }

        boolean usePeripheral = this.getPeripheralVertexCount() == 2;

        MeshVertex p1 = usePeripheral ? getLowerPeripheralVertex() : getLowerInternalVertex();
        MeshVertex p2 = usePeripheral ? getHigherPeripheralVertex() : getHigherInternalVertex();
        MeshVertex i1 = usePeripheral ? getLowerInternalVertex() : getLowerPeripheralVertex();

        double p1p = getEdgeProportion(p1, p);
        double p2p = getEdgeProportion(p2, p);
        double i1p = getEdgeProportion(i1, p);

        if (p1p == 0 && p2p == 0 & i1p == 0) {
            LOGGER.warning("Point " + p + " does not have edge proportion calculated");
        }

        return new DefaultMeshFaceCoordinate(p1p, p2p, i1p);

    }
}
