package com.bmskinner.nuclear_morphology.components.options;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.nuclear.IShellResult.ShrinkType;
import com.bmskinner.nuclear_morphology.components.options.IDetectionOptions.IDetectionSubOptions;

/**
 * Options for shell analysis
 * @author ben
 * @since 1.14.0
 *
 */
public interface IShellOptions extends IDetectionSubOptions {
	
	static final String SHELL_COUNT_KEY = "SHELL_COUNT";
	static final String EROSION_METHOD_KEY = "EROSION_METHOD";
	
	static final int DEFAULT_SHELL_COUNT = 5;
	static final ShrinkType DEFAULT_EROSION_METHOD = ShrinkType.AREA;
	
	/**
	 * Get the number of shells to divide the nucleus into
	 * @return
	 */
	int getShellNumber();
	
	/**
	 * Set the number of shells to divide the nucleus into
	 * @return
	 */
	void setShellNumber(int i);
	
	/**
	 * Get the erosion method to use for dividing
	 * @return
	 */
	ShrinkType getErosionMethod();
	
	/**
	 * Set the erosion method to use for dividing
	 * @return
	 */
	void setErosionMethod(@NonNull ShrinkType s);
	
	

}
