package com.bmskinner.nuclear_morphology.api;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;

import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.IJ;
import ij.Prefs;

public abstract class AnalysisPipelineTest {

    protected static Logger logger;

    @Before
	public void setUp(){
		logger = Logger.getLogger(Loggable.ROOT_LOGGER);
		logger.setLevel(Level.FINE);

		boolean hasHandler = false;
		for(Handler h : logger.getHandlers()) {
			if(h instanceof ConsoleHandler)
				hasHandler = true;
		}
		if(!hasHandler)
			logger.addHandler(new ConsoleHandler(new LogPanelFormatter()));
		Prefs.blackBackground = true;
    	IJ.setBackgroundColor(0, 0, 0);
	}

}
