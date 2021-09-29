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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.generic.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.UnprofilableObjectException;

import ij.gui.Roi;

/**
 * The class of non-round nuclei from which all other assymetric nuclei derive
 * 
 * @author ben
 * @since 1.13.3
 */
public abstract class AbstractAsymmetricNucleus extends DefaultNucleus {

    private static final long serialVersionUID = 1L;

    // points considered to be sperm tails before filtering
    private transient List<IBorderPoint> tailEstimatePoints = new ArrayList<>(3);
    
   // Does the orientation of the nucleus have RP clockwise to the CoM. Only applicable to hooked sperm
    protected transient boolean clockwiseRP = true;
    protected transient boolean orientationChecked = false;
    
    /**
     * Construct with an ROI, a source image and channel, and the original
     * position in the source image. It sets the immutable original centre of
     * mass, and the mutable current centre of mass. It also assigns a random ID
     * to the component.
     * 
     * @param roi the roi of the object
     * @param centerOfMass the original centre of mass of the component
     * @param sourceFile the image file the component was found in
     * @param channel the RGB channel the component was found in
     * @param position the bounding position of the component in the original image
     * @param number the number of the nucleus in the source image
     * @param id the id of the component. Only use when deserialising!
     */
    public AbstractAsymmetricNucleus(@NonNull Roi roi, @NonNull IPoint centreOfMass, File sourceFile, int channel, int[] position, int number, @NonNull UUID id) {
        super(roi, centreOfMass, sourceFile, channel, position, number, id);
    }

    /**
     * Construct with an ROI, a source image and channel, and the original
     * position in the source image
     * 
     * @param roi the roi of the object
     * @param centreOfMass the original centre of mass of the component
     * @param sourceFile the image file the component was found in
     * @param channel the RGB channel the component was found in
     * @param position the bounding position of the component in the original image
     * @param number the number of the nucleus in the source image
     */
    public AbstractAsymmetricNucleus(@NonNull Roi roi, @NonNull IPoint centreOfMass, File sourceFile, int channel, int[] position, int number) {
        super(roi, centreOfMass, sourceFile, channel, position, number);
    }

    /**
     * Construct from an existing nucleus
     * @param n the nucleus to use as a template
     * @throws UnprofilableObjectException
     */
    protected AbstractAsymmetricNucleus(Nucleus n) throws UnprofilableObjectException {
        super(n);
    }

    protected void addTailEstimatePosition(IBorderPoint p) {
        tailEstimatePoints.add(p);
    }

    @Override
    public boolean isClockwiseRP() {
        return clockwiseRP;
    }
    
    @Override
    public void updateVerticallyRotatedNucleus() {
    	orientationChecked = false;
    	super.updateVerticallyRotatedNucleus();
    }
        
    protected abstract Nucleus createVerticallyRotatedNucleus();
        
    
    @Override
   	public void setBorderTag(@NonNull Landmark tag, int i) {
    	super.setBorderTag(tag, i);
    	orientationChecked = false;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        tailEstimatePoints = new ArrayList<>(0);
        clockwiseRP = true;
        orientationChecked = false;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
    }
}
