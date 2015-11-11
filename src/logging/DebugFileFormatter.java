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

public class DebugFileFormatter extends Formatter {
	
	 @Override
	    public String format(LogRecord record) {
		 String date = calcDate(record.getMillis());
		 String log = date 
				 + "\t" 
				 + record.getLevel() 
				 + "\t" 
				 + record.getLoggerName()
				 + "\t"
				 + record.getMessage() 
				 + " in "
				 +record.getSourceMethodName()
				 + "()" 
				 + "\r\n";
		 
		 	if(record.getLevel()==Level.SEVERE){
		 		
		 		if(record.getThrown()!=null){
		 			Throwable t = record.getThrown();
		 			log += date 
	 						+ "\t" 
	 						+"STACK" 
	 						+ "\t"
	 						+ t.getMessage() 
	 						+ "\r\n";
		 			
		 			for(StackTraceElement el : t.getStackTrace()){
		 				log +=  date 
		 						+ "\t" 
		 						+"STACK" 
		 						+ "\t" 
		 						+ el.toString() 
		 						+ "\r\n";
		 			}
		 		}
		 		
		 	}
		 	return log;
	    }
	 
	 private String calcDate(long millisecs) {

		 SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss.SSS");
		 Date date = new Date(millisecs);
		 return df.format(date);
	 }

}

