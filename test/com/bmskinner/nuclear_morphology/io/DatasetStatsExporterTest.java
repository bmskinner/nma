package com.bmskinner.nuclear_morphology.io;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.UUID;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.options.DefaultOptions;
import com.bmskinner.nuclear_morphology.components.options.HashOptions;
import com.bmskinner.nuclear_morphology.components.profiles.IProfileSegment;
import com.bmskinner.nuclear_morphology.components.profiles.Landmark;
import com.bmskinner.nuclear_morphology.components.rules.RuleSetCollection;

public class DatasetStatsExporterTest {

    
	@Test
	public void testSegmentsInterpolated() throws Exception {
		
		IAnalysisDataset d = new TestDatasetBuilder()
				.cellCount(1)
				.baseHeight(500)
				.baseWidth(1000)
				.ofType(RuleSetCollection.roundRuleSetCollection())
				.segmented()
				.build();
		
		// Ensure profiles are longer than 1000
		int length = d.getCollection().getMedianArrayLength();
		assertTrue(length>1000);
		
		// Decrease the length of the segments to below minimum regular length
		while(d.getCollection().getProfileCollection().getSegments(OrientationMark.REFERENCE).get(0).length()>IProfileSegment.MINIMUM_SEGMENT_LENGTH*0.7){
			IProfileSegment seg = d.getCollection().getProfileCollection().getSegments(OrientationMark.REFERENCE).get(0);
			boolean b = d.getCollection().getProfileManager().splitSegment(seg, UUID.randomUUID(), UUID.randomUUID());
			if(!b)
				fail("Unable to split "+seg);

		}
		
		// Seg 0 is smaller than regular minimum length
		// Segment 0 is still longer than the minimum interpolatable length
		assertTrue(d.getCollection().getProfileCollection().getSegments(OrientationMark.REFERENCE).get(0).length()<IProfileSegment.MINIMUM_SEGMENT_LENGTH);
		
		// Interpolation length should be chosen to be at least the current length
		// If this operation fails, the interpolation logic did not succeed 
		
		HashOptions op = new DefaultOptions();
		op.setInt(Io.PROFILE_SAMPLES_KEY, 10);
		
		File outFile = new File("test");
		
		DatasetStatsExporter dse = new DatasetStatsExporter(outFile, d, op);
		StringBuilder outLine = new StringBuilder();
		dse.append(d, outLine);
	}
}
