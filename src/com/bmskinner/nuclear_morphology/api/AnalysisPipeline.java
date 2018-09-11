package com.bmskinner.nuclear_morphology.api;

import java.io.File;

import org.eclipse.jdt.annotation.NonNull;

/**
 * The interface for pipelines when being called externally
 * @author ben
 * @since 1.14.0
 */
public interface AnalysisPipeline {
	
	/**
	 * Run the pipeline on the given folder of images, using analysis options
	 * provided in an xml file. 
	 * @param imageFolder the folder of images to be analysed. Can be recursive.
	 * @param xmlSettingsFile the options file for the analysis
	 */
	void run(@NonNull final File imageFolder, @NonNull final File xmlSettingsFile) throws AnalysisPipelineException;
	
	
	/**
	 * Thrown when an analysis encounters an error.
	 * @author ben
	 * @since 1.14.0
	 *
	 */
	public class AnalysisPipelineException extends Exception {
		private static final long serialVersionUID = 1L;
		
		public AnalysisPipelineException(String message) {
			super(message);
		}
		
		public AnalysisPipelineException(Exception e) {
			super(e);
		}
	}

}
