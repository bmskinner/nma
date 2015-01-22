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
import ij.process.FloatPolygon;
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
  private static final int NUCLEUS_THRESHOLD = 40;
  
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
         IJ.log("Error:"+e);
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
    RoiArray roiArray = new RoiArray(nucleus);
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
    ip.drawDot(spermTip.getXAsInt(), spermTip.getYAsInt());

    ip.setColor(Color.GREEN);
    p.setLineWidth(3);
    IJ.log("Found "+roiArray.minimaCount+" local minima");
    XYPoint[] minima = roiArray.getLocalMinima();
    for (XYPoint example : minima){
         ip.drawDot(example.getXAsInt(), example.getYAsInt());
    }

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


    roiArray.printAngleInfo();
    // find CoM in colour
    // within nuclear roi, analyze particles in colour channels
    // RoiManager   redSignalsInImage = findSignalInNucleus(smallRegion, 0);
    // RoiManager greenSignalsInImage = findSignalInNucleus(smallRegion, 1);
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
    private double x;
    private double y;
    private double minAngle;
    private double interiorAngle; // depends on whether the min angle is inside or outside the shape
    private int index; // keep the original index position in case we need to change
    private boolean localMin; // is this angle a local minimum
  
    public XYPoint (double x, double y){
      this.x = x;
      this.y = y;
    }

    public double getX(){
      return this.x;
    }
    public double getY(){
      return this.y;
    }

    public int getXAsInt(){
      Double obj = new Double(this.x);
      int i = obj.intValue();
      return i;
    }

    public int getYAsInt(){
      Double obj = new Double(this.y);
      int i = obj.intValue();
      return i;
    }

    public void setX(double x){
      this.x = x;
    }

    public void setY(double y){
      this.y = y;
    }

    public int getIndex(){
      return this.index;
    }

    public double getMinAngle(){
      return this.minAngle;
    }

    public void setIndex(int i){
      this.index = i;
    }

    public void setMinAngle(double d){
      this.minAngle = d;
    }

    public double getInteriorAngle(){
      return this.interiorAngle;
    }

    public void setInteriorAngle(double d){
      this.interiorAngle = d;
    }

    public void setLocalMin(boolean b){
      this.localMin = b;
    }

    public boolean isLocalMin(){
      return this.localMin;
    }

    public double getLengthTo(XYPoint a){

      // a2 = b2 + c2
      double dx = Math.abs(this.getX() - a.getX());
      double dy = Math.abs(this.getY() - a.getY());
      double dx2 = dx * dx;
      double dy2 = dy * dy;
      double length = Math.sqrt(dx2+dy2);
      return length;
    }

    public String toString(){
      return x+","+y;
    }
  }


  class RoiArray {
  
    private int nucleusNumber;
    private int windowSize = 23; // default size, can be overridden if needed
    private int minimaCount; // the number of local minima detected in the array
    private int length;
    private int smoothLength;
    private int minimaLookupDistance = 6;

    private XYPoint[] array; // this will hold the index and angle as well
    private XYPoint[] smoothedArray; // this allows the same calculations on interpolated array
    
    private String imagePath;

    private boolean minimaCalculated = false; // has detectLocalMinima been run
    private boolean anglesCalculated = false; // has makeAngleArray been run
    
    private Roi roi; // the original ROI
    private Polygon polygon; // the source of the array

    private FloatPolygon smoothedPolygon; // hold the smoothed polygon data
    
    // DEPRECATED - WE NEED THE POLYGON FOR ANGLE CONSTRUCTION
    // public RoiArray (int points) { // construct an empty array of given size
    //   this.array = new XYPoint[points]; // x and y for each point 
    //   this.points = points;
    //   this.minimaCalculated = false;
    // }

    public RoiArray (Roi roi) { // construct from an roi

      // get the polygon from the roi
      this.polygon = roi.getPolygon();
      this.array = new XYPoint[this.polygon.npoints];
      this.length = this.array.length;
      for(int i=0; i<this.polygon.npoints; i++){
        array[i] = new XYPoint(this.polygon.xpoints[i],this.polygon.ypoints[i]);
      }

      // interpolate and smooth the roi
      this.smoothedPolygon = roi.getInterpolatedPolygon(1,true); // pixels 1 apart, smoothed
      this.smoothedArray = new XYPoint[this.smoothedPolygon.npoints];
      this.smoothLength = this.smoothedArray.length;
      for(int i=0; i<this.smoothedPolygon.npoints; i++){
        smoothedArray[i] = new XYPoint(this.smoothedPolygon.xpoints[i],this.smoothedPolygon.ypoints[i]);
      }
      
    }

    public void setWindowSize(int i){
    	this.windowSize = i;
    }

    public int getWindowSize(){
    	return this.windowSize;
    }

    public void setPath(String path){
      this.imagePath = path;
    }

    public void setNucleusNumber(int n){
      this.nucleusNumber = n;
    }

    public XYPoint getPoint(int i){
      return this.array[i];
    }

    public XYPoint getSmoothedPoint(int i){
      // XYPoint p = new XYPoint(this.smoothedPolygon.xpoints[i], this.smoothedPolygon.ypoints[i]);
      return this.smoothedArray[i];
    }

    public String getPath(){
      return this.imagePath;
    }

    public String getDirectory(){
      File f = new File(this.imagePath);
      return f.getParent();
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

    // public RoiArray flipROI(){

    //   // reverse the array and create a new roi

    //   RoiArray newArray = new RoiArray(this.points);
    //   int j=0;
    //   for(int i=this.getPoints()-1;i>=0;i--){

    //     XYPoint point = this.array[i];
    //     int x = point.getX();
    //     int y = point.getY();
    //     newArray.setX(j, x);
    //     newArray.setY(j, y);
    //     j++;
    //   }
    //   return newArray;
    // }

    // public RoiArray trimROI(int percent){

    //   // find the number of points to include for a given percentage
    //   // create a new RoiArray and copy the current RoiArray values to it up to the endpoint
    //   int end = this.points*(percent/100);

    //   // int end = Math.floor(this.points*(percent/100)); // fetch first x% of signals
    //   // int end = d.intValue();

    //   RoiArray newArray = new RoiArray(end);

    //   System.arraycopy(this.array, 0, newArray, 0, end); // copy over the 0 to end values

    //   // for(int i=0;i<end;i++){
    //   //  newArray.setX(i, this.getX(i));
    //   //  newArray.setY(i, this.getY(i));
    //   // }
    //   return newArray;
    // }

    // public RoiArray shuffleROI(){

    //   // Look for largest discontinuity between points
    //   // Set these coordinates to the ROI end
          
    //   double max_distance = 0;
    //   int max_i = 0;
      
    //   // find the two most divergent points
    //   for(int i=0;i<this.getPoints();i++){
      
    //     XYPoint pointA = this.array[i];
    //     XYPoint pointB  = i == this.getPoints()-1 
    //             ? this.array[0] 
    //             : this.array[i+1]; // check last array element against first

    //     // if(i==this.points-1){ // check last against first
    //     //  XYPoint pointB = this.array[0];
          
    //     // } else { // otherwise check to the next in array
    //     //  XYPoint pointB = this.array[i+1];
    //     // }
    //     double distance = pointA.getLengthTo(pointB);
        
    //     if(distance > max_distance){
    //       max_distance = distance;
    //       max_i = i;
    //     }
    //   }

    //   // we now have the position just before the discontinuity
    //   // chop the array in two and reassemble in the correct order
    //   RoiArray newArray = new RoiArray(this.getPoints()); // new array, empty
    //   System.arraycopy(this.array, max_i+1, newArray, 0, this.getPoints()-max_i); // copy over the max_i to end values
    //   System.arraycopy(this.array, 0, newArray, max_i+1, max_i); // copy over index 0 to max_i

    //   return newArray;
    // }

    public void findAngleBetweenPoints(int index, int window){

      // from the given index, draw a line between this point, and the points window before and window after
      // measure the angle between these points

      // wrap the array
      int indexBefore = index < window
                      ? this.smoothLength - (window-index)
                      : index - window;

      int indexAfter = index + window > this.smoothLength-1
                     ? Math.abs(this.smoothLength - (index+window))
                     : index + window;

      XYPoint pointBefore = this.getSmoothedPoint(indexBefore);
      XYPoint pointAfter = this.getSmoothedPoint(indexAfter);
      XYPoint point = this.getSmoothedPoint(index);

      // make a segmented line
      float[] xpoints = { (float) pointBefore.getX(), (float) point.getX(), (float) pointAfter.getX()};
      float[] ypoints = { (float) pointBefore.getY(), (float) point.getY(), (float) pointAfter.getY()};
      PolygonRoi roi = new PolygonRoi(xpoints, ypoints, 3, Roi.ANGLE);


      // measure the angle of the line
      double angle = roi.getAngle();

      // find the halfway point between the first and last points.
      // is this within the roi?
      // if yes, keep min angle as interior angle
      // if no, 360-min is interior
      double midX = (pointBefore.getX()+pointAfter.getX())/2;
      double midY = (pointBefore.getY()+pointAfter.getY())/2;

      this.smoothedArray[index].setMinAngle(angle);
      if(this.smoothedPolygon.contains( (float) midX, (float) midY)){
        this.smoothedArray[index].setInteriorAngle(angle);
      } else {
        this.smoothedArray[index].setInteriorAngle(360-angle);
      }
    }

    // Make an angle array for the current coordinates in the XYPoint array
    // Will need to be rerun on each index order change
    public void makeAngleArray(){
    	// go through points
    	// find angle
    	// assign to angle array

	    for(int i=0; i<this.smoothLength;i++){

	      // use a window size of 25 for now
	      findAngleBetweenPoints(i, this.getWindowSize());
        this.smoothedArray[i].setIndex(i);
	    }
      IJ.log("Measured angles with window size "+this.windowSize);
    }

    public XYPoint findMinimumAngle(){

      if(!this.anglesCalculated){
        this.makeAngleArray();
      }
      if(!this.minimaCalculated){
        this.detectLocalMinima();
      }
      double minAngle = 180.0;
      int minIndex = 0;
      for(int i=0; i<this.length;i++){

          // use a window size of 25 for now
          double angle = this.smoothedArray[i].getMinAngle();
          if(angle<minAngle){
            minAngle = angle;
            minIndex = i;
          }

          // IJ.log(i+" \t "+angle+" \t "+minAngle+"  "+minIndex);
      }
      return this.smoothedArray[minIndex];
    }

    // retrieve an array of the points designated as local minima
    public XYPoint[] getLocalMinima(){

      XYPoint[] newArray = new XYPoint[this.minimaCount];
      if(!this.minimaCalculated){
        this.detectLocalMinima();
      }

      int j = 0;
      for (int i=0; i<this.smoothLength; i++) {
        if(this.smoothedArray[i].isLocalMin()){
          newArray[j] = this.smoothedArray[i];
          j++;
        }
      }

      IJ.log("Detected "+j+" local minima with lookup size "+this.minimaLookupDistance);
      return newArray;
    }

    public void detectLocalMinima(){
      // go through angle array (with tip at start)
      // look at 1-2-3-4-5 points ahead and behind.
      // if all greater, local minimum
      
      double[] prevAngles = new double[this.minimaLookupDistance]; // slots for previous angles
      double[] nextAngles = new double[this.minimaLookupDistance]; // slots for next angles

      int count = 0;

      for (int i=0; i<this.smoothLength; i++) { // for each position in sperm

        // go through each lookup position and get the appropriate angles
        for(int j=0;j<prevAngles.length;j++){

          int prev_i = i-(j+1);
          int next_i = i+(j+1);

          // handle beginning and end of array - wrap around
          if(prev_i < 0){
            prev_i = this.smoothLength + prev_i;
          }
          if(next_i >= this.smoothLength){
            next_i = next_i - this.smoothLength;
          }

          // fill the lookup array
          prevAngles[j] = this.smoothedArray[prev_i].getMinAngle();
          nextAngles[j] = this.smoothedArray[next_i].getMinAngle();
        }
        
        // with the lookup positions, see if minimum at i
        // return a 1 if all higher than last, 0 if not
        // prev_l = 0;
        boolean ok = true;
        for(int l=0;l<prevAngles.length;l++){

          // not ok if the outer entries are not higher than inner entries
          if(l==0){
            if(prevAngles[l] < this.smoothedArray[i].getMinAngle() || nextAngles[l] < this.smoothedArray[i].getMinAngle()){
              ok = false;
            }
          } else {
            
            if(prevAngles[l] < prevAngles[l-1] || nextAngles[l] < nextAngles[l-1]){
              ok = false;
            }
          }
        }
        if(ok){
          count++;
        }

        // put oks into array to put into multiarray
        smoothedArray[i].setLocalMin(ok);
      }
      this.minimaCalculated = true;
      this.minimaCount =  count;
    }

    public void printAngleInfo(){

      String path = this.getPathWithoutExtension()+"\\"+this.getNucleusNumber()+".log";
      
       IJ.append("X\tY", path);
      for(int i=0;i<this.length;i++){
        // IJ.log(array[i].getXAsInt()+"  "+array[i].getYAsInt()+"  "+this.getSmoothedPoint(i).getX()+"  "+this.getSmoothedPoint(i).getY()+"  "+array[i].getInteriorAngle()+"  "+array[i].getMinAngle()+"  "+array[i].isLocalMin());
        IJ.append(array[i].getXAsInt()+"\t"+array[i].getYAsInt(), path);
      
      }


      IJ.append("SX\tSY\tFX\tFY\tIA\tMA\tLM", path);
      
      for(int i=0;i<this.smoothLength;i++){
        // IJ.log(smoothedPolygon.xpoints[i]+"  "+smoothedPolygon.ypoints[i]);
        IJ.append(smoothedArray[i].getXAsInt()+"\t"+smoothedArray[i].getYAsInt()+"\t"+smoothedArray[i].getX()+"\t"+smoothedArray[i].getY()+"\t"+smoothedArray[i].getInteriorAngle()+"  "+smoothedArray[i].getMinAngle()+"  "+smoothedArray[i].isLocalMin(), path);
      }
    }
  }
}
