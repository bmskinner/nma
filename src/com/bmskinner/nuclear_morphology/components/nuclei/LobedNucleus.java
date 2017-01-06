package com.bmskinner.nuclear_morphology.components.nuclei;

import java.util.Set;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;

/**
 * Describes the features of a lobed nucleus, such as in a granulocyte.
 * @author ben
 * @since 1.13.4
 *
 */
public interface LobedNucleus extends Nucleus {
	
	/**
	 * Add a lobe centre of mass
	 * @param com
	 */
	void addLobe(IPoint com);
	
	/**
	 * Get the centres of mass of all lobes
	 * @return the centres of mass
	 */
	Set<IPoint> getLobeCoMs();
	
	
	/**
	 * Get the number of lobes in this nucleus
	 * @return the number of lobes
	 */
	int getLobeCount();
	

}
