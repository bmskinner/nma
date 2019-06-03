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

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclear.Lobe;
import com.bmskinner.nuclear_morphology.components.nuclei.LobedNucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.process.ImageProcessor;

/**
 * Display the original image for a cell, with the nucleus outlines drawn on it.
 * 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class AnnotatedNucleusPanel extends JPanel {
	
	private static final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    private ICell  cell;
    private JLabel imageLabel = new JLabel();

    public AnnotatedNucleusPanel() {

        this.setLayout(new BorderLayout());
        this.setBorder(new EmptyBorder(5, 5, 5, 5));
        this.add(imageLabel, BorderLayout.CENTER);
        imageLabel.setHorizontalTextPosition(JLabel.CENTER);
        imageLabel.setVerticalTextPosition(JLabel.BOTTOM);
        imageLabel.setVerticalAlignment(JLabel.CENTER);
        imageLabel.setHorizontalAlignment(JLabel.CENTER);

    }

    /**
     * Display the given cell annotated on its source image
     * @param c the cell to draw
     * @throws Exception
     */
    public void showCell(ICell c) throws Exception {
        this.cell = c;
        importCellImage();

    }
    
    /**
     * Display the given cell cropped
     * @param c
     * @throws Exception
     */
    public void showOnlyCell(ICell c, boolean annotated) throws Exception {
        this.cell = c;
        ImageProcessor ip;

        try {
            if (c.hasCytoplasm()) {
                ip = c.getCytoplasm().getComponentRGBImage();
            } else {
                ip = c.getNuclei().get(0).getComponentImage();
            }

        } catch (UnloadableImageException e) {
            LOGGER.log(Loggable.STACK, "Cannot load image for component", e);
            return;
        }
        
        if(annotated){

        	ImageAnnotator an = new ImageAnnotator(ip);
        	if (c.hasCytoplasm()) {

        		an = an.annotateBorder(c.getCytoplasm(), c.getCytoplasm(), Color.CYAN);
        		for (Nucleus n : c.getNuclei()) {
        			an.annotateBorder(n, c.getCytoplasm(), Color.ORANGE);

        			if (n instanceof LobedNucleus) {

        				for (Lobe l : ((LobedNucleus) n).getLobes()) {
        					an.annotateBorder(l, c.getCytoplasm(), Color.RED);
        					an.annotatePoint(l.getCentreOfMass(), c.getCytoplasm(), Color.GREEN);
        				}
        			}
        		}

        	} else {

        		for (Nucleus n : c.getNuclei()) {
        			an = an.annotateSegments(n, n);
        		}
        	}
        	ip = an.toProcessor();
        }
        
        drawIcon(ip);
    }
    

    private void importCellImage() throws Exception {

        boolean useRGB = false;

        if (cell.hasCytoplasm()) {
            useRGB = true;
        }
        
        ImageProcessor openProcessor = useRGB ? cell.getCytoplasm().getRGBImage() : cell.getNuclei().get(0).getRGBImage();

//        ImageProcessor openProcessor = useRGB ? cell.getCytoplasm().getRGBImage() : cell.getNuclei().get(0).getImage();

        ImageAnnotator an = new ImageAnnotator(openProcessor);

        if (cell.hasCytoplasm()) {
            an.annotateBorder(cell.getCytoplasm(), Color.CYAN);
        }

        for (Nucleus n : cell.getNuclei()) {
            an.annotateBorder(n, Color.ORANGE);
        }

        drawIcon(an.toProcessor());
    }
    
    /**
     * Draw the given processor to the image icon
     * @param ip
     */
    private void drawIcon(ImageProcessor ip){
    	ImageIcon icon = null;
        if (imageLabel.getIcon() != null) {
            icon = (ImageIcon) imageLabel.getIcon();
            icon.getImage().flush();
        }
        icon = makeIcon(ip);
//        this.setSize(icon.getIconWidth(), icon.getIconHeight());
        imageLabel.setIcon(icon);
        imageLabel.revalidate();
        imageLabel.repaint();

        this.repaint();
    }

    /*
     * Resize the image to half of the screen height
     */
    private ImageIcon makeIcon(ImageProcessor processor) {

        ImageProcessor resized = new ImageFilterer(processor).resize(2).toProcessor();

        ImageIcon smallImageIcon = new ImageIcon(resized.getBufferedImage());

        return smallImageIcon;

    }

}
