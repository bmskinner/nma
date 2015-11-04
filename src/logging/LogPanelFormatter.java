package logging;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogPanelFormatter extends Formatter {
	
	 @Override
	    public String format(LogRecord record) {
		 String date = calcDate(record.getMillis());
		 String log = date + " " + record.getMessage() + "\r\n";
		 
		 	if(record.getLevel()==Level.SEVERE){
		 		
		 		if(record.getThrown()!=null){
		 			Throwable t = record.getThrown();

		 			log += t.getClass().getSimpleName() + ": " + t.getMessage() + "\r\n";
		 			
		 			for(StackTraceElement el : t.getStackTrace()){
		 				log += el.toString() + "\r\n";
		 			}
		 			log += "\r\n";
		 		}
		 		
		 	}
		 	
		 	return log;
	    }
	 
	 private String calcDate(long millisecs) {

		 SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
		 Date date = new Date(millisecs);
		 return df.format(date);
	 }

}
