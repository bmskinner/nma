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

import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

import no.gui.LogPanel;

public class TextAreaHandler  extends StreamHandler{

//	    JTextArea textArea = null;
	    LogPanel logPanel; // the log panel to log to
	    
	    public TextAreaHandler(LogPanel logPanel){
	    	this.logPanel = logPanel;
	    }

	    public void setTextArea(LogPanel logPanel) {
	        this.logPanel = logPanel;
	    }

	    @Override
	    public void publish(LogRecord record) {
	        super.publish(record);
	        flush();

	        if (logPanel != null) {
	        	logPanel.log(getFormatter().format(record));
	        }
	    }
	}
