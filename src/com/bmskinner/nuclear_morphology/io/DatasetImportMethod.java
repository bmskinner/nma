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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.analysis.AbstractAnalysisMethod;
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

    	try{ 
    		run(); 
    	} catch (UnsupportedVersionException e) {
    		throw(e);
    	}  

        if (dataset == null)
            throw new UnloadableDatasetException(String.format("Could not load file '%s'", file.getAbsolutePath()));

        DefaultAnalysisResult r = new DefaultAnalysisResult(dataset);
        r.setBoolean(WAS_CONVERTED_BOOL, wasConverted);
        return r;
    }

    private void run() throws Exception {

        try {
            // Clean up old log lock files. Legacy.
            cleanLockFilesInDir(file.getParentFile());

            try {
            	// Deserialise whatever is in the file
            	LOGGER.fine("Trying to read file as XML");
            	dataset = readXMLDataset(file);

            	fireIndeterminateState();
            } catch (UnsupportedVersionException e) {
            	LOGGER.warning("Version "+e.getMessage()+" not supported");
            	if(e.getDetectedVersion().isNewerThan(Version.currentVersion()))
            		LOGGER.warning(String.format("Dataset version %s is from a newer software version; upgrade to view", e.getDetectedVersion()));
            	if(e.getDetectedVersion().isOlderThan(Version.currentVersion()))
            		LOGGER.warning(String.format("Dataset version %s is too old to read in this software", e.getDetectedVersion()));
            	throw(e);

            } catch (UnloadableDatasetException e) {
            	LOGGER.warning(e.getMessage());
            	LOGGER.log(Loggable.STACK, "Error reading dataset", e);
            }

            if(dataset==null)
                return; // Exception will be thrown in call() method
                        
            validateDataset();

        } catch (IllegalArgumentException e) {
            LOGGER.warning("Unable to open file '" + file.getAbsolutePath() + "': " + e.getMessage());
            LOGGER.log(Loggable.STACK, "Error opening file", e);
        }
    }
        
    /**
     * Check the dataset has valid segments and profiles
     */
    private void validateDataset() {
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
            try {
				new CellFileExporter(dataset).call();
			} catch (Exception e) {
				LOGGER.warning("Unable to save cell locations");
				LOGGER.warning("Redetect these nuclei");
			}
           
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
    
    private IAnalysisDataset readDataset(File inputFile) throws UnloadableDatasetException, UnsupportedVersionException {
    	LOGGER.fine("Deserialising dataset");
        IAnalysisDataset dataset = null;

        try(FileInputStream fis = new FileInputStream(inputFile.getAbsolutePath());
        	CountedInputStream cis = new CountedInputStream(fis);
        	BufferedInputStream bis = new BufferedInputStream(cis);
        	ObjectInputStream ois = new PackageReplacementObjectInputStream(bis);
        		) {            
            
            cis.addCountListener(this::fireProgressEvent);
            
            // This was needed when classes changed packages between versions
            
            dataset = (IAnalysisDataset) ois.readObject();

        } catch (UnsupportedVersionException e1) {
                
        	throw(e1);

        } catch (ClassNotFoundException e1) {
        	LOGGER.warning("Missing class: "+e1.getMessage());
        	LOGGER.log(Loggable.STACK, "Class not found reading '" + file.getAbsolutePath() + "': ", e1);
        	throw new UnloadableDatasetException("Missing class "+e1.getMessage());
        } catch (NullPointerException e1) {
            // holdover from deserialisation woes when migrating packages
            LOGGER.log(Loggable.STACK, "NPE Error reading '" + file.getAbsolutePath() + "': ", e1);
            throw new UnloadableDatasetException("Cannot load dataset due to " + e1.getClass().getSimpleName(), e1);

        } catch (OptionalDataException e1) {
            /*
             * Exception indicating the failure of an object read operation due
             * to unread primitive data, or the end of data belonging to a
             * serialized object in the stream.
             */
            if (e1.eof) {

                /*
                 * An attempt was made to read past the end of data consumable
                 * by a class-defined readObject or readExternal method. In this
                 * case, the OptionalDataException's eof field is set to true
                 */
                LOGGER.log(Loggable.STACK, "Unexpected end of data '" + file.getAbsolutePath() + "'", e1);
            } else {

                /*
                 * An attempt was made to read an object when the next element
                 * in the stream is primitive data. In this case, the
                 * OptionalDataException's length field is set to the number of
                 * bytes of primitive data immediately readable from the stream,
                 * and the eof field is set to false
                 */
                LOGGER.log(Loggable.STACK, file.getAbsolutePath() + ": " + e1.length + " remaining in buffer", e1);
            }
            throw new UnloadableDatasetException(
                    "Cannot load '" + file.getAbsolutePath() + "' due to unexpected end of file", e1);

        } catch (Exception e1) {
            // Is there anything else left that could go wrong? Probably.
            LOGGER.log(Loggable.STACK, "Error reading '" + file.getAbsolutePath() + "'", e1);
            throw new UnloadableDatasetException(
                    "Cannot load '" + file.getAbsolutePath() + "' due to " + e1.getClass().getSimpleName(), e1);

        } catch (StackOverflowError e) {
            // From when a recursive loop was entered building segments.
            throw new UnloadableDatasetException("Stack overflow loading '" + file.getAbsolutePath() + "'", e);

        }
        return dataset;
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
