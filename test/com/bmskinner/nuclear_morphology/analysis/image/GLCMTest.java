package com.bmskinner.nuclear_morphology.analysis.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.analysis.image.GLCM.GLCMTilePath;
import com.bmskinner.nuclear_morphology.analysis.image.GLCM.GLCMTile;
import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.analysis.image.GLCM.GLCMParameter;
import com.bmskinner.nuclear_morphology.charting.ImageViewer;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.ICell;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.stats.GenericStatistic;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.components.stats.StatisticDimension;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
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
		GLCMTilePath result = glcm.calculate(ip, 25);
		
		System.out.println(result.toString(GLCMParameter.IDM));
//		ImageViewer.showImage(ip, "input");
		ImageViewer.showImage(result.toStack(), "output");
	}

	@Test
	public void testRunningOnImage() throws Exception {
		File f = new File(TestResources.GLCM_SAMPLE_IMAGE);
		ImageProcessor ip = new ImageImporter(f).importImage(ImageImporter.RGB_BLUE);
		
		// DEfault parameters
		GLCM glcm = new GLCM();
		GLCMTile result = glcm.calculate(ip);
		
		// Compare to known values for this image
		assertEquals("ASM", 0.0015, result.get(GLCMParameter.ASM), 0.01);
		assertEquals("IDM", 0.2351, result.get(GLCMParameter.IDM), 0.01);
		assertEquals("Contrast", 77.780, result.get(GLCMParameter.CONSTRAST), 0.01);
		assertEquals("Energy", 0.0015, result.get(GLCMParameter.ENERGY), 0.01);
		assertEquals("Entropy", 7.3240, result.get(GLCMParameter.ENTROPY), 0.01);
		assertEquals("Homogeneity", 0.3201, result.get(GLCMParameter.HOMOGENEITY), 0.01);
		assertEquals("Variance", 5005.8566, result.get(GLCMParameter.VARIANCE), 0.01);
		assertEquals("Shade", -3380213.3374, result.get(GLCMParameter.SHADE), 0.01);
		assertEquals("Prominence", 1133415570.7007, result.get(GLCMParameter.PROMINENCE), 0.01);
		assertEquals("Inertia", 77.7480, result.get(GLCMParameter.INERTIA), 0.01);
		assertEquals("Correlation", 0.0002, result.get(GLCMParameter.CORRELATION), 0.01);
		assertEquals("Sum", 1.000, result.get(GLCMParameter.SUM), 0.01);
		
		
		
		
	}
	
	@Test
	public void testRunningOnComponent() throws Exception {
		GLCM glcm = new GLCM();
		
		IAnalysisDataset d = SampleDatasetReader.openTestRodentDataset();
		
		Nucleus n = d.getCollection().stream().findFirst().get().getNucleus();
		
		Roi roi = n.toRoi();
		roi.setLocation(Imageable.COMPONENT_BUFFER, Imageable.COMPONENT_BUFFER);
		ImageProcessor ip = n.getComponentImage().convertToByte(false);
		GLCMTile result1 = glcm.calculate(ip);
		System.out.println(result1.toString());
		ip.setLineWidth(2);
		ip.setColor(Color.GRAY);
		ip.draw(roi);
//		ImageViewer.showImage(ip, "input");

		GLCMTile result = glcm.calculate(n);
		
		assertEquals("Sum should be 1", 1d, result1.get(GLCMParameter.SUM), 0.01);
		assertFalse("Nucleus specific GLCM should not be identical to whole image GLCM",result.toString().equals(result1.toString()));
		
		
		System.out.println(result.toString());
	}
	
	@Test
	public void testRunningOnDataset() throws Exception {
		GLCM glcm = new GLCM();
		
		IAnalysisDataset d = SampleDatasetReader.openTestRodentDataset();
		
		List<GLCMTile> results = new ArrayList<>();
		for(ICell c : d.getCollection()) {
			for(Nucleus n : c.getNuclei()) {
				GLCMTile r = glcm.calculate(n);
				results.add(r);
				for(GLCMParameter v : GLCMParameter.values())
					n.setStatistic(v.toStat(), r.get(v));
			}
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append("ID"+Io.TAB);
		for(GLCMParameter v : GLCMParameter.values())
			builder.append(v.toString()+Io.TAB);
		builder.append(Io.NEWLINE);
		
		for(GLCMTile r : results) {
			builder.append(r.getIdentifier()+Io.TAB);
			for(GLCMParameter v : GLCMParameter.values())
				builder.append(r.get(v)+Io.TAB);
			builder.append(Io.NEWLINE);
		}
		System.out.println(builder.toString());
	}
}
