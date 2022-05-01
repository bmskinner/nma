/*******************************************************************************
 *      Copyright (C) 2016 Ben Skinner
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

package com.bmskinner.nma.api;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nma.TestImageDatasetCreator;
import com.bmskinner.nma.TestResources;
import com.bmskinner.nma.components.cells.CellularComponent;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.options.IAnalysisOptions;
import com.bmskinner.nma.components.options.OptionsFactory;
import com.bmskinner.nma.io.SampleDatasetReader;

import ij.Prefs;

/**
 * Test the detection methods to ensure new analyses match previously 
 * saved datasets. The test sets should have been created using the 
 * {@link TestImageDatasetCreator} before these tests are invoked.
 * @author bms41
 * @since 1.13.8
 *
 */
public class BasicAnalysisPipelineTest extends AnalysisPipelineTest {
	
	@Before
	public void setUp(){		
		Prefs.setThreads(2); // Attempt to avoid issue 162
	}

	@Test
	public void testMouseDatasetMatchesSavedDataset() throws Exception{
		IAnalysisDataset exp = SampleDatasetReader.openDataset(TestResources.MOUSE_TEST_DATASET);
		IAnalysisOptions op = OptionsFactory.makeDefaultRodentAnalysisOptions(TestResources.MOUSE_INPUT_FOLDER);

		IAnalysisDataset obs = TestImageDatasetCreator.createTestDataset(TestResources.IMAGE_FOLDER, op, false);
		testDatasetEquality(exp, obs);       
	}

	@Test
	public void testPigDatasetMatchesSavedDataset() throws Exception{
		IAnalysisDataset exp = SampleDatasetReader.openDataset(TestResources.PIG_TEST_DATASET);
		IAnalysisOptions op = OptionsFactory.makeDefaultPigAnalysisOptions(TestResources.PIG_INPUT_FOLDER);

		IAnalysisDataset obs = TestImageDatasetCreator.createTestDataset(TestResources.IMAGE_FOLDER, op, false);
		testDatasetEquality(exp, obs);       
	}

	@Test
	public void testRoundDatasetMatchesSavedDataset() throws Exception{
		IAnalysisDataset exp = SampleDatasetReader.openDataset(TestResources.ROUND_TEST_DATASET);
		IAnalysisOptions op = OptionsFactory.makeDefaultRoundAnalysisOptions(TestResources.ROUND_INPUT_FOLDER);

		IAnalysisDataset obs = TestImageDatasetCreator.createTestDataset(TestResources.IMAGE_FOLDER, op, false);
		testDatasetEquality(exp, obs);       
	}
	
	


	/**
	 * Check if the two datasets match.
	 * @param exp the expected (reference) dataset
	 * @param obs the observed (newly created) dataset
	 */
	private void testDatasetEquality(@NonNull IAnalysisDataset exp, @NonNull IAnalysisDataset obs) throws Exception{
		assertEquals("Dataset name", exp.getName(), obs.getName());

		//    	assertEquals("Options",exp.getAnalysisOptions(), obs.getAnalysisOptions());

		assertEquals("Number of images", exp.getCollection().getImageFiles().size(), obs.getCollection().getImageFiles().size());

		List<Nucleus> expN = new ArrayList<>(exp.getCollection().getNuclei());
		List<Nucleus> obsN = new ArrayList<>(obs.getCollection().getNuclei());

		Collections.sort(expN);
		Collections.sort(obsN);

		for(int i=0; i<expN.size(); i++)
			assertEquals("Nucleus file name for: "+expN.get(i).getNameAndNumber(), expN.get(i).getSourceFileName(), obsN.get(i).getSourceFileName());

		assertEquals("Detected nuclei", exp.getCollection().getNucleusCount(), obs.getCollection().getNucleusCount());



		// Check the stats are the same
		for(Measurement s : Measurement.getStats(CellularComponent.NUCLEUS)){
			double eMed = exp.getCollection().getMedian(s, CellularComponent.NUCLEUS, MeasurementScale.PIXELS);
			double oMed = obs.getCollection().getMedian(s, CellularComponent.NUCLEUS, MeasurementScale.PIXELS);

			assertEquals("Stats should be equal: " +s.toString(), eMed, oMed, 0.3);
		}
	}
}
