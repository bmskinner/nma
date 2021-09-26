package com.bmskinner.nuclear_morphology.charting;

import org.junit.Ignore;
import org.junit.Test;

import com.bmskinner.nuclear_morphology.TestDatasetBuilder;
import com.bmskinner.nuclear_morphology.components.datasets.IAnalysisDataset;
import com.bmskinner.nuclear_morphology.components.nuclei.NucleusType;

/**
 * Test that individual components are drawn correctly in outline charts.
 * @author bms41
 * @since 1.14.0
 *
 */
@Ignore
public class OutlineChartFactoryTest extends ChartFactoryTest {

	@Test
	public void testOutlineChartOfSingleCellNonSegmentedDataset() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(12345).cellCount(1)
				.baseHeight(40).baseWidth(40)
				.ofType(NucleusType.ROUND)
				.profiled().build();
		OutlineTestChartFactory.generateOutlineChartsForAllCells(d, null);
	}
	
	@Test
	public void testOutlineChartOfSingleCellSegmentedDataset() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(12345).cellCount(1)
				.baseHeight(40).baseWidth(40)
				.ofType(NucleusType.ROUND)
				.segmented().build();
		OutlineTestChartFactory.generateOutlineChartsForAllCells(d, null);
	}
	
	@Test
	public void testOutlineChartOfMultiCellNonSegmentedDataset() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(1234).cellCount(10)
				.baseHeight(40).baseWidth(40)
				.ofType(NucleusType.ROUND)
				.profiled().build();
		OutlineTestChartFactory.generateOutlineChartsForAllCells(d, null);
	}
	
	@Test
	public void testOutlineChartOfMultiCellSegmentedDataset() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(1234).cellCount(10)
				.baseHeight(40).baseWidth(40)
				.ofType(NucleusType.ROUND)
				.withMaxSizeVariation(10)
				.segmented().build();
		OutlineTestChartFactory.generateOutlineChartsForAllCells(d, null);
	}
	
	@Test
	public void testOutlineChartOfRotatedMultiCellSegmentedDataset() throws Exception {
		IAnalysisDataset d = new TestDatasetBuilder(12345).cellCount(20)
				.baseHeight(40).baseWidth(40)
				.ofType(NucleusType.ROUND)
				.maxRotation(90)
				.segmented().build();
		OutlineTestChartFactory.generateOutlineChartsForAllCells(d, null);
	}
	
	

}
