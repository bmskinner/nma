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
package com.bmskinner.nuclear_morphology.components.nuclei.sperm;

import java.io.File;
import java.util.UUID;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileIndexFinder.NoDetectedIndexException;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.IProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.generic.UnprofilableObjectException;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.AbstractAsymmetricNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.rules.RuleSet;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.gui.Roi;

/**
 * The standard pig sperm nucleus
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultPigSpermNucleus extends AbstractAsymmetricNucleus {
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.ROOT_LOGGER);

    private static final long serialVersionUID = 1L;
    

    /**
     * Construct with an ROI, a source image and channel, and the original
     * position in the source image. It sets the immutable original centre of
     * mass, and the mutable current centre of mass. It also assigns a random ID
     * to the component.
     * 
     * @param roi the roi of the object
     * @param centerOfMass the original centre of mass of the component
     * @param source the image file the component was found in
     * @param channel the RGB channel the component was found in
     * @param position the bounding position of the component in the original image
     * @param id the id of the component. Only use when deserialising!
     */
    public DefaultPigSpermNucleus(@NonNull Roi roi, @NonNull IPoint centreOfMass, File source, int channel, int[] position, int number, @NonNull UUID id) {
        super(roi, centreOfMass, source, channel, position, number, id);
    }

    /**
     * Construct with an ROI, a source image and channel, and the original
     * position in the source image
     * 
     * @param roi
     * @param f
     * @param channel
     * @param position
     * @param centreOfMass
     */
    public DefaultPigSpermNucleus(Roi roi, IPoint centreOfMass, File f, int channel, int[] position, int number) {
        super(roi, centreOfMass, f, channel, position, number);
    }

    protected DefaultPigSpermNucleus(Nucleus n) throws UnprofilableObjectException {
        super(n);
    }

    @Override
    public Nucleus duplicate() {
        try {
            return new DefaultPigSpermNucleus(this);
        } catch (UnprofilableObjectException e) {
            LOGGER.log(Loggable.STACK, "Error duplicating nucleus", e);
        }
        return null;
    }

    @Override
    public void findPointsAroundBorder() throws ComponentCreationException {

        try {
            RuleSet rpSet = RuleSet.pigSpermRPRuleSet();
            IProfile p = this.getProfile(rpSet.getType());
            ProfileIndexFinder f = new ProfileIndexFinder();
            int rpIndex = f.identifyIndex(p, rpSet);

            if (rpIndex == -1) {
                LOGGER.finest( "RP index was not found in nucleus, setting to zero in profile");
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
            if (!this.isProfileOrientationOK() && canReverse) {
                this.reverse(); // reverses all profiles, border array and
                                // tagged points

                // the number of border points can change when reversing
                // due to float interpolation from different starting positions
                // so do the whole thing again
                initialise(this.getWindowProportion(ProfileType.ANGLE));
                canReverse = false;
                findPointsAroundBorder();
            }

        } catch (UnavailableProfileTypeException e) {
            LOGGER.log(Loggable.STACK, "Error getting profile type", e);
        } catch (NoDetectedIndexException e) {
            LOGGER.fine("Unable to detect RP in nucleus");
            setBorderTag(Tag.REFERENCE_POINT, 0);
            setBorderTag(Tag.ORIENTATION_POINT, 0);
        }

    }

	@Override
	protected Nucleus createVerticallyRotatedNucleus() {
		// TODO Auto-generated method stub
		return super.getVerticallyRotatedNucleus();
	}
}
