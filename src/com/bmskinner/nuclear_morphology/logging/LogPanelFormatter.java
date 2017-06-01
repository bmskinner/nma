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

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogPanelFormatter extends Formatter {

    private static final String NEWLINE = System.getProperty("line.separator");

    private static final String SEPARATOR = " | ";

    private static final String INFO_LEVEL_LBL = "log";
    private static final String FINE_LEVEL_LBL = "fine";

    @Override
    public String format(LogRecord record) {

        StringBuffer buffer = new StringBuffer();

        String date = calcDate(record.getMillis());

        buffer.append(date);
        buffer.append(" ");

        if (record.getLevel() == Level.FINE || record.getLevel() == Level.FINER || record.getLevel() == Level.FINEST) {

            buffer.append(formatFinest(record));
        } else {
            buffer.append(record.getMessage());
        }

        if (record.getThrown() != null) {
            Throwable t = record.getThrown();
            buffer.append(" ");
            buffer.append(t.getClass().getSimpleName());
            buffer.append(": ");
            buffer.append(t.getMessage());
            buffer.append(NEWLINE);

            for (StackTraceElement el : t.getStackTrace()) {
                buffer.append(el.toString());
                buffer.append(NEWLINE);
            }
        }

        buffer.append(NEWLINE);

        return buffer.toString();
    }

    private String formatFinest(LogRecord record) {

        String sourceMethod = record.getSourceMethodName();
        String sourceClass = record.getSourceClassName();

        if (sourceMethod.equals(INFO_LEVEL_LBL) || sourceMethod.startsWith(FINE_LEVEL_LBL)) {
            // work back to the actual calling method
            // this should be before the Loggable call

            StackTraceElement[] array = Thread.currentThread().getStackTrace();

            // find the array index with the source method
            boolean useLine = false;
            int useIndex = 0;
            for (int i = 0; i < array.length; i++) {
                if (useLine) {
                    continue;
                }
                StackTraceElement e = array[i];
                if (e.getClassName().equals("com.bmskinner.nuclear_morphology.logging.Loggable")) {
                    useIndex = i;
                    useLine = true;
                }

            }

            sourceMethod = array[useIndex + 1].getMethodName();
            sourceClass = array[useIndex + 1].getClassName();
            // for(int i=0; i< array.length; i++){
            // StackTraceElement e = array[i];
            // if(e.getClassName().equals("com.bmskinner.nuclear_morphology.logging.Loggable")){
            // sourceMethod = array[i+1].getMethodName();
            // sourceClass = array[i+1].getClassName();
            // break;
            // }
            // }

        }

        StringBuffer buffer = new StringBuffer();

        buffer.append(record.getMessage());
        buffer.append(SEPARATOR);
        buffer.append(sourceClass);
        buffer.append(SEPARATOR);
        buffer.append(sourceMethod);
        buffer.append(SEPARATOR);
        buffer.append(record.getThreadID());

        return buffer.toString();
    }

    private String calcDate(long millisecs) {

        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date(millisecs);
        return df.format(date);
    }

}
