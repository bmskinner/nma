package no.components;

import ij.IJ;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import no.utility.ImageImporter;

/**
 * This holds all the signals within a nucleus, within a hash.
 * The hash key is the channel number from which they were found,
 * and links to the channel number in the ImageStack for the nucleus.
 *
 */
public class SignalCollection implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Holds the signals
	 */
	private Map<Integer, ArrayList<NuclearSignal>> collection = new HashMap<Integer, ArrayList<NuclearSignal>>();
	
	/**
	 * Holds the names of the channels for presentation purposes
	 */
	private Map<String, Integer> names = new HashMap<String, Integer>();
	
	public SignalCollection(){
		
	}
	
	public SignalCollection(SignalCollection s){
		
	}
	
	public void addChannel(ArrayList<NuclearSignal> list, int channel){
		if(list==null || Integer.valueOf(channel)==null){
			throw new IllegalArgumentException("Signal list or channel is null");
		}
		if(channel==ImageImporter.COUNTERSTAIN){
			throw new IllegalArgumentException("Channel is reserved for nucleus");
		}
		collection.put(channel, list);
	}
	
	/**
	 * Add a single signal to the given channel
	 * @param n the signal
	 * @param channel the channel
	 */
	public void addSignal(NuclearSignal n, int channel){
		checkChannel(channel);
		collection.get(channel).add(n);
	}
	
	/**
	 * Append a list of signals to the given channel
	 * @param list the signals
	 * @param channel the channel
	 */
	public void addSignals(List<NuclearSignal> list, int channel){
		if(list==null){
			throw new IllegalArgumentException("Signal is null");
		}
		checkChannel(channel);
		collection.get(channel).addAll(list);
	}

	
	/**
	 * Append a list of signals to the given channel
	 * @param list the signals
	 * @param channel the channel name
	 */
	public void addSignals(List<NuclearSignal> list, String channel){
		if(list==null){
			throw new IllegalArgumentException("Signal or channel is null");
		}
		checkChannel(channel);
		this.addSignals(list, names.get(channel));
	}
	
	
	/**
	 * Get all the signals in all planes, as a list of lists
	 * @return the list of signal lists
	 */
	public ArrayList<List<NuclearSignal>> getSignals(){
		ArrayList<List<NuclearSignal>> result = new ArrayList<List<NuclearSignal>>(0);
		for(int channel : this.getChannels()){
			result.add(getSignals(channel));
		}
		return result;
	}
	
	/**
	 * Get the signals in the given channel
	 * @param channel the channel number
	 * @return a list of signals
	 */
	public List<NuclearSignal> getSignals(int channel){
		checkChannel(channel);
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
		checkChannel(channel);
		if(this.hasSignal(channel)){
			return this.collection.get(names.get(channel));
		} else {
			return new ArrayList<NuclearSignal>(0);
		}
	}
	
	/**
	 * Set the channel name
	 * @param channel the channel to name
	 * @param name the new name
	 */
	public void setChannelName(int channel, String name){
		if(Integer.valueOf(channel)==null || name==null){
			throw new IllegalArgumentException("Channel or name is null");
		}
		names.put(name, channel);
	}
	
	public String getChannelName(int channel){
		if(Integer.valueOf(channel)==null){
			throw new IllegalArgumentException("Channel is null");
		}
		if(!names.containsValue(channel)){
			throw new IllegalArgumentException("Channel name is not present");
		}
		String result = null;
		for(String name : getChannelNames()){
			if(names.get(name)==channel){
				result=name;
			}
		}
		return result;
	}
	
	/**
	 * Get the names of channels which have been named; ignores unnamed channels
	 * @return the set of names
	 */
	public Set<String> getChannelNames(){
		return names.keySet();
	}
	
	/**
	 * Get the channel codes
	 * @return the set of names
	 */
	public Set<Integer> getChannels(){
		return collection.keySet();
	}
	
	/**
	 * Get the number of signal channels
	 * @return the number of signal channels
	 */
	public int numberOfChannels(){
		return collection.size();
	}
	
	/**
	 * Get the total number of signals in all channels
	 * @return the count
	 */
	public int numberOfSignals(){
		int count=0;
		for(int channel : collection.keySet()){
			count += numberOfSignals(channel);
		}
		return count;
	}
	
	public boolean hasSignal(int channel){
		if(Integer.valueOf(channel)==null){
			throw new IllegalArgumentException("Channel is null");
		}
		if(!collection.containsKey(channel)){
			return false;
		}
		if(collection.get(channel).isEmpty()){
			return false;
		} else {
			return true;
		}
	}
	
	public boolean hasSignal(String channel){
		if(channel==null){
			throw new IllegalArgumentException("Channel is null");
		}
		if(!collection.containsValue(channel)){
			return false;
		}
		if(collection.get(channel).isEmpty()){
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
	public int numberOfSignals(int channel){
		checkChannel(channel);
		return collection.get(channel).size();
	}
	
	/**
	 * Get the total number of signals in a given channel
	 * @param channel the channel name
	 * @return the count
	 */
	public int numberOfSignals(String channel){
		checkChannel(channel);
		return numberOfSignals(names.get(channel));
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
	private void checkChannel(int channel){
		if(Integer.valueOf(channel)==null){
			throw new IllegalArgumentException("Channel is null");
		}
		if(channel==ImageImporter.COUNTERSTAIN){
			throw new IllegalArgumentException("Channel is reserved for nucleus");
		}
	}
	
	private void checkChannel(String channel){
		if(channel==null){
			throw new IllegalArgumentException("Channel is null");
		}
		if(!names.containsKey(channel)){
			throw new IllegalArgumentException("Channel name not present: "+channel);
		}
	}
	
	// the print function bypasses all input checks to show everything present
	public void print(){
		for(int channel : this.collection.keySet()){
			IJ.log("    Channel "+channel+": "+this.collection.get(channel).size());
		}
	}
}
