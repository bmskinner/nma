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
package com.bmskinner.nuclear_morphology.components.nuclei;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

import com.bmskinner.nuclear_morphology.components.Orientable;
import com.bmskinner.nuclear_morphology.components.Taggable;
import com.bmskinner.nuclear_morphology.components.cells.CellularComponent;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.rules.OrientationMark;
import com.bmskinner.nuclear_morphology.components.signals.ISignalCollection;

/**
 * A Nucleus is the interface to all the possible types of nuclei that will be
 * used.
 * 
 * @author bms41
 *
 */
public interface Nucleus extends CellularComponent, Taggable, Orientable, Comparable<Nucleus> {

    // for debugging - use in calling dumpInfo()
    static final int ALL_POINTS    = 0;
    static final int BORDER_POINTS = 1;
    static final int BORDER_TAGS   = 2;

    /**
     * @return a copy of the data in this nucleus
     */
    @Override
	Nucleus duplicate();

    /**
     * Get a representation of the nucleus name as the name of the image plus
     * the number of the nucleus. For /P12.tiff nucleus 3 : P12.tiff-3
     * 
     * @return
     */
    String getNameAndNumber();

    /**
     * Get the number of the nucleus in the image
     * 
     * @return
     */
    int getNucleusNumber();

    /**
     * Get the path to the nucleus image, and the nucleus number within the image
     * @return
     */
    String getPathAndNumber();

    /**
     * Calculate the angle signal centres of mass make with the nucleus centre
     * of mass and the given border point
     * 
     * @param p the border point to orient from (the zero angle)
     * @throws Exception
     */
    void calculateSignalAnglesFromPoint(@NonNull IPoint p);

    /**
     * Get the signals in this nucleus
     * 
     * @return
     */
    ISignalCollection getSignalCollection();
    
    /**
     * Fetch the oriented copy of the nucleus. This will use 
     * the landmarks and priorities defined by the rulesets
     * used when creating the nucleus.
     * @return an oriented copy of the nucleus
     */
    Nucleus getOrientedNucleus();

    /**
     * Thrown when a nucleus type in a collection is incorrect for a requested
     * analysis
     * 
     * @author ben
     * @since 1.13.5
     */
    class IncorrectNucleusTypeException extends Exception {
        private static final long serialVersionUID = 1L;

        public IncorrectNucleusTypeException() {
            super();
        }

        public IncorrectNucleusTypeException(String message) {
            super(message);
        }

        public IncorrectNucleusTypeException(String message, Throwable cause) {
            super(message, cause);
        }

        public IncorrectNucleusTypeException(Throwable cause) {
            super(cause);
        }
    }

}
