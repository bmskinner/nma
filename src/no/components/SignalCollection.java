package no.components;

import ij.IJ;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import utility.Constants;
import no.imports.ImageImporter;

/**
 * This holds all the signals within a nucleus, within a hash.
 * The hash key is the signal group number from which they were found.
 * The signal group number accesses (a) the nuclear signals (b) the file
 *  they came from and (c) the channel within the file they came from 
 * and links to the channel number in the ImageStack for the nucleus.
 */
public class SignalCollection implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Holds the signals
	 */
	private Map<Integer, ArrayList<NuclearSignal>> collection = new HashMap<Integer, ArrayList<NuclearSignal>>();
	
	// the files that hold the image for the given channel
	private Map<Integer, File> sourceFiles = new HashMap<Integer, File>(0);
	
	// the channel with the signal in the source image
	private Map<Integer, Integer> sourceChannels = new HashMap<Integer, Integer>(0);
	
	/**
	 * Holds the names of the channels for presentation purposes
	 */
	private Map<Integer, String > names = new HashMap<Integer, String>();
	
	public SignalCollection(){
		
	}
	
	public SignalCollection(SignalCollection s){
		
	}
	
	/**
	 * Add a list of nuclear signals to the collection
	 * @param list the signals
	 * @param signalGroup the group id
	 * @param sourceFile the file the signals originated from
	 * @param sourceChannel the channel the signals originated from
	 */
	public void addSignalGroup(ArrayList<NuclearSignal> list, int signalGroup, File sourceFile, int sourceChannel){
		if(list==null || Integer.valueOf(sourceChannel)==null || sourceFile==null || Integer.valueOf(signalGroup)==null){
			throw new IllegalArgumentException("Signal list or channel is null");
		}
		
		collection.put(signalGroup, list);
		sourceFiles.put(signalGroup, sourceFile);
		sourceChannels.put(signalGroup, sourceChannel);
	}
	
	/**
	 * Add a single signal to the given signal group
	 * @param n the signal
	 * @param signalGroup the signal group
	 */
	public void addSignal(NuclearSignal n, int signalGroup){
		checkSignalGroup(signalGroup);
		collection.get(signalGroup).add(n);
	}
	
	/**
	 * Append a list of signals to the given signal group
	 * @param list the signals
	 * @param signalGroup the signal group
	 */
	public void addSignals(List<NuclearSignal> list, int signalGroup){
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
	 * Get all the signals in all signal groups, as a list of lists
	 * @return the list of signal lists
	 */
	public ArrayList<List<NuclearSignal>> getSignals(){
		ArrayList<List<NuclearSignal>> result = new ArrayList<List<NuclearSignal>>(0);
		for(int signalGroup : this.getSignalGroups()){
			result.add(getSignals(signalGroup));
		}
		return result;
	}
	
	/**
	 * Get the signals in the given channel
	 * @param channel the channel number
	 * @return a list of signals
	 */
	public List<NuclearSignal> getSignals(int channel){
		checkSignalGroup(channel);
		if(this.hasSignal(channel)){
			return this.collection.get(channel);
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
	public File getSourceFile(int signalGroup){
		return this.sourceFiles.get(signalGroup);
	}
	
	/**
	 * Get the channel containing the signals in the given signal group
	 * @param signalGroup the group id
	 * @return the RGB channel with the signals (0 if greyscale)
	 */
	public int getSourceChannel(int signalGroup){
		return this.sourceChannels.get(signalGroup);
	}
	
	/**
	 * Set the channel name
	 * @param channel the channel to name
	 * @param name the new name
	 */
	public void setSignalGroupName(int signalGroup, String name){
		if(Integer.valueOf(signalGroup)==null || name==null){
			throw new IllegalArgumentException("Channel or name is null");
		}
		names.put(signalGroup, name);
	}
	
	public String getSignalGroupName(int signalGroup){
		if(Integer.valueOf(signalGroup)==null){
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
	public int getSignalChannel(int signalGroup){
		if(Integer.valueOf(signalGroup)==null){
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
	 * Get the channel codes
	 * @return the set of names
	 */
	public Set<Integer> getSignalGroups(){
		return collection.keySet();
	}
	
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
		for(int group : collection.keySet()){
			count += numberOfSignals(group);
		}
		return count;
	}
	
	/**
	 * Check if the signal group contains signals in this collection
	 * @param signalGroup the group id
	 * @return yes or no
	 */
	public boolean hasSignal(int signalGroup){
		if(Integer.valueOf(signalGroup)==null){
			throw new IllegalArgumentException("Signal group is null");
		}
		if(!collection.containsKey(signalGroup)){
			return false;
		}
		if(collection.get(signalGroup).isEmpty()){
			return false;
		} else {
			return true;
		}
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
	public int numberOfSignals(int signalGroup){
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
	 * Given the id of a channel, make sure it is suitable to use
	 * @param channel the channel to check
	 */
	private void checkSignalGroup(int signalGroup){
		if(Integer.valueOf(signalGroup)==null){
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
		for(int signalGroup : this.collection.keySet()){
			IJ.log("    Signal group "+signalGroup+": "+this.collection.get(signalGroup).size());
		}
	}
	
	/**
	 * Get the areas of signals in a channel
	 * @param channel the signal channel
	 * @return the areas
	 */
	public List<Double> getAreas(int signalGroup){
		List<NuclearSignal> list = getSignals(signalGroup);
		List<Double> result = new ArrayList<Double>(0);
		for(int i=0;i<list.size();i++){
			result.add(list.get(i).getArea());
		}
		return result;
	}
	
	/**
	 * Get the angless of signals in a channel
	 * @param channel the signal channel
	 * @return the angles
	 */
	public List<Double> getAngles(int signalGroup){
		List<NuclearSignal> list = getSignals(signalGroup);
		List<Double> result = new ArrayList<Double>(0);
		for(int i=0;i<list.size();i++){
			result.add(list.get(i).getAngle());
		}
		return result;
	}
	
	/**
	 * Get the maximum feret diameters of signals in a channel
	 * @param channel the signal channel
	 * @return the ferets
	 */
	public List<Double> getFerets(int signalGroup){
		List<NuclearSignal> list = getSignals(signalGroup);
		List<Double> result = new ArrayList<Double>(0);
		for(int i=0;i<list.size();i++){
			result.add(list.get(i).getFeret());
		}
		return result;
	}
	
	/**
	 * Get the fractional distances of the signal centre of mass from the nucleus centre of mass
	 * @param channel the signal channel
	 * @return the distances
	 */
	public List<Double> getDistances(int signalGroup){
		List<NuclearSignal> list = getSignals(signalGroup);
		List<Double> result = new ArrayList<Double>(0);
		for(int i=0;i<list.size();i++){
			result.add(list.get(i).getFractionalDistanceFromCoM());
		}
		return result;
	}
}
