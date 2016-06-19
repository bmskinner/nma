/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
package components.nuclear;

import ij.IJ;
import ij.process.ImageProcessor;
import io.ImageImporter;
import stats.SignalStatistic;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import logging.Loggable;
import components.generic.MeasurementScale;
import components.generic.XYPoint;

/**
 * This holds all the signals within a nucleus, within a hash.
 * The hash key is the signal group number from which they were found.
 * The signal group number accesses (a) the nuclear signals (b) the file
 *  they came from and (c) the channel within the file they came from 
 * and links to the channel number in the ImageStack for the nucleus.
 */
public class SignalCollection implements Serializable, Loggable {

	private static final long serialVersionUID = 1L;

	/**
	 * Holds the signals
	 */
	private Map<UUID, List<NuclearSignal>> collection = new LinkedHashMap<UUID, List<NuclearSignal>>();
	
	// the files that hold the image for the given channel
	private Map<UUID, File> sourceFiles = new HashMap<UUID, File>(0);
	
	// the channel with the signal in the source image
	private Map<UUID, Integer> sourceChannels = new HashMap<UUID, Integer>(0);
	
	/**
	 * Holds the names of the channels for presentation purposes
	 */
	private Map<UUID, String > names = new HashMap<UUID, String>();
	
	public SignalCollection(){
		
	}
	
	/**
	 * Duplicate a signal collection
	 * @param s
	 */
	public SignalCollection(SignalCollection s){
		
//		IJ.log("Duplicating signal collection");
//		IJ.log(s.toString());
		
		for(UUID group : s.getSignalGroupIDs() ){
//			IJ.log("  Group "+group);
			String groupName = s.getSignalGroupName(group);
			int channel = s.getSignalChannel(group);
			File f = new File(s.getSourceFile(group).getAbsolutePath());
			
			ArrayList<NuclearSignal> list = new ArrayList<NuclearSignal>();
			for(NuclearSignal signal : s.getSignals(group)){
				list.add(  new NuclearSignal(signal) );
//				IJ.log("  Copying signal");
			}
						
			this.addSignalGroup(list, group, f, channel);
			this.setSignalGroupName(group, groupName);
		}
//		IJ.log("  New collection has "+this.numberOfSignals()+" signals");
//		IJ.log(this.toString());
	}
	
	/**
	 * Add a list of nuclear signals to the collection
	 * @param list the signals
	 * @param groupID the group id - this should be consistent across all nuclei in a dataset
	 * @param sourceFile the file the signals originated from
	 * @param sourceChannel the channel the signals originated from
	 */
	public void addSignalGroup(List<NuclearSignal> list, UUID groupID, File sourceFile, int sourceChannel){
		if(list==null || Integer.valueOf(sourceChannel)==null || sourceFile==null || groupID==null){
			throw new IllegalArgumentException("Signal list or channel is null");
		}
		
//		UUID groupID = java.util.UUID.randomUUID();
		
		collection.put(    groupID, list);
		sourceFiles.put(   groupID, sourceFile);
		sourceChannels.put(groupID, sourceChannel);
	}
	
	public Set<UUID> getSignalGroupIDs(){
		return collection.keySet();
	}
	
	/**
	 * Change the id of the given signal group
	 * @param signalGroup
	 * @param newID
	 */
	public void updateSignalGroupID(UUID signalGroup, UUID newID){
		collection.put(newID, collection.get(signalGroup));
		sourceFiles.put(newID, sourceFiles.get(signalGroup));
		sourceChannels.put(newID, sourceChannels.get(signalGroup));
		names.put(newID, names.get(signalGroup));
		
		collection.remove(signalGroup);
		sourceFiles.remove(signalGroup);
		sourceChannels.remove(signalGroup);
		names.remove(signalGroup);
	}
	
	/**
	 * Get the group number of a signal group in the collection.
	 * @param signalGroup
	 * @return the group number, or zero if not present
	 */
	public int getSignalGroupNumber(UUID signalGroup){
		int i=0;
		for(UUID id : collection.keySet()){
			i++;
			if(collection.get(id).equals(signalGroup)){
				return i;
			}
		}
		return i;
	}
	
	/**
	 * Add a single signal to the given signal group
	 * @param n the signal
	 * @param signalGroup the signal group
	 */
	public void addSignal(NuclearSignal n, UUID signalGroup){
		checkSignalGroup(signalGroup);
		collection.get(signalGroup).add(n);
	}
	
	/**
	 * Append a list of signals to the given signal group
	 * @param list the signals
	 * @param signalGroup the signal group
	 */
	public void addSignals(List<NuclearSignal> list, UUID signalGroup){
		if(list==null){
			throw new IllegalArgumentException("Signal is null");
		}
		checkSignalGroup(signalGroup);
		collection.get(signalGroup).addAll(list);
	}

	
	/**
	 * Append a list of signals to the given signal group
	 * @param list the signals
	 * @param signalGroupName the signal group name
	 */
	public void addSignals(List<NuclearSignal> list, String signalGroupName){
		if(list==null){
			throw new IllegalArgumentException("Signal or group is null");
		}
		checkSignalGroup(signalGroupName);
		this.addSignals(list, names.get(signalGroupName));
	}
	
	
	/**
	 * Get all the signals in all signal groups, as a list of lists.
	 * Fetches the actual signals, not a copy
	 * @return the list of signal lists
	 */
	public ArrayList<List<NuclearSignal>> getSignals(){
		ArrayList<List<NuclearSignal>> result = new ArrayList<List<NuclearSignal>>(0);
		for(UUID signalGroup : this.getSignalGroupIDs()){
			result.add(getSignals(signalGroup));
		}
		return result;
	}
	
	/**
	 * Get the signals in the given group. Fetches the actual signals, 
	 * not a copy
	 * @param signalGroup the signal group
	 * @return a list of signals
	 */
	public List<NuclearSignal> getSignals(UUID signalGroup){
		checkSignalGroup(signalGroup);
		if(this.hasSignal(signalGroup)){
			return this.collection.get(signalGroup);
		} else {
			return new ArrayList<NuclearSignal>(0);
		}
	}
	
	/**
	 * Get the signals in the given channel
	 * @param channel the channel name
	 * @return a list of signals
	 */
	public List<NuclearSignal> getSignals(String channel){
		checkSignalGroup(channel);
		if(this.hasSignal(channel)){
			return this.collection.get(names.get(channel));
		} else {
			return new ArrayList<NuclearSignal>(0);
		}
	}
	
	/**
	 * Get the file containing the signals in the given signal group
	 * @param signalGroup the group id
	 * @return the File with the signals
	 */
	public File getSourceFile(UUID signalGroup){
		return this.sourceFiles.get(signalGroup);
	}
	
	/**
	 * Update the source file for the given signal group
	 * @param signalGroup
	 * @param f
	 */
	public void updateSourceFile(UUID signalGroup, File f){
		this.sourceFiles.put(signalGroup, f);
	}
	
	/**
	 * Get the channel containing the signals in the given signal group
	 * @param signalGroup the group id
	 * @return the RGB channel with the signals (0 if greyscale)
	 */
	public int getSourceChannel(UUID signalGroup){
		return this.sourceChannels.get(signalGroup);
	}
	
	/**
	 * Set the channel name
	 * @param channel the channel to name
	 * @param name the new name
	 */
	public void setSignalGroupName(UUID signalGroup, String name){
		if(signalGroup==null || name==null){
			throw new IllegalArgumentException("Channel or name is null");
		}
		names.put(signalGroup, name);
	}
	
	/**
	 * Get the signal group with the given name
	 * @param signalGroupName
	 * @return
	 */
	public UUID getSignalGroup(String signalGroupName){
		if(signalGroupName==null){
			throw new IllegalArgumentException("Signal group name is null");
		}
		if(!names.containsValue(signalGroupName)){
			throw new IllegalArgumentException("Signal group name is not present");
		}
		
		for(UUID signalGroup : names.keySet()){
			if(names.get(signalGroup).equals(signalGroupName)){
				return signalGroup;
			}
		}
		return null;
	}
	
	public String getSignalGroupName(UUID signalGroup){
		if(signalGroup==null){
			throw new IllegalArgumentException("Channel is null");
		}
		if(!names.containsKey(signalGroup)){
			throw new IllegalArgumentException("Channel name is not present");
		}
		String result = names.get(signalGroup);
		return result;
	}
	
	/**
	 * Get the channel of the source image containing the given signal
	 * group
	 * @param signalGroup the group
	 * @return the RGB channel (0 if greyscale)
	 */
	public int getSignalChannel(UUID signalGroup){
		if(signalGroup==null){
			throw new IllegalArgumentException("Channel is null");
		}
		return this.sourceChannels.get(signalGroup);
	}
	
	/**
	 * Get the names of signal groups which have been named; ignores unnamed channels
	 * @return the set of names
	 */
	public Collection<String> getSignalGroupNames(){
		return names.values();
	}
	
	/**
	 * Get the set of signal groups in this collection
	 * @return the set of integer group numbers
	 */
//	public Set<Integer> getSignalGroups(){
//		return names.keySet();
//	}
	
	/**
	 * Get the number of signal groups
	 * @return the number of signal groups
	 */
	public int numberOfSignalGroups(){
		return collection.size();
	}
	
	/**
	 * Get the total number of signals in all groups
	 * @return the count
	 */
	public int numberOfSignals(){
		int count=0;
		for(UUID group : collection.keySet()){
			count += numberOfSignals(group);
		}
		return count;
	}
	
	/**
	 * Check if the signal group contains signals in this collection
	 * @param signalGroup the group id
	 * @return yes or no
	 */
	public boolean hasSignal(UUID signalGroup){
		if(signalGroup==null){
			throw new IllegalArgumentException("Signal group is null");
		}
		if(!collection.containsKey(signalGroup)){
			return false;
		}
		if(collection.get(signalGroup).isEmpty()){
			return false;
		} 
		return true;
	}
	
	/**
	 * Check if the signal group contains signals in this collection
	 * @param signalGroup the group id
	 * @return yes or no
	 */
	public boolean hasSignal(){
		
		if(collection.isEmpty()){
			return false;
		}
		return true;
	}
	
	/**
	 * Check if the signal group contains signals in this collection
	 * @param signalGroup the group name
	 * @return yes or no
	 */
	public boolean hasSignal(String signalGroupName){
		if(signalGroupName==null){
			throw new IllegalArgumentException("Channel is null");
		}
		if(!collection.containsValue(signalGroupName)){
			return false;
		}
		if(collection.get(signalGroupName).isEmpty()){
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Get the total number of signals in a given channel
	 * @param channel the channel
	 * @return the count
	 */
	public int numberOfSignals(UUID signalGroup){
		checkSignalGroup(signalGroup);
		return collection.get(signalGroup).size();
	}
	
	/**
	 * Get the total number of signals in a given signal group
	 * @param signalGroup the group name
	 * @return the count
	 */
	public int numberOfSignals(String signalGroupName){
		checkSignalGroup(signalGroupName);
		return numberOfSignals(names.get(signalGroupName));
	}
	
	/**
	 * Remove all signals from the collection
	 */
	public void removeSignals(){
		collection     = new LinkedHashMap<UUID, List<NuclearSignal>>();
		sourceFiles    = new HashMap<UUID, File>(0);
		sourceChannels = new HashMap<UUID, Integer>(0);
		names          = new HashMap<UUID, String>();
	}
	
	/**
	 * Remove the given signal group from the collection
	 */
	public void removeSignals(UUID signalGroup){
		collection.remove(signalGroup);
		sourceFiles.remove(signalGroup);
		sourceChannels.remove(signalGroup);
		names.remove(signalGroup);
	}	
	
	/**
	 * Find the pairwise distances between all signals in the nucleus 
	 */
	private double[][] calculateDistanceMatrix(){

		// create a matrix to hold the data
		// needs to be between every signal and every other signal, irrespective of colour
		int matrixSize = this.numberOfSignals();

		double [][] matrix = new double[matrixSize][matrixSize];
		
		int matrixRow = 0;
		int matrixCol = 0;
		
		for( List<NuclearSignal> signalsRow : getSignals()){

			if(!signalsRow.isEmpty()){

				for(NuclearSignal row : signalsRow){
					
					matrixCol=0;

					XYPoint aCoM = row.getCentreOfMass();

					for( List<NuclearSignal> signalsCol : getSignals()){

						if(!signalsCol.isEmpty()){

							for(NuclearSignal col : signalsCol){
								XYPoint bCoM = col.getCentreOfMass();
								matrix[matrixRow][matrixCol] = aCoM.getLengthTo(bCoM);
								matrixCol++;
							}

						}

					}
					matrixRow++;
				}
			}
		}
		return matrix;
	}
	
	/**
	 * Export the pairwise distances between all signals to the given folder
	 * @param outputFolder the folder to export to
	 */
	public void exportDistanceMatrix(File outputFolder){
		
		double[][] matrix = calculateDistanceMatrix();
		
		File f = new File(outputFolder.getAbsolutePath()+File.separator+"signalDistanceMatrix.txt");
		if(f.exists()){
			f.delete();
		}

//		int matrixSize = matrix.length;
		StringBuilder outLine = new StringBuilder("Signal\t");
		
		// prepare the column headings
		int col = 0;
		for(List<NuclearSignal> signalsRow : getSignals()){
			
			if(!signalsRow.isEmpty()){

				for(NuclearSignal s : signalsRow){
					outLine.append("SIGNAL_"+col+"\t");
					col++;
				}	
			}
			outLine.append("|\t"); // separator between signal channels
		}
		outLine.append("\r\n");

		// add the rows of values
		int matrixRow = 0;
		int matrixCol = 0;
		for(List<NuclearSignal> imagePlane : getSignals()){
			
			
			if(!imagePlane.isEmpty()){
				
				
				for(NuclearSignal s : imagePlane){ // go through all the signals, row by row
					matrixCol=0; // begin a new column
					outLine.append("SIGNAL_"+matrixRow+"\t");
					// within the row, get all signals as a column
					for(List<NuclearSignal> signalsCol : getSignals()){

						if(!signalsCol.isEmpty()){							
							for(NuclearSignal c : signalsCol){
								outLine.append(matrix[matrixRow][matrixCol]+"\t");
								matrixCol++;
							}
						}
						outLine.append("|\t"); // separate between channels within row
					}
					
					outLine.append("\r\n"); // end of a row
					matrixRow++; // increase the row number if we are onto another row
				}
				
			}
			for(int i=0; i<=matrix.length;i++){
				outLine.append("--\t"); // make separator across entire line
			}
			outLine.append("\r\n"); // separate between channels between rows
		}
		IJ.append(outLine.toString(), f.getAbsolutePath());
	}
	
	/**
	 * Given the id of a signal group, make sure it is suitable to use
	 * @param signalGroup the group to check
	 */
	private void checkSignalGroup(UUID signalGroup){
		if(signalGroup==null){
			throw new IllegalArgumentException("Group is null");
		}
	}
	
	private void checkSignalGroup(String signalGroupName){
		if(signalGroupName==null){
			throw new IllegalArgumentException("Group is null");
		}
		if(!names.containsKey(signalGroupName)){
			throw new IllegalArgumentException("Group name not present: "+signalGroupName);
		}
	}
	
	// the print function bypasses all input checks to show everything present
	public void print(){
		for(UUID signalGroup : this.collection.keySet()){
			IJ.log("    Signal group "+signalGroup+": "+this.collection.get(signalGroup).size());
		}
	}
	
	/**
	 * Get the areas of signals in a channel
	 * @param channel the signal channel
	 * @return the areas
	 * @throws Exception 
	 */
	public List<Double> getStatistics(SignalStatistic stat, MeasurementScale scale, UUID signalGroup) {
		List<NuclearSignal> list = getSignals(signalGroup);
		List<Double> result = new ArrayList<Double>(0);
		for(int i=0;i<list.size();i++){
			result.add(list.get(i).getStatistic(stat, scale));
		}
		return result;
	}
	
	
	public ImageProcessor getImage(UUID signalGroup){
		File f = this.sourceFiles.get(signalGroup);
		int channel = this.sourceChannels.get(signalGroup);
		
		return ImageImporter.getInstance().importImage(f, channel);
	}
		
	public String toString(){
		String s = "";
		s += "Signal groups: "+this.numberOfSignalGroups()+"\n";
		for(UUID group : collection.keySet()){
			s += "  "+group+": "
					+names.get(group)
					+" : Channel "
					+sourceChannels.get(group)
					+" : "+sourceFiles.get(group)
					+" : "+collection.get(group).size()
					+" signals "
					+"\n";
		}
		return s;
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		finest("\tReading signal collection");
		in.defaultReadObject();
		finest("\tRead signal collection");
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		finest("\tWriting signal collection");
		out.defaultWriteObject();
		finest("\tWrote signal collection");
	}
}
