package com.bmskinner.nuclear_morphology.api;

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
    	Prefs.blackBackground = true;
    	IJ.setBackgroundColor(0, 0, 0);
    	logger = Logger.getLogger(Loggable.PROGRAM_LOGGER);
    	logger.setLevel(Level.FINE);
    	logger.addHandler(new ConsoleHandler(new LogPanelFormatter()));
    }

}
