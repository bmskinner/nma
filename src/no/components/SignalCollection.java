package no.components;

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
	
	/**
	 * Get the names of channels which have been named; ignores unnamed channels
	 * @return the set of names
	 */
	public Set<String> getChannelNames(){
		return names.keySet();
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
	
	/**
	 * Get the total number of signals in a given channel
	 * @param channel the channel
	 * @return the count
	 */
	public int numberOfSignals(int channel){
		return collection.get(channel).size();
	}
	
	/**
	 * Get the total number of signals in a given channel
	 * @param channel the channel name
	 * @return the count
	 */
	public int numberOfSignals(String channel){
		return numberOfSignals(names.get(channel));
	}

}
