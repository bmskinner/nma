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
		 			for(StackTraceElement el : t.getStackTrace()){
		 				log += "\r\n"
		 						+ date 
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

