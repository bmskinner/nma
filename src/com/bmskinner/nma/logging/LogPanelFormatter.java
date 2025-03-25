/*******************************************************************************
 * Copyright (C) 2018 Ben Skinner
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.bmskinner.nma.logging;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Format log records for display in the log panel
 * 
 * @author Ben Skinner
 *
 */
public class LogPanelFormatter extends Formatter {

	private static final String NEWLINE = System.getProperty("line.separator");

	/**
	 * The log panel controls the paragraph indent for the text. We don't need to
	 * use a tab.
	 */
	private static final String SEPARATOR = " ";

	@Override
	public String format(LogRecord record) {

		StringBuilder buffer = new StringBuilder();

		String date = calcDate(record.getMillis());

		String formattedMsg = formatMessage(record);

		buffer.append(date);
		buffer.append(SEPARATOR);
		buffer.append(formattedMsg);
		return buffer.toString();
	}

	private String calcDate(long millisecs) {

		SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
		Date date = new Date(millisecs);
		return df.format(date);
	}

}
