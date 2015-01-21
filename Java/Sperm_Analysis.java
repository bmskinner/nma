import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.io.FileInfo;
import ij.io.FileOpener;
import ij.io.DirectoryChooser;
import ij.io.Opener;
import ij.io.OpenDialog;
import ij.io.RandomAccessStream;
import ij.measure.ResultsTable;
import ij.plugin.ChannelSplitter;
import ij.plugin.PlugIn;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.frame.RoiManager;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.StackConverter;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Polygon;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class Sperm_Analysis
  extends ImagePlus
  implements PlugIn
{

  private static final String[] fileTypes = {".tif", ".tiff", ".jpg"};
  private static final int SIGNAL_THRESHOLD = 70;
  private static final int NUCLEUS_THRESHOLD = 36;
  
  public void run(String paramString)  {

    DirectoryChooser localOpenDialog = new DirectoryChooser("Select directory of images...");
    String folderName = localOpenDialog.getDirectory();

    IJ.showStatus("Opening directory: " + folderName);
    IJ.log("Directory: "+folderName);

    File folder = new File(folderName);
    File[] listOfFiles = folder.listFiles();

    for (File file : listOfFiles) {
      if (file.isFile()) {

        String fileName = file.getName();

        for( String fileType : fileTypes){
          if( fileName.endsWith(fileType) ){
            IJ.showStatus("Opening file: " + fileName);
            IJ.log("File:    "+fileName);
            try {

              // open and process each image here
              String path = folderName+fileName;
              Opener localOpener = new Opener();
              ImagePlus localImagePlus = localOpener.openImage(path);             
              // handle the image
              processImage(localImagePlus, path);
              Thread.sleep(100);
              localImagePlus.close();

            } catch (InterruptedException ex) { 
                IJ.log("Error in sleeping");
            }
          }
        }
      }
    }
  }

  public void processImage(ImagePlus image, String path){

    // int[] xpoints = {10,100,100,10};
    // int[] ypoints = {10,100,10,100};
    // PolygonRoi testRoi = new PolygonRoi(xpoints,ypoints,4,Roi.POLYGON);
    // testRoi.setStroke(new java.awt.BasicStroke());
    // testRoi.setStrokeColor(new java.awt.Color(255,255,0));
    // image.setRoi(testRoi);
    RoiManager nucleiInImage;

    try {
      nucleiInImage = findNucleiInImage(image);

      Roi[] roiArray = nucleiInImage.getSelectedRoisAsArray();
      int i = 0;

      for(Roi roi : roiArray){

        analyseNucleus(roi, image, i, path);
        i++;

        try{
          Thread.sleep(100);
        } catch (InterruptedException ex) {

        }
      }

    } catch(NullPointerException e){
         IJ.log("No nuclei found");
    }
  }

  // within image, look for nuclei. Return as what? Array of Roi arrays?
  public RoiManager findNucleiInImage(ImagePlus image){

    double minSize = 500;
    double maxSize = 7000;
    double minCirc = 0.3;
    double maxCirc = 1;
    RoiManager manager = new RoiManager(true);

    // split out blue channel
    ChannelSplitter cs = new ChannelSplitter();
    ImagePlus[] channels = cs.split(image);
    ImagePlus blue = channels[2];
    
    // threshold
    ImageProcessor ip = blue.getChannelProcessor();
    ip.threshold(NUCLEUS_THRESHOLD);
    ip.invert();
    // blue.show();

    // run the particle analyser
    ResultsTable rt = new ResultsTable();
    ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.ADD_TO_MANAGER | ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES, ParticleAnalyzer.CENTER_OF_MASS | ParticleAnalyzer.AREA , rt, minSize, maxSize, minCirc, maxCirc);
    try {
      pa.setRoiManager(manager);
      boolean success = pa.analyze(blue);
      if(success){
        IJ.log("Found "+manager.getCount()+ " nuclei");
        // rt.show("Title");
      } else {
        IJ.log("Unable to perform particle analysis");
      }
    } catch(Exception e){
       IJ.log("Error: "+e);
    } finally {
      blue.close();
    }
   
   return manager;
  }

  public void analyseNucleus(Roi nucleus, ImagePlus image, int nucleusNumber, String path){
    
    // IJ.log("Processing nucleus");

    // make a copy of the nucleus only for saving out and processing
    image.setRoi(nucleus);
    image.copy();
    ImagePlus smallRegion = ImagePlus.getClipboard();
    nucleus.setLocation(0,0);
    smallRegion.setRoi(nucleus);

    // turn roi into RoiArray for later manipulation
    RoiArray roiArray = new RoiArray(nucleus.getPolygon());
    roiArray.setPath(path);
    roiArray.setNucleusNumber(nucleusNumber);

    // measure CoM, area, perimeter and feret in blue
    ResultsTable blueResults = findNuclearMeasurements(smallRegion, nucleus);
    // IJ.log("Found nuclear CoM at "+blueResults.getValue("XM", 0)+","+blueResults.getValue("YM", 0));

    // find tip - use the least angle method
    XYPoint spermTip = roiArray.findMinimumAngle();
    // IJ.log("Found sperm tip at "+spermTip.toString());
    
    //draw the sperm tip
    ImageProcessor ip = smallRegion.getProcessor();
    ip.setLineWidth(5);
    ip.setColor(Color.YELLOW);
    ip.drawDot(spermTip.getX(), spermTip.getY());

    File dir = new File(roiArray.getPathWithoutExtension());
    
    if (!dir.exists()) {
      try{
        dir.mkdir();
        IJ.log("Dir created");
      } catch(Exception e) {
        IJ.log("Failed to create dir: "+e);
        IJ.log("Saving to: "+dir.toString());
      }
    }
    IJ.saveAsTiff(smallRegion, roiArray.getPathWithoutExtension()+"\\"+roiArray.getNucleusNumber()+".tiff");
    ip.reset();

    // find CoM in colour
    // within nuclear roi, analyze particles in colour channels
    RoiManager   redSignalsInImage = findSignalInNucleus(smallRegion, 0);
    RoiManager greenSignalsInImage = findSignalInNucleus(smallRegion, 1);
    // get signal roi

  }

  public RoiManager findSignalInNucleus(ImagePlus image, int channel){

    double minSize = 400;
    double maxSize = 3000;
    RoiManager manager = new RoiManager(true);
    ChannelSplitter cs = new ChannelSplitter();
    ImagePlus[] channels = cs.split(image);
    ImagePlus imp = channels[channel];
    
    // threshold
    ImageProcessor ip = imp.getChannelProcessor();
    ip.threshold(SIGNAL_THRESHOLD);
    ip.invert();

    // run the particle analyser
    ResultsTable rt = new ResultsTable();
    ParticleAnalyzer pa = new ParticleAnalyzer(ParticleAnalyzer.ADD_TO_MANAGER, 
                                               0, rt, minSize, maxSize);
    try {
      pa.setRoiManager(manager);
      boolean success = pa.analyze(imp);
      if(success){
        IJ.log("Found "+manager.getCount()+ " signals in channel "+channel);
        // rt.show("Title");
      } else {
        IJ.log("Unable to perform signal analysis");
      }
    } catch(Exception e){
       IJ.log("Error: "+e);
    } finally {
      imp.close();
    }
    return manager;
  }

  public ResultsTable findNuclearMeasurements(ImagePlus imp, Roi roi){

    ChannelSplitter cs = new ChannelSplitter();
    ImagePlus[] channels = cs.split(imp);
    ImagePlus blueChannel = channels[2];

    blueChannel.setRoi(roi);
    double feretDiameter = roi.getFeretsDiameter();

    ResultsTable rt = new ResultsTable();

    Analyzer an = new Analyzer(blueChannel, Analyzer.CENTER_OF_MASS | Analyzer.PERIMETER | Analyzer.AREA | Analyzer.FERET, rt);
    an.measure();

    // IJ.log(rt.getColumnHeadings());
    // try {
    //   double nuclearArea = rt.getValue("Area", 0);
    //   double nuclearCoMX = rt.getValue("XM", 0);
    //   double nuclearCoMY = rt.getValue("YM", 0);
    //   double nuclearPerimeter = rt.getValue("Perim.", 0);

    //   IJ.log("Area: "+nuclearArea+"; CoM: "+nuclearCoMX+","+nuclearCoMY+"; Perimeter: "+nuclearPerimeter);
    // } catch(Exception e){
    //   IJ.log("Results error: "+e);
    // }
    return rt;
  }

  class XYPoint {
    private int x;
    private int y;

    public XYPoint (int x, int y){
      this.x = x;
      this.y = y;
    }

    public int getX(){
      return this.x;
    }
    public int getY(){
      return this.y;
    }

    public void setX(int x){
      this.x = x;
    }

    public void setY(int y){
      this.y = y;
    }

    public double getLengthTo(XYPoint a){

      // a2 = b2 + c2
      int dx = Math.abs(this.getX() - a.getX());
      int dy = Math.abs(this.getY() - a.getY());
      int dx2 = dx * dx;
      int dy2 = dy * dy;
      double length = Math.sqrt(dx2+dy2);
      return length;
    }

    public String toString(){
      return x+","+y;
    }
  }

  class RoiArray {
  
    private int points;
    private XYPoint[] array;
    private String imagePath;
    private int nucleusNumber;
    
    public RoiArray (int points) { // construct an empty array of given size
      this.array = new XYPoint[points]; // x and y for each point 
      this.points = points;
    }

    public RoiArray (Polygon polygon) { // construct from a polygon object

      this.array = new XYPoint[polygon.npoints];
      for(int i=0; i<polygon.npoints; i++){
        array[i] = new XYPoint(polygon.xpoints[i],polygon.ypoints[i]);
      }
      this.points = polygon.npoints;
    }

    // public int getX(int index){
    //   return array[index].getX();
    // }

    // public int getY(int index){
    //   return array[index].getY();
    // }

    public void setX(int index, int x){
      this.array[index].setX(x);
    }

    public void setY(int index, int y){
      this.array[index].setY(y);
    }

    public void setPath(String path){
      this.imagePath = path;
    }

    public void setNucleusNumber(int n){
      this.nucleusNumber = n;
    }

    public int getPoints(){
      return this.points;
    }

    public XYPoint getPoint(int index){
      return this.array[index];
    }

    public String getPath(){
      return this.imagePath;
    }

    public String getPathWithoutExtension(){
      
      String extension = "";
      String trimmed = "";

      int i = this.imagePath.lastIndexOf('.');
      if (i > 0) {
          extension = this.imagePath.substring(i+1);
          trimmed = this.imagePath.substring(0,i);
      }
      return trimmed;
    }    

    public int getNucleusNumber(){
      return this.nucleusNumber;
    }

    public String getPathAndNumber(){
      return this.imagePath+"\\"+this.nucleusNumber;
    }

    public RoiArray flipROI(){

      // reverse the array and create a new roi

      RoiArray newArray = new RoiArray(this.points);
      int j=0;
      for(int i=this.getPoints()-1;i>=0;i--){

        XYPoint point = this.array[i];
        int x = point.getX();
        int y = point.getY();
        newArray.setX(j, x);
        newArray.setY(j, y);
        j++;
      }
      return newArray;
    }

    public RoiArray trimROI(int percent){

      // find the number of points to include for a given percentage
      // create a new RoiArray and copy the current RoiArray values to it up to the endpoint
      int end = this.points*(percent/100);

      // int end = Math.floor(this.points*(percent/100)); // fetch first x% of signals
      // int end = d.intValue();

      RoiArray newArray = new RoiArray(end);

      System.arraycopy(this.array, 0, newArray, 0, end); // copy over the 0 to end values

      // for(int i=0;i<end;i++){
      //  newArray.setX(i, this.getX(i));
      //  newArray.setY(i, this.getY(i));
      // }
      return newArray;
    }

    public RoiArray shuffleROI(){

      // Look for largest discontinuity between points
      // Set these coordinates to the ROI end
          
      double max_distance = 0;
      int max_i = 0;
      
      // find the two most divergent points
      for(int i=0;i<this.getPoints();i++){
      
        XYPoint pointA = this.array[i];
        XYPoint pointB  = i == this.getPoints()-1 
                ? this.array[0] 
                : this.array[i+1]; // check last array element against first

        // if(i==this.points-1){ // check last against first
        //  XYPoint pointB = this.array[0];
          
        // } else { // otherwise check to the next in array
        //  XYPoint pointB = this.array[i+1];
        // }
        double distance = pointA.getLengthTo(pointB);
        
        if(distance > max_distance){
          max_distance = distance;
          max_i = i;
        }
      }

      // we now have the position just before the discontinuity
      // chop the array in two and reassemble in the correct order
      RoiArray newArray = new RoiArray(this.getPoints()); // new array, empty
      System.arraycopy(this.array, max_i+1, newArray, 0, this.getPoints()-max_i); // copy over the max_i to end values
      System.arraycopy(this.array, 0, newArray, max_i+1, max_i); // copy over index 0 to max_i

      return newArray;
    }

    public double findAngleBetweenPoints(int index, int window){

      // from the given index, draw a line between this point, and the points window before and window after
      // measure the angle between these points

      // wrap the array
      int indexBefore = index < window
                      ? this.getPoints() - (window-index)
                      : index - window;

      int indexAfter = index + window > this.getPoints()-1
                     ? Math.abs(this.getPoints() - (index+window))
                     : index + window;

      XYPoint pointBefore = this.getPoint(indexBefore);
      XYPoint pointAfter = this.getPoint(indexAfter);
      XYPoint point = this.getPoint(index);

      // make a segmented line
      int[] xpoints = {pointBefore.getX(), point.getX(),pointAfter.getX()};
      int[] ypoints = {pointBefore.getY(), point.getY(),pointAfter.getY()};
      PolygonRoi roi = new PolygonRoi(xpoints, ypoints, 3, Roi.ANGLE);

      // measure the angle of the line
      double angle = roi.getAngle();
      return angle;
    }

    public XYPoint findMinimumAngle(){

      double minAngle = 180.0;
      int minIndex = 0;
      for(int i=0; i<this.points;i++){

          // use a window size of 25 for now
          double angle = findAngleBetweenPoints(i, 23);
          if(angle<minAngle){
            minAngle = angle;
            minIndex = i;
          }

          // IJ.log(i+" \t "+angle+" \t "+minAngle+"  "+minIndex);
      }
      return this.array[minIndex];

    }
  }
}
