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

import ij.process.ImageProcessor;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.TableModel;

import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.CytoplasmDetectionPipeline;
import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.DetectionPipeline;
import com.bmskinner.nuclear_morphology.analysis.detection.pipelines.LobedNucleusDetectionPipeline;
import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.ICytoplasm;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.ICannyOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.DetectionImageType;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.ImageProberTableCell;
import com.bmskinner.nuclear_morphology.gui.dialogs.prober.ImageSet;
import com.bmskinner.nuclear_morphology.io.ImageImporter;

public class NeutrophilProberWorker extends ImageProberWorker {
	
	private IDetectionOptions nucleusOptions;
	
	public NeutrophilProberWorker(final File f, final IDetectionOptions cytoOptions, final IDetectionOptions nucleusOptions, final ImageSet type, final TableModel model) {
		super(f, cytoOptions, type, model);
		this.nucleusOptions = nucleusOptions;
	}
	
	
	protected void analyseImages() throws Exception {

		if(this.isCancelled()){
			return;
		}
		
		// Detect the cytoplasm
		
		ICannyOptions cannyOptions = options.getCannyOptions();
		
		int stackNumber = ImageImporter.rgbToStack(options.getChannel());
		ImageProcessor original =  new ImageImporter(file)
				.toConverter()
				.convertToGreyscale(stackNumber)
//				.invert()
				.toProcessor();
				
		
		DetectionPipeline<ICytoplasm> cyto = new CytoplasmDetectionPipeline(options, file, ANGLE_PROPORTION);
		cyto.kuwaharaFilter()
			.raise();
		
		
		ImageProberTableCell iconCell1 = makeIconCell(cyto.getInvertedProcessor(), 
				true, 
				DetectionImageType.CYTO_FLATTENED);
		publish(iconCell1);
		
		if(this.isCancelled()){
			return;
		}
			
			
			
		cyto.edgeDetect()
			.gapClose();
			
		ImageProberTableCell iconCell2 = makeIconCell(cyto.getInvertedProcessor(), 
				true, 
				DetectionImageType.CYTOPLASM);
		publish(iconCell2);
		
		if(this.isCancelled()){
			return;
		}
			
		List<ICell> cells = new ArrayList<ICell>(0);
		List<ICytoplasm> cytoplasms =	cyto.findInImage();
		for(ICytoplasm cy : cytoplasms){
			
			ICell cell = new DefaultCell(cy);
			
			cells.add(cell);
			
		}	
		
		DetectionPipeline<Nucleus> nucl = new LobedNucleusDetectionPipeline(nucleusOptions, file, ANGLE_PROPORTION, cells);
		
		
		nucl.kuwaharaFilter()
				.flatten();
				
		ImageProberTableCell iconCell3 = makeIconCell(nucl.getInvertedProcessor(), 
				true, 
				DetectionImageType.NUCLEUS_FLATTENED);
		publish(iconCell3);

		if(this.isCancelled()){
			return;
		}	
		
		nucl.edgeDetect()
			.gapClose();
		
		ImageProberTableCell iconCell4 = makeIconCell(nucl.getInvertedProcessor(), 
				true, 
				DetectionImageType.NUCLEUS);
		publish(iconCell4);
		
		if(this.isCancelled()){
			return;
		}
		
		List<Nucleus> nuclei = nucl.findInImage();
		
		
		// Add nuclei to cells
		for(ICell cell : cells){

			for(Nucleus n : nuclei){
				if( cell.getCytoplasm().containsOriginalPoint(n.getOriginalCentreOfMass())){
					cell.addNucleus(n);
				}
			}

		}

		// Only consider cells with detected nuclei and cytoplasm
		List<ICell> result = new ArrayList<ICell>(0);
		for(ICell cell : cells){
			if(cell.hasCytoplasm() && cell.hasNucleus()){
				result.add(cell);
			}
		}
		

		for(ICell cell : result){
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

		for(ICell cell : result){
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
		ICytoplasm c = cell.getCytoplasm();
		// annotate the image processor with the nucleus outline
		
		Color colour     = nucleusOptions.isValid(n) ? Color.ORANGE : Color.RED;
		Color cytoColour = options.isValid(n) ? Color.ORANGE : Color.RED;

		ip = new ImageAnnotator(ip)
				.annotateBorder(n, colour)
				.annotateBorder(c, cytoColour)
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
