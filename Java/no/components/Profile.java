/*
	DISTANCE PROFILE

	Holds the distances from the centre of mass
*/

package no.components;

import java.util.*;
import no.utility.*;

public class Profile {

	private double[] array;
  private int minimaCount = 0;
  private int maximaCount = 0;

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
    return this.array[i];
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

  public double[] getProfile(){
    return this.array;
  }

  // public double[] getNormalisedIndexes(){
  //   double[] d = new double[this.size()];
  //   for(int i=0;i<this.size();i++){
  //     d[i] = ( (double)i / (double)this.size() ) * 100;
  //   }
  //   return d;
  // }

  // The testProfile must have been offset appropriately
  public double differenceToProfile(Profile testProfile){

    // the test profile needs to be matched to this profile
    // whichever is smaller must be interpolated 
    Profile profile1 = this.copy();
    Profile profile2 = testProfile;
    if(testProfile.size()<this.size()){
      profile2 = profile2.interpolate(this.size());
    } else {
      profile1 = profile1.interpolate(testProfile.size());
    }

    double difference = 0;

    for(int j=0; j<this.size(); j++){ // for each point round the array

      double thisValue = profile1.get(j);
      double testValue = profile2.get(j);
      difference += Math.abs(curveAngle - testAngle);
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
      newArray[i] = this.array[ NuclearOrganisationUtility.wrapIndex( i+j , this.size() ) ];
    }
    return new Profile(newArray);
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

    // wrap the arrays
    indexLower  = NuclearOrganisationUtility.wrapIndex(indexLower , this.size());
    indexHigher = NuclearOrganisationUtility.wrapIndex(indexHigher, this.size());

    // get the angle values in the profile at the given indices
    double valueHigher = array[indexLower ];
    double valueLower  = array[indexHigher];

    // interpolate on a stright line between the points
    double valueDifference = valueHigher - valueLower;
    double positionToFind = indexHigher - normIndex;
    double interpolatedValue = (valueDifference * positionToFind) + valueLower;
    return interpolatedValue;
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
  public List<Integer> getLocalMinima(int windowSize){
    // go through angle array (with tip at start)
    // look at 1-2-3-4-5 points ahead and behind.
    // if all greater, local minimum
    double[] prevValues = new double[windowSize]; // slots for previous angles
    double[] nextValues = new double[windowSize]; // slots for next angles

    int count = 0;
    List<Integer> result = new ArrayList<Integer>(0);

    for (int i=0; i<array.length; i++) { // for each position in sperm

      // go through each lookup position and get the appropriate angles
      for(int j=0;j<prevValues.length;j++){

        int prev_i = NuclearOrganisationUtility.wrapIndex( i-(j+1)  , this.size() ); // the index j+1 before i
        int next_i = NuclearOrganisationUtility.wrapIndex( i+(j+1)  , this.size() ); // the index j+1 after i

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
        count++;
      }

      result.add(i);

    }
    // this.minimaCalculated = true;
    this.minimaCount =  count;
    return result;
  }

  public List<Integer> getLocalMaxima(int windowSize){
    // go through angle array (with tip at start)
    // look at 1-2-3-4-5 points ahead and behind.
    // if all greater, local minimum
    double[] prevValues = new double[windowSize]; // slots for previous angles
    double[] nextValues = new double[windowSize]; // slots for next angles

    int count = 0;
    List<Integer> result = new ArrayList<Integer>(0);

    for (int i=0; i<array.length; i++) { // for each position in sperm

      // go through each lookup position and get the appropriate angles
      for(int j=0;j<prevValues.length;j++){

        int prev_i = NuclearOrganisationUtility.wrapIndex( i-(j+1)  , this.size() ); // the index j+1 before i
        int next_i = NuclearOrganisationUtility.wrapIndex( i+(j+1)  , this.size() ); // the index j+1 after i

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
          if( prevValues[k] > array[i] || 
              nextValues[k] > array[i] ){
            ok = false;
          }
        } else { // for the remainder of the positions in prevValues, compare to the prior prevAngle
          
          if( prevValues[k] > prevValues[k-1] || 
              nextValues[k] > nextValues[k-1]){
            ok = false;
          }
        }
      }

      if(ok){
        count++;
      }

      result.add(i);

    }
    // this.minimaCalculated = true;
    this.minimaCount =  count;
    return result;
  }
}