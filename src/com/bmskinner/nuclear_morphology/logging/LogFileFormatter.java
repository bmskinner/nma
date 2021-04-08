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
        
        StringBuilder buffer = new StringBuilder();

    	String date = calcDate(record.getMillis());
    	
    	buffer.append(date);
    	buffer.append(SEPARATOR);
    	buffer.append(record.getLevel());
    	buffer.append(SEPARATOR);
    	buffer.append(record.getSourceMethodName());
    	buffer.append(SEPARATOR);
    	buffer.append(record.getSourceClassName());
    	buffer.append(SEPARATOR);
    	buffer.append(record.getMessage());
    	buffer.append(NEWLINE);

    	if (record.getLevel() == Level.SEVERE || record.getLevel() == Loggable.STACK) {

    		if (record.getThrown() != null) {
    			Throwable t = record.getThrown();

    			buffer.append(date)
    			.append(SEPARATOR)
    			.append(STACK)
    			.append(SEPARATOR)
    			.append(t.getMessage())
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
