package com.bmskinner.nuclear_morphology.components.options;

/**
 * This describes the detection requirements for finding lobes within nuclei.
 * @author ben
 * @since 1.13.4
 *
 */
public interface IMutableLobeDetectionOptions extends ILobeDetectionOptions {

	/**
	 * Set the diameter of lobes as a fraction of the nuclear diameter
	 */
	void setLobeDiameter(double d);
}
