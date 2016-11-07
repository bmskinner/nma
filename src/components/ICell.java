/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
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
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package components;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import logging.Loggable;
import components.nuclei.Nucleus;

/**
 * All cell types implement this interface. A cell can have a nucleus,
 * and zero, one or many acrosomes, flagella and mitochondria.
 * @author bms41
 * @since 1.13.3
 *
 */
public interface ICell 
	extends Serializable, 
			Loggable,
			Comparable<ICell> {

	/**
	 * Get the ID of the cell
	 * @return
	 */
	UUID getId();

	/**
	 * Get the nucleus of the cell
	 * @return
	 */
	Nucleus getNucleus();

	/**
	 * Set the nucleus of the cell
	 * @param nucleus
	 */
	void setNucleus(Nucleus nucleus);

	/**
	 * Get the mitochondria of the cell
	 * @return
	 */
	List<IMitochondrion> getMitochondria();

	/**
	 * Set the mitochondria of the cell
	 * @param mitochondria
	 */
	void setMitochondria(List<IMitochondrion> mitochondria);

	/**
	 * Add a new mitochondrion to the cell
	 * @param mitochondrion
	 */
	void addMitochondrion(IMitochondrion mitochondrion);

	/**
	 * Get the flagella in the cell
	 * @return
	 */
	List<Flagellum> getFlagella();


	/**
	 * Add a flagellum to the cell
	 * @param tail
	 */
	void addFlagellum(Flagellum tail);

	/**
	 * Get the acrosomes for the cell
	 * @return
	 */
	List<IAcrosome> getAcrosomes();

	/**
	 * Add an acrosome to the cell
	 * @param acrosome
	 */
	void addAcrosome(IAcrosome acrosome);

	/**
	 * Test if the cell has a nucleus
	 * @return
	 */
	boolean hasNucleus();

	/**
	 * Test if the cell has a flagellum
	 * @return
	 */
	boolean hasFlagellum();

	/**
	 * Test if the cell has mitochondria
	 * @return
	 */
	boolean hasMitochondria();
	
	boolean hasAcrosome();

	/**
	 * Compare cells using the UUID only
	 * @param c
	 * @return
	 */
	boolean equals(Object o);

	int compareTo(ICell o);

	int hashCode();

}