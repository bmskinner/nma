/*
-------------------------------------------------
PIG SPERM MORPHOLOGY ANALYSIS: IMAGEJ PLUGIN
-------------------------------------------------
Copyright (C) Ben Skinner 2015



  ---------------
  PLOT AND IMAGE FILES
  ---------------

  ---------------
  LOG FILES
  ---------------
  

*/
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.ProgressBar;
import ij.gui.TextRoi;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.DirectoryChooser;
import ij.io.Opener;
import ij.io.OpenDialog;
import ij.io.RandomAccessStream;
import ij.measure.ResultsTable;
import ij.measure.SplineFitter;
import ij.plugin.ChannelSplitter;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.FloatPolygon;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.Polygon;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.*;
import no.nuclei.*;
import no.nuclei.sperm.*;
import no.analysis.*;

public class Pig_Sperm_Analysis
  extends ImagePlus
  implements PlugIn
{

  // failure codes - not in use, keep to add back to logFailed in refilter
  private static final String IMAGE_PREFIX = "export.";

  private static final int NUCLEUS_THRESHOLD = 36;
  private static final double MIN_NUCLEAR_SIZE = 2000;
  private static final double MAX_NUCLEAR_SIZE = 8000;
  private static final double MIN_NUCLEAR_CIRC = 0.4;
  private static final double MAX_NUCLEAR_CIRC = 0.9;

  private NucleusCollection completeCollection;
  private NucleusCollection failedNuclei;

  private ArrayList<PigSpermNucleusCollection> nuclearPopulations = new ArrayList<PigSpermNucleusCollection>(0);
  private ArrayList<PigSpermNucleusCollection> failedPopulations  = new ArrayList<PigSpermNucleusCollection>(0);
    
  public void run(String paramString)  {

    DirectoryChooser localOpenDialog = new DirectoryChooser("Select directory of images...");
    String folderName = localOpenDialog.getDirectory();

    if(folderName==null){
      return;
    }

    IJ.log("Directory: "+folderName);

    File folder = new File(folderName);
    NucleusDetector detector = new NucleusDetector(folder, MIN_NUCLEAR_SIZE, MAX_NUCLEAR_SIZE);
    detector.runDetector();

    HashMap<File, NucleusCollection> folderCollection = detector.getNucleiCollections();

    IJ.log("Imported folder(s)");
    getPopulations(folderCollection);
    analysePopulations();

    IJ.log("Analysis complete");
  }

  public void getPopulations(HashMap<File, NucleusCollection> folderCollection){
    Set<File> keys = folderCollection.keySet();

    for (File key : keys) {
      NucleusCollection collection = folderCollection.get(key);
      PigSpermNucleusCollection processed = new PigSpermNucleusCollection(key, "complete");
      IJ.log(key.getAbsolutePath()+"   Nuclei: "+collection.getNucleusCount());

      for(int i=0;i<collection.getNucleusCount();i++){
        Nucleus n = collection.getNucleus(i);
        PigSpermNucleus p = new PigSpermNucleus(n);
        spermNuclei.addNucleus(p);
      }
      this.nuclearPopulations.add(spermNuclei);
    }
  }

  public void analysePopulations(){
    IJ.log("Beginning analysis");

    for(PigSpermNucleusCollection r : this.nuclearPopulations){

      if(r.getDebugFile().exists()){
        r.getDebugFile().delete();
      }

      File folder = r.getFolder();
      IJ.log("  Analysing: "+folder.getName());

      PigSpermNucleusCollection failedNuclei = new PigSpermNucleusCollection(folder, "failed");

      r.refilterNuclei(failedNuclei); // put fails into failedNuclei, remove from r

      IJ.log("Analysing population: "+r.getType());
      IJ.log("  Total nuclei: "+r.getNucleusCount());
      IJ.log("  Red signals: "+r.getRedSignalCount());
      IJ.log("  Green signals: "+r.getGreenSignalCount());

      r.recalculateTailPositions();
      r.refilterNuclei(failedNuclei);
      r.measureNuclearOrganisation();
      r.measureAndExportNuclei();

      failedNuclei.measureAndExportNuclei();

      // attemptRefoldingConsensusNucleus(r);

      // split complete set by signals and analyse
      PigSpermNucleusCollection redNuclei = new PigSpermNucleusCollection(folder, "red");
      ArrayList<Nucleus> redList = r.getNucleiWithSignals(Nucleus.RED_CHANNEL);
      for(Nucleus n : redList){
        redNuclei.addNucleus( (PigSpermNucleus)n );
      }
      redNuclei.measureAndExportNuclei();
      // attemptRefoldingConsensusNucleus(redNuclei);


    }
  }

  // public void rotateAndAssembleNucleiForExport(PigSpermNucleusCollection collection){

  //   // foreach nucleus
  //   // createProcessor (500, 500)
  //   // sertBackgroundValue(0)
  //   // paste in old image at centre
  //   // insert(ImageProcessor ip, int xloc, int yloc)
  //   // rotate about CoM (new position)
  //   // display.
  //   IJ.log("Creating composite image...");
    
  //   int totalWidth = 0;
  //   int totalHeight = 0;

  //   int boxWidth = (int)(collection.getMedianNuclearPerimeter()/1.4);
  //   int boxHeight = (int)(collection.getMedianNuclearPerimeter()/1.2);

  //   int maxBoxWidth = boxWidth * 5;
  //   int maxBoxHeight = (boxHeight * (int)(Math.ceil(collection.getNucleusCount()/5)) + boxHeight );

  //   ImagePlus finalImage = new ImagePlus("Final image", new BufferedImage(maxBoxWidth, maxBoxHeight, BufferedImage.TYPE_INT_RGB));
  //   ImageProcessor finalProcessor = finalImage.getProcessor();
  //   finalProcessor.setBackgroundValue(0);

  //   for(int i=0; i<collection.getNucleusCount();i++){
      
  //     PigSpermNucleus n = collection.getNucleus(i);
  //     String path = n.getNucleusFolder().getAbsolutePath()+
  //                     File.separator+
  //                     this.IMAGE_PREFIX+
  //                     n.getNucleusNumber()+
  //                     ".annotated.tiff";

  //     try {
  //       Opener localOpener = new Opener();
  //       ImagePlus image = localOpener.openImage(path);
  //       ImageProcessor ip = image.getProcessor();
  //       int width = ip.getWidth();
  //       int height = ip.getHeight();
  //       ip.setRoi(n.getRoi());


  //       ImageProcessor newProcessor = ip.createProcessor(boxWidth, boxHeight);

  //       newProcessor.setBackgroundValue(0);
  //       newProcessor.insert(ip, (int)boxWidth/4, (int)boxWidth/4); // put the original halfway in
  //       newProcessor.setInterpolationMethod(ImageProcessor.BICUBIC);
  //       newProcessor.rotate( n.findRotationAngle() );
  //       newProcessor.setBackgroundValue(0);

  //       if(totalWidth>maxBoxWidth-boxWidth){
  //         totalWidth=0;
  //         totalHeight+=(int)(boxHeight);
  //       }
  //       int newX = totalWidth;
  //       int newY = totalHeight;
  //       totalWidth+=(int)(boxWidth);
        
  //       finalProcessor.insert(newProcessor, newX, newY);
  //       TextRoi label = new TextRoi(newX, newY, n.getImageName()+"-"+n.getNucleusNumber());
  //       Overlay overlay = new Overlay(label);
  //       finalProcessor.drawOverlay(overlay);  
  //     } catch(Exception e){
  //       IJ.log("Error adding image to composite");
  //       // IJ.append("Error adding image to composite: "+e, debugFile);
  //       // IJ.append("  "+collectionType, debugFile);
  //       // IJ.append("  "+path, debugFile);
  //     }     
  //   }
  //   finalImage.show();
  //   IJ.saveAsTiff(finalImage, collection.getFolder().getAbsolutePath()+"composite.tiff");
  //   IJ.log("Composite image created");
  // }
}
