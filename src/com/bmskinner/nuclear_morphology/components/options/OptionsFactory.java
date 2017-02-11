package com.bmskinner.nuclear_morphology.components.options;

import java.io.File;

/**
 * Provides default options types.
 * @author ben
 * @since 1.13.4
 *
 */
public interface OptionsFactory {

	/**
	 * Create the default options type for nucleus detection
	 * @param folder the folder to be searched
	 * @return
	 */
	static IMutableDetectionOptions makeNucleusDetectionOptions(File folder){
		return new DefaultNucleusHashDetectionOptions(folder);
	}
	
	/**
	 * Create the default options type for nucleus detection based on a template
	 * @param template the template options
	 * @return
	 */
	static IMutableDetectionOptions makeNucleusDetectionOptions(IDetectionOptions template){
		return new DefaultNucleusHashDetectionOptions(template);
	}
	
	/**
	 * Create the default options type for Canny edge detection
	 * @return
	 */
	static IMutableCannyOptions makeCannyOptions(){
		return new DefaultCannyHashOptions();
	}
	
	/**
	 * Create the default options type for Canny edge detection based on a template
	 * @param template the template options
	 * @return
	 */
	static IMutableCannyOptions makeCannyOptions(ICannyOptions template){
		return new DefaultCannyHashOptions(template);
	}
	
	/**
	 * Create the default options type for circle detection
	 * @return
	 */
	static IHoughDetectionOptions makeHoughOptions(){
		return new DefaultHoughOptions();
	}
	
	/**
	 * Create the default options type for circle detection based on a template
	 * @param template the template options
	 * @return
	 */
	static IHoughDetectionOptions makeHoughOptions(IHoughDetectionOptions template){
		return new DefaultHoughOptions(template);
	}
	
	/**
	 * Create the default options type for nuclear signal detection
	 * @param folder the folder to be searched
	 * @return
	 */
	static IMutableNuclearSignalOptions makeNuclearSignalOptions(File folder){
		return new DefaultNuclearSignalHashOptions(folder);
	}
	
	/**
	 * Create the default options type for nuclear signal detection based on a template
	 * @param template the template options
	 * @return
	 */
	static IMutableNuclearSignalOptions makeNuclearSignalOptions(INuclearSignalOptions template){
		return new DefaultNuclearSignalHashOptions(template);
	}
	
	/**
	 * Create the default analysis options type
	 * @return
	 */
	static IMutableAnalysisOptions makeAnalysisOptions(){
		return new DefaultAnalysisOptions();
	}
	
	/**
	 * Create the default analysis options type based on a template
	 * @param template the template options
	 * @return
	 */
	static IMutableAnalysisOptions makeAnalysisOptions(IAnalysisOptions template){
		return new DefaultAnalysisOptions(template);
	}
}
