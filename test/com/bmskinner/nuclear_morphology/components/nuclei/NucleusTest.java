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
package com.bmskinner.nuclear_morphology.components.nuclei;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.bmskinner.nuclear_morphology.analysis.profiles.ProfileException;
import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.generic.ISegmentedProfile;
import com.bmskinner.nuclear_morphology.components.generic.ProfileType;
import com.bmskinner.nuclear_morphology.components.generic.Tag;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableBorderTagException;
import com.bmskinner.nuclear_morphology.components.generic.UnavailableProfileTypeException;
import com.bmskinner.nuclear_morphology.components.TestComponentFactory;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;
import com.bmskinner.nuclear_morphology.samples.dummy.DummyRodentSpermNucleus;

/**
 * Tests for implementations of the Nucleus interface
 * @author ben
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
	 * @param source the class to create
	 * @return
	 * @throws Exception 
	 */
	public static Nucleus createInstance(Class<? extends Nucleus> source) throws Exception {

		if(source==DefaultNucleus.class){
			return TestComponentFactory.rectangularNucleus(100, 100, 20, 20, 0, 20);
		}

		throw new Exception("Unable to create instance of "+source);
	}
	
	@SuppressWarnings("unchecked")
    @Parameters
    public static Iterable<Class<? extends Nucleus>> arguments() {

		// Since the objects created here persist throughout all tests,
		// we're making class references. The actual objects under test
		// are created fresh from the appropriate class.
		return Arrays.asList(DefaultNucleus.class);
	}
	
	@Test
	public void testMinDiameter() throws Exception {
		double expected = 100;
		double epsilon = 2; // the amount of difference permitted
		assertEquals(expected, nucleus.getStatistic(PlottableStatistic.MIN_DIAMETER), epsilon);
	}
		
	@Test
	public void testCentreOfMass() throws ComponentCreationException {
		int expectedX = 70;
		int expectedY = 70;				
		assertEquals("X int values", expectedX, nucleus.getCentreOfMass().getXAsInt());
		assertEquals("Y int values", expectedY, nucleus.getCentreOfMass().getYAsInt());
	}
}
