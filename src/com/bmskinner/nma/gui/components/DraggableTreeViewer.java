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
package com.bmskinner.nma.gui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

import org.virion.jam.controlpanels.ControlPalette;

import jebl.gui.trees.treeviewer.TreeViewer;

/**
 * An extension to jebl's tree viewer that allows lines to be added to the tree
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class DraggableTreeViewer extends TreeViewer {

    private final List<Line2D> lines = new ArrayList<>();
    
    public DraggableTreeViewer() {
        super();
    }

    public DraggableTreeViewer(ControlPalette controlPalette, int alignment) {
        super(controlPalette, alignment);
    }

    
    public void addLine(Line2D line) {
        clearLines();
        lines.add(line);
    }

    public void clearLines() {
    	lines.clear();
    }

    @Override
    public void paint(Graphics g) {

        super.paint(g);

        Graphics2D g2 = (Graphics2D) g;
        Color c = g2.getColor();
        g2.setPaint(Color.BLACK);
        g2.setStroke(new BasicStroke(2f));
        for (Line2D line : lines) {
            g2.draw(line);
        }
        g2.setPaint(c);
    }

}
