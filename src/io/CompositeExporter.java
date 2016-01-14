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
package io;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.io.Opener;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import analysis.AnalysisDataset;
import utility.Utils;
import components.CellCollection;
import components.nuclei.Nucleus;

public class CompositeExporter {

	private static Logger logger;
	public static final int MAX_COMPOSITABLE_NUCLEI = 600;
	
	public static boolean run(AnalysisDataset dataset){
		logger = Logger.getLogger(CompositeExporter.class.getName());
		try {
			logger.addHandler(dataset.getLogHandler());
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		CellCollection collection = dataset.getCollection();
		boolean ok = run(collection, logger);
		return ok;
	}

	public static boolean run(CellCollection collection, Logger logger){

		CompositeExporter.logger = logger;
		

		if(collection.getNucleusCount()==0){
			logger.log(Level.FINE, "No nuclei in collection");
			return false;
		}

		try{

			logger.log(Level.INFO, "Creating composite image...");

			int totalWidth = 0;
			int totalHeight = 0;

			int boxWidth  = (int)(collection.getMedianNuclearPerimeter()/1.4);
			int boxHeight = (int)(collection.getMedianNuclearPerimeter()/1.2);

			int maxBoxWidth = boxWidth * 5;
			int maxBoxHeight = (boxHeight * (int)(Math.ceil(collection.getNucleusCount()/5)) + boxHeight );

			ImagePlus finalImage = new ImagePlus("Final image", new BufferedImage(maxBoxWidth, maxBoxHeight, BufferedImage.TYPE_INT_RGB));
			ImageProcessor finalProcessor = finalImage.getProcessor();
			finalProcessor.setBackgroundValue(0);

			for(Nucleus n : collection.getNuclei()){

				String path = n.getAnnotatedImagePath();

				try {
					Opener localOpener = new Opener();
					ImagePlus image = localOpener.openImage(path);
					ImageProcessor ip = image.getProcessor();

					FloatPolygon polygon = Utils.createPolygon(n);
					PolygonRoi roi = new PolygonRoi(polygon, Roi.POLYGON);
					ip.setRoi(roi);


					ImageProcessor newProcessor = ip.createProcessor(boxWidth, boxHeight);

					newProcessor.setBackgroundValue(0);
					newProcessor.insert(ip, (int)boxWidth/4, (int)boxWidth/4); // put the original halfway in
					newProcessor.setInterpolationMethod(ImageProcessor.BICUBIC);
					newProcessor.rotate( n.findRotationAngle() );
					newProcessor.setBackgroundValue(0);

					if(totalWidth>maxBoxWidth-boxWidth){
						totalWidth=0;
						totalHeight+=(int)(boxHeight);
					}
					int newX = totalWidth;
					int newY = totalHeight;
					totalWidth+=(int)(boxWidth);

					finalProcessor.insert(newProcessor, newX, newY);
					TextRoi label = new TextRoi(newX, newY, n.getImageName()+"-"+n.getNucleusNumber());
					Overlay overlay = new Overlay(label);
					finalProcessor.drawOverlay(overlay);  
				} catch(Exception e){
					logger.log(Level.SEVERE, "Error adding image to composite", e);
				}     
			}
			IJ.saveAsTiff(finalImage, collection.getFolder()+File.separator+collection.getOutputFolderName()+File.separator+"composite"+"."+collection.getType()+".tiff");
		} catch(Exception e){
			logger.log(Level.SEVERE, "Error creating composite image", e);
			return false;
		} finally {
			for(Handler h : logger.getHandlers()){
				h.close();
				logger.removeHandler(h);
			}
		}
		return true;
	}
}
