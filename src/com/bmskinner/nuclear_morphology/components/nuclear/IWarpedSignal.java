package com.bmskinner.nuclear_morphology.components.nuclear;

import java.io.Serializable;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.options.INuclearSignalOptions;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

/**
 * Interface for signals that have been derived from warping signal from 
 * one or more nuclei onto a consensus template.
 * @author bms41
 * @since 1.14.0
 *
 */
public interface IWarpedSignal extends Serializable, Loggable {
	
	/**
	 * Create a copy of this signal
	 * @return
	 */
	IWarpedSignal duplicate();
	
	/**
	 * Defines the signal group which was warped 
	 * @return
	 */
	@NonNull UUID getSignalGroupId();
	
	/**
	 * Get the target shapes onto which signals have been warped
	 * @return
	 */
	@NonNull Set<WarpedSignalKey> getWarpedSignalKeys();
	
	
	/**
	 * Add a warped signal image for the given template
	 * @param template the template object signals were warped on to 
	 * @param name the name of the template object
	 * @param isCellWithSignalsOnly whether the image covers all cells in the source dataset
	 * @param image the warped image
	 */	
	void addWarpedImage(@NonNull CellularComponent template, @NonNull String name, boolean isCellWithSignalsOnly, @NonNull ByteProcessor image);
	
	/**
	 * Get the warped signal image corresponding to the signals warped onto 
	 * the given target shape
	 * @param template
	 * @param isCellWithSignalsOnly whether the image covers all cells in the source dataset, or just those with defined signals
	 * @return
	 */
	Optional<ImageProcessor> getWarpedImage(@NonNull CellularComponent template, boolean isCellWithSignalsOnly);
	
	/**
	 * Get the warped signal image corresponding to the signals warped onto 
	 * the given target shape
	 * @param key the signal key
	 * @return
	 */
	Optional<ImageProcessor> getWarpedImage(@NonNull WarpedSignalKey key);
	
	
	/**
	 * Get the name of the target shape
	 * @param template
	 * @return
	 */
	String getTargetName(@NonNull WarpedSignalKey key);
		
}
