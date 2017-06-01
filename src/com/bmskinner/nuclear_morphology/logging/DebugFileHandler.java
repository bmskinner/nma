/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
package com.bmskinner.nuclear_morphology.logging;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;

public class DebugFileHandler extends FileHandler {

    private static boolean APPEND = true;
    private static int     LIMIT  = 10000000;
    private static int     COUNT  = 1;

    public DebugFileHandler(File logFile) throws SecurityException, IOException {
        super(logFile.getAbsolutePath(), LIMIT, COUNT, APPEND);
    }

}
