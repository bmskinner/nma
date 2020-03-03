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
package com.bmskinner.nuclear_morphology.logging;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;

import org.eclipse.jdt.annotation.NonNull;

public class LogFileHandler extends FileHandler {

    private static boolean APPEND = true;
    private static int     LIMIT  = 10000000;
    private static int     COUNT  = 1;
    
    public LogFileHandler() throws SecurityException, IOException {
    	super();
    }

    public LogFileHandler(@NonNull File logFile) throws SecurityException, IOException {
        super(logFile.getAbsolutePath(), LIMIT, COUNT, APPEND);
    }
    
    public LogFileHandler(@NonNull File logFile, @NonNull Formatter formatter) throws SecurityException, IOException {
    	super(logFile.getAbsolutePath(),LIMIT, COUNT, APPEND);
    	this.setFormatter(formatter);
    }

}
