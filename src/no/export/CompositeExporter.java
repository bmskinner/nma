package no.export;

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

import utility.Logger;
import utility.Utils;
import no.collections.NucleusCollection;
import no.nuclei.Nucleus;

public class CompositeExporter {

	private static Logger logger;

	public static boolean run(NucleusCollection collection){

		logger = new Logger(collection.getDebugFile(), "CompositeExporter");

		if(collection.getNucleusCount()==0){
			logger.log("No nuclei in collection", Logger.DEBUG);
			return false;
		}

		try{

			logger.log("Creating composite image...");

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
					logger.log("Error adding image to composite:", Logger.ERROR);
					logger.log(collection.getType(), Logger.ERROR);
					logger.log(path, Logger.ERROR);
				}     
			}
			IJ.saveAsTiff(finalImage, collection.getFolder()+File.separator+collection.getOutputFolderName()+File.separator+"composite"+"."+collection.getType()+".tiff");
		} catch(Exception e){
			logger.log("Error creating composite image: "+e.getMessage(), Logger.ERROR);
			return false;
		}
		return true;
	}
}
