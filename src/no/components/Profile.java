/*
	DISTANCE PROFILE

	Holds the distances from the centre of mass
*/

package no.components;

import ij.IJ;
import no.utility.*;

public class Profile {

	private double[] array;
	private static final int ARRAY_BEFORE = -1;
	private static final int ARRAY_AFTER = 1;


	public Profile(double[] values){

		this.array = new double[values.length];
		for(int i=0; i<this.array.length; i++){
      array[i] = values[i];
    }
	}

  public Profile(Profile p){

    this.array = new double[p.size()];
    for(int i=0; i<this.array.length; i++){
      array[i] = p.get(i);
    }
  }


  /*
    ---------------------
    Getters
    ---------------------
  */
  public int size(){
    return array.length;
  }

  public double get(int i){
    double result = 0;
    try {
      result = this.array[i];
    } catch(Exception e){
      IJ.log("Cannot get value from profile: "+e.getMessage());
    }
    return result;
  }

  public double getMax(){
    double max = 0;
    for(int i=0; i<array.length;i++){
      if(array[i]>max){
        max = array[i];
      }
    }
    return max;
  }

  public int getIndexOfMax(){
    double max = 0;
    int maxIndex = 0;
    for(int i=0; i<array.length;i++){
      if(array[i]>max){
        max = array[i];
        maxIndex = i;
      }
    }
    return maxIndex;
  }

  public double getMin(){
    double min = this.getMax();
    for(int i=0; i<array.length;i++){
      if(array[i]<min){
        min = array[i];
      }
    }
    return min;
  }

  public int getIndexOfMin(){
    double min = this.getMax();
    int minIndex = 0;
    for(int i=0; i<array.length;i++){
      if(array[i]<min){
        min = array[i];
        minIndex = i;
      }
    }
    return minIndex;
  }

  public double[] asArray(){
    return this.array;
  }
  
  public Profile getPositions(int length){
	  double [] result = new double[array.length];
	  for(int i=0;i<array.length;i++){
		  result[i] = (double) i / (double) array.length * (double) length;
	  }
	  return new Profile(result);
  }

  // The testProfile must have been offset appropriately
  public double differenceToProfile(Profile testProfile){

    // the test profile needs to be matched to this profile
    // whichever is smaller must be interpolated 
    Profile profile1 = this.copy();
    Profile profile2 = testProfile;

    try{
      if(profile2.size()<profile1.size()){
        profile2 = profile2.interpolate(this.size());
      } else {
        profile1 = profile1.interpolate(testProfile.size());
      }
    } catch(Exception e){
      IJ.log("Error interpolating profiles: "+e.getMessage());
      IJ.log("Profile 1: ");
      profile1.print();
      IJ.log("Profile 2: ");
      profile2.print();
    }

    double difference = 0;

    for(int j=0; j<this.size(); j++){ // for each point round the array

      double thisValue = profile1.get(j);
      double testValue = profile2.get(j);
      difference += Math.abs(thisValue - testValue);
    }
    return difference;
  }

  /*
    --------------------
    Profile manipulation
    --------------------
  */

  public Profile copy(){
    return new Profile(this.array);
  }

  public Profile offset(int j){
    double[] newArray = new double[this.size()];
    for(int i=0;i<this.size();i++){
      newArray[i] = this.array[ Utils.wrapIndex( i+j , this.size() ) ];
    }
    return new Profile(newArray);
  }
  
  /**
   * Perform a window-averaging smooth of the profile with the given window size
   * @param windowSize the size of the window
   * @return
   */
  public Profile smooth(int windowSize){

	  double[] result = new double[this.size()];

	  for (int i=0; i<array.length; i++) { // for each position

		  double[] prevValues = getValues(i, windowSize, Profile.ARRAY_BEFORE); // slots for previous angles
		  double[] nextValues = getValues(i, windowSize, Profile.ARRAY_AFTER);

		  double average = array[i];
		  for(int k=0;k<prevValues.length;k++){ 
			  average += prevValues[k] + nextValues[k];	        
		  }

		  result[i] = average / (windowSize*2 + 1);
	  }
	  return new Profile(result);
  }
  
  /**
   * Get an array of the values <windowSize> before or after the current point
   * @param position the position in the array
   * @param windowSize the number of points to find
   * @param type find points before or after
   * @return
   */
  private double[] getValues(int position, int windowSize, int type){

	  double[] values = new double[windowSize]; // slots for previous angles
	  for(int j=0;j<values.length;j++){

		  // If type was before, multiply by -1; if after, multiply by 1
		  int index = Utils.wrapIndex( position + ((j+1)*type)  , this.size() );
		  values[j] = array[index];
	  }
	  return values;
  }

  public void reverse(){

    double tmp;
    for (int i = 0; i < this.array.length / 2; i++) {
        tmp = this.array[i];
        this.array[i] = this.array[this.array.length - 1 - i];
        this.array[this.array.length - 1 - i] = tmp;
    }
  }  

  // newLength must be larger than current
  // Make this profile the length specified
  public Profile interpolate(int newLength){

    if(newLength < this.size()){
      // throw new Exception("Cannot interpolate to a smaller array!");
    }
    
    double[] newArray = new double[newLength];
    // where in the old curve index is the new curve index?
    for (int i=0; i<newLength; i++) {
      // we have a point in the new curve.
      // we want to know which points it lay between in the old curve
      double oldIndex = ( (double)i / (double)newLength)*array.length; // get the frational index position needed
      double interpolatedValue = interpolateValue(oldIndex);
      newArray[i] = interpolatedValue;
    }
    return new Profile(newArray);
  }

  /*
    Take an index position from a non-normalised profile
    Normalise it
    Find the corresponding angle in the median curve
    Interpolate as needed
  */
  private double interpolateValue(double normIndex){

    // convert index to 1 window boundaries
    int index1 = (int)Math.round(normIndex);
    int index2 = index1 > normIndex
                        ? index1 - 1
                        : index1 + 1;

    int indexLower = index1 < index2
                        ? index1
                        : index2;

    int indexHigher = index2 < index1
                             ? index2
                             : index1;
    
//    int absIndex = (int) Math.abs((normIndex - indexLower));

    // wrap the arrays
    indexLower  = Utils.wrapIndex(indexLower , this.size());
    indexHigher = Utils.wrapIndex(indexHigher, this.size());
    
//    int indexTwoLower = Utils.wrapIndex(indexLower-1 , this.size());
//    int indexTwoHigher = Utils.wrapIndex(indexHigher+1 , this.size());

    // get the angle values in the profile at the given indices
//    double valueTwoHigher = array[indexTwoHigher ];
//    double valueTwoLower = array[indexTwoLower ];
    double valueHigher = array[indexLower ];
    double valueLower  = array[indexHigher];
    
//    double[] xvalues = { -1, 0, 1, 2 };
//    double[] yvalues = { valueTwoLower, valueLower, valueHigher, valueTwoHigher };
//
//    double interpolatedValue = 0;
//    try{
//    	Interpolator interpolator = new Interpolator(xvalues, yvalues);
//    	interpolatedValue = interpolator.find(absIndex);
//    } catch(Exception e){
//    	// interpolate on a straight line between the points if the Interpolator fails
//    	double valueDifference = valueHigher - valueLower;
//    	double positionToFind = indexHigher - normIndex;
//    	interpolatedValue = (valueDifference * positionToFind) + valueLower;
//    	IJ.log("    Error in cubic interpolator: falling back to linear");
//    }
    
    double valueDifference = valueHigher - valueLower;
	double positionToFind = indexHigher - normIndex;
	double linearInterpolatedValue = (valueDifference * positionToFind) + valueLower;
////	IJ.log("    L: "+linearInterpolatedValue+" C: "+interpolatedValue);
//	if(Math.abs(interpolatedValue)>Math.abs(linearInterpolatedValue*2) || Math.abs(interpolatedValue)< Math.abs(linearInterpolatedValue/2)){
//		interpolatedValue = linearInterpolatedValue;
////		IJ.log("    Ambiguous curve; falling back to linear interpolation"); 
//	}
    return linearInterpolatedValue;
  }

  /*
    Interpolate another profile to match this, and move this profile
    along it one index at a time. Find the point of least difference, 
    and return this offset. Returns the positive offset to this profile
  */
  public int getSlidingWindowOffset(Profile testProfile){

    double lowestScore = this.differenceToProfile(testProfile);
    int index = 0;
    for(int i=0;i<this.size();i++){

      Profile offsetProfile = this.offset(i);

      double score = offsetProfile.differenceToProfile(testProfile);
      if(score<lowestScore){
        lowestScore=score;
        index=i;
      }

    }
    return index;
  }



  /*
    --------------------
    Detect minima within the profiles
    --------------------
  */

  /*
    For each point in the array, test for a local minimum.
    The values of the points <minimaLookupDistance> ahead and behind are checked.
    Each should be greater than the value before.
    One exception is allowed, to account for noisy data. Returns the indexes of minima
  */
  public Profile getLocalMinima(int windowSize){
    // go through angle array (with tip at start)
    // look at 1-2-3-4-5 points ahead and behind.
    // if all greater, local minimum
    double[] prevValues = new double[windowSize]; // slots for previous angles
    double[] nextValues = new double[windowSize]; // slots for next angles

    // int count = 0;
    // List<Integer> result = new ArrayList<Integer>(0);

    double[] minima = new double[this.size()];

    for (int i=0; i<array.length; i++) { // for each position in sperm

      // go through each lookup position and get the appropriate angles
      for(int j=0;j<prevValues.length;j++){

        int prev_i = Utils.wrapIndex( i-(j+1)  , this.size() ); // the index j+1 before i
        int next_i = Utils.wrapIndex( i+(j+1)  , this.size() ); // the index j+1 after i

        // fill the lookup array
        prevValues[j] = array[prev_i];
        nextValues[j] = array[next_i];
      }
      
      // with the lookup positions, see if minimum at i
      // return a 1 if all higher than last, 0 if not
      // prev_l = 0;
      boolean ok = true;
      for(int k=0;k<prevValues.length;k++){

        // for the first position in prevValues, compare to the current index
        if(k==0){
          if( prevValues[k] < array[i] || 
              nextValues[k] < array[i] ){
            ok = false;
          }
        } else { // for the remainder of the positions in prevValues, compare to the prior prevAngle
          
          if( prevValues[k] < prevValues[k-1] || 
              nextValues[k] < nextValues[k-1]){
            ok = false;
          }
        }
      }

      if(ok){
        // count++;
        minima[i] = 1;
      } else {
        minima[i] = 0;
      }

      // result.add(i);

    }
    Profile minimaProfile = new Profile(minima);
    // this.minimaCalculated = true;
    // this.minimaCount =  count;
    return minimaProfile;
  }

  public Profile getLocalMaxima(int windowSize){
	  // go through array
	  // look at points ahead and behind.
	  // if all lower, local maximum

	  double[] result = new double[this.size()];

	  for (int i=0; i<array.length; i++) { // for each position

		  double[] prevValues = getValues(i, windowSize, Profile.ARRAY_BEFORE); // slots for previous angles
		  double[] nextValues = getValues(i, windowSize, Profile.ARRAY_AFTER);

		  // with the lookup positions, see if maximum at i
		  // return a 1 if all lower than last, 0 if not
		  boolean isMaximum = true;
		  for(int k=0;k<prevValues.length;k++){

			  // for the first position in prevValues, compare to the current index
			  if(k==0){
				  if( prevValues[k] > array[i] || 
						  nextValues[k] > array[i] ){
					  isMaximum = false;
				  }
			  } else { // for the remainder of the positions in prevValues, compare to the prior prevAngle

				  if( prevValues[k] > prevValues[k-1] || 
						  nextValues[k] > nextValues[k-1]){
					  isMaximum = false;
				  }
			  }
		  }

		  result[i] = isMaximum ? 1 : 0;
	  }
	  return new Profile(result);
  }

  public Profile calculateDeltas(int windowSize){

    double[] deltas = new double[this.size()];

    double[] prevValues = new double[windowSize]; // slots for previous angles
    double[] nextValues = new double[windowSize]; // slots for next angles

    for (int i=0; i<array.length; i++) { // for each position in sperm

      for(int j=0;j<prevValues.length;j++){

        int prev_i = Utils.wrapIndex( i-(j+1)  , this.size() ); // the index j+1 before i
        int next_i = Utils.wrapIndex( i+(j+1)  , this.size() ); // the index j+1 after i

        // fill the lookup array
        prevValues[j] = array[prev_i];
        nextValues[j] = array[next_i];
      }

      double delta = 0;
      for(int k=0;k<prevValues.length;k++){

        if(k==0){
          delta += (array[i] - prevValues[k]) + (nextValues[k] - array[i]);
          
        } else {
          delta += ( prevValues[k] - prevValues[k-1]) + (nextValues[k] - nextValues[k-1]);
        }
        
      }

      deltas[i] = delta;
    }
    Profile result = new Profile(deltas);
    return result;
  }
  
  public Profile differentiate(){

	  double[] deltas = new double[this.size()];

	  for (int i=0; i<array.length; i++) { // for each position in sperm

		  int prev_i = Utils.wrapIndex( i-1  , this.size() ); // the index before
		  int next_i = Utils.wrapIndex( i+1  , this.size() ); // the index after


		  double delta = 	array[i]	-  array[prev_i] +
				  			array[next_i]- array[i];


		  deltas[i] = delta;
	  }
	  Profile result = new Profile(deltas);
	  return result;
  }
  
  /**
   * Multiply all values within the profile by a given value
   * @param multiplier the value to multiply by
   * @return the new profile
   */
  public Profile multiply(double multiplier){
	  double[] result = new double[this.size()];

	  for (int i=0; i<array.length; i++) { // for each position in sperm
		  result[i] = array[i] * multiplier;
	  }
	  return new Profile(result);
  }

  // use for debugging
  public void print(){
    for (int i=0; i<array.length; i++) {
      IJ.log("Point "+i+": "+array[i]);
    }
  }
}