package com.bmskinner.nuclear_morphology.logging;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Log format for the system console
 * @author ben
 * @since 1.16.0
 *
 */
public class ConsoleFormatter extends Formatter {

    private static final String SEPARATOR = "\t";

    @Override
    public String format(LogRecord record) {

        StringBuilder buffer = new StringBuilder();

        String date = calcDate(record.getMillis());

        buffer.append(date);
        buffer.append("  "); // no need for a full tab, constant width
        buffer.append(record.getLevel());
        buffer.append(SEPARATOR);
        buffer.append(record.getMessage());
        return buffer.toString();
    }

    private String calcDate(long millisecs) {
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date(millisecs);
        return df.format(date);
    }
}
