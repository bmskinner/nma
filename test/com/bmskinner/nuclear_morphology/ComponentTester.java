package com.bmskinner.nuclear_morphology;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;
import org.junit.Before;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;

/**
 * Base class for the component tests
 * @author bms41
 * @since 1.14.0
 *
 */
public abstract class ComponentTester extends FloatArrayTester {
	
	protected static final long RNG_SEED = 1234;
	protected static final int N_CELLS = 10;
	protected static final int N_CHILD_DATASETS = 2;
	protected static final Logger LOGGER = Logger.getLogger(ComponentTester.class.getName());

	@Before
	public void setUp() throws Exception{

	}
	
	/**
	 * Test if the two given points are vertically aligned
	 * @param topPoint the top point
	 * @param bottomPoint the bottom point
	 * @return
	 */
	public static boolean areVertical(@NonNull IPoint topPoint, @NonNull IPoint bottomPoint) {
		double err = bottomPoint.getX()-topPoint.getX();
		System.out.println(err);
		LOGGER.fine("Error = "+err);
		boolean xEqual = (Math.abs(bottomPoint.getX()- topPoint.getX())<0.0001);
		boolean yAbove = topPoint.getY()>bottomPoint.getY();
		return xEqual & yAbove;
	}
	
	/**
	 * Get all the private fields for the class, including superclass fields
	 * @param type
	 * @return
	 */
	private static List<Field> getInheritedPrivateFields(Class<?> type) {
	    List<Field> result = new ArrayList<>();

	    Class<?> i = type;
	    while (i != null && i != Object.class) {
	        Collections.addAll(result, i.getDeclaredFields());
	        i = i.getSuperclass();
	    }

	    return result;
	}
	
	/**
	 * Get the inherited field for the given class
	 * @param type the class to fetch
	 * @param name the inherited field name
	 * @return
	 */
	protected static Field getInheritedField(Class<?> type, String name) {
		List<Field> fields = getInheritedPrivateFields(type);
		for(Field f : fields)
			if(f.getName().equals(name)) {
				f.setAccessible(true);
				return f;
			}
		return null;
	}
	
	/**
	 * Test if the fields of two objects have the same hashcodes.
	 * Skips cache classes which are not used in hashcode methods. 
	 * @param original
	 * @param dup
	 * @param fieldsToSkip skip fields in the object with these names
	 * @throws Exception
	 */
	protected static void testDuplicatesByField(Object original, Object dup, List<String> fieldsToSkip) throws Exception {
		for(Field f : getInheritedPrivateFields(dup.getClass())) {
			
			if(fieldsToSkip.contains(f.getName()))
				continue;
			f.setAccessible(true);	

			if(f.getType().equals(SoftReference.class))
				continue;
			if(f.getType().equals(WeakReference.class))
				continue;
			if(f.getType().equals(Class.forName("com.bmskinner.nuclear_morphology.components.cells.DefaultCellularComponent$ShapeCache")))
				continue;
			if(f.getType().equals(Class.forName("com.bmskinner.nuclear_morphology.components.profiles.DefaultProfileCollection$ProfileCache")))
				continue;	
			if(f.getType().equals(Class.forName("com.bmskinner.nuclear_morphology.components.measure.StatsCache")))
				continue;
			if(f.getType().equals(Class.forName("com.bmskinner.nuclear_morphology.components.measure.VennCache")))
				continue;
			if(f.getType().equals(Class.forName("com.bmskinner.nuclear_morphology.components.signals.SignalManager")))
				continue;
			if(f.getType().equals(Class.forName("com.bmskinner.nuclear_morphology.components.profiles.ProfileManager")))
				continue;
			Object oValue = f.get(original);
			Object dValue  = f.get(dup);

			// ignore transient fields
			if(!Modifier.isTransient(f.getModifiers())) {
				if(oValue!=null && dValue!=null) {

					if(oValue.getClass().equals(HashMap.class) ||
							oValue.getClass().equals(HashSet.class)) {

						// Issue with arrays in hashmaps: Object.hashcode()
						// depends on reference, so is not equal between two
						// arrays. Need to use Arrays.hashcode().
						if(oValue.getClass().equals(HashMap.class)) {
							testHashMapsByField((HashMap)oValue, (HashMap)dValue);
						}
						if(oValue.getClass().equals(HashSet.class)) {
							testHashSetsByField((HashSet)oValue, (HashSet)dValue);
						}
					}
				} else {
					String msg = "Field '"+f.getName()+"' in "+original.getClass().getSimpleName()+" does not match";
					assertThat(msg, dValue, equalTo(oValue));
				}
			}
		}

		assertEquals("Equals method in "+original.getClass().getSimpleName(), original, dup);
	}
	
	// Issue with arrays in hashmaps: Object.hashcode()
	// depends on reference, so is not equal between two
	// arrays. Need to use Arrays.hashcode().
	private static void testHashMapsByField(HashMap o, HashMap d) {
		assertTrue("Hashmaps should not both be null", o!=null&&d!=null);
		assertEquals("Maps should contain same number of elements", o.size(), d.size());
		
		long oHash = 0;
		long dHash = 0;
	
		for(Object e : o.keySet()) {
			Object v0 = o.get(e);
			Object v1 = d.get(e);
			
			if(v0.getClass().equals(byte[].class)) {
				oHash += Arrays.hashCode((byte[])v0);
				dHash += Arrays.hashCode((byte[])v1);
			}

			if(v0.getClass().equals(long[].class)) {
				oHash += Arrays.hashCode((long[])v0);
				dHash += Arrays.hashCode((long[])v1);
			}
			
			if(v0.getClass().equals(int[].class)) {
				oHash += Arrays.hashCode((int[])v0);
				dHash += Arrays.hashCode((int[])v1);
			}
			
			if(v0.getClass().equals(double[].class)) {
				oHash += Arrays.hashCode((double[])v0);
				dHash += Arrays.hashCode((double[])v1);
			}
			
			if(v0.getClass().equals(float[].class)) {
				oHash += Arrays.hashCode((float[])v0);
				dHash += Arrays.hashCode((float[])v1);
			}
			
			assertEquals("Hashes should match for key "+e.toString(), oHash, dHash);
		}
	}
	
	// Issue with arrays in hashmaps: Object.hashcode()
	// depends on reference, so is not equal between two
	// arrays. Need to use Arrays.hashcode().
	private static void testHashSetsByField(HashSet o, HashSet d) {
		assertTrue("Hashsets should not both be null", o!=null&&d!=null);
		assertEquals("Hashsets should contain same number of elements", o.size(), d.size());
		if(!o.containsAll(d)) {
			System.out.println("Elements not shared in set");
			for(Object v0 : o) {
				System.out.println(d.contains(v0));
			}

		}	
		assertTrue("All elements should be shared in hashset", o.containsAll(d));
	}
		
	/**
	 * Test if the fields of two objects have the same hashcodes.
	 * Skips cache classes which are not used in hashcode methods. 
	 * @param original
	 * @param dup
	 * @throws Exception
	 */
	public static void testDuplicatesByField(Object original, Object dup) throws Exception {
		List<String> fieldsToSkip = new ArrayList<>();
		testDuplicatesByField(original, dup, fieldsToSkip);
	}

}
