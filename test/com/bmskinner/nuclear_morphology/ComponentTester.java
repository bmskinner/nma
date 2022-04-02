package com.bmskinner.nuclear_morphology;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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
	
	/** Classes that we have custom code to inspect **/
	private static final List<Class> SPECIAL_CLASSES = List.of(HashMap.class, 
			ConcurrentHashMap.class,
			Map.class,
			HashSet.class, 
			ArrayList.class);
	
	/**
	 * Test if the two given points are vertically aligned
	 * @param topPoint the top point
	 * @param bottomPoint the bottom point
	 * @return
	 */
	public static boolean areVertical(@NonNull IPoint topPoint, @NonNull IPoint bottomPoint) {
		double err = bottomPoint.getX()-topPoint.getX();
		boolean xEqual = (Math.abs(err)<0.0001);
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
	public static void testDuplicatesByField(Object original, Object dup, Set<String> fieldsToSkip) throws Exception {
		for(Field f : getInheritedPrivateFields(dup.getClass())) {
			
			if(f.getName().equals("this$0")) // skip self recursion
				fieldsToSkip.add(f.getName());
				
			if(fieldsToSkip.contains(f.getName()))
				continue;
			
			if(Modifier.isStatic(f.getModifiers())) // ignore static fields
				continue;
			
			if(Modifier.isTransient(f.getModifiers())) // ignore transient fields
				continue;
						
			f.setAccessible(true);	
			
			// Skip classes we don't need to compare for equality testing

			if(f.getType().equals(SoftReference.class))
				continue;
			if(f.getType().equals(WeakReference.class))
				continue;
			if(f.getType().equals(Class.forName("com.bmskinner.nuclear_morphology.components.cells.DefaultCellularComponent$ShapeCache")))
				continue;
			if(f.getType().equals(Class.forName("com.bmskinner.nuclear_morphology.components.datasets.DefaultCellCollection$DefaultProfileCollection$ProfileCache")))
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
				
				Class oClass = oValue.getClass();
				if(SPECIAL_CLASSES.contains(oClass)) {
					
					if(oClass.equals(Map.class) ) {
						testHashMapsByField(f, (Map)oValue, (Map)dValue);
					}
					
					if(oClass.equals(ConcurrentHashMap.class) ) {
						testHashMapsByField(f, (Map)oValue, (Map)dValue);
					}

					if(oClass.equals(HashMap.class) ) {
						testHashMapsByField(f, (Map)oValue, (Map)dValue);
					}
					if(oClass.equals(HashSet.class)) {
						testHashSetsByField(f, (HashSet)oValue, (HashSet)dValue);
					}
					if(oClass.equals(ArrayList.class)) {
						testListEqualityByField(f, (List)oValue, (List)dValue, fieldsToSkip);
					}

				} else {

					// Don't try to compare classes I didn't write
					// and don't try to unpack enums. In these cases, just
					// do a direct equality test
					if(f.getType().getName().startsWith("com.bmskinner.nuclear_morphology") && !f.getType().isEnum()) {
						try {
							testDuplicatesByField(oValue, dValue, fieldsToSkip);
						} catch(StackOverflowError e) {
							String msg = "Field '"+f.getName()+"' of type "+f.getType().getName()
									+ " and class "+ oClass.getName()
									+" had a stack overflow on value: "+oValue
									+"Expected: "+ original 
									+"Found: "+dup;
							fail(msg);
						}
					} else{
						String msg = "Field '"+f.getName()+"' of type "+f.getType().getName()
								+ " and class "+ oClass.getName()
								+" does not match in original object "
								+original.getClass().getSimpleName()+": "
								+"Expected: "+ original 
								+"Found: "+dup;
						assertThat(msg, dValue, equalTo(oValue));
					}
				}
			}

		}

		assertEquals("Equals method in "+original.getClass().getSimpleName(), original, dup);
	}
	
	// Issue with arrays in hashmaps: Object.hashcode()
	// depends on reference, so is not equal between two
	// arrays. Need to use Arrays.hashcode().
	private static void testHashMapsByField(Field f, Map o, Map d) {
		assertTrue("Hashmaps should not both be null in "+f.getName(), o!=null&&d!=null);
		assertEquals("Maps should contain same number of elements in field '"+f.getName()
		+"'; original: "+o+ " duplicate: "+d, 
		o.size(), d.size());

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
			assertTrue("Field '"+f.getName()+"' should contain element "+v0.toString()+" but does not; set is: "+d, d.contains(v0));
		}
		
//		assertTrue("All elements should be shared in hashset in '"+f.getName()+"'", d.containsAll(d));
	}
	
	/**
	 * Test if two lists are equal, item by item
	 * @param f the field to test
	 * @param o the original list
	 * @param d the duplicate list
	 * @throws Exception 
	 */
	private static void testListEqualityByField(Field f, List o, List d, Set<String> fieldsToSkip) throws Exception {
		assertTrue("Lists should not both be null", o!=null&&d!=null);
		assertEquals("Lists should contain same number of elements", o.size(), d.size());
		
		for(int i=0; i<o.size(); i++) {
			assertEquals("Field '"+f.getName()+"' element "+i+" should be equal", o.get(i), d.get(i));
		}
		assertTrue("All elements should be shared in list in '"+f.getName()+"'", o.containsAll(d));
	}
		
	/**
	 * Test if the fields of two objects have the same hashcodes.
	 * Skips cache classes which are not used in hashcode methods. 
	 * @param original
	 * @param dup
	 * @throws Exception
	 */
	public static void testDuplicatesByField(Object original, Object dup) throws Exception {
		Set<String> fieldsToSkip = new HashSet<>();
		testDuplicatesByField(original, dup, fieldsToSkip);
	}

}
