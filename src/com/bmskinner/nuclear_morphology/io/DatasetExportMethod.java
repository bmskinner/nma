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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Export the dataset to an nmd file
 * 
 * @author bms41
 *
 */
public class DatasetExportMethod extends SingleDatasetAnalysisMethod {
	
	private static final Logger LOGGER = Logger.getLogger(DatasetExportMethod.class.getName());

    private File saveFile = null;
    private ExportFormat format;

    
    /**
     * The formats in which an nmd file can be written.
     * Used because users should not need to deal with a ton of
     * different file extensions.
     * @author bms41
     * @since 1.14.0
     *
     */
    public enum ExportFormat {
    	
    	/** Java serialisation */
    	JAVA,
    	
    	/** XML serialisation */
    	XML;
    }
    /**
     * Construct with a dataset to export and the file location
     * 
     * @param dataset the dataset to be exported
     * @param saveFile the file to export to
     */
    public DatasetExportMethod(@NonNull IAnalysisDataset dataset, @NonNull File saveFile, ExportFormat format) {
        super(dataset);
        this.saveFile = saveFile;
        this.format = format;
    }

    @Override
	public IAnalysisResult call() {
        run();
        return new DefaultAnalysisResult(dataset);
    }

    protected void run() {

    	try {
    		
    		boolean isOk = false;
    		switch(format) {    		
	    		case XML: {
	    			backupExistingSaveFile();
	    			isOk = saveAnalysisDatasetToXML(dataset, saveFile); 
	    			break;
	    		}
	    		case JAVA: 
	    		default: isOk = saveAnalysisDataset(dataset, saveFile); break;
    		}

    		if (isOk) 
    			LOGGER.info("Save was sucessful");
    		else
    			LOGGER.warning("Save was unsucessful");

    	} catch (Exception e) {
    		LOGGER.log(Loggable.STACK, "Unable to save dataset", e);
    	}
    }

    
    /**
     * Save the given dataset in XML format
     * @param dataset the dataset to save
     * @param saveFile the file to save to
     * @return
     */
    public boolean saveAnalysisDatasetToXML(IAnalysisDataset dataset, File saveFile) {
    	 boolean ok = true;
 		LOGGER.fine("Saving XML dataset to " + saveFile.getAbsolutePath());
 		
 		File parentFolder = saveFile.getParentFile();
 		if(!parentFolder.exists())
 			parentFolder.mkdirs();


 		if(saveFile.isDirectory())
 			throw new IllegalArgumentException(String.format("File %s is a directory", saveFile.getName()));
 		if(saveFile.getParentFile()==null)
 			throw new IllegalArgumentException(String.format("Parent directory %s is null", saveFile.getAbsolutePath()));
 		if(!saveFile.getParentFile().canWrite())
 			throw new IllegalArgumentException(String.format("Parent directory %s is not writable", saveFile.getParentFile().getName()));

 		try(
 				OutputStream os = new FileOutputStream(saveFile);
 				CountedOutputStream cos = new CountedOutputStream(os);
 				){
 			cos.addCountListener(this::fireProgressEvent);
 			XMLOutputter xmlOutput = new XMLOutputter();
 			xmlOutput.setFormat(Format.getPrettyFormat());
 			xmlOutput.output(dataset.toXmlElement(), cos);
 		} catch (IOException e) {
 			LOGGER.log(Loggable.STACK, String.format("Unable to write to file %s: %s", saveFile.getAbsolutePath(), e.getMessage()), e);
 			ok = false;
 		}
 		

 		return ok;
    }
    
    /**
     * Save the given dataset to the given file
     * 
     * @param dataset the dataset
     * @param saveFile the file to save as
     * @return
     */
    public boolean saveAnalysisDataset(IAnalysisDataset dataset, File saveFile) {

        boolean ok = true;

		LOGGER.fine("Saving dataset to " + saveFile.getAbsolutePath());
		
		File parentFolder = saveFile.getParentFile();
		if(!parentFolder.exists())
			parentFolder.mkdirs();


		try(OutputStream fos        = new FileOutputStream(saveFile);
		    CountedOutputStream cos = new CountedOutputStream(fos);
		    OutputStream buffer     = new BufferedOutputStream(cos);
		    ObjectOutputStream output = new ObjectOutputStream(buffer);
		   ) {
			
			 cos.addCountListener(this::fireProgressEvent);

		    output.writeObject(dataset);

		} catch (IOException e) {
		    LOGGER.log(Loggable.STACK, "IO error saving dataset", e);
		    ok = false;
		} catch (Exception e1) {
		    LOGGER.log(Loggable.STACK, "Unexpected exception saving dataset to: " + saveFile.getAbsolutePath(), e1);
		    ok = false;
		} catch (StackOverflowError e) {
		    LOGGER.log(Loggable.STACK, "StackOverflow saving dataset to: " + saveFile.getAbsolutePath(), e);
		    ok = false;
		}

		if (!ok)
		    return false;

		DatasetListManager.getInstance().updateHashCode(dataset);
        return true;
    }

    /**
     * Save the given dataset to it's preferred save path
     * 
     * @param dataset the dataset
     * @return ok or not
     */
    public boolean saveAnalysisDataset(IAnalysisDataset dataset) {

        File saveFile = dataset.getSavePath();
        if (saveFile.exists())
            saveFile.delete();
        return saveAnalysisDataset(dataset, saveFile);

    }

    private void backupExistingSaveFile() {
    	File saveFile = dataset.getSavePath();
    	if (!saveFile.exists())
    		return;

    	File backupFile = new File(saveFile.getParent(), 
    			saveFile.getName().replaceAll(Io.SAVE_FILE_EXTENSION, 
    					Io.BACKUP_FILE_EXTENSION));
    	try {
    		copyFile(saveFile, backupFile);
    	} catch (IOException e) {
    		LOGGER.log(Loggable.STACK, e.getMessage(), e);
    	}     
    }


    /**
     * Directly copy the source file to the destination file
     * 
     * @param sourceFile
     * @param destFile
     * @throws IOException
     */
    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists())
            destFile.createNewFile();

        try( FileChannel source = new FileInputStream(sourceFile).getChannel();
        	 FileChannel destination = new FileOutputStream(destFile).getChannel();) {
            destination.transferFrom(source, 0, source.size());
        }
    }

}
