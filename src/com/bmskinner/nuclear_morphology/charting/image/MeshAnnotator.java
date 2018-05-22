package com.bmskinner.nuclear_morphology.charting.image;

import java.awt.Color;

import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.mesh.Mesh;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshEdge;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshImage;
import com.bmskinner.nuclear_morphology.analysis.mesh.MeshImageCreationException;
import com.bmskinner.nuclear_morphology.analysis.mesh.DefaultMeshImage;
import com.bmskinner.nuclear_morphology.analysis.mesh.UncomparableMeshImageException;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;

import ij.ImagePlus;
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
        	        	
        	ip.drawLine( (int) ((edge.getV1().getPosition().getX() - xDiff)*scale),  
        			(int) ((edge.getV1().getPosition().getY() - yDiff)*scale), 
        			(int) ((edge.getV2().getPosition().getX() - xDiff)*scale), 
        			(int) ((edge.getV2().getPosition().getY() - yDiff)*scale));        	

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
