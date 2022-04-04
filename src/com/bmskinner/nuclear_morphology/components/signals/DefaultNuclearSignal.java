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
package com.bmskinner.nuclear_morphology.components.signals;

import java.io.File;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Element;

import com.bmskinner.nuclear_morphology.components.cells.DefaultCellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;

import ij.gui.Roi;

/**
 * An implementation of {@link INuclearSignal}.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultNuclearSignal extends DefaultCellularComponent implements INuclearSignal {
	
	private static final String XML_SIGNAL = "Signal";

	private static final String XML_CLOSEST_BORDER = "ClosestBorder";

    private int closestNuclearBorderPoint;

    public DefaultNuclearSignal(@NonNull Roi roi, @NonNull IPoint centreOfMass, @NonNull File f, int channel, int x, int y, @NonNull UUID id) {
        super(roi, centreOfMass, f, channel, x, y, id);
    }
    
    public DefaultNuclearSignal(@NonNull Roi roi, @NonNull IPoint centreOfMass, @NonNull File f, int channel, int x, int y) {
        super(roi, centreOfMass, f, channel, x, y);
    }

    /**
     * Create a copy of the given signal
     * 
     * @param n
     */
    private DefaultNuclearSignal(@NonNull INuclearSignal n) {
        super(n);
        this.closestNuclearBorderPoint = n.getClosestBorderPoint();
    }
    
    
    /**
     * Construct from an XML element. Use for 
     * unmarshalling. The element should conform
     * to the specification in {@link XmlSerializable}.
     * @param e the XML element containing the data.
     */
    public DefaultNuclearSignal(Element e) {
    	super(e);
    	closestNuclearBorderPoint = Integer.valueOf(e.getChildText(XML_CLOSEST_BORDER));
    }
    
    @Override
    public INuclearSignal duplicate() {
    	return new DefaultNuclearSignal(this);
    }

    @Override
    public int getClosestBorderPoint() {
        return this.closestNuclearBorderPoint;
    }

    @Override
    public void setClosestBorderPoint(int p) {
        this.closestNuclearBorderPoint = p;
    }

	@Override
	public Element toXmlElement() {
		Element e = super.toXmlElement().setName(XML_SIGNAL);
		e.addContent(new Element(XML_CLOSEST_BORDER).setText(String.valueOf(closestNuclearBorderPoint)));
		return e;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(closestNuclearBorderPoint);
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
		DefaultNuclearSignal other = (DefaultNuclearSignal) obj;
		return closestNuclearBorderPoint == other.closestNuclearBorderPoint;
	}
}
