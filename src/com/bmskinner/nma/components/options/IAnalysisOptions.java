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
package com.bmskinner.nma.components.options;

import java.io.File;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.rules.RuleSetCollection;
import com.bmskinner.nma.io.XmlSerializable;

/**
 * This stores details of an analysis setup for an IAnalysisDataset.
 * 
 * @author bms41
 * @since 1.13.3
 *
 */
public interface IAnalysisOptions extends XmlSerializable {

	// Standard detection keys are in CellularComponent
	String SIGNAL_GROUP = "SignalGroup_";

	// Standard secondary options
	String TSNE = "t-SNE";
	String UMAP = "UMAP";

	boolean DEFAULT_REFOLD = true;
	boolean DEFAULT_KEEP_FAILED = false;
	double DEFAULT_WINDOW_PROPORTION = 0.05;

	/**
	 * Duplicate this options object
	 * 
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
	 * Get the detection folder for the given component
	 * 
	 * @param key the component to detect
	 * @return the detection options for the component
	 */
	Optional<File> getDetectionFolder(@NonNull String key);

	/**
	 * Set the given detection folder
	 * 
	 * @param key    a value from CellularComponent, or a group id
	 * @param folder
	 */
	void setDetectionFolder(@NonNull String key, @NonNull File folder);

	/**
	 * Remove the given detection folder from this options
	 */
	void removeDetectionFolder(@NonNull String key);

	/**
	 * Fetch the options used to detect the nucleus, if present. This is a shortcut
	 * for {@code IAnalysisOptions::getDetectionOptions(CellularComponent.NUCLEUS)}
	 * 
	 * @return
	 */
	Optional<HashOptions> getNucleusDetectionOptions();

	/**
	 * Fetch the folder used to detect the nucleus, if present. This is a shortcut
	 * for {@code IAnalysisOptions::getDetectionFolder(CellularComponent.NUCLEUS)}
	 * 
	 * @return
	 */
	Optional<File> getNucleusDetectionFolder();

	/**
	 * Set the folder of images used to detect nuclei. This is a shortcut for
	 * {@code IAnalysisOptions::setDetectionFolder(CellularComponent.NUCLEUS, folder)}
	 * 
	 * @param folder the folder
	 */
	void setNucleusDetectionFolder(@NonNull File folder);

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
	boolean hasDetectionOptions(@NonNull String type);

	/**
	 * Get secondary options for a given key
	 * 
	 * @param key the type of options
	 * @return the options
	 */
	Optional<HashOptions> getSecondaryOptions(@NonNull String key);

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
	boolean hasSecondaryOptions(@NonNull String key);

	/**
	 * Get the proportion of the nucleus perimeter to use for shape profiling
	 * 
	 * @return the profile proportion
	 */
	double getProfileWindowProportion();

	/**
	 * Get the rulesets used to detect landmarks TODO - why do we store these in the
	 * ICellCollection also? Decide on a single canonical source for the rulesets
	 * 
	 * @return
	 */
	RuleSetCollection getRuleSetCollection();

	/**
	 * Set the rulesets used to detect landmarks
	 * 
	 * @param rsc
	 */
	void setRuleSetCollection(@NonNull RuleSetCollection rsc);

	/**
	 * Get the IDs of signal groups
	 * 
	 * @return
	 */
	Set<UUID> getNuclearSignalGroups();

	/**
	 * Get the nuclear signal options associated with the given signal group id.
	 * 
	 * @param signalGroup the group id
	 * @return nuclear detection options for the group
	 */
	Optional<HashOptions> getNuclearSignalOptions(@NonNull UUID signalGroup);

	/**
	 * Fetch the folder used to detect the nuclear signal, if present.
	 * 
	 * @return
	 */
	Optional<File> getNuclearSignalDetectionFolder(@NonNull UUID signalGroup);

	/**
	 * Set signal detection options
	 * 
	 * @param key
	 * @param options
	 */
	void setNuclearSignalDetectionOptions(@NonNull HashOptions options);

	/**
	 * Set the detection folder for a nuclear signal
	 * 
	 * @param id
	 * @param folder
	 */
	void setNuclearSignalDetectionFolder(@NonNull UUID id, @NonNull File folder);

	/**
	 * Check if the given type name is already present
	 * 
	 * @param type the name to check
	 * @return present or not
	 */
	boolean hasNuclearSignalDetectionOptions(@NonNull UUID signalGroup);

	/**
	 * Get the time the analysis was conducted
	 * 
	 * @return the UNIX time
	 */
	long getAnalysisTime();

	/**
	 * Remove the analysis time
	 */
	void clearAnalysisTime();

	/**
	 * Set the detection options for the given component
	 * 
	 * @param key
	 * @param options
	 */
	void setDetectionOptions(@NonNull String key, @NonNull HashOptions options);

	/**
	 * Set the secondary options for the given key
	 * 
	 * @param key     the options key
	 * @param options the options
	 */
	void setSecondaryOptions(@NonNull String key, @NonNull HashOptions options);

	/**
	 * Set the proportion of the perimeter to use when profiling nuclei
	 * 
	 * @param proportion
	 */
	void setAngleWindowProportion(double proportion);

	/**
	 * Set the values in this options to match the given options. Note that
	 * detection folders will not be added.
	 * 
	 * @param o
	 */
	void set(@NonNull IAnalysisOptions o);

}
