package com.bmskinner.nuclear_morphology;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nuclear_morphology.components.generic.IPoint;

/**
 * Base class for the component tests
 * @author bms41
 * @since 1.14.0
 *
 */
public abstract class ComponentTester extends FloatArrayTester {
	
	public static final long RNG_SEED = 1234;
	public static final int N_CELLS = 10;
	public static final int N_CHILD_DATASETS = 2;
	private static final Logger LOGGER = Logger.getLogger(ComponentTester.class.getName());
	
	private static final List<Class> SPECIAL_CLASSES = List.of(HashMap.class, HashSet.class, ArrayList.class);
	
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
	public static void testDuplicatesByField(Object original, Object dup, List<String> fieldsToSkip) throws Exception {
		for(Field f : getInheritedPrivateFields(dup.getClass())) {
			
			if(fieldsToSkip.contains(f.getName()))
				continue;
			
			if(Modifier.isStatic(f.getModifiers())) // ignore static fields
				continue;
			
			if(Modifier.isTransient(f.getModifiers())) // ignore transient fields
				continue;
			
//			LOGGER.fine("Testing field "+f.getName()+" of "+dup.getClass().getName());
			
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
			
			Object oValue  = f.get(original);
			Object dValue  = f.get(dup);
			
			// Avoid self referential recursion
			if(original==oValue)
				continue;

			if(oValue!=null && dValue!=null) {
				
				if(SPECIAL_CLASSES.contains(oValue.getClass())) {

					if(oValue.getClass().equals(HashMap.class)) {
						testHashMapsByField(f, (HashMap)oValue, (HashMap)dValue);
					}
					if(oValue.getClass().equals(HashSet.class)) {
						testHashSetsByField(f, (HashSet)oValue, (HashSet)dValue);
					}
					
					if(oValue.getClass().equals(ArrayList.class)) {
						testListsByField(f, (ArrayList)oValue, (ArrayList)dValue);
					}

				} else {

					// Don't try to compare classes I didn't write
					// and don't try to unpack enums
					if(f.getType().getName().startsWith("com.bmskinner.nuclear_morphology") && !f.getType().isEnum()) {
						testDuplicatesByField(oValue, dValue, fieldsToSkip);
					} else{
						String msg = "Field '"+f.getName()+"' in "+f.getType().getSimpleName()+" does not match";
						assertThat(msg, dValue, equalTo(oValue));
					}
				}
			}

		}

		assertEquals("Equals method in "+original.getClass().getSimpleName(), original, dup);
	}
	
	private static void testListsByField(Field f, List o, List d) {
		assertTrue("Lists should not both be null in "+f.getName(), o!=null&&d!=null);
		assertEquals("List should contain same number of elements: '"+f.getName()+"' in "+f.getDeclaringClass().getName(), o.size(), d.size());
		
		for(int i=0; i<o.size(); i++) {
			assertEquals("Field '"+f.getName()+"' element "+i+" in lists: "+o+" and "+d, o.get(i), d.get(i));
		}
	}
	
	// Issue with arrays in hashmaps: Object.hashcode()
	// depends on reference, so is not equal between two
	// arrays. Need to use Arrays.hashcode().
	private static void testHashMapsByField(Field f, HashMap o, HashMap d) {
		assertTrue("Hashmaps should not both be null in "+f.getName(), o!=null&&d!=null);
		assertEquals("Map should contain same number of elements: '"+f.getName()+"' in "+f.getDeclaringClass().getName(), o.size(), d.size());
		
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
			
			assertEquals("Hashes should match for key in "+f.getName()+": "+e.toString(), oHash, dHash);
		}
	}
	
	private static void testHashSetsByField(Field f, HashSet o, HashSet d) {
		assertTrue("Hashsets should not both be null", o!=null&&d!=null);
		assertEquals("Hashsets should contain same number of elements", o.size(), d.size());
		
		for(Object v0 : o) {
			assertTrue("Field '"+f.getName()+"' should contain element "+v0.toString(), d.contains(v0));
		}
		
		assertTrue("All elements should be shared in hashset in"+f.getName(), o.containsAll(d));
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
