package com.bmskinner.nuclear_morphology.components.nuclear;

import java.io.Serializable;
import java.util.List;
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
	 * Defines the signal group which was warped 
	 * @return
	 */
	@NonNull UUID getSignalGroupId();
	
	/**
	 * Get the templates onto which signals have been warped
	 * @return
	 */
	@NonNull Set<CellularComponent> getTemplates();
	
	
	/**
	 * Add a warped signal image for the given template
	 * @param template the template object signals were warped on to 
	 * @param image the warped image
	 */
//	void addWarpedImage(@NonNull CellularComponent template, @NonNull ImageProcessor image);
	
	void addWarpedImage(@NonNull CellularComponent template, @NonNull ByteProcessor image);
	
	/**
	 * Get the warped signal image corresponding to the signals warped onto 
	 * the given template
	 * @param template
	 * @return
	 */
	Optional<ImageProcessor> getWarpedImage(@NonNull CellularComponent template);
	
	/**
	 * Detect a signal within the warped signal image, using the given detection options.
	 * @param template the template onto which signals were warped
	 * @param options the detection options for retrieving the signal
	 * @return the signal that is detected by the given options
	 */
	Optional<INuclearSignal> getWarpedSignal(@NonNull CellularComponent template, @NonNull INuclearSignalOptions options);
	
}
