package com.bmskinner.nuclear_morphology.components;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileManager;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalManager;
import com.bmskinner.nuclear_morphology.components.generic.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.generic.MeasurementScale;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This interface will provides the primary access to cell data,
 * as well as the aggregate statistics and profile collection.
 * @author ben
 * @since 1.13.3
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
	 * Check if the collection has real cells, or is a virtual collection
	 * with pointers to real cells in another collection
	 * @return true if the collection is virtual
	 */
	boolean isVirtual();
	
	/**
	 * Check if the collection has real cells, or is a virtual collection
	 * with pointers to real cells in another collection
	 * @return true if the collection is real
	 */
	boolean isReal();

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
	void setConsensusNucleus(Nucleus n);
	
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
	
	boolean contains(UUID cellID);
	
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
	IProfileCollection getProfileCollection();
	
	/**
	 * Generate the profile collection and aggregates
	 * based on the profile length of the population.
	 */
	void createProfileCollection();

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
	ISignalGroup getSignalGroup(UUID signalGroup) throws UnavailableSignalGroupException;

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
	 * @throws UnavailableBorderTagException 
	 * @throws UnavailableProfileTypeException 
	 */
	Nucleus getNucleusMostSimilarToMedian(Tag referencePoint) throws ProfileException, UnavailableBorderTagException, UnavailableProfileTypeException;

	/**
	 * Get the profile manager for the collection
	 * @return
	 */
	ProfileManager getProfileManager();

	
	/*
	 * METHODS FOR FILTERING AND DIVIDING THE COLLECTION
	 */
	
	/**
	 * Return a collection of cells present in both collections
	 * @param other the other collection
	 * @return
	 */
	ICellCollection and(ICellCollection collection);
	
	/**
	 * Return a collection of cells present this collection but not the other
	 * @param other the other collection
	 * @return
	 */
	ICellCollection not(ICellCollection collection);
	
	/**
	 * Return a collection of cells present this collection or the other but not both
	 * @param other the other collection
	 * @return a new collection with cells not shared between datasets
	 */
	ICellCollection xor(ICellCollection collection);
	
	/**
	 * Return a collection containing cell in either dataset. 
	 * Cells in both datasets are not duplicated. 
	 * @param collection the comparison dataset
	 * @return a new collection with cells from either dataset
	 */
	ICellCollection or(ICellCollection collection);
	
	/**
	 * Filter the collection on the given statistic
	 * @param stat the stat to filter on
	 * @param scale the measurement scale of the bounds
	 * @param lower the lower bound for the stat
	 * @param upper the upper bound for the stat
	 * @return a new collection with only cells matching the filter
	 */
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
	
	/**
	 * Set the number of cells in the collection that are shared with
	 * another collection. This can be used to reduce calculation times
	 * @param d2 the other collection 
	 * @param i the number of shared nuclei
	 */
	void setSharedCount(ICellCollection d2, int i);

		
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
	
	double getMedianStatistic(PlottableStatistic stat, String component, MeasurementScale scale) throws Exception;

	double[] getMedianStatistics(PlottableStatistic stat, String component, MeasurementScale scale);
	
	double[] getMedianStatistics(PlottableStatistic stat, String component, MeasurementScale scale, UUID id);
	
	/**
	 * Get the perimeter normalised veriabililty of a nucleus angle profile compared to the
	 * median profile of the collection
	 * @param pointType the tag to use as index 0
	 * @param c the cell to test
	 * @return the variabililty score of the nucleus
	 * @throws UnavailableBorderTagException 
	 */
	double getNormalisedDifferenceToMedian(Tag pointType, ICell c) throws UnavailableBorderTagException;



}
