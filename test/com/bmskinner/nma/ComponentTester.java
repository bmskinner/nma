package com.bmskinner.nma;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.eclipse.jdt.annotation.NonNull;

import com.bmskinner.nma.components.generic.IPoint;

/**
 * Base class for the component tests
 * 
 * @author Ben Skinner
 * @since 1.14.0
 *
 */
public abstract class ComponentTester extends FloatArrayTester {

	public static final long RNG_SEED = 1234;
	public static final int N_CELLS = 10;
	public static final int N_CHILD_DATASETS = 2;
	private static final Logger LOGGER = Logger.getLogger(ComponentTester.class.getName());

	/** Classes that we have custom code to inspect **/
	private static final List<Class<?>> SPECIAL_CLASSES = List.of(HashMap.class,
			ConcurrentHashMap.class,
			LinkedHashMap.class, Map.class, HashSet.class, ArrayList.class);

	/**
	 * Test if the two given points are vertically aligned
	 * 
	 * @param topPoint    the top point
	 * @param bottomPoint the bottom point
	 * @return
	 */
	public static boolean areVertical(@NonNull IPoint topPoint, @NonNull IPoint bottomPoint) {
		double err = bottomPoint.getX() - topPoint.getX();
		boolean xEqual = (Math.abs(err) < 0.0001);
		boolean yAbove = topPoint.getY() > bottomPoint.getY();
		return xEqual & yAbove;
	}

	/**
	 * Get all the private fields for the class, including superclass fields
	 * 
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
	 * 
	 * @param type the class to fetch
	 * @param name the inherited field name
	 * @return
	 */
	protected static Field getInheritedField(Class<?> type, String name) {
		List<Field> fields = getInheritedPrivateFields(type);
		for (Field f : fields)
			if (f.getName().equals(name)) {
				f.setAccessible(true);
				return f;
			}
		return null;
	}

	/**
	 * Test if the fields of two objects have the same hashcodes. Skips cache
	 * classes which are not used in hashcode methods.
	 * 
	 * @param original
	 * @param dup
	 * @param fieldsToSkip      the names of fields in the objects that should not
	 *                          be compared
	 * @param includeEqualsTest should we run a test of equals methods of the
	 *                          objects? If some fields are skipped, the equals
	 *                          method may be false, but not in a relevant manner.
	 * @throws Exception
	 */
	public static void testDuplicatesByField(String msg, Object original, Object dup,
			Set<String> fieldsToSkip, boolean includeEqualsTest)
			throws Exception {

		for (Field f : getInheritedPrivateFields(dup.getClass())) {

			if (f.getName().equals("this$0")) // skip self recursion
				fieldsToSkip.add(f.getName());

			if (f.getName().equals("parentDataset")) // skip dataset recursion
				fieldsToSkip.add(f.getName());

			if (f.getName().equals("listeners")) // skip event listener recursion
				fieldsToSkip.add(f.getName());

			if (fieldsToSkip.contains(f.getName()))
				continue;

			if (Modifier.isStatic(f.getModifiers())) // ignore static fields
				continue;

			if (Modifier.isTransient(f.getModifiers())) // ignore transient fields
				continue;

			f.setAccessible(true);
			// Skip classes we don't need to compare for equality testing

			if (f.getType().equals(SoftReference.class))
				continue;
			if (f.getType().equals(WeakReference.class))
				continue;
			if (f.getType().equals(Class.forName(
					"com.bmskinner.nma.components.datasets.DefaultCellCollection$DefaultProfileCollection$ProfileCache")))
				continue;
			if (f.getType().equals(Class.forName(
					"com.bmskinner.nma.components.datasets.VirtualDataset$DefaultProfileCollection$ProfileCache")))
				continue;
			if (f.getType()
					.equals(Class.forName("com.bmskinner.nma.components.measure.MeasurementCache")))
				continue;
			if (f.getType().equals(Class.forName("com.bmskinner.nma.components.measure.VennCache")))
				continue;
			if (f.getType()
					.equals(Class.forName("com.bmskinner.nma.components.signals.SignalManager")))
				continue;
			if (f.getType()
					.equals(Class.forName("com.bmskinner.nma.components.profiles.ProfileManager")))
				continue;

			Object oValue = f.get(original);
			Object dValue = f.get(dup);

			// Avoid self referential recursion
			if (original == oValue)
				continue;

			if (oValue != null && dValue != null) {

				Class<?> oClass = oValue.getClass();
				if (SPECIAL_CLASSES.contains(oClass)) {

					if (oClass.equals(Map.class) || oClass.equals(ConcurrentHashMap.class)
							|| oClass.equals(LinkedHashMap.class) || oClass.equals(HashMap.class)) {
						testHashMapsByField(msg + "->" + f.getName(), f, (Map<?, ?>) oValue,
								(Map<?, ?>) dValue);
					}

					if (oClass.equals(HashSet.class)) {
						testHashSetsByField(msg + "->" + f.getName(), f, (HashSet<?>) oValue,
								(HashSet<?>) dValue);
					}
					if (oClass.equals(ArrayList.class)) {
						testListEqualityByField(msg + "->" + f.getName(), f, (List<?>) oValue,
								(List<?>) dValue,
								fieldsToSkip, includeEqualsTest);
					}

				} else {

					// Don't try to compare classes I didn't write
					// and don't try to unpack enums. In these cases, just
					// do a direct equality test
					if (f.getType().getName().startsWith("com.bmskinner.nma")
							&& !f.getType().isEnum()) {
						try {
							testDuplicatesByField(msg + "->" + f.getName(), oValue, dValue,
									fieldsToSkip, includeEqualsTest);
						} catch (StackOverflowError e) {
							String msg2 = "Field '" + f.getName() + "' of type "
									+ f.getType().getName() + " and class "
									+ oClass.getName() + " had a stack overflow on value: " + oValue
									+ " Expected: "
									+ original + " Found: " + dup;
							fail(msg + " " + msg2);
						}
					} else {
						String msg2 = "Field '" + f.getName() + "' of type " + f.getType().getName()
								+ " and class "
								+ oClass.getName() + " does not match in original object "
								+ original.getClass().getSimpleName() + ": " + "Expected: "
								+ original + "Found: "
								+ dup;
						assertThat(msg + " " + msg2, dValue, equalTo(oValue));
					}
				}
			}

		}

		if (includeEqualsTest)
			assertEquals(msg + " Equals method in " + original.getClass().getSimpleName(), original,
					dup);
	}

	// Issue with arrays in hashmaps: Object.hashcode()
	// depends on reference, so is not equal between two
	// arrays. Need to use Arrays.hashcode().
	private static void testHashMapsByField(String msg, Field f, Map<?, ?> o, Map<?, ?> d) {
		assertTrue(msg + " Hashmaps should not both be null in " + f.getName(),
				o != null && d != null);
		assertEquals(msg + " Maps should contain same number of elements in field '" + f.getName()
				+ "'; original: " + o
				+ " duplicate: " + d, o.size(), d.size());

		List<Class<?>> arrayClasses = List.of(byte[].class, float[].class, long[].class,
				int[].class,
				double[].class);

		for (Object e : o.keySet()) {
			Object v0 = o.get(e);
			Object v1 = d.get(e);

			if (v0 == null || v1 == null)
				continue;

			if (arrayClasses.contains(v0.getClass())) {
				long oHash = 0;
				long dHash = 0;

				if (v0.getClass().equals(byte[].class)) {
					oHash += Arrays.hashCode((byte[]) v0);
					dHash += Arrays.hashCode((byte[]) v1);
				}

				if (v0.getClass().equals(long[].class)) {
					oHash += Arrays.hashCode((long[]) v0);
					dHash += Arrays.hashCode((long[]) v1);
				}

				if (v0.getClass().equals(int[].class)) {
					oHash += Arrays.hashCode((int[]) v0);
					dHash += Arrays.hashCode((int[]) v1);
				}

				if (v0.getClass().equals(double[].class)) {
					oHash += Arrays.hashCode((double[]) v0);
					dHash += Arrays.hashCode((double[]) v1);
				}

				if (v0.getClass().equals(float[].class)) {
					oHash += Arrays.hashCode((float[]) v0);
					dHash += Arrays.hashCode((float[]) v1);
				}
				assertEquals(msg + " Hashes should match for key '" + e + "' in " + f.getName(),
						oHash, dHash);
			} else {
				assertEquals(msg + " Map entries for key '" + e + "' in '" + f.getName()
						+ "' should be equal", v0, v1);
				assertTrue(msg + " Map should contain duplicated object", o.containsKey(e));
			}
		}
	}

	private static void testHashSetsByField(String msg, Field f, HashSet<?> o, HashSet<?> d) {
		assertTrue(msg + " Hashsets should not both be null", o != null && d != null);
		assertEquals(msg + " Hashsets should contain same number of elements", o.size(), d.size());

		for (Object v0 : o) {
			if (v0 == null)
				continue;
			assertTrue(msg + " Field '" + f.getName() + "' should contain element " + v0.toString()
					+ " but does not; set is: " + d, d.contains(v0));
		}
	}

	/**
	 * Test if two lists are equal, item by item
	 * 
	 * @param f                 the field to test
	 * @param o                 the original list
	 * @param d                 the duplicate list
	 * @param fieldsToSkip      the names of fields in the objects that should not
	 *                          be compared
	 * @param includeEqualsTest should we run a test of o.containsAll(d) (which uses
	 *                          the equals methods of objects in o and d)
	 * @throws Exception
	 */
	private static void testListEqualityByField(String msg, Field f, List<?> o, List<?> d,
			Set<String> fieldsToSkip, boolean includeEqualsTest)
			throws Exception {
		assertTrue(msg + " Field '" + f.getName() + "' lists should not both be null",
				o != null && d != null);
		assertEquals(
				msg + " Field '" + f.getName() + "' lists should contain same number of elements",
				o.size(),
				d.size());
		fieldsToSkip.add("prevSegment"); // these will overflow in segmented profiles
		fieldsToSkip.add("nextSegment");
		for (int i = 0; i < o.size(); i++) {
			if (o.get(i).getClass().getName().startsWith("com.bmskinner.nma")) {
				testDuplicatesByField(msg, o.get(i), d.get(i), fieldsToSkip, includeEqualsTest);
			} else {
				assertEquals(msg + " Field '" + f.getName() + "' element " + i + " should be equal",
						o.get(i),
						d.get(i));
			}
		}

		if (includeEqualsTest)
			assertTrue(msg + " Field '" + f.getName() + "' all elements should be shared in list",
					o.containsAll(d));
	}

	/**
	 * Test if the fields of two objects have the same hashcodes. Skips cache
	 * classes which are not used in hashcode methods.
	 * 
	 * @param msg      a message to report if the test fails
	 * @param original the original object
	 * @param dup      the duplicate object
	 * @throws Exception
	 */
	public static void testDuplicatesByField(String msg, Object original, Object dup)
			throws Exception {
		Set<String> fieldsToSkip = new HashSet<>();
		testDuplicatesByField(msg, original, dup, fieldsToSkip, true);
	}

	public static boolean shapesEqual(Shape s, Shape d) {
		List<double[]> sr = convertShape(s);
		List<double[]> tr = convertShape(d);

		assertEquals("Shape lengths should match", sr.size(), tr.size());

		for (int i = 0; i < sr.size(); i++) {
			assertTrue("Segment " + i + " should match ", Arrays.equals(sr.get(i), tr.get(i)));
		}
//		for(double[] d : sr)
//			System.out.println(Arrays.toString(d));

		return true;
	}

	private static List<double[]> convertShape(Shape s) {
		PathIterator it = s.getPathIterator(null);
		List<double[]> result = new ArrayList<>();
		while (!it.isDone()) {
			double[] vals = new double[6];
			int t = it.currentSegment(vals);
			vals[5] = t;
			result.add(vals);
			it.next();
		}
		return result;
	}

}
