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


package com.bmskinner.nuclear_morphology.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

import ij.IJ;

/**
 * This interface provides default methods for logging to the program log panel.
 * 
 * @author bms41
 *
 */
public interface Loggable {

    public static final String ROOT_LOGGER    = "";
    public static final String PROGRAM_LOGGER = "ProgramLogger";
    public static final String ERROR_LOGGER   = "ErrorLogger";

    public static final Level TRACE = new ErrorLevel();

    /**
     * The TRACE error level has a level value of 600, so will display ahead of
     * FINE. It is used for reporting errors whilst hiding uninformative
     * messages from users generally.
     * 
     * @author bms41
     * @since 1.13.3
     *
     */
    @SuppressWarnings("serial")
    public class ErrorLevel extends Level {
        public ErrorLevel() {
            super("ERROR", 600);
        }
    }

    /**
     * Log the given message to the program log window.
     * 
     * @param level
     *            the log level
     * @param message
     *            the message to log
     */
    default void log(Level level, String message) {
        // Logger l = Logger.getLogger( this.getClass().getName());
        // l.setUseParentHandlers(true);
        // l.log(level, message); // will be passed upwards to root logger
        Logger.getLogger(PROGRAM_LOGGER).log(level, message);
//        System.out.println(message);
    }

    /**
     * Log an error to the program log window and to the error log file with
     * Level.SEVERE
     * 
     * @param message
     *            the error messsage
     * @param t
     *            the exception
     */
    default void error(String message, Throwable t) {
        Logger.getLogger(PROGRAM_LOGGER).log(Level.SEVERE, message, t);
        Logger.getLogger(ERROR_LOGGER).log(Level.SEVERE, message, t);
//        System.err.println(message);
        t.printStackTrace();
    }

    /**
     * Log an error to the program log window and to the error log file with a
     * stack trace. The TRACE error level has a level value of 600, so will
     * display ahead of FINE.
     * 
     * @param message
     *            the error messsage
     * @param t
     *            the exception
     */
    default void stack(String message, Throwable t) {
        Logger.getLogger(PROGRAM_LOGGER).log(TRACE, message, t);
        Logger.getLogger(ERROR_LOGGER).log(TRACE, message, t);
//        System.err.println(message);
//        t.printStackTrace();
    }

    /**
     * Log an error to the program log window and to the error log file with a
     * stack trace. The TRACE error level has a level value of 600, so will
     * display ahead of FINE.
     * 
     * @param message
     *            the error messsage
     * @param t
     *            the exception
     */
    default void stack(Throwable t) {
        stack(t.getMessage(), t);
    }

    /**
     * Log a message to the program log window with Level.FINE
     * 
     * @param message
     *            the messsage
     */
    default void fine(String message) {
        Logger.getLogger(PROGRAM_LOGGER).log(Level.FINE, message);
//        System.out.println(message);
    }

    /**
     * Log an error to the program log window with Level.FINE Use to show stack
     * traces when debugging.
     * 
     * @param message the error message
     * @param t the throwable
     */
    default void fine(String message, Throwable t) {
        Logger.getLogger(PROGRAM_LOGGER).log(TRACE, message, t);
//        System.out.println(message);
    }

    /**
     * Log a message to the program log window with Level.FINER
     * 
     * @param message
     *            the error messsage
     */
    default void finer(String message) {
        Logger.getLogger(PROGRAM_LOGGER).log(Level.FINER, message);
        // System.out.println(message);
    }

    /**
     * Log a message to the program log window with Level.FINEST
     * 
     * @param message
     *            the error messsage
     */
    default void finest(String message) {
        Logger.getLogger(PROGRAM_LOGGER).log(Level.FINEST, message);
    }

    /**
     * Log an error to the program log window with Level.WARNING
     * 
     * @param message
     *            the error messsage
     */
    default void warn(String message) {
        Logger.getLogger(PROGRAM_LOGGER).log(Level.WARNING, message);
//        System.err.println(message);
    }

    /**
     * Log a message to the program log window with Level.INFO
     * 
     * @param message
     *            the error messsage
     */
    default void log(String message) {
        log(Level.INFO, message);
    }

    /**
     * Log a message to the program log window and to the dataset debug file.
     * 
     * @param level
     *            the logging level
     * @param message
     *            the error messsage
     * @param t
     *            the exception
     */
    default void log(Level level, String message, Throwable t) {
        Logger.getLogger(PROGRAM_LOGGER).log(level, message, t);
//        System.err.println(message);
        t.printStackTrace();
    }

    /**
     * Log the given message and srack trace from the given throwable to the
     * ImageJ log window. Only use if the program log panel is not expected to
     * be available.
     * 
     * @param message
     * @param t
     */
    default void logToImageJ(String message, Throwable t) {
        IJ.log(message);
        IJ.log(t.getMessage());
        for (StackTraceElement el : t.getStackTrace()) {
            IJ.log(el.toString());
        }
    }

    /**
     * Log the given message to the ImageJ log window. Only use if the program
     * log panel is not expected to be available.
     * 
     * @param message
     */
    default void logIJ(String message) {
        IJ.log(message);
        System.err.println(message);
    }
}
