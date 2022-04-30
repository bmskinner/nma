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
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.analysis.DefaultAnalysisResult;
import com.bmskinner.nma.analysis.IAnalysisResult;
import com.bmskinner.nma.analysis.MultipleDatasetAnalysisMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.logging.Loggable;

import ij.IJ;

/**
 * Abstract exporter
 * @author bms41
 * @since 1.13.8
 *
 */
public abstract class StatsExporter extends MultipleDatasetAnalysisMethod implements Io {
	
	private static final Logger LOGGER = Logger.getLogger(StatsExporter.class.getName());
    
    private File exportFile;
    private static final String DEFAULT_MULTI_FILE_NAME = "Stats_export" + Io.TAB_FILE_EXTENSION;
    
    /**
     * Create specifying the folder stats will be exported into
     * 
     * @param folder
     */
    public StatsExporter(@NonNull File file, @NonNull List<IAnalysisDataset> list) {
        super(list);
        setupExportFile(file);
    }

    /**
     * Create specifying the folder stats will be exported into
     * 
     * @param folder
     */
    public StatsExporter(@NonNull File file, @NonNull IAnalysisDataset dataset) {
        super(dataset);
        setupExportFile(file);
    }

    @Override
    public IAnalysisResult call() throws Exception{
        export(datasets);
        return new DefaultAnalysisResult(datasets);
    }
    
    private void setupExportFile(@NonNull File file) {
        if (file.isDirectory())
            file = new File(file, DEFAULT_MULTI_FILE_NAME);

        exportFile = file;

        try {
			Files.deleteIfExists(exportFile.toPath());
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, "Unable to delete file: "+exportFile);
			LOGGER.log(Loggable.STACK, "Unable to delete existing file", e);
		}
    }
    
    /**
     * Export stats from all datasets in the list to the same file
     * 
     * @param list
     */
    protected void export(@NonNull List<IAnalysisDataset> list) throws Exception {       
        StringBuilder outLine = new StringBuilder();
        appendHeader(outLine);

        for (@NonNull IAnalysisDataset d : list) {
            append(d, outLine);
            fireProgressEvent();
        }

        fireIndeterminateState();
        IJ.append(outLine.toString(), exportFile.getAbsolutePath());
    }
    
    /**
     * Generate the column headers for the stats, and append to the string
     * builder.
     * @param outLine
     */
    protected abstract void appendHeader(@NonNull StringBuilder outLine);
    
    /**
     * Append the stats for the dataset to the builder
     * @param d
     * @param outLine
     * @throws Exception
     */
    protected abstract void append(@NonNull IAnalysisDataset d, @NonNull StringBuilder outLine) throws Exception;

}
