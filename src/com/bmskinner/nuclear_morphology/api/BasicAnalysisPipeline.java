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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.analysis.nucleus.NucleusDetectionMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod;
import com.bmskinner.nuclear_morphology.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nuclear_morphology.components.CellularComponent;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.components.options.OptionsFactory;
import com.bmskinner.nuclear_morphology.io.DatasetExportMethod;
import com.bmskinner.nuclear_morphology.io.DatasetStatsExporter;
import com.bmskinner.nuclear_morphology.io.Io;

/**
 * A test pipeline to be run from the command line. Analyse a folder with default
 * settings, save the nmd, and export a nuclear stats file
 * @author bms41
 * @since 1.14.0
 *
 */
public class BasicAnalysisPipeline {
	
	public BasicAnalysisPipeline(@NonNull final File folder) throws Exception {
		
    	IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(folder);

    	Instant inst = Instant.ofEpochMilli(op.getAnalysisTime());
		LocalDateTime anTime = LocalDateTime.ofInstant(inst, ZoneOffset.systemDefault());
		String outputFolderName = anTime.format(DateTimeFormatter.ofPattern("YYYY-MM-dd_HH-mm-ss"));
    	File outFolder = new File(folder, outputFolderName);
    	outFolder.mkdirs();
    	File saveFile = new File(outFolder, folder.getName()+Io.SAVE_FILE_EXTENSION);
    	runNewAnalysis(folder.getAbsolutePath(), op, saveFile);
	}

	
	/**
     * Run a new analysis on the images using the given options.
     * @param folder the name of the output folder for the nmd file
     * @param op the detection options
     * @param saveFile the full path to the nmd file
     * @return the new dataset
     * @throws Exception
     */
    private void runNewAnalysis(String folder, IAnalysisOptions op, File saveFile) throws Exception {
        
        if(!op.getDetectionOptions(CellularComponent.NUCLEUS).get().getFolder().exists())
            throw new IllegalArgumentException("Detection folder does not exist");
        
        File statsFile = new File(saveFile.getParentFile(), saveFile.getName()+Io.TAB_FILE_EXTENSION);

        IAnalysisDataset obs = new NucleusDetectionMethod(folder, op)
        		.call().getFirstDataset();
        
        new DatasetProfilingMethod(obs)
        	.then(new DatasetSegmentationMethod(obs, MorphologyAnalysisMode.NEW))
        	.then(new DatasetExportMethod(obs, saveFile))
        	.then(new DatasetStatsExporter(statsFile, obs))
        	.call();
    }

}
