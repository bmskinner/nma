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

package com.bmskinner.nuclear_morphology.gui.dialogs.prober.workers;

import java.awt.Color;
import java.io.File;
import java.util.List;

import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.nucleus.FluoresentNucleusDetectionPipeline;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.ICannyOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.DetectionImageType;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.ImageProberTableCell;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.ImageSet;
import com.bmskinner.nuclear_morphology.io.ImageImporter;

import ij.process.ImageProcessor;

public class NucleusProberWorker extends ImageProberWorker {
	
	private static final double ANGLE_PROPORTION = 0.05; // A value is needed for the nucleus detector, but has no effect here

	public NucleusProberWorker(final File f, final IDetectionOptions options, final ImageSet type, final TableModel model) {
		super(f, options, type, model);
	}
	
	
	protected void analyseImages() throws Exception {

		if(this.isCancelled()){
			return;
		}
		
		
		
		ICannyOptions cannyOptions = options.getCannyOptions();
		
		int stackNumber = ImageImporter.rgbToStack(options.getChannel());
		ImageProcessor original =  new ImageImporter(file)
				.toConverter()
				.convertToGreyscale(stackNumber)
				.invert()
				.toProcessor();
		
		FluoresentNucleusDetectionPipeline pipe = new FluoresentNucleusDetectionPipeline(options, file, NucleusType.ROUND, ANGLE_PROPORTION);
		pipe.openSizeParameters();
		
		pipe.kuwaharaFilter();
		ImageProberTableCell iconCell1 = makeIconCell(pipe.getInvertedProcessor(), 
				cannyOptions.isUseKuwahara(), 
				DetectionImageType.KUWAHARA);
		publish(iconCell1);
		
		if(this.isCancelled()){
			return;
		}
		
		pipe.flatten();
		ImageProberTableCell iconCell2 = makeIconCell(pipe.getInvertedProcessor(), 
				cannyOptions.isUseFlattenImage(), 
				DetectionImageType.FLATTENED);
		
		publish(iconCell2);
		if(this.isCancelled()){
			return;
		}
		
		pipe.edgeDetect();
		ImageProberTableCell iconCell3 = makeIconCell(pipe.getInvertedProcessor(), 
				cannyOptions.isUseCanny(), 
				DetectionImageType.EDGE_DETECTION);
		
		publish(iconCell3);
		if(this.isCancelled()){
			return;
		}
		
		pipe.gapClose();
		ImageProberTableCell iconCell4 = makeIconCell(pipe.getInvertedProcessor(), 
				cannyOptions.isUseCanny(), 
				DetectionImageType.MORPHOLOGY_CLOSED);
		
		publish(iconCell4);
		if(this.isCancelled()){
			return;
		}
		
		// Show the detected objects
		List<ICell> cells = pipe.findInImage();

		for(ICell cell : cells){
			if(this.isCancelled()){
				return;
			}
			drawNucleus(cell, original);
		}

		ImageProberTableCell iconCell5 = makeIconCell(original, 
				true, 
				DetectionImageType.DETECTED_OBJECTS);
		publish(iconCell5);

		ImageProcessor ap = original.duplicate();

		for(ICell cell : cells){
			if(this.isCancelled()){
				return;
			}
			annotateStats(cell, ap);
		}

		ImageProberTableCell iconCell6 = makeIconCell(ap, 
				true, 
				DetectionImageType.ANNOTAED_OBJECTS);
		publish(iconCell6);
	}
	
	/**
	 * Draw the outline of a nucleus on the given processor
	 * @param cell
	 * @param ip
	 */
	private void drawNucleus(ICell cell, ImageProcessor ip) {
		if(cell==null){
			throw new IllegalArgumentException("Input cell is null");
		}
		
		
		Nucleus n = cell.getNucleus();
		// annotate the image processor with the nucleus outline
		
		Color colour = options.isValid(n) ? Color.ORANGE : Color.RED;

		ip = new ImageAnnotator(ip)
				.annotateBorder(n, colour)
				.toProcessor();

	}	
	
	private void annotateStats(ICell cell, ImageProcessor ip){
		if(cell==null){
			throw new IllegalArgumentException("Input cell is null");
		}
		
		Nucleus n = cell.getNucleus();
		// annotate the image processor with the nucleus outline
		
		ip = new ImageAnnotator(ip)
				.annotateStats(n, Color.ORANGE, Color.BLUE)
				.toProcessor();
	}
}
