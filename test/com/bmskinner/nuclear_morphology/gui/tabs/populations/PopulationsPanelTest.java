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
package com.bmskinner.nuclear_morphology.gui.tabs.populations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

public class PopulationsPanelTest {
	
	Class c;
	Object panel;
	
	@Before
	public void setUp() throws Exception {
		c = Class.forName("com.bmskinner.nuclear_morphology.gui.tabs.populations.PopulationsPanel");
		panel = c.newInstance();
	}
	
	private void addNameToPanel(String newName) throws Exception {

		// get the populationNames hash and add a population
		Field field;
		field = c.getDeclaredField("populationNames");
		field.setAccessible(true);

		// make the population
		Map<String, UUID> names = (Map<String, UUID>) field.get(panel);

		names.put(newName, java.util.UUID.randomUUID());
		field.set(panel, names);

	}
		
	@Test
	public void renameCollectionDetectsExistingName() throws Exception {
		
		String oldName = "An_old_name";
		String newName = "An_old_name_1";

		addNameToPanel( oldName);

		System.out.println("Beginning test");

		// get the rename method and make it accessible		
		Method method = c.getDeclaredMethod("checkName", new Class[] {String.class});
		method.setAccessible(true);

		// First test - does the existing name get replaced?

		System.out.println("First test");
		String firstPass = (String) method.invoke(panel, new String[] { oldName });

		assertEquals("First pass values should be identical", newName, firstPass );

	}
	
	/**
	 * Does the digit increment?
	 * @throws Exception 
	 */
	@Test
	public void renameCollectionDigitIncrements() throws Exception {

		String oldName = "An_old_name";
		String newName = "An_old_name_1";
		String newerName = "An_old_name_2";

		addNameToPanel(oldName);
		addNameToPanel(newName);

		
		Class c;
		try {
			c = Class.forName("com.bmskinner.nuclear_morphology.gui.tabs.populations");

			Method method = c.getDeclaredMethod("checkName", new Class[] {String.class});
			method.setAccessible(true);
			System.out.println("Second test");

			String secondPass = (String) method.invoke(panel, new String[] { newName });
			assertEquals("Second pass values should be identical", newerName, secondPass);
		} catch (ClassNotFoundException e) {
			fail("Class error");
			e.printStackTrace();
		} catch (SecurityException e) {
			fail("Security error");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			fail("Access error");
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail("Argument error");
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			fail("Invocation error");
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			fail("Method error");
		} 
	}

}
