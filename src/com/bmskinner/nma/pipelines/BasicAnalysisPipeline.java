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
package com.bmskinner.nma.pipelines;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.nucleus.NucleusDetectionMethod;
import com.bmskinner.nma.analysis.profiles.DatasetProfilingMethod;
import com.bmskinner.nma.analysis.profiles.DatasetSegmentationMethod;
import com.bmskinner.nma.analysis.profiles.DatasetSegmentationMethod.MorphologyAnalysisMode;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.DefaultOptions;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.io.DatasetExportMethod;
import com.bmskinner.nma.io.DatasetStatsExporter;
import com.bmskinner.nma.io.Io;

/**
 * A test pipeline to be run from the command line. Analyse a folder with
 * default settings, save the nmd, and export a nuclear stats file
 * 
 * @author bms41
 * @since 1.14.0
 *
 */
public class BasicAnalysisPipeline {

	private static final Logger LOGGER = Logger.getLogger(BasicAnalysisPipeline.class.getName());

	public BasicAnalysisPipeline(@NonNull final File folder) throws Exception {

		IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(folder);

		Instant inst = Instant.ofEpochMilli(op.getAnalysisTime());
		LocalDateTime anTime = LocalDateTime.ofInstant(inst, ZoneOffset.systemDefault());
		String outputFolderName = anTime.format(DateTimeFormatter.ofPattern("YYYY-MM-dd_HH-mm-ss"));
		File outFolder = new File(folder, outputFolderName);
		outFolder.mkdirs();
		File saveFile = new File(outFolder, folder.getName() + Io.NMD_FILE_EXTENSION);
		runNewAnalysis(folder, op, saveFile);
	}

	/**
	 * Run a new analysis on the images using the given options.
	 * 
	 * @param folder   the name of the output folder for the nmd file
	 * @param op       the detection options
	 * @param saveFile the full path to the nmd file
	 * @return the new dataset
	 * @throws Exception
	 */
	private void runNewAnalysis(File folder, IAnalysisOptions op, File saveFile)
			throws Exception {

		if (!op.getDetectionFolder(CellularComponent.NUCLEUS)
				.orElseThrow(() -> new IllegalArgumentException("Non nucleus detection options"))
				.exists())
			throw new IllegalArgumentException("Detection folder does not exist");

		LOGGER.info("Analysing folder: " + folder);

		File statsFile = new File(saveFile.getParentFile(),
				saveFile.getName() + Io.TAB_FILE_EXTENSION);
		LOGGER.info("Saving to: " + statsFile.getAbsolutePath());

		IAnalysisDataset obs = new NucleusDetectionMethod(folder, op)
				.call().getFirstDataset();

		HashOptions exportOptions = new DefaultOptions();
		exportOptions.setInt(Io.PROFILE_SAMPLES_KEY, 100);

		new DatasetProfilingMethod(obs)
				.then(new DatasetSegmentationMethod(obs,
						MorphologyAnalysisMode.SEGMENT_FROM_SCRATCH))
				.then(new DatasetExportMethod(obs, saveFile))
				.then(new DatasetStatsExporter(statsFile, obs, exportOptions))
				.call();
	}

}
