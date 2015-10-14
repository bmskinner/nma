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
package utility;

import ij.IJ;

import java.io.File;
import java.sql.Date;
import java.text.SimpleDateFormat;

public class Logger {

	public static final String ERROR = "ERROR";
	public static final String DEBUG = "DEBUG";
	public static final String INFO  = "INFO";
	public static final String STACK  = "STACK";
	
	private File file;
	private String className;
	
	public Logger(File f, String c){
		this.file = f;
		this.className = c;
	}
	
	/**
	 * Log a message with a given priority level
	 * @param s the message
	 * @param priority 
	 */
	public void log(String s, String priority){
		long time = System.currentTimeMillis();
		String ts = calcDate(time);
		String result = IJ.append(ts+"\t"+priority+"\t"+this.className+"\t"+s+"\r", file.getAbsolutePath());
		if(result!=null){
			IJ.log("Error in logging: "+file.getAbsolutePath());
		}
	}

	/**
	 * Log a message with assumption of info priority
	 * @param s the message
	 */
	public void log(String s){
		log(s, Logger.INFO);
	}
	
	/**
	 * Log an exception message with the corresponding stack trace
	 * @param message a message describing the error
	 * @param e the exception that occurred
	 */
	public void error(String message, Exception e){
		log(message+": "+e.getMessage(), Logger.ERROR);
		for(StackTraceElement e1 : e.getStackTrace()){
			log(e1.toString(), Logger.STACK);
		}
	}
	
	public File getLogfile(){
		return this.file;
	}
	
	private String calcDate(long millisecs) {

	    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");
	    Date date = new Date(millisecs);
	    return df.format(date);
	  }

}
