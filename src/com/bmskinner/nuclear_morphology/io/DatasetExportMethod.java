/*******************************************************************************
 * Copyright (C) 2017 Ben Skinner
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
 *******************************************************************************/


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

import com.bmskinner.nuclear_morphology.analysis.DefaultAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.IAnalysisResult;
import com.bmskinner.nuclear_morphology.analysis.SingleDatasetAnalysisMethod;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.main.DatasetListManager;

/**
 * Export the dataset to an nmd file
 * 
 * @author bms41
 *
 */
public class DatasetExportMethod extends SingleDatasetAnalysisMethod {

    private File saveFile = null;
    // private boolean useHDF5 = false;

    /**
     * Construct with a dataset to export and the file location
     * 
     * @param dataset
     *            the dataset to be exported
     * @param saveFile
     *            the file to export to
     */
    public DatasetExportMethod(IAnalysisDataset dataset, File saveFile) {
        super(dataset);
        this.saveFile = saveFile;
    }

    public IAnalysisResult call() {
        run();
        IAnalysisResult r = new DefaultAnalysisResult(dataset);
        return r;
    }

    protected void run() {

        try {

            if (saveAnalysisDataset(dataset, saveFile)) {
                finest("Save was sucessful");

            } else {
                warn("Save was unsucessful");
            }

        } catch (Exception e) {
            warn("Save was unsucessful");
            stack("Unable to save dataset", e);
        }

    }

    /**
     * Save the given dataset to the given file
     * 
     * @param dataset
     *            the dataset
     * @param saveFile
     *            the file to save as
     * @return
     */
    public boolean saveAnalysisDataset(IAnalysisDataset dataset, File saveFile) {

        boolean ok = true;
        try {
            // Since we're creating a save format, go with nmd: Nuclear
            // Morphology Dataset
            fine("Saving dataset to " + saveFile.getAbsolutePath());

            // use buffering
            OutputStream fos = new FileOutputStream(saveFile);

            CountedOutputStream cos = new CountedOutputStream(fos);
            OutputStream buffer = new BufferedOutputStream(cos);
            ObjectOutputStream output = new ObjectOutputStream(buffer);
            
            cos.addCountListener( (l) ->{
            	fireProgressEvent(l);
            });

            try {

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
            } finally {
                output.close();
                buffer.close();
                fos.close();
            }

            // This line is not always reached when saving multiple datasets
            fine("Save complete");

            if (!ok) {
                return false;
            }

            DatasetListManager.getInstance().updateHashCode(dataset); // track
                                                                      // the
                                                                      // state
                                                                      // since
                                                                      // last
                                                                      // save

        } catch (FileNotFoundException e) {
            warn("File not found when saving dataset");
            return false;
        } catch (IOException e2) {
            error("IO error saving dataset", e2);
            return false;
        }
        return true;
    }

    /**
     * Save the given dataset to it's preferred save path
     * 
     * @param dataset
     *            the dataset
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
