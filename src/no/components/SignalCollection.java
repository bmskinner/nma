package no.components;

import ij.IJ;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This holds all the signals within a nucleus, within a hash.
 * The hash key is the channel number from which they were found,
 * and links to the channel number in the ImageStack for the nucleus
 *
 */
public class SignalCollection {

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
	
	/**
	 * Add a single signal to the given channel
	 * @param n the signal
	 * @param channel the channel
	 */
	public void addSignal(NuclearSignal n, int channel){
		if(n==null || Integer.valueOf(channel)==null){
			throw new IllegalArgumentException("Signal or channel is null");
		}
		if(channel>collection.size()){
			throw new IllegalArgumentException("Channel is out of range");
		}
		if(channel==0){
			throw new IllegalArgumentException("Channel 0 is reserved for nucleus");
		}
		collection.get(channel).add(n);
	}
	
	/**
	 * Append a list of signals to the given channel
	 * @param list the signals
	 * @param channel the channel
	 */
	public void addSignals(List<NuclearSignal> list, int channel){
		if(list==null || Integer.valueOf(channel)==null){
			throw new IllegalArgumentException("Signal or channel is null");
		}
		if(channel>collection.size()){
			throw new IllegalArgumentException("Channel is out of range");
		}
		if(channel==0){
			throw new IllegalArgumentException("Channel 0 is reserved for nucleus");
		}
		collection.get(channel).addAll(list);
	}

	
	/**
	 * Append a list of signals to the given channel
	 * @param list the signals
	 * @param channel the channel name
	 */
	public void addSignals(List<NuclearSignal> list, String channel){
		if(list==null || channel==null){
			throw new IllegalArgumentException("Signal or channel is null");
		}
		this.addSignals(list, names.get(channel));
	}
	
	/**
	 * Get the signals in the given channel
	 * @param channel the channel number
	 * @return a list of signals
	 */
	public List<NuclearSignal> getSignals(int channel){
		if(Integer.valueOf(channel)==null){
			throw new IllegalArgumentException("Channel is null");
		}
		if(channel>collection.size()){
			throw new IllegalArgumentException("Channel is out of range");
		}
		if(channel==0){
			throw new IllegalArgumentException("Channel 0 is reserved for nucleus");
		}
		return collection.get(channel);
	}
	
	/**
	 * Get the signals in the given channel
	 * @param channel the channel name
	 * @return a list of signals
	 */
	public List<NuclearSignal> getSignals(String channel){
		if(channel==null){
			throw new IllegalArgumentException("Channel is null");
		}
		if(!names.containsKey(channel)){
			throw new IllegalArgumentException("Channel name is not present");
		}	
		return this.getSignals(names.get(channel));
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
		for(int i=1; i<=collection.size(); i++){
			count += numberOfSignals(i);
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
	
	/**
	 * Get the total number of signals in a given channel
	 * @param channel the channel
	 * @return the count
	 */
	public int numberOfSignals(int channel){
		if(Integer.valueOf(channel)==null){
			throw new IllegalArgumentException("Channel is null");
		}
		if(!collection.containsKey(channel)){
			throw new IllegalArgumentException("Channel not present in collection: "+channel);
		}
		return collection.get(channel).size();
	}
	
	/**
	 * Get the total number of signals in a given channel
	 * @param channel the channel name
	 * @return the count
	 */
	public int numberOfSignals(String channel){
		if(channel==null){
			throw new IllegalArgumentException("Channel is null");
		}
		if(!names.containsKey(channel)){
			throw new IllegalArgumentException("Channel name not present: "+channel);
		}
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
		
		for( int i : getChannels()){
			List<NuclearSignal> signalsRow = getSignals(i);

			if(!signalsRow.isEmpty()){

				for(NuclearSignal row : signalsRow){
					
					matrixCol=0;

					XYPoint aCoM = row.getCentreOfMass();

					for( int j : getChannels()){
						List<NuclearSignal> signalsCol = getSignals(j);

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
		int matrixRow = 0;
		int matrixCol = 0;

		File f = new File(outputFolder.getAbsolutePath()+File.separator+"signalDistanceMatrix.txt");
		if(f.exists()){
			f.delete();
		}

		int matrixSize = matrix.length;
		StringBuilder outLine = new StringBuilder("Signal\t");
		
		// prepare the column headings
		for( int i : getChannels()){
			List<NuclearSignal> signalsRow = getSignals(i);
			
			if(!signalsRow.isEmpty()){

				for(NuclearSignal row : signalsRow){
					if(names.containsValue(i)){ // if a name has been set for the channel, use it
						outLine.append("SIGNAL_"+i+"_"+getChannelName(i).toUpperCase()+"\t");
							
					} else { // otherwise just the signal number
					outLine.append("SIGNAL_"+i+"\t");
					}
				}
				outLine.append("|\t"); // separator between signal channels
			}
		}
		outLine.append("\r\n");

		// add the rows of values
		for( int i : getChannels()){
			List<NuclearSignal> signalsRow = getSignals(i);
			

			if(!signalsRow.isEmpty()){
				
				outLine.append("SIGNAL_"+matrixRow);

				for(NuclearSignal row : signalsRow){
					
					matrixCol=0;

					for( int j : getChannels()){
						List<NuclearSignal> signalsCol = getSignals(j);

						if(!signalsCol.isEmpty()){

							for(NuclearSignal col : signalsCol){
								outLine.append(matrix[matrixRow][matrixCol]+"\t");
								matrixCol++;
							}
						}
						outLine.append("|\t");
					}
					matrixRow++;
					outLine.append("\r\n");
				}
			}
		}
		IJ.append(outLine.toString(), f.getAbsolutePath());
	}
}
