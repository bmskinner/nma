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


package com.bmskinner.nuclear_morphology.gui.components;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;

import javax.swing.JPanel;

/**
 * This panel contains an Image as its background
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class PaintableJPanel extends JPanel {

    final private Image bgImage;

    public PaintableJPanel(final Image image) {
        super();
        this.bgImage = image;
    }

    public PaintableJPanel() {
        super();
        bgImage = null;
    }

    public void setClip(Rectangle r) {
        if (bgImage != null) {
            bgImage.getGraphics().setClip(r);
            this.repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        if (bgImage != null) {
            g.drawImage(bgImage, 0, 0, null);
        }
    }
}
