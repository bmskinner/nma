package components;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import stats.NucleusStatistic;
import stats.PlottableStatistic;
import analysis.IAnalysisDataset;
import analysis.profiles.ProfileException;
import analysis.profiles.ProfileManager;
import analysis.profiles.RuleSetCollection;
import analysis.signals.SignalManager;
import logging.Loggable;
import components.generic.BorderTagObject;
import components.generic.IProfileCollection;
import components.generic.MeasurementScale;
import components.generic.ProfileCollection;
import components.generic.ProfileType;
import components.generic.Tag;
import components.nuclear.ISignalGroup;
import components.nuclear.NucleusType;
import components.nuclei.ConsensusNucleus;
import components.nuclei.Nucleus;

/**
 * This interface will eventually replace the CellCollection as the
 * primary access to cell data in analysis classes.
 * @author ben
 *
 */
public interface ICellCollection 
	extends Serializable, 
			Loggable{


	/**
	 * Set the name of the collection
	 * @param s
	 */
	void setName(String s);

	/**
	 * Get the name of the collection
	 * @return
	 */
	String getName();

	/**
	 * Get the ID of the collection
	 * @return
	 */
	UUID getID();

	/**
	 * Get the cells in the collection
	 * @return
	 */
	Set<ICell> getCells();
	
	/**
	 * Get the cells in the collection within the given file
	 * @return
	 */
	Set<ICell> getCells(File f);

	/**
	 * Get the UUIDs of all the cells in the collection
	 * @return
	 */
	Set<UUID> getCellIDs();

	/**
	 * Get the nuclei in this collection
	 * @return
	 */
	Set<Nucleus> getNuclei();

	/**
	 * Get the nuclei within the specified image
	 * @param image the file to search
	 * @return the list of nuclei
	 */
	Set<Nucleus> getNuclei(File imageFile);

	/**
	 * Add the given cell to the collection
	 * @param r
	 */
	void addCell(ICell c);

	/**
	 * Replace the cell with the same ID as the given cell with
	 * the new copy
	 * @param r
	 */
	void replaceCell(ICell c);

	/**
	 * Get the cell with the given UUID
	 * @param id
	 * @return
	 */
	ICell getCell(UUID id);


	/**
	 * Get the type of nucleus this collection should contain
	 * @return
	 */
	NucleusType getNucleusType();

	/**
	 * Remove the given cell from the collection. If the cell is
	 * null, has no effect. If the cell is not in the collection, has
	 * no effect. 
	 * @param c the cell to remove
	 */
	void removeCell(ICell c);

	/**
	 * Get the number of cells in the collection
	 * @return
	 */
	int size();


	/**
	 * Check if the collection has a consensus nucleus
	 * @return
	 */
	boolean hasConsensusNucleus();

	/**
	 * Set the consensus nucleus for the collection
	 * @param n
	 */
	void setConsensusNucleus(ConsensusNucleus n);
	
	/**
	 * Get the consensus nucleus if set
	 * @return the consensus, or null if not present
	 */
	Nucleus getConsensusNucleus();
	
	/**
	 * Set the refolding state
	 * @param b
	 */
	void setRefolding(boolean b);
	
	/**
	 * Test if the consensus is being refolded
	 * @return
	 */
	boolean isRefolding();

	/**
	 * Check if the collection contains cells
	 * @return
	 */
	public boolean hasCells();
	
	/**
	 * Test if the given cell is present in the collection
	 * (ID comparison test).
	 * @param cell
	 * @return
	 */
	boolean contains(ICell cell);
	
	/**
	 * Test if the given cell object is present
	 * (== test)
	 * @param cell
	 * @return
	 */
	boolean containsExact(ICell cell);

	/**
	 * Check if the collection contains cells locked from editing
	 * @return
	 */
	boolean hasLockedCells();

	/**
	 * Set a lock on all cells for editing
	 * @param b
	 */
	void setCellsLocked(boolean b);


	/**
	 * Get the profile collection of the given type
	 * @param type
	 * @return
	 */
	IProfileCollection getProfileCollection(ProfileType type);

	/**
	 * Set the profile collection for the given profile type
	 * @param type
	 * @param p
	 */
	void setProfileCollection(ProfileType type, IProfileCollection p);

	/**
	 * Remove the given profile collection
	 * @param type
	 */
	public void removeProfileCollection(ProfileType type);

	/**
	 * Get the folder the nuclei in the collection were imaged from
	 * @return
	 */
	File getFolder();

	/**
	 * Get the name of the analysis output folder
	 * @return
	 */
	String getOutputFolderName();


	/**
	 * Get the output folder (e.g. to save the dataset into).
	 * If an output folder name (such as a date) has been input, it will be included 
	 * @return the folder
	 */
	File getOutputFolder();

	/**
	 * Get the distinct source image file list for all nuclei in the collection 
	 * @return
	 */
	Set<File> getImageFiles();

	
	/*
	 * METHODS FOR HANDLING SIGNALS
	 */
	
	/**
	 * Get the IDs of the signal groups in the collection
	 * @return
	 */
	Set<UUID> getSignalGroupIDs();

	/**
	 * Remove the signal group with the given ID if present
	 * @param id
	 */
	void removeSignalGroup(UUID id);

	/**
	 * Get the signal group with the given ID, if present
	 * @param signalGroup
	 * @return the signal group, or null if not present
	 */
	ISignalGroup getSignalGroup(UUID signalGroup);

	/**
	 * Test if the collection has a signal group with the given ID
	 * @param signalGroup
	 * @return
	 */
	boolean hasSignalGroup(UUID signalGroup);

	/**
	 * Get the signal groups in this collection
	 * @return
	 */
	Collection<ISignalGroup> getSignalGroups();

	/**
	 * Add the given signal group to the collection
	 * @param newID
	 * @param newGroup
	 */
	void addSignalGroup(UUID newID, ISignalGroup newGroup);


	/**
	 * Get the signal manager for the collection
	 * @return
	 */
	SignalManager getSignalManager();
	
	
	/*
	 * METHODS FOR HANDLING RULES
	 */

	/**
	 * Get the rulesets for this collection
	 * @return
	 */
	RuleSetCollection getRuleSetCollection();

	/**
	 * Force a recalculation of vertically oriented nuclei
	 */
	void updateVerticalNuclei();

	/**
	 * Attempt to update the source image folder to the given directory
	 * @param expectedImageDirectory
	 * @return true on success
	 */
	boolean updateSourceFolder(File expectedImageDirectory);

	/**
	 * Get the nucleus in the collection most similar to the 
	 * median profile
	 * @param referencePoint the tag to zero the profile against
	 * @return
	 * @throws ProfileException 
	 */
	Nucleus getNucleusMostSimilarToMedian(Tag referencePoint) throws ProfileException;

	/**
	 * Get the profile manager for the collection
	 * @return
	 */
	ProfileManager getProfileManager();

	
	/*
	 * METHODS FOR FILTERING AND DIVIDING THE COLLECTION
	 */
	
	ICellCollection and(ICellCollection collection);
		
	ICellCollection not(ICellCollection collection);
	
	ICellCollection xor(ICellCollection collection);
	
	ICellCollection filterCollection(PlottableStatistic stat,
			MeasurementScale scale, double lower, double upper);
	
	
	/**
	 * Count the number of cells shared between this collection
	 * and another dataset.
	 * @param d2
	 * @return
	 */
	int countShared(IAnalysisDataset d2);
	
	/**
	 * Count the number of cells shared between this collection
	 * and another collection.
	 * @param d2
	 * @return
	 */
	int countShared(ICellCollection d2);

		
	/*
	 * METHODS FOR GETTING COLLECTION STATISTICS
	 */

	/**
	 * Get the median array size of the collection, for producing
	 * profile aggregates
	 * @return
	 */
	int getMedianArrayLength();
	
	/**
	 * Get the length of the longest profile in the collection
	 * @return
	 */
	int getMaxProfileLength();
	
	double getMedianPathLength();
	
	double getMedianStatistic(PlottableStatistic stat, MeasurementScale scale) throws Exception;

	double[] getMedianStatistics(PlottableStatistic stat, MeasurementScale scale);
	
	double[] getMedianStatistics(PlottableStatistic stat, MeasurementScale scale, UUID id);
	
	/**
	 * Get the perimeter normalised veriabililty of a nucleus angle profile compared to the
	 * median profile of the collection
	 * @param pointType the tag to use as index 0
	 * @param c the cell to test
	 * @return the variabililty score of the nucleus
	 */
	double getNormalisedDifferenceToMedian(Tag pointType, ICell c);



}
