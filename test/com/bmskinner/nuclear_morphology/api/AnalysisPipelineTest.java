package com.bmskinner.nuclear_morphology.api;

import java.util.logging.Logger;

import org.junit.Before;

import ij.IJ;
import ij.Prefs;

public abstract class AnalysisPipelineTest {

    protected static Logger logger;

    @Before
	public void setUp(){
		Prefs.blackBackground = true;
    	IJ.setBackgroundColor(0, 0, 0);
	}

}
