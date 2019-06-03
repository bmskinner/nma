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

import java.util.logging.Level;

import ij.IJ;

/**
 * This class provides static methods for logging to the ImageJ
 * window
 * 
 * @author bms41
 *
 */
public class Loggable {
	
	private Loggable() {
		// private
	}

    public static final Level STACK = new StackLevel();
    
    /**
     * The STACK error level has a level value of 600, so will display ahead of
     * FINE. It is used for reporting errors whilst hiding uninformative
     * messages from users generally.
     * 
     * @author bms41
     * @since 1.13.3
     *
     */
    @SuppressWarnings("serial")
    public static class StackLevel extends Level {
        public StackLevel() {
            super("ERROR", 600);
        }
    }
    
    /**
     * Log the given message and srack trace from the given throwable to the
     * ImageJ log window. Only use if the program log panel  or console is not
     *  expected to be available.
     * 
     * @param message
     * @param t
     */
    public static void logToImageJ(String message, Throwable t) {
        IJ.log(message);
        IJ.log(t.getMessage());
        for (StackTraceElement el : t.getStackTrace()) {
            IJ.log(el.toString());
        }
    }

    /**
     * Log the given message to the ImageJ log window. Only use if the program
     * log panel or console is not expected to be available.
     * 
     * @param message
     */
    public static void logToImageJ(String message) {
        IJ.log(message);
        System.err.println(message);
    }
}
