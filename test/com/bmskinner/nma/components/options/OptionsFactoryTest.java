package com.bmskinner.nma.components.options;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OptionsFactoryTest {

	@Test
	public void testClustersAdded() {
		HashOptions o = OptionsFactory.makeNucleusDetectionOptions().build();

		o.setSubOptions(HashOptions.CLUSTER_SUB_OPTIONS_KEY,
				OptionsFactory.makeDefaultClusteringOptions().build());

		HashOptions t = o.getSubOptions(HashOptions.CLUSTER_SUB_OPTIONS_KEY);
		assertEquals(OptionsFactory.makeDefaultClusteringOptions(), t);
	}

}
