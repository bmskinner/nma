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
package gui.components;

import ij.ImageStack;
import ij.process.ImageProcessor;
import io.ImageImporter;

import java.awt.BorderLayout;
import java.io.File;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import analysis.image.ImageConverter;
import analysis.image.ImageFilterer;
import analysis.image.NucleusAnnotator;
import logging.Loggable;
import components.Cell;

/**
 * Display the original image for a cell, with
 * the nucleus outlines drawn on it. 
 * @author bms41
 *
 */
@SuppressWarnings("serial")
public class AnnotatedNucleusPanel extends JPanel implements Loggable {
	
	private Cell cell;
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
	
	public void updateCell(Cell c) throws Exception {
		this.cell = c;
		importNucleusImage();
		
	}
	
	private void importNucleusImage() throws Exception {
		File imageFile = cell.getNucleus().getSourceFile();
		ImageStack imageStack = ImageImporter.getInstance().importImage(imageFile);
		
		// Get the counterstain stack, make greyscale and invert
		ImageProcessor openProcessor= new ImageConverter(imageStack)
			.convertToGreyscale()
			.invert()
			.getProcessor();

		
		openProcessor = new NucleusAnnotator(openProcessor)
				.annotateSegments(cell.getNucleus())
				.getProcessor();

		
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
			.crop(cell.getNucleus())
			.fitToScreen(0.3)
			.getProcessor();

		ImageIcon smallImageIcon = new ImageIcon(resized.getBufferedImage());

		return smallImageIcon;

	}

}
