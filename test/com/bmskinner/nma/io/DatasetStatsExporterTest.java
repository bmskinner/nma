package com.bmskinner.nma.io;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import com.bmskinner.nma.TestDatasetBuilder;
import com.bmskinner.nma.analysis.profiles.SegmentSplitMethod;
import com.bmskinner.nma.components.datasets.IAnalysisDataset;
import com.bmskinner.nma.components.options.DefaultOptions;
import com.bmskinner.nma.components.options.HashOptions;
import com.bmskinner.nma.components.profiles.IProfileSegment;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.rules.RuleSetCollection;

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
		assertTrue(length > 1000);

		// Decrease the length of the segments to below minimum regular length
		while (d.getCollection().getProfileCollection().getSegments(OrientationMark.REFERENCE)
				.get(0).length() > IProfileSegment.MINIMUM_SEGMENT_LENGTH * 0.7) {

			IProfileSegment seg = d.getCollection().getProfileCollection()
					.getSegments(OrientationMark.REFERENCE).get(0);

			new SegmentSplitMethod(d, seg.getID()).call();
		}

		// Seg 0 is smaller than regular minimum length
		// Segment 0 is still longer than the minimum interpolatable length
		assertTrue(d.getCollection().getProfileCollection().getSegments(OrientationMark.REFERENCE)
				.get(0).length() < IProfileSegment.MINIMUM_SEGMENT_LENGTH);

		// Interpolation length should be chosen to be at least the current length
		// If this operation fails, the interpolation logic did not succeed

		HashOptions op = new DefaultOptions();
		op.setInt(HashOptions.EXPORT_PROFILE_INTERPOLATION_LENGTH, 10);

		File outFile = new File("test");

		DatasetMeasurementsExporter dse = new DatasetMeasurementsExporter(outFile, d, op);
		StringBuilder outLine = new StringBuilder();
		dse.append(d, outLine);
	}
}
