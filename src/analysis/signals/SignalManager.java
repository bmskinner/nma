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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import components.ICell;
import components.ICellCollection;
import components.active.ProfileableCellularComponent.IndexOutOfBoundsException;
import components.active.generic.UnavailableBorderTagException;
import components.active.generic.UnavailableSignalGroupException;
import components.generic.MeasurementScale;
import components.generic.Tag;
import components.nuclear.INuclearSignal;
import components.nuclear.ISignalGroup;
import components.nuclear.NuclearSignal;
import components.nuclear.SignalGroup;
import components.nuclei.Nucleus;
import logging.Loggable;
import stats.Quartile;
import stats.SignalStatistic;
import stats.StatisticDimension;
import utility.ArrayConverter;
import utility.Constants;
import utility.ArrayConverter.ArrayConversionException;

/**
 * This class is designed to simplify operations on CellCollections
 * involving signals. It should be accessed via CellCollection.getSignalManager()
 * @author bms41
 *
 */
public class SignalManager implements Loggable {
	
	private ICellCollection collection;
	
	public SignalManager(ICellCollection collection){
		this.collection = collection;
	}
	
	  /** 
	   * Return the nuclei with or without signals in the given group.
	   * @param signalGroup the group number 
	   * @param hasSignal
	   * @return a list of cells
	   */
	  public Set<ICell> getCellsWithNuclearSignals(UUID signalGroup, boolean hasSignal){
		  Set<ICell> result = new HashSet<ICell>(collection.size());

		  for(ICell c : collection.getCells()){
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
	  
  /**
   * Get the number of signal groups in the cell collection
   * @return the number of signal groups
   */
	public int getSignalGroupCount(){
		  return getSignalGroupIDs().size();
	  }
	  	  
	  /**
	   * Fetch the signal group ids in this collection
	   * @param id
	   * @return
	   */
	  public Set<UUID> getSignalGroupIDs(){
	      return collection.getSignalGroupIDs();
	  }
	  
	  /**
	   * Remove the given signal group
	   * @param id
	   */
	  public void removeSignalGroup(UUID id){
		  collection.removeSignalGroup(id);
	  }
	  
	  public void removeSignalGroups(){
		  for(UUID id : this.getSignalGroupIDs()){
			  removeSignalGroup(id);
		  }
	  }
	  
	  /**
	   * Fetch the signal groups in this collection
	   * @param id
	   * @return
	   */
	  public Collection<ISignalGroup> getSignalGroups(){
	      return collection.getSignalGroups();
	  }
	  
	  public String getSignalGroupName(UUID signalGroup) throws UnavailableSignalGroupException{

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
	  
	  public int getSignalChannel(UUID signalGroup) throws UnavailableSignalGroupException{
          return collection.getSignalGroup(signalGroup).getChannel();
	  }
	  
	  /**
	   * Get the name of the folder containing the images for the given signal group
	   * @param signalGroup
	   * @return
	 * @throws UnavailableSignalGroupException 
	   */
	  public String getSignalSourceFolder(UUID signalGroup) throws UnavailableSignalGroupException{
          return collection.getSignalGroup(signalGroup).getFolder().getAbsolutePath();
	  }
	  
	  /**
	   * Update the source image folder for the given signal group
	   * @param signalGroup
	   * @param f
	   */
	  public void updateSignalSourceFolder(UUID signalGroup, File f){
		  
		  if( ! collection.hasSignalGroup(signalGroup)){
			  return;
		  }
		  
		  collection.getNuclei().parallelStream().forEach( n-> {
			  if(n.getSignalCollection().hasSignal(signalGroup)){
				  String fileName = n.getSignalCollection().getSourceFile(signalGroup).getName();
				  File newFile = new File(f.getAbsolutePath()+File.separator+fileName);
				  n.getSignalCollection().updateSourceFile(signalGroup, newFile);
			  }
		  });
		  
          try {
			collection.getSignalGroup(signalGroup).setFolder(f);
		} catch (UnavailableSignalGroupException e) {
			fine("Error getting signal group", e);
		}
          
	  }
	  
	  /**
	   * Update the signal group id
	   * @param oldID the id to replace
	   * @param newID the new id
	   */
	  public void updateSignalGroupID(UUID oldID, UUID newID){
		  
		  if( ! collection.hasSignalGroup(oldID)){
			  fine("Signal group is not present");
			  return;
		  }
		  
		  finer("Updating signals to new group id in nuclei");
		  for(Nucleus n : collection.getNuclei()){
			  n.getSignalCollection().updateSignalGroupID(oldID, newID);
		  }
		  
		  finer("Updating signal group in cell collection");

		  try {

			  // the group the signals are currently in
			  ISignalGroup oldGroup = collection.getSignalGroup(oldID);

			  finer("Old group: "+oldID+" | "+oldGroup.toString());



			  // Merge and rename signal groups

			  if(collection.hasSignalGroup(newID)){ // check if the group already exists

				  finer("A signal group of id "+newID+" already exists");
				  ISignalGroup existingGroup = collection.getSignalGroup(newID);

				  if( ! oldGroup.getGroupName().equals(existingGroup.getGroupName())){
					  finer("Setting signal group name to merge");
					  existingGroup.setGroupName("Merged_"+oldGroup.getGroupName()+"_"+existingGroup.getGroupName());
				  }

				  if( oldGroup.getChannel()!=existingGroup.getChannel()){
					  finer("Setting signal group name to -1");
					  existingGroup.setChannel(-1);
				  }

				  // Shells and colours?

			  } else { // the signal group does not exist, just copy the old group

				  finer("A signal group of id "+newID+" does not exist");

				  // the new group for the signals
				  ISignalGroup newGroup = new SignalGroup(oldGroup);

				  finer("New group: "+newID+" | "+newGroup.toString());
				  collection.addSignalGroup(newID, newGroup);
				  finer("Added new signal group: "+newID);

			  }

			  collection.removeSignalGroup(oldID);
			  finer("Removed old signal group");
		  } catch(UnavailableSignalGroupException e){
			  warn("Missing expected signal group");
			  fine("Error getting signal group "+oldID, e);
		  }
	  }
	  
	  /**
	   * Find the total number of signals within all nuclei of the collection.
	   * @return the total
	   */
	  public int getSignalCount(){
		  int count = 0;
		  for(UUID signalGroup : getSignalGroupIDs()){
			  count+= this.getSignalCount(signalGroup);
		  }
		  return count;
	  }
	  
	  /**
	   * Get the number of signals in the given group
	   * @param signalGroup the group to search
	   * @return the count or -1 if the signal group is not present
	   */
	  public int getSignalCount(UUID signalGroup){
		  int count = 0;
		  
		  if(collection.getSignalGroupIDs().contains(signalGroup)){
			  count=0;
			  for(Nucleus n : collection.getNuclei()){
				  count += n.getSignalCollection().numberOfSignals(signalGroup);

			  } // end nucleus iterations
		  }
		  return count;
	  }

	  /**
	   * Get the mean number of signals in each nucleus
	   * @param signalGroup
	   * @return
	   */
	  public double getSignalCountPerNucleus(UUID signalGroup){
		  if(getSignalCount(signalGroup)==0){
			  return 0;
		  }

		  return (double) getSignalCount(signalGroup) / (double) getNumberOfCellsWithNuclearSignals(signalGroup);
	  }
		

	  /**
	   * Test whether the current population has signals in any channel
	   * @return
	   */
	  public boolean hasSignals(){
		  for(UUID i : getSignalGroupIDs()){
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
		  for(UUID id : collection.getSignalGroupIDs()){

			  try {
				  if(collection.getSignalGroup(id).hasShellResult()){
					  return true;
				  }
			  } catch (UnavailableSignalGroupException e) {
				  fine("Error getting signal group", e);
			  }
		  }
		  return false;
	  }


	  
	  /**
	   * Get all the signals from all nuclei in the given channel
	   * @param channel the channel to search
	   * @return a list of signals
	   */
	  public List<INuclearSignal> getSignals(UUID signalGroup){

		  List<INuclearSignal> result = new ArrayList<INuclearSignal>(0);
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
      public double getMedianSignalStatistic(SignalStatistic stat, MeasurementScale scale, UUID signalGroup){
          
          double[] values = null;
          double median;
          /*
           * Angles must be wrapped
           */
          if(stat.getDimension().equals(StatisticDimension.ANGLE)){
              values = getOffsetSignalAngles(signalGroup);
              
              if(values.length==0){
            	  fine("No signals detected in group for "+stat);
            	  return 0;
              }
              
              median = new Quartile(values, Quartile.MEDIAN).doubleValue();
              median += getMeanSignalAngle(signalGroup);
          } else {
              values = this.getSignalStatistics(stat, scale, signalGroup);
              
              if(values.length==0){
            	  fine("No signals detected in group for "+stat);
            	  return 0;
              }
              
              median =  new Quartile(values, Quartile.MEDIAN).doubleValue();
          }
          
          return median;
             
      }
      
      public double[] getSignalStatistics(SignalStatistic stat, MeasurementScale scale, UUID signalGroup) {

          Set<ICell> cells = getCellsWithNuclearSignals(signalGroup, true);
          List<Double> a = new ArrayList<Double>(0);
          for(ICell c : cells){
              Nucleus n = c.getNucleus();
              a.addAll(n.getSignalCollection().getStatistics(stat, scale, signalGroup));

          }
          
          double[] values;

			try{
				values = new ArrayConverter(a).toDoubleArray();

			} catch (ArrayConversionException e) {
				values = new double[0]; 
			}
          return values;
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
      public double getMeanSignalAngle(UUID signalGroup) {

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
    public double[] getOffsetSignalAngles(UUID signalGroup) {

          double[] values = getSignalStatistics(SignalStatistic.ANGLE, MeasurementScale.PIXELS, signalGroup); 
                    
          if(values.length==0){
        	  return new double[0];
          }
          
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
     * Copy the signal groups in this cell collection to the target collection,
     * preserving the signal group IDs
     * @param target
     */
    public void copySignalGroups(ICellCollection target){

    	for(UUID id : collection.getSignalGroupIDs()){
    		ISignalGroup newGroup;
    		try {
    			newGroup = new SignalGroup(  collection.getSignalGroup(id)  );
    			target.addSignalGroup(id, newGroup);
    		} catch (UnavailableSignalGroupException e) {
    			warn("Unable to copy signal group");
    			fine("Signal group not present", e);
    		}
    	}
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

    
    /**
     * If the OP has moved, signal angles need to be recalculated
     * 
     */
    public void recalculateSignalAngles(){
    	finer("Recalcalculating signal angles");
    	for(Nucleus n : collection.getNuclei()){
    		try {
				n.calculateSignalAnglesFromPoint(n.getBorderPoint(Tag.ORIENTATION_POINT));
			} catch (UnavailableBorderTagException e) {
				fine("Cannot get OP index");
			}
    	}
    }
	  
}
