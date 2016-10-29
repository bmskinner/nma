package analysis;

import java.io.File;
import java.io.Serializable;
import java.util.Set;
import java.util.UUID;

import analysis.signals.NuclearSignalOptions;
import components.nuclear.NucleusType;
import components.nuclei.Nucleus;
import logging.Loggable;

public interface IAnalysisOptions extends Serializable, Loggable {

	File getFolder();

	File getMappingFile();

	int getNucleusThreshold();

	double getMinNucleusSize();

	double getMaxNucleusSize();

	double getMinNucleusCirc();

	double getMaxNucleusCirc();

	double getAngleWindowProportion();

	NucleusType getNucleusType();

	String getRefoldMode();

	boolean isReanalysis();

	boolean realignImages();

	boolean refoldNucleus();

	int getXOffset();

	int getYOffset();

	double getScale();

	int getChannel();

	void setChannel(int channel);

	void setScale(double scale);

	void setNucleusThreshold(int nucleusThreshold);

	void setMinNucleusSize(double minNucleusSize);

	void setMaxNucleusSize(double maxNucleusSize);

	void setMinNucleusCirc(double minNucleusCirc);

	void setMaxNucleusCirc(double maxNucleusCirc);

	void setAngleWindowProportion(double proportion);

	void setNucleusType(NucleusType nucleusType);

	void setPerformReanalysis(boolean performReanalysis);

	void setRealignMode(boolean realignMode);

	void setRefoldNucleus(boolean refoldNucleus);

	void setFolder(File folder);

	void setMappingFile(File mappingFile);

	void setRefoldMode(String refoldMode);

	void setXoffset(int xoffset);

	void setYoffset(int yoffset);

	boolean isNormaliseContrast();

	void setNormaliseContrast(boolean normaliseContrast);

	/**
	 * Get the canny options associated with the
	 * given type, or null if not present
	 * @param type the name to check
	 * @return canny detection options
	 */
	ICannyOptions getCannyOptions(String type);
	
	void addCannyOptions(String key, ICannyOptions options);

	void addCannyOptions(String type);

	Set<String> getCannyOptionTypes();

	/**
	 * Check if the given type name is already present
	 * @param type the name to check
	 * @return present or not
	 */
	boolean hasCannyOptions(String type);

	Set<UUID> getNuclearSignalGroups();

	/**
	 * Get the nuclear signal options associated with the
	 * given signal group id. If not present, the group is created
	 * @param type the name to check
	 * @return nuclear detection options
	 */
	NuclearSignalOptions getNuclearSignalOptions(UUID signalGroup);

	void addNuclearSignalOptions(UUID id);

	void addNuclearSignalOptions(UUID id, NuclearSignalOptions options);

	/**
	 * Check if the given type name is already present
	 * @param type the name to check
	 * @return present or not
	 */
	boolean hasSignalDetectionOptions(UUID signalGroup);

	boolean isKeepFailedCollections();

	void setKeepFailedCollections(boolean keepFailedCollections);

	boolean isValid(Nucleus c);

	int hashCode();

	boolean equals(Object obj);

}