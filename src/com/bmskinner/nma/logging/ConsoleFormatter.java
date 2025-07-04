package com.bmskinner.nma.logging;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Log format for the system console
 * 
 * @author ben
 * @since 1.16.0
 *
 */
public class ConsoleFormatter extends Formatter {

	private static final String SEPARATOR = "\t";
	private static final String NEWLINE = System.getProperty("line.separator");
	private static final String STACK = "STACK";

	@Override
	public String format(LogRecord record) {

		StringBuilder buffer = new StringBuilder();

		String date = calcDate(record.getMillis());

		String formattedMsg = formatMessage(record);

		buffer.append(date);
		buffer.append(SEPARATOR);
		buffer.append(record.getLevel());
		buffer.append(SEPARATOR);
		buffer.append(record.getMessage());

		if (record.getLevel() == Level.SEVERE || record.getLevel() == Loggable.STACK) {

			if (record.getThrown() != null) {
				Throwable t = record.getThrown();
				buffer.append(NEWLINE)
						.append(date)
						.append(SEPARATOR)
						.append(STACK)
						.append(SEPARATOR)
						.append(formattedMsg)
						.append(NEWLINE);

				// check if the throwable was rethrown; if so, get the original stack trace
				StackTraceElement[] st = t.getCause() == null ? t.getStackTrace()
						: t.getCause().getStackTrace();

				for (StackTraceElement el : st) {
					buffer.append(date).append(SEPARATOR).append(STACK).append(SEPARATOR)
							.append(el.toString())
							.append(NEWLINE);
				}
			}

		}
		return buffer.toString();
	}

	private String calcDate(long millisecs) {
		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
		Date date = new Date(millisecs);
		return df.format(date);
	}
}
