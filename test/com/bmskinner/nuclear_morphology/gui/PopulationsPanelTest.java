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
package com.bmskinner.nuclear_morphology.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

public class PopulationsPanelTest {
	
	private void addNameToPanel(Object panel, String newName){
		
		try {
			Class c = Class.forName("no.gui.PopulationsPanel");
		
			// get the populationNames hash and add a population
			Field field;
			field = c.getDeclaredField("populationNames");
			field.setAccessible(true);

			// make the population
			Map<String, UUID> names = (Map<String, UUID>) field.get(panel);
			
			names.put(newName, java.util.UUID.randomUUID());
			field.set(panel, names);
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private Object createPanelWithNames(){

		Object panel = null;
		try {
			Class c = Class.forName("no.gui.PopulationsPanel");
			panel = c.newInstance();

		} catch (ClassNotFoundException e) {
			fail("Class error");
			e.printStackTrace();
		} catch (SecurityException e) {
			fail("Security error");
			e.printStackTrace();
		} catch (InstantiationException e) {
			fail("Instantiation error");
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			fail("Access error");
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			fail("Argument error");
		} 
		return panel;
	}
	
	@Test
	public void renameCollectionDetectsExistingName() {
		
		String oldName = "An_old_name";
		String newName = "An_old_name_1";


		Object panel = createPanelWithNames();
		addNameToPanel(panel, oldName);

		System.out.println("Beginning test");

		// get the rename method and make it accessible
		Class c;
		try {
			c = Class.forName("no.gui.PopulationsPanel");
		
		Method method = c.getDeclaredMethod("checkName", new Class[] {String.class});
		method.setAccessible(true);

		// First test - does the existing name get replaced?

		System.out.println("First test");
		String firstPass = (String) method.invoke(panel, new String[] { oldName });

		assertEquals("First pass values should be identical", newName, firstPass );
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
	
	/**
	 * Does the digit increment?
	 */
	@Test
	public void renameCollectionDigitIncrements() {

		String oldName = "An_old_name";
		String newName = "An_old_name_1";
		String newerName = "An_old_name_2";

		Object panel = createPanelWithNames();
		addNameToPanel(panel, oldName);
		addNameToPanel(panel, newName);

		
		Class c;
		try {
			c = Class.forName("no.gui.PopulationsPanel");

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
