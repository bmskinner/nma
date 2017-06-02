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

/**
 * Interface for all export classes. Defines file extensions.
 * 
 * @author ben
 *
 */
public interface Exporter {
    static final String TAB_FILE_EXTENSION = ".txt";
    static final String SVG_FILE_EXTENSION = ".svg";

    static final String NEWLINE = System.getProperty("line.separator");
    
    static final String TAB = "\t";
    
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
