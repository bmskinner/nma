package com.bmskinner.nuclear_morphology.analysis.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestResources;
import com.bmskinner.nuclear_morphology.analysis.image.GLCM.GLCMParameter;
import com.bmskinner.nuclear_morphology.analysis.image.GLCM.GLCMStepAngle;
import com.bmskinner.nuclear_morphology.analysis.image.GLCM.GLCMTile;
import com.bmskinner.nuclear_morphology.analysis.image.GLCM.GLCMTilePath;
import com.bmskinner.nuclear_morphology.charting.ImageViewer;
import com.bmskinner.nuclear_morphology.components.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.Imageable;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.io.ImageImporter;
import com.bmskinner.nuclear_morphology.io.SampleDatasetReader;
import com.bmskinner.nuclear_morphology.logging.ConsoleFormatter;
import com.bmskinner.nuclear_morphology.logging.ConsoleHandler;

import ij.gui.Roi;
import ij.process.ImageProcessor;

/**
 * Tests to confirm the GLCM generates the same results
 * as the ImageJ plugin
 * @author ben
 *
 */
public class GLCMTest {
	
	private static final Logger LOGGER = Logger.getLogger(GLCMTest.class.getName());

	@Before
	public void setUp() throws Exception {
		for(Handler h : LOGGER.getHandlers())
			LOGGER.removeHandler(h);
		Handler h = new ConsoleHandler(new ConsoleFormatter());
		LOGGER.setLevel(Level.FINER);
		h.setLevel(Level.FINER);
		LOGGER.addHandler(h);
	}
	
	

//	@Test
//	public void testRunningTilePath() throws Exception {
//		GLCM glcm = new GLCM();
//		
//		IAnalysisDataset d = SampleDatasetReader.openTestRodentDataset();
//		
//		Nucleus n = d.getCollection().stream().findFirst().get().getNucleus();
//		
//		ImageProcessor ip = n.getComponentImage().convertToByte(false);
//		GLCMTilePath result = glcm.calculate(ip, 25);
//
////		ImageViewer.showImage(ip, "input");
////		ImageViewer.showImage(result.toStack(), "output");
//	}

	@Test
	public void testRunningOnImageWithDefaultOptions() throws Exception {
		File f = new File(TestResources.GLCM_SAMPLE_IMAGE);
		ImageProcessor ip = new ImageImporter(f).importImage(ImageImporter.RGB_BLUE);
		
		// Default parameters
		GLCM glcm = new GLCM();
		GLCMTile result = glcm.calculate(ip);

//		 Compare to known values for this image
		assertEquals("ASM", 0.0015, result.get(GLCMParameter.ASM), 0.001);
		assertEquals("IDM", 0.2351, result.get(GLCMParameter.IDM), 0.001);
		assertEquals("Contrast", 77.7480, result.get(GLCMParameter.CONSTRAST), 0.001);
		assertEquals("Energy", 0.0015, result.get(GLCMParameter.ENERGY), 0.001);
		assertEquals("Entropy", 7.3240, result.get(GLCMParameter.ENTROPY), 0.001);
		assertEquals("Homogeneity", 0.3201, result.get(GLCMParameter.HOMOGENEITY), 0.001);
		assertEquals("Variance", 5005.8566, result.get(GLCMParameter.VARIANCE), 0.001);
		
		
		// TODO: why is this a negated version of the true value?
//		assertEquals("Shade", -3380213.3374, result.get(GLCMParameter.SHADE), 0.001);
		assertEquals("Prominence", 1133415570.7007, result.get(GLCMParameter.PROMINENCE), 0.001);
		assertEquals("Inertia", 77.7480, result.get(GLCMParameter.INERTIA), 0.001);
		assertEquals("Correlation", 0.0002, result.get(GLCMParameter.CORRELATION), 0.001);
		assertEquals("Sum", 1.0000, result.get(GLCMParameter.SUM), 0.01);
	}
	
	@Test
	public void testRunningOnImageWithNorthEastStepAngle() throws Exception {
		HashOptions options = GLCM.defaultOptions();
		options.setString(GLCM.ANGLE_KEY, GLCMStepAngle.NORTHEAST.toString());
		
		File f = new File(TestResources.GLCM_SAMPLE_IMAGE);
		ImageProcessor ip = new ImageImporter(f).importImage(ImageImporter.RGB_BLUE);
		
		GLCM glcm = new GLCM(options);
		GLCMTile result = glcm.calculate(ip);
		
//		 Compare to known values for this image
		assertEquals("ASM", 0.0011, result.get(GLCMParameter.ASM), 0.001);
		assertEquals("IDM", 0.1781, result.get(GLCMParameter.IDM), 0.001);
		assertEquals("Contrast", 183.3325, result.get(GLCMParameter.CONSTRAST), 0.001);
		assertEquals("Energy", 0.0011, result.get(GLCMParameter.ENERGY), 0.001);
		assertEquals("Entropy", 7.5597, result.get(GLCMParameter.ENTROPY), 0.001);
		assertEquals("Homogeneity", 0.2640, result.get(GLCMParameter.HOMOGENEITY), 0.001);
		assertEquals("Variance", 5040.1112, result.get(GLCMParameter.VARIANCE), 0.001);
		
		// TODO: why is this a negated version of the true value?
//		assertEquals("Shade", -3330169.2452, result.get(GLCMParameter.SHADE), 0.001);
		assertEquals("Prominence", 1114246303.8456, result.get(GLCMParameter.PROMINENCE), 0.001);
		assertEquals("Inertia", 183.3325, result.get(GLCMParameter.INERTIA), 0.001);
		assertEquals("Correlation", 0.0002, result.get(GLCMParameter.CORRELATION), 0.001);
		assertEquals("Sum", 1.0000, result.get(GLCMParameter.SUM), 0.01);
	}
		
	@Test
	public void testRunningOnImageWithEastStepAngle() throws Exception {
		HashOptions options = GLCM.defaultOptions();
		options.setString(GLCM.ANGLE_KEY, GLCMStepAngle.EAST.toString());
		
		File f = new File(TestResources.GLCM_SAMPLE_IMAGE);
		ImageProcessor ip = new ImageImporter(f).importImage(ImageImporter.RGB_BLUE);
		
		GLCM glcm = new GLCM(options);
		GLCMTile result = glcm.calculate(ip);
		
//		 Compare to known values for this image
		assertEquals("ASM", 0.0014, result.get(GLCMParameter.ASM), 0.001);
		assertEquals("IDM", 0.2195, result.get(GLCMParameter.IDM), 0.001);
		assertEquals("Contrast", 68.7502, result.get(GLCMParameter.CONSTRAST), 0.001);
		assertEquals("Energy", 0.0014, result.get(GLCMParameter.ENERGY), 0.001);
		assertEquals("Entropy", 7.3555, result.get(GLCMParameter.ENTROPY), 0.001);
		assertEquals("Homogeneity", 0.3076, result.get(GLCMParameter.HOMOGENEITY), 0.001);
		assertEquals("Variance", 5012.8402, result.get(GLCMParameter.VARIANCE), 0.001);
		
		// TODO: why is this a negated version of the true value?
//		assertEquals("Shade", -3386116.8969, result.get(GLCMParameter.SHADE), 0.001);
		assertEquals("Prominence", 1134911052.7555, result.get(GLCMParameter.PROMINENCE), 0.001);
		assertEquals("Inertia", 68.7502, result.get(GLCMParameter.INERTIA), 0.001);
		assertEquals("Correlation", 0.0002, result.get(GLCMParameter.CORRELATION), 0.001);
		assertEquals("Sum", 1.0000, result.get(GLCMParameter.SUM), 0.01);
	}
	
	@Test
	public void testRunningOnImageWithSouthEastStepAngle() throws Exception {
		HashOptions options = GLCM.defaultOptions();
		options.setString(GLCM.ANGLE_KEY, GLCMStepAngle.SOUTHEAST.toString());
		
		File f = new File(TestResources.GLCM_SAMPLE_IMAGE);
		ImageProcessor ip = new ImageImporter(f).importImage(ImageImporter.RGB_BLUE);
		
		GLCM glcm = new GLCM(options);
		GLCMTile result = glcm.calculate(ip);
		
//		 Compare to known values for this image
		assertEquals("ASM", 0.0012, result.get(GLCMParameter.ASM), 0.001);
		assertEquals("IDM", 0.1843, result.get(GLCMParameter.IDM), 0.001);
		assertEquals("Contrast", 91.7628, result.get(GLCMParameter.CONSTRAST), 0.001);
		assertEquals("Energy", 0.0012, result.get(GLCMParameter.ENERGY), 0.001);
		assertEquals("Entropy", 7.5147, result.get(GLCMParameter.ENTROPY), 0.001);
		assertEquals("Homogeneity", 0.2730, result.get(GLCMParameter.HOMOGENEITY), 0.001);
		assertEquals("Variance", 5046.7020, result.get(GLCMParameter.VARIANCE), 0.001);
		
		// TODO: why is this a negated version of the true value?
//		assertEquals("Shade", -3362436.7051, result.get(GLCMParameter.SHADE), 0.001);
		assertEquals("Prominence", 1128331783.3927, result.get(GLCMParameter.PROMINENCE), 0.001);
		assertEquals("Inertia", 91.7628, result.get(GLCMParameter.INERTIA), 0.001);
		assertEquals("Correlation", 0.0002, result.get(GLCMParameter.CORRELATION), 0.001);
		assertEquals("Sum", 1.0000, result.get(GLCMParameter.SUM), 0.01);
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
//		System.out.println(result1.toString());
//		ip.setLineWidth(2);
//		ip.setColor(Color.GRAY);
//		ip.draw(roi);

		GLCMTile result = glcm.calculate(n);
		
		assertEquals("Sum should be 1", 1d, result1.get(GLCMParameter.SUM), 0.01);
		assertFalse("Nucleus specific GLCM should not be identical to whole image GLCM",result.toString().equals(result1.toString()));
		
		
//		System.out.println(result.toString());
	}
	
	@Test
	public void testRunningAllStepAnglesOnComponent() throws Exception {
		HashOptions options = GLCM.defaultOptions();
		options.setString(GLCM.ANGLE_KEY, GLCMStepAngle.ALL.toString());
//		options.setInt(GLCM.STEP_SIZE_KEY, 2);
		GLCM glcm = new GLCM(options);
		
		IAnalysisDataset d = SampleDatasetReader.openTestRodentDataset();
		
		Nucleus n = d.getCollection().stream().findFirst().get().getNucleus();
		
		Roi roi = n.toRoi();
		roi.setLocation(Imageable.COMPONENT_BUFFER, Imageable.COMPONENT_BUFFER);
		ImageProcessor ip = n.getComponentImage().convertToByte(false);
		GLCMTile result1 = glcm.calculate(ip);
		System.out.println(result1.toString());

		GLCMTile result = glcm.calculate(n);
		
		assertEquals("Sum should be 1", 1d, result1.get(GLCMParameter.SUM), 0.01);
		assertFalse("Nucleus specific GLCM should not be identical to whole image GLCM",result.toString().equals(result1.toString()));
		
		
//		System.out.println(result.toString());
	}
//	
//	@Test
//	public void testRunningOnDataset() throws Exception {
//		GLCM glcm = new GLCM();
//		
//		IAnalysisDataset d = SampleDatasetReader.openTestRodentDataset();
//		
//		List<GLCMTile> results = new ArrayList<>();
//		for(ICell c : d.getCollection()) {
//			for(Nucleus n : c.getNuclei()) {
//				GLCMTile r = glcm.calculate(n);
//				results.add(r);
//				for(GLCMParameter v : GLCMParameter.values())
//					n.setStatistic(v.toStat(), r.get(v));
//			}
//		}
//		
//		StringBuilder builder = new StringBuilder();
//		builder.append("ID"+Io.TAB);
//		for(GLCMParameter v : GLCMParameter.values())
//			builder.append(v.toString()+Io.TAB);
//		builder.append(Io.NEWLINE);
//		
//		for(GLCMTile r : results) {
//			builder.append(r.getIdentifier()+Io.TAB);
//			for(GLCMParameter v : GLCMParameter.values())
//				builder.append(r.get(v)+Io.TAB);
//			builder.append(Io.NEWLINE);
//		}
//		System.out.println(builder.toString());
//	}
}
