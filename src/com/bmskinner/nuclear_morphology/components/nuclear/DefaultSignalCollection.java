/*******************************************************************************
 *  	Copyright (C) 2015, 2016 Ben Skinner
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
 *     GNU General Public License for more details. Gluten-free. May contain 
 *     traces of LDL asbestos. Avoid children using heavy machinery while under the
 *     influence of alcohol.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package com.bmskinner.nuclear_morphology.components.nuclear;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.stats.SignalStatistic;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;

import ij.process.ImageProcessor;

/**
 * The default implementation of the {@link ISignalCollection} interface
 * @author ben
 * @since 1.13.3
 *
 */
public class DefaultSignalCollection implements ISignalCollection {
	private static final long serialVersionUID = 1L;

	/**
	 * Holds the signals
	 */
	private Map<UUID, List<INuclearSignal>> collection = new LinkedHashMap<UUID, List<INuclearSignal>>();
		
	public DefaultSignalCollection(){}
	
	/**
	 * Duplicate a signal collection
	 * @param s
	 */
	public DefaultSignalCollection(ISignalCollection s){
				
		for(UUID group : s.getSignalGroupIDs()){
			
			List<INuclearSignal> list = new ArrayList<INuclearSignal>();
			
			for(INuclearSignal signal : s.getSignals(group)){
				list.add(  signal.duplicate());
			}
			
			collection.put(    group, list);
		}

	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.ISignalCollection#addSignalGroup(java.util.List, java.util.UUID, java.io.File, int)
	 */
	@Override
	public void addSignalGroup(List<INuclearSignal> list, UUID groupID, File sourceFile, int sourceChannel){
		if(list==null || Integer.valueOf(sourceChannel)==null || sourceFile==null || groupID==null){
			throw new IllegalArgumentException("Signal list or channel is null");
		}
		
		collection.put(    groupID, list);
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.ISignalCollection#getSignalGroupIDs()
	 */
	@Override
	public Set<UUID> getSignalGroupIDs(){
		return collection.keySet();
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.ISignalCollection#updateSignalGroupID(java.util.UUID, java.util.UUID)
	 */
	@Override
	public void updateSignalGroupID(UUID oldID, UUID newID){
		
		if( ! collection.containsKey(oldID)){
			// The nucleus does not have the old id - skip
			return;
		}
		
		List<INuclearSignal> list = collection.get(oldID);
		
		// Remove the old values
		collection.remove(    oldID);
		
		// Insert the new values
		collection.put(    newID, list);		
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.ISignalCollection#getSignalGroupNumber(java.util.UUID)
	 */
	@Override
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
	
	/* (non-Javadoc)
	 * @see components.nuclear.ISignalCollection#addSignal(components.nuclear.NuclearSignal, java.util.UUID)
	 */
	@Override
	public void addSignal(INuclearSignal n, UUID signalGroup){
		if(signalGroup==null){
			throw new IllegalArgumentException("Group is null");
		}
		
		if(collection.get(signalGroup)==null){
			// add new list when not present
			List<INuclearSignal> list = new ArrayList<INuclearSignal>();
			collection.put(signalGroup, list);
		}
		collection.get(signalGroup).add(n);
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.ISignalCollection#addSignals(java.util.List, java.util.UUID)
	 */
	@Override
	public void addSignals(List<INuclearSignal> list, UUID signalGroup){
		if(list==null){
			throw new IllegalArgumentException("Signal is null");
		}
		if(signalGroup==null){
			throw new IllegalArgumentException("Group is null");
		}
		collection.get(signalGroup).addAll(list);
	}

	
	/**
	 * Append a list of signals to the given signal group
	 * @param list the signals
	 * @param signalGroupName the signal group name
	 */
//	public void addSignals(List<NuclearSignal> list, String signalGroupName){
//		if(list==null){
//			throw new IllegalArgumentException("Signal or group is null");
//		}
//		checkSignalGroup(signalGroupName);
//		this.addSignals(list, names.get(signalGroupName));
//	}
	
	
	/* (non-Javadoc)
	 * @see components.nuclear.ISignalCollection#getSignals()
	 */
	@Override
	public List<List<INuclearSignal>> getSignals(){
		List<List<INuclearSignal>> result = new ArrayList<List<INuclearSignal>>(0);
		for(UUID signalGroup : this.getSignalGroupIDs()){
			result.add(getSignals(signalGroup));
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.ISignalCollection#getAllSignals()
	 */
	@Override
	public List<INuclearSignal> getAllSignals(){
		List<INuclearSignal> result = new ArrayList<INuclearSignal>(0);
		for(UUID signalGroup : this.getSignalGroupIDs()){
			result.addAll(getSignals(signalGroup));
		}
		return result;
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.ISignalCollection#getSignals(java.util.UUID)
	 */
	@Override
	public List<INuclearSignal> getSignals(UUID signalGroup){
//		checkSignalGroup(signalGroup);
		if(this.hasSignal(signalGroup)){
			return this.collection.get(signalGroup);
		} else {
			return new ArrayList<INuclearSignal>(0);
		}
	}
		
	/* (non-Javadoc)
	 * @see components.nuclear.ISignalCollection#getSourceFile(java.util.UUID)
	 */
	@Override
	public File getSourceFile(UUID signalGroup){
		if(collection.containsKey(signalGroup)){
			
			List<INuclearSignal> list = collection.get(signalGroup);
			if(list!=null && ! list.isEmpty()){
				return list.get(0).getSourceFile();
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.ISignalCollection#updateSourceFile(java.util.UUID, java.io.File)
	 */
	@Override
	public void updateSourceFile(UUID signalGroup, File f){
		
		for(INuclearSignal s : collection.get(signalGroup)){
			s.setSourceFile(f);
		}
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.ISignalCollection#getSourceChannel(java.util.UUID)
	 */
	@Override
	public int getSourceChannel(UUID signalGroup){
		if(collection.containsKey(signalGroup)){
			
			List<INuclearSignal> list = collection.get(signalGroup);
			if(list!=null && ! list.isEmpty()){
				return list.get(0).getChannel();
			} else {
				return -1;
			}
		} else {
			return -1;
		}
		
	}
		
	/* (non-Javadoc)
	 * @see components.nuclear.ISignalCollection#numberOfSignalGroups()
	 */
	@Override
	public int size(){
		return collection.size();
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.ISignalCollection#numberOfSignals()
	 */
	@Override
	public int numberOfSignals(){
		int count=0;
		for(UUID group : collection.keySet()){
			count += numberOfSignals(group);
		}
		return count;
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.ISignalCollection#hasSignal(java.util.UUID)
	 */
	@Override
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
	
	/* (non-Javadoc)
	 * @see components.nuclear.ISignalCollection#hasSignal()
	 */
	@Override
	public boolean hasSignal(){	
		return  !collection.isEmpty();
	}
		
	/* (non-Javadoc)
	 * @see components.nuclear.ISignalCollection#numberOfSignals(java.util.UUID)
	 */
	@Override
	public int numberOfSignals(UUID signalGroup){
		if(signalGroup==null){
			return 0;
		}
		
		if(this.hasSignal(signalGroup)){
			return collection.get(signalGroup).size();
		} else {
			return 0;
		}
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.ISignalCollection#removeSignals()
	 */
	@Override
	public void removeSignals(){
		collection     = new LinkedHashMap<UUID, List<INuclearSignal>>();
	}
	
	/* (non-Javadoc)
	 * @see components.nuclear.ISignalCollection#removeSignals(java.util.UUID)
	 */
	@Override
	public void removeSignals(UUID signalGroup){
		collection.remove(signalGroup);
	}	
		
	
	/* (non-Javadoc)
	 * @see components.nuclear.ISignalCollection#getStatistics(stats.SignalStatistic, components.generic.MeasurementScale, java.util.UUID)
	 */
	@Override
	public List<Double> getStatistics(SignalStatistic stat, MeasurementScale scale, UUID signalGroup) {
		List<INuclearSignal> list = getSignals(signalGroup);
		List<Double> result = new ArrayList<Double>(0);
		for(int i=0;i<list.size();i++){
			result.add(list.get(i).getStatistic(stat, scale));
		}
		return result;
	}
	
	
	/* (non-Javadoc)
	 * @see components.nuclear.ISignalCollection#getImage(java.util.UUID)
	 */
	@Override
	public ImageProcessor getImage(UUID signalGroup) throws UnloadableImageException{
		File f = this.getSourceFile(signalGroup);
		int c  = this.getSourceChannel(signalGroup);
		
		try {
			return new ImageImporter(f).importImage(c);
		} catch (ImageImportException e) {
			fine("Error importing image source file "+f.getAbsolutePath(), e);
			throw new  UnloadableImageException("Unable to load signal image",e);
		}
	}
		
	/* (non-Javadoc)
	 * @see components.nuclear.ISignalCollection#toString()
	 */
	@Override
	public String toString(){
		
		StringBuilder b = new StringBuilder("Signal groups: ");
		b.append(size());
		b.append("\n ");

		for(UUID group : collection.keySet()){
			b.append(group);
			b.append(": ");
			b.append(" : Channel: ");
			b.append(this.getSourceChannel(group));
			b.append(" : File: ");
			b.append(this.getSourceFile(group));
			b.append("\n");
		}
		return b.toString();
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}
	
	private void writeObject(java.io.ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}
}
