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

import ij.process.ImageProcessor;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Display the original image for a cell, with
 * the nucleus outlines drawn on it. 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class AnnotatedNucleusPanel extends JPanel implements Loggable {
	
	private ICell cell;
	private JLabel imageLabel = new JLabel();
	
	public AnnotatedNucleusPanel(){

		this.setLayout(new BorderLayout());
		this.setBorder(new EmptyBorder(5, 5, 5, 5));
		this.add(imageLabel, BorderLayout.CENTER);
		imageLabel.setHorizontalTextPosition(JLabel.CENTER);
		imageLabel.setVerticalTextPosition(JLabel.BOTTOM);
		imageLabel.setVerticalAlignment(JLabel.CENTER);
		imageLabel.setHorizontalAlignment(JLabel.CENTER);

	}
	
	public void updateCell(ICell c) throws Exception {
		this.cell = c;
		importCellImage();
		
	}
	
	private void importCellImage() throws Exception {
		
		boolean useRGB = false;

		if(cell.hasCytoplasm()){
			useRGB = true;
		}
		
		
		
		ImageProcessor openProcessor = useRGB ? cell.getCytoplasm().getRGBImage() : cell.getNucleus().getImage();
		
		ImageAnnotator an = new ImageAnnotator(openProcessor);
		
		if(cell.hasCytoplasm()){
			an.annotateBorder(cell.getCytoplasm(), Color.CYAN);
		}
		
		for(Nucleus n : cell.getNuclei()){
			an.annotateBorder(n, Color.ORANGE);
		}
		
		openProcessor = an.toProcessor();

		
		ImageIcon icon = null;
		if(imageLabel.getIcon()!=null){
			icon = (ImageIcon) imageLabel.getIcon();
			icon.getImage().flush();
		}
		icon = makeIcon(openProcessor);
		this.setSize(icon.getIconWidth(), icon.getIconHeight());
		imageLabel.setIcon(icon);
		imageLabel.revalidate();
		imageLabel.repaint();
		
		this.repaint();
	}
			
	
	/*
	 * Resize the image to half of the screen height 
	 */
	private ImageIcon makeIcon(ImageProcessor processor){

		ImageProcessor resized = new ImageFilterer(processor)
			.fitToScreen(0.6)
			.toProcessor();

		ImageIcon smallImageIcon = new ImageIcon(resized.getBufferedImage());

		return smallImageIcon;

	}

}
