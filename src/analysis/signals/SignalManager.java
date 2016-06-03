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

import components.Cell;
import components.CellCollection;
import components.nuclear.NuclearSignal;
import components.nuclei.Nucleus;

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
	  public List<Cell> getCellsWithNuclearSignals(int signalGroup, boolean hasSignal){
		  List<Cell> result = new ArrayList<Cell>(0);

		  for(Cell c : collection.getCells()){
			  Nucleus n = c.getNucleus();

			  if(hasSignal){
				  if(n.hasSignal(signalGroup)){
					  result.add(c);
				  }
			  } else {
				  if(!n.hasSignal(Math.abs(signalGroup))){
					  result.add(c);
				  }
			  }
		  }
		  return result;
	  }
	  
	  public int getNumberOfCellsWithNuclearSignals(int signalGroup){
		  return getCellsWithNuclearSignals(signalGroup, true).size();
	  }
	  
	  public int getSignalGroupCount(){
		  return getSignalGroups().size();
	  }
	  
	  /**
	   * Find the signal groups present within the nuclei of the collection
	   * @return the list of groups. Order is not guaranteed
	   */
	  public Set<Integer> getSignalGroups(){
		  Set<Integer> result = new HashSet<Integer>(0);
		  for(Nucleus n : collection.getNuclei()){
			  for( int group : n.getSignalCollection().getSignalGroups()){
					  result.add(group);
			  }
		  }
		  return result;
	  }
	  
	  public String getSignalGroupName(int signalGroup){
		  String result = null;
		  
		  for(Nucleus n : collection.getNuclei()){
			  if(n.hasSignal(signalGroup)){
				  result = n.getSignalCollection().getSignalGroupName(signalGroup);
			  }
		  }
		  return result;
	  }
	  
	  public int getSignalChannel(int signalGroup){
		  int result = 0;
		  
		  for(Nucleus n : collection.getNuclei()){
			  if(n.hasSignal(signalGroup)){
				  result = n.getSignalCollection().getSignalChannel(signalGroup);
			  }
		  }
		  return result;
	  }
	  
	  /**
	   * Get the name of the folder containing the images for the given signal group
	   * @param signalGroup
	   * @return
	   */
	  public String getSignalSourceFolder(int signalGroup){
		  String result = null;

		  for(Nucleus n : collection.getNuclei()){
			  if(n.hasSignal(signalGroup)){
				  File file = n.getSignalCollection().getSourceFile(signalGroup);
				  result = file.getParentFile().getAbsolutePath();
			  }
		  }
		  return result;
	  }
	  
	  /**
	   * Update the source image folder for the given signal group
	   * @param signalGroup
	   * @param f
	   */
	  public void updateSignalSourceFolder(int signalGroup, File f){
		  for(Nucleus n : collection.getNuclei()){
			  if(n.hasSignal(signalGroup)){
				  String fileName = n.getSignalCollection().getSourceFile(signalGroup).getName();
				  File newFile = new File(f.getAbsolutePath()+File.separator+fileName);
				  n.getSignalCollection().updateSourceFile(signalGroup, newFile);
			  }
		  }
	  }
	  
	  /**
	   * Find the total number of signals within all nuclei of the collection.
	   * @return the total
	   */
	  public int getSignalCount(){
		  int count = 0;
		  for(int signalGroup : getSignalGroups()){
			  count+= this.getSignalCount(signalGroup);
		  }
		  return count;
	  }
	  
	  /**
	   * Get the number of signals in the given group
	   * @param signalGroup the group to search
	   * @return the count
	   */
	  public int getSignalCount(int signalGroup){
		  int count = 0;
		  for(Nucleus n : collection.getNuclei()){
			  count += n.getSignalCount(signalGroup);

		  } // end nucleus iterations
		  return count;
	  }

	  /**
	   * Test whether the current population has signals in any channel
	   * @return
	   */
	  public boolean hasSignals(){
		  for(int i : getSignalGroups()){
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
	  public boolean hasSignals(int signalGroup){
		  if(this.getSignalCount(signalGroup)>0){
			  return true;
		  } else{
			  return false;
		  }

	  }
	  
	  /**
	   * Check the signal groups for all nuclei in the colleciton, and
	   * return the highest signal group present, or 0 if no signal groups
	   * are present
	 * @return the highest signal group
	 */
	  public int getHighestSignalGroup(){
		  int maxGroup = 0;
		  for(Nucleus n : collection.getNuclei()){
			  for(int group : n.getSignalCollection().getSignalGroups()){
				  maxGroup = group > maxGroup ? group : maxGroup;
			  }
		  }
		  return maxGroup;
	  }

	  /**
	   * Get all the signals from all nuclei in the given channel
	   * @param channel the channel to search
	   * @return a list of signals
	   */
	  public List<NuclearSignal> getSignals(int channel){

		  List<NuclearSignal> result = new ArrayList<NuclearSignal>(0);
		  for(Nucleus n : collection.getNuclei()){
			  result.addAll(n.getSignals(channel));
		  }
		  return result;
	  }

}
