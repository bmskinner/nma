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
package com.bmskinner.nuclear_morphology.components.nuclei;

import java.awt.Rectangle;
import java.awt.Shape;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileCreator;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.profiles.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileType;
import com.bmskinner.nuclear_morphology.components.profiles.UnprofilableObjectException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.process.FloatPolygon;

/**
 * This describes a consensus shape for a population of cells.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultConsensusNucleus extends AbstractAsymmetricNucleus implements Consensus<Nucleus>  {
	
	private static final Logger LOGGER = Logger.getLogger(DefaultConsensusNucleus.class.getName());

    private static final long serialVersionUID = 1L;
    
    private double xOffset = 0;
    private double yOffset = 0;
    private double rotOffset = 0;

    public DefaultConsensusNucleus(Nucleus n) throws UnprofilableObjectException {

        super(n);
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
	public void offset(double xOffset, double yOffset) {
    	this.xOffset = xOffset;
    	this.yOffset = yOffset;
    }
    
    @Override
	public void addRotation(double angle) {
    	this.rotOffset = angle;
    }
    
    @Override
	public double currentRotation() {
    	return rotOffset;
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

    	return true;
    }
    
    @Override
	protected Nucleus createVerticallyRotatedNucleus() {
    	Nucleus n = super.getVerticallyRotatedNucleus();
    	
    	
    	try {
    		if (n.getBorderPoint(Landmark.REFERENCE_POINT).getX() > n.getCentreOfMass().getX())
    			n.flipHorizontal();
    	} catch (UnavailableBorderTagException e) {
    		LOGGER.log(Loggable.STACK, "Cannot get RP from vertical nucleus; returning default orientation", e);
    	}
    	n.rotate(rotOffset);
    	n.offset(xOffset, yOffset);
    	return n;
    }
    
    @Override
    public Nucleus getVerticallyRotatedNucleus() {
    	return createVerticallyRotatedNucleus();
    }
    
    @Override
    public int hashCode() {
    	final int prime = 31;
        int result = super.hashCode();
        return result;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {

    	in.defaultReadObject();
    	alignVertically();
    }

	@Override
	public IPoint currentOffset() {
		return IPoint.makeNew(xOffset, yOffset);
	}
	
	@Override
	public Consensus<Nucleus> duplicateConsensus() {
		try {
			return new DefaultConsensusNucleus(this);
		} catch (UnprofilableObjectException e) {
			LOGGER.log(Loggable.STACK, "Error duplicating consensus", e);
			return null;
		}
	}
	
	@Override
	public Nucleus component() {
		return this;
	}

}
