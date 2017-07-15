///*******************************************************************************
// * Copyright (C) 2017 Ben Skinner
// * 
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// * 
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// * 
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.\
// *******************************************************************************/
//
//
//package com.bmskinner.nuclear_morphology.io;
//
//import java.io.File;
//import java.io.IOException;
//import java.net.URISyntaxException;
//
//import ij.IJ;
//
//public interface Importer {
//
//    static final String SAVE_FILE_EXTENSION_NODOT = "nmd";
//    static final String SAVE_FILE_EXTENSION       = "." + SAVE_FILE_EXTENSION_NODOT;
//    static final String LOG_FILE_EXTENSION        = ".log";
//    static final String LOC_FILE_EXTENSION        = "cell";                         // locations
//                                                                                    // of
//                                                                                    // cells
//                                                                                    // (in
//                                                                                    // a
//                                                                                    // tsv
//                                                                                    // format)
//    static final String BAK_FILE_EXTENSION        = ".bak";                         // backup
//                                                                                    // files
//                                                                                    // made
//                                                                                    // in
//                                                                                    // conversions
//    static final String WRK_FILE_EXTENSION        = ".wrk";                         // workspace
//                                                                                    // files
//                                                                                    // for
//                                                                                    // multiple
//                                                                                    // nmds
//
//    static final String INVALID_FILE_ERROR       = "File is not valid for importing";
//    static final String CHANNEL_BELOW_ZERO_ERROR = "Channel cannot be less than 0";
//
//    /**
//     * Replace the old file extension in the given file and return a new file
//     * 
//     * @param f
//     * @param oldExt
//     * @param newExt
//     * @return
//     */
//    static File replaceFileExtension(File f, String oldExt, String newExt) {
//
//        if (!f.getName().endsWith(oldExt)) {
//            throw new IllegalArgumentException("Old extension not found");
//        }
//        String newFileName = f.getAbsolutePath().replace(oldExt, newExt);
//        return new File(newFileName);
//
//    }
//
//    /**
//     * Get the directory that the program is being run from
//     * 
//     * @return the program directory
//     */
//    static File getProgramDir() {
//
//        try {
//            // Get the location of the jar file
//            File dir = new File(Importer.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
//
//            // Difference in path between standalone and jar
//            if (dir.getAbsolutePath().endsWith(".jar")) {
//                dir = dir.getParentFile();
//            }
//            return dir;
//        } catch (URISyntaxException e) {
//            System.err.println("Error getting program dir");
//            e.printStackTrace();
//            IJ.log("Error getting program dir");
//            return null;
//        }
//
//    }
//
//    static boolean isSuitableImportFile(File f) {
//
//        if (f == null) {
//            return false;
//        }
//
//        if (!f.exists()) {
//            return false;
//        }
//
//        if (f.isDirectory()) {
//            return false;
//        }
//
//        if (!f.isFile()) {
//            return false;
//        }
//        return true;
//    }
//
//}
