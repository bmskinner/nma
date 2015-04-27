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

import no.collections.INuclearCollection;
import no.nuclei.INuclearFunctions;
import no.utility.Utils;

public class CompositeExporter {

	public static void run(INuclearCollection collection){
	    if(collection.getNucleusCount()==0){
	      return;
	    }
	    
	    IJ.log("    Creating composite image...");
	    
	    int totalWidth = 0;
	    int totalHeight = 0;

	    int boxWidth  = (int)(collection.getMedianNuclearPerimeter()/1.4);
	    int boxHeight = (int)(collection.getMedianNuclearPerimeter()/1.2);

	    int maxBoxWidth = boxWidth * 5;
	    int maxBoxHeight = (boxHeight * (int)(Math.ceil(collection.getNucleusCount()/5)) + boxHeight );

	    ImagePlus finalImage = new ImagePlus("Final image", new BufferedImage(maxBoxWidth, maxBoxHeight, BufferedImage.TYPE_INT_RGB));
	    ImageProcessor finalProcessor = finalImage.getProcessor();
	    finalProcessor.setBackgroundValue(0);

	    for(INuclearFunctions n : collection.getNuclei()){
	      
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
	        IJ.log("Error adding image to composite");
	        IJ.append("Error adding image to composite: "+e, collection.getDebugFile().getAbsolutePath());
	        IJ.append("  "+collection.getType(), collection.getDebugFile().getAbsolutePath());
	        IJ.append("  "+path, collection.getDebugFile().getAbsolutePath());
	      }     
	    }
	    // finalImage.show();
	    IJ.saveAsTiff(finalImage, collection.getFolder()+File.separator+collection.getOutputFolderName()+File.separator+"composite"+"."+collection.getType()+".tiff");
	    IJ.log("    Composite image created");
	  }
}
