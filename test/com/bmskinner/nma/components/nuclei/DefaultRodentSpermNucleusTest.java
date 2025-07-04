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

package com.bmskinner.nma.components.nuclei;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.bmskinner.nma.components.MissingDataException;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.measure.MeasurementScale;
import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;
import com.bmskinner.nma.samples.dummy.DummyRodentSpermNucleus;

public class DefaultRodentSpermNucleusTest {

	private Nucleus testNucleus;

	@Before
	public void setUp() throws ComponentCreationException {
		testNucleus = new DummyRodentSpermNucleus();
	}

	@Test
	public void testGetChannel() {
		int expected = 0;
		assertThat(testNucleus.getChannel(), is(expected));
	}

	@Test
	public void testGetScale() {
		double expected = 1;
		assertThat(testNucleus.getScale(), is(expected));
	}

	@Test
	public void testGetStatisticPlottableStatisticMeasurementScale()
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {
		double scale = 5;

		// Get and save the values with default scale 1
		Map<Measurement, Double> map = new HashMap<>();
		for (Measurement stat : testNucleus.getMeasurements()) {
			map.put(stat, testNucleus.getMeasurement(stat));
		}

		// Update scale
		testNucleus.setScale(scale);

		// Get the actual values for microns and pixels
		for (Measurement stat : testNucleus.getMeasurements()) {
			double m = testNucleus.getMeasurement(stat, MeasurementScale.MICRONS);

			double expected = Measurement.convert(map.get(stat), scale, MeasurementScale.MICRONS,
					stat.getDimension());
			assertEquals(stat.toString(), expected, m, 0);

			double d = testNucleus.getMeasurement(stat, MeasurementScale.PIXELS);
			assertEquals(stat.toString(), map.get(stat), d, 0);
		}
	}

	@Test
	public void testSetStatistic()
			throws MissingDataException, ComponentCreationException, SegmentUpdateException {
		double epsilon = 0; // the amount of difference permitted
		double expected = 25;

		for (Measurement stat : Measurement.getNucleusStats()) {
			testNucleus.setMeasurement(stat, expected);
			double d = testNucleus.getMeasurement(stat);
			assertEquals(stat.toString(), expected, d, epsilon);
		}
	}
}
