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
package com.bmskinner.nuclear_morphology.components.datasets;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.Filterable;
import com.bmskinner.nuclear_morphology.components.MissingLandmarkException;
import com.bmskinner.nuclear_morphology.components.Refoldable;
import com.bmskinner.nuclear_morphology.components.StatisticalCollection;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.cells.ICell;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileCollection;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.profiles.MissingProfileException;
import com.bmskinner.nuclear_morphology.components.profiles.ProfileManager;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.components.signals.ISignalGroup;
import com.bmskinner.nuclear_morphology.components.signals.SignalManager;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;

/**
 * This interface will provides the primary access to cell data, as well as the
 * aggregate statistics and profile collection.
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public interface ICellCollection
        extends XmlSerializable, Filterable, 
        StatisticalCollection, Refoldable, Collection<ICell> {

	
	/** The length to interpolate profiles for comparisons between objects */
	int FIXED_PROFILE_LENGTH = 500;
	
	 /**
     * Create a copy of the collection
     * 
     */
	ICellCollection duplicate();
	
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
    UUID getId();

    /**
     * Get the cells in the collection
     * 
     * @return
     */
    List<ICell> getCells();
    
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
     * Replace the cell with the same ID as the given cell with the new copy.
     * If no cell with the given ID is present, no action is taken.
     * 
     * @param c the replacement cell
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
     * Get the nucleus with the given UUID. Convenience method.
     * 
     * @param id the nucleus id
     * @return the nucleus, if present, or an empty optional
     */
    Optional<Nucleus> getNucleus(@NonNull UUID id);

    /**
     * Get the type of nucleus this collection should contain
     * 
     * @return
     */
//    NucleusType getNucleusType();

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
    @Override
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
     * Test if the given nucleus is present in the collection (ID comparison test).
     * 
     * @param nucleus
     * @return
     */
    boolean contains(Nucleus nucleus);

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
     * Generate the profile aggregates from all cells in the 
     * population based on the currently set reference point in each
     * cell nucleus. The aggregate length will be set to the median
     * nucleus border length of the population.
     * @throws ProfileException if creation fails
     * @throws MissingProfileException 
     * @throws MissingLandmarkException 
     */
    void createProfileCollection() throws ProfileException, MissingLandmarkException, MissingProfileException;
    
    /**
     * Generate the profile aggregates from all cells in the 
     * population based on the currently set reference point in each
     * cell nucleus. The aggregate length will be set to the given
     * value
     * @throws ProfileException if creation fails
     * @throws MissingProfileException 
     * @throws MissingLandmarkException 
     */
    void createProfileCollection(int length) throws ProfileException, MissingLandmarkException, MissingProfileException;

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
    void addSignalGroup(@NonNull ISignalGroup newGroup);

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
     * @throws MissingLandmarkException
     * @throws MissingProfileException
     */
    Nucleus getNucleusMostSimilarToMedian(Landmark referencePoint)
            throws ProfileException, MissingLandmarkException, MissingProfileException;

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
     * @param d2 the other collection
     * @param i the number of shared nuclei
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
     * Update the image scale for all cells in the collection.
     * Note that this should be invoked by an analysis dataset only,
     * since it will otherwise unsync the options from the cells
     * @param scale the new scale in pixels/micron
     */
    @Deprecated
//    void setScale(double scale);

    /**
     * Get the perimeter normalised veriabililty of a nucleus angle profile
     * compared to the median profile of the collection
     * 
     * @param pointType the tag to use as index 0
     * @param t the taggable object to test
     * @return the variabililty score of the object
     * @throws MissingLandmarkException if the tag is not present
     */
    double getNormalisedDifferenceToMedian(Landmark pointType, Taggable t) throws MissingLandmarkException;

}
