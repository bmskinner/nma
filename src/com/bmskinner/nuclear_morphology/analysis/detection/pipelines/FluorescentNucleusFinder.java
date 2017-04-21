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

package com.bmskinner.nuclear_morphology.analysis.detection.pipelines;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.bmskinner.nuclear_morphology.analysis.detection.Detector;
import com.bmskinner.nuclear_morphology.analysis.detection.GenericDetector;
import com.bmskinner.nuclear_morphology.analysis.detection.StatsMap;
import com.bmskinner.nuclear_morphology.analysis.image.ImageAnnotator;
import com.bmskinner.nuclear_morphology.analysis.image.ImageFilterer;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.ComponentFactory;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.DefaultCell;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusFactory;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.ICannyOptions;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions;
import com.bmskinner.nuclear_morphology.components.options.MissingOptionException;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;

import ij.gui.Roi;
import ij.process.ImageProcessor;

public class FluorescentNucleusFinder extends AbstractFinder {
	
	final private ComponentFactory<Nucleus> nuclFactory;
	
	public FluorescentNucleusFinder(IAnalysisOptions op) {
		super(op);
		nuclFactory = new NucleusFactory(op.getNucleusType());
	}

	@Override
	public List<ICell> findInImage(File imageFile) throws ImageImportException, ComponentCreationException {
		List<ICell> list = new ArrayList<>();
		
		try {
			// Get all objects that could be nuclei
			List<Nucleus> nuclei = detectNucleus(imageFile);

			IDetectionOptions nuclOptions = options.getDetectionOptions(CellularComponent.NUCLEUS);
			
			// Display passing and failing size nuclei
			if( hasDetectionListeners()){

				ImageProcessor original =  new ImageImporter(imageFile).importImage(nuclOptions.getChannel()).convertToRGB();
				ImageAnnotator ann = new ImageAnnotator(original);

				for(Nucleus n : nuclei){
					Color colour = nuclOptions.isValid(n) ? Color.ORANGE : Color.RED;
					ann.annotateBorder(n, colour);
				}
				fireDetectionEvent(ann.toProcessor().duplicate(), "Detected objects");

				for(Nucleus n : nuclei){
					ann.annotateStats(n, Color.ORANGE, Color.BLUE);
				}
				fireDetectionEvent(ann.toProcessor().duplicate(), "Annotated objects");
			}

			for(Nucleus n : nuclei){
				if(nuclOptions.isValid(n)){
					list.add(new DefaultCell(n));
				}
			}
		} catch(Exception e){
			error("Error searching in image "+imageFile.getAbsolutePath(), e);
//			stack(e);
		}
		
		fireProgressEvent();
		return list;
		
	}
		 
	private List<Nucleus> detectNucleus(File imageFile) throws ImageImportException, ComponentCreationException, MissingOptionException {
		
		List<Nucleus> list = new ArrayList<>();
		IDetectionOptions nuclOptions = options.getDetectionOptions(CellularComponent.NUCLEUS);
		
		ICannyOptions cannyOptions = nuclOptions.getCannyOptions();
		
		int stackNumber = ImageImporter.rgbToStack(nuclOptions.getChannel());

		ImageImporter importer = new ImageImporter(imageFile);
		
		
		ImageProcessor original =  importer
				.toConverter()
				.convertToGreyscale(stackNumber)
				.invert()
				.toProcessor();
		
		ImageProcessor ip = importer.importToStack().getProcessor(stackNumber);
//		fireDetectionEvent(original.duplicate(), "Input image");

		ImageFilterer filt =  new ImageFilterer(ip.duplicate());
		
		filt.runKuwaharaFiltering( cannyOptions.getKuwaharaKernel());
		ip = filt.toProcessor().duplicate();
		ip.invert();
		fireDetectionEvent(ip.duplicate(), "Kuwahara filter");
		
		filt.squashChromocentres(cannyOptions.getFlattenThreshold());
		ip = filt.toProcessor().duplicate();
		ip.invert();
		fireDetectionEvent(ip.duplicate(), "Chromocentre flattening");
		
		filt.runEdgeDetector( cannyOptions );
		ip = filt.toProcessor().duplicate();
		ip.invert();
		fireDetectionEvent(ip.duplicate(), "Edge detection");
		
		filt.morphologyClose(cannyOptions.getClosingObjectRadius());
		ip = filt.toProcessor().duplicate();
		ip.invert();
		fireDetectionEvent(ip.duplicate(), "Gap closing");
		
		GenericDetector gd = new GenericDetector();
		gd.setSize(MIN_PROFILABLE_OBJECT_SIZE, original.getWidth()*original.getHeight());
		
		ImageProcessor img = filt.toProcessor();
		List<Roi> rois = gd.getRois(img.duplicate());
		

		for(int i=0; i<rois.size(); i++){
			Roi roi = rois.get(i);
			Nucleus n = makeNucleus(roi, imageFile, nuclOptions, img, i, gd);
			list.add(n);
			
		}
		return list;
		
	}
	
	
	private Nucleus makeNucleus(Roi roi, File f, IDetectionOptions nuclOptions, ImageProcessor ip, int objectNumber, Detector gd) throws ComponentCreationException {
		
		  // measure the area, density etc within the nucleus
		StatsMap values   = gd.measure(roi, ip);
//		log("\tMeasured stats");
		  // save the position of the roi, for later use
		int xbase = (int) roi.getXBase();
		int ybase = (int) roi.getYBase();

		Rectangle bounds = roi.getBounds();

		int[] originalPosition = {xbase, ybase, (int) bounds.getWidth(), (int) bounds.getHeight() };

		// create a Nucleus from the roi
		IPoint centreOfMass = IPoint.makeNew(values.get(StatsMap.COM_X), values.get(StatsMap.COM_Y));

		Nucleus result = nuclFactory.buildInstance(roi, f, nuclOptions.getChannel(), originalPosition, centreOfMass); 

		// Move the nucleus xbase and ybase to 0,0 coordinates for charting
		IPoint offsetCoM = IPoint.makeNew( centreOfMass.getX() - xbase, centreOfMass.getY() - ybase  );


		result.moveCentreOfMass(offsetCoM);

		result.setStatistic(PlottableStatistic.AREA,      values.get(StatsMap.AREA));
		result.setStatistic(PlottableStatistic.MAX_FERET, values.get(StatsMap.FERET));
		result.setStatistic(PlottableStatistic.PERIMETER, values.get(StatsMap.PERIM));
		
		result.setScale(nuclOptions.getScale());
		
		double prop = options.getProfileWindowProportion();
		result.initialise(prop);
		result.findPointsAroundBorder();
		return result;
	}
	

}
