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
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.stats.SignalStatistic;
import com.bmskinner.nuclear_morphology.io.UnloadableImageException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.process.ImageProcessor;

/**
 * The collection of nuclear signals that can be found within a single 
 * nucleus. Implementing classes will track the signal group each signal 
 * belongs to, plus the source image the signal was detected in.
 * @author ben
 * @since 1.13.3
 *
 */
public interface ISignalCollection extends Serializable, Loggable {

	/**
	 * Add a list of nuclear signals to the collection
	 * @param list the signals
	 * @param groupID the group id - this should be consistent across all nuclei in a dataset
	 * @param sourceFile the file the signals originated from
	 * @param sourceChannel the channel the signals originated from
	 */
	void addSignalGroup(List<INuclearSignal> list, UUID groupID, File sourceFile, int sourceChannel);

	Set<UUID> getSignalGroupIDs();

	/**
	 * Change the id of the given signal group
	 * @param signalGroup the original signal group ID
	 * @param newID the new ID
	 */
	void updateSignalGroupID(UUID oldID, UUID newID);

	/**
	 * Get the group number of a signal group in the collection.
	 * @param signalGroup
	 * @return the group number, or zero if not present
	 */
	int getSignalGroupNumber(UUID signalGroup);

	/**
	 * Add a single signal to the given signal group
	 * @param n the signal
	 * @param signalGroup the signal group
	 */
	void addSignal(INuclearSignal n, UUID signalGroup);

	/**
	 * Append a list of signals to the given signal group
	 * @param list the signals
	 * @param signalGroup the signal group
	 */
	void addSignals(List<INuclearSignal> list, UUID signalGroup);

	/**
	 * Get all the signals in all signal groups, as a list of lists.
	 * Fetches the actual signals, not a copy
	 * @return the list of signal lists
	 */
	List<List<INuclearSignal>> getSignals();

	/**
	 * Get the signals in the given group. Fetches the actual signals, 
	 * not a copy
	 * @param signalGroup the signal group
	 * @return a list of signals
	 */
	List<INuclearSignal> getSignals(UUID signalGroup);
	
	/**
	 * Get all the signals in the nucleus. Fetches the actual signals, 
	 * not a copy. Unlike {@link ISignalCollection#getSignals()}, this
	 * combines all signals into a single list.
	 * @return a list of signals
	 */
	List<INuclearSignal> getAllSignals();

	/**
	 * Get the file containing the signals in the given signal group
	 * @param signalGroup the group id
	 * @return the File with the signals
	 */
	File getSourceFile(UUID signalGroup);

	/**
	 * Update the source file for the given signal group
	 * @param signalGroup the signal group id
	 * @param f the new source file
	 */
	void updateSourceFile(UUID signalGroup, File f);

	/**
	 * Get the channel containing the signals in the given signal group
	 * @param signalGroup the group id
	 * @return the RGB channel with the signals (0 if greyscale)
	 */
	int getSourceChannel(UUID signalGroup);


	/**
	 * Get the number of signal groups
	 * @return the number of signal groups
	 */
	int size();

	/**
	 * Get the total number of signals in all groups
	 * @return the total signal count of the nucleus
	 */
	int numberOfSignals();

	/**
	 * Check if the signal group contains signals in this collection
	 * @param signalGroup the group id
	 * @return yes or no
	 */
	boolean hasSignal(UUID signalGroup);

	/**
	 * Check if the signal group contains signals in this collection
	 * @param signalGroup the group id
	 * @return yes or no
	 */
	boolean hasSignal();

	/**
	 * Get the total number of signals in a given group
	 * @param signalGroup the group id
	 * @return the number of signals in the given group
	 */
	int numberOfSignals(UUID signalGroup);

	/**
	 * Remove all signals from the collection
	 */
	void removeSignals();

	/**
	 * Remove the given signal group from the collection
	 * @param signalGroup the signal group ID
	 */
	void removeSignals(UUID signalGroup);

	/**
	 * Get the statistics of signals in a group
	 * @param stat the statistic to fetch
	 * @param scale the scale to fetch values at
	 * @param signalGroup the signal group ID
	 * @return the values from each signal in the group
	 */
	List<Double> getStatistics(SignalStatistic stat, MeasurementScale scale, UUID signalGroup);

	/**
	 * Get the ImageJ image processor for the source image for signals in the given group 
	 * @param signalGroup the signal group ID
	 * @return an image processor
	 * @throws UnloadableImageException if the image cannot be loaded from file
	 */
	ImageProcessor getImage(UUID signalGroup) throws UnloadableImageException;
	
	/**
	 * Calculate the pairwise distances between all signals in the nucleus 
	 */
	double[][] calculateDistanceMatrix(MeasurementScale scale);
	
	/**
     * For each signal group pair, find the smallest pairwise distance
     * between signals in the collection.
     * @return a list of shortest distances for each pairwise group
     */
	List<PairwiseSignalDistanceValue> calculateSignalColocalisation(MeasurementScale scale);
}