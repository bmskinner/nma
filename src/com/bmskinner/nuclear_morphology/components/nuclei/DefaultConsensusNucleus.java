/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.components.nuclei;

import java.awt.Rectangle;
import java.awt.Shape;
import java.io.IOException;
import java.io.ObjectInputStream;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileCreator;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnprofilableObjectException;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;

import ij.process.FloatPolygon;

/**
 * This describes a consensus shape for a population of cells.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultConsensusNucleus extends DefaultNucleus {

    private static final long serialVersionUID = 1L;

    private NucleusType type;

    public DefaultConsensusNucleus(Nucleus n, NucleusType type) throws UnprofilableObjectException {

        super(n);
        this.type = type;
        // this.originalCoM = n.getCentreOfMass();

        // At this point the new consensus has created its border list
        // based on the int points from the template nucleus.

        // The border list is no longer at zero.

        // Update the int points as well, so the nucleus does not 'snap back'
        // from 0, 0 after loading from file. Then recreate the border list

        // The centre of mass will match the template nucleus, since this is
        // copied directly.

        if (n instanceof DefaultConsensusNucleus) {

            // If a consensus nucleus is used as the template, the CoM is
            // already at zero, and
            // should not be moved again, otherwise the border list will be
            // dragged out of position.

            // Calculate the difference between the template and the new nucleus

            double minX = n.getMinX();
            double minY = n.getMinY();

            double diffX = minX - this.getMinX();
            double diffY = minY - this.getMinY();

            // Apply the offset to the border list
            this.offset(diffX, diffY);

        } else {
            moveCentreOfMass(IPoint.makeNew(0, 0));
        }
    }
    
    public NucleusType getType() {
        return type;
    }

    @Override
    public int[] getPosition() {

        Rectangle bounds = toPolygon().getBounds();
        int newWidth = (int) bounds.getWidth();
        int newHeight = (int) bounds.getHeight();
        int newX = (int) this.getMinX();
        int newY = (int) this.getMinY();

        int[] newPosition = { newX, newY, newWidth, newHeight };
        return newPosition;
    }

    @Override
    public void calculateProfiles() throws ProfileException {

        /*
         * The CurveRefolder currently only uses the angle profile so ignore the
         * others to speed refolding
         */
        ProfileCreator creator = new ProfileCreator(this);

        ISegmentedProfile profile = creator.createProfile(ProfileType.ANGLE);

        assignProfile(ProfileType.ANGLE, profile);

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
    public boolean equals(Object obj) {
    	if(!super.equals(obj))
    		return false;
    	if(!(obj instanceof DefaultConsensusNucleus))
    		return false;
    	DefaultConsensusNucleus other = (DefaultConsensusNucleus)obj;
    	if(!type.equals(other.type))
    		return false;
    	return true;
    }
    
    @Override
    public int hashCode() {
    	final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {

        in.defaultReadObject();
        try {
        	alignVertically();
        	this.getVerticallyRotatedNucleus().alignVertically();
        	if (type.equals(NucleusType.RODENT_SPERM) && getBorderPoint(Tag.REFERENCE_POINT).getX() > 0)
        			flipXAroundPoint(getCentreOfMass());
        } catch (UnavailableBorderTagException e1) {
        	fine("Cannot get border tag", e1);
        }


    }

}
