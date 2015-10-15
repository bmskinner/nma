package logging;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogPanelFormatter extends Formatter {
	
	 @Override
	    public String format(LogRecord record) {
	        return record.getLevel() + ":" + record.getMessage();
	    }

}
