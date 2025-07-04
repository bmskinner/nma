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
package com.bmskinner.nma.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.MultipleDatasetAnalysisMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.utility.DatasetUtils;

/**
 * Abstract exporter
 * 
 * @author bms41
 * @since 1.13.8
 *
 */
public abstract class MeasurementsExportMethod extends MultipleDatasetAnalysisMethod implements Io {

	private static final Logger LOGGER = Logger.getLogger(MeasurementsExportMethod.class.getName());

	private File exportFile;
	private static final String DEFAULT_MULTI_FILE_NAME = "Stats_export" + Io.TAB_FILE_EXTENSION;

	// Any options to be stored
	protected HashOptions options;

	/**
	 * Create specifying the folder profiles will be exported into. If a file is
	 * given, this file will be used. If a directory is given, a file with a default
	 * name will be created in that directory.
	 * 
	 * @param file    the output file or folder for the export
	 * @param list    the datasets to export
	 * @param options other options for the export
	 */
	protected MeasurementsExportMethod(@NonNull File file, @NonNull List<IAnalysisDataset> list,
			@NonNull HashOptions options) {
		super(list);
		this.options = options;
		createExportFilePath(file);
	}

	/**
	 * Create specifying the folder profiles will be exported into
	 * 
	 * @param file    the output file for the export
	 * @param dataset the dataset to export
	 * @param options other options for the export
	 */
	protected MeasurementsExportMethod(@NonNull File file, @NonNull IAnalysisDataset dataset,
			@NonNull HashOptions options) {
		super(dataset);
		this.options = options;
		createExportFilePath(file);
	}

	@Override
	public IAnalysisResult call() throws Exception {
		export(datasets);
		return new DefaultAnalysisResult(datasets);
	}

	/**
	 * Create the name for the export file. If the input file is a directory, use
	 * the default name, otherwise use the given file.
	 * 
	 * @param file the file or folder to create from
	 */
	private void createExportFilePath(@NonNull File file) {
		if (file.isDirectory())
			file = new File(file, DEFAULT_MULTI_FILE_NAME);

		exportFile = file;

		try {
			Files.deleteIfExists(exportFile.toPath());
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Unable to delete file: %s".formatted(exportFile), e);
		}
	}

	/**
	 * Export stats from all datasets in the list to the same file
	 * 
	 * @param list the datasets to export
	 */
	protected void export(@NonNull List<IAnalysisDataset> list) throws Exception {

		try (
				OutputStream os = new FileOutputStream(exportFile);
				CountedOutputStream cos = new CountedOutputStream(os);
				PrintWriter p = new PrintWriter(cos);) {
			cos.addCountListener((l) -> fireProgressEvent(l));

			StringBuilder outLine = new StringBuilder();
			appendHeader(outLine);

			// Update the progress bar length with ~correct value.
			// Estimate from the header line size and number of cells
			// Should be a slight overestimate
			fireUpdateProgressTotalLength(
					DatasetUtils.size(list) * outLine.toString().getBytes().length);

			p.write(outLine.toString());

			for (@NonNull
			IAnalysisDataset d : list) {
				append(d, p);
			}

		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Unable to write to file: %s".formatted(e.getMessage()), e);
		}

		fireIndeterminateState();
	}

	/**
	 * Generate the column headers for the stats, and append to the string builder.
	 * 
	 * @param outLine
	 */
	protected abstract void appendHeader(@NonNull StringBuilder outLine);

	/**
	 * Append the stats for the dataset to the builder
	 * 
	 * @param d
	 * @param outLine
	 * @throws Exception
	 */
	protected abstract void append(@NonNull IAnalysisDataset d, @NonNull PrintWriter pw)
			throws Exception;

}
