/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
package com.bmskinner.nuclear_morphology.components.nuclei;

import com.bmskinner.nuclear_morphology.analysis.profiles.Profileable;
import com.bmskinner.nuclear_morphology.analysis.profiles.Taggable;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.nuclear.IBorderPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.ISignalCollection;

/**
 * A Nucleus is the interface to all the possible types of nuclei that will be
 * used.
 * 
 * @author bms41
 *
 */
public interface Nucleus extends CellularComponent, Profileable, Taggable, Comparable<Nucleus> {
    // Note that we use Rotatable here although it is provided to objects
    // already through the AbstractCellularComponent
    // so that it can be accessed by the Nucleus interface itself.

    // for debugging - use in calling dumpInfo()
    public static final int ALL_POINTS    = 0;
    public static final int BORDER_POINTS = 1;
    public static final int BORDER_TAGS   = 2;

    /**
     * @return a copy of the data in this nucleus
     */
    public Nucleus duplicate();

    /**
     * Get a representation of the nucleus name as the name of the image plus
     * the number of the nucleus. For /P12.tiff nucleus 3 : P12.tiff-3
     * 
     * @return
     */
    public String getNameAndNumber();

    /**
     * Get the number of the nucleus in the image
     * 
     * @return
     */
    public int getNucleusNumber();

    public String getPathAndNumber();

    /**
     * Calculate the angle signal centres of mass make with the nucleus centre
     * of mass and the given border point
     * 
     * @param p
     *            the border point to orient from (the zero angle)
     * @throws Exception
     */
    public void calculateSignalAnglesFromPoint(IBorderPoint p);

    public String dumpInfo(int type);

    /**
     * Get the signals in this nucleus
     * 
     * @return
     */
    public ISignalCollection getSignalCollection();

    /**
     * Get the name of the folder to store analysis specific data. This is the
     * folder with the analysis date/time name.
     * 
     * @return
     */
    // public String getOutputFolderName();

    /**
     * Fetch the vertically oriented copy of the nucleus. Calculate if needed.
     * The vertical alignment with be by TOP_VERTICAL and BOTTOM_VERTICAL if
     * available, otherwise the ORIENTATION_POINT will be rotated below the CoM
     * 
     * @return
     */
    public Nucleus getVerticallyRotatedNucleus();

    /**
     * Invalidate the existing cached vertically rotated nucleus, and
     * recalculate.
     */
    public void updateVerticallyRotatedNucleus();

    /**
     * Is the reference point of the vertically rotated nucleus pointing to the
     * right?
     * 
     * @return
     */
    public boolean isClockwiseRP();

    /**
     * Thrown when a nucleus type in a collection is incorrect for a requested
     * analysis
     * 
     * @author ben
     * @since 1.13.5
     */
    public class IncorrectNucleusTypeException extends Exception {
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