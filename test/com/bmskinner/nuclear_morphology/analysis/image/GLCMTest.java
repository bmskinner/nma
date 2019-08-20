package com.bmskinner.nuclear_morphology.analysis.image;

import static org.junit.Assert.*;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.image.GLCM.GLCMImage;
import com.bmskinner.nuclear_morphology.analysis.image.GLCM.GLCMValue;
import com.bmskinner.nuclear_morphology.charting.ImageViewer;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;
import com.bmskinner.nuclear_morphology.logging.ConsoleFormatter;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;

import ij.ImagePlus;
import ij.process.ImageProcessor;

public class GLCMTest {
	
	private static final Logger LOGGER = Logger.getGlobal();

	@Before
	public void setUp() throws Exception {
		for(Handler h : LOGGER.getHandlers())
			LOGGER.removeHandler(h);
		Handler h = new ConsoleHandler(new ConsoleFormatter());
		LOGGER.setLevel(Level.FINER);
		h.setLevel(Level.FINER);
		LOGGER.addHandler(h);
	}
	
	

	@Test
	public void test() throws Exception {
		GLCM glcm = new GLCM();
		
		IAnalysisDataset d = SampleDatasetReader.openTestRodentDataset();
		
		Nucleus n = d.getCollection().stream().findFirst().get().getNucleus();
		
		ImageProcessor ip = n.getComponentImage().convertToByte(false);
		GLCMImage result = glcm.calculate(ip, 25);
		
		System.out.println(result.toString(GLCMValue.IDM));
//		ImageViewer.showImage(ip, "input");
		ImageViewer.showImage(result.toStack(), "output");
	}

}
