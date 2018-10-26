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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import org.eclipse.jdt.annotation.NonNull;
import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.core.DatasetListManager;
import com.bmskinner.nuclear_morphology.io.xml.DatasetXMLCreator;

/**
 * Export the dataset to an nmd file
 * 
 * @author bms41
 *
 */
public class DatasetExportMethod extends SingleDatasetAnalysisMethod {

    private File saveFile = null;

    /**
     * Construct with a dataset to export and the file location
     * 
     * @param dataset the dataset to be exported
     * @param saveFile the file to export to
     */
    public DatasetExportMethod(@NonNull IAnalysisDataset dataset, @NonNull File saveFile) {
        super(dataset);
        this.saveFile = saveFile;
    }

    @Override
	public IAnalysisResult call() {
        run();
        IAnalysisResult r = new DefaultAnalysisResult(dataset);
        return r;
    }

    protected void run() {

    	try {
    		if (saveAnalysisDataset(dataset, saveFile)) 
    			fine("Save was sucessful");
    		else
    			warn("Save was unsucessful");

    	} catch (Exception e) {
    		warn("Save was unsucessful");
    		stack("Unable to save dataset", e);
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
         // Since we're creating a save format, go with nmd: Nuclear
 		// Morphology Dataset
 		fine("Saving dataset to " + saveFile.getAbsolutePath());
 		
 		File parentFolder = saveFile.getParentFile();
 		if(!parentFolder.exists())
 			parentFolder.mkdirs();


 		if(saveFile.isDirectory())
 			throw new IllegalArgumentException(String.format("File %s is a directory", saveFile.getName()));
 		if(saveFile.getParentFile()==null)
 			throw new IllegalArgumentException(String.format("Parent directory is null", saveFile.getAbsolutePath()));
 		if(!saveFile.getParentFile().canWrite())
 			throw new IllegalArgumentException(String.format("Parent directory %s is not writable", saveFile.getParentFile().getName()));

 		try(
 				OutputStream os = new FileOutputStream(saveFile);
 				CountedOutputStream cos = new CountedOutputStream(os);
 				){
 			cos.addCountListener( (l) -> fireProgressEvent(l));
 			Document doc = new DatasetXMLCreator(dataset).create();
 			XMLOutputter xmlOutput = new XMLOutputter();
 			xmlOutput.output(doc, cos);
 		} catch (IOException e) {
 			stack(String.format("Unable to write to file %s: %s", saveFile.getAbsolutePath(), e.getMessage()), e);
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
        // Since we're creating a save format, go with nmd: Nuclear
		// Morphology Dataset
		fine("Saving dataset to " + saveFile.getAbsolutePath());
		
		File parentFolder = saveFile.getParentFile();
		if(!parentFolder.exists())
			parentFolder.mkdirs();


		try(OutputStream fos        = new FileOutputStream(saveFile);
		    CountedOutputStream cos = new CountedOutputStream(fos);
		    OutputStream buffer     = new BufferedOutputStream(cos);
		    ObjectOutputStream output = new ObjectOutputStream(buffer);
		   ) {
			
			 cos.addCountListener( (l) -> fireProgressEvent(l));

		    output.writeObject(dataset);

		} catch (IOException e) {
		    error("IO error saving dataset", e);
		    ok = false;
		} catch (Exception e1) {
		    error("Unexpected exception saving dataset to: " + saveFile.getAbsolutePath(), e1);
		    ok = false;
		} catch (StackOverflowError e) {
		    error("StackOverflow saving dataset to: " + saveFile.getAbsolutePath(), e);
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
        if (saveFile.exists()) {
            saveFile.delete();
        }

        return saveAnalysisDataset(dataset, saveFile);

    }

    /**
     * Directly copy the source file to the destination file
     * 
     * @param sourceFile
     * @param destFile
     * @throws IOException
     */
    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        } finally {
            if (source != null) {
                source.close();
            }
            if (destination != null) {
                destination.close();
            }
        }
    }

}
