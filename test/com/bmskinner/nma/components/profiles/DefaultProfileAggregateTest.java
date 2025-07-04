/*******************************************************************************
 *  	Copyright (C) 2015 Ben Skinner
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
package com.bmskinner.nma.components.profiles;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.bmskinner.nma.components.profiles.IProfileSegment.SegmentUpdateException;

public class DefaultProfileAggregateTest {

	@Rule
	public final ExpectedException exception = ExpectedException.none();

	@Test
	public void profileShouldNotBeCreatedWithNullData() {

		exception.expect(IllegalArgumentException.class);
		new DefaultProfileAggregate(100, 0);
	}

	@Test
	public void addAProfileToTheAggregate() throws ProfileException, SegmentUpdateException {

		float[] array = { 10, 10, 10, 10, 10, 10, 10, 10, 10, 10 };
		IProfile values = new DefaultProfile(array);

		IProfileAggregate tester = new DefaultProfileAggregate(10, 50);

		for (int i = 0; i < 50; i++) {
			tester.addValues(values);
		}

		IProfile median = tester.getMedian();

		for (int i = 0; i < 10; i++) {
			assertEquals("Values should be identical", array[i], median.toDoubleArray()[i], 0);
		}
	}

}
