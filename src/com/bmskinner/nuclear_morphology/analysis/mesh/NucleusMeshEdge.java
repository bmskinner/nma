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

import java.awt.geom.Line2D;

import com.bmskinner.nuclear_morphology.components.generic.DoubleEquation;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.LineEquation;
import com.bmskinner.nuclear_morphology.stats.Stats;

public class NucleusMeshEdge implements MeshEdge {
    final private MeshVertex v1;
    final private MeshVertex v2;
    private double           value;

    /**
     * Create from two vertices and assign a value to the edge
     * 
     * @param v1
     *            the first vertex
     * @param v2
     *            the second vertex
     * @param v
     *            the value
     */
    public NucleusMeshEdge(final MeshVertex v1, final MeshVertex v2, final double v) {

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
    public NucleusMeshEdge(final MeshEdge e) {
        this.v1 = new NucleusMeshVertex(e.getV1());
        this.v2 = new NucleusMeshVertex(e.getV2());

        this.value = e.getValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge#getV1()
     */
    @Override
    public MeshVertex getV1() {
        return v1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge#getV2()
     */
    @Override
    public MeshVertex getV2() {
        return v2;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge#reverse()
     */
    @Override
    public MeshEdge reverse() {
        return new NucleusMeshEdge(new NucleusMeshVertex(v2), new NucleusMeshVertex(v1), value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge#setValue(double)
     */
    @Override
    public void setValue(double d) {
        this.value = d;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge#getRatio()
     */
    @Override
    public double getValue() {
        return value;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge#getLog2Ratio()
     */
    @Override
    public double getLog2Ratio() {
        return Stats.calculateLog2Ratio(value);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge#getLength()
     */
    @Override
    public double getLength() {
        return v1.getLengthTo(v2);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge#getMidpoint()
     */
    @Override
    public IPoint getMidpoint() {
        LineEquation eq = new DoubleEquation(v1.getPosition(), v2.getPosition());
        if (v1.getPosition().getX() < v2.getPosition().getX()) {
            return eq.getPointOnLine(v1.getPosition(), getLength() / 2);
        } else {
            return eq.getPointOnLine(v1.getPosition(), -(getLength() / 2));
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge#isLongerThan(com.
     * bmskinner.nuclear_morphology.analysis.mesh.MeshEdge)
     */
    @Override
    public boolean isLongerThan(MeshEdge e) {
        return getLength() > e.getLength();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge#overlaps(com.
     * bmskinner.nuclear_morphology.analysis.mesh.NucleusMeshEdge)
     */
    @Override
    public boolean overlaps(MeshEdge e) {
        return this.containsVertex(e.getV1()) && this.containsVertex(e.getV2());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge#crosses(com.
     * bmskinner.nuclear_morphology.analysis.mesh.NucleusMeshEdge)
     */
    @Override
    public boolean crosses(MeshEdge e) {

        Line2D line1 = new Line2D.Double(v1.getPosition().toPoint2D(), v2.getPosition().toPoint2D());
        Line2D line2 = new Line2D.Double(e.getV1().getPosition().toPoint2D(), e.getV2().getPosition().toPoint2D());

        if (line1.intersectsLine(line2)) {

            if (sharesEndpoint(e)) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge#sharesEndpoint(
     * com.bmskinner.nuclear_morphology.analysis.mesh.NucleusMeshEdge)
     */
    @Override
    public boolean sharesEndpoint(MeshEdge e) {
        return this.containsVertex(e.getV1()) || this.containsVertex(e.getV2());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge#containsVertex(
     * com.bmskinner.nuclear_morphology.analysis.mesh.NucleusMeshVertex)
     */
    @Override
    public boolean containsVertex(MeshVertex v) {
        return v1.overlaps(v) || v2.overlaps(v);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge#equals(com.
     * bmskinner.nuclear_morphology.analysis.mesh.NucleusMeshEdge)
     */
    @Override
    public boolean equals(MeshEdge e) {
        if (this == e) {
            return true;
        }

        return this.overlaps(e);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge#
     * getProportionalPosition(double)
     */
    @Override
    public IPoint getProportionalPosition(double d) {

        return DoubleEquation.getProportionalDistance(v1.getPosition(), v2.getPosition(), d);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge#
     * getPositionProportion(com.bmskinner.nuclear_morphology.components.generic
     * .IPoint)
     */
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

    /*
     * (non-Javadoc)
     * 
     * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((v1 == null) ? 0 : v1.hashCode());
        result = prime * result + ((v2 == null) ? 0 : v2.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge#equals(java.lang.
     * Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NucleusMeshEdge other = (NucleusMeshEdge) obj;
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

    /*
     * (non-Javadoc)
     * 
     * @see com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge#getName()
     */
    @Override
    public String getName() {
        return v1.getName() + " - " + v2.getName();
    }

    public String toString() {
        return v1.getName() + " - " + v2.getName() + " : " + getLength();
    }

}
