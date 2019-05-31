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
package com.bmskinner.nuclear_morphology.io;

import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNull;
import org.jfree.graphics2d.svg.SVGGraphics2D;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.io.Io.Exporter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Write a component to svg format.
 * 
 * @author bms41
 * @since 1.13.5
 *
 */
public class SVGWriter implements Exporter{
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.ROOT_LOGGER);

    private File file;

    /**
     * Create with a destination file
     * 
     * @param f
     */
    public SVGWriter(@NonNull File f) {
        file = f;
    }
    
    /**
     * Export the given component outlines to file. Also exports the dataset name
     * and the number if cells in the dataset.
     * 
     * @param datasets the datasets to export
     */
    public void exportConsensusOutlines(@NonNull List<IAnalysisDataset> datasets) {
        List<Nucleus> consensi = datasets.stream()
                .map(d -> d.getCollection().getConsensus())
                .filter(n -> n!=null)
                .collect(Collectors.toList());
        
        double w = consensi.stream().mapToDouble( c-> c.toShape().getBounds2D().getWidth()).sum();
        double h = consensi.stream().mapToDouble( c-> c.toShape().getBounds2D().getHeight()).max().orElse(100);
        
        w += Imageable.COMPONENT_BUFFER*(consensi.size()+1);
        h += Imageable.COMPONENT_BUFFER*2;
        SVGGraphics2D g2 = new SVGGraphics2D((int) w, (int) h);
        
        double x = Imageable.COMPONENT_BUFFER;

        for(IAnalysisDataset d : datasets){
            if(!d.getCollection().hasConsensus()){
                continue;
            }
            CellularComponent c = d.getCollection().getConsensus();
            Shape s = c.toShape();

            Rectangle2D r = s.getBounds();

            export(s, g2, x, Imageable.COMPONENT_BUFFER+10);
            export(d.getName(), g2, (float) x, Imageable.COMPONENT_BUFFER);
            export(String.valueOf(d.getCollection().size()), g2, (float)x, (float)(Imageable.COMPONENT_BUFFER+(r.getHeight()/2)));
            x+=r.getWidth()+Imageable.COMPONENT_BUFFER;
        }

        write(file, g2);
        
    }
    
    /**
     * Export the given component outlines to file
     * 
     * @param c
     */
    public void export(@NonNull List<? extends @NonNull CellularComponent> list) {
    	
    	double w = list.stream().mapToDouble( c-> c.toShape().getBounds2D().getWidth()).sum();
    	double h = list.stream().mapToDouble( c-> c.toShape().getBounds2D().getHeight()).max().orElse(100);
    	
    	SVGGraphics2D g2 = new SVGGraphics2D((int) w, (int) h);
    	
    	double x = 0;

    	for(CellularComponent c : list){
     		Shape s = c.toShape();

            Rectangle2D r = s.getBounds();

            // Centre the shape on the canvas
            
            if(x==0){
            	x= -r.getMinX();
            }
            double minY = r.getMinY();
            
    		export(s, g2, x, minY);
    		x+=r.getWidth();
    	}

    	write(file, g2);
    }
    
    public void export(@NonNull CellularComponent c, @NonNull String name) {
    	Shape s = c.toShape();

    	//     // Make a canvas of the correct size
    	Rectangle2D r = s.getBounds();

    	// Make a canvas of the correct size
    	SVGGraphics2D g2 = new SVGGraphics2D((int) r.getWidth(), (int) r.getHeight());
    	double minX = -r.getMinX();
    	double minY = r.getMinY();
    	export(s, g2, minX, minY);
    	write(file, g2);

    }
    
    /**
     * Export the given component outline to file
     * 
     * @param s the shape
     * @param g the graphics context
     * @param x the x coordinate in the graphics context
     * @param y the y coordinate in the graphics context
     */
    private void export(@NonNull final Shape s, @NonNull final SVGGraphics2D g, double x, double y) {
        
        SVGGraphics2D g2 = (SVGGraphics2D) g.create();
        
        // Get the location of the shape
        Rectangle2D r = s.getBounds();
        double rx = r.getMinX();
        double ry = r.getMinY();

        // Flip vertically because awt graphics count y from top to bottom
        Shape newS = AffineTransform.getScaleInstance(1, -1).createTransformedShape(s);
       
        // Transform the shape to the needed location for drawing
        double diffx = x - rx;
        double diffy = y - ry;
        AffineTransform at = AffineTransform.getTranslateInstance(diffx, diffy);
        
        // Draw the shape
        g2.setPaint(Color.BLACK);
        g2.setTransform(at);
        g2.draw(newS);
        g2.dispose();
    }
    
    /**
     * Export the given string to the graphics context
     * 
     * @param s the string
     * @param g the graphics context
     * @param x the x coordinate in the graphics context
     * @param y the y coordinate in the graphics context
     */
    private void export(final String s, final SVGGraphics2D g, float x, float y){
        SVGGraphics2D g2 = (SVGGraphics2D) g.create();

        if(s !=null){
            g2.setFont(new Font("Arial", Font.PLAIN, 10)); 
            g2.setPaint(Color.BLACK);
            g2.drawString(s, x, y);
        }
        g2.dispose();
    }
    
    
    private void write(File f, SVGGraphics2D g2){
    	String svgElement = g2.getSVGElement();

        try (PrintWriter out = new PrintWriter(file)) {

            out.println(svgElement);
            LOGGER.info("SVG exported to " + file.getAbsolutePath());

        } catch (FileNotFoundException e) {
            LOGGER.warning("File not found");
            LOGGER.log(Loggable.STACK, e.getMessage(), e);
        }
    }

}
