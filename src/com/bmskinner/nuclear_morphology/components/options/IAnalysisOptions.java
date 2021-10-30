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

import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;
import com.bmskinner.nuclear_morphology.io.XmlSerializable;

/**
 * This stores details of an analysis setup for an IAnalysisDataset.
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public interface IAnalysisOptions extends Serializable, XmlSerializable {

    // Standard detection keys are in CellularComponent
    String SIGNAL_GROUP   = "SignalGroup_";
    
    // Standard secondary options
    String TSNE = "t-SNE";

    boolean     DEFAULT_REFOLD            = true;
    boolean     DEFAULT_KEEP_FAILED       = false;
    double      DEFAULT_WINDOW_PROPORTION = 0.05;

    /**
     * Duplicate this options object
     * @return
     */
    IAnalysisOptions duplicate();
    
    /**
     * Get the detection options for the given component
     * 
     * @param key the component to detect
     * @return the detection options for the component
     */
    Optional<HashOptions> getDetectionOptions(String key);

    
    /**
     * Fetch the options used to detect the nucleus, if present.
     * This is a shortcut for {@code IAnalysisOptions::getDetectionOptions(CellularComponent.NUCLEUS)}
     * @return
     */
    Optional<HashOptions> getNucleusDetectionOptions();
    
    /**
     * Get the type of detection options stored
     * 
     * @return
     */
    Set<String> getDetectionOptionTypes();

    /**
     * Check if the given type name is present
     * 
     * @param type the name to check
     * @return true if present, false otherwise
     */
    boolean hasDetectionOptions(String type);

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
     * Get the rulesets used to detect landmarks
     * @return
     */
    RuleSetCollection getRuleSetCollection();
    
    /**
     * Set the rulesets used to detect landmarks
     * @param rsc
     */
    void setRuleSetCollection(RuleSetCollection rsc);
   

    Set<UUID> getNuclearSignalGroups();

    /**
     * Get the nuclear signal options associated with the given signal group id.
     * 
     * @param signalGroup the group id
     * @return nuclear detection options for the group
     */
    Optional<HashOptions> getNuclearSignalOptions(@NonNull UUID signalGroup);
    
    /**
     * Set signal detection options
     * 
     * @param key
     * @param options
     */
    void setNuclearSignalDetectionOptions(HashOptions options);
    

    /**
     * Check if the given type name is already present
     * 
     * @param type the name to check
     * @return present or not
     */
    boolean hasSignalDetectionOptions(@NonNull UUID signalGroup);

    
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
    void setDetectionOptions(String key, HashOptions options);
    
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
     * Set the values in this options to match the given options
     * @param o
     */
    void set(@NonNull IAnalysisOptions o);
}
