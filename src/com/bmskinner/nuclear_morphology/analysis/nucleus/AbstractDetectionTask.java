/*******************************************************************************
 *  	Copyright (C) 2016 Ben Skinner
 *   
 *     This file is part of Nuclear Morphology Analysis.
 *
 *     Nuclear Morphology Analysis is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Nuclear Morphology Analysis is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Nuclear Morphology Analysis. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/

package com.bmskinner.nuclear_morphology.analysis.nucleus;

import java.io.File;

import com.bmskinner.nuclear_morphology.analysis.AbstractProgressAction;
import com.bmskinner.nuclear_morphology.components.ICellCollection;
import com.bmskinner.nuclear_morphology.components.options.IAnalysisOptions;
import com.bmskinner.nuclear_morphology.io.ImageImporter;

/**
 * Recursive task to find cells in a folder of images
 * 
 * @author bms41
 * @since 1.13.4
 *
 */
@SuppressWarnings("serial")
public abstract class AbstractDetectionTask extends AbstractProgressAction {

    protected final ICellCollection  collection;
    protected File[]                 files;
    protected static final int       THRESHOLD = 5;  // number of images to
                                                     // handle per fork
    protected final int              low, high;
    protected final String           outputFolder;
    protected final IAnalysisOptions analysisOptions;
    protected final File             folder;

    protected AbstractDetectionTask(File folder, File[] files, ICellCollection collection, int low, int high,
            String outputFolder, IAnalysisOptions analysisOptions) {
        this.collection = collection;
        this.files = files;
        this.folder = folder;
        this.low = low;
        this.high = high;
        this.outputFolder = outputFolder;
        this.analysisOptions = analysisOptions;
    }

    protected void analyseFiles() {

        for (int i = low; i < high; i++) {
            analyseFile(files[i]);
        }

    }

    /**
     * Checks that the given file is suitable for analysis. Is the file an
     * image. Also check if it is in the 'banned list'. These are prefixes that
     * are attached to exported images at later stages of analysis. This
     * prevents exported images from previous runs being analysed.
     *
     * @param file
     *            the File to check
     * @return a true or false of whether the file passed checks
     */
    public static boolean checkFile(File file) {

        if (file == null) {
            return false;
        }

        if (!file.isFile()) {
            return false;
        }

        String fileName = file.getName();

        for (String prefix : ImageImporter.PREFIXES_TO_IGNORE) {
            if (fileName.startsWith(prefix)) {
                return false;
            }
        }

        for (String fileType : ImageImporter.IMPORTABLE_FILE_TYPES) {
            if (fileName.endsWith(fileType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create the output folder for the analysis if required
     *
     * @param folder
     *            the folder in which to create the analysis folder
     * @return a File containing the created folder
     */
    protected File makeFolder(File folder) {
        File output = new File(folder.getAbsolutePath() + File.separator + this.outputFolder);
        if (!output.exists()) {
            try {
                output.mkdir();
            } catch (Exception e) {
                error("Failed to create directory", e);
            }
        }
        return output;
    }

    protected abstract void analyseFile(File f);

}
