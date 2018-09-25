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

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * All cell types implement this interface. A cell can have multiple nuclei, and zero,
 * one or many acrosomes, flagella and mitochondria.
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public interface ICell extends Serializable, Loggable, Statistical, Comparable<ICell> {

	/**
	 * Create a copy of this cell
	 * @return
	 */
	ICell duplicate();
	
    /**
     * Get the ID of the cell
     * 
     * @return
     */
	@NonNull UUID getId();

    /**
     * Get the first nucleus of the cell. Use {@link #getNuclei()} instead to
     * ensure cells with multiple nuclei are handled correctly
     * Was deprecated from 1.13.5 to 1.14.0, but reenabled because it is a useful
     * shortcut 
     * 
     * @return
     * 
     */
    Nucleus getNucleus();

    /**
     * Get the nuclei of the cell
     * 
     * @return
     */
    List<Nucleus> getNuclei();

    /**
     * Set the nucleus of the cell
     */
    void setNucleus(Nucleus n);

    /**
     * Add a nucleus to the cell
     * 
     * @param n
     */
    void addNucleus(Nucleus n);

    /**
     * Get the mitochondria of the cell
     * 
     * @return
     */
    List<IMitochondrion> getMitochondria();

    /**
     * Set the mitochondria of the cell
     * 
     * @param mitochondria
     */
    void setMitochondria(List<IMitochondrion> mitochondria);

    /**
     * Get the flagella in the cell
     * 
     * @return
     */
    List<Flagellum> getFlagella();

    /**
     * Get the acrosomes for the cell
     * 
     * @return
     */
    List<IAcrosome> getAcrosomes();

    /**
     * Add an acrosome to the cell
     * 
     * @param acrosome
     */
    void addAcrosome(IAcrosome acrosome);

    /**
     * Get the cytoplasm of the cell
     * 
     * @return the cytoplasm, or null if not present
     */
    ICytoplasm getCytoplasm();
    
    
    /**
     * Get all the taggable components of the cell
     * @return
     */
    List<Taggable> getTaggables();

    /**
     * Test if the cell has a nucleus
     * 
     * @return
     */
    boolean hasNucleus();

    /**
     * Test if the cell has a flagellum
     * 
     * @return
     */
    boolean hasFlagellum();

    /**
     * Test if the cell has mitochondria
     * 
     * @return
     */
    boolean hasMitochondria();

    /**
     * Test if the cell has an acrosome
     * 
     * @return
     */
    boolean hasAcrosome();

    /**
     * Test if the cell has a cytoplasm
     * 
     * @return
     */
    boolean hasCytoplasm();

    int compareTo(ICell o);
    
    /**
     * Set the image scale for all components in the cell
     * @param scale
     */
    void setScale(double scale);
    
    /**
     * Test if any nuclei of the cell have signals
     * @return
     */
    boolean hasNuclearSignals();
    
    /**
     * Test if any nuclei of the cell have signals in the given group
     * @param signalGroupId the signal group to test
     * @return
     */
    boolean hasNuclearSignals(UUID signalGroupId);
    
    /**
     * Add a new mitochondrion to the cell
     * 
     * @param mitochondrion
     */
    void addMitochondrion(IMitochondrion mitochondrion);

    /**
     * Add a flagellum to the cell
     * 
     * @param tail
     */
    void addFlagellum(Flagellum tail);

    /**
     * Set the cytoplasm of the cell
     * 
     * @param cytoplasm
     *            the cytoplasm
     */
    void setCytoplasm(ICytoplasm cytoplasm);
    

}
