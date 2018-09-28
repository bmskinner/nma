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
	
	protected static final String IMAGE_FOLDER   = "test/samples/images/";
	protected static final String DATASET_FOLDER = "test/samples/datasets/";
	
    protected static final String TESTING_MOUSE_FOLDER = IMAGE_FOLDER + "Mouse";
    protected static final String TESTING_PIG_FOLDER   = IMAGE_FOLDER + "Pig";   
    protected static final String TESTING_ROUND_FOLDER = IMAGE_FOLDER + "Round";
    
    
    protected static final String TESTING_MOUSE_SIGNALS_FOLDER = IMAGE_FOLDER + "Mouse_with_signals";
    protected static final String TESTING_PIG_SIGNALS_FOLDER   = IMAGE_FOLDER + "Pig_with_signals";
    protected static final String TESTING_ROUND_SIGNALS_FOLDER = IMAGE_FOLDER + "Round_with_signals";
    
    protected static final String TESTING_MOUSE_CLUSTERS_FOLDER = IMAGE_FOLDER + "Mouse_with_clusters";
    protected static final String TESTING_PIG_CLUSTERS_FOLDER   = IMAGE_FOLDER + "Pig_with_clusters";
    protected static final String TESTING_ROUND_CLUSTERS_FOLDER = IMAGE_FOLDER + "Round_with_clusters";
    
    protected static final String UNIT_TEST_FILENAME = "UnitTest_"+Version.currentVersion();
    
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
