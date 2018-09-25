package com.bmskinner.nuclear_morphology.components;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.bmskinner.nuclear_morphology.components.nuclear.INuclearSignal;

/**
 * Base class for the component tests
 * @author bms41
 * @since 1.14.0
 *
 */
public abstract class ComponentTest {
	
	protected static final long RNG_SEED = 1234;
	protected Logger logger;
	
	/**
	 * Get all the private fields for the class, including superclass fields
	 * @param type
	 * @return
	 */
	protected List<Field> getInheritedPrivateFields(Class<?> type) {
	    List<Field> result = new ArrayList<>();

	    Class<?> i = type;
	    while (i != null && i != Object.class) {
	        Collections.addAll(result, i.getDeclaredFields());
	        i = i.getSuperclass();
	    }

	    return result;
	}
	
	/**
	 * Test if the fields of two objects are identical
	 * @param original
	 * @param dup
	 * @throws Exception
	 */
	protected void testDuplicatesByField(Object original, Object dup) throws Exception {
		for(Field f : getInheritedPrivateFields(dup.getClass())) {
			f.setAccessible(true);			 
			assertEquals(f.getName(), f.get(original), f.get(dup));
		}

		assertEquals(original, dup);
	}

}
