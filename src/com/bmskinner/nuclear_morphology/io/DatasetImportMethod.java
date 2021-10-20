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
package com.bmskinner.nuclear_morphology.io;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.analysis.AbstractAnalysisMethod;
import com.bmskinner.nuclear_morphology.analysis.AnalysisMethodException;
import com.bmskinner.nuclear_morphology.analysis.DatasetRepairer;
import com.bmskinner.nuclear_morphology.analysis.DatasetValidator;
import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.components.Version;
import com.bmskinner.nuclear_morphology.components.Version.UnsupportedVersionException;
import com.bmskinner.nuclear_morphology.components.cells.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.io.Io.Importer;
import com.bmskinner.nuclear_morphology.io.xml.XMLReader;
import com.bmskinner.nuclear_morphology.io.xml.XMLReader.XMLReadingException;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Method to read a dataset from file
 * 
 * @author ben
 * @since 1.13.4
 *
 */
public class DatasetImportMethod extends AbstractAnalysisMethod implements Importer {
	
	private static final Logger LOGGER = Logger.getLogger(DatasetImportMethod.class.getName());

    private final File       file;
    private IAnalysisDataset dataset      = null;
    private boolean          wasConverted = false;
    public static final int WAS_CONVERTED_BOOL = 0;
    
    /**
     * Store a map of signal image locations if necessary 
     */
    private Optional<Map<UUID, File>> signalFileMap = Optional.empty();

    /**
     * Construct with a file to be read
     * 
     * @param f the saved dataset file
     */
    public DatasetImportMethod(final File f) {
        super();

        if (!Importer.isSuitableImportFile(f)) {
        	LOGGER.warning(INVALID_FILE_ERROR);
            throw new IllegalArgumentException(INVALID_FILE_ERROR);
        }

        if (! (f.getName().endsWith(SAVE_FILE_EXTENSION) || f.getName().endsWith(BACKUP_FILE_EXTENSION)) ) {
        	LOGGER.warning("File is not nmd or bak format or has been renamed: "+f.getAbsolutePath());
            throw new IllegalArgumentException("File is not nmd or bak format or has been renamed: "+f.getAbsolutePath());
        }
        
        this.file = f;
    }
        
    /**
     * Call with an existing map of signal ids to dairectories of images. Designed for unit
     * testing.
     * @param f
     * @param signalFiles a map of signal group to folder of signals
     */
    public DatasetImportMethod(final File f, final Map<UUID, File> signalFiles) {
        this(f);
        signalFileMap = Optional.of(signalFiles);
    }

    @Override
    public IAnalysisResult call() throws Exception {
    	run(); 

        if (dataset == null)
            throw new UnloadableDatasetException(String.format("Could not load file '%s'", file.getAbsolutePath()));

        DefaultAnalysisResult r = new DefaultAnalysisResult(dataset);
        r.setBoolean(WAS_CONVERTED_BOOL, wasConverted);
        return r;
    }

    private void run() throws Exception {

    	// Clean up old log lock files. Legacy.
    	cleanLockFilesInDir(file.getParentFile());

    	// Deserialise whatever is in the file
    	LOGGER.fine("Reading file as XML");
    	dataset = readXMLDataset(file);
    	validateDataset();
    	fireIndeterminateState();
    }
        
    /**
     * Check the dataset has valid segments and profiles
     * @throws Exception 
     */
    private void validateDataset() throws Exception {
    	// Check the validity of the loaded dataset
    	// Repair if possible, or warn if not
    	DatasetRepairer dr = new DatasetRepairer();
    	dr.repair(dataset);

    	DatasetValidator dv = new DatasetValidator();
    	if (!dv.validate(dataset)) {
    		for (String s : dv.getSummary()) {
    			LOGGER.log(Loggable.STACK, s);
    		}
    		LOGGER.warning("The dataset is not properly segmented");
    		LOGGER.warning("Curated datasets and groups have been saved");
    		LOGGER.warning("Either resegment (Editing>Segmentation>Segment profile) or redetect cells and import the ." + Importer.LOC_FILE_EXTENSION + " file");

    		new CellFileExporter(dataset).call();
    		throw new AnalysisMethodException("Unable to validate or repair dataset");
    	}
    }

    /**
     * Older version of the program did not always close log handlers properly,
     * so lck files may have proliferated. Kill them with fire.
     * 
     * @param dir the directory to clean
     */
    private void cleanLockFilesInDir(File dir) {
        
        FilenameFilter filter = (folder, name) -> name.toLowerCase().endsWith(Io.LOCK_FILE_EXTENSION);

        File[] files = dir.listFiles(filter);

        if (files == null)
            return;

        for (File lockFile : files)
            lockFile.delete();
    }

    
    private IAnalysisDataset readXMLDataset(File inputFile) throws UnloadableDatasetException, UnsupportedVersionException {

    	try {
    		IAnalysisDataset d = XMLReader.readDataset(inputFile);
    		if(!Version.versionIsSupported(d.getVersionCreated()))
    			throw new UnsupportedVersionException(d.getVersionCreated());
    		return d;
    	} catch(XMLReadingException | ComponentCreationException e) {
    		LOGGER.fine("Error reading XML: "+e.getMessage());
    		throw new UnloadableDatasetException("Cannot read as XML dataset", e);
    	}
    }
    
    public class UnloadableDatasetException extends Exception {
        private static final long serialVersionUID = 1L;

        public UnloadableDatasetException() {
            super();
        }
        public UnloadableDatasetException(String message) {
            super(message);
        }
        public UnloadableDatasetException(String message, Throwable cause) {
            super(message, cause);
        }
        public UnloadableDatasetException(Throwable cause) {
            super(cause);
        }
    }

}
