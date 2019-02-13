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
package com.bmskinner.nuclear_morphology.charting.image;

import java.awt.Color;

import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.mesh.Mesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshImage;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

import ij.process.ImageProcessor;

public class MeshAnnotator extends ImageAnnotator {

	private Mesh<Nucleus> mesh;
	private MeshImage<Nucleus> image;
	
	public MeshAnnotator(ImageProcessor ip, Mesh<Nucleus> m) {
		super(ip);
		mesh = m;
	}
	
	public MeshAnnotator(ImageProcessor ip, int maxWidth, int maxHeight, Mesh<Nucleus> m) {
		super(ip, maxWidth, maxHeight);
		mesh = m;
	}
	
	/**
     * Draw the edges of the mesh. Ratios stored in edges are coloured
     * on a blue-red gradient
     * 
     * @param mesh
     * @return
     * @throws Exception
     */
    public MeshAnnotator annotateNucleusMeshEdges() {
    	    	
    	double maxRatio = mesh.getMaxEdgeRatio();
    	
    	double xDiff = mesh.getComponent().getOriginalBase().minus(Imageable.COMPONENT_BUFFER).getX();
    	double yDiff = mesh.getComponent().getOriginalBase().minus(Imageable.COMPONENT_BUFFER).getY();

    	ip.setLineWidth(2);
	
        for (MeshEdge edge : mesh.getEdges()) {
        	Color c = getGradientColour(edge.getLog2Ratio(), maxRatio);
        	ip.setColor(c);
        	        	
        	ip.drawLine( (int) ((edge.getV1().getPosition().getX() - xDiff)*getScale()),  
        			(int) ((edge.getV1().getPosition().getY() - yDiff)*getScale()), 
        			(int) ((edge.getV2().getPosition().getX() - xDiff)*getScale()), 
        			(int) ((edge.getV2().getPosition().getY() - yDiff)*getScale()));        	

        }
        return this;

    }
    
    /**
     * Log2 ratios are coming in, which must be converted to real ratios
     * 
     * @param ratio
     * @param minRatio
     * @param maxRatio
     * @return
     */
    private Color getGradientColour(double ratio, double maxRatio) {

        double log2Min = -maxRatio;
        double log2Max = maxRatio;

        int rValue = 0;
        int bValue = 0;

        if (ratio <= 0) {

            if (ratio < log2Min) {
                bValue = 255;
            } else {
                // ratio of ratio

                // differnce between 0 and minRatio
                double range = Math.abs(log2Min);
                double actual = range - Math.abs(ratio);

                double realRatio = 1 - (actual / range);
                bValue = (int) (255d * realRatio);
            }

        } else {

            if (ratio > log2Max) {
                rValue = 255;
            } else {

                // differnce between 0 and minRatio
                double range = Math.abs(log2Max);
                double actual = range - Math.abs(ratio);

                double realRatio = 1 - (actual / range);
                rValue = (int) (255d * realRatio);
            }

        }
        int r = rValue;
        int g = 0;
        int b = bValue;
        return new Color(r, g, b);
    }
	
	

}
