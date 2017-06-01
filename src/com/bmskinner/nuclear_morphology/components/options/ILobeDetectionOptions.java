package com.bmskinner.nuclear_morphology.components.options;

/**
 * This describes the detection requirements for finding lobes within nuclei.
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public interface ILobeDetectionOptions extends IDetectionOptions {

    /**
     * Get the diameter of lobes as a fraction of the nuclear diameter
     * 
     * @return
     */
    double getLobeDiameter();

}
