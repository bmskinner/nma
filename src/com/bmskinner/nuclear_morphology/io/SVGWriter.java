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
     * Export the given component outline to file
     * 
     * @param c
     */
    public void export(CellularComponent c) {

        if (c == null) {
            throw new IllegalArgumentException("Component cannot be null");
        }

        Shape s = c.toShape();

        // Flip vertically because awt graphics count y from top to bottom
        AffineTransform at = AffineTransform.getScaleInstance(1, -1);
        s = at.createTransformedShape(s);

        // Make a canvas of the correct size
        Rectangle2D r = s.getBounds();
        SVGGraphics2D g2 = new SVGGraphics2D((int) r.getWidth(), (int) r.getHeight());

        // Centre the shape on the canvas
        double minX = r.getMinX();
        double minY = r.getMinY();
        at = AffineTransform.getTranslateInstance(-minX, -minY);

        // Draw the shape
        g2.setPaint(Color.BLACK);
        g2.setTransform(at);
        g2.draw(s);

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
