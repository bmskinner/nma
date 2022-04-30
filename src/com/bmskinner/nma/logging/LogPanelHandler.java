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

import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

import com.bmskinner.nma.gui.LogPanel;

/**
 * Handle publishing of log messages to the log panel
 * @author bms41
 *
 */
public class LogPanelHandler extends StreamHandler {

    private final LogPanel logPanel; // the log panel to log to

    public LogPanelHandler(LogPanel logPanel) {
        this.logPanel = logPanel;
    }

    @Override
    public void publish(LogRecord record) {
        super.publish(record);
        if(logPanel == null)
        	return;
        flush();
        
        if(record.getLevel().intValue()>=this.getLevel().intValue()) {
        	 logPanel.println(getFormatter().format(record));
        }
    }
}
