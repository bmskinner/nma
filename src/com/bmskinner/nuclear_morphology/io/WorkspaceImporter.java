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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import com.bmskinner.nuclear_morphology.components.DefaultWorkspace;
import com.bmskinner.nuclear_morphology.components.IWorkspace;
import com.bmskinner.nuclear_morphology.io.Orter.Importer;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Load a workspace
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class WorkspaceImporter implements Loggable, Importer {

    private final File  file;
    public final String CHARSET = "ISO-8859-1";
    private final IWorkspace w;

    /**
     * Construct with a file to import.
     * 
     * @param f
     *            the file
     * @throws IllegalArgumentException
     *             if the file is null, a folder, or otherwise not a valid file
     */
    public WorkspaceImporter(final File f) {
        if (!Importer.isSuitableImportFile(f)) {
            throw new IllegalArgumentException(INVALID_FILE_ERROR);
        }

        file = f;
        w = new DefaultWorkspace(file);
    }

    /**
     * Import the workspace described by this importer.
     * 
     * @return a workspace
     */
    public IWorkspace importWorkspace() {

        
        try {

            FileInputStream fstream = new FileInputStream(file);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream, Charset.forName(CHARSET)));

            int i = 0;
            String strLine;
            while ((strLine = br.readLine()) != null) {
                i++;
                parseLine(strLine);
            }
            fstream.close();
        } catch (Exception e) {
            error("Error parsing workspace file", e);
        }

        return w;
    }
    
    private void parseLine(String line){
        
        // Check line format
        // Pre 1.13.8 have just the dataset file name.
        
        File f = null;
        
        if(line.endsWith(Importer.SAVE_FILE_EXTENSION)){
            // old format
            f = new File(line);
            
        } else {
            
            String[] arr = line.split(TAB);
            f = new File(arr[0]);
            
            String group = arr[1];
            
            w.addBioSample(group);
            
            w.getBioSample(group).addDataset(f);
            
            

        }

        if (f.exists()) {
            w.add(f);
        }
    }

}
