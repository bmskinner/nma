/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


package com.bmskinner.nuclear_morphology.components;

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.analysis.signals.SignalManager;
import com.bmskinner.nuclear_morphology.components.generic.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.generic.ProfileManager;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.components.nuclear.UnavailableSignalGroupException;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This interface will provides the primary access to cell data, as well as the
 * aggregate statistics and profile collection.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public interface ICellCollection
        extends Serializable, Loggable, Filterable, StatisticalCollection, Refoldable<Nucleus> {

    /**
     * Set the name of the collection
     * 
     * @param s
     */
    void setName(@NonNull String s);

    /**
     * Check if the collection has real cells, or is a virtual collection with
     * pointers to real cells in another collection
     * 
     * @return true if the collection is virtual
     */
    boolean isVirtual();

    /**
     * Check if the collection has real cells, or is a virtual collection with
     * pointers to real cells in another collection
     * 
     * @return true if the collection is real
     */
    boolean isReal();

    /**
     * Get the name of the collection
     * 
     * @return
     */
    String getName();

    /**
     * Get the ID of the collection
     * 
     * @return
     */
    UUID getID();

    /**
     * Get the cells in the collection
     * 
     * @return
     */
    Set<ICell> getCells();
    
    /**
     * Stream the cells in the collection
     * 
     * @return
     */
    Stream<ICell> streamCells();

    /**
     * Get the cells in the collection within the given file
     * 
     * @return
     */
    Set<ICell> getCells(@NonNull File f);

    /**
     * Test if the collection contains cells from the given source file
     * 
     * @param imageFile
     * @return
     */
    boolean hasCells(@NonNull File imageFile);

    /**
     * Get the UUIDs of all the cells in the collection
     * 
     * @return
     */
    Set<UUID> getCellIDs();

    /**
     * Get the nuclei in this collection.
     * 
     * @return the nuclei, or an empty collection if no nuclei are present
     */
    Set<Nucleus> getNuclei();

    /**
     * Get the nuclei within the specified image
     * 
     * @param image
     *            the file to search
     * @return the nuclei, or an empty collection if no nuclei are present
     */
    Set<Nucleus> getNuclei(@NonNull File imageFile);

    /**
     * Test if the collection contains nuclei from the given source file
     * 
     * @param imageFile
     * @return
     */
    boolean hasNuclei(@NonNull File imageFile);

    /**
     * Add the given cell to the collection
     * 
     * @param r
     */
    void addCell(@NonNull ICell c);

    /**
     * Replace the cell with the same ID as the given cell with the new copy
     * 
     * @param c
     *            the replacement cell
     */
    void replaceCell(@NonNull ICell c);

    /**
     * Get the cell with the given UUID
     * 
     * @param id
     * @return
     */
    ICell getCell(@NonNull UUID id);

    /**
     * Get the type of nucleus this collection should contain
     * 
     * @return
     */
    NucleusType getNucleusType();

    /**
     * Remove the given cell from the collection. If the cell is null, has no
     * effect. If the cell is not in the collection, has no effect.
     * 
     * @param c
     *            the cell to remove
     */
    void removeCell(@NonNull ICell c);

    /**
     * Get the number of cells in the collection
     * 
     * @return
     */
    int size();

    /**
     * Get the number of nuclei in the collection
     * 
     * @return
     */
    int getNucleusCount();

    /**
     * Check if the collection contains cells
     * 
     * @return
     */
    public boolean hasCells();

    /**
     * Test if the given cell is present in the collection (ID comparison test).
     * 
     * @param cell
     * @return
     */
    boolean contains(ICell cell);

    /**
     * Test if a cell with the given id is present in the collection
     * 
     * @param cellID the id of the cell
     * @return
     */
    boolean contains(UUID cellID);

    /**
     * Test if the given cell object is present (== test)
     * 
     * @param cell
     * @return
     */
    boolean containsExact(@NonNull ICell cell);

    /**
     * Check if the collection contains cells locked from editing
     * 
     * @return
     */
    boolean hasLockedCells();

    /**
     * Set a lock on all cells for editing
     * 
     * @param b
     */
    void setCellsLocked(boolean b);

    /**
     * Get the profile collection of the given type
     * 
     * @param type
     * @return
     */
    IProfileCollection getProfileCollection();

    /**
     * Generate the profile collection and aggregates based on the profile
     * length of the population.
     * @throws ProfileException 
     */
    void createProfileCollection() throws ProfileException;

    /**
     * Get the folder the nuclei in the collection were imaged from
     * 
     * @return
     */
    File getFolder();

    /**
     * Get the name of the analysis output folder
     * 
     * @return
     */
    String getOutputFolderName();

    /**
     * Get the output folder (e.g. to save the dataset into). If an output
     * folder name (such as a date) has been input, it will be included
     * 
     * @return the folder
     */
    File getOutputFolder();

    /**
     * Set the output folder of the collection
     */
    void setOutputFolder(@NonNull File folder);

    /**
     * Get the distinct source image file list for all nuclei in the collection
     * 
     * @return
     */
    Set<File> getImageFiles();

    /*
     * METHODS FOR HANDLING SIGNALS
     */

    /**
     * Get the IDs of the signal groups in the collection
     * 
     * @return
     */
    Set<UUID> getSignalGroupIDs();

    /**
     * Remove the signal group with the given ID if present
     * 
     * @param id
     */
    void removeSignalGroup(@NonNull UUID id);

    /**
     * Get the signal group with the given ID, if present
     * 
     * @param signalGroup
     * @return the signal group, or null if not present
     */
    Optional<ISignalGroup> getSignalGroup(@NonNull UUID signalGroup);

    /**
     * Test if the collection has a signal group with the given ID
     * 
     * @param signalGroup
     * @return
     */
    boolean hasSignalGroup(@NonNull UUID signalGroup);

    /**
     * Get the signal groups in this collection
     * 
     * @return
     */
    Collection<ISignalGroup> getSignalGroups();

    /**
     * Add the given signal group to the collection
     * 
     * @param newID
     * @param newGroup
     */
    void addSignalGroup(@NonNull UUID newID, @NonNull ISignalGroup newGroup);

    /**
     * Get the signal manager for the collection
     * 
     * @return
     */
    SignalManager getSignalManager();

    /*
     * METHODS FOR HANDLING RULES
     */

    /**
     * Get the rulesets for this collection
     * 
     * @return
     */
    RuleSetCollection getRuleSetCollection();

    /**
     * Force a recalculation of vertically oriented nuclei
     */
    void updateVerticalNuclei();

    /**
     * Update the source image folder to the given directory for each cell
     * 
     * @param expectedImageDirectory
     */
    void setSourceFolder(@NonNull File expectedImageDirectory);

    /**
     * Get the nucleus in the collection most similar to the median profile
     * 
     * @param referencePoint
     *            the tag to zero the profile against
     * @return
     * @throws ProfileException
     * @throws UnavailableBorderTagException
     * @throws UnavailableProfileTypeException
     */
    Nucleus getNucleusMostSimilarToMedian(Tag referencePoint)
            throws ProfileException, UnavailableBorderTagException, UnavailableProfileTypeException;

    /**
     * Get the profile manager for the collection
     * 
     * @return
     */
    ProfileManager getProfileManager();

    /**
     * Count the number of cells shared between this collection and another
     * dataset.
     * 
     * @param d2
     * @return
     */
    int countShared(@NonNull IAnalysisDataset d2);

    /**
     * Count the number of cells shared between this collection and another
     * collection.
     * 
     * @param d2
     * @return
     */
    int countShared(@NonNull ICellCollection d2);

    /**
     * Set the number of cells in the collection that are shared with another
     * collection. This can be used to reduce calculation times
     * 
     * @param d2
     *            the other collection
     * @param i
     *            the number of shared nuclei
     */
    void setSharedCount(@NonNull ICellCollection d2, int i);

    /*
     * METHODS FOR GETTING COLLECTION STATISTICS
     */

    /**
     * Get the median array size of the collection, for producing profile
     * aggregates
     * 
     * @return
     */
    int getMedianArrayLength();

    /**
     * Get the length of the longest profile in the collection
     * 
     * @return
     */
    int getMaxProfileLength();

    /**
     * Update the image scale for all cells in the collection
     * @param scale
     */
    void setScale(double scale);

    /**
     * Get the perimeter normalised veriabililty of a nucleus angle profile
     * compared to the median profile of the collection
     * 
     * @param pointType
     *            the tag to use as index 0
     * @param c
     *            the cell to test
     * @return the variabililty score of the nucleus
     * @throws UnavailableBorderTagException
     */
    double getNormalisedDifferenceToMedian(Tag pointType, Taggable t) throws UnavailableBorderTagException;

}
