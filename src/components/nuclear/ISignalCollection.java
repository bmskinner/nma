package components.nuclear;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import components.generic.MeasurementScale;
import ij.process.ImageProcessor;
import logging.Loggable;
import stats.SignalStatistic;

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
	 * @param signalGroup
	 * @param newID
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
	 * Get the file containing the signals in the given signal group
	 * @param signalGroup the group id
	 * @return the File with the signals
	 */
	File getSourceFile(UUID signalGroup);

	/**
	 * Update the source file for the given signal group
	 * @param signalGroup
	 * @param f
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
	 * @return the count
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
	 * Get the total number of signals in a given channel
	 * @param channel the channel
	 * @return the count
	 */
	int numberOfSignals(UUID signalGroup);

	/**
	 * Remove all signals from the collection
	 */
	void removeSignals();

	/**
	 * Remove the given signal group from the collection
	 */
	void removeSignals(UUID signalGroup);

	/**
	 * Get the areas of signals in a channel
	 * @param channel the signal channel
	 * @return the areas
	 * @throws Exception 
	 */
	List<Double> getStatistics(SignalStatistic stat, MeasurementScale scale, UUID signalGroup);

	ImageProcessor getImage(UUID signalGroup);

	String toString();

}