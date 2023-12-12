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
package com.bmskinner.nma.components.cells;

import java.awt.Shape;
import java.util.Objects;
import java.util.logging.Logger;

import org.jdom2.Element;

import com.bmskinner.nma.components.XMLNames;
import com.bmskinner.nma.components.generic.FloatPoint;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.profiles.MissingLandmarkException;
import com.bmskinner.nma.components.profiles.UnprofilableObjectException;
import com.bmskinner.nma.io.XmlSerializable;
import com.bmskinner.nma.logging.Loggable;

import ij.process.FloatPolygon;

/**
 * This describes a consensus shape for a population of cells.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultConsensusNucleus extends DefaultNucleus implements Consensus {

	private static final Logger LOGGER = Logger.getLogger(DefaultConsensusNucleus.class.getName());

	private double xOffset = 0;
	private double yOffset = 0;
	private double rOffset = 0;

	/**
	 * Create from a nucleus. Offsets will be zero.
	 * 
	 * @param n
	 * @throws UnprofilableObjectException
	 * @throws ComponentCreationException
	 */
	public DefaultConsensusNucleus(Nucleus n)
			throws UnprofilableObjectException, ComponentCreationException {
		super(n);
	}

	/**
	 * Create from another consensus. Offsets will be copied
	 * 
	 * @param n
	 * @throws UnprofilableObjectException
	 * @throws ComponentCreationException
	 */
	public DefaultConsensusNucleus(Consensus n)
			throws UnprofilableObjectException, ComponentCreationException {
		super(n);
		xOffset = n.currentOffset().getX();
		yOffset = n.currentOffset().getY();
		rOffset = n.currentRotation();
	}

	/**
	 * Construct from an XML element. Use for unmarshalling. The element should
	 * conform to the specification in {@link XmlSerializable}.
	 * 
	 * @param e the XML element containing the data.
	 */
	public DefaultConsensusNucleus(Element e) throws ComponentCreationException {
		super(e);

		if (e.getChild(XMLNames.XML_OFFSET) != null) {
			xOffset = Double
					.valueOf(e.getChild(XMLNames.XML_OFFSET).getAttributeValue(XMLNames.XML_X));
			yOffset = Double
					.valueOf(e.getChild(XMLNames.XML_OFFSET).getAttributeValue(XMLNames.XML_Y));
			rOffset = Double
					.valueOf(e.getChild(XMLNames.XML_OFFSET).getAttributeValue(XMLNames.XML_R));
		}
	}

	@Override
	public Element toXmlElement() {
		Element e = super.toXmlElement().setName(XMLNames.XML_CONSENSUS_NUCLEUS);

		e.addContent(new Element(XMLNames.XML_OFFSET)
				.setAttribute(XMLNames.XML_X, String.valueOf(xOffset))
				.setAttribute(XMLNames.XML_Y, String.valueOf(yOffset))
				.setAttribute(XMLNames.XML_R, String.valueOf(rOffset)));
		return e;
	}

	@Override
	public void setOffset(double xOffset, double yOffset) {
		this.xOffset = xOffset;
		this.yOffset = yOffset;
	}

	@Override
	public void addRotation(double angle) {
		this.rOffset = angle;
	}

	@Override
	public double currentRotation() {
		return rOffset;
	}

	@Override
	public FloatPolygon toOriginalPolygon() {
		// There is no original position for a consensus
		return toPolygon();
	}

	@Override
	public Shape toOriginalShape() {
		// There is no original position for a consensus
		return toShape();
	}

	@Override
	public Nucleus getOrientedNucleus()
			throws MissingLandmarkException, ComponentCreationException {
		Nucleus n = this.duplicate();
		n.orient();
		n.rotate(rOffset);
		n.offset(xOffset, yOffset);
		return n;
	}

	@Override
	public IPoint currentOffset() {
		return new FloatPoint(xOffset, yOffset);
	}

	@Override
	public Consensus duplicate() {
		try {
			return new DefaultConsensusNucleus(this);
		} catch (UnprofilableObjectException | ComponentCreationException e) {
			LOGGER.log(Loggable.STACK, "Error duplicating consensus", e);
			return null;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString() + "\n");

		sb.append("Offset: " + xOffset + "-" + yOffset + "\n");
		sb.append("Rotation: " + rOffset);
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(rOffset, xOffset, yOffset);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		DefaultConsensusNucleus other = (DefaultConsensusNucleus) obj;
		return Double.doubleToLongBits(rOffset) == Double.doubleToLongBits(other.rOffset)
				&& Double.doubleToLongBits(xOffset) == Double.doubleToLongBits(other.xOffset)
				&& Double.doubleToLongBits(yOffset) == Double.doubleToLongBits(other.yOffset);
	}

}
