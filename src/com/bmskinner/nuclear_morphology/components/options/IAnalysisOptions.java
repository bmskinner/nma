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


package com.bmskinner.nuclear_morphology.components.options;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.gui.GlobalOptions;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * This stores details of an analysis setup for an IAnalysisDataset.
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public interface IAnalysisOptions extends Serializable, Loggable {

    // Standard detection keys
    static final String NUCLEUS    = "Nucleus";
    static final String CYTOPLASM  = "Cytoplasm";
    static final String SPERM_TAIL = "SpermTail";

    static final boolean     DEFAULT_REFOLD            = true;
    static final boolean     DEFAULT_KEEP_FAILED       = false;
    static final double      DEFAULT_WINDOW_PROPORTION = 0.05;
    static final NucleusType DEFAULT_TYPE              = GlobalOptions.getInstance().getDefaultType();

    /**
     * Get the detection options for the given component
     * 
     * @param key
     *            the component to detect
     * @return the detection options for the component
     */
    IMutableDetectionOptions getDetectionOptions(String key) throws MissingOptionException;

    /**
     * Get the type of detection options stored
     * 
     * @return
     */
    Set<String> getDetectionOptionTypes();

    /**
     * Check if the given type name is already present
     * 
     * @param type
     *            the name to check
     * @return present or not
     */
    boolean hasDetectionOptions(String type);

    /**
     * Get the proportion of the nucleus perimeter to use for shape profiling
     * 
     * @return the profile proportion
     */
    double getProfileWindowProportion();

    /**
     * Get the type of nucleus being analysed
     * 
     * @return the type of nucleus
     */
    NucleusType getNucleusType();

    /**
     * Should the consensus nucleus be automatically refolded?
     * 
     * @return the refold option: true to refold, false to not refold
     */
    boolean refoldNucleus();

    Set<UUID> getNuclearSignalGroups();

    /**
     * Get the nuclear signal options associated with the given signal group id.
     * If not present, the group is created
     * 
     * @param type
     *            the name to check
     * @return nuclear detection options
     */
    INuclearSignalOptions getNuclearSignalOptions(UUID signalGroup);

    /**
     * Check if the given type name is already present
     * 
     * @param type
     *            the name to check
     * @return present or not
     */
    boolean hasSignalDetectionOptions(UUID signalGroup);

    /**
     * Check if nuclei that do not meet the detection parameters should be kept
     * in a separate collection
     * 
     * @return
     */
    boolean isKeepFailedCollections();

}
