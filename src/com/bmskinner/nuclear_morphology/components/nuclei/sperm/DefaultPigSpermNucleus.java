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
package com.bmskinner.nuclear_morphology.components.nuclei.sperm;

import java.io.File;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder;
import com.bmskinner.nuclear_morphology.analysis.profiles.RuleSet;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnprofilableObjectException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.AbstractAsymmetricNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

import ij.gui.Roi;

/**
 * The standard pig sperm nucleus
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultPigSpermNucleus extends AbstractAsymmetricNucleus {

	private static final long serialVersionUID = 1L;

	/**
	 * Construct with an ROI, a source image and channel, and the original position in the source image
	 * @param roi
	 * @param f
	 * @param channel
	 * @param position
	 * @param centreOfMass
	 */
	public DefaultPigSpermNucleus(Roi roi, IPoint centreOfMass, File f, int channel, int[] position, int number){
		super(roi, centreOfMass , f, channel, position, number);
	}

	protected DefaultPigSpermNucleus(Nucleus n) throws UnprofilableObjectException {
		super(n);
	}

	@Override
	public Nucleus duplicate(){			
		try {
			return new DefaultPigSpermNucleus(this);
		} catch (UnprofilableObjectException e) {
			stack("Error duplicating nucleus", e);
		}
		return null;
	}
	
	@Override
    public void findPointsAroundBorder() {
    	

		try {
			RuleSet rpSet = RuleSet.pigSpermRPRuleSet();
			IProfile p     = this.getProfile(rpSet.getType());
			ProfileIndexFinder f = new ProfileIndexFinder();
			int rpIndex = f.identifyIndex(p, rpSet);

			if( rpIndex== -1 ){
				finest("RP index was not found in nucleus, setting to zero in profile");
				rpIndex = 0;
			}

			setBorderTag(Tag.REFERENCE_POINT, rpIndex);

			/*
			 * The OP is the same as the RP in pigs
			 */
			setBorderTag(Tag.ORIENTATION_POINT, rpIndex);

			/*
			 * The IP is opposite the OP
			 */
			IBorderPoint op = this.getBorderPoint(rpIndex);
			int ipIndex = getBorderIndex(this.findOppositeBorder(op));
			setBorderTag(Tag.INTERSECTION_POINT, ipIndex);

			// decide if the profile is right or left handed; flip if needed
			if(!this.isProfileOrientationOK() && canReverse){
				this.reverse(); // reverses all profiles, border array and tagged points

				// the number of border points can change when reversing
				// due to float interpolation from different starting positions
				// so do the whole thing again
				initialise(this.getWindowProportion(ProfileType.ANGLE));
				canReverse = false;
				findPointsAroundBorder();
			} 

		} catch(UnavailableProfileTypeException e){
			stack("Error getting profile type", e);
		}

	}
}
