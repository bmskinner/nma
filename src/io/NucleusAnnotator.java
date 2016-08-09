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

import gui.components.ColourSelecter.ColourSwatch;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.util.List;
import java.util.UUID;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import utility.Constants;
import utility.Utils;
import analysis.AnalysisDataset;
import components.AbstractCellularComponent;
import components.CellCollection;
import components.generic.BorderTag;
import components.generic.BorderTagObject;
import components.generic.ProfileType;
import components.nuclear.NuclearSignal;
import components.nuclear.BorderPoint;
import components.nuclear.NucleusBorderSegment;
import components.nuclear.SignalCollection;
import components.nuclei.Nucleus;

public class NucleusAnnotator {
	
	private static Logger logger;
	private ColourSwatch swatch;
	
	public NucleusAnnotator(ColourSwatch swatch){
		this.swatch = swatch;
	}
	
	public void run(AnalysisDataset dataset){

		CellCollection collection = dataset.getCollection();
//		logger = Logger.getLogger(NucleusAnnotator.class.getName());
//		try {
//			logger.addHandler(dataset.getLogHandler());
//		} catch (SecurityException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		} catch (Exception e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
////		logger = new Logger(collection.getDebugFile(), "NucleusAnnotator");
//		try{
//			logger.log(Level.INFO, "Annotating images of nuclei...");
			for(Nucleus n : collection.getNuclei()){
				annotateFeatures(n);
			}
//			logger.log(Level.INFO, "Annotation complete");
//
//		}catch(Exception e){
//			logger.log(Level.SEVERE, "Error in annotation", e);
//			return false;
//		} finally {
//			for(Handler h : logger.getHandlers()){
//				h.close();
//				logger.removeHandler(h);
//			}
//		}
//		return true;
	}


//	public static void run(Nucleus n, ColourSwatch swatch){
//		
//		// to add in here - division of functions based on class of nucleus
//		annotateFeatures(n, swatch);
//		
//	}
	
	public ImagePlus annotateFeatures(Nucleus n){

		ImagePlus annotatedImage = new ImagePlus(null, n.getImage());
//				new ImagePlus(n.getAnnotatedImagePath());
		try{

			annotateTail(annotatedImage, n);
			annotateHead(annotatedImage, n);
			annotateCoM(annotatedImage, n);
			annotateMinFeret(annotatedImage, n);
			annotateSegments(annotatedImage, n, swatch);
			annotateSignals(annotatedImage, n);

		}  catch(Exception e){
//			logger.log(Level.SEVERE, "Error annotating nucleus", e);

		} 
		return annotatedImage;
	}

	private static void annotateTail(ImagePlus image, Nucleus n){
		ImageProcessor ip = image.getProcessor();
		
		ip.setColor(Color.CYAN);
		ip.setLineWidth(3);
		ip.drawDot( n.getBorderPoint(BorderTagObject.ORIENTATION_POINT).getXAsInt(), 
				n.getBorderPoint(BorderTagObject.ORIENTATION_POINT).getYAsInt());
	}

	private static void annotateHead(ImagePlus image, Nucleus n){
		ImageProcessor ip = image.getProcessor();
		
		ip.setColor(Color.YELLOW);
		ip.setLineWidth(3);
		ip.drawDot( n.getBorderPoint(BorderTagObject.REFERENCE_POINT).getXAsInt(), 
				n.getBorderPoint(BorderTagObject.REFERENCE_POINT).getYAsInt());
	}
	
	private static void annotateCoM(ImagePlus image, Nucleus n){
		ImageProcessor ip = image.getProcessor();

		ip.setColor(Color.MAGENTA);
		ip.setLineWidth(5);
		ip.drawDot(n.getCentreOfMass().getXAsInt(),  n.getCentreOfMass().getYAsInt());

	}

	// The narrowest part of the nucleus
	private static void annotateMinFeret(ImagePlus image, Nucleus n) throws Exception{
		ImageProcessor ip = image.getProcessor();


		ip.setLineWidth(1);
		ip.setColor(Color.MAGENTA);
		BorderPoint narrow1 = n.getNarrowestDiameterPoint();
		BorderPoint narrow2 = n.findOppositeBorder(narrow1);
		ip.drawLine(narrow1.getXAsInt(), narrow1.getYAsInt(), narrow2.getXAsInt(), narrow2.getYAsInt());

	}
	
	private static void annotateSegments(ImagePlus image, Nucleus n, ColourSwatch swatch){
		ImageProcessor ip = image.getProcessor();

		try{

			if(n.getProfile(ProfileType.ANGLE).getSegments().size()>0){ // only draw if there are segments
				for(int i=0;i<n.getProfile(ProfileType.ANGLE).getSegments().size();i++){

					NucleusBorderSegment seg = n.getProfile(ProfileType.ANGLE).getSegment("Seg_"+i);

					float[] xpoints = new float[seg.length()+1];
					float[] ypoints = new float[seg.length()+1];
					for(int j=0; j<=seg.length();j++){
						int k = AbstractCellularComponent.wrapIndex(seg.getStartIndex()+j, n.getBorderLength());
						BorderPoint p = n.getBorderPoint(k); // get the border points in the segment
						xpoints[j] = (float) p.getX();
						ypoints[j] = (float) p.getY();
					}

					PolygonRoi segRoi = new PolygonRoi(xpoints, ypoints, Roi.POLYLINE);

					// avoid colour wrapping when segment number is 1 more than the colour list
					Color color = swatch.color(i);
//					Color color = i==0 && n.getAngleProfile().getSegments().size()==9 ? Color.MAGENTA : ColourSelecter.getOptimisedColor(i);

					ip.setColor(color);
					ip.setLineWidth(2);
					ip.draw(segRoi);
				}
			}
		} catch(Exception e){
			logger.log(Level.SEVERE, "Error annotating segments", e);
		}
	}
	
	
	private static void annotateSignals(ImagePlus image, Nucleus n){
		ImageProcessor ip = image.getProcessor();
		
		ip.setLineWidth(3);
		SignalCollection signalCollection = n.getSignalCollection();
		for( UUID id : signalCollection.getSignalGroupIDs()){
			int i = signalCollection.getSignalGroupNumber(id);
			List<NuclearSignal> signals = signalCollection.getSignals(id);
			Color colour = i==Constants.FIRST_SIGNAL_CHANNEL 
						 ? Color.RED 
						 : i==Constants.FIRST_SIGNAL_CHANNEL+1 
						 	? Color.GREEN 
						 	: Color.WHITE;
			
			ip.setColor(colour);

			if(!signals.isEmpty()){

				for(NuclearSignal s : signals){
					ip.setLineWidth(3);
					ip.drawDot(s.getCentreOfMass().getXAsInt(), s.getCentreOfMass().getYAsInt());
					ip.setLineWidth(1);
					
					FloatPolygon p = s.createPolygon();
					PolygonRoi roi = new PolygonRoi(p, PolygonRoi.POLYGON);
					ip.draw(roi);
				}
			}
		}
	}
}
