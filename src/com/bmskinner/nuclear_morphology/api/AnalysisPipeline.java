/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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
	class AnalysisPipelineException extends Exception {
		private static final long serialVersionUID = 1L;
		
		public AnalysisPipelineException(String message) {
			super(message);
		}
		
		public AnalysisPipelineException(Exception e) {
			super(e);
		}
	}

}
