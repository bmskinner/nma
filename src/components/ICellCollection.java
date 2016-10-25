package components;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import components.generic.ProfileCollection;
import components.generic.ProfileType;
import components.nuclear.NucleusType;
import components.nuclei.ConsensusNucleus;
import components.nuclei.Nucleus;

/**
 * This interface will eventually replace the CellCollection as the
 * primary access to cell data in analysis classes.
 * @author ben
 *
 */
public interface ICellCollection {


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
	Set<Cell> getCells();

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
	void addCell(Cell c);

	/**
	 * Replace the cell with the same ID as the given cell with
	 * the new copy
	 * @param r
	 */
	void replaceCell(Cell c);

	/**
	 * Get the cell with the given UUID
	 * @param id
	 * @return
	 */
	Cell getCell(UUID id);


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
	void removeCell(Cell c);

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
	 * Check if the collection contains cells
	 * @return
	 */
	public boolean hasCells();

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
	ProfileCollection getProfileCollection(ProfileType type);

	/**
	 * Set the profile collection for the given profile type
	 * @param type
	 * @param p
	 */
	void setProfileCollection(ProfileType type, ProfileCollection p);

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


}
