package analysis;

import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

import analysis.signals.INuclearSignalOptions;
import components.nuclear.NucleusType;
import logging.Loggable;

/**
 * This stores details of an analysis setup for an IAnalysisDataset.
 * @author bms41
 * @since 1.13.3
 *
 */
public interface IAnalysisOptions extends Serializable, Loggable {
	
	// Standard detection keys
	static final String NUCLEUS     = "Nucleus";
	static final String SPERM_TAIL  = "SpermTail";

	
	/**
	 * Get the detection options for the given component
	 * @param key the component to detect
	 * @return the detection options for the component
	 */
	IMutableDetectionOptions getDetectionOptions(String key);
	
	/**
	 * Get the type of detection options stored
	 * @return
	 */
	Set<String> getDetectionOptionTypes();
	
	/**
	 * Check if the given type name is already present
	 * @param type the name to check
	 * @return present or not
	 */
	boolean hasDetectionOptions(String type);
	
	/**
	 * Get the proportion of the nucleus perimeter to use for shape profiling 
	 * @return the profile proportion
	 */
	double getProfileWindowProportion();

	/**
	 * Get the type of nucleus being analysed
	 * @return the type of nucleus
	 */
	NucleusType getNucleusType();

	/**
	 * Should the consensus nucleus be automatically refolded?
	 * @return the refold option: true to refold, false to not refold
	 */
	boolean refoldNucleus();


	Set<UUID> getNuclearSignalGroups();

	/**
	 * Get the nuclear signal options associated with the
	 * given signal group id. If not present, the group is created
	 * @param type the name to check
	 * @return nuclear detection options
	 */
	INuclearSignalOptions getNuclearSignalOptions(UUID signalGroup);


	/**
	 * Check if the given type name is already present
	 * @param type the name to check
	 * @return present or not
	 */
	boolean hasSignalDetectionOptions(UUID signalGroup);

	boolean isKeepFailedCollections();



}