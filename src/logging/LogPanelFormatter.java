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
package logging;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogPanelFormatter extends Formatter {
	
	private static final String NEWLINE = System.getProperty("line.separator");
	
	private static final String SEPARATOR = " | ";
	
	
	@Override
	public String format(LogRecord record) {
		
		StringBuffer buffer = new StringBuffer();

		String date = calcDate(record.getMillis());

		if(record.getLevel()==Level.FINE || record.getLevel()==Level.FINER || record.getLevel()==Level.FINEST){
			
			buffer.append(date + " " + formatFinest(record));
		} else {
			buffer.append(date + " " + record.getMessage() + NEWLINE);
		}

		if(record.getThrown()!=null){
			Throwable t = record.getThrown();

			buffer.append( t.getClass().getSimpleName() + ": " + t.getMessage() + NEWLINE  ) ;

			for(StackTraceElement el : t.getStackTrace()){
				buffer.append( el.toString() + NEWLINE );
			}
			buffer.append( NEWLINE );
		}



		return buffer.toString();
	}
	
	private String formatFinest(LogRecord record){
		
		String sourceMethod = record.getSourceMethodName();
		if(sourceMethod.equals("log")){
			StackTraceElement[] array = Thread.currentThread().getStackTrace();
			sourceMethod = array[8].getMethodName();
		}

		StringBuffer buffer = new StringBuffer();

		buffer.append(record.getMessage());
		buffer.append(SEPARATOR);
		buffer.append(record.getSourceClassName());
		buffer.append(SEPARATOR);
		buffer.append(sourceMethod);
		buffer.append(SEPARATOR);
		buffer.append(record.getThreadID());
		buffer.append(NEWLINE);

		return buffer.toString();
	}
	
	 
	 private String calcDate(long millisecs) {

		 SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
		 Date date = new Date(millisecs);
		 return df.format(date);
	 }

}
