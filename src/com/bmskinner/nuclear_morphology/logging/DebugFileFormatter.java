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


public class DebugFileFormatter extends Formatter {
	
	private static final String NEWLINE = System.getProperty("line.separator");
	private static final String SEPARATOR = "\t";
	
	 @Override
	    public String format(LogRecord record) {
		 
		 StringBuffer buffer = new StringBuffer();
		 
		 /*
		  * The source method name can be obscured by the log functions.
		  * Get the stack trace and find the previous calling method.
		  */
		 
		 String sourceMethod = record.getSourceMethodName();
		 if(sourceMethod.equals("log")){
			 StackTraceElement[] array = Thread.currentThread().getStackTrace();
			 sourceMethod = array[8].getMethodName();

		 }
		 
		 String date = calcDate(record.getMillis());
		 
		 buffer.append(date);
		 buffer.append(SEPARATOR);
		 buffer.append(record.getLevel());
		 buffer.append(SEPARATOR);
		 buffer.append(record.getLoggerName());
		 buffer.append(SEPARATOR);
		 buffer.append(sourceMethod);
		 buffer.append(SEPARATOR);
		 buffer.append(record.getMessage());
		 buffer.append(NEWLINE);
		 		 
		 	if(record.getLevel()==Level.SEVERE){
		 		
		 		if(record.getThrown()!=null){
		 			Throwable t = record.getThrown();
		 			
		 			buffer.append(date);
		 			buffer.append(SEPARATOR);
		 			buffer.append("STACK");
		 			buffer.append(SEPARATOR);
		 			buffer.append(t.getMessage());
		 			buffer.append(NEWLINE);
		 			
		 			for(StackTraceElement el : t.getStackTrace()){
		 				
		 				buffer.append(date);
			 			buffer.append(SEPARATOR);
			 			buffer.append("STACK");
			 			buffer.append(SEPARATOR);
			 			buffer.append(el.toString() );
			 			buffer.append(NEWLINE);
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

