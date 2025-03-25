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
package com.bmskinner.nma.components.nuclei;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.bmskinner.nma.components.TestComponentFactory;
import com.bmskinner.nma.components.cells.ComponentCreationException;
import com.bmskinner.nma.components.cells.DefaultNucleus;
import com.bmskinner.nma.components.cells.Nucleus;
import com.bmskinner.nma.components.generic.IPoint;
import com.bmskinner.nma.components.measure.Measurement;
import com.bmskinner.nma.components.rules.OrientationMark;
import com.bmskinner.nma.components.rules.RuleSetCollection;

/**
 * Tests for implementations of the Nucleus interface
 * 
 * @author Ben Skinner
 *
 */
@RunWith(Parameterized.class)
public class NucleusTest {

	private Nucleus nucleus;

	@Parameter(0)
	public Class<? extends Nucleus> source;

	@Before
	public void setUp() throws Exception {
		nucleus = createInstance(source);
	}

	/**
	 * Create an instance of the class under test
	 * 
	 * @param source the class to create
	 * @return
	 * @throws Exception
	 */
	public static Nucleus createInstance(Class<? extends Nucleus> source) throws Exception {

		if (source == DefaultNucleus.class) {
			return TestComponentFactory.rectangularNucleus(100, 100, 20, 20, 0, 20,
					RuleSetCollection.mouseSpermRuleSetCollection());
		}

		throw new Exception("Unable to create instance of " + source);
	}

	@Parameters
	public static Iterable<Class<? extends Nucleus>> arguments() {

		// Since the objects created here persist throughout all tests,
		// we're making class references. The actual objects under test
		// are created fresh from the appropriate class.
		return Arrays.asList(DefaultNucleus.class);
	}

	/**
	 * The minimum diameter is expressed as a fraction of the max diameter. For a
	 * square the max diameter is the diagonal.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMinDiameter() throws Exception {
		double expected = 100;
		double epsilon = 0.01; // the amount of difference permitted
		assertEquals(expected, nucleus.getMeasurement(Measurement.MIN_DIAMETER), epsilon);
	}

	@Test
	public void testCentreOfMass() throws ComponentCreationException {
		int expectedX = 70;
		int expectedY = 70;
		assertEquals("X int values", expectedX, nucleus.getCentreOfMass().getXAsInt());
		assertEquals("Y int values", expectedY, nucleus.getCentreOfMass().getYAsInt());
	}

	@Test
	public void testAlignVertical() throws Exception {
		nucleus.setOrientationMark(OrientationMark.TOP, 10);
		nucleus.setOrientationMark(OrientationMark.BOTTOM, 20);
		assertTrue(nucleus.hasLandmark(OrientationMark.TOP));
		assertTrue(nucleus.hasLandmark(OrientationMark.BOTTOM));

		IPoint tvPre = nucleus.getBorderPoint(OrientationMark.TOP);
		IPoint bvPre = nucleus.getBorderPoint(OrientationMark.BOTTOM);
		assertFalse(Math.abs(tvPre.getX() - bvPre.getX()) < 1);

		nucleus.orient();
		IPoint tv = nucleus.getBorderPoint(OrientationMark.TOP);
		IPoint bv = nucleus.getBorderPoint(OrientationMark.BOTTOM);

		assertEquals(tv.getX(), bv.getX(), 0.0);
		assertTrue(tv.getY() > bv.getY());
	}

	@Test
	public void testGetVerticalNucleusIsIdenticalToAlignVertical() throws Exception {
		nucleus.setOrientationMark(OrientationMark.TOP, 10);
		nucleus.setOrientationMark(OrientationMark.BOTTOM, 20);
		Nucleus vert = nucleus.getOrientedNucleus();

		nucleus.orient();
		IPoint tv = nucleus.getBorderPoint(OrientationMark.TOP);
		IPoint bv = nucleus.getBorderPoint(OrientationMark.BOTTOM);
		IPoint vTv = vert.getBorderPoint(OrientationMark.TOP);
		IPoint vBv = vert.getBorderPoint(OrientationMark.BOTTOM);

		assertEquals("Top vertical", tv, vTv);
		assertEquals("Bottom vertical", bv, vBv);
	}
}
