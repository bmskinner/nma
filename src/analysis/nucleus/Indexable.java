package analysis.nucleus;

import components.generic.BorderTag;

/**
 * These control the border tags that can be identified
 * within a profile. Applies to individual nuclei, and to 
 * aggregate profiles.
 * @author bms41
 *
 */
public interface Indexable {
	
	/**
	 * Find the index of the requested border tag in the 
	 * appropriate profile for the nucleus type, or aggregate
	 * type.
	 * @param tag
	 * @return the index or -1 on error
	 */
	public int identifyBorderTagIndex(BorderTag tag);

}
