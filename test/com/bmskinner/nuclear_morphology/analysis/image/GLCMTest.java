package com.bmskinner.nuclear_morphology.analysis.image;

import static org.junit.Assert.assertFalse;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.image.GLCM.GLCMImage;
import com.bmskinner.nuclear_morphology.analysis.image.GLCM.GLCMResult;
import com.bmskinner.nuclear_morphology.analysis.image.GLCM.GLCMValue;
import com.bmskinner.nuclear_morphology.charting.ImageViewer;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.io.Io;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;
import com.bmskinner.nuclear_morphology.logging.ConsoleFormatter;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;

import ij.gui.Roi;
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

	@Test
	public void testRunningOnComponent() throws Exception {
		GLCM glcm = new GLCM();
		
		IAnalysisDataset d = SampleDatasetReader.openTestRodentDataset();
		
		Nucleus n = d.getCollection().stream().findFirst().get().getNucleus();
		
		Roi roi = n.toRoi();
		roi.setLocation(Imageable.COMPONENT_BUFFER, Imageable.COMPONENT_BUFFER);
		ImageProcessor ip = n.getComponentImage().convertToByte(false);
		GLCMResult result1 = glcm.calculate(ip);
		System.out.println(result1.toString());
		ip.setLineWidth(2);
		ip.setColor(Color.GRAY);
		ip.draw(roi);
//		ImageViewer.showImage(ip, "input");

		GLCMResult result = glcm.calculate(n);
		
		assertFalse(result.toString().equals(result1.toString()));
		
		System.out.println(result.toString());
	}
	
	@Test
	public void testRunningOnDataset() throws Exception {
		GLCM glcm = new GLCM();
		
		IAnalysisDataset d = SampleDatasetReader.openTestRodentDataset();
		
		List<GLCMResult> results = new ArrayList<>();
		for(ICell c : d.getCollection()) {
			for(Nucleus n : c.getNuclei()) {
				results.add(glcm.calculate(n));
			}
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append("ID"+Io.TAB);
		for(GLCMValue v : GLCMValue.values())
			builder.append(v.toString()+Io.TAB);
		builder.append(Io.NEWLINE);
		
		for(GLCMResult r : results) {
			builder.append(r.getIdentifier()+Io.TAB);
			for(GLCMValue v : GLCMValue.values())
				builder.append(r.get(v)+Io.TAB);
			builder.append(Io.NEWLINE);
		}
		System.out.println(builder.toString());
	}
}
