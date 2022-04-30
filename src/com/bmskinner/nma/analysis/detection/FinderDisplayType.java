package com.bmskinner.nma.analysis.detection;

/**
 * Switch between finder types
 * @author ben
 * @since 2.0.0
 *
 */
public enum FinderDisplayType {
	
	/** Run as a pipeline, with no generation of preview images*/
	PIPELINE,
	
	/** Create preview images of each detection step*/
	PREVIEW

}
