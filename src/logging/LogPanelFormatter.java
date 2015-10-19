package logging;

import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogPanelFormatter extends Formatter {
	
	 @Override
	    public String format(LogRecord record) {
		 String log = record.getMessage() + "\r\n";
		 
		 	if(record.getLevel()==Level.SEVERE){
		 		
		 		if(record.getThrown()!=null){
		 			Throwable t = record.getThrown();
		 			
		 			for(StackTraceElement el : t.getStackTrace()){
		 				log += el.toString() + "\r\n";
		 			}
		 			log += "\r\n";
		 		}
		 		
		 	}
		 	
		 	return log;
	    }

}
