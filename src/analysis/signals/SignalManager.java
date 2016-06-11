/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package analysis.signals;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import components.Cell;
import components.CellCollection;
import components.generic.MeasurementScale;
import components.nuclear.NuclearSignal;
import components.nuclei.Nucleus;
import stats.SignalStatistic;
import stats.StatisticDimension;
import stats.Stats;
import utility.Constants;
import utility.Utils;

/**
 * This class is designed to simplify operations on CellCollections
 * involving signals. It should be accessed via CellCollection.getSignalManager()
 * @author bms41
 *
 */
public class SignalManager {
	
	private CellCollection collection;
	
	public SignalManager(CellCollection collection){
		this.collection = collection;
	}
	
	  /** 
	   * Return the nuclei with or without signals in the given group.
	   * @param signalGroup the group number 
	   * @param hasSignal
	   * @return a list of cells
	   */
	  public List<Cell> getCellsWithNuclearSignals(UUID signalGroup, boolean hasSignal){
		  List<Cell> result = new ArrayList<Cell>(0);

		  for(Cell c : collection.getCells()){
			  Nucleus n = c.getNucleus();

			  if(hasSignal){
				  if(n.getSignalCollection().hasSignal(signalGroup)){
					  result.add(c);
				  }
			  } else {
				  if(!n.getSignalCollection().hasSignal(signalGroup)){
					  result.add(c);
				  }
			  }
		  }
		  return result;
	  }
	  
	  public int getNumberOfCellsWithNuclearSignals(UUID signalGroup){
		  return getCellsWithNuclearSignals(signalGroup, true).size();
	  }
	  
	  public int getSignalGroupCount(){
		  return getSignalGroups().size();
	  }
	  
	  /**
	   * Find the signal groups present within the nuclei of the collection
	   * @return the list of groups. Order is not guaranteed
	   */
	  public Set<UUID> getSignalGroups(){
		  Set<UUID> result = new HashSet<UUID>(0);
		  for(Nucleus n : collection.getNuclei()){
			  for( UUID group : n.getSignalCollection().getSignalGroupIDs()){
				  if(n.getSignalCollection().hasSignal(group)){ // signal groups can be copied over from split collections. Check signals exist
					  result.add(group);
				  }
			  }
		  }
		  return result;
	  }
	  
	  public String getSignalGroupName(UUID signalGroup){
		  return collection.getSignalGroup(signalGroup).getGroupName();
	  }
	  
	  public int getSignalGroupNumber(UUID signalGroup){
		  int result = 0;
		  for(Nucleus n : collection.getNuclei()){
			  if(n.getSignalCollection().hasSignal(signalGroup)){
				  result = n.getSignalCollection().getSignalGroupNumber(signalGroup);
			  }
		  }
		  return result;
	  }
	  
	  public int getSignalChannel(UUID signalGroup){
          return collection.getSignalGroup(signalGroup).getChannel();
	  }
	  
	  /**
	   * Get the name of the folder containing the images for the given signal group
	   * @param signalGroup
	   * @return
	   */
	  public String getSignalSourceFolder(UUID signalGroup){
          return collection.getSignalGroup(signalGroup).getFolder().getAbsolutePath();
	  }
	  
	  /**
	   * Update the source image folder for the given signal group
	   * @param signalGroup
	   * @param f
	   */
	  public void updateSignalSourceFolder(UUID signalGroup, File f){
		  for(Nucleus n : collection.getNuclei()){
			  if(n.getSignalCollection().hasSignal(signalGroup)){
				  String fileName = n.getSignalCollection().getSourceFile(signalGroup).getName();
				  File newFile = new File(f.getAbsolutePath()+File.separator+fileName);
				  n.getSignalCollection().updateSourceFile(signalGroup, newFile);
			  }
		  }
          collection.getSignalGroup(signalGroup).setFolder(f);

	  }
	  
	  /**
	   * Find the total number of signals within all nuclei of the collection.
	   * @return the total
	   */
	  public int getSignalCount(){
		  int count = 0;
		  for(UUID signalGroup : getSignalGroups()){
			  count+= this.getSignalCount(signalGroup);
		  }
		  return count;
	  }
	  
	  /**
	   * Get the number of signals in the given group
	   * @param signalGroup the group to search
	   * @return the count
	   */
	  public int getSignalCount(UUID signalGroup){
		  int count = 0;
		  for(Nucleus n : collection.getNuclei()){
			  count += n.getSignalCollection().numberOfSignals(signalGroup);

		  } // end nucleus iterations
		  return count;
	  }

	  /**
	   * Test whether the current population has signals in any channel
	   * @return
	   */
	  public boolean hasSignals(){
		  for(UUID i : getSignalGroups()){
			  if(this.hasSignals(i)){
				  return true;
			  }
		  }
		  return false;
	  }

	  /**
	   * Test whether the current population has signals in the given group
	   * @return
	   */
	  public boolean hasSignals(UUID signalGroup){
		  if(this.getSignalCount(signalGroup)>0){
			  return true;
		  } else{
			  return false;
		  }

	  }
	  
      /**
       * Test if any of the signal groups in the collection have a shell result
       * @return
       */
      public boolean hasShellResult(){
          for(UUID id : this.getSignalGroups()){
              if(collection.getSignalGroup(id).hasShellResult()){
                  return true;
              }
          }
          return false;
      }


	  
	  /**
	   * Get all the signals from all nuclei in the given channel
	   * @param channel the channel to search
	   * @return a list of signals
	   */
	  public List<NuclearSignal> getSignals(UUID signalGroup){

		  List<NuclearSignal> result = new ArrayList<NuclearSignal>(0);
		  for(Nucleus n : collection.getNuclei()){
			  result.addAll(n.getSignalCollection().getSignals(signalGroup));
		  }
		  return result;
	  }
	  
      
      /**
       * Get the median of the signal statistic in the given signal group
       * @param  signalGroup
       * @return the median
     * @throws Exception 
       */
      public double getMedianSignalStatistic(SignalStatistic stat, MeasurementScale scale, UUID signalGroup) throws Exception{
          
          double[] values = null;
          double median;
          /*
           * Angles must be wrapped
           */
          if(stat.getDimension().equals(StatisticDimension.ANGLE)){
              values = getOffsetSignalAngles(signalGroup);
              median = Stats.quartile(values, Constants.MEDIAN);
              median += getMeanSignalAngle(signalGroup);
          } else {
              values = this.getSignalStatistics(stat, scale, signalGroup);
              median =  Stats.quartile(values, Constants.MEDIAN);
          }
          
          return median;
             
      }
      
      public double[] getSignalStatistics(SignalStatistic stat, MeasurementScale scale, UUID signalGroup) throws Exception{

          List<Cell> cells = getCellsWithNuclearSignals(signalGroup, true);
          List<Double> a = new ArrayList<Double>(0);
          for(Cell c : cells){
              Nucleus n = c.getNucleus();
              a.addAll(n.getSignalCollection().getStatistics(stat, scale, signalGroup));

          }
          return Utils.getdoubleFromDouble(a.toArray(new Double[0]));
      }
      
      /**
       * Signal angles wrap, so a mean must be calculated as a zero point for boxplots.
       * Uses http://catless.ncl.ac.uk/Risks/7.44.html#subj4:
       *                   sum_i_from_1_to_N sin(a[i])
       *   a = arctangent ---------------------------
       *                   sum_i_from_1_to_N cos(a[i])
       * @param signalGroup
       * @return
       */
      public double getMeanSignalAngle(UUID signalGroup) throws Exception {

          double[] values = getSignalStatistics(SignalStatistic.ANGLE, MeasurementScale.PIXELS, signalGroup); 
          
          double sumSin = 0;
          double sumCos = 0;
          for(double value : values){
              sumSin += Math.sin(value);
              sumCos += Math.cos(value);
          }
          
          double mean = Math.atan2(sumSin, sumCos);
          
          if(mean<0){
              mean += 360;
          }
          return mean;
      }
      
      /**
       * For the signals in a group, find the corrected mean angle using the arctangent
       * method, then rescale the angles to use the mean as a zero point.
       * The returned values should be in the range -180 - +180 from the new zero

     * @param signalGroup
     * @return
     * @throws Exception
     */
    public double[] getOffsetSignalAngles(UUID signalGroup) throws Exception{

          double[] values = getSignalStatistics(SignalStatistic.ANGLE, MeasurementScale.PIXELS, signalGroup); 
          
          /*
           * The mean is the actual mean of the series of signal angles, with correction for wrapping.
           */

          double meanAngle = getMeanSignalAngle(signalGroup);
         
          /*
           * This is the distance from the mean angle to the zero angle, so values can be
           * corrected back to 'real' angles
           */
          double offset = angleDistance (meanAngle, 0) ;

          double[] result = new double[values.length];
          
          for(int i=0;i<values.length; i++){
             
        	  /*
               * Calculate the distance of the signal from the mean value, including a wrap.
               */
              
              double distance = angleDistance (values[i], meanAngle) ;
              
              /*
               * Correct the distance into the distance from the zero point of the nucleus
               */
              result[i] = distance + offset;

          }
          return result;
      }

    
    /**
     * Length (angular) of a shortest way between two angles.
     * It will be in range [-180, 180].
     */
    private double angleDistance(double a, double b) {
    	double phi = Math.abs(b - a) % 360;       // This is either the distance or 360 - distance
    	double distance = phi > 180 ? 360 - phi : phi;

    	double sign = (a - b >= 0 && a - b <= 180) || (a - b <= -180 && a- b>= -360) ? 1 : -1;
    	distance *= sign; 
    	return distance;


    }

	  
}
