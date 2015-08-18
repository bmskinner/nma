package no.export;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.util.List;

import utility.Constants;
import utility.Logger;
import utility.Utils;
import no.analysis.ProfileSegmenter;
import no.collections.CellCollection;
import no.components.NuclearSignal;
import no.components.NucleusBorderPoint;
import no.components.NucleusBorderSegment;
import no.components.SignalCollection;
import no.gui.ColourSelecter;
import no.imports.ImageImporter;
import no.nuclei.Nucleus;

public class NucleusAnnotator {
	
	private static Logger logger;
	
	public static boolean run(CellCollection collection){

		logger = new Logger(collection.getDebugFile(), "NucleusAnnotator");
		try{
			logger.log("Annotating images of nuclei...");
			for(Nucleus n : collection.getNuclei()){
				NucleusAnnotator.run(n);
			}
			logger.log("Annotation complete");

		}catch(Exception e){
			logger.log("Error in annotaion: "+e.getMessage(), Logger.ERROR);
			return false;
		}
		return true;
	}


	public static void run(Nucleus n){
		
		// to add in here - division of functions based on class of nucleus
		annotateFeatures(n);
		
	}
	
	private static void annotateFeatures(Nucleus n){

		ImagePlus annotatedImage = new ImagePlus(n.getAnnotatedImagePath());
		try{

			annotateTail(annotatedImage, n);
			annotateHead(annotatedImage, n);
			annotateCoM(annotatedImage, n);
			annotateMinFeret(annotatedImage, n);
			annotateSegments(annotatedImage, n);
			annotateSignals(annotatedImage, n);

		}  catch(Exception e){
			logger.log("Error annotating nucleus: "+e, Logger.ERROR);

		} finally {
			IJ.saveAsTiff(annotatedImage, n.getAnnotatedImagePath());
		}

	}

	private static void annotateTail(ImagePlus image, Nucleus n){
		ImageProcessor ip = image.getProcessor();
		
		ip.setColor(Color.CYAN);
		ip.setLineWidth(3);
		ip.drawDot( n.getBorderTag("tail").getXAsInt(), 
				n.getBorderTag("tail").getYAsInt());
	}

	private static void annotateHead(ImagePlus image, Nucleus n){
		ImageProcessor ip = image.getProcessor();
		
		ip.setColor(Color.YELLOW);
		ip.setLineWidth(3);
		ip.drawDot( n.getBorderTag("head").getXAsInt(), 
				n.getBorderTag("head").getYAsInt());
	}
	
	private static void annotateCoM(ImagePlus image, Nucleus n){
		ImageProcessor ip = image.getProcessor();

		ip.setColor(Color.MAGENTA);
		ip.setLineWidth(5);
		ip.drawDot(n.getCentreOfMass().getXAsInt(),  n.getCentreOfMass().getYAsInt());

	}

	// The narrowest part of the nucleus
	private static void annotateMinFeret(ImagePlus image, Nucleus n){
		ImageProcessor ip = image.getProcessor();


		ip.setLineWidth(1);
		ip.setColor(Color.MAGENTA);
		NucleusBorderPoint narrow1 = n.getNarrowestDiameterPoint();
		NucleusBorderPoint narrow2 = n.findOppositeBorder(narrow1);
		ip.drawLine(narrow1.getXAsInt(), narrow1.getYAsInt(), narrow2.getXAsInt(), narrow2.getYAsInt());

	}
	
	private static void annotateSegments(ImagePlus image, Nucleus n){
		ImageProcessor ip = image.getProcessor();

		if(n.getAngleProfile().getSegments().size()>0){ // only draw if there are segments
			for(int i=0;i<n.getAngleProfile().getSegments().size();i++){

				NucleusBorderSegment seg = n.getAngleProfile().getSegment("Seg_"+i);

				float[] xpoints = new float[seg.length()+1];
				float[] ypoints = new float[seg.length()+1];
				for(int j=0; j<=seg.length();j++){
					int k = Utils.wrapIndex(seg.getStartIndex()+j, n.getLength());
					NucleusBorderPoint p = n.getBorderPoint(k); // get the border points in the segment
					xpoints[j] = (float) p.getX();
					ypoints[j] = (float) p.getY();
				}

				PolygonRoi segRoi = new PolygonRoi(xpoints, ypoints, Roi.POLYLINE);

				// avoid colour wrapping when segment number is 1 more than the colour list
				Color color = i==0 && n.getAngleProfile().getSegments().size()==9 ? Color.MAGENTA : ColourSelecter.getSegmentColor(i);

				ip.setColor(color);
				ip.setLineWidth(2);
				ip.draw(segRoi);
			}
		}
	}
	
	
	private static void annotateSignals(ImagePlus image, Nucleus n){
		ImageProcessor ip = image.getProcessor();
		
		ip.setLineWidth(3);
		SignalCollection signalCollection = n.getSignalCollection();
		for( int i : signalCollection.getSignalGroups()){
			List<NuclearSignal> signals = signalCollection.getSignals(i);
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
					
					FloatPolygon p = Utils.createPolygon(s.getBorder());
					PolygonRoi roi = new PolygonRoi(p, PolygonRoi.POLYGON);
					ip.draw(roi);
				}
			}
		}
	}
}
