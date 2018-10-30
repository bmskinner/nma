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
package com.bmskinner.nuclear_morphology.gui.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

import org.virion.jam.controlpanels.ControlPalette;

import jebl.gui.trees.treeviewer.TreeViewer;
//import jebl.gui.trees.treeviewer_dev.DefaultTreeViewer;

/**
 * An extension to jebl's tree viewer that allows lines to be added to the tree
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class DraggableTreeViewer extends TreeViewer {

    List<Line2D.Double> lines = new ArrayList<Line2D.Double>();

    public DraggableTreeViewer() {
        super();

    }

    public DraggableTreeViewer(ControlPalette controlPalette, int CONTROL_PALETTE_ALIGNMENT) {
        super(controlPalette, CONTROL_PALETTE_ALIGNMENT);
    }

    public void addLine(Line2D.Double line) {
        clearLines();
        this.lines.add(line);
    }

    public void clearLines() {
        this.lines = new ArrayList<Line2D.Double>();
    }

    @Override
    public void paint(Graphics g) {

        super.paint(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setPaint(Color.BLACK);
        g2.setStroke(new BasicStroke(2f));
        for (Line2D.Double line : lines) {
            g2.draw(line);
        }
    }

}
