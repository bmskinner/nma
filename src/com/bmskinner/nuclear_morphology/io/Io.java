/*******************************************************************************
 *      Copyright (C) 2016 Ben Skinner
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

package com.bmskinner.nuclear_morphology.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URISyntaxException;

import ij.IJ;

public interface Io {
    
    static final String TAB_FILE_EXTENSION = ".txt";
    static final String SVG_FILE_EXTENSION = ".svg";

    static final String NEWLINE = System.getProperty("line.separator");
    
    static final String TAB = "\t";
    
    static final String SAVE_FILE_EXTENSION_NODOT = "nmd";
    static final String SAVE_FILE_EXTENSION       = "." + SAVE_FILE_EXTENSION_NODOT;
    static final String LOG_FILE_EXTENSION        = ".log";
    
    
    /**
     * Locations of saved cells
     */
    static final String LOC_FILE_EXTENSION        = "cell";
    
    /**
     * Backup files made in nmd conversions
     */
    static final String BAK_FILE_EXTENSION        = ".bak";
    
    /**
     * Workspace file extension
     */
    static final String WRK_FILE_EXTENSION        = ".wrk";

    static final String INVALID_FILE_ERROR       = "File is not valid for importing";
    static final String CHANNEL_BELOW_ZERO_ERROR = "Channel cannot be less than 0";
    
    /**
     * Interface for all export classes. Defines file extensions.
     * 
     * @author ben
     *
     */
    public interface Exporter extends Io {
        
        
        static void writeString(final String s, final File f){

            if(f==null){
                throw new IllegalArgumentException("File cannot be null");
            }
            
            try (PrintWriter out = new PrintWriter(f)){

                out.println(s);

            } catch (FileNotFoundException e) {
                return;
            }
        }
    }
    
    public interface Importer extends Io {

        /**
         * Replace the old file extension in the given file and return a new file
         * 
         * @param f
         * @param oldExt
         * @param newExt
         * @return
         */
        static File replaceFileExtension(final File f, final String oldExt, final String newExt) {

            if (!f.getName().endsWith(oldExt)) {
                throw new IllegalArgumentException("Old extension not found");
            }
            String newFileName = f.getAbsolutePath().replace(oldExt, newExt);
            return new File(newFileName);

        }

        /**
         * Get the directory that the program is being run from
         * 
         * @return the program directory
         */
        static File getProgramDir() {

            try {
                // Get the location of the jar file
                File dir = new File(Importer.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());

                // Difference in path between standalone and jar
                if (dir.getAbsolutePath().endsWith(".jar")) {
                    dir = dir.getParentFile();
                }
                return dir;
            } catch (URISyntaxException e) {
                System.err.println("Error getting program dir");
                e.printStackTrace();
                IJ.log("Error getting program dir");
                return null;
            }

        }

        /**
         * Test if the given file can be imported
         * @param f
         * @return
         */
        static boolean isSuitableImportFile(final File f) {

            if (f == null) {
                return false;
            }

            if (!f.exists()) {
                return false;
            }

            if (f.isDirectory()) {
                return false;
            }

            if (!f.isFile()) {
                return false;
            }
            return true;
        }
    }

}
