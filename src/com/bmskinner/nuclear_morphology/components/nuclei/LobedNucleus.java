package com.bmskinner.nuclear_morphology.components.nuclei;

import java.util.Set;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclear.Lobe;

/**
 * Describes the features of a lobed nucleus, such as in a granulocyte.
 * @author ben
 * @since 1.13.4
 *
 */
public interface LobedNucleus extends Nucleus {
		
	/**
	 * Add a lobe to the nucleus. The nucleis must contain the 
	 * lobe centre of mass
	 * @param l the lobe to add
	 */
	void addLobe(Lobe l);
	
	/**
	 * Get all the lobes in the nucleus
	 * @return
	 */
	Set<Lobe> getLobes();
	
	/**
	 * Get the centres of mass of all lobes
	 * @return the centres of mass
	 */
	Set<IPoint> getLobeCoMs();
	
	/**
	 * Get the number of lobes detected in the nucleus
	 * @return
	 */
	int getLobeCount();
	
	/**
	 * Remove all lobes from the nucleus
	 */
	void removeAllLobes();

}
