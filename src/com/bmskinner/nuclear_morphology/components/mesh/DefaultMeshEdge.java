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
package com.bmskinner.nuclear_morphology.components.mesh;

import java.awt.geom.Line2D;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.measure.DoubleEquation;
import com.bmskinner.nuclear_morphology.components.measure.LineEquation;
import com.bmskinner.nuclear_morphology.stats.Stats;

public class DefaultMeshEdge implements MeshEdge {
    final private MeshVertex v1;
    final private MeshVertex v2;
    private double           value;

    /**
     * Create from two vertices and assign a value to the edge
     * 
     * @param v1 the first vertex
     * @param v2 the second vertex
     * @param v the weight of the edge
     */
    public DefaultMeshEdge(final MeshVertex v1, final MeshVertex v2, final double v) {

        if (v1 == v2) {
            throw new IllegalArgumentException("Vertices are identical in edge constructor");
        }
        this.v1 = v1;
        this.v2 = v2;
        this.value = v;

        v1.addEdge(this);
        v2.addEdge(this);
    }

    /**
     * Duplicate the edge
     * 
     * @param e
     */
    public DefaultMeshEdge(final MeshEdge e) {
        this.v1 = new DefaultMeshVertex(e.getV1());
        this.v2 = new DefaultMeshVertex(e.getV2());

        this.value = e.getValue();
    }

    @Override
    public MeshVertex getV1() {
        return v1;
    }

    @Override
    public MeshVertex getV2() {
        return v2;
    }

    @Override
    public MeshEdge reverse() {
        return new DefaultMeshEdge(new DefaultMeshVertex(v2), new DefaultMeshVertex(v1), value);
    }

    @Override
    public void setValue(double d) {
        this.value = d;
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
    public double getLength() {
        return v1.getLengthTo(v2);
    }

    @Override
    public IPoint getMidpoint() {
        LineEquation eq = new DoubleEquation(v1.getPosition(), v2.getPosition());
        if (v1.getPosition().getX() < v2.getPosition().getX()) {
            return eq.getPointOnLine(v1.getPosition(), getLength() / 2);
        }
		return eq.getPointOnLine(v1.getPosition(), -(getLength() / 2));
    }

    @Override
    public boolean isLongerThan(MeshEdge e) {
        return getLength() > e.getLength();
    }

    @Override
    public boolean overlaps(MeshEdge e) {
        return this.containsVertex(e.getV1()) && this.containsVertex(e.getV2());
    }

    @Override
    public boolean crosses(MeshEdge e) {

        Line2D line1 = new Line2D.Double(v1.getPosition().toPoint2D(), v2.getPosition().toPoint2D());
        Line2D line2 = new Line2D.Double(e.getV1().getPosition().toPoint2D(), e.getV2().getPosition().toPoint2D());

        if (line1.intersectsLine(line2)) {

            if (sharesEndpoint(e)) {
                return false;
            }
			return true;
        }
        return false;
    }

    @Override
    public boolean sharesEndpoint(MeshEdge e) {
        return this.containsVertex(e.getV1()) || this.containsVertex(e.getV2());
    }

    @Override
    public boolean containsVertex(MeshVertex v) {
        return v1.overlaps(v) || v2.overlaps(v);
    }

    @Override
    public boolean equals(MeshEdge e) {
        if (this == e) 
            return true;
        return this.overlaps(e);
    }

    @Override
    public IPoint getProportionalPosition(double d) {
        return DoubleEquation.getProportionalDistance(v1.getPosition(), v2.getPosition(), d);
    }

    @Override
    public double getPositionProportion(IPoint p) {

        IPoint upperX, lowerX, upperY, lowerY;

        // Establish the bounds for the edge
        if (v1.getPosition().getX() < v2.getPosition().getX()) {

            upperX = v2.getPosition();
            lowerX = v1.getPosition();
        } else {
            upperX = v1.getPosition();
            lowerX = v2.getPosition();
        }

        // Establish the bounds for the edge
        if (v1.getPosition().getY() < v2.getPosition().getY()) {

            upperY = v2.getPosition();
            lowerY = v1.getPosition();
        } else {
            upperY = v1.getPosition();
            lowerY = v2.getPosition();
        }

        // Ensure the point lies within the X bounds of the line.
        // Avoid using the equation in case of float / double precision issues
        if (p.getX() < lowerX.getX() || p.getX() > upperX.getX()) {
            throw new IllegalArgumentException("Point " + p + " does not fit on edge X: " + lowerX + " - " + upperX);

        }

        // Ensure the point lies within the Y bounds of the line.
        // Avoid using the equation in case of float / double precision issues
        if (p.getY() < lowerY.getY() || p.getY() > upperY.getY()) {
            throw new IllegalArgumentException("Point " + p + " does not fit on edge Y: " + lowerY + " - " + upperY);

        }

        // LineEquation eq = new FloatEquation(v1.getPosition(),
        // v2.getPosition());

        // if(eq.isOnLine(p)){

        double totalLength = v1.getLengthTo(v2);
        double length = p.getLengthTo(v1.getPosition());

        return length / totalLength;

        // } else {
        // throw new IllegalArgumentException("Point "+p+" does not lie on the
        // edge line "+eq.toString()+" from "+v1.toString()+" -
        // "+v2.toString());
        // }

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((v1 == null) ? 0 : v1.hashCode());
        result = prime * result + ((v2 == null) ? 0 : v2.hashCode());
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
        DefaultMeshEdge other = (DefaultMeshEdge) obj;
        if (v1 == null) {
            if (other.v1 != null)
                return false;
        } else if (!v1.equals(other.v1) || !v1.equals(other.v2))
            return false;
        if (v2 == null) {
            if (other.v2 != null)
                return false;
        } else if (!v2.equals(other.v2) || !v2.equals(other.v1))
            return false;
        return true;
    }


    @Override
    public String getName() {
        return v1.getName() + " - " + v2.getName();
    }

    @Override
	public String toString() {
        return v1.getName() + " - " + v2.getName() + " : " + getLength();
    }

	@Override
	public boolean isPeripheral() {
		return v1.isPeripheral() && v2.isPeripheral();
	}

}
