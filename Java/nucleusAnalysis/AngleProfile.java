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

public class AngleProfile {

	private NucleusBorderPoint[] array;
	private int angleProfileWindowSize   = 23;
	private int deltaSmoothingWindowSize = 5;
	private int blockCount               = 0;
	private FloatPolygon polygon;
	

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

	public FloatPolygon getPolygon(){
		return this.polygon;
	}

	public double[] getAngleProfile(){
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
        IJ.log("    Unequal array size");
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
}