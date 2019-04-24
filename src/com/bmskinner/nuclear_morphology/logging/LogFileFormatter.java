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

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogFileFormatter extends Formatter {

    private static final String NEWLINE   = System.getProperty("line.separator");
    private static final String SEPARATOR = "\t";
    private static final String STACK     = "STACK";

    @Override
    public String format(LogRecord record) {

        StringBuffer buffer = new StringBuffer();

        /*
         * The source method name can be obscured by the log functions. Get the
         * stack trace and find the previous calling method.
         */

        String sourceMethod = record.getSourceMethodName();
        String sourceClass = record.getSourceClassName();

        if (sourceMethod.equals("log") 
                || sourceMethod.startsWith("stack") 
                || sourceMethod.startsWith("error")
                || sourceMethod.startsWith("config")) {
            // work back to the actual calling method
            // this should be before the Loggable call

            StackTraceElement[] array = Thread.currentThread().getStackTrace();
            sourceMethod = array[9].getMethodName();

            for (int i = 0; i < array.length; i++) {
                StackTraceElement e = array[i];
                if (e.getClassName().equals("com.bmskinner.nuclear_morphology.logging.Loggable")) {
                    sourceMethod = array[i + 1].getMethodName();
                    sourceClass = array[i + 1].getClassName();
                    break;
                }
            }
        }

        if (sourceMethod.equals("log")) {
            StackTraceElement[] array = Thread.currentThread().getStackTrace();
            sourceMethod = array[8].getMethodName();

        }

        String date = calcDate(record.getMillis());

        buffer.append(date)
            .append(SEPARATOR)
            .append(record.getLevel())
            .append(SEPARATOR)
            .append(sourceClass)
            .append(SEPARATOR)
            .append(sourceMethod)
            .append(SEPARATOR)
            .append(record.getMessage())
            .append(NEWLINE);

        if (record.getLevel() == Level.SEVERE || record.getLevel() == Loggable.STACK) {

            if (record.getThrown() != null) {
                Throwable t = record.getThrown();

                buffer.append(date).append(SEPARATOR).append(STACK).append(SEPARATOR).append(t.getMessage())
                        .append(NEWLINE);

                for (StackTraceElement el : t.getStackTrace()) {

                    buffer.append(date).append(SEPARATOR).append(STACK).append(SEPARATOR).append(el.toString())
                            .append(NEWLINE);
                }
            }

        }
        return buffer.toString();
    }

    private String calcDate(long millisecs) {

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");
        Date date = new Date(millisecs);
        return df.format(date);
    }

}
