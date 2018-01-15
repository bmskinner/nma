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
package components;

import static org.junit.Assert.assertEquals;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import samples.dummy.DummyRodentSpermNucleus;

import java.io.File;

import org.junit.Test;

import com.bmskinner.nuclear_morphology.components.ComponentFactory.ComponentCreationException;
import com.bmskinner.nuclear_morphology.components.generic.IPoint;
import com.bmskinner.nuclear_morphology.components.nuclei.Nucleus;
import com.bmskinner.nuclear_morphology.components.nuclei.sperm.DefaultRodentSpermNucleus;
import com.bmskinner.nuclear_morphology.components.stats.NucleusStatistic;
import com.bmskinner.nuclear_morphology.components.stats.PlottableStatistic;

public class NucleusTest {

	@Test
	public void testMinDiameter() throws Exception {
		Nucleus n = new DummyRodentSpermNucleus();
		double expected = 53.14;
		double epsilon = 0; // the amount of difference permitted
		
		assertEquals("Values should be identical", expected, n.getStatistic(PlottableStatistic.MIN_DIAMETER), epsilon);
	}
	
	@Test
	public void testFeret() throws Exception {
	    Nucleus n = new DummyRodentSpermNucleus();
		double expected = 134.27;
		double epsilon = 0.01; // the amount of difference permitted
		assertEquals("Values should be identical", expected, n.getStatistic(PlottableStatistic.MAX_FERET), epsilon);
	}
	
	@Test
	public void testCentreOfMass() throws ComponentCreationException {
	    Nucleus n = new DummyRodentSpermNucleus();
		int expectedX = 74;
		int expectedY = 46;
		
		double expectedXD = 74.0;
		double expectedYD = 46.0;
		double epsilon = 0.01; // the amount of difference permitted
				
		assertEquals("X int values should be identical", expectedX, n.getCentreOfMass().getXAsInt());
		assertEquals("Y int values should be identical", expectedY, n.getCentreOfMass().getYAsInt());
		
		assertEquals("X double values should be identical", expectedXD, n.getCentreOfMass().getX(), epsilon);
		assertEquals("Y double values should be identical", expectedYD, n.getCentreOfMass().getY(), epsilon);
	}

}
