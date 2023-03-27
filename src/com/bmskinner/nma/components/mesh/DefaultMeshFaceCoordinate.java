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

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.DoubleEquation;
import com.bmskinner.nma.components.measure.LineEquation;

/**
 * An implementation of the MeshFaceCoordinate, which stores a pixel position
 * within a MeshFace.
 * 
 * @author bms41
 *
 */
public class DefaultMeshFaceCoordinate implements MeshFaceCoordinate {

	// edge opposite peripheral vertex with lower number
	// Value runs from 0 at internal vertex to 1 at peripheral vertex
	final private double p1;

	// edge opposite peripheral vertex with higher number
	// Value runs from 0 at internal vertex to 1 at peripheral vertex
	final private double p2;

	// edge opposite internal vertex
	// Value runs from 0 at peripheral vertex with lower number to 1 at
	// peripheral vertex with higher number
	final private double i1;

	/**
	 * Constructor. The values should be between 0 and 1 and describe the proportion
	 * along the given edge.
	 * 
	 * @param p1 edge opposite peripheral vertex with lower number
	 * @param p2 edge opposite peripheral vertex with higher number
	 * @param i1 edge opposite internal vertex
	 */
	public DefaultMeshFaceCoordinate(final double p1, final double p2, final double i1) {

		if (p1 > 1 || p2 > 1 || i1 > 1) {
			throw new IllegalArgumentException("Coordinates must be less than 1");
		}

		if (p1 < 0 || p2 < 0 || i1 < 0) {
			throw new IllegalArgumentException("Coordinates must be greater than or equal to 0");
		}

		this.p1 = p1;
		this.p2 = p2;
		this.i1 = i1;
	}

	/**
	 * Convert the face coordinate into the cartesian coordinates in the given face
	 * 
	 * @param face
	 * @return
	 */
	@Override
	public IPoint getCartesianCoordinate(@NonNull final MeshFace face) {

		if (face == null)
			throw new IllegalArgumentException("Face is null when getting cartesian coordinate");

		// Identify the vertices
		boolean usePeripheral = face.getPeripheralVertexCount() == 2;

		MeshVertex p1 = usePeripheral ? face.getLowerPeripheralVertex()
				: face.getLowerInternalVertex();
		MeshVertex p2 = usePeripheral ? face.getHigherPeripheralVertex()
				: face.getHigherInternalVertex();
		MeshVertex i1 = usePeripheral ? face.getLowerInternalVertex()
				: face.getLowerPeripheralVertex();

		// Identify the edges
		MeshEdge i1_p1 = i1.getEdgeTo(p1);
		MeshEdge i1_p2 = i1.getEdgeTo(p2);
		MeshEdge p1_p2 = p1.getEdgeTo(p2);

		// Identify and correct the orientation of the edges
		i1_p1 = face.correctEdgeOrientation(i1_p1);
		i1_p2 = face.correctEdgeOrientation(i1_p2);
		p1_p2 = face.correctEdgeOrientation(p1_p2);

		// Draw lines
		IPoint i1_p1_prop = i1_p1.getProportionalPosition(this.p2);

		LineEquation eq1 = new DoubleEquation(p2.getPosition(), i1_p1_prop);

		IPoint i1_p2_prop = i1_p2.getProportionalPosition(this.p1);

		LineEquation eq2 = new DoubleEquation(p1.getPosition(), i1_p2_prop);

		// Find intersection
		IPoint position = eq1.getIntercept(eq2);

		// Return at point
		return position;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;

		long temp;
		temp = Double.doubleToLongBits(i1);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(p1);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(p2);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		DefaultMeshFaceCoordinate other = (DefaultMeshFaceCoordinate) obj;

		if (Double.doubleToLongBits(i1) != Double.doubleToLongBits(other.i1))
			return false;
		if (Double.doubleToLongBits(p1) != Double.doubleToLongBits(other.p1))
			return false;
		if (Double.doubleToLongBits(p2) != Double.doubleToLongBits(other.p2))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return i1 + " : " + p1 + " : " + p2;
	}

}
