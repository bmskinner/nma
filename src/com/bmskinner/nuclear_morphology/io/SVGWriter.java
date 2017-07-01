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


package com.bmskinner.nuclear_morphology.io;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

import org.jfree.graphics2d.svg.SVGGraphics2D;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Write a component to svg format.
 * 
 * @author bms41
 * @since 1.13.5
 *
 */
public class SVGWriter implements Exporter, Loggable {

    private File file;

    /**
     * Create with a destination file
     * 
     * @param f
     */
    public SVGWriter(File f) {

        if (f == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        file = f;
    }
    
    /**
     * Export the given component outlines to file
     * 
     * @param c
     */
    public void export(List<? extends CellularComponent> list) {
    	
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
    
    public void export(CellularComponent c) {
    	if (c == null) {
            throw new IllegalArgumentException("Component cannot be null");
        }

    	Shape s = c.toShape();
    	//
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
     * @param c
     */
    private void export(Shape s, SVGGraphics2D g2, double x, double y) {
    	
       	// Flip vertically because awt graphics count y from top to bottom
        AffineTransform at = AffineTransform.getScaleInstance(1, -1);
        s = at.createTransformedShape(s);


        at = AffineTransform.getTranslateInstance(x, -y);

        // Draw the shape
        g2.setPaint(Color.BLACK);
        g2.setTransform(at);
        g2.draw(s);


        
    }
    
    private void write(File f, SVGGraphics2D g2){
    	String svgElement = g2.getSVGElement();

        try (PrintWriter out = new PrintWriter(file)) {

            out.println(svgElement);
            log("SVG exported to " + file.getAbsolutePath());

        } catch (FileNotFoundException e) {
            warn("File not found");
            stack(e);
        }
    }

}
