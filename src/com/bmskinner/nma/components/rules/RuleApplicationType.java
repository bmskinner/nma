package com.bmskinner.nma.components.rules;

/**
 * Control whether profile rules are applied to 
 * individual nuclei or to the median profile 
 * @author ben
 * @since 1.18.3
 *
 */
public enum RuleApplicationType {
	/** Rules are applied on a single nucleus, 
	 * independent of the median or other nuclei
	 */
	PER_NUCLEUS,
	
	/** Rules are applied to the median profile,
	 * and then propogated to each nucleus
	 */
	VIA_MEDIAN;
		
	public String prettyFormat() {
		return this.name().substring(0, 1).toUpperCase() + 
				this.name().substring(1).replace("_", " ").toLowerCase();
	}

}
