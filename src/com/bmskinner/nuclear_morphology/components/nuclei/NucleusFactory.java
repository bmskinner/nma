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
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.ComponentFactory;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.sperm.DefaultPigSpermNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.sperm.DefaultRodentSpermNucleus;

import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;

/**
 * Constructs nuclei for an image. Tracks the number of nuclei created.
 * 
 * @author ben
 *
 */
public class NucleusFactory implements ComponentFactory<Nucleus> {

    private int nucleusCount = 0; // store the number of nuclei  created by this factory
    private final NucleusType type;

    /**
     * Create a factory for nuclei of the given type
     * 
     * @param imageFile
     * @param nucleusType
     */
    public NucleusFactory(@NonNull NucleusType nucleusType) {

        if (nucleusType == null)
            throw new IllegalArgumentException("Type cannot be null in nucleus factory");
        type = nucleusType;
    }

    /**
     * Create a nucleus from the given list of points
     * 
     * @param points the border points of the nucleus
     * @param imageFile the image file the nucleus came from
     * @param channel the image channel of the nucleus
     * @param centreOfMass the centre of mass of the nucleus
     * @return a new nucleus of the factory NucleusType
     * @throws ComponentCreationException
     */
    public Nucleus buildInstance(@NonNull List<IPoint> points, File imageFile, int channel, @NonNull IPoint centreOfMass)
            throws ComponentCreationException {
    	if(points.size()<3)
    		throw new ComponentCreationException("Cannot create a nucleus with a border list of only "+points.size()+" points");
    	
        Roi roi = makRoi(points);
        
        Rectangle bounds = roi.getBounds();

        int[] original = { (int) roi.getXBase(), (int) roi.getYBase(), (int) bounds.getWidth(),
                (int) bounds.getHeight() };
        return buildInstance(roi, imageFile, channel, original, centreOfMass);
    }
    
    public Nucleus buildInstance(@NonNull List<IPoint> points, File imageFile, int channel, @NonNull IPoint centreOfMass, @NonNull UUID id)
            throws ComponentCreationException {
    	if(points.size()<3)
    		throw new ComponentCreationException("Cannot create a nucleus with a border list of only "+points.size()+" points");
    	
        Roi roi = makRoi(points);
        
        Rectangle bounds = roi.getBounds();

        int[] original = { (int) roi.getXBase(), (int) roi.getYBase(), (int) bounds.getWidth(),
                (int) bounds.getHeight() };
        return buildInstance(roi, imageFile, channel, original, centreOfMass, id);
    }

    private Roi makRoi(List<IPoint> list) {
        float[] xpoints = new float[list.size()];
        float[] ypoints = new float[list.size()];

        for (int i = 0; i < list.size(); i++) {
            IPoint p = list.get(i);
            xpoints[i] = (float) p.getX();
            ypoints[i] = (float) p.getY();
        }

        // If the points are closer than 1 pixel, the float polygon smoothing
        // during object creation may disrupt the border. Ensure the spacing
        // is corrected to something larger
        Roi roi = new PolygonRoi(xpoints, ypoints, Roi.POLYGON);
        FloatPolygon smoothed = roi.getInterpolatedPolygon(2, false);
        return new PolygonRoi(smoothed.xpoints, smoothed.ypoints, Roi.POLYGON);
    }
    
    @Override
    public Nucleus buildInstance(@NonNull Roi roi, File imageFile, int channel, int[] originalPosition, @NonNull IPoint centreOfMass)
            throws ComponentCreationException {
    	if (roi == null)
    		throw new IllegalArgumentException("Roi cannot be null in nucleus factory");
        if (centreOfMass == null)
            throw new IllegalArgumentException("Centre of mass cannot be null in nucleus factory");
        
        Nucleus n = null;

        switch(type) {
        	case RODENT_SPERM: n = new DefaultRodentSpermNucleus(roi, centreOfMass, imageFile, channel, originalPosition,
                    nucleusCount); break;
        	case PIG_SPERM: n = new DefaultPigSpermNucleus(roi, centreOfMass, imageFile, channel, originalPosition,
                    nucleusCount); break;
        	case NEUTROPHIL: n = new DefaultLobedNucleus(roi, centreOfMass, imageFile, channel, originalPosition,
                    nucleusCount); break;    
        	case ROUND: 
        	default: n = new DefaultNucleus(roi, centreOfMass, imageFile, channel, originalPosition,
                    nucleusCount);
        }

        nucleusCount++;
        if (n == null)
            throw new ComponentCreationException("Error making nucleus; contstucted object is null");
        finer("Created nucleus with border length "+n.getBorderLength());
        return n;
    }

    @Override
    public Nucleus buildInstance(@NonNull Roi roi, File imageFile, int channel, int[] originalPosition, @NonNull IPoint centreOfMass, UUID id)
            throws ComponentCreationException {
    	if (roi == null)
    		throw new IllegalArgumentException("Roi cannot be null in nucleus factory");
        if (centreOfMass == null)
            throw new IllegalArgumentException("Centre of mass cannot be null in nucleus factory");
        
        Nucleus n = null;

        switch(type) {
        	case RODENT_SPERM: n = new DefaultRodentSpermNucleus(roi, centreOfMass, imageFile, channel, originalPosition,
                    nucleusCount, id); break;
        	case PIG_SPERM: n = new DefaultPigSpermNucleus(roi, centreOfMass, imageFile, channel, originalPosition,
                    nucleusCount, id); break;
        	case NEUTROPHIL: n = new DefaultLobedNucleus(roi, centreOfMass, imageFile, channel, originalPosition,
                    nucleusCount, id); break;    
        	case ROUND: 
        	default: n = new DefaultNucleus(roi, centreOfMass, imageFile, channel, originalPosition,
                    nucleusCount, id);
        }

        nucleusCount++;
        if (n == null)
            throw new ComponentCreationException("Error making nucleus; contstucted object is null");
        finer("Created nucleus with border length "+n.getBorderLength());
        return n;
    }

}
