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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import com.bmskinner.nuclear_morphology.analysis.IWorkspace;
import com.bmskinner.nuclear_morphology.logging.Loggable;

/**
 * Saves a workspace to a *.wrk file
 * 
 * @author ben
 * @since 1.13.3
 *
 */
public class WorkspaceExporter implements Loggable, Exporter {

    private static final String NEWLINE = System.getProperty("line.separator");
    final IWorkspace            w;

    public WorkspaceExporter(final IWorkspace w) {

        this.w = w;
    }

    public void export() {

        File exportFile = w.getSaveFile();

        if (exportFile.exists()) {
            exportFile.delete();
        }

        StringBuilder builder = new StringBuilder();

        /*
         * Add the save paths of nmds
         */
        for (File f : w.getFiles()) {
            builder.append(f.getAbsolutePath());
            builder.append(NEWLINE);
        }

        try {

            PrintWriter out;
            out = new PrintWriter(exportFile);
            out.print(builder.toString());
            out.close();

        } catch (FileNotFoundException e) {
            warn("Cannot export workspace file");
            fine("Error writing file", e);
        }
    }

}
