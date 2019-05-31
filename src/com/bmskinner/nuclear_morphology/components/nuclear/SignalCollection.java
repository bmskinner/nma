/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nuclear_morphology.components.nuclear;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.ImageImporter.ImageImportException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.process.ImageProcessor;

/**
 * This holds all the signals within a nucleus, within a hash. The hash key is
 * the signal group number from which they were found. The signal group number
 * accesses (a) the nuclear signals (b) the file they came from and (c) the
 * channel within the file they came from and links to the channel number in the
 * ImageStack for the nucleus.
 */
@Deprecated
public class SignalCollection implements ISignalCollection {
	
	private static final Logger LOGGER = Logger.getLogger(Loggable.ROOT_LOGGER);

    private static final long serialVersionUID = 1L;

    /**
     * Holds the signals
     */
    private Map<UUID, List<INuclearSignal>> collection = new LinkedHashMap<UUID, List<INuclearSignal>>();

    // the files that hold the image for the given channel
    private Map<UUID, File> sourceFiles = new HashMap<UUID, File>(0);

    // the channel with the signal in the source image
    private Map<UUID, Integer> sourceChannels = new HashMap<UUID, Integer>(0);

    public SignalCollection() {
    }

    /**
     * Duplicate a signal collection
     * 
     * @param s
     */
    public SignalCollection(ISignalCollection s) {

        for (UUID group : s.getSignalGroupIds()) {

            // String groupName = s.names.get(group);
            int channel = s.getSourceChannel(group);
            File f = new File(s.getSourceFile(group).getAbsolutePath());

            List<INuclearSignal> list = new ArrayList<INuclearSignal>();

            for (INuclearSignal signal : s.getSignals(group)) {
                list.add(signal.duplicate());
            }

            collection.put(group, list);
            sourceFiles.put(group, f);
            sourceChannels.put(group, channel);
            // names.put( group, groupName);
        }

    }
    
    @Override
	public ISignalCollection duplicate() {
		return new SignalCollection(this);
	}


    @Override
    public void addSignalGroup(List<INuclearSignal> list, UUID groupID, File sourceFile, int sourceChannel) {
        if (list == null || Integer.valueOf(sourceChannel) == null || sourceFile == null || groupID == null) {
            throw new IllegalArgumentException("Signal list or channel is null");
        }

        collection.put(groupID, list);
        sourceFiles.put(groupID, sourceFile);
        sourceChannels.put(groupID, sourceChannel);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.ISignalCollection#getSignalGroupIDs()
     */
    @Override
    public Set<UUID> getSignalGroupIds() {
        return collection.keySet();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.nuclear.ISignalCollection#updateSignalGroupID(java.util.UUID,
     * java.util.UUID)
     */
    @Override
    public void updateSignalGroupId(UUID oldID, UUID newID) {

        if (!collection.containsKey(oldID)) {
            // The nucleus does not have the old id - skip
            return;
        }

        List<INuclearSignal> list = collection.get(oldID);
        File sourceFile = sourceFiles.get(oldID);
        int sourceChannel = sourceChannels.get(oldID);
        // String name = names.get(oldID);

        // Remove the old values
        collection.remove(oldID);
        sourceFiles.remove(oldID);
        sourceChannels.remove(oldID);
        // names.remove( oldID);

        // Insert the new values
        collection.put(newID, list);
        sourceFiles.put(newID, sourceFile);
        sourceChannels.put(newID, sourceChannel);
        // names.put( newID, name);

    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.ISignalCollection#addSignal(components.nuclear.
     * NuclearSignal, java.util.UUID)
     */
    @Override
    public void addSignal(INuclearSignal n, UUID signalGroup) {
        checkSignalGroup(signalGroup);
        collection.get(signalGroup).add(n);
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.ISignalCollection#addSignals(java.util.List,
     * java.util.UUID)
     */
    @Override
    public void addSignals(List<INuclearSignal> list, UUID signalGroup) {
        if (list == null) {
            throw new IllegalArgumentException("Signal is null");
        }
        checkSignalGroup(signalGroup);
        collection.get(signalGroup).addAll(list);
    }

    /**
     * Append a list of signals to the given signal group
     * 
     * @param list
     *            the signals
     * @param signalGroupName
     *            the signal group name
     */
    // public void addSignals(List<NuclearSignal> list, String signalGroupName){
    // if(list==null){
    // throw new IllegalArgumentException("Signal or group is null");
    // }
    // checkSignalGroup(signalGroupName);
    // this.addSignals(list, names.get(signalGroupName));
    // }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.ISignalCollection#getSignals()
     */
    @Override
    public List<List<INuclearSignal>> getSignals() {
        ArrayList<List<INuclearSignal>> result = new ArrayList<List<INuclearSignal>>(0);
        for (UUID signalGroup : this.getSignalGroupIds()) {
            result.add(getSignals(signalGroup));
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.ISignalCollection#getAllSignals()
     */
    @Override
    public List<INuclearSignal> getAllSignals() {
        List<INuclearSignal> result = new ArrayList<INuclearSignal>(0);
        for (UUID signalGroup : this.getSignalGroupIds()) {
            result.addAll(getSignals(signalGroup));
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.ISignalCollection#getSignals(java.util.UUID)
     */
    @Override
    public List<INuclearSignal> getSignals(UUID signalGroup) {
        // checkSignalGroup(signalGroup);
        if (this.hasSignal(signalGroup)) {
            return this.collection.get(signalGroup);
        } else {
            return new ArrayList<INuclearSignal>(0);
        }
    }

    /**
     * Get the signals in the given channel
     * 
     * @param channel
     *            the channel name
     * @return a list of signals
     */
    // public List<NuclearSignal> getSignals(String channel){
    // checkSignalGroup(channel);
    // if(this.hasSignal(channel)){
    // return this.collection.get(names.get(channel));
    // } else {
    // return new ArrayList<NuclearSignal>(0);
    // }
    // }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.ISignalCollection#getSourceFile(java.util.UUID)
     */
    @Override
    public File getSourceFile(UUID signalGroup) {
        return this.sourceFiles.get(signalGroup);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.nuclear.ISignalCollection#updateSourceFile(java.util.UUID,
     * java.io.File)
     */
    @Override
    public void updateSourceFile(UUID signalGroup, File f) {
        this.sourceFiles.put(signalGroup, f);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.nuclear.ISignalCollection#getSourceChannel(java.util.UUID)
     */
    @Override
    public int getSourceChannel(UUID signalGroup) {
        return this.sourceChannels.get(signalGroup);
    }

    // /**
    // * Set the channel name
    // * @param channel the channel to name
    // * @param name the new name
    // */
    // public void setSignalGroupName(UUID signalGroup, String name){
    // if(signalGroup==null || name==null){
    // throw new IllegalArgumentException("Channel or name is null");
    // }
    // names.put(signalGroup, name);
    // }

    /**
     * Get the signal group with the given name
     * 
     * @param signalGroupName
     * @return
     */
    // public UUID getSignalGroup(String signalGroupName){
    // if(signalGroupName==null){
    // throw new IllegalArgumentException("Signal group name is null");
    // }
    // if(!names.containsValue(signalGroupName)){
    // throw new IllegalArgumentException("Signal group name is not present");
    // }
    //
    // for(UUID signalGroup : names.keySet()){
    // if(names.get(signalGroup).equals(signalGroupName)){
    // return signalGroup;
    // }
    // }
    // return null;
    // }

    // public String getSignalGroupName(UUID signalGroup){
    // if(signalGroup==null){
    // throw new IllegalArgumentException("Channel is null");
    // }
    // if(!names.containsKey(signalGroup)){
    // throw new IllegalArgumentException("Channel name is not present");
    // }
    // String result = names.get(signalGroup);
    // return result;
    // }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.nuclear.ISignalCollection#getSignalChannel(java.util.UUID)
     */
    // @Override
    // public int getSignalChannel(UUID signalGroup){
    // if(signalGroup==null){
    // throw new IllegalArgumentException("Channel is null");
    // }
    // return this.sourceChannels.get(signalGroup);
    // }

    // /**
    // * Get the names of signal groups which have been named; ignores unnamed
    // channels
    // * @return the set of names
    // */
    // public Collection<String> getSignalGroupNames(){
    // return names.values();
    // }

    /**
     * Get the set of signal groups in this collection
     * 
     * @return the set of integer group numbers
     */
    // public Set<Integer> getSignalGroups(){
    // return names.keySet();
    // }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.ISignalCollection#numberOfSignalGroups()
     */
    @Override
    public int size() {
        return collection.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.ISignalCollection#numberOfSignals()
     */
    @Override
    public int numberOfSignals() {
        int count = 0;
        for (UUID group : collection.keySet()) {
            count += numberOfSignals(group);
        }
        return count;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.ISignalCollection#hasSignal(java.util.UUID)
     */
    @Override
    public boolean hasSignal(UUID signalGroup) {
        if (signalGroup == null) {
            throw new IllegalArgumentException("Signal group is null");
        }
        if (!collection.containsKey(signalGroup)) {
            return false;
        }
        if (collection.get(signalGroup).isEmpty()) {
            return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.ISignalCollection#hasSignal()
     */
    @Override
    public boolean hasSignal() {

        return !collection.isEmpty();
    }

    // /**
    // * Check if the signal group contains signals in this collection
    // * @param signalGroup the group name
    // * @return yes or no
    // */
    // public boolean hasSignal(String signalGroupName){
    // if(signalGroupName==null){
    // throw new IllegalArgumentException("Channel is null");
    // }
    // if(!collection.containsValue(signalGroupName)){
    // return false;
    // }
    // if(collection.get(signalGroupName).isEmpty()){
    // return false;
    // } else {
    // return true;
    // }
    // }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.ISignalCollection#numberOfSignals(java.util.UUID)
     */
    @Override
    public int numberOfSignals(UUID signalGroup) {
        if (signalGroup == null) {
            return 0;
        }

        if (this.hasSignal(signalGroup)) {
            return collection.get(signalGroup).size();
        } else {
            return 0;
        }
    }

    // /**
    // * Get the total number of signals in a given signal group
    // * @param signalGroup the group name
    // * @return the count
    // */
    // public int numberOfSignals(String signalGroupName){
    // checkSignalGroup(signalGroupName);
    // return numberOfSignals(names.get(signalGroupName));
    // }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.ISignalCollection#removeSignals()
     */
    @Override
    public void removeSignals() {
        collection = new LinkedHashMap<UUID, List<INuclearSignal>>();
        sourceFiles = new HashMap<UUID, File>(0);
        sourceChannels = new HashMap<UUID, Integer>(0);
        // names = new HashMap<UUID, String>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.ISignalCollection#removeSignals(java.util.UUID)
     */
    @Override
    public void removeSignals(UUID signalGroup) {
        collection.remove(signalGroup);
        sourceFiles.remove(signalGroup);
        sourceChannels.remove(signalGroup);
        // names.remove(signalGroup);
    }

    /**
     * Find the pairwise distances between all signals in the nucleus
     */
    public double[][] calculateDistanceMatrix(MeasurementScale scale) {

        // create a matrix to hold the data
        // needs to be between every signal and every other signal, irrespective
        // of colour
        int matrixSize = this.numberOfSignals();

        double[][] matrix = new double[matrixSize][matrixSize];

        int matrixRow = 0;
        int matrixCol = 0;

        for (List<INuclearSignal> signalsRow : getSignals()) {

            if (!signalsRow.isEmpty()) {

                for (INuclearSignal row : signalsRow) {

                    matrixCol = 0;

                    IPoint aCoM = row.getCentreOfMass();

                    for (List<INuclearSignal> signalsCol : getSignals()) {

                        if (!signalsCol.isEmpty()) {

                            for (INuclearSignal col : signalsCol) {
                                IPoint bCoM = col.getCentreOfMass();
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
    //
    // /**
    // * Export the pairwise distances between all signals to the given folder
    // * @param outputFolder the folder to export to
    // */
    // public void exportDistanceMatrix(File outputFolder){
    //
    // double[][] matrix = calculateDistanceMatrix();
    //
    // File f = new
    // File(outputFolder.getAbsolutePath()+File.separator+"signalDistanceMatrix.txt");
    // if(f.exists()){
    // f.delete();
    // }
    //
    //// int matrixSize = matrix.length;
    // StringBuilder outLine = new StringBuilder("Signal\t");
    //
    // // prepare the column headings
    // int col = 0;
    // for(List<NuclearSignal> signalsRow : getSignals()){
    //
    // if(!signalsRow.isEmpty()){
    //
    // for(NuclearSignal s : signalsRow){
    // outLine.append("SIGNAL_"+col+"\t");
    // col++;
    // }
    // }
    // outLine.append("|\t"); // separator between signal channels
    // }
    // outLine.append("\r\n");
    //
    // // add the rows of values
    // int matrixRow = 0;
    // int matrixCol = 0;
    // for(List<NuclearSignal> imagePlane : getSignals()){
    //
    //
    // if(!imagePlane.isEmpty()){
    //
    //
    // for(NuclearSignal s : imagePlane){ // go through all the signals, row by
    // row
    // matrixCol=0; // begin a new column
    // outLine.append("SIGNAL_"+matrixRow+"\t");
    // // within the row, get all signals as a column
    // for(List<NuclearSignal> signalsCol : getSignals()){
    //
    // if(!signalsCol.isEmpty()){
    // for(NuclearSignal c : signalsCol){
    // outLine.append(matrix[matrixRow][matrixCol]+"\t");
    // matrixCol++;
    // }
    // }
    // outLine.append("|\t"); // separate between channels within row
    // }
    //
    // outLine.append("\r\n"); // end of a row
    // matrixRow++; // increase the row number if we are onto another row
    // }
    //
    // }
    // for(int i=0; i<=matrix.length;i++){
    // outLine.append("--\t"); // make separator across entire line
    // }
    // outLine.append("\r\n"); // separate between channels between rows
    // }
    // IJ.append(outLine.toString(), f.getAbsolutePath());
    // }

    /**
     * Given the id of a signal group, make sure it is suitable to use
     * 
     * @param signalGroup
     *            the group to check
     */
    private void checkSignalGroup(UUID signalGroup) {
        if (signalGroup == null) {
            throw new IllegalArgumentException("Group is null");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * components.nuclear.ISignalCollection#getStatistics(stats.SignalStatistic,
     * components.generic.MeasurementScale, java.util.UUID)
     */
    // @Override
    // public List<Double> getStatistics(SignalStatistic stat, MeasurementScale
    // scale, UUID signalGroup) {
    // List<INuclearSignal> list = getSignals(signalGroup);
    // List<Double> result = new ArrayList<Double>(0);
    // for(int i=0;i<list.size();i++){
    // result.add(list.get(i).getStatistic(stat, scale));
    // }
    // return result;
    // }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.ISignalCollection#getImage(java.util.UUID)
     */
    @Override
    public ImageProcessor getImage(UUID signalGroup) {
        File f = this.sourceFiles.get(signalGroup);
        int channel = this.sourceChannels.get(signalGroup);

        try {
            return new ImageImporter(f).importImageAndInvert(channel);
        } catch (ImageImportException e) {
            LOGGER.log(Loggable.STACK, "Error importing image source file " + f.getAbsolutePath(), e);
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see components.nuclear.ISignalCollection#toString()
     */
    @Override
    public String toString() {
        String s = "";
        s += "Signal groups: " + this.size() + "\n";
        for (UUID group : collection.keySet()) {
            s += "  " + group + ": " + " : Channel " + sourceChannels.get(group) + " : " + sourceFiles.get(group)
                    + " : " + collection.get(group).size() + " signals " + "\n";
        }
        return s;
    }

    /**
     * For each signal group pair, find the smallest pairwise distance between
     * signals in the collection.
     * 
     * @return a list of shortest distances for each pairwise group
     */
    @Override
    public List<PairwiseSignalDistanceValue> calculateSignalColocalisation(MeasurementScale scale) {

        List<PairwiseSignalDistanceValue> result = new ArrayList<PairwiseSignalDistanceValue>();

        for (UUID id1 : this.getSignalGroupIds()) {

            List<INuclearSignal> signalList1 = this.getSignals(id1);

            for (UUID id2 : this.getSignalGroupIds()) {

                if (id1.equals(id2)) {
                    continue;
                }

                List<INuclearSignal> signalList2 = this.getSignals(id2);

                // Compare all signal pairwise distances between groups 1 and 2
                double smallest = Double.MAX_VALUE;
                for (INuclearSignal s1 : signalList1) {

                    for (INuclearSignal s2 : signalList2) {

                        double distance = s1.getCentreOfMass().getLengthTo(s2.getCentreOfMass());
                        smallest = distance < smallest ? distance : smallest;

                    }

                }

                // Assign the pairwise distance
                PairwiseSignalDistanceValue p = new PairwiseSignalDistanceValue(id1, id2, smallest);
                result.add(p);

            }
        }
        return result;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        // finest("\tReading signal collection");
        in.defaultReadObject();
        // finest("\tRead signal collection");
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        // finest("\tWriting signal collection");
        out.defaultWriteObject();
        // finest("\tWrote signal collection");
    }

    /**
     * Calculate the shortest distances between signals in the given signal
     * groups. Each signal is considered only once. Hence a group with 4 signals
     * compared to a group with 3 signals will produce a list of 3 values.
     * 
     * @param id1
     *            the first signal group
     * @param id2
     *            the second signal group
     * @return a list of the pixel distances between paired signals
     */
    @Override
    public List<Colocalisation<INuclearSignal>> calculateColocalisation(UUID id1, UUID id2) {

        if (id1.equals(id2)) {
            throw new IllegalArgumentException("Signal IDs are the same");
        }

        Set<INuclearSignal> d1 = new HashSet<INuclearSignal>(this.getSignals(id1));
        Set<INuclearSignal> d2 = new HashSet<INuclearSignal>(this.getSignals(id2));

        List<Colocalisation<INuclearSignal>> result = findColocalisingSignals(d1, d2);

        return result;

    }

    /**
     * Recursively find signal pairs with the shortest distance between them.
     * 
     * @param d1
     *            the nuclear signals in group 1
     * @param d2
     *            the nuclear signals in group 2
     * @param scale
     *            the measurement scale
     * @return a list of best colocalising signals
     */
    private List<Colocalisation<INuclearSignal>> findColocalisingSignals(Set<INuclearSignal> d1,
            Set<INuclearSignal> d2) {

        List<Colocalisation<INuclearSignal>> result = new ArrayList<Colocalisation<INuclearSignal>>();

        if (d2.isEmpty() || d1.isEmpty()) {
            return result;
        }

        double smallest = Double.MAX_VALUE;

        INuclearSignal chosen1 = null, chosen2 = null;

        // Check all pairwise comparisons before returning a Colocalisation
        // in case the set lengths are unequal
        Iterator<INuclearSignal> it1 = d1.iterator();
        while (it1.hasNext()) {

            INuclearSignal s1 = it1.next();

            Iterator<INuclearSignal> it2 = d2.iterator();
            while (it2.hasNext()) {
                INuclearSignal s2 = it2.next();
                double distance = s1.getCentreOfMass().getLengthTo(s2.getCentreOfMass());

                boolean smaller = distance < smallest;

                // Replace selected signals if closer
                smallest = smaller ? distance : smallest;
                chosen2 = smaller ? s2 : chosen2;
                chosen1 = smaller ? s1 : chosen1;
            }
        }

        // Make a cColocalisation from the best pair

        if (chosen1 != null && chosen2 != null) {

            Colocalisation<INuclearSignal> col = new Colocalisation<INuclearSignal>(chosen1, chosen2);
            d1.remove(chosen1);
            d2.remove(chosen2);
            result.add(col);

            if (!d1.isEmpty() && !d2.isEmpty()) {
                result.addAll(findColocalisingSignals(d1, d2));
            }
        }

        return result;
    }

    @Override
    public List<Double> getStatistics(PlottableStatistic stat, MeasurementScale scale, UUID signalGroup) {
        // TODO Auto-generated method stub
        LOGGER.warning("Unimplemented method in " + this.getClass().getName());
        return null;
    }
}
