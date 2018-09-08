package com.bmskinner.nuclear_morphology.api;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;

import com.bmskinner.nuclear_morphology.components.generic.Version;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;
import com.bmskinner.nuclear_morphology.logging.LogPanelFormatter;
import com.bmskinner.nuclear_morphology.logging.Loggable;

import ij.IJ;
import ij.Prefs;

public abstract class AnalysisPipelineTest {
	
	protected static final String IMAGE_FOLDER = "test/samples/images/";
	
    protected static final String TESTING_RODENT_FOLDER = IMAGE_FOLDER +"Testing";
    
    protected static final String TESTING_PIG_FOLDER = IMAGE_FOLDER +"Testing_pig";    
    protected static final String TESTING_ROUND_FOLDER = IMAGE_FOLDER +"Testing_round";
    
    protected static final String OUT_FOLDER = "UnitTest_"+Version.currentVersion();
    
    protected static Logger logger;
   
    
    @Before
    public void setUp(){
    	Prefs.blackBackground = true;
    	IJ.setBackgroundColor(0, 0, 0);
    	logger = Logger.getLogger(Loggable.PROGRAM_LOGGER);
    	logger.setLevel(Level.FINER);
    	logger.addHandler(new ConsoleHandler(new LogPanelFormatter()));
    }

}
