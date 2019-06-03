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
package com.bmskinner.nuclear_morphology.components.options;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.nuclear.NucleusType;
import com.bmskinner.nuclear_morphology.core.GlobalOptions;

/**
 * This stores details of an analysis setup for an IAnalysisDataset.
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public interface IAnalysisOptions extends Serializable {

    // Standard detection keys
    String NUCLEUS        = "Nucleus";
    String CYTOPLASM      = "Cytoplasm";
    String SPERM_TAIL     = "SpermTail";
    String SIGNAL_GROUP   = "SignalGroup_";
    String NUCLEAR_SIGNAL = "NuclearSignal";
    
    // Standard secondary options
    String TSNE = "t-SNE";

    boolean     DEFAULT_REFOLD            = true;
    boolean     DEFAULT_KEEP_FAILED       = false;
    double      DEFAULT_WINDOW_PROPORTION = 0.05;
    NucleusType DEFAULT_TYPE              = GlobalOptions.getInstance().getDefaultType();

    IAnalysisOptions duplicate();
    
    /**
     * Get the detection options for the given component
     * 
     * @param key the component to detect
     * @return the detection options for the component
     */
    Optional<IDetectionOptions> getDetectionOptions(String key);

    /**
     * Get the type of detection options stored
     * 
     * @return
     */
    Set<String> getDetectionOptionTypes();

    /**
     * Check if the given type name is already present
     * 
     * @param type the name to check
     * @return true if present, false otherwise
     */
    boolean hasDetectionOptions(String type);
    
    /**
     * Fetch the options used to detect the nucleus, if present.
     * This is a shortcut for {@code IAnalysisOptions::getDetectionOptions(CellularComponent.NUCLEUS)}
     * @return
     */
    Optional<IDetectionOptions> getNuclusDetectionOptions();
    
    /**
     * Get secondary options for a given key
     * 
     * @param key the type of options
     * @return the  options
     */
    Optional<HashOptions> getSecondaryOptions(String key);
    
    /**
     * Get the types of secondary options stored
     * 
     * @return
     */
    Set<String> getSecondaryOptionKeys();

    /**
     * Check if the given type name is already present
     * 
     * @param key the key to check
     * @return true if present, false otherwise
     */
    boolean hasSecondaryOptions(String key);

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
    @Deprecated
    boolean refoldNucleus();

    Set<UUID> getNuclearSignalGroups();

    /**
     * Get the nuclear signal options associated with the given signal group id.
     * 
     * @param signalGroup the group id
     * @return nuclear detection options for the group
     */
    INuclearSignalOptions getNuclearSignalOptions(@NonNull UUID signalGroup);
    

    /**
     * Check if the given type name is already present
     * 
     * @param type the name to check
     * @return present or not
     */
    boolean hasSignalDetectionOptions(@NonNull UUID signalGroup);

    /**
     * Check if nuclei that do not meet the detection parameters should be kept
     * in a separate collection
     * 
     * @return
     */
    @Deprecated
    boolean isKeepFailedCollections();
    
    /**
     * Get the time the analysis was conducted
     * @return the UNIX time
     */
    long getAnalysisTime();
    
    /**
     * Set the detection options for the given component
     * 
     * @param key
     * @param options
     */
    void setDetectionOptions(String key, IDetectionOptions options);
    
    /**
     * Set the secondary options for the given key
     * 
     * @param key the options key
     * @param options the options
     */
    void setSecondaryOptions(String key, HashOptions options);

    /**
     * Set the proportion of the perimeter to use when profiling nuclei
     * 
     * @param proportion
     */
    void setAngleWindowProportion(double proportion);

    /**
     * Set the type of nucleus / cell being analysed
     * 
     * @param nucleusType
     */
    void setNucleusType(NucleusType nucleusType);

    /**
     * Set whether the consensus nucleus should be refolded during the analysis
     * 
     * @param refoldNucleus
     */
    @Deprecated
    void setRefoldNucleus(boolean refoldNucleus);

    /**
     * Set whether nuclei that cannot be detected should be retained as a
     * separate collection
     * 
     * @param keepFailedCollections
     */
    @Deprecated
    void setKeepFailedCollections(boolean keepFailedCollections);
    
    /**
     * Set the values in this options to match the given options
     * @param o
     */
    void set(@NonNull IAnalysisOptions o);

}
