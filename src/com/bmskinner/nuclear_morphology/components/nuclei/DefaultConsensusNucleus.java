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
package com.bmskinner.nuclear_morphology.components.nuclei;

import java.awt.Rectangle;
import java.awt.Shape;
import java.io.IOException;
import java.io.ObjectInputStream;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileCreator;
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
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultConsensusNucleus extends DefaultNucleus {
	
	private static final long serialVersionUID = 1L;
	
	private NucleusType type;
//	private IPoint originalCoM; // store to allow repositioning on load
	
	public DefaultConsensusNucleus(Nucleus n, NucleusType type) throws UnprofilableObjectException {
		
		super(n);
		this.type = type;
//		this.originalCoM = n.getCentreOfMass();
		
		// At this point the new consensus has created its border list
		// based on the int points from  the template nucleus.
		
		// The border list is no longer at zero.
		
		
		// Update the int points as well, so the nucleus does not 'snap back'
		// from 0, 0 after loading from file. Then recreate the border list
		
		// The centre of mass will match the template nucleus, since this is copied directly.
		
		if(  n instanceof DefaultConsensusNucleus ){
			
			// If a consensus nucleus is used as the template, the CoM is already at zero, and 
			// should not be moved again, otherwise the border list will be dragged out of position.
			
			// Calculate the difference between the template and the new nucleus
			
			double minX = n.getMinX();
			double minY = n.getMinY();
			
			double  diffX = minX - this.getMinX();
			double  diffY = minY - this.getMinY();
			
			// Apply the offset to the border list
			this.offset(diffX, diffY);

		} else {
			
			this.moveCentreOfMass(IPoint.makeNew(0,0));			
		}

		finest("Constructed consensus nucleus from "+n.getNameAndNumber());
	}
	
	public NucleusType getType(){
		return this.type;
	}
	
	@Override
	public int[] getPosition(){
		
		Rectangle bounds = createPolygon().getBounds();
		int newWidth  = (int) bounds.getWidth();
		int newHeight = (int) bounds.getHeight();
		int newX      = (int) this.getMinX();
		int newY      = (int) this.getMinY();

		int[] newPosition = { newX, newY, newWidth, newHeight };
		return  newPosition;
	}
	
	@Override
	public void calculateProfiles() {
		
		/*
		 * The CurveRefolder currently only uses the angle profile
		 * so ignore the others to speed refolding
		 */
		ProfileCreator creator = new ProfileCreator(this);

		ISegmentedProfile profile = creator.createProfile(ProfileType.ANGLE);
				
		profileMap.put(ProfileType.ANGLE, profile);

	}
	
	@Override
	public FloatPolygon createOriginalPolygon(){
		
		// There is no original position for a consensus
		return this.createPolygon();
	}
	
	@Override
	public Shape toOriginalShape(){
		
		// There is no original position for a consensus
		return this.toShape();
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {

		
		in.defaultReadObject();
		
		// After loading, the border list has been constructed wrt to the
		// int array, and so is offset from the 0,0 origin.
		
		// Reposition the border list so the CoM is at the origin,
				
		// Note - the CoM has been saved as 0,0. Use the original CoM for positioning
				
//		double  diffX = getCentreOfMass().getX() - getOriginalCentreOfMass().getX();
//		double  diffY = getCentreOfMass().getY() - getOriginalCentreOfMass().getY();
//		
//		// Apply the offset to the border list
//		this.offset(diffX, diffY);
////		this.setCentreOfMassDirectly(IPoint.makeNew(0,0));
		
		this.alignVertically();
		
		// Check that the horizontal orientation is correct
		
		try {
		
			if(type.equals(NucleusType.RODENT_SPERM)){
				if(getBorderTag(Tag.REFERENCE_POINT).getX()>0){
					flipXAroundPoint(getCentreOfMass());
				}
			}
		} catch (UnavailableBorderTagException e){
			fine("Cannot get border tag", e);
		}
		
		

	}

}
