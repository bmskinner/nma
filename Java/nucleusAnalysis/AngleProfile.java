/*
	ANGLE_PROFILE

	Generate angle profiles around a nucleus.
	These are the values that will be used to 
	orient a nucleus
*/

package nucleusAnalysis;	

import ij.IJ;
import ij.process.FloatPolygon;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import java.util.*;

public class AngleProfile {

	private NucleusBorderPoint[] array;
	private int angleProfileWindowSize      = 23;
	private int deltaSmoothingWindowSize    = 5;
	private int blockCount                  = 0;
  private int minimaAndMaximaLookupWindow = 5;

  private int minimaCount = 0;
  private int maximaCount = 0;
	private FloatPolygon polygon;

  private double medianAngle = 0;
	

	public AngleProfile(FloatPolygon p){
		this.polygon = p;
		this.array = new NucleusBorderPoint[p.npoints];
		for(int i=0; i<this.polygon.npoints; i++){
          array[i] = new NucleusBorderPoint( p.xpoints[i], p.ypoints[i]);
        }
		updateAngleCalculations();
	}

	public NucleusBorderPoint[] getBorderPointArray(){
		return this.array;
	}

	public void updateAngleCalculations(){
		calculateAngles();
		calculateDeltaAngles();
		calculateSmoothedDeltaAngles();
		countConsecutiveDeltas();
    updatePointsWithBlockCount();
    detectLocalMinima();
    detectLocalMaxima();
    calculateMedianAngle();
	}

	public int getAngleProfileWindowSize(){
		return this.angleProfileWindowSize;
	}

	public void setAngleProfileWindowSize(int i){
		int old = this.angleProfileWindowSize;
		this.angleProfileWindowSize = i;
		if(old != i){
			calculateAngles(); // trigget a recalc on change
		}
	}

	public int getBlockCount(){
		return this.blockCount;
	}

	public int size(){
		return array.length;
	}

  public int getIndexOfPoint(NucleusBorderPoint p){

    int index = -1;
    for (int i = 0; (i < array.length) && (index == -1); i++) {
        if (array[i] == p) {
            index = i;
        }
    }
    return index;
  }

  public double getMedianInteriorAngle(){
    return this.medianAngle;
  }

	public NucleusBorderPoint getBorderPoint(int i){
      return this.array[i];
    }

	public int getDeltaSmoothingWindowSize(){
		return this.deltaSmoothingWindowSize;
	}

	public void setDeltaSmoothingWindowSize(int i){
		int old = this.deltaSmoothingWindowSize;
		this.deltaSmoothingWindowSize = i;
		if(old != i){
			calculateSmoothedDeltaAngles();
		}
	}

  public void setMinimaAndMaximaLookupWindow(int i){
    this.minimaAndMaximaLookupWindow = i;
  }

  public NucleusBorderPoint getPointWithMinimumAngle(){

    double minAngle = 180.0;
    int minIndex = 0;
    for(int i=0; i<array.length;i++){

      double angle = array[i].getInteriorAngle();
      if(angle<minAngle){
        minAngle = angle;
        minIndex = i;
      }
    }
    return array[minIndex];
  }

  public NucleusBorderPoint getPointWithMaximumAngle(){

    double maxAngle = 0.0;
    int maxIndex = 0;
    for(int i=0; i<array.length;i++){

      double angle = array[i].getInteriorAngle();
      if(angle>maxAngle){
        maxAngle = angle;
        maxIndex = i;
      }
    }
    return array[maxIndex];
  }

  public int getMinimaAndMaximaLookupWindow(){
    return this.minimaAndMaximaLookupWindow;
  }

	public FloatPolygon getPolygon(){
		return this.polygon;
	}

	public double[] getAngleArray(){
      double[] d = new double[this.array.length]; // allow the first and last element to be duplicated
      for(int i=0;i<this.array.length;i++){
        d[i] = this.array[i].getInteriorAngle();
      }
      return d;
    }

	/*
    For a given index in the smoothed angle array: 
      Draw a line between this point, and the points <window> ahead and <window> behind.
      Measure the angle between these points and store as minAngle
      Determine if the angle lies inside or outside the shape. Adjust the angle to always report the interior angle.
      Store interior angle as interiorAngle.     
  */
  private void calculateAngles(){

    // IJ.log("Calculating angles...");
    for(int i=0; i<array.length;i++){
      // use a window size of 25 for now
    	int indexBefore = wrapIndex(i - this.getAngleProfileWindowSize(), this.array.length);
    int indexAfter  = wrapIndex(i + this.getAngleProfileWindowSize(), this.array.length);

    NucleusBorderPoint pointBefore = this.getBorderPoint(indexBefore);
    NucleusBorderPoint pointAfter = this.getBorderPoint(indexAfter);
    NucleusBorderPoint point = this.getBorderPoint(i);


    double angle = findAngleBetweenXYPoints(pointBefore, point, pointAfter);
    this.array[i].setMinAngle(angle);

    // find the halfway point between the first and last points.
      // is this within the roi?
      // if yes, keep min angle as interior angle
      // if no, 360-min is interior
    double midX = (pointBefore.getX()+pointAfter.getX())/2;
    double midY = (pointBefore.getY()+pointAfter.getY())/2;

    if(this.getPolygon().contains( (float) midX, (float) midY)){
      this.array[i].setInteriorAngle(angle);
    } else {
      this.array[i].setInteriorAngle(360-angle);
    }
    }
  }

  private void calculateDeltaAngles(){

  	// calculate deltas
  	// IJ.log("Calculating deltas...");
  	double angleDelta = 0;
  	for(int i=0; i<this.array.length;i++){

     	NucleusBorderPoint prevPoint = this.array[ wrapIndex(i-1, this.array.length) ];
     	NucleusBorderPoint nextPoint = this.array[ wrapIndex(i+1, this.array.length) ];

     	angleDelta = nextPoint.getInteriorAngle() - prevPoint.getInteriorAngle();
	    this.array[i].setInteriorAngleDelta(angleDelta);
    }
  }

  private void calculateSmoothedDeltaAngles(){

  	// IJ.log("Smoothing deltas...");
    double smoothedDelta = 0;
    for(int i=0; i<this.array.length;i++){

      smoothedDelta = this.array[i].getInteriorAngleDelta();

      // handle array wrapping for arbitrary length smoothing
      for(int j=1;j<=(int)(this.getDeltaSmoothingWindowSize()-1)/2;j++){

      	smoothedDelta += ( this.array[ wrapIndex(i-j, this.array.length) ].getInteriorAngleDelta() +
	      		this.array[ wrapIndex(i+j, this.array.length) ].getInteriorAngleDelta());
      }
      smoothedDelta = smoothedDelta / this.deltaSmoothingWindowSize;
	  this.array[i].setInteriorAngleDeltaSmoothed(smoothedDelta);
    }
  }

  private void countConsecutiveDeltas(){
  	
  	int blockNumber = 0;
  	for(int i=0;i<this.array.length;i++){ // iterate over every point in the array

  		int count = 0;
  		if(this.array[i].getInteriorAngleDeltaSmoothed() < 1){ // if the current NucleusBorderPoint has an angle < 1, move on
  			this.array[i].setConsecutiveBlocks(0);
  			this.array[i].setBlockNumber(0);
        		this.array[i].setPositionWithinBlock(0);
  			continue;
  		}

        int positionInBlock = i==0
                            ? 0
                            : this.array[i-1].getPositionWithinBlock() + 1; // unless first element of array, use prev value++
    		
        for(int j=1;j<this.array.length-i;j++){ // next point on until up to end of array
    		if(this.array[i+j].getInteriorAngleDeltaSmoothed() >= 1){
    			count++;
    		} else {
    			break; // stop counting on first point below 1 degree delta
    		}
    	}
    		
    	this.array[i].setConsecutiveBlocks(count);
        this.array[i].setPositionWithinBlock(positionInBlock);
    	if(i>0){
	    	if(this.array[i-1].getBlockNumber()==0){
	    			blockNumber++;
	    	}
	    }
    	this.array[i].setBlockNumber(blockNumber);
  		
  	}
  	this.blockCount = blockNumber;
  }

  /* 
  For a given delta block number in the smoothed NucleusBorderPoint array:
  Get all the points in the array that have the same block number
  Input: int block number
  Return: NucleusBorderPoint[] all the points in the block
  */
  public NucleusBorderPoint[] getBlockOfBorderPoints(int b){

    int count = countPointsWithBlockNumber(b);
    NucleusBorderPoint[] result = new NucleusBorderPoint[count];
    
    int j=0;
    for(int i=0; i<this.array.length;i++){

      if(this.array[i].getBlockNumber() == b){
        result[j] = this.array[i];
        j++;
      }

    }
    return result;
  }

  /* 
    For a given delta block number in the smoothed NucleusBorderPoint array:
    Count the number of points in the array that have the same block number
  */
  private int countPointsWithBlockNumber(int b){
    int count = 0;
    for(int i=0; i<this.array.length;i++){

      if(this.array[i].getBlockNumber() == b){
        count++;
      }
    }
    return count;
  }   

  /* 
 	  For each point in the smoothed NucleusBorderPoint array:
    Find how many points lie within the angle delta block
    Add this number to the blockSize variable of the point
  */
  public void updatePointsWithBlockCount(){

    for(int i=0; i<this.array.length;i++){

      int p = countPointsWithBlockNumber(this.array[i].getBlockNumber());
      this.array[i].setBlockSize(p);

    }
  }   

  private int wrapIndex(int i, int length){
    if(i<0)
      i = length + i; // if i = -1, in a 200 length array,  will return 200-1 = 199
    if(Math.floor(i / length)>0)
      i = i - ( ((int)Math.floor(i / length) )*length); // if i is 250 in a 200 length array, will return 250-(200*1) = 50
    // if i is 201 in a 200 length array, will return 201 - floor(201/200)=1 * 200 = 1
    // if 200 in 200 length array: 200/200 = 1; 200-200 = 0

    if(i<0 || i>length){
    	IJ.log("Warning: array out of bounds: "+i);
    }
    
    return i;
  }

  /*
    Given three XYPoints, measure the angle a-b-c
      a   c
       \ /
        b
  */
  private double findAngleBetweenXYPoints(XYPoint a, XYPoint b, XYPoint c){

    float[] xpoints = { (float) a.getX(), (float) b.getX(), (float) c.getX()};
    float[] ypoints = { (float) a.getY(), (float) b.getY(), (float) c.getY()};
    PolygonRoi roi = new PolygonRoi(xpoints, ypoints, 3, Roi.ANGLE);
   return roi.getAngle();
  }

  /*
    Change the smoothed array order to put the selected index at the beginning
    only works for smoothed array - indexes are different for normal array
    Input: int the index to move to the start
  */
  public void moveIndexToArrayStart(int i){

    // copy the array to refer to
    NucleusBorderPoint[] tempSmooth = new NucleusBorderPoint[this.array.length];
    System.arraycopy(this.array, 0, tempSmooth, 0 , this.array.length);
   
    System.arraycopy(tempSmooth, i, this.array, 0 , this.array.length-i); // copy over the i to end values
    System.arraycopy(tempSmooth, 0, this.array, this.array.length-i, i); // copy over index 0 to i
   
    if(tempSmooth.length != this.array.length){
      IJ.log("Unequal array size");
    }
  }  

  public void reverseAngleProfile(){

    NucleusBorderPoint tmp;
    for (int i = 0; i < this.array.length / 2; i++) {
        tmp = this.array[i];
        this.array[i] = this.array[this.array.length - 1 - i];
        this.array[this.array.length - 1 - i] = tmp;
    }
  }  

  /*
  	Flip the array around an X position
  */
  public void flipXAroundPoint(XYPoint p){

    double xCentre = p.getX();
    for(int i = 0; i<this.array.length;i++){
      double dx = xCentre - this.array[i].getX();
      double xNew = xCentre + dx;
      this.array[i].setX(xNew);
    }
  }

  /*
    Functions for detecting minima within the profiles
  */

  /*
    Retrieves an NucleusBorderPoint array of the points designated as local minima.
    If the local minimum detection has not yet been run, calculates local minima
  */
  public NucleusBorderPoint[] getLocalMinima(){

    NucleusBorderPoint[] newArray = new NucleusBorderPoint[this.minimaCount];
    int j = 0;

    try{
      for (int i=0; i<array.length; i++) {
        if(array[i].isLocalMin()){
          newArray[j] = array[i];
          j++;
        }
      }
    } catch(Exception e){
      IJ.log("Error in fetching minima: "+e);
    }
    return newArray;
  }

  /*
    Retrieves an NucleusBorderPoint array of the points designated as local maxima.
    If the local maximum detection has not yet been run, calculates local maxima
  */
  public NucleusBorderPoint[] getLocalMaxima(){

    NucleusBorderPoint[] newArray = new NucleusBorderPoint[this.maximaCount];
    int j = 0;

    try{  
      for (int i=0; i<array.length; i++) {
        if(array[i].isLocalMax()){
          newArray[j] = this.getBorderPoint(i);
          j++;
        }
      }
    } catch(Exception e){
      IJ.log("Error in fetching maxima: "+e);
    }
    return newArray;
  }

  /*
    For each point in the smoothed angle array, test for a local minimum.
    The angles of the points <minimaLookupDistance> ahead and behind are checked.
    Each should be greater than the angle before.
    One exception is allowed, to account for noisy data.
  */
  private void detectLocalMinima(){
    // go through angle array (with tip at start)
    // look at 1-2-3-4-5 points ahead and behind.
    // if all greater, local minimum
    double[] prevAngles = new double[this.getMinimaAndMaximaLookupWindow()]; // slots for previous angles
    double[] nextAngles = new double[this.getMinimaAndMaximaLookupWindow()]; // slots for next angles

    int count = 0;

    for (int i=0; i<array.length; i++) { // for each position in sperm

      // go through each lookup position and get the appropriate angles
      for(int j=0;j<prevAngles.length;j++){

        int prev_i = i-(j+1); // the index j+1 before i
        int next_i = i+(j+1); // the index j+1 after i

        // handle beginning of array - wrap around
        if(prev_i < 0){
          prev_i = array.length + prev_i; // length of array - appropriate value
        }

        // handle end of array - wrap
        if(next_i >= array.length){
          next_i = next_i - array.length;
        }

        // fill the lookup array
        prevAngles[j] = array[prev_i].getInteriorAngle();
        nextAngles[j] = array[next_i].getInteriorAngle();
      }
      
      // with the lookup positions, see if minimum at i
      // return a 1 if all higher than last, 0 if not
      // prev_l = 0;
      boolean ok = true;
      for(int l=0;l<prevAngles.length;l++){

        // for the first position in prevAngles, compare to the current index
        if(l==0){
          if(prevAngles[l] < array[i].getInteriorAngle() || nextAngles[l] < array[i].getInteriorAngle()){
            ok = false;
          }
        } else { // for the remainder of the positions in prevAngles, compare to the prior prevAngle
          
          if(prevAngles[l] < prevAngles[l-1] || nextAngles[l] < nextAngles[l-1]){
            ok = false;
          }
        }

        if( array[i].getInteriorAngle()-180 > -20){ // ignore any values close to 180 degrees
          ok = false;
        }
      }

      if(ok){
        count++;
      }

      // put oks into array to put into multiarray
      array[i].setLocalMin(ok);
    }
    // this.minimaCalculated = true;
    this.minimaCount =  count;
  }

  /*
    For each point in the smoothed angle array, test for a local maximum.
    The angles of the points <minimaLookupDistance> ahead and behind are checked.
      *Note that this uses the same variable as detectLocalMinima()*
    Each should be lower than the angle before.
    One exception is allowed, to account for noisy data.
  */
  private void detectLocalMaxima(){
    // go through interior angle array (with tip at start)
    // look at 1-2-3-4-5 array ahead and behind.
    // if all lower, local maximum

    double[] prevAngles = new double[this.getMinimaAndMaximaLookupWindow()]; // slots for previous angles
    double[] nextAngles = new double[this.getMinimaAndMaximaLookupWindow()]; // slots for next angles

    int count = 0;

    for (int i=0; i<array.length; i++) { // for each position in sperm

      // go through each lookup position and get the appropriate angles
      for(int j=0;j<prevAngles.length;j++){

        int prev_i = wrapIndex( i-(j+1) , array.length );
        int next_i = wrapIndex( i+(j+1) , array.length );

        // fill the lookup array
        prevAngles[j] = array[prev_i].getInteriorAngle();
        nextAngles[j] = array[next_i].getInteriorAngle();
      }
      
      // with the lookup positions, see if maximum at i
      // return true if all lower than last, false if not
      // prev_l = 0;
      boolean ok = true;
      boolean ignoreOne = true; // allow a single value to be out of place (account for noise in pixel data)
      for(int k=0;k<prevAngles.length;k++){

        // not ok if the outer entries are not higher than inner entries
        if(k==0){
          if( prevAngles[k] > array[i].getInteriorAngle() ||
              nextAngles[k] > array[i].getInteriorAngle() ){

            if( !ignoreOne){
              ok = false;
            }
            ignoreOne = false;
          }
        } else {
          
          if(  prevAngles[k] > prevAngles[k-1] || nextAngles[k] > nextAngles[k-1] )  {
            
            if( !ignoreOne){
              ok = false;
            }
            
            ignoreOne = false;
          }
        }

        // // we want the angle of a maximum to be higher than the median angle of the array set
        // // if( this.getBorderPointArray()[i].getInteriorAngle()-180 < -10){
        // if( array[i].getInteriorAngle() < this.medianAngle){
        //   ok = false;
        // }
      }

      if(ok){
        count++;
      }

      // put oks into array to put into multiarray
      array[i].setLocalMax(ok);
    }
    this.maximaCount =  count;
  }

  public double[] getInteriorAngles(){

    double[] points = new double[array.length];

    for(int j=0;j<array.length;j++){
        points[j] = array[j].getInteriorAngle();
    }
    return points;
  }

  /*
    For the interior angles in the smoothed angle array:
      Calculate the median angle in the array.
    Stores in medianAngle
  */    
  private void calculateMedianAngle() {

      double[] m = new double[array.length];
      for(int i = 0; i<array.length; i++){
        m[i] = this.array[i].getInteriorAngle();
      }
      Arrays.sort(m);

      int middle = m.length/2;
      if (m.length%2 == 1) {
          this.medianAngle = m[middle];
      } else {
          this.medianAngle = (m[middle-1] + m[middle]) / 2.0;
      }
  }
}