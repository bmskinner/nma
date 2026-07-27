package com.bmskinner.nma.analysis.classification;

import org.junit.Test;

public class ClusteringMethodTest {

	/**
	 * If we add more clustering methods or change the enum names, ensure all can be
	 * read
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFrom() throws Exception {
		ClusteringMethod.from("Expectation maximisation");
		ClusteringMethod.from("EM");
		ClusteringMethod.from("Hierarchical");
		ClusteringMethod.from("Imported");
		ClusteringMethod.from("Manual");
	}

}
