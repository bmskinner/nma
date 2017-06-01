/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.gui.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;
import jebl.evolution.graphs.Node;
import jebl.evolution.trees.RootedTree;
import jebl.gui.trees.treeviewer.painters.BasicLabelPainter;

public class VariableNodePainter extends BasicLabelPainter {

    public VariableNodePainter(String title, RootedTree tree, PainterIntent intent) {
        super(title, tree, intent);

    }

    @Override
    public void paint(Graphics2D g2, Node item, Justification justification, Rectangle2D bounds) {
        final Font oldFont = g2.getFont();
        final Font newFont = new Font(oldFont.getFontName(), Font.BOLD, oldFont.getSize());

        if (item.getAttributeNames().contains("Color")) {
            g2.setFont(newFont);
            Paint p = (Paint) item.getAttribute("Color");
            g2.setPaint(p);
        } else {
            g2.setFont(newFont);
            g2.setPaint(Color.BLACK);
        }

        // final String label = getLabel(item);

        final String label = item.getAttribute("ShortName").toString();

        if (label != null) {

            Rectangle2D rect = g2.getFontMetrics().getStringBounds(label, g2);

            float xOffset = 0;

            float yOffset = (float) g2.getFontMetrics().getAscent();

            float y = yOffset + (float) bounds.getY();
            switch (justification) {
            case CENTER:
                break;
            case FLUSH:
            case LEFT:
                xOffset = (float) bounds.getX();
                break;
            case RIGHT:
                xOffset = (float) (bounds.getX() + bounds.getWidth() - rect.getWidth());
                break;
            default:
                throw new IllegalArgumentException("Unrecognized alignment enum option");
            }

            g2.drawString(label, xOffset, y);
        }

        g2.setFont(oldFont);

    }

}
